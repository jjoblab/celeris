'use client';

/**
 * Les cartes des popups — chaque genre rend ses SECTIONS exactes,
 * dans la langue du terrain (FR par défaut, EN au basculement).
 * Encres réelles : erreur #F44747, avertissement #CCA700, accent du
 * thème, badges BadgeGenre (K violet, M accent, F bleu, v cyan, C doré…).
 */

import { useState } from 'react';
import { Lightbulb } from 'lucide-react';
import type { GenreId } from '@/lib/popups';
import type { MachinePopups } from '@/lib/machine';
import { DIAGNOSTICS, USAGES } from '@/lib/machine';

export const ENCRE = {
  erreur: '#F44747',
  avertissement: '#CCA700',
  accent: '#E6C178',
  motcle: '#CD7EE0',
  champ: '#61AFEF',
  variable: '#5CCFE6',
  classe: '#E6C178',
  interface: '#57B6C2',
  enumeration: '#D9A066',
  paquet: '#A0A1AA',
  extrait: '#98C97A',
  muet: 'rgba(255,255,255,0.42)',
};

/* — Les primitives du patron TextRowsPopup — ------------------------------ */

export function Filet({ className = '' }: { className?: string }) {
  return <div className={`mt-1.5 border-t border-white/10 ${className}`} />;
}

export function Titre({ children, encre }: { children: React.ReactNode; encre?: string }) {
  return (
    <div className="text-[12px] font-semibold leading-snug" style={encre ? { color: encre } : undefined}>
      {children}
    </div>
  );
}

export function Corps({ children, encre }: { children: React.ReactNode; encre?: string }) {
  return (
    <div className="text-[11px] leading-relaxed" style={encre ? { color: encre } : undefined}>
      {children}
    </div>
  );
}

export function Meta({ children }: { children: React.ReactNode }) {
  return <div className="text-[10px] leading-relaxed text-white/45">{children}</div>;
}

/** L’en-tête de section — petites capitales muettes, jamais pris. */
export function EnTeteSection({ children }: { children: React.ReactNode }) {
  return (
    <div className="mt-2 select-none text-[9px] font-semibold uppercase tracking-[0.14em] text-white/40">
      {children}
    </div>
  );
}

export function Marqueur({ gravite, taille = 12 }: { gravite: 'erreur' | 'avertissement'; taille?: number }) {
  const encre = gravite === 'erreur' ? ENCRE.erreur : ENCRE.avertissement;
  return (
    <span className="inline-flex select-none items-center justify-center font-bold" style={{ color: encre, fontSize: taille, minWidth: 14 }}>
      {gravite === 'erreur' ? '×' : '!'}
    </span>
  );
}

const BADGES: Record<string, { texte: string; encre: string }> = {
  motcle: { texte: 'K', encre: ENCRE.motcle },
  methode: { texte: 'M', encre: ENCRE.accent },
  champ: { texte: 'F', encre: ENCRE.champ },
  variable: { texte: 'v', encre: ENCRE.variable },
  classe: { texte: 'C', encre: ENCRE.classe },
  interface: { texte: 'I', encre: ENCRE.interface },
  enumeration: { texte: 'E', encre: ENCRE.enumeration },
  paquet: { texte: 'p', encre: ENCRE.paquet },
  extrait: { texte: '{}', encre: ENCRE.extrait },
  annotation: { texte: '@', encre: ENCRE.classe },
};

export function BadgeGenre({ genre }: { genre: keyof typeof BADGES }) {
  const b = BADGES[genre];
  return (
    <span
      className="inline-flex h-4 min-w-4 select-none items-center justify-center rounded-[4px] px-1 text-[9px] font-bold"
      style={{ color: b.encre, backgroundColor: `${b.encre}33` }}
    >
      {b.texte}
    </span>
  );
}

/** Une rangée cliquable du patron TextRowsPopup — surbrillance au doigt. */
export function Rangee({
  children,
  onClick,
  muet,
  indent,
  surbrillance,
  titre: palierTitre,
}: {
  children: React.ReactNode;
  onClick?: () => void;
  muet?: boolean;
  indent?: number;
  surbrillance?: boolean;
  titre?: boolean;
}) {
  return (
    <div
      role={onClick ? 'button' : undefined}
      onClick={onClick}
      className={[
        'flex items-center gap-1.5 rounded-md px-1.5 py-1',
        palierTitre ? 'text-[12px] font-medium' : 'text-[11px]',
        onClick && !muet ? 'cursor-pointer hover:bg-white/10' : '',
        surbrillance ? 'bg-white/10' : '',
        muet ? 'opacity-45' : '',
      ].join(' ')}
      style={indent ? { paddingLeft: 6 + indent * 10 } : undefined}
    >
      {children}
    </div>
  );
}

