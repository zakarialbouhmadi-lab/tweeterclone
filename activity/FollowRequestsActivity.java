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
import com.zakarialbouhmadi.tweeterclone.adapter.FollowRequestAdapter;
import com.zakarialbouhmadi.tweeterclone.model.FollowRequest;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FollowRequestsActivity extends AppCompatActivity implements FollowRequestAdapter.OnRequestHandledListener {
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView textViewEmpty;
    private FollowRequestAdapter adapter;
    private SessionManager sessionManager;
    private static final String REQUESTS_URL = "https://tweeterclone.com.pl/api/get_follow_requests.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_requests);

        sessionManager = new SessionManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Follow Requests");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FollowRequestAdapter(this, this);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadRequests);

        // Load requests
        loadRequests();
    }

    private void loadRequests() {
        String url = REQUESTS_URL + "?user_id=" + sessionManager.getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray requestsArray = jsonResponse.getJSONArray("requests");
                            List<FollowRequest> requests = new ArrayList<>();
                            
                            for (int i = 0; i < requestsArray.length(); i++) {
                                requests.add(new FollowRequest(requestsArray.getJSONObject(i)));
                            }
                            
                            adapter.setRequests(requests);
                            
                            // Show/hide empty message
                            if (requests.isEmpty()) {
                                textViewEmpty.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                textViewEmpty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(this, 
                                    jsonResponse.optString("message", "Error loading requests"), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public void onRequestHandled() {
        // Refresh the list when a request is handled
        loadRequests();
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
