package com.zakarialbouhmadi.tweeterclone.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.activity.ChatActivity;
import com.zakarialbouhmadi.tweeterclone.model.Conversation;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    private List<Conversation> conversations = new ArrayList<>();
    private Context context;
    private SessionManager sessionManager;

    public ConversationAdapter(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewProfile;
        private TextView textViewUsername;
        private TextView textViewLastMessage;
        private TextView textViewTime;
        private TextView textViewUnreadBadge;
        private Conversation currentConversation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewUnreadBadge = itemView.findViewById(R.id.textViewUnreadBadge);

            itemView.setOnClickListener(v -> {
                if (currentConversation != null) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("conversation_id", currentConversation.getConversationId());
                    intent.putExtra("other_user_id", currentConversation.getOtherUserId());
                    intent.putExtra("other_username", currentConversation.getOtherUsername());
                    intent.putExtra("other_profile_pic", currentConversation.getOtherProfilePic());
                    context.startActivity(intent);
                }
            });
        }

        public void bind(Conversation conversation) {
            this.currentConversation = conversation;
            
            textViewUsername.setText(conversation.getOtherUsername());
            
            // Show last message with "You: " prefix if sent by current user
            String lastMessage = conversation.getLastMessage();
            if (conversation.getLastSenderId() == sessionManager.getUserId()) {
                lastMessage = "You: " + lastMessage;
            }
            textViewLastMessage.setText(lastMessage);
            
            // Format time
            textViewTime.setText(formatTime(conversation.getLastMessageTime()));
            
            // Handle unread count
            int unreadCount = conversation.getUnreadCount();
            if (unreadCount > 0) {
                textViewUnreadBadge.setVisibility(View.VISIBLE);
                textViewUnreadBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                textViewLastMessage.setTypeface(null, Typeface.BOLD);
                textViewUsername.setTypeface(null, Typeface.BOLD);
            } else {
                textViewUnreadBadge.setVisibility(View.GONE);
                textViewLastMessage.setTypeface(null, Typeface.NORMAL);
                textViewUsername.setTypeface(null, Typeface.NORMAL);
            }

            // Load profile picture
            String profilePic = conversation.getOtherProfilePic();
            if (profilePic != null && !profilePic.isEmpty()) {
                String imageUrl = "https://tweeterclone.com.pl/images/profile/" + profilePic;
                Glide.with(context)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(imageViewProfile);
            } else {
                imageViewProfile.setImageResource(R.drawable.ic_default_profile);
            }
        }

        private String formatTime(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) return "";
            
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(timestamp);
                if (date == null) return "";

                long diffInMillis = System.currentTimeMillis() - date.getTime();
                long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
                long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

                if (diffInMinutes < 1) {
                    return "now";
                } else if (diffInMinutes < 60) {
                    return diffInMinutes + "m";
                } else if (diffInHours < 24) {
                    return diffInHours + "h";
                } else if (diffInDays < 7) {
                    return diffInDays + "d";
                } else {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
                    return outputFormat.format(date);
                }
            } catch (ParseException e) {
                return "";
            }
        }
    }
}
