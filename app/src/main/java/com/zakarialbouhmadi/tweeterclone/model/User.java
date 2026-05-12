package com.zakarialbouhmadi.tweeterclone.model;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private int id;
    private String username;
    private String bio;
    private int followersCount;
    private int followingCount;
    private boolean isFollowing;
    private String followStatus; // "none", "pending", "accepted"
    private String profilePic;

    public User(JSONObject json) throws JSONException {
        this.id = json.getInt("user_id");
        this.username = json.getString("username");
        this.bio = json.optString("bio", "");
        this.followersCount = json.optInt("followers_count", 0);
        this.followingCount = json.optInt("following_count", 0);
        this.isFollowing = json.optBoolean("is_following", false);
        this.followStatus = json.optString("follow_status", "none");
        
        // Backward compatibility: if follow_status not provided, derive from is_following
        if (this.followStatus.isEmpty() || this.followStatus.equals("null")) {
            this.followStatus = this.isFollowing ? "accepted" : "none";
        }
        
        String picValue = json.optString("profile_pic", "");
        this.profilePic = (picValue.equals("null") || picValue.equals("default_profile.jpg")) ? "" : picValue;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getBio() { return bio; }
    public int getFollowersCount() { return followersCount; }
    public int getFollowingCount() { return followingCount; }
    public boolean isFollowing() { return isFollowing || followStatus.equals("accepted"); }
    public String getFollowStatus() { return followStatus; }
    public String getProfilePic() { return profilePic; }

    // Setters for follow status updates
    public void setFollowStatus(String status) {
        this.followStatus = status;
        this.isFollowing = status.equals("accepted");
    }

    public void setFollowersCount(int count) {
        this.followersCount = count;
    }

    // Toggle following status (for backward compatibility)
    public void toggleFollowing() {
        if (followStatus.equals("accepted")) {
            followStatus = "none";
            isFollowing = false;
            followersCount--;
        } else if (followStatus.equals("pending")) {
            followStatus = "none";
            isFollowing = false;
        } else {
            followStatus = "pending";
            isFollowing = false;
        }
    }

    // Check if request is pending
    public boolean isPending() {
        return followStatus.equals("pending");
    }
}
