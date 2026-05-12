package com.zakarialbouhmadi.tweeterclone.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.adapter.ConversationAdapter;
import com.zakarialbouhmadi.tweeterclone.model.Conversation;
import com.zakarialbouhmadi.tweeterclone.util.E2EEManager;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView textViewEmpty;
    private ConversationAdapter adapter;
    private SessionManager sessionManager;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 10000; // 10 seconds
    private static final String CONVERSATIONS_URL = "https://tweeterclone.com.pl/api/get_conversations.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        sessionManager = new SessionManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Messages");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadConversations);

        // Setup auto-refresh
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            loadConversations();
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        };

        // Load conversations
        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void loadConversations() {
        String url = CONVERSATIONS_URL + "?user_id=" + sessionManager.getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray conversationsArray = jsonResponse.getJSONArray("conversations");
                            List<Conversation> conversations = new ArrayList<>();
                            
                            for (int i = 0; i < conversationsArray.length(); i++) {
                                conversations.add(new Conversation(conversationsArray.getJSONObject(i)));
                            }
                            
                            adapter.setConversations(conversations);
                            decryptLastMessages(conversations);

                            // Show/hide empty message
                            if (conversations.isEmpty()) {
                                textViewEmpty.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                textViewEmpty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(this, 
                                    jsonResponse.optString("message", "Error loading conversations"), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading conversations", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void decryptLastMessages(List<Conversation> conversations) {
        int userId = sessionManager.getUserId();
        new Thread(() -> {
            boolean anyDecrypted = false;
            for (Conversation c : conversations) {
                String encKey = c.getLastMessageEncryptedKey();
                if (encKey == null || encKey.isEmpty()) continue;
                try {
                    String plaintext = E2EEManager.decrypt(c.getLastMessage(), encKey, userId);
                    c.setLastMessage(plaintext);
                    anyDecrypted = true;
                } catch (Exception e) {
                    // leave ciphertext as-is
                }
            }
            if (anyDecrypted) {
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        }).start();
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
