package com.zakarialbouhmadi.tweeterclone;

import static com.zakarialbouhmadi.tweeterclone.UserAdapter.FOLLOW_URL;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;



public class ProfileActivity extends AppCompatActivity {
    private TextView textViewUsername, textViewBio, textViewFollowers, textViewFollowing;
    private Button buttonFollow;
    private RecyclerView recyclerViewTweets;
    private TweetAdapter tweetAdapter;
    private SessionManager sessionManager;
    private int userId;
    private boolean isOwnProfile;
    private static final String PROFILE_URL = "https://blog.kraftsport.pl/api/twitter/get_profile.php";
    private static final String FOLLOW_URL = "https://blog.kraftsport.pl/api/twitter/follow.php";
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        userId = getIntent().getIntExtra("user_id", -1);
        isOwnProfile = userId == sessionManager.getUserId();

        Log.d(TAG, "onCreate - userId: " + userId + ", isOwnProfile: " + isOwnProfile);

        // Initialize views
        textViewUsername = findViewById(R.id.textViewUsername);
        textViewBio = findViewById(R.id.textViewBio);
        textViewFollowers = findViewById(R.id.textViewFollowers);
        textViewFollowing = findViewById(R.id.textViewFollowing);
        buttonFollow = findViewById(R.id.buttonFollow);
        recyclerViewTweets = findViewById(R.id.recyclerViewTweets);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup RecyclerView
        recyclerViewTweets.setLayoutManager(new LinearLayoutManager(this));
        tweetAdapter = new TweetAdapter(this);
        recyclerViewTweets.setAdapter(tweetAdapter);

        // Setup button visibility and click listener
        buttonFollow.setVisibility(isOwnProfile ? View.GONE : View.VISIBLE);
        buttonFollow.setOnClickListener(v -> toggleFollow());

        if (isOwnProfile) {
            Button buttonEditProfile = findViewById(R.id.buttonEditProfile);
            buttonEditProfile.setVisibility(View.VISIBLE);
            buttonEditProfile.setOnClickListener(v -> showEditProfileDialog());
        } else {
            findViewById(R.id.buttonEditProfile).setVisibility(View.GONE);
        }

        // Load profile data
        loadProfile();
    }



    private void loadProfile() {
        String url = PROFILE_URL + "?user_id=" + userId + "&current_user_id=" + sessionManager.getUserId();
        Log.d("ProfileDebug", "Loading URL: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("ProfileDebug", "Response: " + response); // Add this debug log
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject profileData = jsonResponse.getJSONObject("profile");
                            updateProfileUI(profileData);

                            if (jsonResponse.has("tweets")) {
                                JSONArray tweetsArray = jsonResponse.getJSONArray("tweets");
                                tweetAdapter.setTweets(tweetsArray);
                            }
                        } else {
                            String message = jsonResponse.optString("message", "Unknown error");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("ProfileDebug", "JSON Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("ProfileDebug", "Error: " + error.toString());
                    Toast.makeText(this,
                            "Error loading profile: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }



    private void updateProfileUI(JSONObject profile) throws JSONException {
        Log.d(TAG, "Updating UI with profile: " + profile.toString());

        textViewUsername.setText(profile.getString("username"));
        textViewBio.setText(profile.optString("bio", "No bio yet"));
        textViewFollowers.setText(profile.getInt("followers_count") + " Followers");
        textViewFollowing.setText(profile.getInt("following_count") + " Following");

        if (!isOwnProfile) {
            boolean isFollowing = profile.getBoolean("is_following");
            updateFollowButton(isFollowing);
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText editTextUsername = view.findViewById(R.id.editTextUsername);
        EditText editTextBio = view.findViewById(R.id.editTextBio);

        // Pre-fill current values
        editTextUsername.setText(textViewUsername.getText());
        editTextBio.setText(textViewBio.getText());

        builder.setView(view)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, which) -> {
                    String username = editTextUsername.getText().toString().trim();
                    String bio = editTextBio.getText().toString().trim();
                    updateProfile(username, bio);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfile(String username, String bio) {
        String UPDATE_PROFILE_URL = "https://blog.kraftsport.pl/api/twitter/update_profile.php";

        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            loadProfile(); // Reload profile data
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("username", username);
                params.put("bio", bio);
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


    private void updateFollowButton(boolean isFollowing) {
        buttonFollow.setText(isFollowing ? "Unfollow" : "Follow");
    }

    private void toggleFollow() {
        StringRequest request = new StringRequest(Request.Method.POST, FOLLOW_URL,
                response -> {
                    try {
                        Log.d("ProfileActivity", "Follow Response: " + response); // Debug log
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            boolean isFollowing = jsonResponse.getBoolean("is_following");
                            updateFollowButton(isFollowing);
                            // Show toast message
                            String message = isFollowing ? "Following " + textViewUsername.getText() :
                                    "Unfollowed " + textViewUsername.getText();
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            // Reload profile to update counts
                            loadProfile();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("ProfileActivity", "Follow JSON Error: " + e.getMessage()); // Debug log
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("ProfileActivity", "Follow Volley Error: " + error.getMessage()); // Debug log
                    Toast.makeText(this, "Error updating follow status", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("follower_id", String.valueOf(sessionManager.getUserId()));
                params.put("following_id", String.valueOf(userId));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}