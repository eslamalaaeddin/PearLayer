package com.example.mediaplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.example.mediaplayer.ui.MainActivity;
import com.example.mediaplayer.ui.PlayingActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import android.view.View;

public class TestActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "166";
    public static final String CHANNEL_NAME = "My channel";
    public static final int NOTIFICATION_ID = 123;
    public static final String NOTIFICATION_TITLE = "Testing";
    public static final String CONTENT_TEXT = "Dummy notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyMe();
            }
        });
    }

    //template code to create simple notification
    void notifyMe () {
        //1 Create the channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription( getString(R.string.channel_description));

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        //2 Create the builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this , CHANNEL_NAME)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(CONTENT_TEXT)
                .setChannelId(CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long [] {0,3000})
                .setAutoCancel(true);
        //3 Create the action
        Intent actionIntent = new Intent(TestActivity.this , PlayingActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(actionIntent);
// Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(this , 0 , actionIntent , PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        //4 Issue the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }



}