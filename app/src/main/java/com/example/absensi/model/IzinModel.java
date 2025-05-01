package com.example.absensi.model;

public class IzinModel {
    private String nama;
    private String alasan;
    private String waktuPengajuan;

    public IzinModel(String nama, String alasan, String waktuPengajuan) {
        this.nama = nama;
        this.alasan = alasan;
        this.waktuPengajuan = waktuPengajuan;
    }

    public String getNama() {
        return nama;
    }

    public String getAlasan() {
        return alasan;
    }

    public String getWaktuPengajuan() {
        return waktuPengajuan;
    }
}
