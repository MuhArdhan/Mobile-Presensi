package com.example.absensi;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CutiFragment extends Fragment {

    private static final int REQUEST_FILE_PICKER = 1;
    private static final int MAX_FILE_SIZE_KB = 1024; // 1MB max

    // URL Google Apps Script Anda (pastikan ini URL untuk doPost router)
    private static final String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwRN5kSDPHwTTEeNjiXcoe2nZI-zILjJeruiu5FZk3GMrzLAhxcdfQ8KK1ro5BDCz8QJg/exec";

    // Deklarasi View
    private EditText etNameCuti, etStartDateCuti, etEndDateCuti, etReasonCuti;
    private TextView tvTitleCuti, tvTotalDaysCuti, tvFileNameCuti; // tvRemainingQuotaCuti dihapus
    private Button btnSelectFileCuti, btnSubmitCuti;
    private Spinner spinnerCategoryCuti; // Spinner untuk jenis cuti

    // Variabel untuk data cuti
    private Uri fileUri;
    private String selectedCutiType; // Jenis cuti yang dipilih dari Spinner
    private Calendar startDateCalendar; // Menyimpan tanggal mulai yang dipilih
    private Calendar endDateCalendar;   // Menyimpan tanggal selesai yang dipilih

    private ProgressDialog progressDialog;

    public CutiFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cuti, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        // Isi otomatis nama pengguna dari Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etNameCuti.setText(user.getDisplayName());
        } else {
            etNameCuti.setText("Pengguna Tidak Dikenal");
        }
        // Pastikan tidak bisa diedit
        etNameCuti.setEnabled(false);
        etNameCuti.setFocusable(false);
        etNameCuti.setClickable(false);

        // Konfigurasi Spinner Jenis Cuti
        setupCutiTypeSpinner();

        // Listener untuk DatePicker pada EditText tanggal mulai dan selesai
        etStartDateCuti.setOnClickListener(v -> showDatePicker(etStartDateCuti));
        etEndDateCuti.setOnClickListener(v -> showDatePicker(etEndDateCuti));

        // Listener untuk tombol pilih file dan submit
        btnSelectFileCuti.setOnClickListener(v -> selectFile());
        btnSubmitCuti.setOnClickListener(v -> submitCuti());

        // Inisialisasi ProgressDialog
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Mengirim Pengajuan Cuti...\nMohon ditunggu sebentar");
        progressDialog.setCancelable(false);

        // Inisialisasi Calendar
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
    }

    private void initViews(View view) {
        // Semua view diinisialisasi dari 'view' yang di-inflate oleh onCreateView
        spinnerCategoryCuti = view.findViewById(R.id.spinnerCategoryCuti);
        etNameCuti = view.findViewById(R.id.etNameCuti);
        etStartDateCuti = view.findViewById(R.id.etStartDateCuti);
        etEndDateCuti = view.findViewById(R.id.etEndDateCuti);
        etReasonCuti = view.findViewById(R.id.etReasonCuti);
        tvTitleCuti = view.findViewById(R.id.tvTitleCuti);
        tvTotalDaysCuti = view.findViewById(R.id.tvTotalDaysCuti);
        // tvRemainingQuotaCuti = view.findViewById(R.id.tvRemainingQuotaCuti); // Dihapus karena tidak ada di XML
        tvFileNameCuti = view.findViewById(R.id.tvFileNameCuti);
        btnSelectFileCuti = view.findViewById(R.id.btnSelectFileCuti);
        btnSubmitCuti = view.findViewById(R.id.btnSubmitCuti);
    }

    private void setupCutiTypeSpinner() {
        String[] cutiTypes = getResources().getStringArray(R.array.jenis_cuti_array);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cutiTypes
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryCuti.setAdapter(adapter);

        spinnerCategoryCuti.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCutiType = parent.getItemAtPosition(position).toString();
                // Anda bisa menambahkan logika khusus di sini jika jenis cuti mempengaruhi hal lain
                // Misal: jika 'Cuti Sakit', mungkin wajibkan lampiran surat dokter
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCutiType = null; // Tidak ada jenis cuti yang dipilih
            }
        });
    }

    // --- Metode untuk DatePicker ---
    private void showDatePicker(final EditText editText) {
        Calendar c = Calendar.getInstance();
        // Set kalender dialog ke tanggal yang sudah ada di EditText jika ada
        if (editText == etStartDateCuti && startDateCalendar != null) {
            c = (Calendar) startDateCalendar.clone(); // Gunakan clone agar tidak mengubah objek asli
        } else if (editText == etEndDateCuti && endDateCalendar != null) {
            c = (Calendar) endDateCalendar.clone(); // Gunakan clone
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Tanggal yang dipilih
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    // Format tanggal untuk ditampilkan
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(selectedDate.getTime());
                    editText.setText(formattedDate);

                    // Simpan tanggal yang dipilih ke variabel Calendar yang sesuai
                    if (editText == etStartDateCuti) {
                        startDateCalendar = selectedDate;
                    } else if (editText == etEndDateCuti) {
                        endDateCalendar = selectedDate;
                    }

                    // Hitung total hari cuti setelah tanggal diubah
                    calculateTotalDays();
                }, year, month, day);


        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // Mulai dari hari ini

        if (editText == etEndDateCuti && startDateCalendar != null) {
            datePickerDialog.getDatePicker().setMinDate(startDateCalendar.getTimeInMillis());
        }

        datePickerDialog.show();
    }

    // --- Metode untuk Menghitung Total Hari Cuti ---
    private void calculateTotalDays() {
        if (startDateCalendar != null && endDateCalendar != null) {
            // Pastikan tanggal selesai tidak sebelum tanggal mulai
            if (endDateCalendar.before(startDateCalendar)) {
                tvTotalDaysCuti.setText("0 Hari");
                // Toast.makeText(requireContext(), "Tanggal selesai tidak boleh sebelum tanggal mulai", Toast.LENGTH_SHORT).show();
                return;
            }

            long diffMillis = endDateCalendar.getTimeInMillis() - startDateCalendar.getTimeInMillis();
            long days = TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS) + 1; // +1 untuk menghitung hari mulai

            tvTotalDaysCuti.setText(days + " Hari");
        } else {
            tvTotalDaysCuti.setText("0 Hari");
        }
    }

    // --- Metode untuk Memilih File ---
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    // --- Metode untuk Submit Cuti ---
    private void submitCuti() {
        // Validasi input
        if (selectedCutiType == null || selectedCutiType.isEmpty() || selectedCutiType.equals(getResources().getStringArray(R.array.jenis_cuti_array)[0])) {
            // Asumsi item pertama di spinner adalah placeholder atau "Pilih..."
            Toast.makeText(requireContext(), "Pilih jenis cuti", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etStartDateCuti.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Pilih tanggal mulai cuti", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etEndDateCuti.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Pilih tanggal selesai cuti", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startDateCalendar != null && endDateCalendar != null && endDateCalendar.before(startDateCalendar)) {
            Toast.makeText(requireContext(), "Tanggal selesai tidak boleh sebelum tanggal mulai", Toast.LENGTH_SHORT).show();
            return;
        }
        String reason = Objects.requireNonNull(etReasonCuti.getText()).toString().trim();
        if (reason.isEmpty()) {
            etReasonCuti.setError("Alasan cuti harus diisi");
            return;
        }

        // Tampilkan dialog konfirmasi
        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Pengajuan Cuti")
                .setMessage(getString(R.string.konfirmasi_isi_cuti,
                        etNameCuti.getText(), selectedCutiType, etStartDateCuti.getText(),
                        etEndDateCuti.getText(), tvTotalDaysCuti.getText(), reason))
                .setPositiveButton("Kirim", (dialog, which) -> sendDataToGoogleScript())
                .setNegativeButton("Batal", null)
                .show();
    }

    // Membaca byte dari file Uri
    private byte[] getFileBytes(Uri uri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while (true) {
            assert inputStream != null;
            if ((len = inputStream.read(buffer)) == -1) break;
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // Mengirim data cuti ke Google Apps Script
    private void sendDataToGoogleScript() {
        progressDialog.show();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "unknown_user";

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String base64File = "";
                String fileName = "";
                if (fileUri != null) { // Hanya proses file jika ada
                    byte[] fileBytes = getFileBytes(fileUri);
                    if (fileBytes.length > MAX_FILE_SIZE_KB * 1024) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(requireContext(), "File terlalu besar (max 1MB)", Toast.LENGTH_LONG).show();
                            });
                        }
                        return;
                    }
                    base64File = Base64.encodeToString(fileBytes, Base64.DEFAULT);
                    fileName = getFileName(fileUri);
                }

                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                json.put("nama", Objects.requireNonNull(etNameCuti.getText()).toString());
                json.put("kategori", selectedCutiType); // Mengirim jenis cuti yang dipilih
                json.put("tanggalMulai", etStartDateCuti.getText().toString());
                json.put("tanggalSelesai", etEndDateCuti.getText().toString());
                json.put("totalHari", tvTotalDaysCuti.getText().toString());
                json.put("alasan", Objects.requireNonNull(etReasonCuti.getText()).toString());
                json.put("fileBase64", base64File);
                json.put("fileName", fileName);
                json.put("userId", userId);
                json.put("action", "submit_cuti"); // Action baru untuk router Apps Script

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://script.google.com/macros/s/AKfycbybJiPgC__UHLqG5wdjV6nnfQCmBSdxzNCfkl3V7lZskaplikhYUCWUAcL44LtrYRff/exec")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(requireContext(), "Gagal mengirim data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("CutiFragment", "Gagal mengirim data: " + e.getMessage(), e);
                            });
                        }
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                try {
                                    ResponseBody responseBodyObj = response.body();
                                    if (responseBodyObj == null) {
                                        Toast.makeText(requireContext(), "Response kosong", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String responseBody = responseBodyObj.string();
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    if ("success".equals(jsonResponse.getString("status"))) {
                                        Toast.makeText(requireContext(), "Pengajuan cuti berhasil dikirim!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Error: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                                        Log.e("CutiFragment", "Server Error: " + jsonResponse.getString("message"));
                                    }
                                } catch (Exception e) {
                                    Log.e("CutiFragment", "Error parsing response", e);
                                    Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });

            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Log.e("CutiFragment", "Error sending data", e);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(
                            android.provider.OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                Log.e("CutiFragment", "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICKER && resultCode == getActivity().RESULT_OK && data != null) {
            fileUri = data.getData();
            assert fileUri != null;
            String fileName = getFileName(fileUri);
            tvFileNameCuti.setText(fileName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}