/* — Le contenu de chaque genre — ------------------------------------------ */

export interface PropsCarte {
  genre: GenreId;
  machine: MachinePopups;
  resteMs: number;
}

const CANDIDATS: { libelle: string; badge: keyof typeof BADGES; identite: string }[] = [
  { libelle: 'consulter', badge: 'methode', identite: 'jo.banque.Compte' },
  { libelle: 'Compte', badge: 'classe', identite: 'jo.banque.Compte' },
  { libelle: 'comptes', badge: 'variable', identite: 'local' },
  { libelle: 'copie', badge: 'variable', identite: 'local' },
  { libelle: 'CompteEpargne', badge: 'classe', identite: 'jo.banque.CompteEpargne' },
  { libelle: 'consulterSolde', badge: 'methode', identite: 'jo.banque.Compte' },
  { libelle: 'couleurs', badge: 'paquet', identite: 'java.awt' },
];

const INTENTIONS: { libelle: string; action: (m: MachinePopups) => void }[] = [
  { libelle: 'Lint — correctifs de la ligne', action: (m) => m.poserAnnonce({ message: '3 correctifs proposés par le lint de la ligne', action: 'Corriger' }) },
  { libelle: 'Aperçu de définition', action: (m) => m.poserVariantSurvol('peek') },
  { libelle: 'Rechercher les usages', action: (m) => m.ouvrirGenre('REFERENCES') },
  { libelle: 'Info de type au curseur', action: (m) => m.ouvrirGenre('TYPE_INFO') },
  { libelle: "Hiérarchie de types ou d’appels", action: (m) => m.ouvrirGenre('HIERARCHY') },
  { libelle: 'Documentation externe', action: (m) => m.ouvrirGenre('EXTERNAL_DOC') },
  { libelle: 'Générer du code', action: (m) => m.ouvrirGenre('GABARITS') },
  { libelle: 'Suppression sûre de la déclaration', action: (m) => m.ouvrirGenre('SAFE_DELETE') },
  { libelle: 'Déplacer la méthode dans une classe', action: (m) => { m.sauter(23); m.ouvrirGenre('SAFE_MOVE'); } },
  { libelle: "Masquer l’inspection de la ligne", action: (m) => { m.sauter(12); m.ouvrirGenre('SUPPRESS_INSPECTION'); } },
  { libelle: 'Exécuter le fichier (simulé)', action: (m) => m.ouvrirGenre('EXCEPTION_DETAILS') },
  { libelle: 'Diff du renommage', action: (m) => m.ouvrirVoile() },
  { libelle: 'Aperçu du correctif LSP', action: (m) => m.poserAnnonce({ message: 'Le correctif importe la classe — l\'aperçu se consulte', action: 'Apercevoir' }) },
  { libelle: 'Agent — révocations', action: (m) => m.poserAnnonce({ message: "La feuille de l’agent s’ouvre (ADR-018)", action: 'Ouvrir' }) },
  { libelle: 'IA fantôme', action: (m) => m.poserAnnonce({ message: 'La suggestion fantôme se peint dans la trame — Tab l\'accepte', action: 'Voir' }) },
  { libelle: 'IA prédiction', action: (m) => m.poserAnnonce({ message: 'La prédiction se prépare (core-ai)', action: 'Voir' }) },
];

