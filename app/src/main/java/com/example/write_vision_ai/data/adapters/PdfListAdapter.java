package com.example.write_vision_ai.data.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.write_vision_ai.R;

import java.util.List;

public class PdfListAdapter extends ArrayAdapter<String> {

    private final LayoutInflater inflater;

    public PdfListAdapter(@NonNull Context context, @NonNull List<String> pdfNames) {
        super(context, 0, pdfNames);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_pdf, parent, false);
        }

        TextView pdfNameTextView = convertView.findViewById(R.id.pdfNameTextView);
        ImageView pdfIcon = convertView.findViewById(R.id.imageView);

        String pdfName = getItem(position);
        pdfNameTextView.setText(pdfName);

        // Si quieres hacer algo con el ícono, aquí sería (opcional)

        return convertView;
    }
}
