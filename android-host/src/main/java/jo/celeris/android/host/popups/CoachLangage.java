package jo.celeris.android.host.popups;

import java.util.List;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.SignatureAideModel;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Le coach de la langue ouverte : les mots réservés du LANGAGE — la
 * syntaxe du support les porte ({@code reservedWords()} de
 * {@link jo.celeris.core.syntax.LanguageSyntax}, les dialectes P0 y
 * posent leurs listes, la voie de grammaires y moissonne les
 * alternatives littérales des règles keyword) — badgés en mots
 * réservés, lisible par toute application hôte sans table à poser.
 *
 * <p>La complétion de base vit donc DÈS LE MONTAGE comme tout éditeur
 * de code : frapper {@code cla} propose {@code class} en Java,
 * {@code def} en Python — sans serveur de langage, sans coach de
 * l'hôte. La documentation et les signatures restent l'affaire des
 * tables de l'hôte ({@link CoachPopups#muet()}) ou du serveur branché
 * — ce coach ne fait que la table des mots.</p>
 */
final class CoachLangage implements CoachPopups {

    /** La langue ouverte — null garde la table vide (texte brut). */
    private final @Nullable LanguageSupport language;

    /**
     * Le coach de la langue ouverte.
     *
     * @param language le support de langage posé, null sans langage
     */
    CoachLangage(@Nullable LanguageSupport language) {
        this.language = language;
    }

    @Override
    public List<String> motsReserves() {
        if (language == null || language.syntax() == null) {
            return List.of();
        }
        return language.syntax().reservedWords();
    }

    @Override
    public List<CompletionListView.Candidat> candidats(List<String> bruts) {
        List<String> reserves = motsReserves();
        List<CompletionListView.Candidat> types =
                new java.util.ArrayList<>(bruts.size());
        for (String nom : bruts) {
            types.add(new CompletionListView.Candidat(nom, "",
                    reserves.contains(nom)
                            ? jo.celeris.android.widgets.BadgeGenre.Genre.KEYWORD
                            : jo.celeris.android.widgets.BadgeGenre.Genre.VARIABLE));
        }
        return types;
    }

    @Override
    public @Nullable CompletionDocModel docDe(String symbole) {
        return null;
    }

    @Override
    public @Nullable SignatureAideModel signatureDe(Document document,
            int caret) {
        return null;
    }
}
