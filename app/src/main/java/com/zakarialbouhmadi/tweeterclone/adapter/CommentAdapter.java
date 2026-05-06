package com.zakarialbouhmadi.tweeterclone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.zakarialbouhmadi.tweeterclone.R;
import com.zakarialbouhmadi.tweeterclone.model.Comment;
import com.zakarialbouhmadi.tweeterclone.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> comments = new ArrayList<>();
    private SessionManager sessionManager;
    private Context context;

    public CommentAdapter(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, position);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void setComments(JSONArray jsonComments) {
        comments.clear();
        for (int i = 0; i < jsonComments.length(); i++) {
            try {
                comments.add(new Comment(jsonComments.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        notifyDataSetChanged();
    }

    private void showDeleteDialog(Comment comment, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteComment(Comment comment, int position) {
        String DELETE_COMMENT_URL = "https://blog.kraftsport.pl/api/twitter/delete_comment.php";

        StringRequest request = new StringRequest(Request.Method.POST, DELETE_COMMENT_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            comments.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(context, "Error deleting comment", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("comment_id", String.valueOf(comment.getId()));
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    // Remove static keyword from inner class
    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewUsername;
        private TextView textViewContent;
        private TextView textViewTimestamp;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }

        public void bind(Comment comment, int position) {
            textViewUsername.setText(comment.getUsername());
            textViewContent.setText(comment.getContent());
            textViewTimestamp.setText(comment.getCreatedAt());

            // Check if comment belongs to current user
            if (comment.getUserId() == sessionManager.getUserId()) {
                itemView.setOnLongClickListener(v -> {
                    showDeleteDialog(comment, position);
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
            }
        }
    }
}
