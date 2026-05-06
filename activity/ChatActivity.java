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
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
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
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds
    
    private static final String MESSAGES_URL = "https://tweeterclone.com.pl/api/get_messages.php";
    private static final String SEND_URL = "https://tweeterclone.com.pl/api/send_message.php";
    private static final String GET_CONVERSATION_URL = "https://tweeterclone.com.pl/api/get_or_create_conversation.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sessionManager = new SessionManager(this);

        // Get intent extras
        conversationId = getIntent().getIntExtra("conversation_id", -1);
        otherUserId = getIntent().getIntExtra("other_user_id", -1);
        otherUsername = getIntent().getStringExtra("other_username");
        otherProfilePic = getIntent().getStringExtra("other_profile_pic");

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        textViewUsername = findViewById(R.id.textViewUsername);

        // Set user info in toolbar
        textViewUsername.setText(otherUsername);
        loadProfileImage();

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup send button
        buttonSend.setOnClickListener(v -> sendMessage());

        // Setup auto-refresh
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            loadMessages(false);
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        };

        // If no conversation ID, get or create one
        if (conversationId == -1 && otherUserId != -1) {
            getOrCreateConversation();
        } else {
            loadMessages(true);
        }
    }

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

    private void getOrCreateConversation() {
        String url = GET_CONVERSATION_URL + "?user_id=" + sessionManager.getUserId() + 
                     "&other_user_id=" + otherUserId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            conversationId = jsonResponse.getInt("conversation_id");
                            
                            if (jsonResponse.has("other_user")) {
                                JSONObject otherUser = jsonResponse.getJSONObject("other_user");
                                otherUsername = otherUser.getString("username");
                                otherProfilePic = otherUser.optString("profile_pic", "");
                                textViewUsername.setText(otherUsername);
                                loadProfileImage();
                            }
                            
                            loadMessages(true);
                        } else {
                            Toast.makeText(this, 
                                    jsonResponse.optString("message", "Cannot start conversation"), 
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error starting conversation", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

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

    private void loadMessages(boolean scrollToBottom) {
        if (conversationId == -1) return;

        String url = MESSAGES_URL + "?conversation_id=" + conversationId + 
                     "&user_id=" + sessionManager.getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray messagesArray = jsonResponse.getJSONArray("messages");
                            List<Message> messages = new ArrayList<>();
                            
                            for (int i = 0; i < messagesArray.length(); i++) {
                                messages.add(new Message(messagesArray.getJSONObject(i)));
                            }
                            
                            adapter.setMessages(messages);
                            
                            if (scrollToBottom && !messages.isEmpty()) {
                                recyclerView.scrollToPosition(messages.size() - 1);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("ChatActivity", "Error loading messages: " + error.toString()));

        Volley.newRequestQueue(this).add(request);
    }

    private void sendMessage() {
        String content = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        // Disable send button temporarily
        buttonSend.setEnabled(false);

        StringRequest request = new StringRequest(Request.Method.POST, SEND_URL,
                response -> {
                    buttonSend.setEnabled(true);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            editTextMessage.setText("");
                            
                            // Add message to adapter
                            if (jsonResponse.has("message_data")) {
                                Message newMessage = new Message(jsonResponse.getJSONObject("message_data"));
                                adapter.addMessage(newMessage);
                                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                            }
                            
                            // Update conversation ID if it was created
                            if (conversationId == -1) {
                                conversationId = jsonResponse.getInt("conversation_id");
                            }
                        } else {
                            Toast.makeText(this, 
                                    jsonResponse.optString("message", "Error sending message"), 
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
                params.put("sender_id", String.valueOf(sessionManager.getUserId()));
                params.put("receiver_id", String.valueOf(otherUserId));
                params.put("content", content);
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
