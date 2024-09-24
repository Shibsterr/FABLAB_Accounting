package com.example.fablab.ui.authen.home.stations;

import android.view.View;

import androidx.cardview.widget.CardView;

public class CardViewPosition {
    public int position;
    public View cardView;

    public CardViewPosition() {
    }

    public CardViewPosition(CardView cardView, int position) {
        this.cardView = cardView;
        this.position = position;
    }
}
