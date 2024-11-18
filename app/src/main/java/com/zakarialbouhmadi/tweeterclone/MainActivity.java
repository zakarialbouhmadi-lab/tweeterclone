package com.zakarialbouhmadi.tweeterclone;


import static com.zakarialbouhmadi.tweeterclone.TweetAdapter.LIKE_URL;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zakarialbouhmadi.tweeterclone.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewTweets;
    private SwipeRefreshLayout swipeRefresh;
    private TweetAdapter tweetAdapter;
    private  SessionManager sessionManager;
    private static final String TWEETS_URL = "https://blog.kraftsport.pl/api/twitter/get_tweets.php";

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



    private void showCreateTweetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_create_tweet, null);
        EditText editTextTweet = view.findViewById(R.id.editTextTweet);

        builder.setView(view)
                .setTitle("New Tweet")
                .setPositiveButton("Tweet", (dialog, which) -> {
                    String content = editTextTweet.getText().toString().trim();
                    if (!content.isEmpty()) {
                        createTweet(content);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createTweet(String content) {
        String CREATE_TWEET_URL = "https://blog.kraftsport.pl/api/twitter/create_tweet.php";

        StringRequest request = new StringRequest(Request.Method.POST, CREATE_TWEET_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this, "Tweet posted!", Toast.LENGTH_SHORT).show();
                            loadTweets(); // Refresh the feed
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