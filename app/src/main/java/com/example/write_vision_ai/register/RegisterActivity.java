package com.example.write_vision_ai.register;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;


import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.write_vision_ai.login.LoginActivity;
import com.example.write_vision_ai.main.MainActivity;
import com.example.write_vision_ai.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {

    Button btn_register;
    EditText name, email, password;
    FirebaseFirestore mFirestore;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    TextView goToLogin; // 👈 Añadimos la referencia

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        this.setTitle("Registro");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registrando usuario...");
        progressDialog.setCancelable(false);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        name = findViewById(R.id.nombre);
        email = findViewById(R.id.correo);
        password = findViewById(R.id.contrasena);
        btn_register = findViewById(R.id.btn_registro);
        goToLogin = findViewById(R.id.goToLogin); // 👈 Conectamos el TextView del layout

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nameUser = name.getText().toString().trim();
                String emailUser = email.getText().toString().trim();
                String passUser = password.getText().toString().trim();

                if (nameUser.isEmpty() || emailUser.isEmpty() || passUser.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                } else if (passUser.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                } else {
                    registerUser(nameUser, emailUser, passUser);
                }
            }
        });

        // 👇 Nueva lógica para ir al login
        goToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser(String nameUser, String emailUser, String passUser) {
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(emailUser, passUser)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String id = mAuth.getCurrentUser().getUid();
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", id);
                            map.put("name", nameUser);
                            map.put("email", emailUser);

                            mFirestore.collection("user").document(id).set(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            progressDialog.dismiss();
                                            Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();

                                            // Cerrar sesión tras el registro
                                            mAuth.signOut();

                                            // Ir al LoginActivity
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            intent.putExtra("register_success", true);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            Toast.makeText(RegisterActivity.this, "El registro fue exitoso", Toast.LENGTH_SHORT).show();

                                            // Cerrar sesión tras el registro
                                            mAuth.signOut();

                                            // Ir al LoginActivity
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            intent.putExtra("register_success", true);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }



    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return false;
    }
}