const DEBORDEMENTS: { libelle: string; action: (m: MachinePopups) => void }[] = [
  { libelle: 'Rechercher dans le fichier', action: (m) => m.ouvrirVoile() },
  { libelle: 'Plan du fichier', action: (m) => m.poserAnnonce({ message: "La feuille du plan s’ouvre (feuille du bas)", action: 'Ouvrir' }) },
  { libelle: 'Aller à — fichier ou symbole', action: (m) => m.poserAnnonce({ message: "La feuille de l’aller-à s’ouvre (B-13, floue)", action: 'Ouvrir' }) },
  { libelle: 'Renommage', action: (m) => m.poserAnnonce({ message: 'Le renommage prépare son diff (LSP)', action: 'Ouvrir' }) },
  { libelle: 'Réglages', action: (m) => m.poserAnnonce({ message: "La feuille des réglages s’ouvre", action: 'Ouvrir' }) },
  { libelle: 'Diff du renommage', action: (m) => m.ouvrirVoile() },
  { libelle: 'Correctifs LSP', action: (m) => m.poserAnnonce({ message: "La feuille des correctifs s’ouvre (codeAction)", action: 'Ouvrir' }) },
  { libelle: 'IA — révocations', action: (m) => m.poserAnnonce({ message: "La feuille IA s’ouvre", action: 'Ouvrir' }) },
];

function sautEtFerme(m: MachinePopups, genre: GenreId, l: number) {
  m.sauter(l);
  m.fermerGenre(genre);
}

/* — La carte de complétion — */
function CarteCompletion({ machine }: PropsCarte) {
  const prefixe = 'co';
  const [idx, setIdx] = useState(0);
  const actif = machine.ouverts.has('COMPLETION_LIST') ? machine.indexClavier : idx;
  return (
    <div className="w-[204px]">
      {CANDIDATS.map((c, i) => (
        <Rangee key={c.libelle} surbrillance={i === actif} onClick={() => { setIdx(i); machine.validerClavier(); }}>
          <BadgeGenre genre={c.badge} />
          <span className="text-[11px]">
            <span style={{ color: ENCRE.accent, fontWeight: 700 }}>{c.libelle.slice(0, prefixe.length)}</span>
            {c.libelle.slice(prefixe.length)}
          </span>
          <span className="ml-auto max-w-[92px] truncate text-[9px] text-white/35">{c.identite}</span>
        </Rangee>
      ))}
      <Meta>8 candidats au plus · Enter ou Tab accepte · Échap referme</Meta>
    </div>
  );
}

