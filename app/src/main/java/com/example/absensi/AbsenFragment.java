package com.example.absensi; // Pastikan package sesuai

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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

public class AbsenFragment extends Fragment {

    private static final String ARG_TYPE = "presensi_type"; // Kunci untuk argumen tipe presensi

    private String currentPresensiType; // Akan menyimpan "Masuk" atau "Pulang" yang sedang aktif

    // Deklarasi Views dari header dan tombol tab
    private ImageView btnBack;
    private TextView tvHeader;
    private Button btnMasuk, btnPulang;
    private Button currentSelectedTabButton; // Untuk melacak tombol tab yang aktif

    // Deklarasi Views dari layout form presensi
    private ImageView ivSelfie;
    private Button btnTakePhoto;
    private TextInputEditText etNama, etKategori, etLokasi;
    private TextView tvKoordinat;
    private Button btnGetLocation;
    private Button btnSubmit;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final int MAX_IMAGE_SIZE_KB = 500;
    private String kategoriPresensi;
    private Uri fotoUri;
    private String currentLatLng;
    private FusedLocationProviderClient fusedLocationClient;
    private Thread mUiThread;

    final Handler mHandler = new Handler();

    private ProgressDialog progressDialog;

    public AbsenFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method untuk membuat instance baru dari fragmen ini.
     * @param initialType Tipe presensi awal: "Masuk" atau "Pulang".
     * @return Instance baru dari AbsenFragment.
     */
    public static AbsenFragment newInstance(String initialType) {
        AbsenFragment fragment = new AbsenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, initialType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dapatkan tipe presensi awal dari argumen yang diteruskan
        if (getArguments() != null) {
            currentPresensiType = getArguments().getString(ARG_TYPE);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout untuk fragmen ini
        View view = inflater.inflate(R.layout.fragment_absen, container, false);

        // Inisialisasi Views dari header dan tombol tab
        btnMasuk = view.findViewById(R.id.btnMasuk);
        btnPulang = view.findViewById(R.id.btnPulang);

        // Inisialisasi Views dari form presensi
        ivSelfie = view.findViewById(R.id.ivSelfie);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        etNama = view.findViewById(R.id.etNama);
        etKategori = view.findViewById(R.id.etKategori);
        etLokasi = view.findViewById(R.id.etLokasi);
        tvKoordinat = view.findViewById(R.id.tvKoordinat);
        btnGetLocation = view.findViewById(R.id.btnGetLocation);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // Set listener untuk tombol Masuk dan Pulang
        btnMasuk.setOnClickListener(v -> setPresensiType("Masuk"));
        btnPulang.setOnClickListener(v -> setPresensiType("Pulang"));

        // Set tipe presensi awal saat fragmen dibuat atau direstore
        if (savedInstanceState != null) {
            // Restore tipe dari savedInstanceState jika ada
            String savedType = savedInstanceState.getString(ARG_TYPE);
            setPresensiType(savedType != null ? savedType : "Masuk"); // Default ke Masuk
        } else {
            // Gunakan tipe awal yang diteruskan dari newInstance
            setPresensiType(currentPresensiType != null ? currentPresensiType : "Masuk"); // Default ke Masuk
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etNama.setText(user.getDisplayName());
        }


        // Listener untuk tombol Ambil Foto
        btnTakePhoto.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Ambil foto untuk presensi " + currentPresensiType, Toast.LENGTH_SHORT).show();
            takePhoto();
        });

