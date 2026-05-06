package com.zakarialbouhmadi.tweeterclone;

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
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.activity.ProfileActivity;
import com.zakarialbouhmadi.tweeterclone.model.User;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users = new ArrayList<>();
    protected static final String FOLLOW_URL = "https://blog.kraftsport.pl/api/twitter/follow.php";
    private SessionManager sessionManager;

    public UserAdapter(Context context) {
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

    public void setUsers(JSONArray jsonUsers) {
        List<User> newUsers = new ArrayList<>();
        for (int i = 0; i < jsonUsers.length(); i++) {
            try {
                newUsers.add(new User(jsonUsers.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        setUsers(newUsers);
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
            imageViewProfilePic = itemView.findViewById(R.id.imageViewProfilePic);

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
            textViewFollowStatus.setVisibility(user.isFollowing() ? View.VISIBLE : View.GONE);

            if (!user.getProfilePic().isEmpty()) {
                String imageUrl = "https://blog.kraftsport.pl/api/twitter/images/profile/" + user.getProfilePic();
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imageViewProfilePic);
            } else {
                imageViewProfilePic.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }

}

