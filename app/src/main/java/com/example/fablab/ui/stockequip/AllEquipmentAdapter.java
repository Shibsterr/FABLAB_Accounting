package com.example.fablab.ui.stockequip;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fablab.R;
import com.example.fablab.ui.Equipment;

import java.util.List;

public class AllEquipmentAdapter extends RecyclerView.Adapter<AllEquipmentAdapter.EquipmentViewHolder> {

    // Saraksts ar visiem aprīkojuma objektiem, kas jāparāda
    private static List<Equipment> equipmentList;

    // Konstruktors, lai iestatītu aprīkojuma sarakstu
    public AllEquipmentAdapter(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflato `item_equipment.xml` skatu un izveido skatītāja turētāju
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        // Sasaiste konkrētajai rindai esošo `Equipment` objektu ar skatītāju
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
        Log.d("AllEquipmentAdapter", "Binding equipment: " + equipment.getName());
    }

    @Override
    public int getItemCount() {
        // Atgriež kopējo aprīkojuma vienību skaitu
        return equipmentList.size();
    }

    // Iekšēja klase, kas pārvalda katras vienības skatītāju (ViewHolder)
    public static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView descriptionTextView;
        private ImageView imageView;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);

            // Iniciē skatītājus no XML
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            imageView = itemView.findViewById(R.id.imageView);
            imageView.setClipToOutline(true); // Nodrošina, ka attēls seko formai (piemēram, apaļiem stūriem)

            // Noklikšķināšanas notikums katrai aprīkojuma kartei
            itemView.setOnClickListener(v -> {
                // Iegūst navigācijas kontrolieri
                NavController navController = Navigation.findNavController(v);

                // Iegūst noklikšķināto aprīkojuma objektu
                Equipment equipment = equipmentList.get(getAdapterPosition());
                String equipmentName = equipment.getName();

                Log.d("Equipment Adapter", "Clicked item: " + equipmentName);

                // Izveido pakotni ar informāciju, ko nodot nākamajam fragmentam
                Bundle bundle = new Bundle();
                bundle.putString("allequipname", equipmentName);

                // Veic navigāciju uz specifisko aprīkojuma fragmentu
                navController.navigate(R.id.action_equipmentFragment_to_specificEquipmentFragment, bundle);
            });
        }

        // Metode, lai saistītu datus ar skatītāju komponentēm
        public void bind(Equipment equipment) {
            nameTextView.setText(equipment.getName());
            descriptionTextView.setText(equipment.getDescription());

            // Ielādē aprīkojuma attēlu ar Glide bibliotēku
            Glide.with(itemView.getContext())
                    .load(equipment.getImageUrl())
                    .into(imageView);
        }
    }
}
