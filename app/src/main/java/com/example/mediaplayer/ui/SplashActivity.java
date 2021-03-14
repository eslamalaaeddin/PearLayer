package com.example.mediaplayer.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.example.mediaplayer.R;

public class SplashActivity extends AppCompatActivity {
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_splash);

        handler = new Handler();
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

//        getSupportActionBar().hide();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}