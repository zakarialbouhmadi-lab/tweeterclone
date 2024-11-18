package com.zakarialbouhmadi.tweeterclone;

import org.json.JSONException;
import org.json.JSONObject;

public class Comment {
    private int id;
    private int userId;
    private String username;
    private String content;
    private String createdAt;

    public Comment(JSONObject json) throws JSONException {
        this.id = json.getInt("comment_id");
        this.userId = json.getInt("user_id");
        this.username = json.getString("username");
        this.content = json.getString("content");
        this.createdAt = json.getString("created_at");
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
}