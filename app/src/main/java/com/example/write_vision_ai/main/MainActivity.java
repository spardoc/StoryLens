package com.example.write_vision_ai.main;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
    private final List<Spinner> spinners = new ArrayList<>();
    private LinearLayout storyLayout;
    private TextView tvStoryPreview;

    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME  = 2002;
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
        db    = FirebaseManager.getFirestore();

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

        // Construir el formulario de historia
        buildStoryForm(StoryConstants.basePrompts, StoryConstants.options);

        // Generador de imágenes
        ImageGenerator imageGenerator = new ImageGenerator(
                this,
                spinners,
                StoryConstants.basePrompts,
                imageUrls,
                imageAdapter,
                apiService
        );
        btnGenerate.setOnClickListener(v -> imageGenerator.generateImages());

        // Configurar exportación de PDF
        setupExportPdfButton();
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
        btnGenerate    = findViewById(R.id.btnGenerate);
        btnLogout      = findViewById(R.id.btnLogout);
        btnExportPdf   = findViewById(R.id.btnExportPdf);
        btnVerPdfs     = findViewById(R.id.btnVerPdfs);
        recyclerView   = findViewById(R.id.recyclerView);
        storyLayout    = findViewById(R.id.storyLayout);
        tvStoryPreview = findViewById(R.id.tvStoryPreview);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v ->
                FirebaseManager.logout(new FirebaseManager.OnLogoutListener() {
                    @Override public void onLogoutSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                            finish();
                        });
                    }
                    @Override public void onLogoutFailure(String error) {
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
                    @Override public void onSuccess(File pdfFile) {
                        abrirPdfGenerado(fileName);
                        uploadPdfToFirebase(pdfFile);
                    }
                    @Override public void onFailure(Exception e) {
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
        Uri    uri    = Uri.fromFile(pdfFile);
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

    private void buildStoryForm(String[] storyParts, String[][] options) {
        for (int i = 0; i < storyParts.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(16, 16, 16, 16);

            TextView tvPart = new TextView(this);
            tvPart.setText(storyParts[i]);
            tvPart.setTextSize(18);
            item.addView(tvPart);

            Spinner spinner = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, R.layout.spinner_item, options[i]
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            final int idx = i;
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    updateStoryPreview(idx, p.getItemAtPosition(pos).toString());
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });

            spinners.add(spinner);
            item.addView(spinner);
            storyLayout.addView(item);
        }
    }

    private void updateStoryPreview(int changedIndex, String selection) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spinners.size(); i++) {
            String sel = (i == changedIndex)
                    ? selection
                    : spinners.get(i).getSelectedItem().toString();
            sb.append(String.format(StoryConstants.basePrompts[i], sel))
                    .append("\n\n");
        }
        tvStoryPreview.setText(sb.toString());
    }
}