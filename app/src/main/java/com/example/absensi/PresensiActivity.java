package com.example.absensi;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PresensiActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final int MAX_IMAGE_SIZE_KB = 500;

    private ImageView ivSelfie;
    private TextInputEditText etNama, etKategori, etLokasi;
    private TextView tvKoordinat, tvHeader;
    private Button btnTakePhoto, btnGetLocation, btnSubmit;

    private String kategoriPresensi;
    private Uri fotoUri;
    private String currentLatLng;
    private FusedLocationProviderClient fusedLocationClient;

    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presensi);

        initViews();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        kategoriPresensi = getIntent().getStringExtra("KATEGORI");
        etKategori.setText(kategoriPresensi);
        tvHeader.setText(getString(R.string.form_presensi, kategoriPresensi));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etNama.setText(user.getDisplayName());
        }

        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnSubmit.setOnClickListener(v -> submitPresensi());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menyimpan presensi..." +
                "Mohon ditunggu sebentar");
        progressDialog.setCancelable(false); // tidak bisa di-cancel biar user nunggu
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // untuk kembali ke activity sebelumnya
        });


    }

    private void initViews() {
        ivSelfie = findViewById(R.id.ivSelfie);
        etNama = findViewById(R.id.etNama);
        etKategori = findViewById(R.id.etKategori);
        etLokasi = findViewById(R.id.etLokasi);
        tvKoordinat = findViewById(R.id.tvKoordinat);
        tvHeader = findViewById(R.id.tvHeader);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void takePhoto() {
        ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .compress(MAX_IMAGE_SIZE_KB)
                .maxResultSize(800, 800)
                .start(REQUEST_IMAGE_CAPTURE);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatLng = location.getLatitude() + "," + location.getLongitude();
                        tvKoordinat.setText(getString(R.string.koordinat, currentLatLng));
                        getAddressFromLocation(location);
                    } else {
                        tvKoordinat.setText(R.string.lokasi_tidak_ditemukan);
                    }
                });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = getString(R.string.address_format,
                        address.getThoroughfare(),
                        address.getSubAdminArea(),
                        address.getAdminArea());

                etLokasi.setText(addressText);
            }
        } catch (IOException e) {
            Log.e("PresensiActivity", "Error getting address", e);
        }
    }

    private void submitPresensi() {
        if (fotoUri == null) {
            Toast.makeText(this, R.string.foto_diperlukan, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLatLng == null) {
            Toast.makeText(this, R.string.lokasi_diperlukan, Toast.LENGTH_SHORT).show();
            return;
        }

        String lokasi = Objects.requireNonNull(etLokasi.getText()).toString().trim();
        if (lokasi.isEmpty()) {
            etLokasi.setError(getString(R.string.error_lokasi_kosong));
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.konfirmasi_presensi)
                .setMessage(getString(R.string.konfirmasi_isi_presensi,
                        kategoriPresensi,
                        etNama.getText(),
                        lokasi,
                        currentLatLng))
                .setPositiveButton(R.string.kirim, (dialog, which) -> sendDataToGoogleScript())
                .setNegativeButton(R.string.batal, null)
                .show();
    }

    private String convertImageToBase64(Uri uri) throws IOException, ExecutionException, InterruptedException {
        Bitmap bitmap = Glide.with(this)
                .asBitmap()
                .load(uri)
                .submit(800, 800)
                .get();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int quality = 70;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

        while (byteArrayOutputStream.toByteArray().length > MAX_IMAGE_SIZE_KB * 1024 && quality > 20) {
            byteArrayOutputStream.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        }

        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    private void sendDataToGoogleScript() {
        progressDialog.show(); // <-- Tampilkan loading

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String base64Image = convertImageToBase64(fotoUri);

                if (base64Image.length() > 3_000_000) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss(); // Tutup loading kalau error
                        Toast.makeText(this, "Foto terlalu besar, silahkan ambil foto lain", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                json.put("nama", Objects.requireNonNull(etNama.getText()).toString());
                json.put("kategori", kategoriPresensi);
                json.put("lokasi", Objects.requireNonNull(etLokasi.getText()).toString());
                json.put("koordinat", currentLatLng);
                json.put("fotoBase64", base64Image);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://script.google.com/macros/s/AKfycbzBQJ2oiPKR6G-G1WhY5isnvb9D_bAtqpza9rApjSXiW3sRcd0wt8ran46u2WaEEZp4rg/exec")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss(); // Tutup loading
                            Toast.makeText(
                                    PresensiActivity.this,
                                    "Gagal mengirim data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss(); // Tutup loading setelah berhasil/kegagalan
                            try {
                                ResponseBody responseBodyObj = response.body();
                                if (responseBodyObj == null) {
                                    Toast.makeText(PresensiActivity.this, "Response kosong", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String responseBody = responseBodyObj.string();
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    Toast.makeText(
                                            PresensiActivity.this,
                                            "Presensi berhasil!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    finish();
                                } else {
                                    Toast.makeText(
                                            PresensiActivity.this,
                                            "Error: " + jsonResponse.getString("message"),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            } catch (Exception e) {
                                Log.e("PresensiActivity", "Error parsing response", e);
                                Toast.makeText(
                                        PresensiActivity.this,
                                        "Error parsing response",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss(); // Tutup loading jika ada exception
                    Log.e("PresensiActivity", "Error sending data", e);
                    Toast.makeText(PresensiActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            fotoUri = data.getData();
            Glide.with(this).load(fotoUri).into(ivSelfie);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, R.string.izin_lokasi_diperlukan, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
