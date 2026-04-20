package com.v2retail.dotvik.srm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.MultipartUploadRequest;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SrmMyDocumentsActivity extends AppCompatActivity {

    private static final String[] DOC_TYPES  = {"PAN_CARD","CANCELLED_CHEQUE","GST_CERTIFICATE","BILL_COPY","MSME","TRADE_LICENCE","ISO"};
    private static final String[] DOC_LABELS = {"PAN Card ★","Cancelled Cheque ★","GST Certificate ★","Bill Copy ★","MSME Certificate","Trade Licence","ISO Certificate"};
    private static final int[]    PICK_RC    = {201,202,203,204,205,206,207};

    private String appId;
    private final Map<String, JSONObject> uploadedDocs = new HashMap<>();

    private ProgressBar progress;
    private LinearLayout docContainer;
    private TextView tvNoApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_my_documents);
        progress     = findViewById(R.id.progressSrmMyDocs);
        docContainer = findViewById(R.id.llSrmDocContainer);
        tvNoApp      = findViewById(R.id.tvSrmMyDocsNoApp);
        loadApplication();
    }

    private void loadApplication() {
        progress.setVisibility(View.VISIBLE);
        SrmApiClient.get(this, "/applications?limit=1", res -> {
            try {
                JSONArray data = res.getJSONArray("data");
                if (data.length() == 0) {
                    progress.setVisibility(View.GONE);
                    tvNoApp.setVisibility(View.VISIBLE);
                    return;
                }
                appId = data.getJSONObject(0).getString("id");
                loadDocs();
            } catch (Exception e) { progress.setVisibility(View.GONE); }
        }, e -> progress.setVisibility(View.GONE));
    }

    private void loadDocs() {
        SrmApiClient.get(this, "/documents/" + appId, res -> {
            progress.setVisibility(View.GONE);
            try {
                JSONArray docs = res.getJSONArray("data");
                uploadedDocs.clear();
                for (int i = 0; i < docs.length(); i++) {
                    JSONObject d = docs.getJSONObject(i);
                    uploadedDocs.put(d.optString("doc_type"), d);
                }
                buildDocCards();
            } catch (Exception ignored) {}
        }, e -> progress.setVisibility(View.GONE));
    }

    private void buildDocCards() {
        docContainer.removeAllViews();
        for (int i = 0; i < DOC_TYPES.length; i++) {
            final int idx = i;
            String type = DOC_TYPES[i];
            JSONObject uploaded = uploadedDocs.get(type);
            boolean required = i < 4;

            View card = getLayoutInflater().inflate(R.layout.item_srm_doc_card, docContainer, false);
            TextView tvLabel  = card.findViewById(R.id.tvDocCardLabel);
            TextView tvStatus = card.findViewById(R.id.tvDocCardStatus);
            TextView tvVerify = card.findViewById(R.id.tvDocCardVerified);
            Button   btnAction= card.findViewById(R.id.btnDocCardAction);

            tvLabel.setText(DOC_LABELS[i]);

            if (uploaded != null) {
                tvStatus.setText(uploaded.optString("original_name", "Uploaded"));
                tvStatus.setTextColor(0xFF388E3C);
                tvVerify.setVisibility(uploaded.optBoolean("is_verified") ? View.VISIBLE : View.GONE);
                btnAction.setText("Replace");
            } else {
                tvStatus.setText(required ? "Not uploaded — REQUIRED" : "Not uploaded (Optional)");
                tvStatus.setTextColor(required ? 0xFFC62828 : 0xFF9E9E9E);
                tvVerify.setVisibility(View.GONE);
                btnAction.setText("Upload");
            }

            btnAction.setOnClickListener(v -> pickFile(idx));
            docContainer.addView(card);
        }
    }

    private void pickFile(int idx) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf","image/jpeg","image/png"});
        startActivityForResult(Intent.createChooser(intent, "Select " + DOC_LABELS[idx]), PICK_RC[idx]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || appId == null) return;
        for (int i = 0; i < PICK_RC.length; i++) {
            if (requestCode == PICK_RC[i]) {
                Uri uri = data.getData();
                String name = getFileName(uri);
                uploadDoc(DOC_TYPES[i], uri, name);
                break;
            }
        }
    }

    private void uploadDoc(String docType, Uri uri, String name) {
        progress.setVisibility(View.VISIBLE);
        MultipartUploadRequest req = new MultipartUploadRequest(this, appId, docType, uri, name,
                res -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, name + " uploaded", Toast.LENGTH_SHORT).show();
                    loadDocs();
                },
                e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed: " + SrmApiClient.parseError(e), Toast.LENGTH_LONG).show();
                });
        SrmApiClient.getQueue(this).add(req);
    }

    private String getFileName(Uri uri) {
        String name = "document";
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) name = c.getString(idx);
            c.close();
        }
        return name;
    }
}
