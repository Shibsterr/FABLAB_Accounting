package com.example.fablab.ui.logs;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
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
        private TextView textSummary;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            buttonExpand = itemView.findViewById(R.id.button_expand);
            layoutDetails = itemView.findViewById(R.id.layout_details);
            textUserName = itemView.findViewById(R.id.text_view_username);
            textEmail = itemView.findViewById(R.id.text_view_email);
            textSummary = itemView.findViewById(R.id.text_view_summary);

            buttonExpand.setOnClickListener(v -> toggleDetails());
        }

        public void bind(LogItem logItem) {
            // Iestatiet pogas apzīmējumu atbilstoši žurnāla nosaukumam
            buttonExpand.setText(logItem.getTitle() != null ? logItem.getTitle() : "Žurnāla ieraksts");

            String nameLabel = "Vārds uzvārds: ";
            String name = logItem.getUser();
            SpannableString spannableName = new SpannableString(nameLabel + name);
            spannableName.setSpan(new StyleSpan(Typeface.BOLD), 0, nameLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textUserName.setText(spannableName);

            String emailLabel = "E-pasts: ";
            String email = logItem.getEmail();
            SpannableString spannableEmail = new SpannableString(emailLabel + email);
            spannableEmail.setSpan(new StyleSpan(Typeface.BOLD), 0, emailLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textEmail.setText(spannableEmail);

            if (logItem.getSummary() != null && !logItem.getSummary().isEmpty()) {
                String summaryLabel = "Kopsavilkums: ";
                String summary = logItem.getSummary();
                SpannableString spannableSummary = new SpannableString(summaryLabel + summary);
                spannableSummary.setSpan(new StyleSpan(Typeface.BOLD), 0, summaryLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textSummary.setText(spannableSummary);
                textSummary.setVisibility(View.VISIBLE);
            } else {
                textSummary.setText("");
                textSummary.setVisibility(View.GONE);
            }
        }

        private void toggleDetails() {
            if (layoutDetails.getVisibility() == View.VISIBLE) {
                layoutDetails.setVisibility(View.GONE);
            } else {
                layoutDetails.setVisibility(View.VISIBLE);
            }
        }
    }
}
