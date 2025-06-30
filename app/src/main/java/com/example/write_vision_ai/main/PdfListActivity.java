package com.example.write_vision_ai.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.write_vision_ai.R;
import com.example.write_vision_ai.data.adapters.PdfListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PdfListActivity extends AppCompatActivity {

    private ListView pdfListView;
    private PdfListAdapter adapter;
    private List<String> pdfNames = new ArrayList<>();
    private List<StorageReference> pdfRefs = new ArrayList<>();
    private FirebaseStorage storage;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_list);

        pdfListView = findViewById(R.id.pdfListView);
        adapter = new PdfListAdapter(this, pdfNames);
        pdfListView.setAdapter(adapter);

        storage = FirebaseStorage.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserPdfs();

        pdfListView.setOnItemClickListener((parent, view, position, id) -> {
            StorageReference ref = pdfRefs.get(position);
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            });
        });
    }

    private void loadUserPdfs() {
        StorageReference userFolder = storage.getReference().child("pdfs/" + userId);
        userFolder.listAll().addOnSuccessListener(listResult -> {
            for (StorageReference item : listResult.getItems()) {
                pdfNames.add(item.getName());
                pdfRefs.add(item);
            }
            adapter.notifyDataSetChanged();
        });
    }
}