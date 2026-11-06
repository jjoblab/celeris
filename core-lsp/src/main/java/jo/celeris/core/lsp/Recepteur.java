package jo.celeris.core.lsp;

/**
 * Le récepteur d'un transport : les messages qui arrivent du pair —
 * une charge JSON par appel, jamais une trame partielle. La fermeture
 * du pair est annoncée une fois, après quoi plus aucun message ne
 * arrive.
 */
public interface Recepteur {

    /**
     * Un message reçu du pair.
     *
     * @param trame la charge JSON complète
     */
    void recoit(String trame);

    /** Le pair a fermé son bout du transport. */
    default void ferme() {
    }
}
