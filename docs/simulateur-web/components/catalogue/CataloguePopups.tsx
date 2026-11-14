'use client';

/**
 * Le catalogue — la grille des 26 genres par catégorie A–G et la fiche
 * détaillée : sections exactes du design, déclencheurs, fermeture,
 * régimes, vue, satellite, données, ancre, chaînes FR/EN du terrain.
 */

import { useMemo, useState } from 'react';
import { ChevronRight, Keyboard, Layers, Play } from 'lucide-react';
import { CATEGORIES, POPUPS, type Categorie, type GenreId, type PopupSpec } from '@/lib/popups';
import type { MachinePopups } from '@/lib/machine';

const ENCRE_CATEGORIE: Record<Categorie, string> = {
  A: '#5CCFE6',
  B: '#CCA700',
  C: '#98C97A',
  D: '#E6C178',
  E: '#F44747',
  F: '#CD7EE0',
  G: '#A0A1AA',
};

const LIBELLE_FRAPPE: Record<PopupSpec['frappe'], string> = {
  ferme: 'la frappe la ferme',
  filtre: 'la frappe filtre — elle reste',
  rafraichit: 'la frappe la rafraîchit',
  epargne: "la frappe l’épargne",
};

const LIBELLE_DEFILEMENT: Record<PopupSpec['defilement'], string> = {
  ferme: 'le défilement la ferme',
  repositionne: 'le défilement la repositionne',
  epargne: "le défilement l’épargne",
  suit: 'le défilement la suit',
};

function Puce({ texte, encre, contour }: { texte: string; encre?: string; contour?: boolean }) {
  return (
    <span
      className={[
        'inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-medium leading-4',
        contour ? 'border' : '',
      ].join(' ')}
      style={
        contour
          ? { borderColor: `${encre ?? 'rgba(255,255,255,0.25)'}66`, color: encre ?? 'rgba(255,255,255,0.6)' }
          : { backgroundColor: `${encre ?? '#A0A1AA'}1f`, color: encre ?? 'rgba(255,255,255,0.6)' }
      }
    >
      {texte}
    </span>
  );
}

