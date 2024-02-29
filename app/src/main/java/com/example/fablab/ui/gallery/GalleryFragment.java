package com.example.fablab.ui.gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fablab.R;

public class GalleryFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://calendar.google.com"));

        startActivity(intent);

        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        com.example.fablab.databinding.FragmentGalleryBinding binding = null;
    }
}