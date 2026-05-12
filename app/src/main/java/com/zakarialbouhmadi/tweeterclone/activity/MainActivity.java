package com.zakarialbouhmadi.tweeterclone.activity;

import static com.zakarialbouhmadi.tweeterclone.adapter.TweetAdapter.LIKE_URL;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.adapter.TweetAdapter;
import com.zakarialbouhmadi.tweeterclone.model.Tweet;
import com.zakarialbouhmadi.tweeterclone.util.ImagePickerHelper;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewTweets;
    private SwipeRefreshLayout swipeRefresh;
    private TweetAdapter tweetAdapter;
    private SessionManager sessionManager;
    private static final String TWEETS_URL = "https://tweeterclone.com.pl/api/get_tweets.php";
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private AlertDialog currentDialog;
    private ImageView dialogImagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            sessionManager.logout();
            return;
        }

        // Setup toolbar with logout option
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("News Feed");
        setSupportActionBar(toolbar);

        // Initialize views
        recyclerViewTweets = findViewById(R.id.recyclerViewTweets);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        FloatingActionButton fabCreateTweet = findViewById(R.id.fabCreateTweet);

        // Setup RecyclerView
        recyclerViewTweets.setLayoutManager(new LinearLayoutManager(this));
        tweetAdapter = new TweetAdapter(this);
        recyclerViewTweets.setAdapter(tweetAdapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadTweets);

        // Setup FAB
        fabCreateTweet.setOnClickListener(v -> showCreateTweetDialog());

        // Initial load
        loadTweets();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchUsersActivity.class));
            return true;
        } else if (id == R.id.action_messages) {
            startActivity(new Intent(this, ConversationsActivity.class));
            return true;
        } else if (id == R.id.action_follow_requests) {
            startActivity(new Intent(this, FollowRequestsActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            profileIntent.putExtra("user_id", sessionManager.getUserId());
            startActivity(profileIntent);
            return true;
        } else if (id == R.id.action_logout) {
            sessionManager.logout();
            return true;
        } else if (item.getItemId() == R.id.action_theme) {
            int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadTweets() {
        String url = TWEETS_URL + "?user_id=" + sessionManager.getUserId();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray tweets = jsonResponse.getJSONArray("tweets");
                            if (tweets.length() == 0) {
                                Toast.makeText(this,
                                        "No tweets yet. Follow some users to see their tweets!",
                                        Toast.LENGTH_LONG).show();
                            }
                            tweetAdapter.setTweets(tweets);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this,
                            "Error loading tweets",
                            Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void toggleTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private void showCreateTweetDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_tweet, null);
        EditText editTextTweet = view.findViewById(R.id.editTextTweet);
        dialogImagePreview = view.findViewById(R.id.imageViewPreview);
        Button buttonAddImage = view.findViewById(R.id.buttonAddImage);

        buttonAddImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        currentDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("New Tweet")
                .setPositiveButton("Tweet", null)
                .setNegativeButton("Cancel", (dialog1, which) -> {
                    selectedImageUri = null;
                    dialogImagePreview = null;
                })
                .create();

        currentDialog.setOnShowListener(dialogInterface -> {
            Button button = currentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String content = editTextTweet.getText().toString().trim();
                if (!content.isEmpty()) {
                    createTweet(content, selectedImageUri);
                    currentDialog.dismiss();
                    selectedImageUri = null;
                    dialogImagePreview = null;
                }
            });
        });

        currentDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            if (dialogImagePreview != null) {
                dialogImagePreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(selectedImageUri).into(dialogImagePreview);
            }
        }
    }

    private void createTweet(String content, Uri imageUri) {
        String UPLOAD_IMAGE_URL = "https://tweeterclone.com.pl/api/upload_image.php";

        if (imageUri != null) {
            // Show progress
            Toast.makeText(this, "Compressing and uploading image...", Toast.LENGTH_SHORT).show();
            
            // Compress and upload in background thread
            new Thread(() -> {
                try {
                    // Use the new compression method for tweet images
                    String encodedImage = ImagePickerHelper.compressTweetImage(getContentResolver(), imageUri);
                    
                    Log.d("TweetUpload", "Compressed image Base64 length: " + encodedImage.length());

                    runOnUiThread(() -> {
                        StringRequest uploadRequest = new StringRequest(Request.Method.POST, UPLOAD_IMAGE_URL,
                                response -> {
                                    Log.d("TweetUpload", "Upload response: " + response);
                                    try {
                                        JSONObject jsonResponse = new JSONObject(response);
                                        if (jsonResponse.getBoolean("success")) {
                                            String filename = jsonResponse.getString("filename");
                                            postTweetWithImage(content, filename);
                                        } else {
                                            Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Toast.makeText(this, "Error parsing upload response", Toast.LENGTH_SHORT).show();
                                    }
                                },
                                error -> {
                                    Log.e("TweetUpload", "Upload error: " + error.toString());
                                    if (error.networkResponse != null) {
                                        Log.e("TweetUpload", "Status: " + error.networkResponse.statusCode);
                                    }
                                    Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show();
                                }) {
                            @Override
                            protected Map<String, String> getParams() {
                                Map<String, String> params = new HashMap<>();
                                params.put("image", encodedImage);
                                params.put("type", "tweet");
                                return params;
                            }
                        };

                        Volley.newRequestQueue(MainActivity.this).add(uploadRequest);
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        Log.e("TweetUpload", "Image processing error: " + e.getMessage());
                        Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }
            }).start();
        } else {
            postTweetWithImage(content, null);
        }
    }

    private void postTweetWithImage(String content, String imageFilename) {
        String CREATE_TWEET_URL = "https://tweeterclone.com.pl/api/create_tweet.php";

        StringRequest request = new StringRequest(Request.Method.POST, CREATE_TWEET_URL,
                response -> {
                    Log.d("TweetCreate", "Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this, "Tweet posted!", Toast.LENGTH_SHORT).show();
                            loadTweets();
                        } else {
                            Toast.makeText(this, "Error posting tweet", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("TweetCreate", "Error: " + error.toString());
                    Toast.makeText(this, "Error posting tweet", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("content", content);
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                if (imageFilename != null) {
                    params.put("image", imageFilename);
                }
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}
