package com.example.absensi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date; // Import Date
import java.util.Locale; // Import Locale

// Import OkHttp dan Google JSON jika Anda akan mengambil data rekap dari Apps Script
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.Executors; // Untuk menjalankan di background thread

public class HomeFragment extends Fragment {

    private Context mContext;

    // Views untuk Absen Harian
    private TextView jamMasukTextView;
    private TextView jamPulangTextView;

    // Views untuk Rekap Bulanan
    private TextView monthText;
    private TextView yearText;
    private TextView hadirCountTextView;
    private TextView izinCountTextView;
    private TextView cutiCountTextView;
    private TextView telatCountTextView;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // SharedPreferences untuk data harian
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AbsensiPrefs";
    private static final String KEY_LAST_UPDATE_DATE = "lastUpdateDate";
    private static final String KEY_JAM_MASUK = "jamMasuk";
    private static final String KEY_JAM_PULANG = "jamPulang";

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mContext = requireContext(); // Gunakan requireContext() untuk Context yang pasti tidak null
        mAuth = FirebaseAuth.getInstance(); // Inisialisasi FirebaseAuth
        currentUser = mAuth.getCurrentUser(); // Dapatkan user saat ini

        sharedPreferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // --- Inisialisasi Views ---
        TextView welcomeText = view.findViewById(R.id.textView2);
        ImageButton logoutButton = view.findViewById(R.id.logout_button);
        ImageView presensiButton = view.findViewById(R.id.presensiButton);
        ImageView riwayatButton = view.findViewById(R.id.riwayatButton);
        ImageView perizinanButton = view.findViewById(R.id.perizinanButton);
        ImageView comingSoonButton = view.findViewById(R.id.comingSoonButton);

        // Absen Harian
        jamMasukTextView = view.findViewById(R.id.jamMasuk);
        jamPulangTextView = view.findViewById(R.id.jamPulang);

        // Rekap Bulanan
        monthText = view.findViewById(R.id.monthText);
        yearText = view.findViewById(R.id.yearText);
        hadirCountTextView = view.findViewById(R.id.hadirCount);
        izinCountTextView = view.findViewById(R.id.izinCount);
        cutiCountTextView = view.findViewById(R.id.cutiCount);
        telatCountTextView = view.findViewById(R.id.telatCount);
        // --- Akhir Inisialisasi Views ---


        // --- Logika Welcome Text ---
        if (currentUser != null && currentUser.getDisplayName() != null) {
            welcomeText.setText(currentUser.getDisplayName());
        } else {
            welcomeText.setText("User");
        }

        // --- Listener Tombol Navigasi Utama ---
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out dari Firebase
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(mContext, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(mContext, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        });

