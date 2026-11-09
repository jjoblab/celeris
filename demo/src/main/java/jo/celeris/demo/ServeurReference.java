package jo.celeris.demo;

import java.util.HashMap;
import java.util.Map;

import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurvolLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.lsp.protocole.ServeurEmbarque;

import org.jspecify.annotations.Nullable;

/**
 * Le serveur de langage de référence du terrain (M7.5, §8) : le
 * service de documents du DOMAINE (ADR-015) — la démonstration y
 * branche ses tables par le SPI du module, le protocole reste à la
 * frontière. Il suit la synchronisation complète (didOpen, didChange,
 * didClose) et répond à l'aide aux paramètres (B-07) et au survol
 * (B-09) : l'appel ouvert sous la position sert sa signature connue,
 * le symbole survolé sert sa documentation — les tables du terrain,
 * servies par le protocole, pas par l'appel direct.
 */
public final class ServeurReference implements ServeurEmbarque.Documents {

    private final Map<String, String> documents = new HashMap<>();

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        documents.put(uri, texte);
    }

    @Override
    public void changer(String uri, int version, String texte) {
        documents.put(uri, texte);
    }

    @Override
    public void fermer(String uri) {
        documents.remove(uri);
    }

    /**
     * L'aide aux paramètres (B-07) : l'appel ouvert sous la position
     * sert sa signature — la table du terrain, la forme du protocole.
     */
    @Override
    public @Nullable SignatureLsp signature(String uri, PositionLsp position) {
        Document document = documentSuivi(uri);
        if (document == null) {
            return null;
        }
        SignatureAideModel modele = PopupCoach.signatureFor(document,
                PositionsLsp.offset(document, position));
        if (modele == null) {
            return null;
        }
        return SignatureLsp.de(
                java.util.List.of(new SignatureLsp.Entree(modele.libelle(),
                        parametresDuLibelle(modele.libelle()), null)),
                0, modele.parametreCourant());
    }

    /**
     * Le survol (B-09) : la documentation du symbole à la position —
     * le contenu balisé markdown, la forme du protocole.
     */
    @Override
    public @Nullable SurvolLsp survol(String uri, PositionLsp position) {
        Document document = documentSuivi(uri);
        if (document == null) {
            return null;
        }
        String symbole = PopupCoach.symboleA(document,
                PositionsLsp.offset(document, position));
        String documentation = symbole == null ? null
                : PopupCoach.documentationBrute(symbole);
        if (documentation == null) {
            return null;
        }
        return new SurvolLsp(documentation, "markdown", null);
    }

    /** Le document suivi d'une demande — null quand l'uri n'est pas suivie. */
    private @Nullable Document documentSuivi(String uri) {
        String texte = documents.get(uri);
        return texte == null ? null
                : TextSession.of(texte, () -> 0L).document();
    }

    /** Les paramètres d'un libellé de signature — les noms, dans l'ordre des virgules. */
    private static java.util.List<SignatureLsp.Parametre> parametresDuLibelle(
            String libelle) {
        java.util.List<SignatureLsp.Parametre> noms = new java.util.ArrayList<>();
        int ouverture = libelle.indexOf('(');
        int fermeture = libelle.lastIndexOf(')');
        if (ouverture < 0 || fermeture <= ouverture) {
            return noms;
        }
        for (String morceau : libelle.substring(ouverture + 1, fermeture)
                .split(",")) {
            String nom = morceau.strip();
            int espace = nom.lastIndexOf(' ');
            noms.add(new SignatureLsp.Parametre(
                    espace < 0 ? nom : nom.substring(espace + 1), null));
        }
        return noms;
    }
}
