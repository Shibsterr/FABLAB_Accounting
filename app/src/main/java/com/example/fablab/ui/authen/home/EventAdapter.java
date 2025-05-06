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

// Adapteris, kas nodrošina pasākumu (event) datu attēlošanu RecyclerView sarakstā
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events; // Saraksts ar notikumiem, ko attēlot
    private final String userStatus; // Lietotāja statuss ("Admin", "Darbinieks", utt.)
    private final OnEventActionListener onEventActionListener; // Klausītājs pogu klikšķiem

    // Konstruktors, kas saņem notikumus, lietotāja statusu un pogu klikšķu klausītāju
    public EventAdapter(List<Event> events, String userStatus, OnEventActionListener onEventActionListener) {
        this.events = events;
        this.userStatus = userStatus;
        this.onEventActionListener = onEventActionListener;
    }

    // Izveido skatītāju (ViewHolder) katram saraksta elementam
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    // Pievieno datus skatītājam konkrētā pozīcijā
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        // Uzstāda virsrakstu un aprakstu ar tulkotu tekstu
        holder.eventDetails.setText(
                holder.itemView.getContext().getString(R.string.titleE, event.getTitle())
                        + "\n" +
                        holder.itemView.getContext().getString(R.string.descriptionE, event.getDescription()));

        // Uzstāda statusu, sākuma un beigu laiku
        holder.event_status.setText(holder.itemView.getContext().getString(R.string.status_event, event.getStatus()));
        holder.startTime.setText(holder.itemView.getContext().getString(R.string.start_time, event.getStartTime()));
        holder.endTime.setText(holder.itemView.getContext().getString(R.string.end_time, event.getEndTime()));

        // Sākotnēji paslēpj visas pogas
        holder.acceptButton.setVisibility(View.GONE);
        holder.declineButton.setVisibility(View.GONE);
        holder.finishButton.setVisibility(View.GONE);

        // Rāda pogas atkarībā no lietotāja statusa
        if ("Admin".equals(userStatus)) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.declineButton.setVisibility(View.VISIBLE);
            holder.finishButton.setVisibility(View.VISIBLE);
        } else if ("Darbinieks".equals(userStatus)) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.finishButton.setVisibility(View.VISIBLE);
        }

        // Poga: Akceptēt pasākumu
        holder.acceptButton.setOnClickListener(v -> {
            if (onEventActionListener != null) {
                onEventActionListener.onAccept(event);
            }
        });

        // Poga: Noraidīt pasākumu
        holder.declineButton.setOnClickListener(v -> {
            if (onEventActionListener != null) {
                onEventActionListener.onDecline(event);
            }
        });

        // Poga: Atzīmēt pasākumu kā pabeigtu
        holder.finishButton.setOnClickListener(v -> {
            if (onEventActionListener != null) {
                onEventActionListener.onFinish(event);
            }
        });
    }

    // Atgriež notikumu skaitu sarakstā
    @Override
    public int getItemCount() {
        return events.size();
    }

    // Interfeiss pogu klikšķu apstrādei
    public interface OnEventActionListener {
        void onAccept(Event event);
        void onDecline(Event event);
        void onFinish(Event event);
    }

    // Skatītāja (ViewHolder) klase, kas satur visus vienības skatītos elementus
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
