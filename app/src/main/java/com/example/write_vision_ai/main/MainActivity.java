package com.example.write_vision_ai.main;


import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.write_vision_ai.R;
import com.example.write_vision_ai.auth.LoginActivity;
import com.example.write_vision_ai.data.ApiService;
import com.example.write_vision_ai.data.adapters.ImageAdapter;
import com.example.write_vision_ai.manager.ApiManager;
import com.example.write_vision_ai.manager.FirebaseManager;
import com.example.write_vision_ai.utils.ImageGenerator;
import com.example.write_vision_ai.utils.PdfGenerator;
import com.example.write_vision_ai.utils.StoryConstants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.*;

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnAddTextClickListener {

    private Button btnGenerate, btnLogout, btnExportPdf, btnVerPdfs;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final List<String> imageUrls = new ArrayList<>();
    private TextView tvInteractiveStory;
    private String[] currentSelections;
    private SpannableStringBuilder interactiveStoryBuilder;

    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME = 2002;
    private int pendingImageIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        // Inicializar Firebase y AppCheck
        FirebaseManager.initialize(this);
        mAuth = FirebaseManager.getAuth();
        db = FirebaseManager.getFirestore();

        // Vistas
        initializeViews();

        // Retrofit
        apiService = ApiManager.createApiService();

        // RecyclerView
        setupRecyclerView();

        // Botón cerrar sesión
        setupLogoutButton();

        // Botón ver PDFs existentes
        btnVerPdfs.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PdfListActivity.class))
        );

        // Inicializar selecciones
        currentSelections = new String[StoryConstants.basePrompts.length];
        for (int i = 0; i < StoryConstants.options.length; i++) {
            currentSelections[i] = StoryConstants.options[i][0];
        }
        Arrays.fill(currentSelections, StoryConstants.options[0][0]); // Valor por defecto

        // Construir historia interactiva
        buildInteractiveStory();

        // Configurar clics en palabras variables
        tvInteractiveStory.setMovementMethod(LinkMovementMethod.getInstance());
        tvInteractiveStory.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int offset = getOffsetForPosition(tvInteractiveStory, event.getX(), event.getY());
                if (offset != -1) {
                    ClickableSpan[] links = interactiveStoryBuilder.getSpans(offset, offset, ClickableSpan.class);
                    if (links.length > 0) {
                        links[0].onClick(tvInteractiveStory);
                        return true;
                    }
                }
            }
            return false;
        });

        // Generador de imágenes
        // Cambia esta línea en el onCreate():
        ImageGenerator imageGenerator = new ImageGenerator(
                this,
                currentSelections, // Ahora es String[] en lugar de List<Spinner>
                StoryConstants.basePrompts,
                imageUrls,
                imageAdapter,
                apiService
        );
        btnGenerate.setOnClickListener(v -> imageGenerator.generateImages());

        // Configurar exportación de PDF
        setupExportPdfButton();
    }

    private void buildInteractiveStory() {
        interactiveStoryBuilder = new SpannableStringBuilder();

        for (int i = 0; i < StoryConstants.basePrompts.length; i++) {
            if (currentSelections[i] == null) {
                currentSelections[i] = StoryConstants.options[i][0];
            }
            String prompt = StoryConstants.basePrompts[i];
            String[] parts = prompt.split("%s");

            // Parte antes del marcador
            interactiveStoryBuilder.append(parts[0]);

            // Palabra variable (clickable)
            int start = interactiveStoryBuilder.length();
            String selection = currentSelections[i] != null ? currentSelections[i] : StoryConstants.options[i][0];
            interactiveStoryBuilder.append(selection);
            interactiveStoryBuilder.setSpan(new VariableWordSpan(i), start, interactiveStoryBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Parte después del marcador (si existe)
            if (parts.length > 1) {
                interactiveStoryBuilder.append(parts[1]);
            }

            interactiveStoryBuilder.append("\n\n");
        }

        tvInteractiveStory.setText(interactiveStoryBuilder);
    }

    private class VariableWordSpan extends ClickableSpan {
        private final int partIndex;

        public VariableWordSpan(int partIndex) {
            this.partIndex = partIndex;
        }

        @Override
        public void onClick(View widget) {
            showSelectionDialog(partIndex);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(Color.BLUE);
            ds.setUnderlineText(true);
        }
    }

    private void showSelectionDialog(int partIndex) {
        new AlertDialog.Builder(this)
                .setTitle("Selecciona una opción")
                .setItems(StoryConstants.options[partIndex], (dialog, which) -> {
                    currentSelections[partIndex] = StoryConstants.options[partIndex][which];
                    buildInteractiveStory();
                })
                .show();
    }

    private int getOffsetForPosition(TextView textView, float x, float y) {
        if (textView.getLayout() == null) return -1;

        int line = textView.getLayout().getLineForVertical((int) y);
        return textView.getLayout().getOffsetForHorizontal(line, x);
    }

    @Override
    public void onAddTextClicked(String imageUrl) {
        pendingImageIndex = imageUrls.indexOf(imageUrl);
        if (pendingImageIndex < 0) return;
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("base_image_url", imageUrl);
        startActivityForResult(intent, REQ_CAPTURE_TEXT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQ_CAPTURE_TEXT || requestCode == REQ_SELECT_FRAME)
                && resultCode == RESULT_OK
                && data != null
                && data.hasExtra("final_image_path")) {

            String path = data.getStringExtra("final_image_path");
            if (path != null && pendingImageIndex >= 0) {
                imageUrls.set(pendingImageIndex, path);
                imageAdapter.notifyItemChanged(pendingImageIndex);
                pendingImageIndex = -1;
            }
        }
    }

    private void initializeViews() {
        btnGenerate = findViewById(R.id.btnGenerate);
        btnLogout = findViewById(R.id.btnLogout);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnVerPdfs = findViewById(R.id.btnVerPdfs);
        recyclerView = findViewById(R.id.recyclerView);
        tvInteractiveStory = findViewById(R.id.tvInteractiveStory);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v ->
                FirebaseManager.logout(new FirebaseManager.OnLogoutListener() {
                    @Override
                    public void onLogoutSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                            finish();
                        });
                    }

                    @Override
                    public void onLogoutFailure(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Error al cerrar sesión: " + error,
                                        Toast.LENGTH_SHORT).show());
                    }
                }));
    }

    private void setupExportPdfButton() {
        btnExportPdf.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("comic_historia.pdf");
            new AlertDialog.Builder(this)
                    .setTitle("Nombre del archivo PDF")
                    .setView(input)
                    .setPositiveButton("Exportar", (d, w) -> {
                        String fileName = input.getText().toString().trim();
                        if (!fileName.endsWith(".pdf")) fileName += ".pdf";
                        exportPdfConNombre(fileName);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void exportPdfConNombre(String fileName) {
        List<String> paths = imageAdapter.getCompositedImages();
        PdfGenerator.generateAndSavePdf(
                this, paths, fileName,
                new PdfGenerator.PdfExportCallback() {
                    @Override
                    public void onSuccess(File pdfFile) {
                        abrirPdfGenerado(fileName);
                        uploadPdfToFirebase(pdfFile);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Error al generar PDF", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void abrirPdfGenerado(String fileName) {
        File pdfFile = new File(getExternalFilesDir(null), fileName);
        Uri uri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicación para abrir PDF",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void uploadPdfToFirebase(File pdfFile) {
        String userId = mAuth.getCurrentUser().getUid();
        Uri uri = Uri.fromFile(pdfFile);
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("pdfs/" + userId + "/" + pdfFile.getName());

        ref.putFile(uri)
                .addOnSuccessListener(t ->
                        Toast.makeText(this, "PDF subido con éxito", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al subir PDF: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}