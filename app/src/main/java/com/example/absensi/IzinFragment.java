package com.example.absensi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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

public class IzinFragment extends Fragment {

    private static final int REQUEST_FILE_PICKER = 1;
    private static final int MAX_FILE_SIZE_KB = 1024; // 1MB max

    private EditText etName, etCategory, etReason;
    private TextView tvFileName;
    private Button btnSelectFile, btnSubmit;

    private Uri fileUri;
    private String kategoriPerizinan;

    private ProgressDialog progressDialog;

    public IzinFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_izin, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view); // Inisialisasi semua view

        // Karena fragment ini KHUSUS untuk Izin, set kategori secara hardcode
        kategoriPerizinan = "Izin";
        etCategory.setText(kategoriPerizinan);
        etCategory.setEnabled(false);
        etCategory.setFocusable(false);
        etCategory.setClickable(false);

        // Prefill nama pengguna dari Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etName.setText(user.getDisplayName());
            etName.setEnabled(false);
            etName.setFocusable(false);
            etName.setClickable(false);
        } else {
            // Jika user null (belum login), set nama default atau minta login
            etName.setText("Pengguna Tidak Dikenal");
            etName.setEnabled(false);
        }


        // Set listener untuk tombol-tombol
        btnSelectFile.setOnClickListener(v -> selectFile());
        btnSubmit.setOnClickListener(v -> submitPerizinan());

        // Inisialisasi ProgressDialog
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Menyimpan Perizinan...\nMohon ditunggu sebentar");
        progressDialog.setCancelable(false);
    }


    private void initViews(View view) { // Accept View parameter
        etName = view.findViewById(R.id.etName);
        etCategory = view.findViewById(R.id.etCategory);
        etReason = view.findViewById(R.id.etReason);
        tvFileName = view.findViewById(R.id.tvFileName);
        btnSelectFile = view.findViewById(R.id.btnSelectFile);
        btnSubmit = view.findViewById(R.id.btnSubmit);
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        // Use startActivityForResult from fragment
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    private void submitPerizinan() {
        if (fileUri == null) {
            Toast.makeText(requireContext(), "File PDF diperlukan", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = Objects.requireNonNull(etReason.getText()).toString().trim();
        if (reason.isEmpty()) {
            etReason.setError("Alasan izin harus diisi");
            return;
        }

        new AlertDialog.Builder(requireContext()) // Use requireContext() for AlertDialog
                .setTitle("Konfirmasi Perizinan")
                .setMessage(getString(R.string.konfirmasi_isi_perizinan_sederhana,
                        etName.getText(), kategoriPerizinan, reason))
                .setPositiveButton("Kirim", (dialog, which) -> sendDataToGoogleScript())
                .setNegativeButton("Batal", null)
                .show();
    }

    private byte[] getFileBytes(Uri uri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
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
        progressDialog.show(); // Show the fragment's progress dialog

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "unknown_user";

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                byte[] fileBytes = getFileBytes(fileUri);

                if (fileBytes.length > MAX_FILE_SIZE_KB * 1024) {
                    if (getActivity() != null) { // Check if fragment is attached
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(),
                                    "File terlalu besar (max 1MB)",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
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
                json.put("userId", userId);

                json.put("action", "submit_perizinan");

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://script.google.com/macros/s/AKfycbybJiPgC__UHLqG5wdjV6nnfQCmBSdxzNCfkl3V7lZskaplikhYUCWUAcL44LtrYRff/exec")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        if (getActivity() != null) { // Check if fragment is attached
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(
                                        requireContext(),
                                        "Gagal mengirim data: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if (getActivity() != null) { // Check if fragment is attached
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                try {
                                    ResponseBody responseBodyObj = response.body();
                                    if (responseBodyObj == null) {
                                        Toast.makeText(requireContext(),
                                                "Response kosong",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String responseBody = responseBodyObj.string();
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    if ("success".equals(jsonResponse.getString("status"))) {
                                        Toast.makeText(
                                                requireContext(),
                                                "Perizinan berhasil dikirim!",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    } else {
                                        Toast.makeText(
                                                requireContext(),
                                                "Error: " + jsonResponse.getString("message"),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                } catch (Exception e) {
                                    Log.e("IzinFragment", "Error parsing response", e);
                                    Toast.makeText(
                                            requireContext(),
                                            "Error parsing response",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });
                        }
                    }
                });

            } catch (Exception e) {
                if (getActivity() != null) { // Check if fragment is attached
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e("IzinFragment", "Error sending data", e);
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(
                            android.provider.OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                Log.e("IzinFragment", "Error getting file name", e);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICKER && resultCode == getActivity().RESULT_OK && data != null) {
            fileUri = data.getData();
            assert fileUri != null;
            String fileName = getFileName(fileUri);
            tvFileName.setText(fileName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dismiss the dialog to prevent window leaks if the fragment is destroyed
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}