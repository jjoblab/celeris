'use client';

/**
 * Le simulateur — un éditeur Android de Celeris en miniature : le cadre,
 * la gouttière, le caret, les diagnostics, le chrome (fil d’Ariane, bande,
 * minimap, pouce, barre de touches), les cartes flottantes ancrées et le
 * voile de la feuille. Chaque geste passe par la machine (régimes exacts).
 */

import { useCallback, type CSSProperties, type KeyboardEvent as ReactKeyboardEvent } from 'react';
import {
  ArrowDown,
  Bell,
  BookOpen,
  CornerDownLeft,
  Eye,
  EyeOff,
  FileCode,
  GitBranch,
  Info,
  Keyboard,
  Layers,
  Lightbulb,
  Move,
  MousePointerClick,
  Palette,
  Play,
  Search,
  Square,
  TextCursorInput,
  TextSelect,
  Trash2,
  Undo2,
  Wand2,
  X,
} from 'lucide-react';
import type { GenreId } from '@/lib/popups';
import { POPUP_PAR_ID } from '@/lib/popups';
import { DIAGNOSTICS, FICHIER, GOUTTIERE, INLAY_HINTS } from '@/lib/machine';
import type { MachinePopups } from '@/lib/machine';
import { HAUT_LIGNE, LIGNES, LIGNES_VISIBLES } from '@/lib/machine';
import { ContenuCarte } from '@/components/popup/cartes';

const ENCRE_MOTCLE = '#FF8A65';
const ENCRE_TYPE = '#5CCFE6';
const ENCRE_CHAINE = '#98C97A';
const ENCRE_NOMBRE = '#D9A066';
const ENCRE_NOTE = '#E6C178';
const ENCRE_COMMENTAIRE = '#5C6773';
const ENCRE_TEXTE = '#CFD3DB';

const RE = /("(?:[^"\\]|\\.)*")|(\/\/.*)|(\b\d\w*\b)|(@\w+)|\b(public|private|protected|static|final|class|void|int|boolean|return|if|else|throw|new|this|null|true|false|super|extends|implements)\b|\b([A-Z][A-Za-z0-9_]*)\b/g;

function colorer(texte: string): { t: string; c?: string; i?: boolean }[] {
  const parties: { t: string; c?: string; i?: boolean }[] = [];
  let dernier = 0;
  let m: RegExpExecArray | null;
  RE.lastIndex = 0;
  while ((m = RE.exec(texte)) !== null) {
    if (m.index > dernier) parties.push({ t: texte.slice(dernier, m.index) });
    const [t, chaine, comment, nombre, annotation, motcle, type] = m;
    if (chaine) parties.push({ t, c: ENCRE_CHAINE });
    else if (comment) parties.push({ t, c: ENCRE_COMMENTAIRE, i: true });
    else if (nombre) parties.push({ t, c: ENCRE_NOMBRE });
    else if (annotation) parties.push({ t, c: ENCRE_NOTE });
    else if (motcle) parties.push({ t, c: ENCRE_MOTCLE });
    else if (type) parties.push({ t, c: ENCRE_TYPE });
    dernier = m.index + t.length;
  }
  if (dernier < texte.length) parties.push({ t: texte.slice(dernier) });
  return parties;
}

function chemin(caret: number): string {
  const base = 'jo.banque › Compte';
  if (caret >= 6 && caret <= 9) return `${base} › Compte(titulaire, soldeInitial)`;
  if (caret >= 11 && caret <= 14) return `${base} › consulter()`;
  if (caret >= 16 && caret <= 21) return `${base} › retirer(montant)`;
  if (caret >= 23 && caret <= 25) return `${base} › deposer(montant)`;
  if (caret >= 27 && caret <= 30) return `${base} › toString()`;
  return base;
}

