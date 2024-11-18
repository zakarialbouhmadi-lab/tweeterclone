package com.zakarialbouhmadi.tweeterclone;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewFollowers = itemView.findViewById(R.id.textViewFollowers);
            textViewFollowStatus = itemView.findViewById(R.id.textViewFollowStatus);

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
        }
    }

}