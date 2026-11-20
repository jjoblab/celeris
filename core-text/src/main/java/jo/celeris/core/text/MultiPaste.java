package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Le remplacement multi-plages (D-13, l'analyse croisée) : la i-ème
 * chaîne sur la i-ème plage de la sélection — la distribution du
 * coller multi-curseurs. Porteur extrait de {@link TextSession}
 * (ADR-002 : 600 lignes par classe, sans cliquet) ; la voie de
 * transaction reste CELLE DE LA SESSION — une édition par la voie
 * unique, annulable (PIEGE-07).
 */
final class MultiPaste {

    private MultiPaste() {
    }

    /**
     * Applique les textes sur les plages de la session : la i-ème
     * chaîne remplace la i-ème plage, une transaction unique.
     *
     * @param session la session porteuse de la sélection
     * @param textes  le texte de chaque plage, en ordre de sélection
     * @return vrai si la pose a eu lieu ; faux en lecture seule, sur
     *         une sélection vide ou si les tailles divergent
     */
    static boolean appliquer(TextSession session, List<String> textes) {
        if (session.isReadOnly() || session.selection().isEmpty()) {
            return false;
        }
        List<SelRange> plages = session.selection().ranges();
        if (textes.size() != plages.size()) {
            return false;
        }
        List<TextEdit> editions = new ArrayList<>(plages.size());
        int decalage = 0;
        boolean change = false;
        for (int i = 0; i < plages.size(); i++) {
            SelRange plage = plages.get(i);
            String retire = session.document().subText(plage.start(), plage.end());
            String pose = textes.get(i);
            if (!retire.equals(pose)) {
                editions.add(new TextEdit(plage.start() + decalage,
                        retire, pose));
                decalage += pose.length() - retire.length();
                change = true;
            }
        }
        if (!change) {
            return false;
        }
        return session.appliquerEditionsMulti(editions);
    }
}
