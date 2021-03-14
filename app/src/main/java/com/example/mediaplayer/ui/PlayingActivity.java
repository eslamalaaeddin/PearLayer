package com.example.mediaplayer.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayer.Utils;
import com.example.mediaplayer.MediaPlayerService;
import com.example.mediaplayer.helpers.PlaybackStatus;
import com.example.mediaplayer.R;
import com.example.mediaplayer.helpers.StorageUtil;

import java.util.List;
import java.util.Locale;

import static com.example.mediaplayer.helpers.Constants.BROADCAST_CLOSE_NOTIFICATION;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_NEXT;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PAUSE;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PLAY;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PLAY_NEW_AUDIO;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PREVIOUS;
import static com.example.mediaplayer.ui.MainActivity.audioList;

public class PlayingActivity extends AppCompatActivity {
    private static final String TAG = "PlayingActivity";

    private boolean serviceBound = false;
    private Intent playerIntent;
    private MediaPlayerService mediaPlayerService;
    private long albumId;
    private NotificationBroadcastReceiver notificationBroadcastReceiver;

    private ImageView mediaImageView;
    private SeekBar seekBar;
    private ImageButton previousMediaImageButton;
    private ImageButton nextMediaImageButton;
    private ImageButton playMediaImageButton;
    private ImageButton previousSecondsImageButton;
    private ImageButton nextSecondsImageButton;
    private Button changeMediaSpeedButton;
    private ImageButton repeatMediaImageButton;

    private Handler handler;

    //Binding this Client to the AudioPlayer Service
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        Intent intent = getIntent();
        albumId = intent.getLongExtra("albumId", 0);
        int audioIndex = intent.getIntExtra("index", 0);

        int fromService = intent.getIntExtra("fromService", 0);
        initViews();

        if (fromService == 1){
//            Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();
        }
        else{
            playAudio(audioIndex);
//            Toast.makeText(this, "0", Toast.LENGTH_SHORT).show();
        }
        StorageUtil storageUtil = new StorageUtil(getApplicationContext());
        if (storageUtil.getRepeatingState()){
            repeatMediaImageButton.setImageResource(R.drawable.ic_repeat_filled);
        }
        else{
            repeatMediaImageButton.setImageResource(R.drawable.ic_repeat_stroked);
        }

        playMediaImageButton.setOnClickListener(v -> {
            if (serviceBound && isServiceRunning(MediaPlayerService.class.getName()) && mediaPlayerService.getStatus() == PlaybackStatus.PLAYING){
                playMediaImageButton.setImageResource(R.drawable.ic_play_button_arrow);
                mediaPlayerService.onPauseButtonClicked();
            }
            else{
                playMediaImageButton.setImageResource(R.drawable.ic_pause);
                //order is important
                mediaPlayerService.onChangeMediaPlayerSpeedClicked();
                mediaPlayerService.onPlayButtonClicked();
            }
        });

        previousSecondsImageButton.setOnClickListener(v -> {
            mediaPlayerService.onBackwardMediaButtonClicked();
        });

        nextSecondsImageButton.setOnClickListener(v -> {
            mediaPlayerService.onForwardMediaButtonClicked();
        });

        previousMediaImageButton.setOnClickListener(v -> {
            mediaPlayerService.onPreviousButtonClicked();
            long albId = mediaPlayerService.getAlbumIdToUpdateMediaImage();
            Utils.setMediaImage(PlayingActivity.this,albId, mediaImageView);
        });

        nextMediaImageButton.setOnClickListener(v -> {
            mediaPlayerService.onNextButtonClicked();
            long albId = mediaPlayerService.getAlbumIdToUpdateMediaImage();
            Utils.setMediaImage(PlayingActivity.this,albId, mediaImageView);
        });

        changeMediaSpeedButton.setOnClickListener(v -> {

            mediaPlayerService.onChangeMediaPlayerSpeedClicked();
            changeMediaSpeedButton.setText(String.format("%s x", mediaPlayerService.getPlaybackSpeed()));
        });

