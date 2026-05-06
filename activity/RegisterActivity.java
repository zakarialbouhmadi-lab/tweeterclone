package com.zakarialbouhmadi.tweeterclone.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.zakarialbouhmadi.tweeterclone.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private static final String REGISTER_URL = "https://tweeterclone.com.pl/api/register.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);

        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLogin.setOnClickListener(v -> finish());
    }
    private void registerUser() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Debug log
        Log.d("RegisterActivity", "Attempting registration with: " + email);

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> {
                    // Debug log
                    Log.d("RegisterActivity", "Server Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");

                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();

                        if (success) {
                            finish();
                        }
                    } catch (JSONException e) {
                        Log.e("RegisterActivity", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(RegisterActivity.this,
                                "Server error. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Debug log
                    Log.e("RegisterActivity", "Volley Error: " + error.toString());
                    Toast.makeText(RegisterActivity.this,
                            "Connection error: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }
}

