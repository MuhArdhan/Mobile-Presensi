package com.example.absensi.model;

public class HistoryItemModel {
    private String type; // "presensi", "perizinan", "cuti"
    private String nama;
    private String userId; // Penting untuk filter

    // Untuk Presensi
    private String tanggalPresensi; // Format dd MMMM yyyy
    private String waktuPresensi;   // Format HH:mm:ss
    private String kategoriPresensi; // "Masuk", "Pulang", "Telat"
    private String lokasi;
    private String koordinat;
    private String imageUrl; // URL foto absensi

    // Untuk Perizinan (Izin)
    private String kategoriIzin;    // Harusnya "Izin"
    private String alasanIzin;
    private String tanggalPengajuanIzin; // Tanggal Pengajuan Perizinan
    private String waktuPengajuanIzin;   // Waktu Pengajuan Perizinan
    private String fileUrlIzin;     // URL file lampiran izin

    // Untuk Cuti
    private String jenisCuti; // Misal: "Cuti Tahunan", "Cuti Sakit"
    private String tanggalMulaiCuti;
    private String tanggalSelesaiCuti;
    private String totalHariCuti;
    private String alasanCuti;
    private String tanggalPengajuanCuti; // Tanggal Pengajuan Cuti
    private String waktuPengajuanCuti;   // Waktu Pengajuan Cuti
    private String fileUrlCuti;     // URL file lampiran cuti

    // Konstruktor fleksibel atau setter untuk mengisi data
    // Anda bisa membuat konstruktor spesifik, atau menggunakan setter

    // Contoh Konstruktor untuk Presensi
    public HistoryItemModel(String type, String nama, String userId, String tanggalPresensi, String waktuPresensi, String kategoriPresensi, String lokasi, String koordinat, String imageUrl) {
        this.type = type;
        this.nama = nama;
        this.userId = userId;
        this.tanggalPresensi = tanggalPresensi;
        this.waktuPresensi = waktuPresensi;
        this.kategoriPresensi = kategoriPresensi;
        this.lokasi = lokasi;
        this.koordinat = koordinat;
        this.imageUrl = imageUrl;
    }

    // Contoh Konstruktor untuk Perizinan (Izin)
    public HistoryItemModel(String type, String nama, String userId, String kategoriIzin, String alasanIzin, String tanggalPengajuanIzin, String waktuPengajuanIzin, String fileUrlIzin) {
        this.type = type;
        this.nama = nama;
        this.userId = userId;
        this.kategoriIzin = kategoriIzin;
        this.alasanIzin = alasanIzin;
        this.tanggalPengajuanIzin = tanggalPengajuanIzin;
        this.waktuPengajuanIzin = waktuPengajuanIzin;
        this.fileUrlIzin = fileUrlIzin;
    }

    // Contoh Konstruktor untuk Cuti
    public HistoryItemModel(String type, String nama, String userId, String jenisCuti, String tanggalMulaiCuti, String tanggalSelesaiCuti, String totalHariCuti, String alasanCuti, String tanggalPengajuanCuti, String waktuPengajuanCuti, String fileUrlCuti) {
        this.type = type;
        this.nama = nama;
        this.userId = userId;
        this.jenisCuti = jenisCuti;
        this.tanggalMulaiCuti = tanggalMulaiCuti;
        this.tanggalSelesaiCuti = tanggalSelesaiCuti;
        this.totalHariCuti = totalHariCuti;
        this.alasanCuti = alasanCuti;
        this.tanggalPengajuanCuti = tanggalPengajuanCuti;
        this.waktuPengajuanCuti = waktuPengajuanCuti;
        this.fileUrlCuti = fileUrlCuti;
    }

    // --- GETTERS untuk semua properti ---
    public String getType() { return type; }
    public String getNama() { return nama; }
    public String getUserId() { return userId; }
    public String getTanggalPresensi() { return tanggalPresensi; }
    public String getWaktuPresensi() { return waktuPresensi; }
    public String getKategoriPresensi() { return kategoriPresensi; }
    public String getLokasi() { return lokasi; }
    public String getKoordinat() { return koordinat; }
    public String getImageUrl() { return imageUrl; }
    public String getKategoriIzin() { return kategoriIzin; }
    public String getAlasanIzin() { return alasanIzin; }
    public String getTanggalPengajuanIzin() { return tanggalPengajuanIzin; }
    public String getWaktuPengajuanIzin() { return waktuPengajuanIzin; }
    public String getFileUrlIzin() { return fileUrlIzin; }
    public String getJenisCuti() { return jenisCuti; }
    public String getTanggalMulaiCuti() { return tanggalMulaiCuti; }
    public String getTanggalSelesaiCuti() { return tanggalSelesaiCuti; }
    public String getTotalHariCuti() { return totalHariCuti; }
    public String getAlasanCuti() { return alasanCuti; }
    public String getTanggalPengajuanCuti() { return tanggalPengajuanCuti; }
    public String getWaktuPengajuanCuti() { return waktuPengajuanCuti; }
    public String getFileUrlCuti() { return fileUrlCuti; }
}