/** Ouvre le genre dans le simulateur — les préconditions réelles d’abord. */
function ouvrirDansSimulateur(machine: MachinePopups, g: GenreId) {
  switch (g) {
    case 'COMPLETION_DOC':
      machine.ouvrirGenre('COMPLETION_LIST');
      return;
    case 'SELECTION_OVERFLOW':
      machine.ouvrirGenre('SELECTION_BAR');
      return;
    case 'NOTIFICATION':
      machine.poserAnnonce({ message: '3 correctifs proposés par le lint de la ligne 13', action: 'Corriger' });
      return;
    case 'SEARCH_BAR':
      machine.ouvrirVoile();
      return;
    case 'HOVER':
      machine.poserVariantSurvol('doc');
      return;
    case 'COLOR_PICKER':
      machine.sauter(4);
      machine.ouvrirGenre('COLOR_PICKER');
      return;
    case 'SAFE_MOVE':
      machine.sauter(23);
      machine.ouvrirGenre('SAFE_MOVE');
      return;
    case 'SUPPRESS_INSPECTION':
      machine.sauter(12);
      machine.ouvrirGenre('SUPPRESS_INSPECTION');
      return;
    case 'DIAGNOSTIC_TOOLTIP':
      machine.sauter(12);
      machine.ouvrirGenre('DIAGNOSTIC_TOOLTIP');
      return;
    default:
      machine.ouvrirGenre(g);
  }
  document.getElementById('simulateur')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function Fiche({ spec, machine }: { spec: PopupSpec; machine: MachinePopups }) {
  const encre = ENCRE_CATEGORIE[spec.categorie];
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-5">
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded-md px-1.5 py-0.5 font-mono text-[10px] font-bold" style={{ backgroundColor: `${encre}1f`, color: encre }}>
          {spec.categorie}
        </span>
        <h3 className="text-[16px] font-semibold text-white/90">{spec.nomFr}</h3>
        <span className="font-mono text-[10px] text-white/35">{spec.constName}</span>
        <span className="ml-auto">
          <button
            type="button"
            onClick={() => ouvrirDansSimulateur(machine, spec.id)}
            className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-semibold transition-colors hover:bg-white/10"
            style={{ borderColor: `${encre}66`, color: encre }}
          >
            <Play className="h-3 w-3" aria-hidden /> Ouvrir dans le simulateur
          </button>
        </span>
      </div>

      <p className="mt-3 text-[12.5px] leading-relaxed text-white/70">{spec.description}</p>

      <div className="mt-4">
        <div className="mb-2 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">Les sections du design</div>
        <ol className="space-y-1.5">
          {spec.sections.map((s, i) => (
            <li key={s.titre} className="rounded-lg border border-white/10 bg-white/[0.03] px-3 py-2">
              <div className="flex items-baseline gap-2">
                <span className="font-mono text-[10px]" style={{ color: encre }}>{String(i + 1).padStart(2, '0')}</span>
                <span className="text-[12px] font-medium text-white/85">{s.titre}</span>
              </div>
              {s.lignes.length > 0 && (
                <ul className="mt-1 space-y-0.5 pl-7">
                  {s.lignes.map((l) => (
                    <li key={l} className="text-[11px] leading-relaxed text-white/55">— {l}</li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ol>
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <div>
          <div className="mb-2 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">Comment la déclencher</div>
          <ul className="space-y-1">
            {spec.declencheurs.map((d) => (
              <li key={d} className="flex items-start gap-2 text-[11.5px] leading-relaxed text-white/70">
                <ChevronRight className="mt-0.5 h-3 w-3 shrink-0 text-white/30" aria-hidden /> {d}
              </li>
            ))}
          </ul>
        </div>
        <div>
          <div className="mb-2 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">Comment elle se ferme</div>
          <p className="text-[11.5px] leading-relaxed text-white/70">{spec.fermeture}</p>
          {spec.horlogeMs && (
            <div className="mt-2">
              <Puce texte={`horloge — ${spec.horlogeMs} ms`} encre="#CCA700" />
            </div>
          )}
        </div>
      </div>

      <div className="mt-4">
        <div className="mb-2 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">Les régimes</div>
        <div className="flex flex-wrap gap-1.5">
          <Puce texte={LIBELLE_FRAPPE[spec.frappe]} encre={spec.frappe === 'ferme' ? '#F44747' : spec.frappe === 'filtre' ? '#5CCFE6' : spec.frappe === 'rafraichit' ? '#98C97A' : '#A0A1AA'} />
          <Puce texte={LIBELLE_DEFILEMENT[spec.defilement]} encre={spec.defilement === 'ferme' ? '#F44747' : spec.defilement === 'repositionne' ? '#5CCFE6' : '#A0A1AA'} />
          <Puce texte={spec.tapHorsCartes ? 'le tap hors cartes la referme' : 'le tap hors cartes l\u2019épargne'} encre={spec.tapHorsCartes ? '#F44747' : '#A0A1AA'} />
          {spec.echap ? (
            <Puce texte="Échap la referme — ORDRE_CLAVIER puis éphémères" encre="#CD7EE0" />
          ) : (
            <Puce texte="Échap ne l\u2019atteint jamais — ni interactive, ni éphémère" encre="#A0A1AA" />
          )}
          <Puce texte={spec.voile ? 'le voile modal la coupe' : 'le voile ne la coupe pas — vit avec le document'} encre={spec.voile ? '#CD7EE0' : '#A0A1AA'} />
          {spec.clavier ? (
            <Puce texte="PopupClavier — flèches, Enter, page" encre="#5CCFE6" />
          ) : (
            <Puce texte="carte passive — jamais le focus (PIEGE-27)" encre="#A0A1AA" />
          )}
          {spec.chrome && <Puce texte="chrome — vit avec le document" encre="#98C97A" />}
        </div>
      </div>

      <div className="mt-4 grid gap-2.5 rounded-xl border border-white/10 bg-white/[0.02] p-3 text-[11px] sm:grid-cols-2">
        <div className="flex gap-2"><span className="w-20 shrink-0 text-white/35">Vue</span><span className="font-mono text-white/70">{spec.vue}</span></div>
        <div className="flex gap-2"><span className="w-20 shrink-0 text-white/35">Satellite</span><span className="font-mono text-white/70">{spec.satellite}</span></div>
        <div className="flex gap-2"><span className="w-20 shrink-0 text-white/35">Données</span><span className="text-white/70">{spec.donnees}</span></div>
        <div className="flex gap-2"><span className="w-20 shrink-0 text-white/35">Ancre</span><span className="text-white/70">{spec.ancre}</span></div>
      </div>

      {spec.strings && spec.strings.length > 0 && (
        <div className="mt-3">
          <div className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">Les chaînes du terrain (strings.xml)</div>
          <div className="space-y-1">
            {spec.strings.map((s) => (
              <div key={s.fr} className="flex flex-wrap items-baseline gap-2 rounded-lg border border-white/[0.07] bg-white/[0.02] px-2.5 py-1.5">
                <span className="text-[11px] text-white/80">« {s.fr} »</span>
                <span className="text-[10px] text-white/30">FR</span>
                <span className="ml-auto text-[10px] text-white/45">“{s.en}” EN</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export function CataloguePopups({ machine }: { machine: MachinePopups }) {
  const [choisi, poserChoisi] = useState<GenreId>('TYPE_INFO');
  const [filtre, poserFiltre] = useState<Categorie | 'TOUT' | 'OUVRABLES'>('TOUT');

  const groupes = useMemo(() => {
    const lettres = (Object.keys(CATEGORIES) as Categorie[]).filter(
      (c) => POPUPS.some((p) => p.categorie === c),
    );
    return lettres.map((c) => ({
      c,
      items: POPUPS.filter(
        (p) => p.categorie === c && (filtre === 'TOUT' || filtre === c),
      ),
    }));
  }, [filtre]);

  const spec = POPUPS.find((p) => p.id === choisi)!;
  const compte = filtre === 'TOUT' ? POPUPS.length : POPUPS.filter((p) => p.categorie === filtre).length;

  return (
    <div className="space-y-4">
      {/* Le filtre */}
      <div className="flex flex-wrap items-center gap-1.5">
        {(['TOUT', ...(Object.keys(CATEGORIES) as Categorie[])] as const).map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => poserFiltre(f as Categorie | 'TOUT')}
            className={[
              'rounded-full border px-2.5 py-1 text-[10px] font-semibold transition-colors',
              filtre === f ? 'text-white' : 'border-white/10 text-white/45 hover:border-white/25 hover:text-white/70',
            ].join(' ')}
            style={filtre === f ? { borderColor: f === 'TOUT' ? 'rgba(255,255,255,0.4)' : `${ENCRE_CATEGORIE[f as Categorie]}99`, color: f === 'TOUT' ? undefined : ENCRE_CATEGORIE[f as Categorie], backgroundColor: f === 'TOUT' ? 'rgba(255,255,255,0.08)' : `${ENCRE_CATEGORIE[f as Categorie]}14` } : undefined}
          >
            {f === 'TOUT' ? `Tout — ${POPUPS.length}` : `${f} · ${CATEGORIES[f]}`}
          </button>
        ))}
        <span className="ml-auto text-[10px] text-white/30">{compte} genres</span>
      </div>

      {/* La grille */}
      <div className="space-y-3">
        {groupes.map(({ c, items }) =>
          items.length === 0 ? null : (
            <div key={c}>
              <div className="mb-1.5 flex items-center gap-2">
                <span className="rounded-md px-1.5 py-0.5 font-mono text-[10px] font-bold" style={{ backgroundColor: `${ENCRE_CATEGORIE[c]}1f`, color: ENCRE_CATEGORIE[c] }}>{c}</span>
                <span className="text-[11px] font-medium text-white/55">{CATEGORIES[c]}</span>
              </div>
              <div className="grid gap-1.5 sm:grid-cols-2 xl:grid-cols-3">
                {items.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => poserChoisi(p.id)}
                    className={[
                      'group rounded-xl border px-3 py-2 text-left transition-colors',
                      choisi === p.id
                        ? 'border-white/30 bg-white/[0.07]'
                        : 'border-white/10 bg-white/[0.02] hover:border-white/20 hover:bg-white/[0.05]',
                    ].join(' ')}
                  >
                    <div className="flex items-center gap-2">
                      <span className="text-[12px] font-medium text-white/85">{p.nomFr}</span>
                      {p.clavier && <Keyboard className="h-3 w-3 shrink-0 text-white/25" aria-label="clavier délégué" />}
                      {p.chrome && <Layers className="h-3 w-3 shrink-0 text-white/25" aria-label="chrome" />}
                    </div>
                    <div className="mt-0.5 flex items-center gap-1.5">
                      <span className="font-mono text-[9px] text-white/30">{p.constName}</span>
                      <span className="ml-auto flex gap-1">
                        {p.frappe === 'ferme' && <span className="rounded-sm px-1 text-[8px] font-bold" style={{ color: '#F44747', backgroundColor: '#F4474714' }}>frappe</span>}
                        {p.horlogeMs && <span className="rounded-sm px-1 text-[8px] font-bold" style={{ color: '#CCA700', backgroundColor: '#CCA70014' }}>horloge</span>}
                        {p.chrome && <span className="rounded-sm px-1 text-[8px] font-bold" style={{ color: '#98C97A', backgroundColor: '#98C97A14' }}>chrome</span>}
                        {p.voile && !p.chrome && <span className="rounded-sm px-1 text-[8px] font-bold" style={{ color: '#CD7EE0', backgroundColor: '#CD7EE014' }}>voile</span>}
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ),
        )}
      </div>

      {/* La fiche */}
      <Fiche spec={spec} machine={machine} />
    </div>
  );
}
