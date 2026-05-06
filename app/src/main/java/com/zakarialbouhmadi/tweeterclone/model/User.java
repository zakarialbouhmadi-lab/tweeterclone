package com.zakarialbouhmadi.tweeterclone;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private int id;
    private String username;
    private String bio;
    private int followersCount;
    private int followingCount;
    private boolean isFollowing;
    private String profilePic;



    public User(JSONObject json) throws JSONException {
        this.id = json.getInt("user_id");
        this.username = json.getString("username");
        this.bio = json.optString("bio", ""); // optional field
        this.followersCount = json.optInt("followers_count", 0);
        this.followingCount = json.optInt("following_count", 0);
        this.isFollowing = json.optBoolean("is_following", false);
        this.profilePic = json.optString("profile_pic", "");
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getBio() { return bio; }
    public int getFollowersCount() { return followersCount; }
    public int getFollowingCount() { return followingCount; }
    public boolean isFollowing() { return isFollowing; }

    // Toggle following status
    public void toggleFollowing() {
        isFollowing = !isFollowing;
        followersCount += isFollowing ? 1 : -1;
    }

    public String getProfilePic() { return profilePic; }

}

