package jo.celeris.android.host.spi;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * La porte {@link ArbreDocuments} sur le framework d'accès au stockage
 * (E-01) : chaque appel devient une requête {@link DocumentsContract}
 * au résolveur de contenu de l'hôte. C'est la seule classe du module
 * qui touche au framework — mince par conception, documentée ligne à
 * ligne, sans logique de chemin (elle vit dans
 * {@link AdaptateurStockageDocuments}, testée sur JVM).
 *
 * <p>L'arbre parlé est l'arbre du dossier accordé
 * ({@code ACTION_OPEN_DOCUMENT_TREE}) : l'URI racine porte
 * l'autorité du fournisseur et l'identifiant du dossier.</p>
 */
public final class ArbreDocumentsSaf implements ArbreDocuments {

    private final ContentResolver resolver;
    private final Uri racineArbre;
    private final Uri documentsEnfants;

    /**
     * Construit la porte sur le dossier accordé.
     *
     * @param contexte   le contexte de l'hôte
     * @param uriAccordee l'URI du dossier racine accordé, telle que
     *                    rendue par le sélecteur de documents
     */
    public ArbreDocumentsSaf(Context contexte, Uri uriAccordee) {
        this.resolver = contexte.getContentResolver();
        this.racineArbre = Objects.requireNonNull(uriAccordee, "uriAccordee");
        this.documentsEnfants = DocumentsContract.buildChildDocumentsUriUsingTree(
                racineArbre, DocumentsContract.getTreeDocumentId(racineArbre));
    }

    @Override
    public List<EntreeDocument> enfants(String idDossier) {
        Uri enfants = DocumentsContract.buildChildDocumentsUriUsingTree(
                racineArbre, idDossier);
        List<EntreeDocument> entrees = new ArrayList<>();
        try (Cursor curseur = resolver.query(
                enfants,
                new String[] {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE },
                null, null, null)) {
            if (curseur == null) {
                return entrees;
            }
            while (curseur.moveToNext()) {
                String id = curseur.getString(0);
                String nom = curseur.getString(1);
                String mime = curseur.getString(2);
                boolean dossier = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                entrees.add(new EntreeDocument(nom, id, dossier));
            }
        }
        return entrees;
    }

    @Override
    public String lire(String idFichier) {
        return new String(lireOctets(idFichier), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] lireOctets(String idFichier) {
        Uri document = DocumentsContract.buildDocumentUriUsingTree(racineArbre, idFichier);
        try (InputStream flot = resolver.openInputStream(document)) {
            if (flot == null) {
                throw new IllegalStateException("stream refused: " + idFichier);
            }
            ByteArrayOutputStream tampon = new ByteArrayOutputStream();
            byte[] bloc = new byte[8192];
            for (int lu = flot.read(bloc); lu >= 0; lu = flot.read(bloc)) {
                tampon.write(bloc, 0, lu);
            }
            return tampon.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("read failed: " + idFichier, e);
        }
    }

    @Override
    public void remplacer(String idFichier, String contenu) {
        Uri document = DocumentsContract.buildDocumentUriUsingTree(racineArbre, idFichier);
        // Mode « wt » : écriture depuis le début, tronquée — le
        // remplacement du document se fait en place, l'identifiant et
        // le nom survivent.
        try (OutputStream flot = resolver.openOutputStream(document, "wt")) {
            if (flot == null) {
                throw new IllegalStateException("stream refused: " + idFichier);
            }
            flot.write(contenu.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("write failed: " + idFichier, e);
        }
    }

    @Override
    public String creer(String idParent, String nom, boolean dossier) {
        String mime = dossier
                ? DocumentsContract.Document.MIME_TYPE_DIR
                : "text/plain";
        try {
            Uri cree = DocumentsContract.createDocument(
                    resolver,
                    DocumentsContract.buildDocumentUriUsingTree(racineArbre, idParent),
                    mime,
                    nom);
            return DocumentsContract.getDocumentId(cree);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("create failed: " + nom, e);
        }
    }

    @Override
    public boolean estFichier(String idDocument) {
        Uri document = DocumentsContract.buildDocumentUriUsingTree(racineArbre, idDocument);
        try (Cursor curseur = resolver.query(
                document,
                new String[] {DocumentsContract.Document.COLUMN_MIME_TYPE},
                null, null, null)) {
            if (curseur == null || !curseur.moveToFirst()) {
                return false;
            }
            return !DocumentsContract.Document.MIME_TYPE_DIR.equals(curseur.getString(0));
        }
    }
}
