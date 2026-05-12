package com.zakarialbouhmadi.tweeterclone.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.adapter.MessageAdapter;
import com.zakarialbouhmadi.tweeterclone.model.Message;
import com.zakarialbouhmadi.tweeterclone.util.E2EEManager;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageView imageViewProfile;
    private TextView textViewUsername;
    private MessageAdapter adapter;
    private SessionManager sessionManager;

    private int conversationId;
    private int otherUserId;
    private String otherUsername;
    private String otherProfilePic;

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 5000;

    // E2EE
    private String myPublicKeyBase64 = null;
    private String recipientPublicKeyBase64 = null;
    // Cache plaintext so we never run RSA decrypt twice on the same message
    private final ConcurrentHashMap<Integer, String> decryptionCache = new ConcurrentHashMap<>();

    private static final String MESSAGES_URL         = "https://tweeterclone.com.pl/api/get_messages.php";
    private static final String SEND_URL             = "https://tweeterclone.com.pl/api/send_message.php";
    private static final String GET_CONVERSATION_URL = "https://tweeterclone.com.pl/api/get_or_create_conversation.php";
    private static final String GET_PUBLIC_KEY_URL   = "https://tweeterclone.com.pl/api/get_public_key.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sessionManager = new SessionManager(this);

        conversationId  = getIntent().getIntExtra("conversation_id", -1);
        otherUserId     = getIntent().getIntExtra("other_user_id", -1);
        otherUsername   = getIntent().getStringExtra("other_username");
        otherProfilePic = getIntent().getStringExtra("other_profile_pic");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        recyclerView      = findViewById(R.id.recyclerViewMessages);
        editTextMessage   = findViewById(R.id.editTextMessage);
        buttonSend        = findViewById(R.id.buttonSend);
        imageViewProfile  = findViewById(R.id.imageViewProfile);
        textViewUsername  = findViewById(R.id.textViewUsername);

        textViewUsername.setText(otherUsername);
        loadProfileImage();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(this);
        recyclerView.setAdapter(adapter);

        buttonSend.setEnabled(false);
        buttonSend.setOnClickListener(v -> sendMessage());

        refreshHandler  = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            loadMessages(false);
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        };

        // Load own public key and recipient's public key in parallel
        loadOwnPublicKey();
        fetchRecipientPublicKey();

        if (conversationId == -1 && otherUserId != -1) {
            getOrCreateConversation();
        } else {
            loadMessages(true);
        }
    }

    // ---------- E2EE key loading ----------

    private void loadOwnPublicKey() {
        int userId = sessionManager.getUserId();
        new Thread(() -> {
            try {
                E2EEManager.generateKeyPairIfNeeded(userId);
                myPublicKeyBase64 = E2EEManager.getPublicKeyBase64(userId);
                runOnUiThread(this::updateSendButtonState);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load own public key", e);
            }
        }).start();
    }

    private void fetchRecipientPublicKey() {
        if (otherUserId == -1) return;
        String url = GET_PUBLIC_KEY_URL + "?user_id=" + otherUserId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            recipientPublicKeyBase64 = json.getString("public_key");
                        } else {
                            Log.w(TAG, "Recipient has no public key yet: " + json.optString("message"));
                            Toast.makeText(this,
                                    "This user hasn't set up messaging yet. Ask them to log in.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing public key response", e);
                    }
                    updateSendButtonState();
                },
                error -> {
                    Log.e(TAG, "Error fetching recipient public key: " + error);
                    updateSendButtonState();
                });
        Volley.newRequestQueue(this).add(request);
    }

    private void updateSendButtonState() {
        buttonSend.setEnabled(myPublicKeyBase64 != null && recipientPublicKeyBase64 != null);
    }

    // ---------- Profile image ----------

    private void loadProfileImage() {
        if (otherProfilePic != null && !otherProfilePic.isEmpty()) {
            String imageUrl = "https://tweeterclone.com.pl/images/profile/" + otherProfilePic;
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(imageViewProfile);
        } else {
            imageViewProfile.setImageResource(R.drawable.ic_default_profile);
        }
    }

    // ---------- Conversation ----------

    private void getOrCreateConversation() {
        String url = GET_CONVERSATION_URL + "?user_id=" + sessionManager.getUserId()
                + "&other_user_id=" + otherUserId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            conversationId = json.getInt("conversation_id");
                            if (json.has("other_user")) {
                                JSONObject other = json.getJSONObject("other_user");
                                otherUsername   = other.getString("username");
                                otherProfilePic = other.optString("profile_pic", "");
                                textViewUsername.setText(otherUsername);
                                loadProfileImage();
                            }
                            loadMessages(true);
                        } else {
                            Toast.makeText(this,
                                    json.optString("message", "Cannot start conversation"),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error starting conversation", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    // ---------- Lifecycle ----------

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // ---------- Load messages ----------

    private void loadMessages(boolean scrollToBottom) {
        if (conversationId == -1) return;

        String url = MESSAGES_URL + "?conversation_id=" + conversationId
                + "&user_id=" + sessionManager.getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.getBoolean("success")) return;

                        JSONArray arr = json.getJSONArray("messages");
                        List<Message> messages = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            messages.add(new Message(arr.getJSONObject(i)));
                        }

                        // Apply any already-cached plaintexts immediately
                        for (Message m : messages) {
                            String cached = decryptionCache.get(m.getMessageId());
                            if (cached != null) m.setContent(cached);
                        }

                        // Show messages right away (encrypted ones show ciphertext briefly)
                        adapter.setMessages(messages);
                        if (scrollToBottom && !messages.isEmpty()) {
                            recyclerView.scrollToPosition(messages.size() - 1);
                        }

                        // Decrypt any encrypted messages not yet in cache (background)
                        decryptPendingMessages(messages, scrollToBottom);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "Error loading messages: " + error));

        Volley.newRequestQueue(this).add(request);
    }

    private void decryptPendingMessages(List<Message> messages, boolean scrollToBottom) {
        // Collect messages that need decryption
        List<Message> pending = new ArrayList<>();
        for (Message m : messages) {
            if (m.isEncrypted() && !decryptionCache.containsKey(m.getMessageId())) {
                pending.add(m);
            }
        }
        if (pending.isEmpty()) return;

        new Thread(() -> {
            boolean anyDecrypted = false;
            for (Message m : pending) {
                try {
                    String plaintext = E2EEManager.decrypt(m.getContent(), m.getEncryptedKey(), sessionManager.getUserId());
                    decryptionCache.put(m.getMessageId(), plaintext);
                    m.setContent(plaintext);
                    anyDecrypted = true;
                } catch (Exception e) {
                    // Leave ciphertext as-is; cache a sentinel so we don't retry forever
                    decryptionCache.put(m.getMessageId(), m.getContent());
                    Log.e(TAG, "Decryption failed for message " + m.getMessageId(), e);
                }
            }
            if (anyDecrypted) {
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    if (scrollToBottom && !messages.isEmpty()) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                });
            }
        }).start();
    }

    // ---------- Send message ----------

    private void sendMessage() {
        String plaintext = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(plaintext)) return;

        if (myPublicKeyBase64 == null || recipientPublicKeyBase64 == null) {
            Toast.makeText(this,
                    "Encryption keys not ready yet. Please wait a moment.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSend.setEnabled(false);

        // Encrypt on background thread (RSA + AES ops)
        new Thread(() -> {
            try {
                E2EEManager.EncryptedMessage em = E2EEManager.encrypt(
                        plaintext, recipientPublicKeyBase64, myPublicKeyBase64);
                runOnUiThread(() -> sendEncryptedRequest(plaintext, em));
            } catch (Exception e) {
                Log.e(TAG, "Encryption failed", e);
                runOnUiThread(() -> {
                    buttonSend.setEnabled(true);
                    Toast.makeText(this, "Encryption failed. Message not sent.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void sendEncryptedRequest(String plaintext, E2EEManager.EncryptedMessage em) {
        StringRequest request = new StringRequest(Request.Method.POST, SEND_URL,
                response -> {
                    buttonSend.setEnabled(true);
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            editTextMessage.setText("");

                            if (json.has("message_data")) {
                                Message newMessage = new Message(json.getJSONObject("message_data"));
                                // We already know the plaintext — no need to decrypt
                                decryptionCache.put(newMessage.getMessageId(), plaintext);
                                newMessage.setContent(plaintext);
                                adapter.addMessage(newMessage);
                                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                            }

                            if (conversationId == -1) {
                                conversationId = json.getInt("conversation_id");
                            }
                        } else {
                            Toast.makeText(this,
                                    json.optString("message", "Error sending message"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    buttonSend.setEnabled(true);
                    Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("sender_id",                  String.valueOf(sessionManager.getUserId()));
                params.put("receiver_id",                String.valueOf(otherUserId));
                params.put("content",                    em.ciphertext);
                params.put("encrypted_key_for_sender",   em.encryptedKeyForSender);
                params.put("encrypted_key_for_receiver", em.encryptedKeyForReceiver);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