        repeatMediaImageButton.setOnClickListener(v -> {
            handleRepeatingUIState();
            mediaPlayerService.onRepeatMediaImageButtonClicked();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayerService.onSeekBarChanges(seekBar.getProgress());
            }
        });
    }

//    private void getRepeatingStateFromSharedPrefs() {
//        StorageUtil storage = new StorageUtil(getApplicationContext());
//        boolean repeatingState = storage.getRepeatingState();
//        if (repeatingState){
//            repeatMediaImageButton.setImageResource(R.drawable.ic_repeat_filled);
//        }
//        else{
//            repeatMediaImageButton.setImageResource(R.drawable.ic_repeat_stroked);
//        }
//    }

//    private void getPlayingSpeedFromShardPrefs() {
//        StorageUtil storage = new StorageUtil(getApplicationContext());
//        float mediaSpeed = storage.getMediaSpeed();
//        changeMediaSpeedButton.setText(String.format("%s x", mediaSpeed));
//    }

    private void handleRepeatingUIState(){
        StorageUtil storage = new StorageUtil(getApplicationContext());
        if (storage.getRepeatingState()){
            repeatMediaImageButton.setImageResource(R.drawable.ic_repeat_stroked);
            storage.setRepeatingState(false);
        }
        else{
            repeatMediaImageButton.setImageResource(R.drawable.ic_repeat_filled);
            storage.setRepeatingState(true);
        }

    }

    private void initViews() {
        mediaImageView = findViewById(R.id.mediaImageView);
        Utils.setMediaImage(PlayingActivity.this, albumId, mediaImageView);
        seekBar = findViewById(R.id.seekBar);
        previousMediaImageButton = findViewById(R.id.previousImageButton);
        nextMediaImageButton = findViewById(R.id.nextMediaImageButton);
        playMediaImageButton = findViewById(R.id.playButtonImageButton);
        previousSecondsImageButton = findViewById(R.id.previousSecondsImageButton);
        nextSecondsImageButton = findViewById(R.id.nextSecondsImageButton);
        repeatMediaImageButton = findViewById(R.id.repeatMediaImageButton);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            changeMediaSpeedButton = findViewById(R.id.changeMediaSpeedButton);
            changeMediaSpeedButton.setVisibility(View.VISIBLE);
        }


    }

    private void updateSeekBarPosition() {
        handler = new Handler();
        final TextView currentTimeTextView = findViewById(R.id.currentTimeTextView);
        final TextView maxTimeTextView = findViewById(R.id.maxTimeTextView);

        handler.post(new Runnable() {

            @Override
            public void run() {
                int progressInSeconds = 0;
                int duration = 0;
                if (mediaPlayerService != null && serviceBound && isServiceRunning(MediaPlayerService.class.getName())) {
                    try {
                        duration = mediaPlayerService.getDuration();
                        progressInSeconds = mediaPlayerService.getCurrentPosition();
                    }
                    catch (IllegalStateException ex){
                        //Toast.makeText(PlayingActivity.this, "Exception", Toast.LENGTH_SHORT).show();
                    }

                }
                int currentHours = progressInSeconds / 3600;
                int currentMinutes = (progressInSeconds % 3600) / 60;
                int currentSeconds = progressInSeconds % 60;

                int maxHours = duration / 3600;
                int maxMinutes = (duration % 3600) / 60;
                int maxSeconds = duration % 60;

                String currentTime = convertMediaDuration(currentSeconds, currentMinutes, currentHours);
                String maxTime = convertMediaDuration(maxSeconds, maxMinutes, maxHours);

                seekBar.setMax(duration);
                currentTimeTextView.setText(currentTime);
                maxTimeTextView.setText(maxTime);
                seekBar.setProgress(progressInSeconds);
                handler.postDelayed(this, 1000);
            }
        });
    }

    private String convertMediaDuration(int seconds, int minutes, int hours) {
        return String.format(Locale.getDefault(),
                "%d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!serviceBound){
            if (playerIntent == null){
                playerIntent = new Intent(this, MediaPlayerService.class);
            }
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else{
            if (isServiceRunning(MediaPlayerService.class.getName()) && mediaPlayerService.getStatus() != null) {
                if (mediaPlayerService.getStatus() == PlaybackStatus.PLAYING) {
                    playMediaImageButton.setImageResource(R.drawable.ic_pause);
                }
                else{
                    playMediaImageButton.setImageResource(R.drawable.ic_play_button_arrow);
                }
            }

        }
        updateSeekBarPosition();

        if (notificationBroadcastReceiver == null){
            notificationBroadcastReceiver = new NotificationBroadcastReceiver();
            IntentFilter intentFilter1 = new IntentFilter(BROADCAST_PREVIOUS);
            IntentFilter intentFilter2 = new IntentFilter(BROADCAST_CLOSE_NOTIFICATION);
            IntentFilter intentFilter3 = new IntentFilter(BROADCAST_NEXT);
            IntentFilter intentFilter4 = new IntentFilter(BROADCAST_PLAY);
            IntentFilter intentFilter5 = new IntentFilter(BROADCAST_PAUSE);

            registerReceiver(notificationBroadcastReceiver, intentFilter1);
            registerReceiver(notificationBroadcastReceiver, intentFilter2);
            registerReceiver(notificationBroadcastReceiver, intentFilter3);
            registerReceiver(notificationBroadcastReceiver, intentFilter4);
            registerReceiver(notificationBroadcastReceiver, intentFilter5);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayerService != null){
            changeMediaSpeedButton.setText(String.format("%s x", mediaPlayerService.getPlaybackSpeed()));
        }
    }

    private void playAudio(int audioIndex) {
        StorageUtil storage = new StorageUtil(getApplicationContext());
        if (!isServiceRunning(MediaPlayerService.class.getName())) {
            if (!serviceBound) {
                //Store Serializable audioList to SharedPreferences
                storage.storeAudio(audioList);
                storage.storeAudioIndex(audioIndex);
                playerIntent = new Intent(this, MediaPlayerService.class);
                ContextCompat.startForegroundService(this, playerIntent);
                bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        }
        //Foreground is running
        else {
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
        }
        // if i did'nt comment this, it would throw an exception if i want to just use the notification without the playing activ..
        //new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();

        if (notificationBroadcastReceiver != null) {
            unregisterReceiver(notificationBroadcastReceiver);
        }
    }


    private boolean isServiceRunning(String serviceName) {
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : l) {
            if (runningServiceInfo.service.getClassName().equals(serviceName)) {
                serviceRunning = true;
                break;
            }
        }
        return serviceRunning;
    }

    class NotificationBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_PREVIOUS)){
                mediaPlayerService.onPreviousButtonClicked();
                long albId = mediaPlayerService.getAlbumIdToUpdateMediaImage();
                Utils.setMediaImage(PlayingActivity.this, albId, mediaImageView);
            }

            if (intent.getAction().equals(BROADCAST_NEXT)){
                mediaPlayerService.onNextButtonClicked();
                long albId = mediaPlayerService.getAlbumIdToUpdateMediaImage();
                Utils.setMediaImage(PlayingActivity.this, albId, mediaImageView);
            }

            if (intent.getAction().equals(BROADCAST_CLOSE_NOTIFICATION)){
                finishAffinity();
            }

            if (intent.getAction().equals(BROADCAST_PLAY)){
                playMediaImageButton.setImageResource(R.drawable.ic_pause);
            }

            if (intent.getAction().equals(BROADCAST_PAUSE)){
                playMediaImageButton.setImageResource(R.drawable.ic_play_button_arrow);
            }

        }
    }

}

