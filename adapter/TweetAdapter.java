package com.zakarialbouhmadi.tweeterclone.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.activity.CommentsActivity;
import com.zakarialbouhmadi.tweeterclone.activity.FullscreenImageActivity;
import com.zakarialbouhmadi.tweeterclone.activity.ProfileActivity;
import com.zakarialbouhmadi.tweeterclone.model.Tweet;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TweetAdapter extends RecyclerView.Adapter<TweetAdapter.TweetViewHolder> {
    private List<Tweet> tweets = new ArrayList<>();
    public static final String LIKE_URL = "https://tweeterclone.com.pl/api/like_tweet.php";
    private SessionManager sessionManager;
    private Context context;
    private boolean usernameClickable = true;

    public TweetAdapter(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
    }

    public TweetAdapter(Context context, boolean usernameClickable) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.usernameClickable = usernameClickable;
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
        private ImageView imageViewTweet;
        private ImageView imageViewProfilePic;

        public TweetViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            textViewLikes = itemView.findViewById(R.id.textViewLikes);
            buttonComment = itemView.findViewById(R.id.buttonComment);
            textViewComments = itemView.findViewById(R.id.textViewComments);
            sessionManager = new SessionManager(itemView.getContext());
            textViewDate = itemView.findViewById(R.id.textViewDate);
            imageViewTweet = itemView.findViewById(R.id.imageViewTweet);
            imageViewProfilePic = itemView.findViewById(R.id.imageViewProfilePic);
        }

        private void showDeleteDialog(Tweet tweet, int position) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Tweet")
                    .setMessage("Are you sure you want to delete this tweet?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteTweet(tweet, position))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteTweet(Tweet tweet, int position) {
            String DELETE_TWEET_URL = "https://tweeterclone.com.pl/api/delete_tweet.php";

            StringRequest request = new StringRequest(Request.Method.POST, DELETE_TWEET_URL,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                tweets.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Tweet deleted", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> Toast.makeText(context, "Error deleting tweet", Toast.LENGTH_SHORT).show()) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("tweet_id", String.valueOf(tweet.getId()));
                    params.put("user_id", String.valueOf(sessionManager.getUserId()));
                    return params;
                }
            };

            Volley.newRequestQueue(context).add(request);
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

        public void bind(Tweet tweet) {
            textViewUsername.setText(tweet.getUsername());
            textViewContent.setText(tweet.getContent());
            textViewDate.setText(tweet.getFormattedDate());
            textViewLikes.setText(String.valueOf(tweet.getLikesCount()));
            textViewComments.setText(String.valueOf(tweet.getCommentsCount()));

            buttonLike.setImageResource(tweet.isLiked() ?
                    android.R.drawable.star_big_on :
                    android.R.drawable.star_big_off);

            // Load profile picture
            if (imageViewProfilePic != null) {
                if (tweet.getProfilePic() != null && !tweet.getProfilePic().isEmpty()) {
                    String profileImageUrl = "https://tweeterclone.com.pl/images/profile/" + tweet.getProfilePic();
                    Glide.with(itemView.getContext())
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .into(imageViewProfilePic);
                } else {
                    imageViewProfilePic.setImageResource(R.drawable.ic_default_profile);
                }
                
                // Click profile picture to view profile
                imageViewProfilePic.setOnClickListener(v -> {
                    if (usernameClickable) {
                        Intent intent = new Intent(itemView.getContext(), ProfileActivity.class);
                        intent.putExtra("user_id", tweet.getUserId());
                        itemView.getContext().startActivity(intent);
                    }
                });
            }

            // Handle tweet image
            if (tweet.getImage() != null && !tweet.getImage().isEmpty()) {
                imageViewTweet.setVisibility(View.VISIBLE);
                String imageUrl = "https://tweeterclone.com.pl/images/tweets/" + tweet.getImage();
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .into(imageViewTweet);
                
                // Click image to view fullscreen
                imageViewTweet.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), FullscreenImageActivity.class);
                    intent.putExtra("image_url", imageUrl);
                    itemView.getContext().startActivity(intent);
                });
            } else {
                imageViewTweet.setVisibility(View.GONE);
            }

            // Click username to view profile (only if enabled)
            if (usernameClickable) {
                textViewUsername.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), ProfileActivity.class);
                    intent.putExtra("user_id", tweet.getUserId());
                    itemView.getContext().startActivity(intent);
                });
            } else {
                textViewUsername.setOnClickListener(null);
                textViewUsername.setClickable(false);
            }

            // Click listeners
            buttonLike.setOnClickListener(v -> likeTweet(tweet));
            buttonComment.setOnClickListener(v -> showComments(tweet));
            itemView.setOnLongClickListener(v -> {
                if (tweet.getUserId() == sessionManager.getUserId()) {
                    showDeleteDialog(tweet, getAdapterPosition());
                }
                return true;
            });
        }
    }
}
