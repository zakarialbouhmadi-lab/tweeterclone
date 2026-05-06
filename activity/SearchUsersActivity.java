package com.zakarialbouhmadi.tweeterclone.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.adapter.UserAdapter;
import com.zakarialbouhmadi.tweeterclone.model.User;
import com.zakarialbouhmadi.tweeterclone.util.FollowStatusListener;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchUsersActivity extends AppCompatActivity {
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private EditText editTextSearch;
    private List<User> allUsers = new ArrayList<>();
    private static final String GET_USERS_URL = "https://tweeterclone.com.pl/api/get_users.php";
    static FollowStatusListener followStatusListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Search Users");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);

        // Setup RecyclerView
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this);
        recyclerViewUsers.setAdapter(userAdapter);

        // Setup search
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterUsers(s.toString());
            }
        });

        // Load initial users
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUsers();
    }

    private void refreshUsers() {
        loadUsers();
    }

    private void loadUsers() {
        String url = GET_USERS_URL + "?user_id=" + new SessionManager(this).getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray usersArray = jsonResponse.getJSONArray("users");
                            allUsers.clear();
                            for (int i = 0; i < usersArray.length(); i++) {
                                allUsers.add(new User(usersArray.getJSONObject(i)));
                            }
                            userAdapter.setUsers(allUsers);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this,
                        "Error loading users",
                        Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    private void filterUsers(String query) {
        if (query.isEmpty()) {
            userAdapter.setUsers(allUsers);
            return;
        }

        List<User> filteredUsers = allUsers.stream()
                .filter(user -> user.getUsername().toLowerCase()
                        .contains(query.toLowerCase()))
                .collect(Collectors.toList());

        userAdapter.setUsers(filteredUsers);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

