package com.example.write_vision_ai.data.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface OnImageActionListener {
        void onAddTextClicked(String imageUrl);
        void onExportImageClicked(String imageUrl);
        void onDeleteImageClicked(int position);
    }

    private final List<String> imageUrls;
    private final OnImageActionListener listener;

    public ImageAdapter(List<String> imageUrls, OnImageActionListener listener) {
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);

        Glide.with(holder.itemView.getContext())
                .load(url.startsWith("file:") ? Uri.parse(url) : url)
                .into(holder.imageView);

        holder.tvImageNumber.setText("Imagen " + (position + 1));

        holder.btnAddText.setOnClickListener(v -> listener.onAddTextClicked(url));
        holder.btnExport.setOnClickListener(v -> listener.onExportImageClicked(url));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteImageClicked(position));
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public void removeImage(int position) {
        imageUrls.remove(position);
        notifyItemRemoved(position);
    }

    public List<String> getCompositedImages() {
        return imageUrls;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        Button btnAddText, btnExport, btnDelete;
        TextView tvImageNumber;

        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.imageView);
            btnAddText = view.findViewById(R.id.btnAddText);
            btnExport = view.findViewById(R.id.btnExport);
            btnDelete = view.findViewById(R.id.btnDelete);
            tvImageNumber = view.findViewById(R.id.tvImageNumber);
        }
    }
}