        // Tombol Presensi (AbsenFragment)
        presensiButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                // Navigasi ke AbsenFragment, default ke "Masuk"
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, AbsenFragment.newInstance("Masuk"))
                        .addToBackStack(null) // Tambahkan ke back stack agar bisa kembali
                        .commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Absen", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol Riwayat (RiwayatFragment)
        riwayatButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                if (mContext != null && getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new RiwayatFragment()) // Asumsi RiwayatFragment
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(mContext, "Gagal memuat fragmen Riwayat", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol Perizinan (IzinFragment)
        perizinanButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new IzinFragment()) // Asumsi IzinFragment
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Izin", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol Coming Soon (CutiFragment)
        comingSoonButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CutiFragment()) // Asumsi CutiFragment
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Cuti", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data setiap kali Fragment ini kembali ke foreground
        updateDailyAttendanceDisplay();
        updateMonthlyRecap();
    }

    // --- LOGIKA ABSENSI HARIAN ---
    private void updateDailyAttendanceDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date()); // Tanggal hari ini

        String lastUpdateDate = sharedPreferences.getString(KEY_LAST_UPDATE_DATE, "");

        // Cek apakah tanggal terakhir update bukan hari ini
        if (!todayDate.equals(lastUpdateDate)) {
            // Jika bukan hari ini, reset data harian
            sharedPreferences.edit().remove(KEY_JAM_MASUK).remove(KEY_JAM_PULANG).apply();
            jamMasukTextView.setText("--:--:--");
            jamPulangTextView.setText("--:--:--");
            Log.d("HomeFragment", "Daily attendance reset for new day.");
        } else {
            // Jika hari ini, tampilkan data yang tersimpan
            String storedJamMasuk = sharedPreferences.getString(KEY_JAM_MASUK, "--:--:--");
            String storedJamPulang = sharedPreferences.getString(KEY_JAM_PULANG, "--:--:--");
            jamMasukTextView.setText(storedJamMasuk);
            jamPulangTextView.setText(storedJamPulang);
            Log.d("HomeFragment", "Daily attendance loaded from SharedPreferences.");
        }

        fetchDailyAttendanceFromBackend();
    }

    // --- LOGIKA PENGAMBILAN ABSENSI HARIAN DARI BACKEND ---
    // Fungsi ini akan query Google Apps Script untuk absen Masuk/Pulang hari ini
    private void fetchDailyAttendanceFromBackend() {
        if (currentUser == null) {
            Log.w("HomeFragment", "User not logged in, cannot fetch daily attendance.");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());
        String userId = currentUser.getUid(); // Dapatkan User ID Firebase

        String url = "https://script.google.com/macros/s/AKfycbybJiPgC__UHLqG5wdjV6nnfQCmBSdxzNCfkl3V7lZskaplikhYUCWUAcL44LtrYRff/exec" +
                "?action=getDailyAttendance" +
                "&userId=" + userId +
                "&date=" + todayDate;

        Executors.newSingleThreadExecutor().execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("HomeFragment", "Failed to fetch daily attendance: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Gagal ambil absen harian", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("HomeFragment", "Daily attendance fetch failed: " + response.message());
                        requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Gagal ambil absen harian", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    try {
                        String responseBody = response.body().string();
                        Log.d("HomeFragment", "Raw daily attendance response: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.optString("status");

                        if (!isAdded()) {
                            Log.w("HomeFragment", "Fragment not attached, skipping daily attendance UI updates.");
                            return; // Exit if fragment is not attached
                        }

                        if ("success".equals(status)) {
                            JSONObject data = jsonResponse.optJSONObject("data");
                            if (data != null) {
                                String jamMasuk = data.optString("jamMasuk", "--:--:--");
                                String jamPulang = data.optString("jamPulang", "--:--:--");

                                // Simpan ke SharedPreferences dan update UI
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(KEY_JAM_MASUK, jamMasuk);
                                editor.putString(KEY_JAM_PULANG, jamPulang);
                                editor.putString(KEY_LAST_UPDATE_DATE, todayDate); // Simpan tanggal hari ini
                                editor.apply();

                                requireActivity().runOnUiThread(() -> {
                                    jamMasukTextView.setText(jamMasuk);
                                    jamPulangTextView.setText(jamPulang);
                                    Log.d("HomeFragment", "Daily attendance updated from backend.");
                                });
                            } else {
                                // Data tidak ditemukan untuk hari ini (belum absen)
                                Log.d("HomeFragment", "No daily attendance data for today.");
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.remove(KEY_JAM_MASUK).remove(KEY_JAM_PULANG).apply(); // Pastikan bersih
                                requireActivity().runOnUiThread(() -> {
                                    if (isAdded()) {
                                        jamMasukTextView.setText("--:--:--");
                                        jamPulangTextView.setText("--:--:--");
                                    }
                                });
                            }
                        } else {
                            Log.e("HomeFragment", "API Status not success: " + jsonResponse.optString("message"));
                            requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Gagal ambil absen harian: " + jsonResponse.optString("message"), Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error parsing daily attendance response: " + e.getMessage(), e);
                        requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Error parsing absen harian", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }


    // --- LOGIKA REKAP BULANAN ---
    private void updateMonthlyRecap() {
        // 1. Tampilkan Bulan dan Tahun Saat Ini
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", new Locale("id", "ID")); // Format bulan dalam bahasa Indonesia
        String currentMonthName = monthFormat.format(calendar.getTime());
        int currentYear = calendar.get(Calendar.YEAR);

        monthText.setText(currentMonthName);
        yearText.setText(String.valueOf(currentYear));
        Log.d("HomeFragment", "Displaying monthly recap for " + currentMonthName + " " + currentYear);

        // 2. Ambil Data Rekap dari Backend (Firebase/Google Sheet)
        // Ini memerlukan endpoint API di Google Apps Script yang bisa:
        //    - Menerima userId dan bulan/tahun.
        //    - Mengkueri Google Sheet untuk data absensi pengguna tersebut di bulan itu.
        //    - Mengembalikan jumlah Hadir, Izin, Cuti, Telat.
        fetchMonthlyRecapFromBackend(currentMonthName, currentYear);
    }

    // --- LOGIKA PENGAMBILAN REKAP BULANAN DARI BACKEND ---
    private void fetchMonthlyRecapFromBackend(String monthName, int year) {
        if (currentUser == null) {
            Log.w("HomeFragment", "User not logged in, cannot fetch monthly recap.");
            return;
        }

        String userId = currentUser.getUid();

        String url = "https://script.google.com/macros/s/AKfycbybJiPgC__UHLqG5wdjV6nnfQCmBSdxzNCfkl3V7lZskaplikhYUCWUAcL44LtrYRff/exec" +
                "?action=getMonthlyRecap" +
                "&userId=" + userId +
                "&month=" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + // Kirim bulan dalam format angka (1-12)
                "&year=" + year;

        Executors.newSingleThreadExecutor().execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("HomeFragment", "Failed to fetch monthly recap: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Gagal ambil rekap bulanan", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("HomeFragment", "Monthly recap fetch failed: " + response.message());
                        requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Gagal ambil rekap bulanan", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    try {
                        String responseBody = response.body().string();
                        Log.d("HomeFragment", "Raw monthly attendance response: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.optString("status");

                        if (!isAdded()) {
                            Log.w("HomeFragment", "Fragment not attached, skipping UI updates.");
                            return; // Exit if fragment is not attached
                        }

                        StringBuilder debugLogMessage = new StringBuilder("Debug Logs from Script:\n");
                        if (jsonResponse.has("debugLogs")) {
                            org.json.JSONArray logsArray = jsonResponse.getJSONArray("debugLogs");
                            for (int k = 0; k < logsArray.length(); k++) {
                                debugLogMessage.append(logsArray.getString(k)).append("\n");
                            }
                        } else {
                            debugLogMessage.append("No debug logs found in response.");
                        }
                        Log.d("HomeFragment_Debug", debugLogMessage.toString());

                        if ("success".equals(status)) {
                            JSONObject data = jsonResponse.optJSONObject("data");
                            if (data != null) {
                                int hadir = data.optInt("hadir", 0);
                                int izin = data.optInt("izin", 0);
                                int cuti = data.optInt("cuti", 0);
                                int telat = data.optInt("telat", 0);

                                requireActivity().runOnUiThread(() -> {
                                    if (isAdded()) { // Check again before updating UI
                                        hadirCountTextView.setText(hadir + " Hari");
                                        izinCountTextView.setText(izin + " Hari");
                                        cutiCountTextView.setText(cuti + " Hari");
                                        telatCountTextView.setText(telat + " Hari");
                                        Log.d("HomeFragment", "Monthly recap updated from backend.");
                                    }
                                });
                            } else {
                                Log.d("HomeFragment", "No monthly recap data found.");
                                requireActivity().runOnUiThread(() -> {
                                    hadirCountTextView.setText("0 Hari");
                                    izinCountTextView.setText("0 Hari");
                                    cutiCountTextView.setText("0 Hari");
                                    telatCountTextView.setText("0 Hari");
                                });
                            }
                        } else {
                            Log.e("HomeFragment", "API Status not success: " + jsonResponse.optString("message"));
                            requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Gagal ambil rekap bulanan: " + jsonResponse.optString("message"), Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error parsing monthly recap response: " + e.getMessage(), e);
                        requireActivity().runOnUiThread(() -> Toast.makeText(mContext, "Error parsing rekap bulanan", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }

    // --- LOGIKA CEK KONEKSI INTERNET (DARI KODE ANDA SEBELUMNYA) ---
    private boolean isNetworkAvailable() {
        // ... (kode isNetworkAvailable Anda yang sudah ada)
        return true; // Placeholder
    }
}