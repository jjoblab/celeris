package jo.celeris.sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Les échantillons de l'exemple (le retour terrain du 2026-11-19 :
 * « ajouter des exemples d'autres langages — kt, xml… — choix depuis
 * un drawer, ouverture dans des onglets ») : un fichier par langage
 * du dépôt, chacun assez riche pour porter guides d'indentation,
 * plis, complétion et coloration — le même esprit que la démo du
 * dépôt de départ, à la mesure de l'exemple.
 */
public final class Echantillons {

    private Echantillons() {
    }

    /** Un échantillon : son nom de fichier (la langue s'y résout), son texte. */
    public record Echantillon(String nomFichier, String texte) {
    }

    /** Le fichier Java de l'exemple — le seul branché au mini serveur LSP. */
    static final String EXEMPLE_JAVA =
            "package exemple;\n"
            + "\n"
            + "/** Salut — l'exemple vit sur la façade. */\n"
            + "public final class Salut {\n"
            + "    private String nom = \"celeris\";\n"
            + "\n"
            + "    public static void main(String[] args) {\n"
            + "        String nom = \"celeris\";\n"
            + "        System.out.println(\"Salut, \" + nom + \" !\");\n"
            + "        System.out.println(nom.length());\n"
            + "    }\n"
            + "}\n";

    /** Le fichier Kotlin : data class, coroutines, when. */
    static final String EXEMPLE_KOTLIN =
            "package exemple\n"
            + "\n"
            + "import kotlinx.coroutines.launch\n"
            + "\n"
            + "/** Le panier de l'exemple. */\n"
            + "data class Panier(\n"
            + "    val articles: List<String>,\n"
            + "    val total: Double = 0.0,\n"
            + ") {\n"
            + "    fun ajouter(article: String): Panier {\n"
            + "        val nouveaux = articles + article\n"
            + "        return copy(articles = nouveaux)\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "fun decrire(panier: Panier): String {\n"
            + "    return when {\n"
            + "        panier.articles.isEmpty() -> \"vide\"\n"
            + "        else -> \"${panier.articles.size} articles\"\n"
            + "    }\n"
            + "}\n";

    /** Le fichier XML : un gabarit Android. */
    static final String EXEMPLE_XML =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"wrap_content\"\n"
            + "    android:orientation=\"vertical\"\n"
            + "    android:padding=\"16dp\">\n"
            + "\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/titre\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Salut, celeris !\"\n"
            + "        android:textSize=\"18sp\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/action\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Ouvrir le journal\" />\n"
            + "</LinearLayout>\n";

    /** Le fichier Python : indentation, f-strings. */
    static final String EXEMPLE_PYTHON =
            "from dataclasses import dataclass\n"
            + "\n"
            + "\n"
            + "@dataclass\n"
            + "class Panier:\n"
            + "    articles: list[str]\n"
            + "\n"
            + "    def ajouter(self, article: str) -> \"Panier\":\n"
            + "        self.articles.append(article)\n"
            + "        return self\n"
            + "\n"
            + "    def decrire(self) -> str:\n"
            + "        if not self.articles:\n"
            + "            return \"vide\"\n"
            + "        return f\"{len(self.articles)} articles\"\n";

    /** Le fichier JSON : un manifeste de paquet du dépôt. */
    static final String EXEMPLE_JSON =
            "{\n"
            + "  \"langue\": \"json\",\n"
            + "  \"nom\": \"JSON\",\n"
            + "  \"motsReserves\": [],\n"
            + "  \"plis\": {\n"
            + "    \"parIndentation\": true\n"
            + "  }\n"
            + "}\n";

    /** Tous les échantillons de l'exemple, dans l'ordre du drawer. */
    public static List<Echantillon> tous() {
        List<Echantillon> echantillons = new ArrayList<>();
        echantillons.add(new Echantillon("Salut.java", EXEMPLE_JAVA));
        echantillons.add(new Echantillon("Panier.kt", EXEMPLE_KOTLIN));
        echantillons.add(new Echantillon("gabarit.xml", EXEMPLE_XML));
        echantillons.add(new Echantillon("panier.py", EXEMPLE_PYTHON));
        echantillons.add(new Echantillon("manifeste.json", EXEMPLE_JSON));
        return echantillons;
    }
}
