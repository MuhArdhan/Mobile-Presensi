package com.example.absensi.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.absensi.R;
import com.example.absensi.model.RiwayatModel;

import java.util.ArrayList;

public class RiwayatAdapter extends RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder> {

    private final ArrayList<RiwayatModel> riwayatList;

    public RiwayatAdapter(ArrayList<RiwayatModel> riwayatList) {
        this.riwayatList = riwayatList;
    }

    @NonNull
    @Override
    public RiwayatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_riwayat, parent, false);
        return new RiwayatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RiwayatViewHolder holder, int position) {
        RiwayatModel model = riwayatList.get(position);

        holder.txtNama.setText(model.getNama());
        holder.txtTanggal.setText(model.getTanggal());
        holder.txtWaktu.setText(model.getWaktu());
    }

    @Override
    public int getItemCount() {
        return riwayatList.size();
    }

    public static class RiwayatViewHolder extends RecyclerView.ViewHolder {
        TextView txtNama, txtTanggal, txtWaktu;

        public RiwayatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNama = itemView.findViewById(R.id.txtNama);
            txtTanggal = itemView.findViewById(R.id.txtTanggal);
            txtWaktu = itemView.findViewById(R.id.txtWaktu);
        }
    }
}
