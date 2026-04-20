package com.v2retail.dotvik.srm.api;

import android.content.Context;
import android.net.Uri;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

/**
 * Multipart upload request for document files.
 * Sends file as multipart/form-data with doc_type field.
 */
public class MultipartUploadRequest extends Request<JSONObject> {

    private static final String BOUNDARY = "SRM_BOUNDARY_" + System.currentTimeMillis();
    private static final String CRLF = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private final Context ctx;
    private final String appId;
    private final String docType;
    private final Uri fileUri;
    private final String fileName;
    private final Response.Listener<JSONObject> listener;

    public MultipartUploadRequest(Context ctx, String appId, String docType,
                                  Uri fileUri, String fileName,
                                  Response.Listener<JSONObject> listener,
                                  Response.ErrorListener errorListener) {
        super(Method.POST, SrmApiClient.BASE_URL + "/documents/" + appId + "/upload",
                errorListener);
        this.ctx = ctx;
        this.appId = appId;
        this.docType = docType;
        this.fileUri = fileUri;
        this.fileName = fileName;
        this.listener = listener;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }

    @Override
    public byte[] getBody() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            // doc_type field
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"doc_type\"" + CRLF + CRLF);
            dos.writeBytes(docType + CRLF);

            // file field
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + CRLF);
            dos.writeBytes("Content-Type: application/octet-stream" + CRLF + CRLF);

            InputStream is = ctx.getContentResolver().openInputStream(fileUri);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) dos.write(buf, 0, len);
            is.close();

            dos.writeBytes(CRLF + TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + CRLF);
            dos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> h = new HashMap<>();
        String token = SrmApiClient.getToken(ctx);
        if (token != null) h.put("Authorization", "Bearer " + token);
        return h;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data);
            return Response.success(new JSONObject(json), HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new com.android.volley.ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }
}
