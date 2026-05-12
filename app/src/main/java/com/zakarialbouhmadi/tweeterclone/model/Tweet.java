package com.zakarialbouhmadi.tweeterclone.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Tweet {
    private int id;
    private int userId;
    private String username;
    private String content;
    private int likesCount;
    private int commentsCount;
    private boolean isLiked;
    private String image;
    private String createdAt;
    private String formattedDate;
    private String profilePic;

    public Tweet(JSONObject json) throws JSONException {
        this.id = json.getInt("tweet_id");
        this.userId = json.getInt("user_id");
        this.username = json.getString("username");
        this.content = json.getString("content");
        this.likesCount = json.getInt("likes_count");
        this.commentsCount = json.getInt("comments_count");
        this.isLiked = json.getBoolean("is_liked");
        this.createdAt = json.getString("created_at");
        
        // Handle tweet image
        String imageValue = json.optString("image", "");
        this.image = (imageValue.equals("null") || imageValue.isEmpty()) ? null : imageValue;
        
        // Handle profile picture
        String profilePicValue = json.optString("profile_pic", "");
        this.profilePic = (profilePicValue.equals("null") || profilePicValue.equals("default_profile.jpg") || profilePicValue.isEmpty()) 
                ? "" : profilePicValue;
        
        // Format the date
        try {
            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            Date date = serverFormat.parse(this.createdAt);
            this.formattedDate = displayFormat.format(date);
        } catch (ParseException e) {
            this.formattedDate = this.createdAt;
        }
    }

    public String getImage() { return image; }

    public String getFormattedDate() {
        return formattedDate;
    }
    
    public String getProfilePic() { return profilePic; }

    public void toggleLike() {
        isLiked = !isLiked;
        likesCount += isLiked ? 1 : -1;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public int getLikesCount() { return likesCount; }
    public int getCommentsCount() { return commentsCount; }
    public boolean isLiked() { return isLiked; }
    public String getCreatedAt() { return createdAt; }
}
