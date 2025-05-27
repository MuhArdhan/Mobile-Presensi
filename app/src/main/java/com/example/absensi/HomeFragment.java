package com.example.absensi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Import ini
import androidx.fragment.app.FragmentTransaction; // Import ini

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    private Context mContext;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mContext = getActivity();

        TextView welcomeText = view.findViewById(R.id.textView2);
        ImageButton logoutButton = view.findViewById(R.id.logout_button);
        ImageView presensiButton = view.findViewById(R.id.presensiButton); // Ini adalah tombol "Absen Sekarang"
        ImageView riwayatButton = view.findViewById(R.id.riwayatButton);
        ImageView perizinanButton = view.findViewById(R.id.perizinanButton);
        ImageView comingSoonButton = view.findViewById(R.id.comingSoonButton);

        String name = requireActivity().getIntent().getStringExtra("name");
        if (name != null) {
            welcomeText.setText(name);
        }

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
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


        presensiButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                // Ganti R.id.fragment_container dengan ID container fragmen Anda di layout activity utama
                // Navigasi langsung ke AbsenFragment, default ke "Masuk"
                fragmentTransaction.replace(R.id.fragment_container, AbsenFragment.newInstance("Masuk"));
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Absen", Toast.LENGTH_SHORT).show();
            }
        });
        // --- AKHIR PERUBAHAN ---


        riwayatButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                startActivity(new Intent(mContext, RiwayatActivity.class));
            } else {
                Toast.makeText(mContext, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        perizinanButton.setOnClickListener(v -> showPerizinanDialog());
        comingSoonButton.setOnClickListener(v -> Toast.makeText(mContext, "Fitur dalam pengembangan", Toast.LENGTH_SHORT).show());

        return view;
    }

    // Jika Anda tidak lagi menggunakan dialog presensi, Anda bisa menghapus metode ini.
    // private void showPresensiDialog() { ... }

    private void showPerizinanDialog() {
        View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_perizinan_options, null);
        TextView perizinanPKL = dialogView.findViewById(R.id.perizinanPKL);
        TextView perizinanPKN = dialogView.findViewById(R.id.perizinanPKN);
        AlertDialog dialog = new AlertDialog.Builder(mContext).setView(dialogView).create();

        perizinanPKL.setOnClickListener(v -> {
            dialog.dismiss();
            if (isNetworkAvailable()) {
                Intent intent = new Intent(mContext, PerizinanActivity.class);
                intent.putExtra("KATEGORI", "PKL");
                startActivity(intent);
            } else {
                Toast.makeText(mContext, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        perizinanPKN.setOnClickListener(v -> {
            dialog.dismiss();
            if (isNetworkAvailable()) {
                Intent intent = new Intent(mContext, PerizinanActivity.class);
                intent.putExtra("KATEGORI", "PKN");
                startActivity(intent);
            } else {
                Toast.makeText(mContext, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
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
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }
}