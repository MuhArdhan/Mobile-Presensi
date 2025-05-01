package com.example.absensi.model;

public class RiwayatModel {
    private final String nama;
    private final String tanggal;
    private final String waktu;

    public RiwayatModel(String nama, String tanggal, String waktu) {
        this.nama = nama;
        this.tanggal = tanggal;
        this.waktu = waktu;
    }

    public String getNama() {
        return nama;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getWaktu() {
        return waktu;
    }
}
