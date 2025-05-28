package com.example.absensi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.absensi.adapter.HistoryAdapter; // Menggunakan HistoryAdapter
import com.example.absensi.model.HistoryItemModel; // Menggunakan HistoryItemModel
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone; // Tambahkan import ini

public class RiwayatFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter; // Ganti dengan HistoryAdapter
    private ArrayList<HistoryItemModel> historyList; // Ganti dengan HistoryItemModel

    // URL GET untuk Riwayat (Presensi, Perizinan, Cuti)
    private static final String URL_BASE_GET_HISTORY = "https://script.google.com/macros/s/AKfycbwRN5kSDPHwTTEeNjiXcoe2nZI-zILjJeruiu5FZk3GMrzLAhxcdfQ8KK1ro5BDCz8QJg/exec"; // Ganti dengan URL doGet Anda

    private Button btnPresensi, btnPerizinan, btnCuti;
    private String namaUser;
    private String userId;

    private TextView emptyText;
    private ProgressBar progressBar;

    public RiwayatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_riwayat, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi View
        recyclerView = view.findViewById(R.id.riwayatRecyclerView);
        emptyText = view.findViewById(R.id.emptyText);
        progressBar = view.findViewById(R.id.progressBar);

        btnPresensi = view.findViewById(R.id.btnPresensi);
        btnPerizinan = view.findViewById(R.id.btnPerizinan);
        btnCuti = view.findViewById(R.id.btnCuti);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inisialisasi ArrayList
        historyList = new ArrayList<>(); // Hanya satu list

        // Inisialisasi Adapter
        historyAdapter = new HistoryAdapter(historyList); // Menggunakan HistoryAdapter
        recyclerView.setAdapter(historyAdapter);

        // Ambil nama dan ID user dari FirebaseAuth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            namaUser = user.getDisplayName();
            userId = user.getUid();
        } else {
            namaUser = "Pengguna Tidak Dikenal";
            userId = "unknown_user";
            // Jika user tidak dikenal, mungkin arahkan ke halaman login atau tampilkan pesan
            Toast.makeText(requireContext(), "Harap login untuk melihat riwayat.", Toast.LENGTH_LONG).show();
            showEmptyText(true, "Harap login untuk melihat riwayat.");
            return; // Penting: keluar dari onViewCreated jika user tidak ada
        }

        // Default tampilkan riwayat Presensi saat fragment dibuat
        highlightButton(btnPresensi, btnPerizinan, btnCuti);
        fetchHistory("presensi"); // Panggil fetchHistory dengan tipe "presensi"

        // Set Listener untuk tombol tab
        btnPresensi.setOnClickListener(v -> {
            highlightButton(btnPresensi, btnPerizinan, btnCuti);
            fetchHistory("presensi");
        });

        btnPerizinan.setOnClickListener(v -> {
            highlightButton(btnPerizinan, btnPresensi, btnCuti);
            fetchHistory("perizinan");
        });

        btnCuti.setOnClickListener(v -> {
            highlightButton(btnCuti, btnPresensi, btnPerizinan);
            fetchHistory("cuti");
        });
    }

    // Metode untuk mengelola highlight tombol tab
    private void highlightButton(Button activeButton, Button... inactiveButtons) {
        activeButton.setAlpha(1f);
        for (Button button : inactiveButtons) {
            button.setAlpha(0.5f);
        }
    }

    // Metode umum untuk mengambil semua jenis riwayat
    private void fetchHistory(String type) {
        historyList.clear(); // Bersihkan list sebelum memuat data baru
        historyAdapter.notifyDataSetChanged(); // Beri tahu adapter bahwa data telah dihapus

        showEmptyText(false);
        showLoading(true);

        String url = URL_BASE_GET_HISTORY;
        url += "?action=getHistory&type=" + type + "&userId=" + userId;

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    showLoading(false);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        if (dataArray.length() == 0) {
                            showEmptyText(true, "Belum ada data " + type + ".");
                            return;
                        }

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);

                            String serverUserId = item.optString("userId", "");

                            // Filter berdasarkan userId yang log-in
                            if (serverUserId.equalsIgnoreCase(userId)) {
                                if (type.equals("presensi")) {
                                    String nama = item.optString("nama", "");
                                    String timestamp = item.optString("timestampRaw", ""); // Gunakan timestampRaw dari Apps Script
                                    String waktuPresensi = item.optString("waktuPresensi", "");
                                    String tanggal = formatTanggal(timestamp);
                                    String kategori = item.optString("kategori", "");
                                    String lokasi = item.optString("lokasi", "");
                                    String koordinat = item.optString("koordinat", "");
                                    String imageUrl = item.optString("imageUrl", "");

                                    historyList.add(new HistoryItemModel(type, nama, userId, tanggal, waktuPresensi, kategori, lokasi, koordinat, imageUrl));

                                } else if (type.equals("perizinan")) {
                                    String nama = item.optString("nama", "");
                                    String kategoriIzin = item.optString("kategori", ""); // Harusnya "Izin"
                                    String alasan = item.optString("alasan", "");
                                    String tanggalPengajuan = item.optString("tanggalPengajuan", "");
                                    String waktuPengajuan = item.optString("waktuPengajuan", "");
                                    String fileUrl = item.optString("fileUrl", "");

                                    historyList.add(new HistoryItemModel(type, nama, userId, kategoriIzin, alasan, tanggalPengajuan, waktuPengajuan, fileUrl));

                                } else if (type.equals("cuti")) {
                                    String nama = item.optString("nama", "");
                                    String jenisCuti = item.optString("jenisCuti", ""); // Key dari Apps Script untuk jenis cuti
                                    String tanggalMulai = item.optString("tanggalMulai", "");
                                    String tanggalSelesai = item.optString("tanggalSelesai", "");
                                    String totalHari = item.optString("totalHari", "");
                                    String alasanCuti = item.optString("alasan", "");
                                    String tanggalPengajuan = item.optString("tanggalPengajuan", "");
                                    String waktuPengajuan = item.optString("waktuPengajuan", "");
                                    String fileUrl = item.optString("fileUrl", "");

                                    historyList.add(new HistoryItemModel(type, nama, userId, jenisCuti, tanggalMulai, tanggalSelesai, totalHari, alasanCuti, tanggalPengajuan, waktuPengajuan, fileUrl));
                                }
                            }
                        }

                        historyAdapter.notifyDataSetChanged(); // Notifikasi adapter setelah data dimuat

                        if (historyList.isEmpty()) {
                            showEmptyText(true, "Belum ada data " + type + ".");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Gagal parsing data riwayat", Toast.LENGTH_SHORT).show();
                        showEmptyText(true, "Error parsing data.");
                    }
                },
                error -> {
                    showLoading(false);
                    error.printStackTrace();
                    Toast.makeText(requireContext(), "Gagal mengambil data riwayat", Toast.LENGTH_SHORT).show();
                    showEmptyText(true, "Gagal memuat data.");
                });

        queue.add(request);
    }

    private void showEmptyText(boolean show) {
        showEmptyText(show, "Belum ada data.");
    }

    private void showEmptyText(boolean show, String message) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(message);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            // Hanya tampilkan RecyclerView jika tidak ada emptyText yang perlu ditampilkan
            if (emptyText.getVisibility() != View.VISIBLE) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Helper untuk format tanggal dari timestamp ISO 8601
    private String formatTanggal(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Penting: TimeZone harus UTC jika Apps Script mengirim 'Z'
            Date date = inputFormat.parse(timestamp);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("id", "ID")); // Tambah jam juga
            assert date != null;
            return outputFormat.format(date);
        } catch (Exception e) {
            android.util.Log.e("RiwayatFragment", "Error formatting timestamp: " + timestamp, e);
            return "-";
        }
    }

    // Normalisasi string (untuk perbandingan)
    private String normalize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }
}