// File: app/src/main/java/com/example/absensi/adapter/HistoryAdapter.java
package com.example.absensi.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.absensi.R;
import com.example.absensi.model.HistoryItemModel;
import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private ArrayList<HistoryItemModel> historyList;

    public HistoryAdapter(ArrayList<HistoryItemModel> historyList) {
        this.historyList = historyList;
    }

    // Kita akan menggunakan layout item tunggal untuk semua jenis,
    // lalu menyembunyikan/menampilkan TextView sesuai tipe
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Buat satu layout XML untuk item riwayat yang bisa menampilkan semua jenis data
        // Misalnya, item_history.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItemModel currentItem = historyList.get(position);

        // Reset semua visibilitas untuk setiap item
        holder.tvDate.setVisibility(View.GONE);
        holder.tvTime.setVisibility(View.GONE);
        holder.tvCategory.setVisibility(View.GONE);
        holder.tvReason.setVisibility(View.GONE);
        holder.tvStartDate.setVisibility(View.GONE);
        holder.tvEndDate.setVisibility(View.GONE);
        holder.tvTotalDays.setVisibility(View.GONE);
        holder.tvFileLink.setVisibility(View.GONE);

        // Tampilkan data berdasarkan tipe
        holder.tvName.setText(currentItem.getNama()); // Nama selalu ada

        if (currentItem.getType().equals("presensi")) {
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvCategory.setVisibility(View.VISIBLE);
            holder.tvDate.setText("Tanggal: " + currentItem.getTanggalPresensi());
            holder.tvTime.setText("Waktu: " + currentItem.getWaktuPresensi());
            holder.tvCategory.setText("Kategori: " + currentItem.getKategoriPresensi());
            // Anda bisa tambahkan lokasi, koordinat, gambar jika ingin menampilkan di item ini
            // holder.tvReason.setText("Lokasi: " + currentItem.getLokasi()); // Re-use tvReason jika tidak ada alasan
        } else if (currentItem.getType().equals("perizinan")) {
            holder.tvDate.setVisibility(View.VISIBLE); // Tanggal pengajuan
            holder.tvTime.setVisibility(View.VISIBLE); // Waktu pengajuan
            holder.tvCategory.setVisibility(View.VISIBLE); // Kategori Izin
            holder.tvReason.setVisibility(View.VISIBLE); // Alasan

            holder.tvDate.setText("Diajukan: " + currentItem.getTanggalPengajuanIzin());
            holder.tvTime.setText("Pukul: " + currentItem.getWaktuPengajuanIzin());
            holder.tvCategory.setText("Jenis: " + currentItem.getKategoriIzin());
            holder.tvReason.setText("Detail: " + currentItem.getAlasanIzin());
            // Tambahkan listener untuk klik link file jika ada
        } else if (currentItem.getType().equals("cuti")) {
            holder.tvCategory.setVisibility(View.VISIBLE); // Jenis Cuti
            holder.tvStartDate.setVisibility(View.VISIBLE);
            holder.tvEndDate.setVisibility(View.VISIBLE);
            holder.tvTotalDays.setVisibility(View.VISIBLE);
            holder.tvReason.setVisibility(View.VISIBLE); // Alasan Cuti

            holder.tvCategory.setText("Jenis Cuti: " + currentItem.getJenisCuti());
            holder.tvStartDate.setText("Mulai: " + currentItem.getTanggalMulaiCuti());
            holder.tvEndDate.setText("Selesai: " + currentItem.getTanggalSelesaiCuti());
            holder.tvTotalDays.setText("Total: " + currentItem.getTotalHariCuti());
            holder.tvReason.setText("Detail: " + currentItem.getAlasanCuti());
            // Tambahkan listener untuk klik link file jika ada
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvDate, tvTime, tvCategory, tvReason, tvStartDate, tvEndDate, tvTotalDays, tvFileLink;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvTotalDays = itemView.findViewById(R.id.tvTotalDays);
            tvFileLink = itemView.findViewById(R.id.tvFileLink);
        }
    }
}