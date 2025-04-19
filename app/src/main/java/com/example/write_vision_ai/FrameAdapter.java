package com.example.write_vision_ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FrameAdapter extends RecyclerView.Adapter<FrameAdapter.ViewHolder> {
    private List<String> framePaths;
    private Context context;
    private OnFrameSelectedListener listener;

    public interface OnFrameSelectedListener {
        void onFrameSelected(String assetPath);
    }

    public FrameAdapter(Context ctx, List<String> frames, OnFrameSelectedListener listener) {
        this.context = ctx;
        this.framePaths = frames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(180, 180));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String path = framePaths.get(position);
        try {
            InputStream is = context.getAssets().open(path);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            ((ImageView) holder.itemView).setImageBitmap(bitmap);
            holder.itemView.setOnClickListener(v -> listener.onFrameSelected(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return framePaths.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull ImageView itemView) {
            super(itemView);
        }
    }
}
