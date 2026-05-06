package com.zakarialbouhmadi.tweeterclone.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    private int messageId;
    private int senderId;
    private String senderUsername;
    private String senderProfilePic;
    private String content;
    private boolean isRead;
    private String createdAt;

    public Message(JSONObject json) throws JSONException {
        this.messageId = json.getInt("message_id");
        this.senderId = json.getInt("sender_id");
        this.senderUsername = json.optString("sender_username", "");
        this.senderProfilePic = json.optString("sender_profile_pic", "");
        this.content = json.getString("content");
        this.isRead = json.optBoolean("is_read", false);
        this.createdAt = json.optString("created_at", "");
        
        if (this.senderProfilePic.equals("null") || this.senderProfilePic.equals("default_profile.jpg")) {
            this.senderProfilePic = "";
        }
    }

    // Getters
    public int getMessageId() { return messageId; }
    public int getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getSenderProfilePic() { return senderProfilePic; }
    public String getContent() { return content; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setRead(boolean read) { this.isRead = read; }
}
