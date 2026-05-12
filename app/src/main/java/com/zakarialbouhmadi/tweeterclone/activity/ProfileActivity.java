package com.zakarialbouhmadi.tweeterclone.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.adapter.TweetAdapter;
import com.zakarialbouhmadi.tweeterclone.util.ImagePickerHelper;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private TextView textViewUsername, textViewBio, textViewFollowers, textViewFollowing, textViewVisibility, textViewPrivateMessage, textViewProfileAllTweetsTitle;
    private Button buttonFollow, buttonFollowRequests, buttonMessage;
    private RecyclerView recyclerViewTweets;
    private TweetAdapter tweetAdapter;
    private SessionManager sessionManager;
    private int userId;
    private boolean isOwnProfile;
    private boolean isPublicProfile = true;
    private boolean isMutualFollow = false;
    private String currentFollowStatus = "none"; // "none", "pending", "accepted"
    private String otherUsername = "";
    private String otherProfilePic = "";
    private ImageView imageViewProfile;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String uploadServerUrl = "https://tweeterclone.com.pl/api/upload_profile_photo.php";
    private static final String PROFILE_URL = "https://tweeterclone.com.pl/api/get_profile.php";
    private static final String FOLLOW_URL = "https://tweeterclone.com.pl/api/follow.php";
    private static final String MUTUAL_CHECK_URL = "https://tweeterclone.com.pl/api/check_mutual_follow.php";
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
        textViewVisibility = findViewById(R.id.textViewVisibility);
        textViewPrivateMessage = findViewById(R.id.textViewPrivateMessage);
        buttonFollow = findViewById(R.id.buttonFollow);
        buttonFollowRequests = findViewById(R.id.buttonFollowRequests);
        buttonMessage = findViewById(R.id.buttonMessage);
        recyclerViewTweets = findViewById(R.id.recyclerViewTweets);
        textViewProfileAllTweetsTitle = findViewById(R.id.textViewProfileAllTweetsTitle);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup RecyclerView
        recyclerViewTweets.setLayoutManager(new LinearLayoutManager(this));
        tweetAdapter = new TweetAdapter(this, false); // Disable username clicks in profile
        recyclerViewTweets.setAdapter(tweetAdapter);

        // Setup button visibility and click listener
        buttonFollow.setVisibility(isOwnProfile ? View.GONE : View.VISIBLE);
        buttonFollow.setOnClickListener(v -> toggleFollow());

        // Setup message button (hidden by default, shown when mutual follow)
        buttonMessage.setVisibility(View.GONE);
        buttonMessage.setOnClickListener(v -> openChat());

        // Setup clickable followers/following counts
        textViewFollowers.setOnClickListener(v -> openFollowersList());
        textViewFollowing.setOnClickListener(v -> openFollowingList());

        if (isOwnProfile) {
            Button buttonEditProfile = findViewById(R.id.buttonEditProfile);
            buttonEditProfile.setVisibility(View.VISIBLE);
            buttonEditProfile.setOnClickListener(v -> showEditProfileDialog());
            Button buttonChangePhoto = findViewById(R.id.buttonChangePhoto);
            buttonChangePhoto.setOnClickListener(v -> openImageChooser());
            
            // Show follow requests button for own profile
            buttonFollowRequests.setVisibility(View.VISIBLE);
            buttonFollowRequests.setOnClickListener(v -> {
                startActivity(new Intent(this, FollowRequestsActivity.class));
            });
        } else {
            findViewById(R.id.buttonEditProfile).setVisibility(View.GONE);
            findViewById(R.id.buttonChangePhoto).setVisibility(View.GONE);
            buttonFollowRequests.setVisibility(View.GONE);
        }

        imageViewProfile = findViewById(R.id.imageViewProfile);

        // Load profile data
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile when returning to this activity
        loadProfile();
    }

    private void openChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user_id", userId);
        intent.putExtra("other_username", otherUsername);
        intent.putExtra("other_profile_pic", otherProfilePic);
        startActivity(intent);
    }

    private void checkMutualFollow() {
        if (isOwnProfile) return;

        String url = MUTUAL_CHECK_URL + "?user_id=" + sessionManager.getUserId() + "&other_user_id=" + userId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            isMutualFollow = jsonResponse.getBoolean("is_mutual_follow");
                            buttonMessage.setVisibility(isMutualFollow ? View.VISIBLE : View.GONE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "Error checking mutual follow: " + error.toString()));

        Volley.newRequestQueue(this).add(request);
    }

    private void openFollowersList() {
        Intent intent = new Intent(this, FollowListActivity.class);
        intent.putExtra(FollowListActivity.EXTRA_USER_ID, userId);
        intent.putExtra(FollowListActivity.EXTRA_TYPE, FollowListActivity.TYPE_FOLLOWERS);
        startActivity(intent);
    }

    private void openFollowingList() {
        Intent intent = new Intent(this, FollowListActivity.class);
        intent.putExtra(FollowListActivity.EXTRA_USER_ID, userId);
        intent.putExtra(FollowListActivity.EXTRA_TYPE, FollowListActivity.TYPE_FOLLOWING);
        startActivity(intent);
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (imageUri == null) return;

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Compressing and uploading...");
        progressDialog.show();

        // Compress image in background thread
        new Thread(() -> {
            try {
                // Use the new compression method
                String imageString = ImagePickerHelper.compressProfileImage(getContentResolver(), imageUri);
                
                Log.d("ProfileUpload", "Compressed image Base64 length: " + imageString.length());

                // Upload on main thread
                runOnUiThread(() -> {
                    StringRequest request = new StringRequest(Request.Method.POST, uploadServerUrl,
                            response -> {
                                progressDialog.dismiss();
                                Log.d("ProfileUpload", "Raw server response: " + response);
                                try {
                                    JSONObject jsonResponse = new JSONObject(response);
                                    Log.d("ProfileUpload", "Success status: " + jsonResponse.getBoolean("success"));
                                    if (jsonResponse.getBoolean("success")) {
                                        String imageUrl = jsonResponse.getString("image_url");
                                        Log.d("ProfileUpload", "Image URL received: " + imageUrl);
                                        Glide.with(ProfileActivity.this)
                                                .load(imageUrl)
                                                .placeholder(R.drawable.ic_default_profile)
                                                .into(imageViewProfile);
                                        Toast.makeText(ProfileActivity.this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                                    } else {
                                        String errorMessage = jsonResponse.getString("message");
                                        Log.e("ProfileUpload", "Server reported failure: " + errorMessage);
                                        Toast.makeText(ProfileActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    Log.e("ProfileUpload", "JSON parsing error: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            },
                            error -> {
                                progressDialog.dismiss();
                                Log.e("ProfileUpload", "Network error: " + error.toString());
                                if (error.networkResponse != null) {
                                    Log.e("ProfileUpload", "Status code: " + error.networkResponse.statusCode);
                                    Log.e("ProfileUpload", "Response data: " + new String(error.networkResponse.data));
                                }
                                Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            params.put("user_id", String.valueOf(sessionManager.getUserId()));
                            params.put("image", imageString);
                            return params;
                        }
                    };

                    Volley.newRequestQueue(ProfileActivity.this).add(request);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.e("ProfileUpload", "Image processing error: " + e.getMessage());
                    Toast.makeText(ProfileActivity.this, "Error processing image", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void loadProfileImage(JSONObject profile) {
        try {
            if (profile != null && profile.has("profile_pic")) {
                otherProfilePic = profile.getString("profile_pic");
                String imageUrl = "https://tweeterclone.com.pl/images/profile/" + otherProfilePic;
                Log.d("ProfileImage", "Loading image from URL: " + imageUrl);

                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                Log.e("ProfileImage", "Error loading image: " + e.getMessage());
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Log.d("ProfileImage", "Image loaded successfully");
                                return false;
                            }
                        })
                        .into(imageViewProfile);
            } else {
                Log.d("ProfileImage", "No profile_pic in profile data");
            }
        } catch (JSONException e) {
            Log.e("ProfileImage", "Error parsing profile data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadProfile() {
        String url = PROFILE_URL + "?user_id=" + userId + "&current_user_id=" + sessionManager.getUserId();
        Log.d("ProfileDebug", "Loading URL: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("ProfileDebug", "Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject profileData = jsonResponse.getJSONObject("profile");
                            updateProfileUI(profileData);

                            // Check if tweets should be visible
                            boolean canSeeTweets = profileData.optBoolean("can_see_tweets", true);

                            if (canSeeTweets && jsonResponse.has("tweets")) {
                                JSONArray tweetsArray = jsonResponse.getJSONArray("tweets");
                                tweetAdapter.setTweets(tweetsArray);
                                recyclerViewTweets.setVisibility(View.VISIBLE);
                                textViewProfileAllTweetsTitle.setVisibility(View.VISIBLE);
                                textViewPrivateMessage.setVisibility(View.GONE);
                            } else if (!canSeeTweets) {
                                recyclerViewTweets.setVisibility(View.GONE);
                                textViewProfileAllTweetsTitle.setVisibility(View.GONE);
                                textViewPrivateMessage.setVisibility(View.VISIBLE);
                                
                                // Update message based on follow status
                                if (currentFollowStatus.equals("pending")) {
                                    textViewPrivateMessage.setText("This profile is private. Follow request pending.");
                                } else {
                                    textViewPrivateMessage.setText("This profile is private. Send a follow request to see tweets.");
                                }
                            }

                            // Check mutual follow for messaging
                            checkMutualFollow();
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

        otherUsername = profile.getString("username");
        textViewUsername.setText(otherUsername);
        textViewBio.setText(profile.optString("bio", "No bio yet"));
        
        int followersCount = profile.getInt("followers_count");
        int followingCount = profile.getInt("following_count");
        textViewFollowers.setText(followersCount + " Followers");
        textViewFollowing.setText(followingCount + " Following");

        // Handle public/private visibility
        isPublicProfile = profile.optBoolean("is_public", true);
        if (isPublicProfile) {
            textViewVisibility.setText("Public");
            textViewVisibility.setBackgroundResource(R.drawable.badge_background);
            textViewVisibility.getBackground().setTint(getResources().getColor(R.color.success));
        } else {
            textViewVisibility.setText("Private");
            textViewVisibility.setBackgroundResource(R.drawable.badge_background);
            textViewVisibility.getBackground().setTint(getResources().getColor(R.color.error));
        }

        // Load profile image
        loadProfileImage(profile);

        // Update follow requests button for own profile
        if (isOwnProfile) {
            int pendingCount = profile.optInt("pending_requests_count", 0);
            if (pendingCount > 0) {
                buttonFollowRequests.setText("Follow Requests (" + pendingCount + ")");
            } else {
                buttonFollowRequests.setText("Follow Requests");
            }
        }

        if (!isOwnProfile) {
            // Get follow status
            currentFollowStatus = profile.optString("follow_status", "none");
            updateFollowButton();
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText editTextUsername = view.findViewById(R.id.editTextUsername);
        EditText editTextBio = view.findViewById(R.id.editTextBio);
        SwitchCompat switchPublicProfile = view.findViewById(R.id.switchPublicProfile);

        // Pre-fill current values
        editTextUsername.setText(textViewUsername.getText());
        editTextBio.setText(textViewBio.getText());
        switchPublicProfile.setChecked(isPublicProfile);

        builder.setView(view)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, which) -> {
                    String username = editTextUsername.getText().toString().trim();
                    String bio = editTextBio.getText().toString().trim();
                    boolean isPublic = switchPublicProfile.isChecked();
                    updateProfile(username, bio, isPublic);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfile(String username, String bio, boolean isPublic) {
        String UPDATE_PROFILE_URL = "https://tweeterclone.com.pl/api/update_profile.php";

        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            loadProfile(); // Reload profile data
                        } else {
                            Toast.makeText(this, jsonResponse.optString("message", "Error"), Toast.LENGTH_SHORT).show();
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
                params.put("is_public", isPublic ? "1" : "0");
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

    private void updateFollowButton() {
        switch (currentFollowStatus) {
            case "accepted":
                buttonFollow.setText("Unfollow");
                buttonFollow.setBackgroundTintList(getResources().getColorStateList(R.color.error));
                break;
            case "pending":
                buttonFollow.setText("Requested");
                buttonFollow.setBackgroundTintList(getResources().getColorStateList(R.color.warning));
                break;
            default:
                buttonFollow.setText("Follow");
                buttonFollow.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                break;
        }
    }

    private void toggleFollow() {
        StringRequest request = new StringRequest(Request.Method.POST, FOLLOW_URL,
                response -> {
                    try {
                        Log.d("ProfileActivity", "Follow Response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            currentFollowStatus = jsonResponse.getString("follow_status");
                            updateFollowButton();
                            
                            String message = jsonResponse.getString("message");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            
                            // Reload profile to update counts and tweet visibility
                            loadProfile();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("ProfileActivity", "Follow JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("ProfileActivity", "Follow Volley Error: " + error.getMessage());
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