/* — La documentation de complétion (au flanc) — */
function CarteDocCompletion() {
  return (
    <div className="w-[168px]">
      <div className="rounded-md bg-white/5 px-2 py-1">
        <Titre encre={ENCRE.accent}>int consulter()</Titre>
      </div>
      <EnTeteSection>Parameters</EnTeteSection>
      <Corps>Aucun — la méthode lit seule.</Corps>
      <EnTeteSection>Returns</EnTeteSection>
      <Corps>le solde courant du compte.</Corps>
      <Meta>```signature · Markdown typé</Meta>
    </div>
  );
}

/* — L’aide aux paramètres (signature) — */
function CarteSignature() {
  const [surcharge, poserSurcharge] = useState(0);
  const surcharges = [
    'void retirer(int montant)',
    'void retirer(int montant, String motif)',
  ];
  const active = surcharges[surcharge];
  const voisines = surcharges.filter((_, i) => i !== surcharge);
  return (
    <div className="w-[212px]">
      {voisines.map((v) => (
        <Meta key={v}>{v} — surcharge voisine</Meta>
      ))}
      <div className="text-[11px] font-medium">
        <span className="text-white/55">void retirer(</span>
        <span style={{ color: ENCRE.accent, fontWeight: 700 }}>int montant</span>
        {surcharge === 1 && (
          <>
            <span className="text-white/40">, </span>
            <span style={{ color: ENCRE.accent, fontWeight: 700 }}>String motif</span>
          </>
        )}
        <span className="text-white/55">)</span>
      </div>
      <Rangee onClick={() => poserSurcharge((x) => (x + 1) % surcharges.length)}>
        <Meta>‹ {surcharge + 1}/{surcharges.length} › — stepper tactile</Meta>
      </Rangee>
      <Meta>appui long 500 ms — le peek de la documentation</Meta>
    </div>
  );
}

/* — Le survol de symbole (doc ou peek) — */
function CarteSurvol({ machine }: PropsCarte) {
  if (machine.variantSurvol === 'peek') {
    return (
      <div className="w-[188px]">
        <Titre>Définition de retirer</Titre>
        <Filet className="mb-1" />
        <div className="font-mono text-[10px] leading-relaxed text-white/75">
          <div>public void retirer(int montant) {'{'}</div>
          <div>  if (montant &gt; LIMITE) {'{'}</div>
          <div>    throw new IllegalArgumentException(…);</div>
          <div>  {'}'}</div>
          <div>  this.solde -= montant;</div>
          <div>{'}'}</div>
        </div>
        <Meta>extrait du plan — 12 lignes au plus</Meta>
      </div>
    );
  }
  return (
    <div className="w-[188px]">
      <div className="rounded-md bg-white/5 px-2 py-1">
        <Titre encre={ENCRE.accent}>void retirer(int montant)</Titre>
      </div>
      <EnTeteSection>Parameters</EnTeteSection>
      <Corps>
        <span style={{ color: ENCRE.variable }}>montant</span> — la somme à retirer, strictement positive.
      </Corps>
      <EnTeteSection>Throws</EnTeteSection>
      <Corps>
        <span style={{ color: ENCRE.erreur }}>IllegalArgumentException</span> — si le montant dépasse la LIMITE.
      </Corps>
      <Meta>double-tap · « i » de la pilule · action de menu</Meta>
    </div>
  );
}

/* — L’info-bulle de diagnostic (problèmes empilés) — */
function CarteDiagnostic({ machine }: PropsCarte) {
  const problemes = DIAGNOSTICS.filter((d) => d.ligne === machine.caretLigne);
  if (problemes.length === 0) {
    return <Corps>Aucun problème à la ligne du caret.</Corps>;
  }
  return (
    <div className="w-[212px]">
      {problemes.map((d, i) => (
        <div key={d.regle}>
          {i > 0 && <Filet className="my-1.5" />}
          <div className="flex items-start gap-1.5">
            <Marqueur gravite={d.gravite} taille={14} />
            <div>
              <Titre>{d.message}</Titre>
              <Meta>à la ligne {d.ligne + 1} · [{d.regle}]</Meta>
            </div>
          </div>
        </div>
      ))}
      <Filet className="my-1" />
      <Rangee onClick={() => machine.ouvrirGenre('INTENTIONS_MENU')}>
        <Meta>Plus d&apos;actions · menu d&apos;intentions →</Meta>
      </Rangee>
    </div>
  );
}

/* — La carte des usages — */
function CarteUsages({ machine, genre }: PropsCarte) {
  const [idx, setIdx] = useState(0);
  const actif = machine.ouverts.has('REFERENCES') ? machine.indexClavier : idx;
  const tous = [
    ...USAGES.locaux.map((l) => ({ ...l, etranger: false })),
    ...USAGES.etrangeres.map((e) => ({ ligne: e.ligne, extrait: e.fichier, etranger: true })),
  ];
  return (
    <div className="w-[236px]">
      <Titre>{USAGES.compte} usages de « {USAGES.symbole} »</Titre>
      <Meta>le compte du serveur — tous fichiers</Meta>
      <Filet className="mb-1" />
      {tous.map((u, i) => (
        <Rangee
          key={`${u.ligne}-${i}`}
          muet={u.etranger}
          surbrillance={!u.etranger && i === actif}
          onClick={u.etranger ? undefined : () => { setIdx(i); sautEtFerme(machine, genre, u.ligne); }}
        >
          <span style={{ color: ENCRE.muet }}>L{u.ligne + 1} · </span>
          <span className="truncate">
            {u.etranger ? `${u.extrait} — l’occurrence étrangère vit muette` : `${u.extrait.trim().slice(0, 34)}…`}
          </span>
        </Rangee>
      ))}
    </div>
  );
}

/* — L’info de type — */
function CarteTypeInfo() {
  return (
    <div className="w-[132px]">
      <Titre>int</Titre>
      <Meta>le type lu à la déclaration</Meta>
    </div>
  );
}

/* — La hiérarchie — */
function CarteHierarchie({ machine, genre }: PropsCarte) {
  const [idx, setIdx] = useState(0);
  const actif = machine.ouverts.has('HIERARCHY') ? machine.indexClavier : idx;
  const prend = (i: number) => { setIdx(i); sautEtFerme(machine, genre, 0); };
  return (
    <div className="w-[236px]">
      <Titre>Hiérarchie de types de « Compte »</Titre>
      <Meta>la racine vit UNE fois — à l&apos;en-tête</Meta>
      <Filet className="mb-1" />
      <EnTeteSection>Supertypes</EnTeteSection>
      <Rangee indent={1} surbrillance={0 === actif} onClick={() => prend(0)}>
        <span className="text-white/80">Object</span>
      </Rangee>
      <EnTeteSection>Sous-types</EnTeteSection>
      <Rangee indent={1} surbrillance={1 === actif} onClick={() => prend(1)}>
        <span className="text-white/80">CompteEpargne</span>
      </Rangee>
      <Rangee indent={1} surbrillance={2 === actif} onClick={() => prend(2)}>
        <span className="text-white/80">CompteCourant</span>
      </Rangee>
      <EnTeteSection>Hors projet</EnTeteSection>
      <Rangee indent={1} muet>
        <span>CompteJoint</span>
        <Meta>l&apos;introuvable vit muet à la frontière seule</Meta>
      </Rangee>
      <Filet className="mt-1.5" />
      <Meta>Appels : la section « Appelants », la légende du doute — le doute se montre, jamais ne se tranche</Meta>
    </div>
  );
}

/* — La notification — */
function CarteNotification({ machine, resteMs }: PropsCarte) {
  const a = machine.annonce;
  if (!a) return null;
  const reste = Math.max(0, Math.min(resteMs, 4000));
  return (
    <div className="w-[204px]">
      <Titre>{machine.langue === 'fr' ? 'Annonce' : 'Notice'}</Titre>
      <Corps>{a.message}</Corps>
      <Filet className="my-1" />
      <button
        type="button"
        className="text-[11px] font-semibold hover:underline"
        style={{ color: ENCRE.accent }}
        onClick={() => machine.prendreAnnonce()}
      >
        {a.action} →
      </button>
      <div className="mt-1.5 h-0.5 w-full overflow-hidden rounded bg-white/10">
        <div className="h-full transition-[width] duration-100" style={{ width: `${(reste / 4000) * 100}%`, backgroundColor: ENCRE.accent }} />
      </div>
      <Meta>l&apos;horloge la referme — {(reste / 1000).toFixed(1).replace('.', ',')} s</Meta>
    </div>
  );
}

/* — La documentation externe — */
function CarteDocExterne() {
  return (
    <div className="w-[192px]">
      <div className="rounded-md bg-white/5 px-2 py-1">
        <Titre encre={ENCRE.accent}>void retirer(int montant)</Titre>
        <Meta>jo.banque.Compte.retirer</Meta>
      </div>
      <EnTeteSection>Parameters</EnTeteSection>
      <Corps>
        <span style={{ color: ENCRE.variable }}>montant</span> — la somme à retirer.
      </Corps>
      <EnTeteSection>Throws</EnTeteSection>
      <Corps>
        <span style={{ color: ENCRE.erreur }}>IllegalArgumentException</span> — si le montant dépasse LIMITE (500).
      </Corps>
      <Meta>régime de la consultation — la frappe ne la chasse pas</Meta>
    </div>
  );
}

/* — Les gabarits — */
const GABARITS: { libelle: string }[] = [
  { libelle: 'Constructeur de Compte' },
  { libelle: 'Accesseurs de solde' },
  { libelle: 'equals, hashCode et toString de Compte' },
  { libelle: 'Entourer de try/catch' },
  { libelle: 'Entourer de if' },
  { libelle: 'Insérer le gabarit de méthode' },
];

function CarteGabarits({ machine }: PropsCarte) {
  return (
    <div className="w-[236px]">
      <EnTeteSection>Génération &amp; gabarits</EnTeteSection>
      {GABARITS.map((g) => (
        <Rangee
          key={g.libelle}
          titre
          onClick={() => {
            machine.fermerGenre('GABARITS');
            machine.poserAnnonce({ message: `${g.libelle} — la transaction annulable se pose`, action: 'Défaire' });
          }}
        >
          {g.libelle}
        </Rangee>
      ))}
      <Filet className="mt-1" />
      <Meta>le choix referme la carte AVANT d&apos;appliquer</Meta>
    </div>
  );
}

/* — La suppression sûre — */
function CarteSuppression({ machine, genre }: PropsCarte) {
  const bloquants = [
    ...USAGES.locaux.map((l) => ({ ligne: l.ligne, extrait: l.extrait, local: true })),
    ...USAGES.etrangeres.map((e) => ({ ligne: e.ligne, extrait: e.fichier, local: false })),
  ];
  return (
    <div className="w-[236px]">
      <Titre>{USAGES.compte} usages empêchent la suppression de « {USAGES.symbole} »</Titre>
      <Filet className="mb-1" />
      {bloquants.map((u, i) => (
        <Rangee
          key={`${u.ligne}-${i}`}
          muet={!u.local}
          onClick={u.local ? () => sautEtFerme(machine, genre, u.ligne) : undefined}
        >
          <span style={{ color: ENCRE.erreur }}>L{u.ligne + 1} · </span>
          <span className="truncate">
            {u.local ? `${u.extrait.trim().slice(0, 32)}…` : `${u.extrait} — la suppression attend`}
          </span>
        </Rangee>
      ))}
      <Meta>la suppression attend son heure — chacun saute à son lieu</Meta>
    </div>
  );
}

/* — Le déplacement sûr — */
function CarteDeplacement({ machine }: PropsCarte) {
  return (
    <div className="w-[212px]">
      <EnTeteSection>Déplacer dans</EnTeteSection>
      {['CompteEpargne', 'CompteCourant'].map((c) => (
        <Rangee
          key={c}
          titre
          onClick={() => {
            machine.fermerGenre('SAFE_MOVE');
            machine.poserAnnonce({ message: `Déplacée dans « ${c} » — un geste la défait`, action: 'Défaire' });
          }}
        >
          {c}
        </Rangee>
      ))}
      <Filet className="mt-1" />
      <Meta>les conditions de sûreté du moteur seules décident</Meta>
    </div>
  );
}

/* — Le masquage d’inspection — */
function CarteMasquage({ machine }: PropsCarte) {
  const reglables = DIAGNOSTICS.filter((d) => d.ligne === machine.caretLigne);
  if (reglables.length === 0) {
    return <Corps>Aucun diagnostic réglé à la ligne du caret — l&apos;annonce le dirait.</Corps>;
  }
  return (
    <div className="w-[212px]">
      <EnTeteSection>Masquer</EnTeteSection>
      {reglables.map((d) => (
        <Rangee
          key={d.regle}
          titre
          onClick={() => {
            machine.fermerGenre('SUPPRESS_INSPECTION');
            machine.poserAnnonce({ message: `${d.regle} masquée — le lint se relance`, action: 'Relancer' });
          }}
        >
          <Marqueur gravite={d.gravite} taille={11} />
          Masquer {d.regle}
        </Rangee>
      ))}
      <Filet className="mt-1" />
      <Meta>le commentaire noinspection se pose — le lint l&apos;honore à sa relance</Meta>
    </div>
  );
}

/* — Les détails d’exception — */
function CarteException({ machine, genre }: PropsCarte) {
  return (
    <div className="w-[244px]">
      <div className="flex items-center gap-1.5">
        <Marqueur gravite="erreur" taille={14} />
        <Titre encre={ENCRE.erreur}>java.lang.Exception</Titre>
      </div>
      <Corps>L&apos;exécution simulée échoue — la trace suit le plan du fichier</Corps>
      <Filet className="my-1" />
      <Rangee onClick={() => sautEtFerme(machine, genre, 16)}>
        <span style={{ color: ENCRE.muet }}>L17 · </span>Compte.retirer — le cadre du fichier ouvert
      </Rangee>
      <Rangee muet>
        <span style={{ color: ENCRE.muet }}>L8 · </span>Main.main — l&apos;étranger vit muet
      </Rangee>
      <Meta>la trace se relit par l&apos;analyseur de piles (E-06)</Meta>
    </div>
  );
}

/* — Le menu d’intentions — */
function CarteIntentions({ machine }: PropsCarte) {
  return (
    <div className="max-h-[286px] w-[228px] overflow-y-auto pr-1">
      {INTENTIONS.map((i) => (
        <Rangee
          key={i.libelle}
          titre
          onClick={() => {
            machine.fermerGenre('INTENTIONS_MENU');
            i.action(machine);
          }}
        >
          {/* l'icône d'ampoule de chaque intention (IconeCarte.AMP — le
              menu Alt+Enter de l'IDE iconise ses rangées, à l'encre
              d'avertissement comme la carte réelle) */}
          <Lightbulb className="h-3 w-3 shrink-0" style={{ color: ENCRE.avertissement }} aria-hidden />
          {i.libelle}
        </Rangee>
      ))}
      <Filet className="mt-1" />
      <Meta>16 intentions — le choix referme la carte AVANT d&apos;exécuter</Meta>
    </div>
  );
}

/* — Le débordement de la toolbar — */
function CarteDebordementToolbar({ machine }: PropsCarte) {
  return (
    <div className="max-h-[286px] w-[212px] overflow-y-auto pr-1">
      <EnTeteSection>Plus</EnTeteSection>
      {DEBORDEMENTS.map((d) => (
        <Rangee
          key={d.libelle}
          titre
          onClick={() => {
            machine.fermerGenre('TOOLBAR_OVERFLOW');
            d.action(machine);
          }}
        >
          {d.libelle}
        </Rangee>
      ))}
      <Filet className="mt-1" />
      <Meta>pendue sous la puce — le défilement l&apos;épargne</Meta>
    </div>
  );
}

/* — Le sélecteur de couleur — */
function CarteCouleur() {
  const [hex, poserHex] = useState('#E6C178');
  const teintes = ['#F44747', '#E6C178', '#CCA700', '#98C97A', '#5CCFE6', '#57B6C2', '#61AFEF', '#CD7EE0', '#D9A066', '#A0A1AA', '#FF8A65', '#A5D6A7', '#4DD0E1', '#9575CD', '#A1887F', '#78909C'];
  return (
    <div className="w-[176px]">
      <div className="flex items-center gap-2">
        <span className="h-4 w-4 rounded border border-white/20" style={{ backgroundColor: '#E6C178' }} />
        <span className="text-white/35">→</span>
        <span className="h-4 w-4 rounded border border-white/25" style={{ backgroundColor: hex }} />
        <span className="font-mono text-[10px] text-white/70">{hex}</span>
      </div>
      <div
        className="mt-2 h-14 w-full rounded-md"
        style={{ background: `linear-gradient(to top, #000, ${hex})` }}
        aria-label="Champ saturation / valeur"
      />
      <div className="mt-1.5 flex h-3 w-full rounded" style={{ background: 'linear-gradient(to right, #F44747, #E6C178, #98C97A, #5CCFE6, #57B6C2)' }} />
      <div className="mt-1.5 grid grid-cols-8 gap-1">
        {teintes.map((t) => (
          <button key={t} type="button" aria-label={`teinte ${t}`} className="h-3 w-full rounded-sm" style={{ backgroundColor: t }} onClick={() => poserHex(t)} />
        ))}
      </div>
      <Meta>le pick s&apos;applique implicite — l&apos;alpha d&apos;origine préservé</Meta>
    </div>
  );
}

/** Le dispatch — le contenu de la carte d’un genre. */
export function ContenuCarte(props: PropsCarte): React.ReactNode {
  switch (props.genre) {
    case 'COMPLETION_LIST': return <CarteCompletion {...props} />;
    case 'COMPLETION_DOC': return <CarteDocCompletion />;
    case 'SIGNATURE_HELP': return <CarteSignature />;
    case 'HOVER': return <CarteSurvol {...props} />;
    case 'DIAGNOSTIC_TOOLTIP': return <CarteDiagnostic {...props} />;
    case 'REFERENCES': return <CarteUsages {...props} />;
    case 'TYPE_INFO': return <CarteTypeInfo />;
    case 'HIERARCHY': return <CarteHierarchie {...props} />;
    case 'NOTIFICATION': return <CarteNotification {...props} />;
    case 'EXTERNAL_DOC': return <CarteDocExterne />;
    case 'GABARITS': return <CarteGabarits {...props} />;
    case 'SAFE_DELETE': return <CarteSuppression {...props} />;
    case 'SAFE_MOVE': return <CarteDeplacement {...props} />;
    case 'SUPPRESS_INSPECTION': return <CarteMasquage {...props} />;
    case 'EXCEPTION_DETAILS': return <CarteException {...props} />;
    case 'INTENTIONS_MENU': return <CarteIntentions {...props} />;
    case 'TOOLBAR_OVERFLOW': return <CarteDebordementToolbar {...props} />;
    case 'COLOR_PICKER': return <CarteCouleur />;
    default: return null;
  }
}

export { INTENTIONS, DEBORDEMENTS, GABARITS };
