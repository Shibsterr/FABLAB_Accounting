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

    private EditText quantityEditText;
    private TextView dialogTitleTextView;

    private boolean isAddOperation;

    public interface QuantityInputListener {
        void onQuantityInput(int quantity);
    }

    private QuantityInputListener listener;

    public QuantityInputDialogFragment(QuantityInputListener listener, boolean isAddOperation) {
        this.listener = listener;
        this.isAddOperation = isAddOperation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_quantity_input, null);

        dialogTitleTextView = view.findViewById(R.id.dialogTitleTextView);
        quantityEditText = view.findViewById(R.id.quantityEditText);

        if (isAddOperation) {
            dialogTitleTextView.setText(R.string.add_to_the_stock);
        } else {
            dialogTitleTextView.setText(R.string.subtract_from_the_stock);
        }

        builder.setView(view)
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    String quantityStr = quantityEditText.getText().toString().trim();
                    if (quantityStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int quantity = Integer.parseInt(quantityStr);
                    listener.onQuantityInput(quantity);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}
