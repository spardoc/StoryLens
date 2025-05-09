package com.example.write_vision_ai.data.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface OnAddTextClickListener {
        void onAddTextClicked(String imageUrl);
    }

    private final List<String> imageUrls;
    private final OnAddTextClickListener listener;

    public ImageAdapter(List<String> imageUrls, OnAddTextClickListener listener) {
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);
        Glide.with(holder.imageView.getContext())
                .load(url)
                .into(holder.imageView);

        holder.btnAddText.setOnClickListener(v -> {
            listener.onAddTextClicked(url);
        });
    }

    @Override public int getItemCount() { return imageUrls.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        Button btnAddText;

        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.imageView);
            btnAddText = view.findViewById(R.id.btnAddText);
        }
    }
}
