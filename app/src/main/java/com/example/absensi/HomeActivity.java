package com.example.absensi;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.view.Window;
import android.view.ViewGroup;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inisialisasi komponen UI
        TextView welcomeText = findViewById(R.id.textView2);
        ImageButton logoutButton = findViewById(R.id.logout_button);
        TextView presensiButton = findViewById(R.id.presensiButton);
        TextView riwayatButton = findViewById(R.id.riwayatButton);
        TextView perizinanButton = findViewById(R.id.perizinanButton);
        TextView comingSoonButton = findViewById(R.id.comingSoonButton);

        // Set welcome message dengan nama pengguna
        String name = getIntent().getStringExtra("name");
        if (name != null) {
            welcomeText.setText("Welcome, " + name + "!");
        }

        // Logout button
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(HomeActivity.this, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        });

        // Presensi button
        presensiButton.setOnClickListener(v -> showPresensiDialog());

        // Riwayat button
        riwayatButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                Intent intent = new Intent(HomeActivity.this, RiwayatActivity.class);
                startActivity(intent);
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        // Perizinan button (opsional)
        perizinanButton.setOnClickListener(v -> showPerizinanDialog());

        // Coming soon button
        comingSoonButton.setOnClickListener(v -> Toast.makeText(this, "Fitur dalam pengembangan", Toast.LENGTH_SHORT).show());
    }

    private void showPresensiDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_presensi_options, null);

        TextView presensiPKL = dialogView.findViewById(R.id.presensiPKL);
        TextView presensiPKN = dialogView.findViewById(R.id.presensiPKN);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        presensiPKL.setOnClickListener(v -> {
            dialog.dismiss();
            if (isNetworkAvailable()) {
                Intent intent = new Intent(this, PresensiActivity.class);
                intent.putExtra("KATEGORI", "PKL");
                startActivity(intent);
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        presensiPKN.setOnClickListener(v -> {
            dialog.dismiss();
            if (isNetworkAvailable()) {
                Intent intent = new Intent(this, PresensiActivity.class);
                intent.putExtra("KATEGORI", "PKN");
                startActivity(intent);
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void showPerizinanDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_perizinan_options, null);

        TextView perizinanPKL = dialogView.findViewById(R.id.perizinanPKL);
        TextView perizinanPKN = dialogView.findViewById(R.id.perizinanPKN);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        perizinanPKL.setOnClickListener(v -> {
            dialog.dismiss();
            if (isNetworkAvailable()) {
                Intent intent = new Intent(this, PerizinanActivity.class);
                intent.putExtra("KATEGORI", "PKL");
                startActivity(intent);
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        perizinanPKN.setOnClickListener(v -> {
            dialog.dismiss();
            if (isNetworkAvailable()) {
                Intent intent = new Intent(this, PerizinanActivity.class);
                intent.putExtra("KATEGORI", "PKN");
                startActivity(intent);
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // Fallback untuk versi lama
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    @Override
    public void onBackPressed() {
        // Tampilkan dialog konfirmasi saat tombol back ditekan
        super.onBackPressed();
        new AlertDialog.Builder(this)
                .setTitle("Keluar Aplikasi")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya", (dialog, which) -> finish())
                .setNegativeButton("Tidak", null)
                .show();
    }
}