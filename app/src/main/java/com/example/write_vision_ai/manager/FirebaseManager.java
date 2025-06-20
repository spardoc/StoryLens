package com.example.write_vision_ai.manager;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
//import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {
    private static FirebaseAuth mAuth;
    private static FirebaseFirestore db;

    public static void initialize(Context context) {
        FirebaseApp.initializeApp(context);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        //firebaseAppCheck.installAppCheckProviderFactory(SafetyNetAppCheckProviderFactory.getInstance());
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }


    public static FirebaseAuth getAuth() {
        return mAuth;
    }

    public static FirebaseFirestore getFirestore() {
        return db;
    }

    // Método para cerrar sesión
    public static void logout(OnLogoutListener listener) {
        if (mAuth != null) {
            mAuth.signOut();
            if (listener != null) {
                listener.onLogoutSuccess();
            }
        } else {
            if (listener != null) {
                listener.onLogoutFailure("No hay sesión activa");
            }
        }
    }

    // Interface para callback de logout
    public interface OnLogoutListener {
        void onLogoutSuccess();
        void onLogoutFailure(String errorMessage);
    }
}