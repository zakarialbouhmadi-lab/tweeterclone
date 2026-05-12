package com.zakarialbouhmadi.tweeterclone.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.recaptcha.Recaptcha;
import com.google.android.recaptcha.RecaptchaAction;
import com.google.android.recaptcha.RecaptchaTasksClient;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private SessionManager sessionManager;
    private TextView textViewRegister;
    private RecaptchaTasksClient recaptchaTasksClient;
    private static final String LOGIN_URL = "https://tweeterclone.com.pl/api/login.php";
    private static final String SITE_KEY = "6LflauUsAAAAADnhveCokPOvE4Cq-OnxJlgl1dnc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);

        buttonLogin.setOnClickListener(v -> loginUser());
        textViewRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        Log.d(TAG, "Initializing reCAPTCHA client...");
        Recaptcha.fetchTaskClient(getApplication(), SITE_KEY)
                .addOnSuccessListener(this, client -> {
                    recaptchaTasksClient = client;
                    Log.d(TAG, "reCAPTCHA client initialized successfully");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "reCAPTCHA init FAILED: " + e.getMessage(), e);
                    Toast.makeText(this, "Security check unavailable", Toast.LENGTH_SHORT).show();
                });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Invalid email address");
            editTextEmail.requestFocus();
            return;
        }

        if (recaptchaTasksClient == null) {
            Log.e(TAG, "executeTask skipped: recaptchaTasksClient is null");
            Toast.makeText(this, "Security check not ready. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonLogin.setEnabled(false);
        Log.d(TAG, "Executing reCAPTCHA token...");

        recaptchaTasksClient.executeTask(RecaptchaAction.LOGIN)
                .addOnSuccessListener(this, token -> {
                    Log.d(TAG, "reCAPTCHA token received, length=" + (token != null ? token.length() : "null"));
                    sendLoginRequest(email, password, token);
                })
                .addOnFailureListener(this, e -> {
                    buttonLogin.setEnabled(true);
                    Log.e(TAG, "reCAPTCHA executeTask FAILED: " + e.getMessage(), e);
                    Toast.makeText(this, "Security check failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendLoginRequest(String email, String password, String recaptchaToken) {
        Log.d(TAG, "Sending login request, token empty=" + (recaptchaToken == null || recaptchaToken.isEmpty()));
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGIN_URL,
                response -> {
                    buttonLogin.setEnabled(true);
                    Log.d(TAG, "Server response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            int userId = jsonResponse.getInt("user_id");
                            String username = jsonResponse.getString("username");
                            sessionManager.createSession(userId, username);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    jsonResponse.getString("message"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this,
                                "Error processing response",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    buttonLogin.setEnabled(true);
                    Log.e(TAG, "Volley error: " + error.toString());
                    Toast.makeText(LoginActivity.this,
                            "Connection error",
                            Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("password", password);
                params.put("recaptcha_token", recaptchaToken);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }
}
