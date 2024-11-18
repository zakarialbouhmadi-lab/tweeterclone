package com.zakarialbouhmadi.tweeterclone;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewComments;
    private EditText editTextComment;
    private CommentAdapter commentAdapter;
    private SessionManager sessionManager;
    private int tweetId;
    private static final String COMMENTS_URL = "https://blog.kraftsport.pl/api/twitter/get_comments.php";
    private static final String POST_COMMENT_URL = "https://blog.kraftsport.pl/api/twitter/post_comment.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        tweetId = getIntent().getIntExtra("tweet_id", -1);
        if (tweetId == -1) {
            finish();
            return;
        }

        sessionManager = new SessionManager(this);

        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        editTextComment = findViewById(R.id.editTextComment);
        Button buttonPostComment = findViewById(R.id.buttonPostComment);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        buttonPostComment.setOnClickListener(v -> postComment());

        loadComments();
    }

    private void loadComments() {
        String url = COMMENTS_URL + "?tweet_id=" + tweetId;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray comments = jsonResponse.getJSONArray("comments");
                            commentAdapter.setComments(comments);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this,
                        "Error loading comments", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(stringRequest);
    }

    private void postComment() {
        String content = editTextComment.getText().toString().trim();
        if (content.isEmpty()) return;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, POST_COMMENT_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            editTextComment.setText("");
                            loadComments();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this,
                        "Error posting comment", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tweet_id", String.valueOf(tweetId));
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                params.put("content", content);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }
}