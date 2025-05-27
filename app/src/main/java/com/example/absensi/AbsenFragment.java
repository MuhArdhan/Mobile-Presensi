package com.example.absensi; // Pastikan package sesuai

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

public class AbsenFragment extends Fragment {

    private static final String ARG_TYPE = "presensi_type"; // Kunci untuk argumen tipe presensi

    private String presensiType; // Akan menyimpan "Masuk" atau "Pulang"

    // Deklarasi semua View dari layout form presensi
    private ImageView ivSelfie;
    private Button btnTakePhoto;
    private TextInputEditText etNama, etKategori, etLokasi;
    private TextView tvKoordinat;
    private Button btnGetLocation;
    private Button btnSubmit;

    public AbsenFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method untuk membuat instance baru dari fragmen ini.
     * @param type Tipe presensi: "Masuk" atau "Pulang".
     * @return Instance baru dari FormPresensiFragment.
     */
    public static AbsenFragment newInstance(String type) {
        AbsenFragment fragment = new AbsenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dapatkan tipe presensi dari argumen yang diteruskan
        if (getArguments() != null) {
            presensiType = getArguments().getString(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout untuk fragmen ini (layout form presensi Anda)
        View view = inflater.inflate(R.layout.fragment_absen, container, false); // Menggunakan fragment_absen.xml

        // Inisialisasi semua View dari layout
        ivSelfie = view.findViewById(R.id.ivSelfie);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        etNama = view.findViewById(R.id.etNama);
        etKategori = view.findViewById(R.id.etKategori);
        etLokasi = view.findViewById(R.id.etLokasi);
        tvKoordinat = view.findViewById(R.id.tvKoordinat);
        btnGetLocation = view.findViewById(R.id.btnGetLocation);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // Atur teks tombol submit agar sesuai dengan tipe presensi (Masuk/Pulang)
        if (presensiType != null) {
            btnSubmit.setText("Kirim Presensi " + presensiType);
        } else {
            btnSubmit.setText(R.string.kirim_presensi); // Default jika tipe tidak ada
        }

        // TODO: Anda bisa menambahkan logika untuk mengisi data otomatis (nama, kategori) di sini
        // etNama.setText("Nama Pengguna");
        // etKategori.setText("PKL");

        // Listener untuk tombol Ambil Foto
        btnTakePhoto.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Ambil foto untuk presensi " + presensiType, Toast.LENGTH_SHORT).show();
            // Implementasi logika ambil foto di sini
        });

        // Listener untuk tombol Dapatkan Lokasi
        btnGetLocation.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Mendapatkan lokasi untuk presensi " + presensiType, Toast.LENGTH_SHORT).show();
            // Implementasi logika mendapatkan lokasi di sini
            // tvKoordinat.setText("Lat: -6.123, Lon: 106.456");
            // etLokasi.setText("Jl. Contoh No. 123, Kota Contoh");
        });

        // Listener untuk tombol Submit
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Presensi " + presensiType + " berhasil dikirim!", Toast.LENGTH_SHORT).show();
            // Implementasi logika pengiriman data presensi ke database/API
        });

        return view;
    }
}