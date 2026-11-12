package jo.celeris.serveur.java;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.jspecify.annotations.Nullable;

import jo.celeris.serveur.java.JavaSymbols.Symbol;

/**
 * L'aide aux paramètres du mini serveur (B-07) : l'appel ouvert sous
 * le curseur reçoit la signature de SA déclaration — le libellé porte
 * les paramètres tels que déclarés, le paramètre courant se compte aux
 * virgules de premier niveau (celles des appels imbriqués ne comptent
 * pas, celles des littéraux non plus — la ligne dépouillée de ses
 * chaînes et commentaires est comptée).
 *
 * <p>Hors appel, après l'appel fermé, dans l'en-tête d'une
 * déclaration, sur un nom inconnu : {@code null} — la popup se ferme
 * au bon moment, jamais elle ne s'ouvre à côté. Le constructeur
 * appelé par {@code new} répond comme toute méthode.</p>
 */
public final class JavaSignature {

    private JavaSignature() {
    }

    /**
     * L'aide aux paramètres d'une position : l'appel ouvert le plus
     * intime qui englobe le curseur rend la signature de sa
     * déclaration.
     *
     * @param source  le document suivi
     * @param symbols les symboles déjà extraits du document
     * @param offset  le décalage du curseur
     * @return l'aide, ou null hors appel ouvert
     */
    public static @Nullable SignatureHelp at(JavaSource source,
            List<Symbol> symbols, int offset) {
        Appel appel = appelOuvert(source, symbols, offset);
        if (appel == null) {
            return null;
        }
        Symbol declaration = declarationDe(symbols, appel.nom());
        if (declaration == null) {
            // le nom inconnu ne sers rien : la popup ne s'ouvre pas à côté
            return null;
        }
        List<String> parametres = parametresDeclares(source, declaration);
        SignatureInformation information = new SignatureInformation(
                libelle(declaration, parametres));
        List<ParameterInformation> infos = new ArrayList<>();
        for (String parametre : parametres) {
            infos.add(new ParameterInformation(parametre));
        }
        information.setParameters(infos);
        SignatureHelp aide = new SignatureHelp();
        aide.getSignatures().add(information);
        aide.setActiveSignature(0);
        aide.setActiveParameter(Math.min(appel.parametreCourant(),
                Math.max(0, parametres.size() - 1)));
        return aide;
    }

    /** Un appel ouvert : le nom appelé, le paramètre courant. */
    private record Appel(String nom, int parametreCourant) {
    }

    /**
     * L'appel ouvert le plus intime qui englobe le curseur : balayage
     * inversé des parenthèses de la ligne dépouillée — l'ouvrante
     * trouvée remonte au nom qui la précède ; les parenthèses fermées
     * avant le curseur n'englobent rien.
     */
    private static @Nullable Appel appelOuvert(JavaSource source,
            List<Symbol> symbols, int offset) {
        JavaSource.StrippedCode code = source.stripped();
        String texte = code.text();
        // la coupe déborde : le curseur sur le fermant tient l'appel
        if (offset > texte.length()) {
            return null;
        }
        int profondeur = 0;
        for (int i = offset - 1; i >= 0; i--) {
            char c = texte.charAt(i);
            if (c == ')') {
                profondeur++;
            } else if (c == '(') {
                if (profondeur > 0) {
                    profondeur--;
                    continue;
                }
                // l'ouvrante la plus proche : le nom avant elle décide
                int finNom = i;
                int debutNom = finNom;
                while (debutNom > 0
                        && Character.isJavaIdentifierPart(
                        texte.charAt(debutNom - 1))) {
                    debutNom--;
                }
                if (debutNom == finNom) {
                    return null;
                }
                String nom = texte.substring(debutNom, finNom);
                // « new » n'est pas un appel : le constructeur se nomme avant
                if ("new".equals(nom)) {
                    int finConstr = debutNom;
                    int debutConstr = finConstr;
                    while (debutConstr > 0
                            && Character.isJavaIdentifierPart(
                            texte.charAt(debutConstr - 1))) {
                        debutConstr--;
                    }
                    if (debutConstr == finConstr) {
                        return null;
                    }
                    nom = texte.substring(debutConstr, finConstr);
                } else if (siteDeDeclaration(symbols, nom, debutNom)) {
                    // l'EN-TÊTE d'une déclaration : la signature qui se
                    // déclare n'est pas un appel qui se sert — null, la
                    // popup ne s'ouvre pas à côté
                    return null;
                }
                return new Appel(nom,
                        parametreCourant(texte, i + 1, offset));
            }
        }
        return null;
    }

