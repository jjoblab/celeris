'use client';

/**
 * Celeris — preview interactive des popups de l’éditeur.
 *
 * Le simulateur (gauche) : un éditeur Android miniature où chaque geste
 * passe par la machine à états — portage exact du PopupController.
 * Le catalogue (droite) : les 26 genres par catégorie A–G, la fiche
 * détaillée de chacun (sections du design, déclencheurs, régimes).
 */

import { Layers3, MousePointerClick } from 'lucide-react';
import { useMachinePopups } from '@/lib/machine';
import { FAMILLE_ACTIONS } from '@/lib/popups';
import { SimulateurEditeur } from '@/components/simulateur/SimulateurEditeur';
import { CataloguePopups } from '@/components/catalogue/CataloguePopups';

const REGIMES = [
  { titre: 'La frappe', texte: "Frappe de saisie : ferme survol, info-bulle, barre de sélection, débordements, sélecteur de couleur, usages, info de type, hiérarchie, usages qui barrent. La complétion RESTE — elle filtre localement ; l’aide aux paramètres se rafraîchit ; le chrome survit ; la consultation (doc externe, gabarits, cibles, masquage, exception) est épargnée." },
  { titre: 'Le défilement', texte: "Ferme l’ancre au curseur : survol, info-bulle, sélecteur, usages, info de type, hiérarchie, usages qui barrent. Le reste se REPOSITIONNE — complétion, fil d’Ariane, barre de sélection, menu du caret, documentation externe, gabarits, cibles, masquage, exception. La carte de la toolbar est épargnée (ancre au coin de scène)." },
  { titre: 'Le tap hors cartes', texte: "Referme les cartes d’actions et les bulles — le geste ne se consomme pas : le caret se pose quand même. La complétion, la signature, la barre de sélection, le fil d’Ariane, le chrome et l’annonce suivent le curseur — ils ne cèdent pas." },
  { titre: "L’horloge", texte: "La notification seule : 4000 ms, réarmés à la re-pose sans cumul, coupée par le tap qui la prend. Frappe, défilement et geste extérieur l’épargnent — l’annonce vit son quart." },
  { titre: "L’échappement", texte: "La première carte interactive ouverte de l’ORDRE_CLAVIER (complétion, intentions, gabarits, débordements, usages, hiérarchie, sûretés, masquage, exception) ; sinon le premier éphémère (signature, survol, info-bulle, sélecteur, info de type, doc externe, exception). Jamais le chrome, jamais la barre." },
  { titre: 'Le voile modal', texte: "Une feuille ADR-018 coupe 19 genres : intelligence et actions cèdent au voile. Le chrome (fil d’Ariane, bande, minimap, pouce, touches) vit avec le document ; l’exception et la recherche y échappent." },
];

const SURFACES = [
  { n: '1ʳᵉ', nom: 'TypeInfo', note: 'LecteurTypes — le type à la déclaration' },
  { n: '2ᵉ', nom: 'Usages', note: 'le serveur par le fil references' },
  { n: '3ᵉ', nom: 'DocExterne', note: 'la carte dérivée, instance propre' },
  { n: '4ᵉ', nom: 'CarteHierarchies', note: 'la transpose pure déménagée' },
  { n: '5ᵉ', nom: 'Gabarits', note: 'champs réels B-24, canal A-22' },
  { n: '6ᵉ', nom: 'Surete', note: 'deux cartes — suppression et déménagement' },
  { n: '7ᵉ', nom: 'Masquage', note: 'noinspection + relance du lint' },
  { n: '8ᵉ', nom: 'ExceptionCarte', note: 'la trace du document réel, E-06' },
];

