package com.example.fablab.ui.stockequip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.fablab.R;

public class QuantityInputDialogFragment extends DialogFragment {

    // Teksta lauks daudzuma ievadei
    private EditText quantityEditText;
    // Virsraksta teksts virs ievades
    private TextView dialogTitleTextView;

    // Norāda, vai šī ir pieskaitīšanas (true) vai atņemšanas (false) darbība
    private boolean isAddOperation;

    // Interfeiss datu nosūtīšanai atpakaļ uz izsaucēju
    public interface QuantityInputListener {
        void onQuantityInput(int quantity);
    }

    // Listener, kurš apstrādā rezultātu
    private QuantityInputListener listener;

    // Konstruktors, kas pieņem listeneri un darbības tipu
    public QuantityInputDialogFragment(QuantityInputListener listener, boolean isAddOperation) {
        this.listener = listener;
        this.isAddOperation = isAddOperation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Izveido AlertDialog būvētāju
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Uzpūš dialoga izkārtojumu no XML
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_quantity_input, null);

        // Inicializē vizuālos komponentus
        dialogTitleTextView = view.findViewById(R.id.dialogTitleTextView);
        quantityEditText = view.findViewById(R.id.quantityEditText);

        // Uzstāda virsrakstu atkarībā no darbības veida
        if (isAddOperation) {
            dialogTitleTextView.setText(R.string.add_to_the_stock);
        } else {
            dialogTitleTextView.setText(R.string.subtract_from_the_stock);
        }

        // Konfigurē dialoga pogas
        builder.setView(view)
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    // Iegūst ievadīto daudzumu
                    String quantityStr = quantityEditText.getText().toString().trim();

                    // Validācija: pārbauda vai ir ievadīts skaitlis
                    if (quantityStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(quantityStr);

                    // Validācija: daudzums nevar būt negatīvs
                    if (quantity < 0) {
                        Toast.makeText(getContext(), "Quantity cannot be negative", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Atgriež daudzumu uz izsaucēju
                    listener.onQuantityInput(quantity);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        // Izveido un atgriež dialogu
        return builder.create();
    }
}