function position(genre: GenreId, m: MachinePopups): CSSProperties {
  const visuel = Math.max(0, Math.min(m.caretLigne - m.fenetre, LIGNES_VISIBLES - 1));
  const dessous: CSSProperties = { top: (visuel + 1) * HAUT_LIGNE + 4, left: GOUTTIERE + 4 };
  const dessus: CSSProperties = { top: visuel * HAUT_LIGNE - 4, left: GOUTTIERE + 4, transform: 'translateY(-100%)' };
  // L'ancre au point place la bulle AU POINT DU GESTE — trois lignes au-dessus
  // du caret, retournée vers le haut : elle ne recouvre jamais une carte
  // ancrée au curseur, elles vivent à des hauteurs distinctes.
  const auPoint: CSSProperties =
    visuel >= 4
      ? { top: (visuel - 3) * HAUT_LIGNE - 4, left: GOUTTIERE + 118, transform: 'translateY(-100%)' }
      : { top: (visuel + 1) * HAUT_LIGNE + 4, left: GOUTTIERE + 118 };
  switch (genre) {
    case 'COMPLETION_LIST':
      return { top: (visuel + 1) * HAUT_LIGNE + 4, left: GOUTTIERE };
    case 'COMPLETION_DOC':
      return { top: (visuel + 1) * HAUT_LIGNE + 4, left: GOUTTIERE + 194 };
    case 'SIGNATURE_HELP':
      return visuel >= 2 ? dessus : dessous;
    case 'HOVER':
    case 'DIAGNOSTIC_TOOLTIP':
      return auPoint;
    case 'NOTIFICATION':
      return { bottom: 8, right: 8 };
    case 'TYPE_INFO':
    case 'REFERENCES':
    case 'HIERARCHY':
    case 'EXTERNAL_DOC':
    case 'GABARITS':
    case 'SAFE_DELETE':
    case 'SAFE_MOVE':
    case 'SUPPRESS_INSPECTION':
    case 'EXCEPTION_DETAILS':
    case 'COLOR_PICKER':
    case 'INTENTIONS_MENU':
      return visuel <= 8 ? dessous : dessus;
    default:
      return dessous;
  }
}

/* — Les petites pilules de commande — */
function Pilule({ onClick, children, actif }: { onClick: () => void; children: React.ReactNode; actif?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        'inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-[11px] font-medium transition-colors',
        actif
          ? 'border-[#E6C178]/60 bg-[#E6C178]/15 text-[#E6C178]'
          : 'border-white/10 bg-white/[0.04] text-white/75 hover:border-white/25 hover:bg-white/10',
      ].join(' ')}
    >
      {children}
    </button>
  );
}

function Groupe({ titre, children }: { titre: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">{titre}</div>
      <div className="flex flex-wrap gap-1.5">{children}</div>
    </div>
  );
}

/* — La carte flottante générique — */
function CarteFlottante({ genre, machine, resteMs }: { genre: GenreId; machine: MachinePopups; resteMs: number }) {
  const style = position(genre, machine);
  return (
    <div
      style={{ ...style, boxShadow: '0 18px 50px rgba(0,0,0,0.55)' }}
      className="animate-in fade-in slide-in-from-bottom-1 pointer-events-auto absolute z-30 rounded-xl border border-white/10 bg-[#14161c]/95 px-2.5 py-2 backdrop-blur-md duration-150"
    >
      <ContenuCarte genre={genre} machine={machine} resteMs={resteMs} />
    </div>
  );
}

