package app.anondev.patches.zotero.extension;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Supplies the upload form envelope a Zotero server may leave out of an upload authorization.
 *
 * <p>Kept free of application classes on purpose: the extension is compiled on its own and merged
 * into the patched app, so it depends on nothing but the JDK and Gson, which the app itself ships.
 */
public class AuthorizeUploadParams {

    /**
     * Returns the authorization's {@code params} element, or an empty envelope when the response
     * carries none.
     *
     * <p>The client parses that element into a map and dereferences the result, so a server that
     * sends no {@code params} field — having no upload form to hand out, because it is not S3 —
     * turns a completed authorization into a {@code NullPointerException}: the attachment is never
     * uploaded and the sync reports a failure it cannot explain. The desktop client accepts the
     * same response, and an empty envelope is exactly what "send the file with nothing wrapped
     * around it" means.
     *
     * @param response the upload authorization the server returned
     * @return the element to parse as the upload form, never null
     */
    public static JsonElement orEmpty(JsonObject response) {
        JsonElement params = response.get("params");
        if (params == null || params.isJsonNull()) {
            return new JsonObject();
        }
        return params;
    }
}
