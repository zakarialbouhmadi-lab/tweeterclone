package com.zakarialbouhmadi.tweeterclone.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.activity.ProfileActivity;
import com.zakarialbouhmadi.tweeterclone.model.User;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users = new ArrayList<>();
    protected static final String FOLLOW_URL = "https://tweeterclone.com.pl/api/follow.php";
    private SessionManager sessionManager;
    private Context context;

    public UserAdapter(Context context) {
        this.context = context;
        sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }


    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewUsername;
        private TextView textViewFollowers;
        private TextView textViewFollowStatus;
        private User currentUser;
        private ImageView imageViewProfilePic;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewFollowers = itemView.findViewById(R.id.textViewFollowers);
            textViewFollowStatus = itemView.findViewById(R.id.textViewFollowStatus);
            imageViewProfilePic = itemView.findViewById(R.id.imageViewProfile);

            itemView.setOnClickListener(v -> {
                if (currentUser != null) {
                    Intent intent = new Intent(itemView.getContext(), ProfileActivity.class);
                    intent.putExtra("user_id", currentUser.getId());
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        public void bind(User user) {
            this.currentUser = user;
            textViewUsername.setText(user.getUsername());
            textViewFollowers.setText(user.getFollowersCount() + " followers");
            
            // Update follow status display based on new follow_status field
            String followStatus = user.getFollowStatus();
            if (followStatus.equals("accepted")) {
                textViewFollowStatus.setText("Following");
                textViewFollowStatus.setTextColor(context.getResources().getColor(R.color.success));
                textViewFollowStatus.setVisibility(View.VISIBLE);
            } else if (followStatus.equals("pending")) {
                textViewFollowStatus.setText("Requested");
                textViewFollowStatus.setTextColor(context.getResources().getColor(R.color.warning));
                textViewFollowStatus.setVisibility(View.VISIBLE);
            } else {
                textViewFollowStatus.setVisibility(View.GONE);
            }

            // Load profile picture
            if (imageViewProfilePic != null) {
                String profilePic = user.getProfilePic();
                if (profilePic != null && !profilePic.isEmpty()) {
                    String imageUrl = "https://tweeterclone.com.pl/images/profile/" + profilePic;
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .into(imageViewProfilePic);
                } else {
                    // Clear any previous image and set default
                    Glide.with(itemView.getContext())
                            .clear(imageViewProfilePic);
                    imageViewProfilePic.setImageResource(R.drawable.ic_default_profile);
                }
            }
        }
    }
}
