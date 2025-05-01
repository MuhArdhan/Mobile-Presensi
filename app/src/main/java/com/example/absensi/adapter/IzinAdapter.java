package com.example.absensi.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.absensi.R;
import com.example.absensi.model.IzinModel;

import java.util.ArrayList;

public class IzinAdapter extends RecyclerView.Adapter<IzinAdapter.ViewHolder> {

    private ArrayList<IzinModel> izinList;

    public IzinAdapter(ArrayList<IzinModel> izinList) {
        this.izinList = izinList;
    }

    @NonNull
    @Override
    public IzinAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_izin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IzinAdapter.ViewHolder holder, int position) {
        IzinModel izin = izinList.get(position);
        holder.namaTextView.setText(izin.getNama());
        holder.alasanTextView.setText(izin.getAlasan());
        holder.waktuTextView.setText(izin.getWaktuPengajuan());
    }

    @Override
    public int getItemCount() {
        return izinList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView namaTextView, alasanTextView, waktuTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            namaTextView = itemView.findViewById(R.id.namaTextView);
            alasanTextView = itemView.findViewById(R.id.alasanTextView);
            waktuTextView = itemView.findViewById(R.id.waktuTextView);
        }
    }
}
