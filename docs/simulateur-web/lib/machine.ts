'use client';

/**
 * La machine du simulateur — le même état qu’un éditeur réel :
 * genres ouverts (Set), caret, fenêtre visible, journal, annonce.
 * Toutes les transitions passent par les fonctions pures de popups.ts
 * (portage exact du PopupController) puis se journalisent en français.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  GenreId,
  POPUP_PAR_ID,
  echapper,
  fermer,
  fermesEntre,
  frapper,
  taperHorsCartes,
  poserVoile,
  defiler,
  ouvrir,
} from './popups';

export interface EntreeJournal {
  id: number;
  genre: 'action' | 'regime' | 'voile' | 'saut' | 'horloge' | 'info';
  texte: string;
}

export interface Annonce {
  message: string;
  action: string;
}

export const LIGNES = 31;
export const HAUT_LIGNE = 22;
export const LIGNES_VISIBLES = 13;
export const GOUTTIERE = 44;

export interface MachinePopups {
  ouverts: Set<GenreId>;
  caretLigne: number;
  fenetre: number;
  journal: EntreeJournal[];
  langue: 'fr' | 'en';
  voile: boolean;
  annonce: Annonce | null;
  resteMs: number;
  variantSurvol: 'doc' | 'peek';
  chrome: { bande: boolean; minimap: boolean; pouce: boolean; touches: boolean; ampoule: boolean; hints: boolean };
  indexClavier: number;
  ouvrirGenre: (g: GenreId, note?: string) => void;
  fermerGenre: (g: GenreId) => void;
  frapperLettre: () => void;
  defilerVers: (pas: number) => void;
  taperHorsCartes: () => void;
  echapperCarte: () => void;
  ouvrirVoile: () => void;
  fermerVoile: () => void;
  fermerTout: () => void;
  sauter: (l: number) => void;
  poserCaret: (l: number) => void;
  basculerChrome: (k: 'bande' | 'minimap' | 'pouce' | 'touches' | 'ampoule' | 'hints') => void;
  poserAnnonce: (a: Annonce) => void;
  cloreAnnonce: (raison: string) => void;
  prendreAnnonce: () => void;
  basculerLangue: () => void;
  poserVariantSurvol: (v: 'doc' | 'peek') => void;
  bougerIndexClavier: (pas: number) => void;
  validerClavier: () => void;
}

const NOMS_FR: Record<GenreId, string> = Object.fromEntries(
  (Object.keys(POPUP_PAR_ID) as GenreId[]).map((g) => [g, POPUP_PAR_ID[g].nomFr]),
) as Record<GenreId, string>;

export function lister(genres: GenreId[]): string {
  return genres.map((g) => NOMS_FR[g].toLowerCase()).join(', ');
}

export function useMachinePopups(): MachinePopups {
  const [ouverts, setOuverts] = useState<Set<GenreId>>(new Set());
  const [caretLigne, setCaretLigne] = useState(12);
  const [fenetre, setFenetre] = useState(0);
  const [journal, setJournal] = useState<EntreeJournal[]>([]);
  const [langue, setLangue] = useState<'fr' | 'en'>('fr');
  const [voile, setVoile] = useState(false);
  const [annonce, setAnnonce] = useState<Annonce | null>(null);
  const [reste, setReste] = useState(4000);
  const [variantSurvol, setVariantSurvol] = useState<'doc' | 'peek'>('doc');
  const [chrome, setChrome] = useState({ bande: true, minimap: true, pouce: true, touches: true, ampoule: true, hints: true });
  const [indexClavier, setIndexClavier] = useState(0);
  const compteur = useRef(0);

  const noter = useCallback((genre: EntreeJournal['genre'], texte: string) => {
    compteur.current += 1;
    setJournal((j) => [{ id: compteur.current, genre, texte }, ...j].slice(0, 9));
  }, []);

  const transition = useCallback(
    (avant: Set<GenreId>, apres: Set<GenreId>, verbe: string, complement?: string) => {
      const fermes = fermesEntre(avant, apres);
      if (fermes.length > 0) {
        noter('regime', `${verbe} ferme : ${lister(fermes)}${complement ?? ''}`);
      } else if (complement) {
        noter('regime', complement);
      }
      setOuverts(apres);
    },
    [noter],
  );

  const ouvrirGenre = useCallback(
    (g: GenreId, note?: string) => {
      const avant = ouverts;
      const apres = ouvrir(avant, g);
      if (apres === avant) {
        const spec = POPUP_PAR_ID[g];
        if (g === 'COMPLETION_DOC') {
          noter('info', 'la documentation de complétion refuse — sa liste est fermée');
        } else if (g === 'SELECTION_OVERFLOW') {
          noter('info', 'le débordement refuse — sa barre de sélection est fermée');
        } else {
          noter('info', `${spec.nomFr} est déjà ouverte`);
        }
        return;
      }
      const fermes = fermesEntre(avant, apres);
      if (g === 'COMPLETION_LIST' && !avant.has('COMPLETION_LIST')) {
        // La doc suit sa liste : docDe du premier candidat documenté.
        apres.add('COMPLETION_DOC');
      }
      if (fermes.length > 0) {
        noter('action', `${POPUP_PAR_ID[g].nomFr} s’ouvre — une surface d’actions à la fois, elle chasse : ${lister(fermes)}`);
      } else {
        noter('action', `${POPUP_PAR_ID[g].nomFr} s’ouvre${note ? ` — ${note}` : ''}`);
      }
      setIndexClavier(0);
      setOuverts(apres);
    },
    [ouverts, noter],
  );

  const fermerGenre = useCallback(
    (g: GenreId) => {
      const apres = fermer(ouverts, g);
      const fermes = fermesEntre(ouverts, apres);
      if (fermes.length > 0) {
        const suites = fermes.filter((x) => x !== g);
        const texte = suites.length > 0 ? `${NOMS_FR[g]} se ferme — sa cascade emporte : ${lister(suites)}` : `${NOMS_FR[g]} se ferme`;
        noter('info', texte);
      } else {
        noter('info', `${NOMS_FR[g]} était déjà fermée`);
      }
      setOuverts(apres);
    },
    [ouverts, noter],
  );

  const frapperLettre = useCallback(() => {
    const avant = ouverts;
    const apres = frapper(avant);
    const fermes = fermesEntre(avant, apres);
    const lignes: string[] = [];
    if (fermes.length > 0) lignes.push(`la frappe ferme : ${lister(fermes)}`);
    if (apres.has('COMPLETION_LIST')) lignes.push('la complétion RESTE — elle filtre localement, la requête ne repart pas');
    if (apres.has('SIGNATURE_HELP')) lignes.push("l’aide aux paramètres se rafraîchit");
    if (apres.has('NOTIFICATION')) lignes.push("l’annonce vit son quart — la frappe l’épargne");
    if (lignes.length === 0) lignes.push('aucune bulle ne cède — les régimes de la consultation épargnent ce qui est ouvert');
    noter('regime', lignes.join(' ; '));
    // La frappe propose : le caret termine un identifiant → la liste se pose.
    const ligne = LIGNE(caretLigne);
    if (/[A-Za-z_]\s*$/.test(ligne) && !apres.has('COMPLETION_LIST')) {
      const avec = new Set(apres);
      avec.add('COMPLETION_LIST');
      avec.add('COMPLETION_DOC');
      noter('action', 'la frappe propose — la liste de complétion se pose, sa documentation au flanc');
      setOuverts(avec);
    } else {
      setOuverts(apres);
    }
  }, [ouverts, caretLigne, noter]);

  const defilerVers = useCallback(
    (pas: number) => {
      const avant = ouverts;
      const apres = defiler(avant);
      const fermes = fermesEntre(avant, apres);
      const cible = Math.min(Math.max(fenetre + pas, 0), Math.max(LIGNES - LIGNES_VISIBLES, 0));
      setFenetre(cible);
      const repositionnees = Array.from(apres).filter((g) => !POPUP_PAR_ID[g].chrome);
      const notes: string[] = [];
      if (fermes.length > 0) notes.push(`le défilement ferme : ${lister(fermes)} — l’ancre vit au curseur`);
      if (repositionnees.length > 0) {
        notes.push(`le reste se REPOSITIONNE : ${lister(repositionnees)}`);
      }
      if (notes.length === 0) notes.push('rien de pose — le défilement ne dérange aucun régime ouvert');
      noter('regime', notes.join(' ; '));
      setOuverts(apres);
    },
    [ouverts, fenetre, noter],
  );

  const taperHorsCartesCb = useCallback(() => {
    const avant = ouverts;
    const apres = taperHorsCartes(avant);
    const fermes = fermesEntre(avant, apres);
    if (fermes.length > 0) {
      noter('regime', `le tap hors cartes referme : ${lister(fermes)}`);
    } else {
      noter('regime', 'le tap hors cartes ne referme rien — la complétion, la signature et le chrome suivent le curseur');
    }
    setOuverts(apres);
  }, [ouverts, noter]);

  const echapperCarte = useCallback(() => {
    const apres = echapper(ouverts);
    if (apres.size === ouverts.size) {
      noter('regime', "l’échappement ne trouve rien — ni carte interactive, ni éphémère ouverte");
      return;
    }
    const prises = fermesEntre(ouverts, apres);
    const interactive = (Object.keys(POPUP_PAR_ID) as GenreId[]).filter((g) => ouverts.has(g));
    void interactive;
    noter('regime', `l’échappement referme : ${lister(prises)} — la première carte interactive de l’ORDRE_CLAVIER, sinon le premier éphémère`);
    setOuverts(apres);
  }, [ouverts, noter]);

  const ouvrirVoile = useCallback(() => {
    const apres = poserVoile(ouverts);
    const fermes = fermesEntre(ouverts, apres);
    setVoile(true);
    setAnnonce(null);
    noter('voile', fermes.length > 0 ? `la feuille pose son voile — l’intelligence et les actions cèdent : ${lister(fermes)}` : 'la feuille pose son voile — le chrome (fil d\'Ariane, bande, minimap, pouce, touches) vit avec le document');
    setOuverts(apres);
  }, [ouverts, noter]);

  const fermerVoile = useCallback(() => {
    setVoile(false);
    noter('voile', 'la feuille se referme — sur demande seule, rien ne se rouvre');
  }, [noter]);

  const fermerToutCb = useCallback(() => {
    if (ouverts.size === 0) {
      noter('info', 'rien à fermer — la couche est déjà vide');
      return;
    }
    noter('regime', `closeAll vide la couche : ${lister(Array.from(ouverts))}`);
    setOuverts(new Set());
    setAnnonce(null);
  }, [ouverts, noter]);

  const sauter = useCallback(
    (l: number) => {
      setCaretLigne(l);
      if (l < fenetre) setFenetre(Math.max(l - 2, 0));
      if (l > fenetre + LIGNES_VISIBLES - 2) {
        setFenetre(Math.min(l - 4, Math.max(LIGNES - LIGNES_VISIBLES, 0)));
      }
      noter('saut', `le saut porte le caret à la ligne ${l + 1}`);
    },
    [fenetre, noter],
  );

  const poserCaret = useCallback(
    (l: number) => {
      setCaretLigne(l);
      const apres = taperHorsCartes(ouverts);
      const fermes = fermesEntre(ouverts, apres);
      if (fermes.length > 0) {
        noter('regime', `le tap dans le texte referme (hors cartes) : ${lister(fermes)} — l’avis ne consomme jamais le geste`);
      }
      setOuverts(apres);
    },
    [ouverts, noter],
  );

  const basculerChrome = useCallback((k: 'bande' | 'minimap' | 'pouce' | 'touches' | 'ampoule' | 'hints') => {
    setChrome((c) => ({ ...c, [k]: !c[k] }));
  }, []);

  const poserAnnonce = useCallback(
    (a: Annonce) => {
      const apres = ouvrir(ouverts, 'NOTIFICATION');
      setReste(4000);
      noter('action', "l’annonce se pose au coin bas-droit — l’horloge s’arme (4000 ms), la re-pose la réarme sans cumul");
      setAnnonce(a);
      setOuverts(apres);
    },
    [ouverts, noter],
  );

  const cloreAnnonce = useCallback(
    (raison: string) => {
      const apres = fermer(ouverts, 'NOTIFICATION');
      setAnnonce(null);
      setOuverts(apres);
      noter('horloge', raison);
    },
    [ouverts, noter],
  );

  // L’horloge de la notification — 4000 ms, l’état se réarme dans le geste qui
  // pose (poserAnnonce), jamais dans le corps de l’effet.
  useEffect(() => {
    if (!ouverts.has('NOTIFICATION')) return;
    const depart = Date.now();
    const t = window.setInterval(() => {
      const ecoule = Date.now() - depart;
      if (ecoule >= 4000) {
        window.clearInterval(t);
        cloreAnnonce("l’horloge referme l’annonce — son quart est venu (4000 ms)");
      } else {
        setReste(4000 - ecoule);
      }
    }, 100);
    return () => window.clearInterval(t);
  }, [ouverts, cloreAnnonce]);

  const prendreAnnonce = useCallback(() => {
    cloreAnnonce('le tap prend l\'annonce — la prise referme la bulle AVANT de faire courir l\'action');
  }, [cloreAnnonce]);

  const basculerLangue = useCallback(() => {
    setLangue((l) => (l === 'fr' ? 'en' : 'fr'));
  }, []);

  const poserVariantSurvol = useCallback(
    (v: 'doc' | 'peek') => {
      setVariantSurvol(v);
      ouvrirGenre('HOVER', v === 'peek' ? 'peek — « Définition de solde », extrait du plan (≤ 12 lignes)' : 'documentation du symbole au point');
    },
    [ouvrirGenre],
  );

  const bougerIndexClavier = useCallback((pas: number) => {
    setIndexClavier((i) => Math.max(0, Math.min(7, i + pas)));
  }, []);

  const validerClavier = useCallback(() => {
    if (ouverts.has('COMPLETION_LIST')) {
      const apres = fermer(ouverts, 'COMPLETION_LIST');
      setOuverts(apres);
      noter('regime', 'Enter accepte le candidat surligné — le mot se remplace, la liste et sa documentation se ferment');
    } else if (ouverts.has('REFERENCES')) {
      const apres = fermer(ouverts, 'REFERENCES');
      setOuverts(apres);
      noter('regime', 'Enter prend la rangée surlignée — le saut court, la carte se referme');
    } else if (ouverts.has('HIERARCHY')) {
      const apres = fermer(ouverts, 'HIERARCHY');
      setOuverts(apres);
      noter('regime', 'Enter prend le sommet surligné — le saut court, la carte se referme');
    }
  }, [ouverts, noter]);

  return {
    ouverts,
    caretLigne,
    fenetre,
    journal,
    langue,
    voile,
    annonce,
    resteMs: ouverts.has('NOTIFICATION') ? reste : 4000,
    variantSurvol,
    chrome,
    indexClavier,
    ouvrirGenre,
    fermerGenre: fermerGenre,
    frapperLettre,
    defilerVers,
    taperHorsCartes: taperHorsCartesCb,
    echapperCarte,
    ouvrirVoile,
    fermerVoile,
    fermerTout: fermerToutCb,
    sauter,
    poserCaret,
    basculerChrome,
    poserAnnonce,
    cloreAnnonce,
    prendreAnnonce,
    basculerLangue,
    poserVariantSurvol,
    bougerIndexClavier,
    validerClavier,
  };
}

/** Le document simulé — Compte.java (contenu illustratif, régimes exacts). */
export const FICHIER: string[] = [
  'public class Compte {',
  '    private final String titulaire;',
  '    private int solde;',
  '    private static final int LIMITE = 500;',
  '    private int teinte = 0xFFE6C178;',
  '',
  '    public Compte(String titulaire, int soldeInitial) {',
  '        this.titulaire = titulaire;',
  '        this.solde = soldeInitial;',
  '    }',
  '',
  '    public int consulter() {',
  '        var resultat = solde;',
  '        return solde;',
  '    }',
  '',
  '    public void retirer(int montant) {',
  '        if (montant > LIMITE) {',
  '            throw new IllegalArgumentException("Montant trop élevé");',
  '        }',
  '        this.solde -= montant;',
  '    }',
  '',
  '    public void deposer(int montant) {',
  '        this.solde += montant;',
  '    }',
  '',
  '    @Override',
  '    public String toString() {',
  '        return titulaire + " · " + solde;',
  '    }',
  '}',
];

