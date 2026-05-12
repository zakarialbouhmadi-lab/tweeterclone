package com.zakarialbouhmadi.tweeterclone.activity;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private RecaptchaTasksClient recaptchaTasksClient;
    private static final String REGISTER_URL = "https://tweeterclone.com.pl/api/register.php";
    private static final String SITE_KEY = "6LflauUsAAAAADnhveCokPOvE4Cq-OnxJlgl1dnc";

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

        Recaptcha.fetchTaskClient(getApplication(), SITE_KEY)
                .addOnSuccessListener(this, client -> recaptchaTasksClient = client)
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Security check unavailable", Toast.LENGTH_SHORT).show());
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String getPasswordError(String password) {
        if (password.length() < 8)
            return "Password must be at least 8 characters";
        if (!password.matches(".*[A-Z].*"))
            return "Password must contain at least one uppercase letter";
        if (!password.matches(".*[a-z].*"))
            return "Password must contain at least one lowercase letter";
        if (!password.matches(".*[0-9].*"))
            return "Password must contain at least one digit";
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
            return "Password must contain at least one special character";
        return null;
    }

    private void registerUser() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            editTextEmail.setError("Invalid email address");
            editTextEmail.requestFocus();
            return;
        }

        String passwordError = getPasswordError(password);
        if (passwordError != null) {
            editTextPassword.setError(passwordError);
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords don't match");
            editTextConfirmPassword.requestFocus();
            return;
        }

        if (recaptchaTasksClient == null) {
            Toast.makeText(this, "Security check not ready. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonRegister.setEnabled(false);

        recaptchaTasksClient.executeTask(RecaptchaAction.SIGNUP)
                .addOnSuccessListener(this, token -> sendRegisterRequest(username, email, password, token))
                .addOnFailureListener(this, e -> {
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "Security check failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendRegisterRequest(String username, String email, String password, String recaptchaToken) {
        Log.d("RegisterActivity", "Attempting registration with: " + email);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> {
                    buttonRegister.setEnabled(true);
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
                    buttonRegister.setEnabled(true);
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
                params.put("recaptcha_token", recaptchaToken);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }
}