/* — Le simulateur complet — */
export function SimulateurEditeur({ machine }: { machine: MachinePopups }) {
  const visuel = Math.max(0, Math.min(machine.caretLigne - machine.fenetre, LIGNES_VISIBLES - 1));

  const surToucheClavier = useCallback(
    (e: ReactKeyboardEvent<HTMLDivElement>) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        machine.echapperCarte();
        return;
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        machine.validerClavier();
        return;
      }
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        const clavierOuvert =
          machine.ouverts.has('COMPLETION_LIST') ||
          machine.ouverts.has('REFERENCES') ||
          machine.ouverts.has('HIERARCHY');
        if (clavierOuvert) machine.bougerIndexClavier(e.key === 'ArrowDown' ? 1 : -1);
        else machine.defilerVers(e.key === 'ArrowDown' ? 1 : -1);
      }
    },
    [machine],
  );

  const lignes = FICHIER.slice(machine.fenetre, machine.fenetre + LIGNES_VISIBLES);

  return (
    <div className="flex w-full flex-col items-center">
      <div className="w-full max-w-[440px] overflow-x-auto">
        <div
          tabIndex={0}
          onKeyDown={surToucheClavier}
          className="relative mx-auto w-[400px] shrink-0 rounded-2xl border border-white/10 bg-[#0d0e12] shadow-2xl outline-none focus-visible:ring-1 focus-visible:ring-[#E6C178]/50"
          aria-label="Simulateur de l’éditeur Celeris"
        >
          {/* La mini toolbar du cadre */}
          <div className="flex h-[30px] items-center gap-2 border-b border-white/10 px-3">
            <FileCode className="h-3 w-3 text-white/40" aria-hidden />
            <span className="font-mono text-[10px] text-white/45">Compte.java</span>
            <span className="ml-auto flex items-center gap-1">
              <Undo2 className="h-3 w-3 text-white/40" aria-hidden />
              <button
                type="button"
                aria-label="Débordement de la barre d’application"
                onClick={() => machine.ouvrirGenre('TOOLBAR_OVERFLOW')}
                className="rounded px-1.5 py-0.5 text-white/60 hover:bg-white/10 hover:text-white"
              >
                <Layers className="h-3.5 w-3.5" aria-hidden />
              </button>
            </span>
          </div>

          {/* La zone de code + la couche porte-popups */}
          <div className="relative" style={{ height: LIGNES_VISIBLES * HAUT_LIGNE + 10 }}>
            {/* Le fil d’Ariane — coin haut-gauche, suit le curseur */}
            <div className="pointer-events-none absolute left-11 top-1 z-10 select-none text-[9px] text-white/40">
              {chemin(machine.caretLigne)}
            </div>

            {/* Les lignes */}
            <div className="pt-[4px]">
              {lignes.map((texte, i) => {
                const n = machine.fenetre + i;
                const diags = DIAGNOSTICS.filter((d) => d.ligne === n);
                return (
                  <div
                    key={n}
                    role="button"
                    tabIndex={-1}
                    onClick={() => machine.poserCaret(n)}
                    onDoubleClick={() =>
                      n === 4
                        ? machine.ouvrirGenre('COLOR_PICKER')
                        : machine.ouvrirGenre('SELECTION_BAR')
                    }
                    className="flex cursor-pointer font-mono text-[11px] leading-[22px] hover:bg-white/[0.04]"
                    style={{ height: HAUT_LIGNE }}
                  >
                    <div className="flex w-11 shrink-0 items-center justify-end gap-1 pr-1.5 text-right text-[9px] text-white/30">
                      {machine.chrome.ampoule && diags.length > 0 && n === machine.caretLigne ? (
                        <button
                          type="button"
                          aria-label="Ampoule d’intentions — le menu s’ouvre"
                          onClick={(e) => {
                            e.stopPropagation();
                            machine.ouvrirGenre('INTENTIONS_MENU', "l’ampoule de la gouttière l’ouvre — les intentions de la ligne du caret");
                          }}
                          className="rounded px-0.5 hover:bg-white/10"
                        >
                          <Lightbulb className="h-3 w-3" style={{ color: '#FFCB6B' }} aria-hidden />
                        </button>
                      ) : (
                        diags.map((d) => (
                          <span key={d.regle} style={{ color: d.gravite === 'erreur' ? '#F44747' : '#CCA700' }}>
                            {d.marqueur}
                          </span>
                        ))
                      )}
                      {n === 20 && <span className="h-1.5 w-1.5 rounded-full bg-[#E6C178]" aria-label="point d’arrêt" />}
                      <span>{n + 1}</span>
                    </div>
                    <div
                      className={
                        diags.length > 0
                          ? 'decoration-wavy underline decoration-1 underline-offset-2'
                          : ''
                      }
                      style={diags.length > 0 ? { textDecorationColor: diags[0].gravite === 'erreur' ? '#F44747' : '#CCA700' } : undefined}
                    >
                      {colorer(texte).map((p, j) => (
                        <span key={j} style={{ color: p.c }} className={p.i ? 'italic' : undefined}>
                          {p.t}
                        </span>
                      ))}
                      {machine.chrome.hints && (() => {
                        const hint = INLAY_HINTS.find((h) => h.ligne === n);
                        return hint ? (
                          <span
                            className="ml-2 select-none italic"
                            style={{ color: 'rgba(255,255,255,0.42)' }}
                            aria-label="inlay hint — le type du var"
                          >
                            {hint.texte}
                          </span>
                        ) : null;
                      })()}
                      {texte === '' ? '\u00A0' : ''}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Le caret */}
            <div
              className="pointer-events-none absolute animate-pulse rounded-full"
              style={{ left: GOUTTIERE + 8, top: visuel * HAUT_LIGNE + 5, width: 2, height: 14, backgroundColor: '#E6C178' }}
            />

            {/* Le chrome du bord droit */}
            {machine.chrome.bande && (
              <div className="absolute right-[14px] top-[4px] flex w-[7px] flex-col items-center gap-[3px]" style={{ height: LIGNES_VISIBLES * HAUT_LIGNE }}>
                {DIAGNOSTICS.map((d) => {
                  const v = d.ligne - machine.fenetre;
                  if (v < 0 || v >= LIGNES_VISIBLES) return null;
                  return (
                    <button
                      key={d.regle}
                      type="button"
                      aria-label={`diagnostic ligne ${d.ligne + 1}`}
                      className="w-[7px] rounded-[2px]"
                      style={{ position: 'absolute', top: v * HAUT_LIGNE + 6, height: 10, backgroundColor: d.gravite === 'erreur' ? '#F44747' : '#CCA700' }}
                      onClick={() => machine.sauter(d.ligne)}
                    />
                  );
                })}
              </div>
            )}
            {machine.chrome.minimap && (
              <div className="absolute right-[24px] top-[4px] h-full w-[34px] select-none" aria-hidden>
                {FICHIER.map((t, n) => (
                  <div
                    key={n}
                    className="mx-auto w-[24px] rounded-[1px]"
                    style={{
                      height: 3,
                      marginTop: n === 0 ? 0 : 1,
                      backgroundColor:
                        t.includes('class') ? '#E6C178' : t.trim().startsWith('public') || t.trim().startsWith('private') && t.includes('(') ? '#5CCFE6' : t === '' ? 'transparent' : 'rgba(255,255,255,0.22)',
                      opacity: machine.fenetre <= n && n < machine.fenetre + LIGNES_VISIBLES ? 0.9 : 0.28,
                    }}
                  />
                ))}
                <div
                  className="pointer-events-none absolute left-0 w-full rounded-sm border"
                  style={{
                    top: (machine.fenetre / LIGNES) * 100 + '%',
                    height: (LIGNES_VISIBLES / LIGNES) * 100 + '%',
                    borderColor: '#E6C17899',
                    backgroundColor: '#E6C17814',
                  }}
                />
              </div>
            )}
            {machine.chrome.pouce && (
              <div className="absolute right-1 top-[4px] h-full w-[10px] select-none" aria-hidden>
                <div
                  className="w-[5px] rounded-full bg-white/35"
                  style={{
                    position: 'absolute',
                    left: 2,
                    height: (LIGNES_VISIBLES / LIGNES) * 100 + '%',
                    top: (machine.fenetre / Math.max(LIGNES - LIGNES_VISIBLES, 1)) * (1 - LIGNES_VISIBLES / LIGNES) * 100 + '%',
                  }}
                />
              </div>
            )}

            {/* La couche des cartes flottantes */}
            <div className="pointer-events-none absolute inset-0">
              {Array.from(machine.ouverts).map((g) => {
                const spec = POPUP_PAR_ID[g];
                if (!spec || spec.chrome) return null;
                if (g === 'SEARCH_BAR' || g === 'SELECTION_BAR' || g === 'SELECTION_OVERFLOW' || g === 'TOOLBAR_OVERFLOW' || g === 'NOTIFICATION') return null;
                return <CarteFlottante key={g} genre={g} machine={machine} resteMs={machine.resteMs} />;
              })}

              {/* La barre de sélection — pilule au-dessus de la sélection */}
              {machine.ouverts.has('SELECTION_BAR') && (
                <div
                  style={{
                    top: visuel * HAUT_LIGNE - 6,
                    left: '50%',
                    transform: 'translate(-50%, -100%)',
                    boxShadow: '0 14px 40px rgba(0,0,0,0.5)',
                  }}
                  className="animate-in fade-in slide-in-from-bottom-1 pointer-events-auto absolute z-30 flex items-center gap-1 rounded-full border border-white/10 bg-[#14161c]/95 px-2 py-1 backdrop-blur-md duration-150"
                >
                  <span className="rounded-full px-2 py-0.5 text-[10px] text-white/75 hover:bg-white/10">Couper</span>
                  <span className="rounded-full px-2 py-0.5 text-[10px] text-white/75 hover:bg-white/10">Copier</span>
                  <span className="rounded-full px-2 py-0.5 text-[10px] text-white/75 hover:bg-white/10">Coller</span>
                  <span className="rounded-full px-2 py-0.5 text-[10px] text-white/75 hover:bg-white/10">Tout</span>
                  <span className="h-3 w-px bg-white/15" />
                  <button
                    type="button"
                    aria-label="Survoler la documentation de la sélection"
                    onClick={() => machine.poserVariantSurvol('doc')}
                    className="rounded-full px-1.5 py-0.5 text-[10px] font-semibold hover:bg-white/10"
                    style={{ color: '#E6C178' }}
                  >
                    i
                  </button>
                  <button
                    type="button"
                    aria-label="Menu de débordement de la sélection"
                    onClick={() => machine.ouvrirGenre('SELECTION_OVERFLOW')}
                    className="rounded-full px-1.5 py-0.5 text-[10px] text-white/75 hover:bg-white/10"
                  >
                    ⋯
                  </button>
                </div>
              )}

              {/* Le débordement de la barre de sélection — sous sa barre */}
              {machine.ouverts.has('SELECTION_OVERFLOW') && (
                <div
                  style={{
                    top: visuel * HAUT_LIGNE + 16,
                    left: '50%',
                    transform: 'translateX(-50%)',
                    boxShadow: '0 18px 50px rgba(0,0,0,0.55)',
                  }}
                  className="animate-in fade-in pointer-events-auto absolute z-30 rounded-xl border border-white/10 bg-[#14161c]/95 px-2.5 py-2 backdrop-blur-md duration-150"
                >
                  <div className="w-[168px]">
                    <div className="mt-0 select-none text-[9px] font-semibold uppercase tracking-[0.14em] text-white/40">Plus</div>
                    <div
                      role="button"
                      onClick={() => {
                        machine.fermerGenre('SELECTION_OVERFLOW');
                        machine.poserAnnonce({ message: 'La ligne se duplique — un geste la défait', action: 'Défaire' });
                      }}
                      className="cursor-pointer rounded-md px-1.5 py-1 text-[12px] font-medium hover:bg-white/10"
                    >
                      Dupliquer la ligne
                    </div>
                    <div className="mt-1 border-t border-white/10 pt-1 text-[10px] leading-relaxed text-white/45">
                      le menu ne survit jamais à sa barre — cascade
                    </div>
                  </div>
                </div>
              )}

              {/* Le débordement de la toolbar — pendu sous la puce */}
              {machine.ouverts.has('TOOLBAR_OVERFLOW') && (
                <div
                  style={{ top: 4, right: 6, boxShadow: '0 18px 50px rgba(0,0,0,0.55)' }}
                  className="animate-in fade-in pointer-events-auto absolute z-30 rounded-xl border border-white/10 bg-[#14161c]/95 px-2.5 py-2 backdrop-blur-md duration-150"
                >
                  <ContenuCarte genre="TOOLBAR_OVERFLOW" machine={machine} resteMs={machine.resteMs} />
                </div>
              )}

              {/* La notification — coin bas-droit, l’horloge la referme */}
              {machine.ouverts.has('NOTIFICATION') && machine.annonce && (
                <div
                  style={{ bottom: 8, right: 8, boxShadow: '0 18px 50px rgba(0,0,0,0.55)' }}
                  className="animate-in fade-in slide-in-from-bottom-2 pointer-events-auto absolute z-50 rounded-xl border border-white/10 bg-[#14161c]/95 px-2.5 py-2 backdrop-blur-md duration-150"
                  onClick={() => machine.prendreAnnonce()}
                  role="button"
                  aria-label="Annonce — le tap la prend"
                >
                  <ContenuCarte genre="NOTIFICATION" machine={machine} resteMs={machine.resteMs} />
                </div>
              )}
            </div>

            {/* Le voile modal de la feuille de recherche */}
            {machine.voile && (
              <>
                <div className="absolute inset-0 z-40 bg-black/60" onClick={() => machine.fermerVoile()} aria-hidden />
                <div className="absolute inset-x-0 bottom-0 z-[41] rounded-t-2xl border-t border-white/15 bg-[#15171d] px-4 pb-4 pt-3">
                  <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-white/20" aria-hidden />
                  <div className="flex items-center gap-2">
                    <Search className="h-4 w-4 text-white/40" aria-hidden />
                    <input
                      defaultValue="solde"
                      className="w-full bg-transparent font-mono text-[12px] text-white/85 outline-none"
                      aria-label="Recherche dans le fichier"
                    />
                    <button
                      type="button"
                      onClick={() => machine.fermerVoile()}
                      aria-label="Fermer la recherche"
                      className="rounded p-1 text-white/50 hover:bg-white/10 hover:text-white"
                    >
                      <X className="h-4 w-4" aria-hidden />
                    </button>
                  </div>
                  <div className="mt-2 text-[10px] text-white/45">
                    6 occurrences de « solde » — la feuille du bas (ADR-018), le genre SEARCH_BAR vit en état seul
                  </div>
                </div>
              </>
            )}
          </div>

          {/* La barre de touches de code — au-dessus du clavier */}
          {machine.chrome.touches && (
            <div className="flex h-[34px] items-center justify-center gap-1 border-t border-white/10 bg-[#101218] px-2">
              {[
                ['Tab', () => machine.poserAnnonce({ message: 'Tab accepte la suggestion fantôme', action: 'Voir' })],
                ['Commenter', () => machine.defilerVers(0)],
                ['Ligne↑', () => machine.defilerVers(-1)],
                ['Ligne↓', () => machine.defilerVers(1)],
                ['Dupliquer', () => machine.poserAnnonce({ message: 'La ligne se duplique — un geste la défait', action: 'Défaire' })],
              ].map(([libelle, action]) => (
                <button
                  key={libelle as string}
                  type="button"
                  onClick={action as () => void}
                  className="rounded-md border border-white/10 bg-white/[0.04] px-2.5 py-1 text-[10px] text-white/65 hover:bg-white/10"
                >
                  {libelle as string}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* — Les commandes du simulateur — */}
      <div className="mt-5 w-full max-w-[440px] space-y-4">
        <Groupe titre="Le menu de l’éditeur (CatalogueActions)">
          <Pilule onClick={() => machine.ouvrirGenre('COMPLETION_LIST', 'la frappe propose — 8 candidats au plus')}>
            <TextCursorInput className="h-3 w-3" aria-hidden /> Compléter le mot
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('INTENTIONS_MENU')}>
            <Square className="h-3 w-3" aria-hidden /> Intentions au curseur
          </Pilule>
          <Pilule onClick={() => machine.poserVariantSurvol('doc')}>
            <Eye className="h-3 w-3" aria-hidden /> Survoler le symbole
          </Pilule>
          <Pilule onClick={() => machine.poserVariantSurvol('peek')}>
            <Search className="h-3 w-3" aria-hidden /> Aperçu de définition
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('REFERENCES')}>
            <Search className="h-3 w-3" aria-hidden /> Rechercher les usages
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('TYPE_INFO')}>
            <Info className="h-3 w-3" aria-hidden /> Info de type au curseur
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('HIERARCHY')}>
            <GitBranch className="h-3 w-3" aria-hidden /> Hiérarchie de types ou d&apos;appels
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('EXTERNAL_DOC')}>
            <BookOpen className="h-3 w-3" aria-hidden /> Documentation externe
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('GABARITS')}>
            <Wand2 className="h-3 w-3" aria-hidden /> Générer du code
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('SAFE_DELETE')}>
            <Trash2 className="h-3 w-3" aria-hidden /> Suppression sûre
          </Pilule>
          <Pilule onClick={() => { machine.sauter(23); machine.ouvrirGenre('SAFE_MOVE'); }}>
            <Move className="h-3 w-3" aria-hidden /> Déplacer dans
          </Pilule>
          <Pilule onClick={() => { machine.sauter(12); machine.ouvrirGenre('SUPPRESS_INSPECTION'); }}>
            <EyeOff className="h-3 w-3" aria-hidden /> Masquer l&apos;inspection
          </Pilule>
          <Pilule
            onClick={() => {
              machine.poserAnnonce({ message: "L’exécution simulée échoue — la trace suit le plan du fichier", action: 'Voir la trace' });
              machine.ouvrirGenre('EXCEPTION_DETAILS');
            }}
          >
            <Play className="h-3 w-3" aria-hidden /> Exécuter le fichier (simulé)
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('TOOLBAR_OVERFLOW')}>
            <Layers className="h-3 w-3" aria-hidden /> Plus d&apos;actions
          </Pilule>
          <Pilule onClick={() => machine.ouvrirVoile()}>
            <Search className="h-3 w-3" aria-hidden /> Rechercher (voile)
          </Pilule>
        </Groupe>

        <Groupe titre="Les gestes du doigt">
          <Pilule onClick={() => { machine.sauter(4); machine.ouvrirGenre('COLOR_PICKER'); }}>
            <Palette className="h-3 w-3" aria-hidden /> Double-tap littéral couleur
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('SELECTION_BAR')}>
            <TextSelect className="h-3 w-3" aria-hidden /> Sélection d&apos;un mot
          </Pilule>
          <Pilule onClick={() => machine.ouvrirGenre('SIGNATURE_HELP')}>
            <CornerDownLeft className="h-3 w-3" aria-hidden /> Aide aux paramètres
          </Pilule>
          <Pilule onClick={() => { machine.sauter(12); machine.ouvrirGenre('DIAGNOSTIC_TOOLTIP'); }}>
            <Info className="h-3 w-3" aria-hidden /> Info-bulle de diagnostic
          </Pilule>
          <Pilule onClick={() => machine.poserAnnonce({ message: '3 correctifs proposés par le lint de la ligne 13', action: 'Corriger' })}>
            <Bell className="h-3 w-3" aria-hidden /> Annonce
          </Pilule>
        </Groupe>

        <Groupe titre="Les régimes de la machine (à essayer)">
          <Pilule onClick={() => machine.frapperLettre()}>
            <Keyboard className="h-3 w-3" aria-hidden /> Frapper une lettre
          </Pilule>
          <Pilule onClick={() => machine.defilerVers(2)}>
            <ArrowDown className="h-3 w-3" aria-hidden /> Défiler ↓
          </Pilule>
          <Pilule onClick={() => machine.taperHorsCartes()}>
            <MousePointerClick className="h-3 w-3" aria-hidden /> Tap hors cartes
          </Pilule>
          <Pilule onClick={() => machine.echapperCarte()}>
            <X className="h-3 w-3" aria-hidden /> Échapper
          </Pilule>
          <Pilule onClick={() => machine.ouvrirVoile()}>
            <Layers className="h-3 w-3" aria-hidden /> Voile modal
          </Pilule>
          <Pilule onClick={() => machine.fermerTout()}>Tout fermer</Pilule>
        </Groupe>

        <Groupe titre="Le chrome et la langue">
          <Pilule actif={machine.chrome.bande} onClick={() => machine.basculerChrome('bande')}>Bande d&apos;erreurs</Pilule>
          <Pilule actif={machine.chrome.minimap} onClick={() => machine.basculerChrome('minimap')}>Minimap</Pilule>
          <Pilule actif={machine.chrome.pouce} onClick={() => machine.basculerChrome('pouce')}>Pouce</Pilule>
          <Pilule actif={machine.chrome.touches} onClick={() => machine.basculerChrome('touches')}>Barre de touches</Pilule>
          <Pilule actif={machine.chrome.ampoule} onClick={() => machine.basculerChrome('ampoule')}>
            <Lightbulb className="h-3 w-3" aria-hidden /> Ampoule d&apos;intentions
          </Pilule>
          <Pilule actif={machine.chrome.hints} onClick={() => machine.basculerChrome('hints')}>Inlay hints</Pilule>
          <Pilule actif onClick={() => machine.basculerLangue()}>{machine.langue === 'fr' ? 'FR → EN' : 'EN → FR'}</Pilule>
        </Groupe>

        <p className="text-[10px] leading-relaxed text-white/35">
          Cliquez une ligne du code : le caret s&apos;y pose et le tap hors cartes referme ce qui cède.
          Double-tapez : la ligne 5 ouvre le sélecteur de couleur, les autres la pilule de sélection.
          Le cadre prend le focus (Tab) — les flèches délèguent aux cartes clavier, Enter prend la
          rangée, Échap referme. L&apos;AMPOULE JAUNE vit dans la gouttière à la ligne du caret qui porte
          un diagnostic — comme dans l&apos;IDE de référence, elle remplace le marqueur et son tap ouvre
          le menu d&apos;intentions. Les INLAY HINTS se lisent en fin de ligne utile, à l&apos;encre muette
          italique — le type du « var », jamais dans le flux : le texte ne se décale pas.
        </p>

        {/* Le journal des régimes */}
        <div>
          <div className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/35">Le journal des régimes</div>
          <div className="max-h-44 space-y-1 overflow-y-auto rounded-xl border border-white/10 bg-white/[0.03] p-2.5">
            {machine.journal.length === 0 && (
              <div className="text-[11px] text-white/35">Rien encore — déclenchez une popup, puis essayez les régimes.</div>
            )}
            {machine.journal.map((e) => (
              <div key={e.id} className="flex items-start gap-2 text-[11px] leading-relaxed">
                <span
                  className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full"
                  style={{
                    backgroundColor:
                      e.genre === 'action' ? '#E6C178' :
                      e.genre === 'regime' ? '#5CCFE6' :
                      e.genre === 'voile' ? '#CD7EE0' :
                      e.genre === 'saut' ? '#98C97A' :
                      e.genre === 'horloge' ? '#CCA700' : '#A0A1AA',
                  }}
                  aria-hidden
                />
                <span className="text-white/70">{e.texte}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
