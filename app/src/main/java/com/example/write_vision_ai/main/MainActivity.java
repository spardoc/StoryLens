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

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnImageActionListener {

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
    private FrameLayout resultsSection;

    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME = 2002;
    private static final int REQUEST_WRITE_STORAGE = 3001;
    private int pendingImageIndex = -1;

    private void initializeApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/") // URL base de OpenAI
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeApiService();

        // Verificar permisos
        checkPermissions();

        // Inicializar Firebase
        FirebaseManager.initialize(this);
        mAuth = FirebaseManager.getAuth();
        db = FirebaseManager.getFirestore();

        // Inicializar vistas
        initializeViews();

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar listeners
        setupListeners();

        // Inicializar selecciones
        initializeSelections();

        // Construir historia inicial
        buildInteractiveStory();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    private void initializeViews() {
        btnGenerate = findViewById(R.id.btnGenerate);
        btnLogout = findViewById(R.id.btnLogout);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnVerPdfs = findViewById(R.id.btnVerPdfs);
        recyclerView = findViewById(R.id.recyclerView);
        tvInteractiveStory = findViewById(R.id.tvInteractiveStory);
        resultsSection = findViewById(R.id.resultsSection);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupListeners() {
        // Inicializar ApiService ANTES de usarlo
        apiService = ApiManager.createApiService();

        btnLogout.setOnClickListener(v -> FirebaseManager.logout(new FirebaseManager.OnLogoutListener() {
            @Override public void onLogoutSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                });
            }
            @Override public void onLogoutFailure(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        }));

        btnVerPdfs.setOnClickListener(v -> startActivity(new Intent(this, PdfListActivity.class)));

        btnGenerate.setOnClickListener(v -> {
            // Verificar que apiService no sea nulo
            if (apiService == null) {
                Toast.makeText(this, "Error: Servicio no disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            ImageGenerator imageGenerator = new ImageGenerator(
                    this,
                    currentSelections,
                    StoryConstants.basePrompts,
                    imageUrls,
                    imageAdapter,
                    apiService
            );
            imageGenerator.generateImages();

            ScrollView storyScroll = findViewById(R.id.storyScroll);
            storyScroll.setVisibility(View.GONE); // Oculta historia
            resultsSection.setVisibility(View.VISIBLE);
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
        });

        setupExportPdfButton();
    }

    private void initializeSelections() {
        currentSelections = new String[StoryConstants.basePrompts.length];
        for (int i = 0; i < StoryConstants.options.length; i++) {
            currentSelections[i] = StoryConstants.options[i][0];
        }
    }

    private void buildInteractiveStory() {
        interactiveStoryBuilder = new SpannableStringBuilder();

        for (int i = 0; i < StoryConstants.basePrompts.length; i++) {
            String prompt = StoryConstants.basePrompts[i];
            String[] parts = prompt.split("%s");

            interactiveStoryBuilder.append(parts[0]);

            int start = interactiveStoryBuilder.length();
            String selection = currentSelections[i] != null ? currentSelections[i] : StoryConstants.options[i][0];
            interactiveStoryBuilder.append(selection);
            interactiveStoryBuilder.setSpan(new VariableWordSpan(i), start, interactiveStoryBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (parts.length > 1) {
                interactiveStoryBuilder.append(parts[1]);
            }

            interactiveStoryBuilder.append("\n\n");
        }

        tvInteractiveStory.setText(interactiveStoryBuilder);
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
    }

    private class VariableWordSpan extends ClickableSpan {
        private final int partIndex;
        public VariableWordSpan(int partIndex) { this.partIndex = partIndex; }
        @Override public void onClick(View widget) { showSelectionDialog(partIndex); }
        @Override public void updateDrawState(TextPaint ds) {
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

    // Implementación de ImageAdapter.OnImageActionListener
    @Override
    public void onAddTextClicked(String imageUrl) {
        pendingImageIndex = imageUrls.indexOf(imageUrl);
        if (pendingImageIndex >= 0) {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("base_image_url", imageUrl);
            startActivityForResult(intent, REQ_CAPTURE_TEXT);
        }
    }

    @Override
    public void onExportImageClicked(String imageUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Exportar imagen")
                .setItems(new String[]{"Guardar en galería", "Compartir"}, (dialog, which) -> {
                    if (which == 0) saveImageToGallery(imageUrl);
                    else shareImage(imageUrl);
                })
                .show();
    }

    @Override
    public void onDeleteImageClicked(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar imagen")
                .setMessage("¿Estás seguro?")
                .setPositiveButton("Eliminar", (d, w) -> imageAdapter.removeImage(position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveImageToGallery(String imageUrl) {
        // Implementación para guardar imagen
        Toast.makeText(this, "Imagen guardada en galería", Toast.LENGTH_SHORT).show();
    }

    private void shareImage(String imageUrl) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl));
        startActivity(Intent.createChooser(shareIntent, "Compartir imagen"));
    }

    private void setupExportPdfButton() {
        btnExportPdf.setOnClickListener(v -> {
            if (imageUrls.isEmpty()) {
                Toast.makeText(this, "No hay imágenes para exportar", Toast.LENGTH_SHORT).show();
                return;
            }

            EditText input = new EditText(this);
            input.setHint("comic_historia.pdf");
            new AlertDialog.Builder(this)
                    .setTitle("Exportar a PDF")
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
        PdfGenerator.generateAndSavePdf(this, imageUrls, fileName, new PdfGenerator.PdfExportCallback() {
            @Override public void onSuccess(File pdfFile) {
                abrirPdfGenerado(fileName);
                uploadPdfToFirebase(pdfFile);
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Error al generar PDF", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirPdfGenerado(String fileName) {
        File pdfFile = new File(getExternalFilesDir(null), fileName);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicación para abrir PDF", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadPdfToFirebase(File pdfFile) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("pdfs/" + mAuth.getCurrentUser().getUid() + "/" + pdfFile.getName());
        ref.putFile(Uri.fromFile(pdfFile))
                .addOnSuccessListener(t -> Toast.makeText(this, "PDF subido", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al subir PDF", Toast.LENGTH_LONG).show());
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
}