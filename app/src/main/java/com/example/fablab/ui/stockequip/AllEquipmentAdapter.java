package com.example.fablab.ui.stockequip;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fablab.R;
import com.example.fablab.ui.Equipment;

import java.util.List;

public class AllEquipmentAdapter extends RecyclerView.Adapter<AllEquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;

    public AllEquipmentAdapter(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
        Log.d("AllEquipmentAdapter", "Binding equipment: " + equipment.getName());
    }


    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView descriptionTextView;
        private ImageView imageView;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            imageView = itemView.findViewById(R.id.imageView);
            imageView.setClipToOutline(true);
        }

        public void bind(Equipment equipment) {
            nameTextView.setText(equipment.getName());
            descriptionTextView.setText(equipment.getDescription());
            Glide.with(itemView.getContext())
                    .load(equipment.getImageUrl())
                    .into(imageView);
        }
    }
}
//nice