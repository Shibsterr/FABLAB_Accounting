package com.example.fablab.ui.stockequip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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
            dialogTitleTextView.setText("Add to the stock");
        } else {
            dialogTitleTextView.setText("Subtract from the stock");
        }

        builder.setView(view)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String quantityStr = quantityEditText.getText().toString();
                        int quantity = Integer.parseInt(quantityStr);
                        listener.onQuantityInput(quantity);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }
}
