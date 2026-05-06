package com.zakarialbouhmadi.tweeterclone.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.activity.ProfileActivity;
import com.zakarialbouhmadi.tweeterclone.model.FollowRequest;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowRequestAdapter extends RecyclerView.Adapter<FollowRequestAdapter.ViewHolder> {
    private List<FollowRequest> requests = new ArrayList<>();
    private Context context;
    private SessionManager sessionManager;
    private OnRequestHandledListener listener;
    private static final String RESPOND_URL = "https://tweeterclone.com.pl/api/respond_follow_request.php";

    public interface OnRequestHandledListener {
        void onRequestHandled();
    }

    public FollowRequestAdapter(Context context, OnRequestHandledListener listener) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FollowRequest request = requests.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void setRequests(List<FollowRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewProfile;
        private TextView textViewUsername;
        private TextView textViewBio;
        private Button buttonAccept;
        private Button buttonDecline;
        private FollowRequest currentRequest;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewBio = itemView.findViewById(R.id.textViewBio);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonDecline = itemView.findViewById(R.id.buttonDecline);

            // Click on profile to view user
            itemView.setOnClickListener(v -> {
                if (currentRequest != null) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("user_id", currentRequest.getUserId());
                    context.startActivity(intent);
                }
            });

            buttonAccept.setOnClickListener(v -> handleRequest("accept"));
            buttonDecline.setOnClickListener(v -> handleRequest("decline"));
        }

        public void bind(FollowRequest request) {
            this.currentRequest = request;
            textViewUsername.setText(request.getUsername());
            
            String bio = request.getBio();
            if (bio != null && !bio.isEmpty()) {
                textViewBio.setText(bio);
                textViewBio.setVisibility(View.VISIBLE);
            } else {
                textViewBio.setVisibility(View.GONE);
            }

            // Load profile picture
            String profilePic = request.getProfilePic();
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

        private void handleRequest(String action) {
            if (currentRequest == null) return;

            // Disable buttons while processing
            buttonAccept.setEnabled(false);
            buttonDecline.setEnabled(false);

            StringRequest request = new StringRequest(Request.Method.POST, RESPOND_URL,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                String message = action.equals("accept") ? 
                                        "Follow request accepted" : "Follow request declined";
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                
                                // Remove from list
                                int pos = getAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    requests.remove(pos);
                                    notifyItemRemoved(pos);
                                }
                                
                                if (listener != null) {
                                    listener.onRequestHandled();
                                }
                            } else {
                                Toast.makeText(context, 
                                        jsonResponse.optString("message", "Error"), 
                                        Toast.LENGTH_SHORT).show();
                                buttonAccept.setEnabled(true);
                                buttonDecline.setEnabled(true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            buttonAccept.setEnabled(true);
                            buttonDecline.setEnabled(true);
                        }
                    },
                    error -> {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                        buttonAccept.setEnabled(true);
                        buttonDecline.setEnabled(true);
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("follower_id", String.valueOf(currentRequest.getUserId()));
                    params.put("following_id", String.valueOf(sessionManager.getUserId()));
                    params.put("action", action);
                    return params;
                }
            };

            Volley.newRequestQueue(context).add(request);
        }
    }
}
