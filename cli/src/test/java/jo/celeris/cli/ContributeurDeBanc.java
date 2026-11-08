package jo.celeris.cli;

import jo.celeris.core.language.LanguageContributor;

/**
 * Le contributeur de banc du cli (M7.6) : un contributeur découvert par
 * {@code ServiceLoader} sur la JVM — le mécanisme exclusif du cli. Il
 * ne déclare rien ; sa présence seule prouve la découverte, le
 * registre s'assemble avec lui.
 */
public final class ContributeurDeBanc implements LanguageContributor {

    @Override
    public void contribute(Contribution contribution) {
        // rien à déclarer : la découverte suffit au banc
    }
}
