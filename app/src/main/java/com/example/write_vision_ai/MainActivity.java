package com.example.write_vision_ai;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.write_vision_ai.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'write_vision_ai' library on application startup.
    static {
        System.loadLibrary("write_vision_ai");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'write_vision_ai' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}