    /**
     * Vrai quand le nom devant la parenthèse ouverte est le SITE DE
     * DÉCLARATION d'un symbole : le nom qui se déclare commence
     * exactement où le symbole dit que son nom commence. Un usage
     * (un appel) commence ailleurs — même nom, autre site.
     */
    private static boolean siteDeDeclaration(List<Symbol> symbols,
            String nom, int debutNom) {
        for (Symbol symbole : symbols) {
            if (symbole.name().equals(nom)
                    && symbole.nameStart() == debutNom) {
                return true;
            }
            if (siteDeDeclaration(symbole.children(), nom, debutNom)) {
                return true;
            }
        }
        return false;
    }

    /** Le paramètre courant : les virgules de premier niveau entre
     *  l'ouvrante et le curseur ; les littéraux sont déjà dépouillés. */
    private static int parametreCourant(String texte, int apresOuvrante,
            int curseur) {
        int profondeur = 0;
        int numero = 0;
        for (int i = apresOuvrante; i < curseur && i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                profondeur++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (profondeur == 0) {
                    return numero;
                }
                profondeur--;
            } else if (c == ',' && profondeur == 0) {
                numero++;
            }
        }
        return numero;
    }

    /** La déclaration du nom appelé : méthodes, constructeurs, champs. */
    private static @Nullable Symbol declarationDe(List<Symbol> symbols,
            String nom) {
        for (Symbol symbole : symbols) {
            for (Symbol enfant : symbole.children()) {
                if (enfant.name().equals(nom)
                        && (enfant.kind() == 6 || enfant.kind() == 9)) {
                    return enfant;
                }
            }
            // le constructeur porte le nom de la classe : cherché aussi
            if (symbole.name().equals(nom)) {
                for (Symbol enfant : symbole.children()) {
                    if (enfant.kind() == 9) {
                        return enfant;
                    }
                }
            }
            Symbol imbrique = declarationDe(symbole.children(), nom);
            if (imbrique != null) {
                return imbrique;
            }
        }
        return null;
    }

    /** Les paramètres déclarés : la tranche entre les parenthèses de
     *  l'en-tête, coupée aux virgules de premier niveau. */
    private static List<String> parametresDeclares(JavaSource source,
            Symbol declaration) {
        JavaSource.StrippedCode code = source.stripped();
        String texte = code.text();
        int finNom = declaration.nameEnd();
        int ouvrante = texte.indexOf('(', finNom);
        if (ouvrante < 0 || ouvrante > declaration.end()) {
            return List.of();
        }
        List<String> parametres = new ArrayList<>();
        int profondeur = 0;
        int debut = ouvrante + 1;
        for (int i = ouvrante + 1; i <= declaration.end() && i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '(' || c == '[' || c == '<' || c == '{') {
                profondeur++;
            } else if (c == ')' || c == ']' || c == '>' || c == '}') {
                if (c == ')' && profondeur == 0) {
                    ajoute(parametres, texte.substring(debut, i));
                    return parametres;
                }
                profondeur--;
            } else if (c == ',' && profondeur == 0) {
                ajoute(parametres, texte.substring(debut, i));
                debut = i + 1;
            }
        }
        return parametres;
    }

    /** La tranche nettoyée — vide si rien ne reste. */
    private static void ajoute(List<String> parametres, String tranche) {
        String nette = tranche.strip();
        if (!nette.isEmpty()) {
            parametres.add(nette);
        }
    }

    /** Le libellé de la signature : « nom(type nom, ...) » tel que déclaré. */
    private static String libelle(Symbol declaration,
            List<String> parametres) {
        StringBuilder libelle = new StringBuilder(declaration.name())
                .append('(');
        for (int i = 0; i < parametres.size(); i++) {
            if (i > 0) {
                libelle.append(", ");
            }
            libelle.append(parametres.get(i));
        }
        return libelle.append(')').toString();
    }
}