        // Listener untuk tombol Dapatkan Lokasi
        btnGetLocation.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Mendapatkan lokasi untuk presensi " + currentPresensiType, Toast.LENGTH_SHORT).show();
            getCurrentLocation();
        });

        // Listener untuk tombol Submit
        btnSubmit.setOnClickListener(v -> {
            submitPresensi();
        });

        return view;
    }

    private void getCurrentLocation() {
        // Periksa izin lokasi
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> { // Ganti 'this' dengan 'requireActivity()'
                    if (location != null) {
                        currentLatLng = location.getLatitude() + "," + location.getLongitude();
                        tvKoordinat.setText(getString(R.string.koordinat, currentLatLng));
                        getAddressFromLocation(location);
                    } else {
                        tvKoordinat.setText(R.string.lokasi_tidak_ditemukan);
                        Toast.makeText(requireContext(), R.string.lokasi_tidak_ditemukan, Toast.LENGTH_LONG).show(); // Tambahkan Toast agar lebih jelas
                        etLokasi.setText(""); // Kosongkan lokasi jika tidak ditemukan
                        tvKoordinat.setText(getString(R.string.koordinat_default)); // Set kembali default
                    }
                })
                .addOnFailureListener(requireActivity(), e -> { // Juga ganti 'this' di addOnFailureListener
                    Toast.makeText(requireContext(), "Gagal mendapatkan lokasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvKoordinat.setText(getString(R.string.koordinat_default));
                    etLokasi.setText("");
                    Log.e("AbsenFragment", "Error getting location", e);
                });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireActivity(), Locale.getDefault());
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
            Toast.makeText(requireActivity(), R.string.foto_diperlukan, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLatLng == null) {
            Toast.makeText(requireActivity(), R.string.lokasi_diperlukan, Toast.LENGTH_SHORT).show();
            return;
        }

        String lokasi = Objects.requireNonNull(etLokasi.getText()).toString().trim();
        if (lokasi.isEmpty()) {
            etLokasi.setError(getString(R.string.error_lokasi_kosong));
            return;
        }

        new AlertDialog.Builder(requireActivity())
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
        // Memastikan progressDialog sudah diinisialisasi
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Menyimpan presensi...\nMohon ditunggu sebentar");
            progressDialog.setCancelable(false);
        }
        progressDialog.show(); // Tampilkan loading

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Pastikan fotoUri tidak null sebelum memanggil convertImageToBase64
                if (fotoUri == null) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Foto belum diambil.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String base64Image = null;
                try {
                    base64Image = convertImageToBase64(fotoUri);
                } catch (ExecutionException | InterruptedException e) {
                    // Tangani exception dari Glide.get()
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e("AbsenFragment", "Error converting image to Base64: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Gagal mengolah foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return;
                }


                // Batasan ukuran Base64 string, bukan byte array
                if (base64Image == null || base64Image.length() > 3_000_000) { // Sekitar 3MB untuk Base64
                    runOnUiThread(() -> {
                        progressDialog.dismiss(); // Tutup loading kalau error
                        Toast.makeText(requireContext(), "Foto terlalu besar atau kosong, silahkan ambil foto lain", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                json.put("nama", Objects.requireNonNull(etNama.getText()).toString());
                json.put("kategori", currentPresensiType);
                json.put("lokasi", Objects.requireNonNull(etLokasi.getText()).toString());
                json.put("koordinat", currentLatLng);
                json.put("fotoBase64", base64Image);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://script.google.com/macros/s/AKfycbybJiPgC__UHLqG5wdjV6nnfQCmBSdxzNCfkl3V7lZskaplikhYUCWUAcL44LtrYRff/exec") // Ganti dengan URL kamu
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> { // Pindah ke UI thread
                            progressDialog.dismiss(); // Tutup loading
                            Toast.makeText(
                                    requireContext(), // Ganti 'PresensiActivity.this' dengan 'requireContext()'
                                    "Gagal mengirim data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        runOnUiThread(() -> { // Pindah ke UI thread
                            progressDialog.dismiss(); // Tutup loading setelah berhasil/kegagalan
                            try {
                                ResponseBody responseBodyObj = response.body();
                                if (responseBodyObj == null) {
                                    Toast.makeText(requireContext(), "Response kosong", Toast.LENGTH_SHORT).show(); // Ganti 'PresensiActivity.this'
                                    return;
                                }

                                String responseBody = responseBodyObj.string();
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    Toast.makeText(
                                            requireContext(), // Ganti 'PresensiActivity.this'
                                            "Presensi berhasil!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    // Di Fragment, Anda tidak bisa langsung memanggil finish().
                                    // Gunakan FragmentManager untuk pop back stack, atau finish Activity.
                                    getParentFragmentManager().popBackStack();
                                } else {
                                    Toast.makeText(
                                            requireContext(), // Ganti 'PresensiActivity.this'
                                            "Error: " + jsonResponse.getString("message"),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            } catch (Exception e) {
                                Log.e("AbsenFragment", "Error parsing response", e);
                                Toast.makeText(
                                        requireContext(), // Ganti 'PresensiActivity.this'
                                        "Error parsing response",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> { // Pindah ke UI thread
                    progressDialog.dismiss(); // Tutup loading jika ada exception
                    Log.e("AbsenFragment", "Error sending data", e);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // Ganti 'PresensiActivity.this'
                });
            }
        });
    }

    private void takePhoto() {
        ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .compress(MAX_IMAGE_SIZE_KB)
                .maxResultSize(800, 800)
                .start(REQUEST_IMAGE_CAPTURE);
    }


    // Metode untuk mengubah tipe presensi dan memperbarui UI
    private void setPresensiType(String type) {
        Log.d("AbsenFragment", "setPresensiType called with type: " + type);
        currentPresensiType = type;

        // Atur state selected untuk tombol
        if ("Masuk".equals(type)) {
            btnMasuk.setSelected(true);
            btnPulang.setSelected(false);
            currentSelectedTabButton = btnMasuk;
        } else {
            btnMasuk.setSelected(false);
            btnPulang.setSelected(true);
            currentSelectedTabButton = btnPulang;
        }

        // Perbarui teks tombol submit
        if (btnSubmit != null) {
            btnSubmit.setText("Kirim Presensi " + currentPresensiType);
        }

        if (etKategori != null) {
            String newCategoryText = "Presensi " + currentPresensiType; // Bentuk teks baru
            etKategori.setText(newCategoryText);
            Log.d("AbsenFragment", "etKategori text set to: " + newCategoryText); // Log 2: Pastikan setText dipanggil
        } else {
            Log.e("AbsenFragment", "ERROR: etKategori is NULL when setting text!"); // Log 3: Ini seharusnya tidak terjadi
        }

        // Misalnya, mengubah warna header atau pesan tertentu.
        Toast.makeText(getContext(), "Form Presensi: " + currentPresensiType, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Simpan tipe presensi yang sedang aktif untuk dipulihkan nanti
        outState.putString(ARG_TYPE, currentPresensiType);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data); // Penting untuk memanggil super

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) { // Gunakan Activity.RESULT_OK
                fotoUri = data.getData();
                if (fotoUri != null) {
                    // 'this' di Glide.with(this) ini benar karena Glide memiliki overload untuk Fragment
                    Glide.with(this).load(fotoUri).into(ivSelfie);
                } else {
                    Toast.makeText(requireContext(), "Gagal mendapatkan URI foto.", Toast.LENGTH_SHORT).show(); // Ganti 'this' dengan 'requireContext()'
                }
            } else if (resultCode == ImagePicker.RESULT_ERROR) { // Jika Anda menggunakan ImagePicker library
                String errorMessage = ImagePicker.Companion.getError(data); // <-- Pertahankan ini atau sesuaikan jika masih error
                Toast.makeText(requireContext(), "Error mengambil gambar: " + errorMessage, Toast.LENGTH_LONG).show(); // Ganti 'this' dengan 'requireContext()'
            } else {
                Toast.makeText(requireContext(), "Pengambilan gambar dibatalkan", Toast.LENGTH_SHORT).show(); // Ganti 'this' dengan 'requireContext()'
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Penting untuk memanggil super

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), R.string.izin_lokasi_diperlukan, Toast.LENGTH_SHORT).show(); // Ganti 'this' dengan 'requireContext()'
                // Tambahkan juga set text default jika izin tidak diberikan
                tvKoordinat.setText(getString(R.string.koordinat_default));
                etLokasi.setText("");
            }
        }
    }
}
