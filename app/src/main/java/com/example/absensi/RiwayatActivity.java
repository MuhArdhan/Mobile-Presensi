package com.example.absensi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.absensi.adapter.IzinAdapter;
import com.example.absensi.adapter.RiwayatAdapter;
import com.example.absensi.model.IzinModel;
import com.example.absensi.model.RiwayatModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RiwayatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RiwayatAdapter riwayatAdapter;
    private IzinAdapter izinAdapter;
    private ArrayList<RiwayatModel> riwayatList;
    private ArrayList<IzinModel> izinList;

    private static final String URL_GET_RIWAYAT = "https://script.google.com/macros/s/AKfycbzBQJ2oiPKR6G-G1WhY5isnvb9D_bAtqpza9rApjSXiW3sRcd0wt8ran46u2WaEEZp4rg/exec";
    private static final String URL_GET_PERIZINAN = "https://script.google.com/macros/s/AKfycbwRN5kSDPHwTTEeNjiXcoe2nZI-zILjJeruiu5FZk3GMrzLAhxcdfQ8KK1ro5BDCz8QJg/exec";

    private Button btnPresensi, btnPerizinan;
    private String namaUser;

    private TextView emptyText;
    private ProgressBar progressBar; // Tambahan ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        recyclerView = findViewById(R.id.riwayatRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyText = findViewById(R.id.emptyText);
        progressBar = findViewById(R.id.progressBar); // Sambungkan progressBar

        btnPresensi = findViewById(R.id.btnPresensi);
        btnPerizinan = findViewById(R.id.btnPerizinan);

        riwayatList = new ArrayList<>();
        izinList = new ArrayList<>();

        riwayatAdapter = new RiwayatAdapter(riwayatList);
        izinAdapter = new IzinAdapter(izinList);

        recyclerView.setAdapter(riwayatAdapter);

        // Set tombol kembali di action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Riwayat");
        }

        // Ambil nama user dari FirebaseAuth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            namaUser = user.getDisplayName();
        } else {
            namaUser = "";
        }

        fetchRiwayat();

        btnPresensi.setOnClickListener(v -> {
            recyclerView.setAdapter(riwayatAdapter);
            fetchRiwayat();
            highlightButton(btnPresensi, btnPerizinan);
        });

        btnPerizinan.setOnClickListener(v -> {
            recyclerView.setAdapter(izinAdapter);
            fetchPerizinan();
            highlightButton(btnPerizinan, btnPresensi);
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // untuk kembali ke activity sebelumnya
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void highlightButton(Button activeButton, Button inactiveButton) {
        activeButton.setAlpha(1f);
        inactiveButton.setAlpha(0.5f);
    }

    private void fetchRiwayat() {
        riwayatList.clear();
        showEmptyText(false);
        showLoading(true);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest request = new StringRequest(Request.Method.GET, URL_GET_RIWAYAT,
                response -> {
                    showLoading(false);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);

                            String nama = item.optString("nama", "Tidak ada nama");
                            String timestamp = item.optString("timestamp", "");
                            String waktuPresensi = item.optString("waktuPresensi", "");

                            android.util.Log.d("CEK_NAMA_RIWAYAT", "Server: '" + normalize(nama) + "' vs User: '" + normalize(namaUser) + "'");

                            if (normalize(nama).equalsIgnoreCase(normalize(namaUser))) {
                                String tanggal = formatTanggal(timestamp);
                                riwayatList.add(new RiwayatModel(nama, tanggal, waktuPresensi));
                            }
                        }

                        riwayatAdapter.notifyDataSetChanged();

                        if (riwayatList.isEmpty()) {
                            showEmptyText(true, "Belum ada data presensi.");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(RiwayatActivity.this, "Gagal parsing data presensi", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    showLoading(false);
                    error.printStackTrace();
                    Toast.makeText(RiwayatActivity.this, "Gagal mengambil data presensi", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    private void fetchPerizinan() {
        izinList.clear();
        showEmptyText(false);
        showLoading(true);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest request = new StringRequest(Request.Method.GET, URL_GET_PERIZINAN,
                response -> {
                    showLoading(false);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);

                            String nama = item.optString("nama", "Tidak ada nama");
                            String alasan = item.optString("alasan", "-");
                            String waktuPengajuan = item.optString("waktuPengajuan", "-");

                            android.util.Log.d("CEK_NAMA_IZIN", "Server: '" + normalize(nama) + "' vs User: '" + normalize(namaUser) + "'");

                            if (normalize(nama).equalsIgnoreCase(normalize(namaUser))) {
                                izinList.add(new IzinModel(nama, alasan, waktuPengajuan));
                            }
                        }

                        izinAdapter.notifyDataSetChanged();

                        if (izinList.isEmpty()) {
                            showEmptyText(true, "Belum ada data perizinan.");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(RiwayatActivity.this, "Gagal parsing data perizinan", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    showLoading(false);
                    error.printStackTrace();
                    Toast.makeText(RiwayatActivity.this, "Gagal mengambil data perizinan", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    private void showEmptyText(boolean show) {
        showEmptyText(show, "");
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
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String formatTanggal(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(timestamp);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            assert date != null;
            return outputFormat.format(date);
        } catch (Exception e) {
            return "-";
        }
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }
}
