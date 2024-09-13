package com.example.fablab.ui.authen.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fablab.R;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events;
    private final String userStatus;
    private final OnEventActionListener onEventActionListener;

    public EventAdapter(List<Event> events, String userStatus, OnEventActionListener onEventActionListener) {
        this.events = events;
        this.userStatus = userStatus;
        this.onEventActionListener = onEventActionListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.eventDetails.setText(
                holder.itemView.getContext().getString(R.string.titleE, event.getTitle())
                + "\n" +
                holder.itemView.getContext().getString(R.string.descriptionE, event.getDescription()));

        holder.event_status.setText(holder.itemView.getContext().getString(R.string.status_event, event.getStatus()));
        holder.startTime.setText(holder.itemView.getContext().getString(R.string.start_time, event.getStartTime()));
        holder.endTime.setText(holder.itemView.getContext().getString(R.string.end_time, event.getEndTime()));


        holder.acceptButton.setVisibility(View.GONE);
        holder.declineButton.setVisibility(View.GONE);
        holder.finishButton.setVisibility(View.GONE);

        if ("Admin".equals(userStatus)) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.declineButton.setVisibility(View.VISIBLE);
            holder.finishButton.setVisibility(View.VISIBLE);
        } else if ("Worker".equals(userStatus)) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.finishButton.setVisibility(View.VISIBLE);
        }

        holder.acceptButton.setOnClickListener(v -> {
            if (onEventActionListener != null) {
                onEventActionListener.onAccept(event);
            }
        });

        holder.declineButton.setOnClickListener(v -> {
            if (onEventActionListener != null) {
                onEventActionListener.onDecline(event);
            }
        });

        holder.finishButton.setOnClickListener(v -> {
            if (onEventActionListener != null) {
                onEventActionListener.onFinish(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public interface OnEventActionListener {
        void onAccept(Event event);
        void onDecline(Event event);
        void onFinish(Event event);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventDetails;
        TextView startTime;
        TextView endTime;
        TextView event_status;
        Button acceptButton;
        Button declineButton;
        Button finishButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventDetails = itemView.findViewById(R.id.event_details);
            event_status = itemView.findViewById(R.id.event_status);
            startTime = itemView.findViewById(R.id.start_time);
            endTime = itemView.findViewById(R.id.end_time);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
            finishButton = itemView.findViewById(R.id.finish_button);
        }
    }
}
