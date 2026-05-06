package com.zakarialbouhmadi.tweeterclone.activity;

import android.os.Bundle;
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
import com.zakarialbouhmadi.tweeterclone.adapter.UserAdapter;
import com.zakarialbouhmadi.tweeterclone.model.User;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FollowListActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_TYPE = "type"; // "followers" or "following"
    public static final String TYPE_FOLLOWERS = "followers";
    public static final String TYPE_FOLLOWING = "following";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView textViewEmpty;
    private UserAdapter adapter;
    private SessionManager sessionManager;
    private int userId;
    private String type;

    private static final String FOLLOWERS_URL = "https://tweeterclone.com.pl/api/get_followers.php";
    private static final String FOLLOWING_URL = "https://tweeterclone.com.pl/api/get_following.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);

        sessionManager = new SessionManager(this);
        userId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        type = getIntent().getStringExtra(EXTRA_TYPE);

        if (type == null) {
            type = TYPE_FOLLOWERS;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(type.equals(TYPE_FOLLOWERS) ? "Followers" : "Following");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadList);

        // Load data
        loadList();
    }

    private void loadList() {
        String baseUrl = type.equals(TYPE_FOLLOWERS) ? FOLLOWERS_URL : FOLLOWING_URL;
        String url = baseUrl + "?user_id=" + userId + "&current_user_id=" + sessionManager.getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            String arrayKey = type.equals(TYPE_FOLLOWERS) ? "followers" : "following";
                            JSONArray usersArray = jsonResponse.getJSONArray(arrayKey);
                            List<User> users = new ArrayList<>();
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                users.add(new User(usersArray.getJSONObject(i)));
                            }
                            
                            adapter.setUsers(users);
                            
                            // Show/hide empty message
                            if (users.isEmpty()) {
                                textViewEmpty.setVisibility(View.VISIBLE);
                                textViewEmpty.setText(type.equals(TYPE_FOLLOWERS) ? 
                                        "No followers yet" : "Not following anyone yet");
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                textViewEmpty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(this, 
                                    jsonResponse.optString("message", "Error loading list"), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading list", Toast.LENGTH_SHORT).show();
                });

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
