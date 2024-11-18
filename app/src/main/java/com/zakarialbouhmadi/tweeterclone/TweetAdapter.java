package com.zakarialbouhmadi.tweeterclone;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TweetAdapter extends RecyclerView.Adapter<TweetAdapter.TweetViewHolder> {
    private List<Tweet> tweets = new ArrayList<>();
    public static final String LIKE_URL = "https://blog.kraftsport.pl/api/twitter/like_tweet.php";
    private Context context;

    public TweetAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public TweetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tweet, parent, false);
        return new TweetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TweetViewHolder holder, int position) {
        Tweet tweet = tweets.get(position);
        holder.bind(tweet);
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    public void setTweets(JSONArray jsonTweets) {
        tweets.clear();
        for (int i = 0; i < jsonTweets.length(); i++) {
            try {
                tweets.add(new Tweet(jsonTweets.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        notifyDataSetChanged();
    }

    class TweetViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewDate;
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
            textViewDate = itemView.findViewById(R.id.textViewDate); // Add this

        }

        public void bind(Tweet tweet) {
            // Set the text values
            textViewUsername.setText(tweet.getUsername());
            textViewContent.setText(tweet.getContent());
textViewDate.setText(tweet.getFormattedDate());
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