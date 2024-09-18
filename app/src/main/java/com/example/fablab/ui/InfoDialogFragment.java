    package com.example.fablab.ui;

    import android.os.Bundle;
    import android.text.Html;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.fragment.app.DialogFragment;

    import com.example.fablab.R;

    public class InfoDialogFragment extends DialogFragment {

        private static final String ARG_INFO_KEY = "infoKey";

        public static InfoDialogFragment newInstance(String infoKey) {
            InfoDialogFragment fragment = new InfoDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_INFO_KEY, infoKey);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_info, container, false);

            TextView infoTextView = view.findViewById(R.id.info_text_view);
            Button closeButton = view.findViewById(R.id.button_close);

            String infoKey = getArguments().getString(ARG_INFO_KEY);
            String htmlText = getStringResource(infoKey);
            infoTextView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));

            closeButton.setOnClickListener(v -> dismiss());

            return view;
        }

        private String getStringResource(String key) {
            switch (key) {
                case "code":
                    return getString(R.string.info_code);
                case "stock":
                    return getString(R.string.info_stock);
                case "integer_limit":
                    return getString(R.string.info_izg_code);
                default:
                    return getString(R.string.info_default);
            }
        }
    }
