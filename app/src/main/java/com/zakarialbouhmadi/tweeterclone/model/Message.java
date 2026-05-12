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
    private String encryptedKey;
    private boolean isEncrypted;

    public Message(JSONObject json) throws JSONException {
        this.messageId = json.getInt("message_id");
        this.senderId = json.getInt("sender_id");
        this.senderUsername = json.optString("sender_username", "");
        this.senderProfilePic = json.optString("sender_profile_pic", "");
        this.content = json.getString("content");
        this.isRead = json.optBoolean("is_read", false);
        this.createdAt = json.optString("created_at", "");
        this.encryptedKey = json.optString("encrypted_key", "");
        this.isEncrypted = json.optBoolean("is_encrypted", false);

        if (this.senderProfilePic.equals("null") || this.senderProfilePic.equals("default_profile.jpg")) {
            this.senderProfilePic = "";
        }
    }

    public int getMessageId() { return messageId; }
    public int getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getSenderProfilePic() { return senderProfilePic; }
    public String getContent() { return content; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
    public String getEncryptedKey() { return encryptedKey; }
    public boolean isEncrypted() { return isEncrypted; }

    public void setRead(boolean read) { this.isRead = read; }
    public void setContent(String content) { this.content = content; }
}
