package com.zakarialbouhmadi.tweeterclone.model;

import org.json.JSONException;
import org.json.JSONObject;

public class FollowRequest {
    private int userId;
    private String username;
    private String profilePic;
    private String bio;
    private String requestDate;

    public FollowRequest(JSONObject json) throws JSONException {
        this.userId = json.getInt("user_id");
        this.username = json.getString("username");
        this.profilePic = json.optString("profile_pic", "");
        this.bio = json.optString("bio", "");
        this.requestDate = json.optString("request_date", "");
        
        if (this.profilePic.equals("null") || this.profilePic.equals("default_profile.jpg")) {
            this.profilePic = "";
        }
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getProfilePic() { return profilePic; }
    public String getBio() { return bio; }
    public String getRequestDate() { return requestDate; }
}
