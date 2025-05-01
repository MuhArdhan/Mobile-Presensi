package com.example.absensi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PerizinanActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1;
    private static final int MAX_FILE_SIZE_KB = 1024; // 1MB max

    private EditText etName, etCategory, etReason;
    private TextView tvFileName, tvTitle;
    private Button btnSelectFile, btnSubmit;

    private Uri fileUri;
    private String kategoriPerizinan;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perizinan);

        initViews();

        kategoriPerizinan = getIntent().getStringExtra("KATEGORI");
        etCategory.setText(kategoriPerizinan);
        tvTitle.setText(getString(R.string.form_perizinan, kategoriPerizinan));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etName.setText(user.getDisplayName());
        }

        btnSelectFile.setOnClickListener(v -> selectFile());
        btnSubmit.setOnClickListener(v -> submitPerizinan());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menyimpan Perizinan..." +
                "Mohon ditunggu sebentar");
        progressDialog.setCancelable(false);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // untuk kembali ke activity sebelumnya
        });

    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etCategory = findViewById(R.id.etCategory);
        etReason = findViewById(R.id.etReason);
        tvFileName = findViewById(R.id.tvFileName);
        tvTitle = findViewById(R.id.tvTitle);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    private void submitPerizinan() {
        if (fileUri == null) {
            Toast.makeText(this, "File PDF diperlukan", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = Objects.requireNonNull(etReason.getText()).toString().trim();
        if (reason.isEmpty()) {
            etReason.setError("Alasan izin harus diisi");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Perizinan")
                .setMessage(getString(R.string.konfirmasi_isi_perizinan_sederhana,
                        etName.getText(), kategoriPerizinan, reason))
                .setPositiveButton("Kirim", (dialog, which) -> sendDataToGoogleScript())
                .setNegativeButton("Batal", null)
                .show();
    }

    private byte[] getFileBytes(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while (true) {
            assert inputStream != null;
            if ((len = inputStream.read(buffer)) == -1) break;
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void sendDataToGoogleScript() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mengirim data perizinan...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                byte[] fileBytes = getFileBytes(fileUri);

                if (fileBytes.length > MAX_FILE_SIZE_KB * 1024) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this,
                                "File terlalu besar (max 1MB)",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String base64File = Base64.encodeToString(fileBytes, Base64.DEFAULT);

                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                json.put("nama", Objects.requireNonNull(etName.getText()).toString());
                json.put("kategori", kategoriPerizinan);
                json.put("alasan", Objects.requireNonNull(etReason.getText()).toString());
                json.put("fileBase64", base64File);
                json.put("fileName", getFileName(fileUri));

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://script.google.com/macros/s/AKfycbwRN5kSDPHwTTEeNjiXcoe2nZI-zILjJeruiu5FZk3GMrzLAhxcdfQ8KK1ro5BDCz8QJg/exec") // ganti dengan URL kamu
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(
                                    PerizinanActivity.this,
                                    "Gagal mengirim data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            try {
                                ResponseBody responseBodyObj = response.body();
                                if (responseBodyObj == null) {
                                    Toast.makeText(PerizinanActivity.this,
                                            "Response kosong",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String responseBody = responseBodyObj.string();
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    Toast.makeText(
                                            PerizinanActivity.this,
                                            "Perizinan berhasil dikirim!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    finish();
                                } else {
                                    Toast.makeText(
                                            PerizinanActivity.this,
                                            "Error: " + jsonResponse.getString("message"),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            } catch (Exception e) {
                                Log.e("PerizinanActivity", "Error parsing response", e);
                                Toast.makeText(
                                        PerizinanActivity.this,
                                        "Error parsing response",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.e("PerizinanActivity", "Error sending data", e);
                    Toast.makeText(PerizinanActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(
                            android.provider.OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                Log.e("PerizinanActivity", "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            assert fileUri != null;
            String fileName = getFileName(fileUri);
            tvFileName.setText(fileName);
        }
    }
}
