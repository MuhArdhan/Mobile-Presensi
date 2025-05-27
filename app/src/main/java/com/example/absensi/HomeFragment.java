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
        ImageView perizinanButton = view.findViewById(R.id.perizinanButton); // Ini adalah tombol "Perizinan" / "Izin"
        ImageView comingSoonButton = view.findViewById(R.id.comingSoonButton); // Asumsi ini adalah tombol "Cuti" atau lainnya

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

        // --- Aksi untuk tombol PRESENSI ---
        presensiButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                // Ganti R.id.fragment_container dengan ID container fragmen Anda di layout activity utama
                fragmentTransaction.replace(R.id.fragment_container, new PresensiContainerFragment());
                fragmentTransaction.addToBackStack(null); // Memungkinkan kembali ke HomeFragment
                fragmentTransaction.commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Absen", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Aksi untuk tombol RIWAYAT ---
        riwayatButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) { // Tetap cek koneksi internet jika riwayat membutuhkan data online
                if (mContext != null && getParentFragmentManager() != null) {
                    FragmentManager fragmentManager = getParentFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    // Ganti R.id.fragment_container dengan ID container fragmen Anda di layout activity utama
                    fragmentTransaction.replace(R.id.fragment_container, new RiwayatFragment());
                    fragmentTransaction.addToBackStack(null); // Memungkinkan kembali ke HomeFragment
                    fragmentTransaction.commit();
                } else {
                    Toast.makeText(mContext, "Gagal memuat fragmen Riwayat", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Aksi untuk tombol PERIZINAN (sebagai Izin) ---
        perizinanButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                // Ganti R.id.fragment_container dengan ID container fragmen Anda di layout activity utama
                fragmentTransaction.replace(R.id.fragment_container, new IzinFragment()); // Asumsi IzinFragment untuk perizinan
                fragmentTransaction.addToBackStack(null); // Memungkinkan kembali ke HomeFragment
                fragmentTransaction.commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Izin", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Aksi untuk tombol COMING SOON (sebagai Cuti) ---
        // Jika comingSoonButton ini sebenarnya dimaksudkan untuk Cuti, gunakan ini
        comingSoonButton.setOnClickListener(v -> {
            if (mContext != null && getParentFragmentManager() != null) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                // Ganti R.id.fragment_container dengan ID container fragmen Anda di layout activity utama
                fragmentTransaction.replace(R.id.fragment_container, new CutiFragment()); // Asumsi CutiFragment untuk comingSoon
                fragmentTransaction.addToBackStack(null); // Memungkinkan kembali ke HomeFragment
                fragmentTransaction.commit();
            } else {
                Toast.makeText(mContext, "Gagal memuat fragmen Cuti", Toast.LENGTH_SHORT).show();
            }
        });

        // Jika Anda tetap ingin tombol "Coming Soon" menampilkan toast, gunakan yang ini:
        // comingSoonButton.setOnClickListener(v -> Toast.makeText(mContext, "Fitur dalam pengembangan", Toast.LENGTH_SHORT).show());


        return view;
    }

    // Metode showPresensiDialog() dan showPerizinanDialog() dapat dihapus jika tidak lagi digunakan
    // karena aksinya sudah diganti untuk langsung membuka fragmen.
    /*
    private void showPresensiDialog() {
        // ... (kode dialog presensi sebelumnya)
    }

    private void showPerizinanDialog() {
        // ... (kode dialog perizinan sebelumnya)
    }
    */

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