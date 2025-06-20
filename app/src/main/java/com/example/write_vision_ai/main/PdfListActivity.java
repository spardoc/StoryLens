package com.example.write_vision_ai.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.write_vision_ai.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PdfListActivity extends AppCompatActivity {

    private ListView pdfListView;
    private ArrayAdapter<String> adapter;
    private List<String> pdfNames = new ArrayList<>();
    private List<StorageReference> pdfRefs = new ArrayList<>();
    private FirebaseStorage storage;
    private String userId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_list);

        pdfListView = findViewById(R.id.pdfListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pdfNames);
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
