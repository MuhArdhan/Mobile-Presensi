package com.example.absensi;

import android.os.Bundle;
import android.util.Log;
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
import com.example.absensi.adapter.HistoryAdapter;
import com.example.absensi.model.HistoryItemModel;
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
import java.util.TimeZone;

public class RiwayatFragment extends Fragment {

    private static final String TAG = "RiwayatFragment";

    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private ArrayList<HistoryItemModel> historyList;

    private static final String URL_BASE_GET_HISTORY = "https://script.google.com/macros/s/AKfycbybJiPgC__UHLqG5wdjV6nnfQCmBSdxzNCfkl3V7lZskaplikhYUCWUAcL44LtrYRff/exec";

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
        Log.d(TAG, "onCreateView: Fragment Riwayat dimulai.");
        return inflater.inflate(R.layout.fragment_riwayat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: View telah dibuat.");

        recyclerView = view.findViewById(R.id.riwayatRecyclerView);
        emptyText = view.findViewById(R.id.emptyText);
        progressBar = view.findViewById(R.id.progressBar);

        btnPresensi = view.findViewById(R.id.btnPresensi);
        btnPerizinan = view.findViewById(R.id.btnPerizinan);
        btnCuti = view.findViewById(R.id.btnCuti);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(historyAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            namaUser = user.getDisplayName();
            userId = user.getUid();
            Log.d(TAG, "User logged in: namaUser=" + namaUser + ", userId=" + userId);
        } else {
            namaUser = "Pengguna Tidak Dikenal";
            userId = "unknown_user";
            Log.w(TAG, "User not logged in. userId set to 'unknown_user'.");
            Toast.makeText(requireContext(), "Harap login untuk melihat riwayat.", Toast.LENGTH_LONG).show();
            showEmptyText(true, "Harap login untuk melihat riwayat.");
            return;
        }

        highlightButton(btnPresensi, btnPerizinan, btnCuti);
        fetchHistory("presensi");

        btnPresensi.setOnClickListener(v -> {
            Log.d(TAG, "btnPresensi clicked. Fetching 'presensi' history.");
            highlightButton(btnPresensi, btnPerizinan, btnCuti);
            fetchHistory("presensi");
        });

        btnPerizinan.setOnClickListener(v -> {
            Log.d(TAG, "btnPerizinan clicked. Fetching 'perizinan' history.");
            highlightButton(btnPerizinan, btnPresensi, btnCuti);
            fetchHistory("perizinan");
        });

        btnCuti.setOnClickListener(v -> {
            Log.d(TAG, "btnCuti clicked. Fetching 'cuti' history.");
            highlightButton(btnCuti, btnPresensi, btnPerizinan);
            fetchHistory("cuti");
        });
    }

    private void highlightButton(Button activeButton, Button... inactiveButtons) {
        activeButton.setAlpha(1f);
        for (Button button : inactiveButtons) {
            button.setAlpha(0.5f);
        }
    }

