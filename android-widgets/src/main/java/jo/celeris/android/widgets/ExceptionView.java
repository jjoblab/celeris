package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La carte des détails d'exception (catégorie E de l'inventaire —
 * « Exception Details », la surface dormait, la trace se réveille) :
 * le type de l'exception en rangée titre à l'encre d'erreur (le
 * marqueur « × » de l'info-bulle), le message au corps, puis les
 * cadres de la pile — le cadre CONNU du fichier ouvert se prend et
 * son saut court, l'étranger vit muet à l'encre muette (il ne saute
 * pas, il ne se devine pas). PIEGE-27 : jamais de focus — la carte
 * vit le régime de la consultation : le doigt seul y choisit,
 * l'échappement la referme.
 */
public final class ExceptionView extends TextRowsPopup {

    /** Un cadre de la pile : son libellé et son saut — null quand le
     *  cadre vit hors du document ouvert (le lieu étranger ne se
     *  devine jamais). */
    public record Cadre(String libelle, @Nullable Runnable saut) {
    }

    private List<Cadre> cadres = List.of();
    /** Le type de l'exception — l'en-tête titre, null pour aucun. */
    private @Nullable String type;
    /** Le message de l'exception — null pour aucun. */
    private @Nullable String message;

    public ExceptionView(Context context, PopupPalette palette) {
        super(context, palette);
        setOnPick(this::prendre);
    }

    /**
     * Pose l'exception : son type en rangée titre à l'encre d'erreur,
     * son message au corps — la pile suit par
     * {@link #setCadres(List)}.
     *
     * @param type    le type de l'exception (par exemple
     *                {@code java.lang.Exception})
     * @param message le message de l'exception, ses lignes comprises
     */
    public void setException(@Nullable String type, @Nullable String message) {
        this.type = type;
        this.message = message;
        rebatirRangees();
    }

    /**
     * Pose les cadres de la pile — la trace suit le message, chacun
     * sa rangée : le prenant à l'encre du panneau, l'étranger à
     * l'encre muette.
     *
     * @param cadres la pile, dans l'ordre de la trace
     */
    public void setCadres(List<Cadre> cadres) {
        this.cadres = List.copyOf(cadres);
        rebatirRangees();
    }

    /** Le cadre d'index donné, null s'il n'existe pas. */
    public @Nullable Cadre cadreAt(int index) {
        return index >= 0 && index < cadres.size() ? cadres.get(index) : null;
    }

    /** Le nombre de cadres posés — l'hôte y compte sa pile. */
    public int nombre() {
        return cadres.size();
    }

    /** Rebâtit les rangées : le marqueur et le type, le message, puis
     *  la pile — le premier cadre vit derrière un filet. */
    private void rebatirRangees() {
        List<Row> rangees = new ArrayList<>(cadres.size() + 2);
        if (type != null && !type.isEmpty()) {
            rangees.add(Row.tier("× " + type, palette.error(), Tier.TITRE));
        }
        if (message != null && !message.isEmpty()) {
            for (String ligne : message.split("\n", -1)) {
                rangees.add(Row.of(ligne));
            }
        }
        boolean premier = true;
        for (Cadre cadre : cadres) {
            Row rangee = Row.of(cadre.libelle(),
                    cadre.saut() == null ? palette.muted() : palette.text());
            rangees.add(premier ? Row.apresFilet(rangee) : rangee);
            premier = false;
        }
        setRows(rangees);
    }

    /** L'appui d'une rangée : le type et le message ne se prennent
     *  pas — seul le cadre sous le doigt saute. */
    private void prendre(int rang) {
        int index = rang - decalage();
        Cadre cadre = cadreAt(index);
        if (cadre != null && cadre.saut() != null) {
            cadre.saut().run();
        }
    }

    /** Le décalage des cadres — le type et le message éventuels
     *  comptent leurs rangées. */
    private int decalage() {
        int rangées = 0;
        if (type != null && !type.isEmpty()) {
            rangées++;
        }
        if (message != null && !message.isEmpty()) {
            rangées += message.split("\n", -1).length;
        }
        return rangées;
    }
}
