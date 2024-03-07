package com.example.fablab.ui.logs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fablab.R;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private List<LogItem> logList;

    public LogAdapter(List<LogItem> logList) {
        this.logList = logList;
    }


    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogItem logItem = logList.get(position);
        holder.bind(logItem);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private Button buttonExpand;
        private LinearLayout layoutDetails;
        private TextView textDateTime;
        private TextView textUserName;
        private TextView textEmail;
        private TextView textItemName;
        private TextView textQuantity;
        private TextView textAddition;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            buttonExpand = itemView.findViewById(R.id.button_expand);
            layoutDetails = itemView.findViewById(R.id.layout_details);
            textDateTime = itemView.findViewById(R.id.text_view_summary);
            textUserName = itemView.findViewById(R.id.text_view_username);
            textEmail = itemView.findViewById(R.id.text_view_email);
            textItemName = itemView.findViewById(R.id.text_view_item_name);
            textQuantity = itemView.findViewById(R.id.text_view_quantity);
            textAddition = itemView.findViewById(R.id.text_view_addition);

            // Set OnClickListener for the button
            buttonExpand.setOnClickListener(v -> toggleDetails());
        }

        public void bind(LogItem logItem) {
            // Set text of button to date, time, and username
            buttonExpand.setText(logItem.getDateTime() + " " + logItem.getUserName());

            // Populate detailed information
            textDateTime.setText("Date and Time: " + logItem.getDateTime());
            textUserName.setText("User Name: " + logItem.getUserName());
            textEmail.setText("Email: " + logItem.getEmail());
            textItemName.setText("Item Name: " + logItem.getItemName());
            textQuantity.setText("Quantity: " + logItem.getQuantity());
            textAddition.setText("Addition: " + (logItem.isAddition() ? "Yes" : "No"));
        }

        // Method to toggle visibility of details
        private void toggleDetails() {
            if (layoutDetails.getVisibility() == View.VISIBLE) {
                layoutDetails.setVisibility(View.GONE);
            } else {
                layoutDetails.setVisibility(View.VISIBLE);
            }
        }
    }
}