    private void fetchHistory(String type) {
        historyList.clear();
        historyAdapter.notifyDataSetChanged();

        Log.d(TAG, "fetchHistory: Memuat data riwayat untuk tipe: " + type + ", userId: " + userId);
        showEmptyText(false);
        showLoading(true);

        String url = URL_BASE_GET_HISTORY + "?action=getHistory&type=" + type + "&userId=" + userId;

        Log.d(TAG, "Request URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    showLoading(false);
                    Log.d(TAG, "Volley Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        Log.d(TAG, "Number of items in dataArray (raw from server): " + dataArray.length());

                        if (dataArray.length() == 0) {
                            showEmptyText(true, "Belum ada data " + type + ".");
                            Log.d(TAG, "Data array kosong dari server.");
                            return;
                        }

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);

                            String serverUserId = item.optString("userId", "NO_USER_ID_IN_JSON");
                            Log.d(TAG, "Processing item " + (i + 1) + ": serverUserId = '" + serverUserId + "', current userId = '" + userId + "'");

                            if (serverUserId.equalsIgnoreCase(userId)) {
                                Log.d(TAG, "Matching userId found. Adding item for type: " + type + ". Item JSON: " + item.toString());

                                if (type.equals("presensi")) {
                                    String nama = item.optString("nama", "");
                                    String timestampRaw = item.optString("timestampRaw", ""); // Use timestampRaw for date
                                    String waktuPresensi = item.optString("waktuPresensi", ""); // Already formatted HH:mm:ss from Apps Script
                                    String tanggal = formatTimestampToDateTime(timestampRaw); // Use the general formatter
                                    String kategori = item.optString("kategori", "");
                                    String lokasi = item.optString("lokasi", "");
                                    String koordinat = item.optString("koordinat", "");
                                    String imageUrl = item.optString("imageUrl", "");

                                    Log.d(TAG, "Presensi Data: Nama=" + nama + ", Tanggal=" + tanggal + ", Waktu=" + waktuPresensi + ", Kategori=" + kategori);
                                    historyList.add(new HistoryItemModel(type, nama, userId, tanggal, waktuPresensi, kategori, lokasi, koordinat, imageUrl));

                                } else if (type.equals("perizinan")) {
                                    String nama = item.optString("nama", "");
                                    String kategoriIzin = item.optString("kategori", "");
                                    String alasan = item.optString("alasan", "");
                                    String timestampPengajuanRaw = item.optString("timestampRaw", ""); // Use timestampRaw for date
                                    String waktuPengajuan = item.optString("waktuPengajuan", ""); // Already formatted HH:mm:ss from Apps Script

                                    // Format tanggalPengajuan dari timestampRaw
                                    String tanggalPengajuan = formatTimestampToDateTime(timestampPengajuanRaw);
                                    String fileUrl = item.optString("fileUrl", "");

                                    Log.d(TAG, "Perizinan Data: Nama=" + nama + ", Kategori=" + kategoriIzin + ", Alasan=" + alasan + ", Tanggal Pengajuan=" + tanggalPengajuan + ", Waktu Pengajuan=" + waktuPengajuan);
                                    historyList.add(new HistoryItemModel(type, nama, userId, kategoriIzin, alasan, tanggalPengajuan, waktuPengajuan, fileUrl));

                                } else if (type.equals("cuti")) {
                                    String nama = item.optString("nama", "");
                                    String jenisCuti = item.optString("jenisCuti", "");
                                    // Ambil raw timestamp untuk tanggalMulai dan tanggalSelesai
                                    String rawTanggalMulai = item.optString("tanggalMulai", "");
                                    String rawTanggalSelesai = item.optString("tanggalSelesai", "");

                                    String totalHari = item.optString("totalHari", "");
                                    String alasanCuti = item.optString("alasan", "");
                                    String timestampPengajuanRaw = item.optString("timestampRaw", "");
                                    String waktuPengajuan = item.optString("waktuPengajuan", "");

                                    // Format tanggalMulai dan tanggalSelesai menggunakan fungsi yang sudah ada
                                    String tanggalMulaiFormatted = formatTimestampToDateTime(rawTanggalMulai);
                                    String tanggalSelesaiFormatted = formatTimestampToDateTime(rawTanggalSelesai);
                                    String tanggalPengajuan = formatTimestampToDateTime(timestampPengajuanRaw);

                                    String fileUrl = item.optString("fileUrl", "");

                                    Log.d(TAG, "Cuti Data: Nama=" + nama +
                                            ", Jenis Cuti=" + jenisCuti +
                                            ", Mulai=" + tanggalMulaiFormatted + // Gunakan yang sudah diformat
                                            ", Selesai=" + tanggalSelesaiFormatted + // Gunakan yang sudah diformat
                                            ", Total Hari=" + totalHari +
                                            ", Tanggal Pengajuan=" + tanggalPengajuan +
                                            ", Waktu Pengajuan=" + waktuPengajuan);

                                    // Pastikan HistoryItemModel memiliki constructor yang sesuai
                                    historyList.add(new HistoryItemModel(
                                            type,
                                            nama,
                                            userId,
                                            jenisCuti,
                                            tanggalMulaiFormatted, // Kirim yang sudah diformat
                                            tanggalSelesaiFormatted, // Kirim yang sudah diformat
                                            totalHari,
                                            alasanCuti,
                                            tanggalPengajuan,
                                            waktuPengajuan,
                                            fileUrl
                                    ));
                                }
                            } else {
                                Log.d(TAG, "Skipping item due to userId mismatch.");
                            }
                        }

                        historyAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Total items added to historyList after filter: " + historyList.size());

                        if (historyList.isEmpty()) {
                            showEmptyText(true, "Belum ada data " + type + ".");
                            Log.d(TAG, "historyList kosong setelah filtering. Menampilkan pesan kosong.");
                        } else {
                            showEmptyText(false);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Gagal parsing data riwayat", Toast.LENGTH_SHORT).show();
                        showEmptyText(true, "Error parsing data.");
                    }
                },
                error -> {
                    showLoading(false);
                    error.printStackTrace();
                    Log.e(TAG, "Volley Error: " + error.getMessage(), error);
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
            Log.d(TAG, "Showing empty text: " + message);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            Log.d(TAG, "Hiding empty text.");
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            Log.d(TAG, "Showing ProgressBar.");
        } else {
            progressBar.setVisibility(View.GONE);
            if (emptyText.getVisibility() != View.VISIBLE) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "Hiding ProgressBar.");
        }
    }

    private String formatTimestampToDateTime(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(timestamp);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
            assert date != null;
            String formattedDate = outputFormat.format(date);
            Log.d(TAG, "Formatted timestamp '" + timestamp + "' to: " + formattedDate);
            return formattedDate;
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp: " + timestamp, e);
            return "-";
        }
    }

    // Fungsi formatWaktuPresensi tidak lagi diperlukan jika Apps Script sudah memformatnya
    // private String formatWaktuPresensi(String timestampWaktu) {
    //     try {
    //         SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    //         inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    //         Date date = inputFormat.parse(timestampWaktu);
    //
    //         SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    //         assert date != null;
    //         String formattedTime = outputFormat.format(date);
    //         Log.d(TAG, "Formatted time '" + timestampWaktu + "' to: " + formattedTime);
    //         return formattedTime;
    //     } catch (Exception e) {
    //         Log.e(TAG, "Error formatting time: " + timestampWaktu, e);
    //         return "-";
    //     }
    // }
}