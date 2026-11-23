package jo.celeris.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.itsaky.androidide.treesitter.c.TSLanguageC;
import com.itsaky.androidide.treesitter.cpp.TSLanguageCpp;
import com.itsaky.androidide.treesitter.java.TSLanguageJava;
import com.itsaky.androidide.treesitter.json.TSLanguageJson;
import com.itsaky.androidide.treesitter.kotlin.TSLanguageKotlin;
import com.itsaky.androidide.treesitter.python.TSLanguagePython;
import com.itsaky.androidide.treesitter.xml.TSLanguageXml;

import jo.celeris.core.syntax.treesitter.TreeSitterGrammars;

/**
 * Les langues tree-sitter de l'EXEMPLE (ADR-0033 — le modèle hôte de
 * l'éditeur de référence) : chaque grammaire vivrait PAR DÉPENDANCE
 * — l'exemple embarque les sept qu'il veut servir (json, python,
 * java, kotlin, xml, c, cpp), chacune avec SA requête de
 * surbrillance lue des assets ({@code requetes/<langue>.scm}). Un
 * hôte qui n'en veut qu'une retire la dépendance et l'inscription :
 * la langue disparait, rien d'autre ne change.
 */
public final class NativeLanguages {

    private NativeLanguages() {
    }

    /** Inscrit les sept grammaires de l'exemple avec leurs requêtes —
     *  idempotent (les réinscriptions se remplacent). */
    public static void inscrire() {
        grammaire("json", TSLanguageJson::getInstance);
        grammaire("python", TSLanguagePython::getInstance);
        grammaire("java", TSLanguageJava::getInstance);
        grammaire("kotlin", TSLanguageKotlin::getInstance);
        grammaire("xml", TSLanguageXml::getInstance);
        grammaire("c", TSLanguageC::getInstance);
        grammaire("cpp", TSLanguageCpp::getInstance);
    }

    /** La grammaire d'une langue : l'inscription l'attache à SA
     *  requête lue des assets de l'exemple. */
    private static void grammaire(String id,
            jo.celeris.core.syntax.treesitter.TSLanguageSupplier langue) {
        TreeSitterGrammars.enregistrer(id, langue, requete(id));
    }

    /** La requête highlights.scm d'une langue — vide si l'asset
     *  manque (les heuristiques de repli servent, rien ne casse). */
    private static String requete(String id) {
        try (InputStream flux = NativeLanguages.class.getResourceAsStream(
                "/requetes/" + id + ".scm")) {
            return flux == null ? "" : lire(flux);
        } catch (IOException echec) {
            return "";
        }
    }

    /** La lecture compatible minSdk 26 : readAllBytes exige
     *  l'API 33 — la boucle y sert toutes les versions (les
     *  requêtes sont petites, quelques ko). */
    private static String lire(java.io.InputStream flux)
            throws IOException {
        java.io.ByteArrayOutputStream tampon =
                new java.io.ByteArrayOutputStream();
        byte[] lot = new byte[2048];
        int lus;
        while ((lus = flux.read(lot)) > 0) {
            tampon.write(lot, 0, lus);
        }
        return new String(tampon.toByteArray(), StandardCharsets.UTF_8);
    }
}
