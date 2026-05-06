package com.zakarialbouhmadi.tweeterclone;


import static com.zakarialbouhmadi.tweeterclone.adapter.TweetAdapter.LIKE_URL;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewTweets;
    private SwipeRefreshLayout swipeRefresh;
    private TweetAdapter tweetAdapter;
    private SessionManager sessionManager;
    private static final String TWEETS_URL = "https://blog.kraftsport.pl/api/twitter/get_tweets.php";
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

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
        } else if (id == R.id.action_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            profileIntent.putExtra("user_id", sessionManager.getUserId());
            startActivity(profileIntent);
            return true;
        } else if (id == R.id.action_logout) {
            sessionManager.logout();
            return true;
        }else if (item.getItemId() == R.id.action_theme) {
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
                                // Show empty state message
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
        // Follow system theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

        private void showCreateTweetDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_tweet, null);
        EditText editTextTweet = view.findViewById(R.id.editTextTweet);
        ImageView imagePreview = view.findViewById(R.id.imageViewPreview);
        Button buttonAddImage = view.findViewById(R.id.buttonAddImage);

        buttonAddImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("New Tweet")
                .setPositiveButton("Tweet", null) // Set to null initially
                .setNegativeButton("Cancel", (dialog1, which) -> {
                    selectedImageUri = null;
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String content = editTextTweet.getText().toString().trim();
                if (!content.isEmpty()) {
                    createTweet(content, selectedImageUri);
                    dialog.dismiss();
                    selectedImageUri = null;
                }
            });
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ImageView imagePreview = findViewById(R.id.imageViewPreview);
            if (imagePreview != null) {
                imagePreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(selectedImageUri).into(imagePreview);
            }
        }
    }

    private void createTweet(String content, Uri imageUri) {
        String CREATE_TWEET_URL = "https://blog.kraftsport.pl/api/twitter/create_tweet.php";

        StringRequest request = new StringRequest(Request.Method.POST, CREATE_TWEET_URL,
                response -> {
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
                error -> Toast.makeText(this, "Error posting tweet", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("content", content);
                params.put("user_id", String.valueOf(sessionManager.getUserId()));

                // Handle image if selected
                if (imageUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        byte[] imageBytes = baos.toByteArray();
                        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                        params.put("image", encodedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }



    class TweetViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewUsername;
        private TextView textViewContent;
        private ImageButton buttonLike;
        private TextView textViewLikes;
        private ImageButton buttonComment;
        private TextView textViewComments;
        private SessionManager sessionManager;

        public TweetViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            textViewLikes = itemView.findViewById(R.id.textViewLikes);
            buttonComment = itemView.findViewById(R.id.buttonComment);
            textViewComments = itemView.findViewById(R.id.textViewComments);
            sessionManager = new SessionManager(itemView.getContext());
        }

        public void bind(Tweet tweet) {
            // Set the text values
            textViewUsername.setText(tweet.getUsername());
            textViewContent.setText(tweet.getContent());
            textViewLikes.setText(String.valueOf(tweet.getLikesCount()));
            textViewComments.setText(String.valueOf(tweet.getCommentsCount()));

            // Set like button image based on whether user has liked
            buttonLike.setImageResource(tweet.isLiked() ?
                    android.R.drawable.star_big_on :
                    android.R.drawable.star_big_off);

            // Set click listeners
            buttonLike.setOnClickListener(v -> likeTweet(tweet));
            buttonComment.setOnClickListener(v -> showComments(tweet));
        }

        private void likeTweet(Tweet tweet) {
            Log.d("LikeDebug", "Attempting to like tweet: " + tweet.getId());
            Log.d("LikeDebug", "User ID: " + sessionManager.getUserId());

            StringRequest request = new StringRequest(Request.Method.POST, LIKE_URL,
                    response -> {
                        Log.d("LikeDebug", "Server response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                tweet.toggleLike();
                                buttonLike.setImageResource(tweet.isLiked() ?
                                        android.R.drawable.star_big_on :
                                        android.R.drawable.star_big_off);
                                int newLikeCount = jsonResponse.getInt("likes_count");
                                textViewLikes.setText(String.valueOf(newLikeCount));

                                Toast.makeText(itemView.getContext(),
                                        jsonResponse.getString("message"),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(itemView.getContext(),
                                        "Error: " + jsonResponse.getString("message"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("LikeDebug", "JSON parsing error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        Log.e("LikeDebug", "Volley error: " + error.getMessage());
                        Toast.makeText(itemView.getContext(),
                                "Network error while liking tweet",
                                Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("tweet_id", String.valueOf(tweet.getId()));
                    params.put("user_id", String.valueOf(sessionManager.getUserId()));
                    return params;
                }
            };

            Volley.newRequestQueue(itemView.getContext()).add(request);
        }

        private void showComments(Tweet tweet) {
            Context context = itemView.getContext();
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("tweet_id", tweet.getId());
            context.startActivity(intent);
        }
    }
}

