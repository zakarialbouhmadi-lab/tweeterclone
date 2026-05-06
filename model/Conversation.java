package com.zakarialbouhmadi.tweeterclone.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Conversation {
    private int conversationId;
    private int otherUserId;
    private String otherUsername;
    private String otherProfilePic;
    private String lastMessage;
    private String lastMessageTime;
    private int lastSenderId;
    private int unreadCount;

    public Conversation(JSONObject json) throws JSONException {
        this.conversationId = json.getInt("conversation_id");
        this.otherUserId = json.getInt("other_user_id");
        this.otherUsername = json.getString("other_username");
        this.otherProfilePic = json.optString("other_profile_pic", "");
        this.lastMessage = json.optString("last_message", "");
        this.lastMessageTime = json.optString("last_message_time", "");
        this.lastSenderId = json.optInt("last_sender_id", 0);
        this.unreadCount = json.optInt("unread_count", 0);
        
        if (this.otherProfilePic.equals("null") || this.otherProfilePic.equals("default_profile.jpg")) {
            this.otherProfilePic = "";
        }
    }

    // Getters
    public int getConversationId() { return conversationId; }
    public int getOtherUserId() { return otherUserId; }
    public String getOtherUsername() { return otherUsername; }
    public String getOtherProfilePic() { return otherProfilePic; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }
    public int getLastSenderId() { return lastSenderId; }
    public int getUnreadCount() { return unreadCount; }

    // Setters
    public void setUnreadCount(int count) { this.unreadCount = count; }
    public void setLastMessage(String message) { this.lastMessage = message; }
    public void setLastMessageTime(String time) { this.lastMessageTime = time; }
    public void setLastSenderId(int senderId) { this.lastSenderId = senderId; }
}
