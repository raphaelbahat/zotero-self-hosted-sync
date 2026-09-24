package app.anondev.patches.zotero.extension;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends an attachment upload as the file itself rather than as a form wrapped around it.
 *
 * <p>The client uploads with the S3 shape it is written for: a {@code multipart/form-data} body made
 * of the upload form's fields plus a single {@code file} part. A server that takes the bytes itself
 * instead of relaying them to S3 — having handed out no upload form, so there are no fields to send
 * — reads the body as the file and compares its length against the size the client declared, so the
 * form around the file makes the upload fail as a size (and digest) mismatch.
 *
 * <p>This is a workaround for that server behaviour, not a client fix: the client speaks the
 * protocol the official server expects. It applies the narrowest rewrite that covers the case — a
 * multipart body whose only part is the {@code file} part. An upload that does carry a form (the S3
 * case the client was written for) has more than one part and is passed through untouched.
 */
public class AttachmentUploadInterceptor implements Interceptor {

    /** The disposition naming the part as the file, as OkHttp writes it. */
    private static final String FILE_PART = "name=\"file\"";
    private static final String DISPOSITION = "Content-Disposition";

    /**
     * Adds this interceptor to a client's chain, keeping the interceptor already being added there.
     *
     * <p>Replaces the client's last {@code addInterceptor} call, so the chain it already had is
     * installed in the same order and this one follows it. Written as a replacement for that call
     * rather than an insertion, because the call's shape — a builder and an interceptor in, the
     * builder out — is exactly the shape needed here.
     *
     * @param builder the client builder the replaced call was called on
     * @param existing the interceptor that call was installing, now installed here
     * @return the same builder, so the chain continues unchanged
     */
    public static OkHttpClient.Builder install(OkHttpClient.Builder builder, Interceptor existing) {
        builder.addInterceptor(existing);
        builder.addInterceptor(new AttachmentUploadInterceptor());
        return builder;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        RequestBody body = request.body();
        if (body instanceof MultipartBody) {
            RequestBody file = soleFilePart(((MultipartBody) body).parts());
            if (file != null) {
                // The file body is the one the client wrapped, so its length and bytes are the ones
                // the upload was authorized for. Headers, including any precondition, are kept.
                return chain.proceed(
                    request.newBuilder().method(request.method(), file).build()
                );
            }
        }
        return chain.proceed(request);
    }

    /**
     * The body of the only part, when that part is the file and nothing is wrapped with it.
     *
     * @param parts the multipart body's parts
     * @return the file body to send on its own, or null when this is not that shape
     */
    private static RequestBody soleFilePart(List<MultipartBody.Part> parts) {
        if (parts.size() != 1) {
            return null;
        }
        MultipartBody.Part part = parts.get(0);
        String disposition = part.headers() == null ? null : part.headers().get(DISPOSITION);
        if (disposition == null || !disposition.contains(FILE_PART)) {
            return null;
        }
        return part.body();
    }
}