export function LIGNE(i: number): string {
  return FICHIER[i] ?? '';
}

/** Les inlay hints du terrain simulé — le type d’un « var » se lit en
 *  fin de ligne utile, à l’encre muette italique (comme l’IDE de
 *  référence : jamais dans le flux, le texte ne se décale pas). */
export const INLAY_HINTS: { ligne: number; texte: string }[] = [
  { ligne: 12, texte: ': int' },
];

/** Les diagnostics du terrain simulé — id entre crochets, jamais deviné. */
export const DIAGNOSTICS: { ligne: number; gravite: 'erreur' | 'avertissement'; marqueur: string; message: string; regle: string }[] = [
  {
    ligne: 12,
    gravite: 'avertissement',
    marqueur: '!',
    message: 'La variable « resultat » n\'est jamais lue',
    regle: 'UnusedVariable',
  },
  {
    ligne: 18,
    gravite: 'erreur',
    marqueur: '×',
    message: "L’exception IllegalArgumentException s’échappe de retirer",
    regle: 'UnhandledException',
  },
];

/** Les usages de « solde » — le compte du serveur, tous fichiers. */
export const USAGES = {
  symbole: 'solde',
  compte: 7,
  locaux: [
    { ligne: 2, extrait: '    private int solde;' },
    { ligne: 8, extrait: '        this.solde = soldeInitial;' },
    { ligne: 12, extrait: '        var resultat = solde;' },
    { ligne: 20, extrait: '        this.solde -= montant;' },
    { ligne: 24, extrait: '        this.solde += montant;' },
    { ligne: 29, extrait: '        return titulaire + " · " + solde;' },
  ],
  etrangeres: [{ fichier: 'Main.java', ligne: 7 }],
};
