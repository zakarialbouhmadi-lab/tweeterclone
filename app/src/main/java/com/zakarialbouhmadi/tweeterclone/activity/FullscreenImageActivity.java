package com.zakarialbouhmadi.tweeterclone.activity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.zakarialbouhmadi.tweeterclone.R;

public class FullscreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make fullscreen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_fullscreen_image);

        ImageView imageView = findViewById(R.id.imageViewFullscreen);
        ImageButton buttonClose = findViewById(R.id.buttonClose);

        String imageUrl = getIntent().getStringExtra("image_url");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(imageView);
        }

        buttonClose.setOnClickListener(v -> finish());
        
        // Click anywhere to close
        imageView.setOnClickListener(v -> finish());
    }
}
