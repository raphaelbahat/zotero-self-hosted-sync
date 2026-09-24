package app.anondev.patches.zotero.extension;

import java.io.File;

/**
 * Decides whether a stored attachment digest can be sent to the sync server as it stands.
 *
 * <p>Kept free of application classes on purpose: the extension is compiled on its own and merged
 * into the patched app, so it must depend on nothing but the JDK.
 */
public class AttachmentMd5 {

    /** Length of a hex MD5 digest, which is what the sync protocol accepts. */
    private static final int MD5_LENGTH = 32;

    /**
     * Returns whether the upload reader must recompute the attachment's digest before uploading.
     *
     * <p>The client's upload reader recomputes the digest from the file only when the stored value
     * is the literal string {@code "null"}, and otherwise passes the stored value straight to the
     * upload-authorization request. A row whose field is empty therefore sends an empty {@code md5}
     * field, which the server refuses ("md5 not provided"). That is the same closed loop as an
     * unusable {@code mtime}: the file never uploads, so the field is never repaired, so the
     * attachment never reaches any other client.
     *
     * <p>The test is the protocol's own — the server accepts a 32-character hex digest — so this
     * generalises the original {@code "null"} case rather than replacing it.
     *
     * @param value the stored field value, possibly null, empty or malformed
     * @param file the attachment's local file, the only thing the recomputation can hash
     * @return whether the reader should recompute the digest from the file and store it
     */
    public static boolean unusable(String value, File file) {
        if (isHexMd5(value)) {
            return false;
        }
        // Only ask the reader to hash when there is something to hash: the reader treats a failure
        // as fatal for the whole pass, so a file that is absent must not reach the hasher.
        return file != null && file.isFile();
    }

    private static boolean isHexMd5(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() != MD5_LENGTH) {
            return false;
        }
        for (int i = 0; i < MD5_LENGTH; i++) {
            char c = Character.toLowerCase(trimmed.charAt(i));
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}
