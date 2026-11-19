package jo.celeris.core.ai;

/**
 * Le fichier d'instructions d'agent du dépôt (F-07, ADR-012) : le
 * texte que les outils exposés servent aux agents — conventions du
 * projet, règles du propriétaire. Le fichier se lit tel quel, la
 * exemple consommateur honore le sien par la voie des outils
 * ({@code celeris.instructions}).
 *
 * <p>Une instance vide ({@link #AUCUNE}) sert les dépôts sans
 * fichier : l'outil répond honnêtement qu'il n'y a rien, jamais il
 * n'invente de conventions.</p>
 *
 * @param text les instructions, lues telles quelles
 */
public record AgentInstructions(String text) {

    /** Les instructions absentes : le dépôt n'en déclare pas. */
    public static final AgentInstructions AUCUNE = new AgentInstructions("");

    /**
     * Valide les instructions : jamais nulles — vides, c'est
     * {@link #AUCUNE}.
     *
     * @throws NullPointerException si le texte est null
     */
    public AgentInstructions {
        java.util.Objects.requireNonNull(text, "texte");
    }

    /**
     * Les instructions du fichier, lues telles quelles — la marque du
     * fichier absent reste vide.
     *
     * @param contenuFichier le contenu du fichier d'instructions
     * @return les instructions, ou {@link #AUCUNE} si le contenu est
     *         blanc
     */
    public static AgentInstructions of(String contenuFichier) {
        return contenuFichier.isBlank() ? AUCUNE
                : new AgentInstructions(contenuFichier.strip());
    }

    /** Vrai quand le dépôt ne déclare aucune instruction. */
    public boolean isAbsent() {
        return text.isEmpty();
    }
}