export default function Page() {
  const machine = useMachinePopups();

  return (
    <div className="min-h-screen bg-[#0a0b0f] text-white/85">
      <div className="mx-auto w-full max-w-7xl px-4 pb-10 pt-8 sm:px-6 lg:px-8">
        {/* L’en-tête */}
        <header className="mb-8">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-[#E6C178]/40 bg-[#E6C178]/10">
              <Layers3 className="h-5 w-5 text-[#E6C178]" aria-hidden />
            </div>
            <div>
              <h1 className="text-xl font-semibold tracking-tight text-white sm:text-2xl">
                Celeris — les popups de l&apos;éditeur
              </h1>
              <p className="mt-0.5 text-[12px] text-white/50">
                Preview interactive des 26 genres — fidèle au PopupController du dépôt (commit 88b3206)
              </p>
            </div>
            <div className="ml-auto hidden flex-wrap gap-1.5 sm:flex">
              {[
                '26 genres',
                '6 régimes',
                '8 satellites',
                'catégories A–G',
                '2 466 tests verts',
              ].map((s) => (
                <span key={s} className="rounded-full border border-white/10 bg-white/[0.04] px-2.5 py-1 text-[10px] font-medium text-white/60">
                  {s}
                </span>
              ))}
            </div>
          </div>
          <p className="mt-3 max-w-3xl text-[12.5px] leading-relaxed text-white/55">
            Chaque popup affiche ici exactement ce qu&apos;elle est censée afficher : ses sections,
            dans son ordre, aux encres du terrain. Déclenchez, frappez, défilez, échappez — la machine
            applique les tables réelles du contrôleur (FAMILLE_ACTIONS, frappe, défilement, tap hors
            cartes, voile, échappement) et le journal dit ce qui cède et ce qui demeure.
          </p>
        </header>

        {/* Le corps : simulateur + catalogue */}
        <div className="grid items-start gap-8 lg:grid-cols-[440px_1fr]">
          <section id="simulateur" className="lg:sticky lg:top-6">
            <h2 className="mb-3 flex items-center gap-2 text-[13px] font-semibold uppercase tracking-[0.14em] text-white/50">
              <MousePointerClick className="h-4 w-4" aria-hidden /> Le simulateur
            </h2>
            <SimulateurEditeur machine={machine} />
          </section>

          <section aria-label="Le catalogue des popups">
            <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.14em] text-white/50">
              Le catalogue — 26 genres, catégorie par catégorie
            </h2>
            <CataloguePopups machine={machine} />
          </section>
        </div>

        {/* Les régimes */}
        <section className="mt-12" aria-label="Les régimes de coexistence">
          <h2 className="mb-1 text-[13px] font-semibold uppercase tracking-[0.14em] text-white/50">
            Les six régimes — qui cède, qui demeure
          </h2>
          <p className="mb-4 max-w-3xl text-[12px] text-white/45">
            Les listes exactes du contrôleur — essayez chaque régime dans le simulateur, le journal
            nomme ce qui se ferme.
          </p>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {REGIMES.map((r) => (
              <div key={r.titre} className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
                <h3 className="text-[13px] font-semibold text-[#E6C178]">{r.titre}</h3>
                <p className="mt-1.5 text-[11.5px] leading-relaxed text-white/60">{r.texte}</p>
              </div>
            ))}
          </div>
        </section>

        {/* La famille des actions */}
        <section className="mt-8" aria-label="La famille des surfaces d’actions">
          <h2 className="mb-1 text-[13px] font-semibold uppercase tracking-[0.14em] text-white/50">
            La famille des surfaces d&apos;actions — une seule à la fois
          </h2>
          <p className="mb-4 max-w-3xl text-[12px] text-white/45">
            La table FAMILLE_ACTIONS du contrôleur : l&apos;ouverture chasse les concurrents. Les
            MENUS (intentions, débordement toolbar, gabarits, cibles, masquage) cèdent aussi à la
            complétion ; les CARTES qui décrivent (usages, hiérarchie, usages qui barrent)
            cohabitent avec elle.
          </p>
          <div className="overflow-x-auto rounded-2xl border border-white/10">
            <table className="w-full min-w-[560px] border-collapse text-left text-[11px]">
              <thead>
                <tr className="border-b border-white/10 bg-white/[0.04]">
                  <th className="px-3 py-2 font-semibold text-white/70">Le genre ouvert…</th>
                  <th className="px-3 py-2 font-semibold text-white/70">…chasse</th>
                  <th className="px-3 py-2 font-semibold text-white/70">…cohabite avec</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(FAMILLE_ACTIONS).map(([g, chasses]) => (
                  <tr key={g} className="border-b border-white/[0.06] align-top">
                    <td className="px-3 py-2 font-mono text-[10px] text-white/80">{g}</td>
                    <td className="px-3 py-2 font-mono text-[10px] text-[#F44747]/80">
                      {chasses.join(' · ')}
                    </td>
                    <td className="px-3 py-2 text-[10px] text-white/40">
                      {[g === 'COMPLETION_LIST' ? 'signature, cartes qui décrivent (usages, hiérarchie, usages qui barrent)' : 'la complétion, l\u2019aide aux paramètres, les bulles qui décrivent (type info, doc externe), l\u2019annonce']}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* Les huit surfaces portées */}
        <section className="mt-8" aria-label="Les huit surfaces portées">
          <h2 className="mb-1 text-[13px] font-semibold uppercase tracking-[0.14em] text-white/50">
            Les huit satellites de l&apos;orchestrateur — le port du feu vert
          </h2>
          <p className="mb-4 max-w-3xl text-[12px] text-white/45">
            Les fonctionnalités vivent dans les modules éditeur : PopupsEditeur (android-host)
            délègue à ses satellites, le pont de test de la démo est mort. Chaque satellite porte
            sa surface entière — données, carte, câbles.
          </p>
          <ol className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            {SURFACES.map((s) => (
              <li key={s.nom} className="rounded-xl border border-white/10 bg-white/[0.03] px-3 py-2.5">
                <div className="flex items-baseline gap-2">
                  <span className="text-[10px] text-[#E6C178]">{s.n}</span>
                  <span className="font-mono text-[12px] font-semibold text-white/85">{s.nom}</span>
                </div>
                <p className="mt-0.5 text-[10.5px] leading-relaxed text-white/45">{s.note}</p>
              </li>
            ))}
          </ol>
        </section>

        {/* Le pied */}
        <footer className="mt-12 border-t border-white/10 pt-5">
          <p className="text-[11px] leading-relaxed text-white/40">
            Simulation documentaire : les régimes, les exclusions, l&apos;ordre d&apos;échappement,
            la table du voile et l&apos;horloge de 4000 ms viennent du code source
            (PopupController.java, PopupManager.java — android-widgets) ; les libellés FR/EN des
            strings.xml d&apos;android-host ; le contenu du fichier Compte.java et ses diagnostics
            sont illustratifs. Sources : docs/POPUP_INVENTORY.md, docs/POPUP_DEVELOPER_GUIDE.md,
            README.md § popups.
          </p>
        </footer>
      </div>
    </div>
  );
}
