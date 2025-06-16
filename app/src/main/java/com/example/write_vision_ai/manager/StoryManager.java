package com.example.write_vision_ai.manager;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class StoryManager {

    private final Context context;
    private final LinearLayout storyLayout;
    private final TextView tvStoryPreview;
    private final Button btnPreviewStory;
    private final List<Spinner> spinners = new ArrayList<>();
    private final String[] basePrompts;
    private final String[][] options;

    public StoryManager(Context context, LinearLayout storyLayout,
                        String[] basePrompts, String[][] options) {
        this.context = context;
        this.storyLayout = storyLayout;
        this.basePrompts = basePrompts;
        this.options = options;

        // Crear y configurar el TextView internamente
        tvStoryPreview = new TextView(context);
        tvStoryPreview.setTextSize(16);
        tvStoryPreview.setTextColor(Color.parseColor("#333333"));
        tvStoryPreview.setPadding(0, 24, 0, 8);
        tvStoryPreview.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Crear y configurar el Button internamente
        btnPreviewStory = new Button(context);
        btnPreviewStory.setText("Previsualizar historia");
        btnPreviewStory.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        btnPreviewStory.setOnClickListener(v -> previewStory());
    }

    public void setupStoryBuilder() {
        storyLayout.removeAllViews();
        spinners.clear();

        // Agregar el TextView y el Button al layout
        storyLayout.addView(tvStoryPreview);
        storyLayout.addView(btnPreviewStory);

        // Crear los spinners con sus etiquetas
        for (int i = 0; i < options.length; i++) {
            agregarSpinnerDeVigneta(i + 1, options[i]);
        }
    }

    private void previewStory() {
        StringBuilder storyBuilder = new StringBuilder();
        for (int i = 0; i < spinners.size(); i++) {
            String selection = spinners.get(i).getSelectedItem().toString();
            storyBuilder.append(String.format(basePrompts[i], selection));
        }
        tvStoryPreview.setText(storyBuilder.toString());
    }

    private void agregarSpinnerDeVigneta(int numero, String[] opciones) {
        // Etiqueta
        TextView label = new TextView(context);
        label.setText("Viñeta " + numero);
        label.setTextSize(18);
        label.setTextColor(Color.parseColor("#333333"));
        label.setPadding(0, 24, 0, 8);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        storyLayout.addView(label);

        // Spinner
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                previewStory(); // Actualiza la historia automáticamente
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        storyLayout.addView(spinner);
        spinners.add(spinner);
    }

    public List<Spinner> getSpinners() {
        return spinners;
    }

}
