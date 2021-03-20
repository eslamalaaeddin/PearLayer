package com.example.mediaplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mediaplayer.helpers.Constants;
import com.example.mediaplayer.helpers.PlaybackStatus;
import com.example.mediaplayer.helpers.StorageUtil;
import com.example.mediaplayer.helpers.Utils;
import com.example.mediaplayer.listeners.MediaStateListener;
import com.example.mediaplayer.listeners.SeekBarListener;
import com.example.mediaplayer.models.Audio;
import com.example.mediaplayer.ui.MainActivity;
import com.example.mediaplayer.ui.PlayingActivity;

import java.io.IOException;
import java.util.ArrayList;

import static com.example.mediaplayer.helpers.Constants.ACTION_EXIT;
import static com.example.mediaplayer.helpers.Constants.ACTION_NEXT;
import static com.example.mediaplayer.helpers.Constants.ACTION_PAUSE;
import static com.example.mediaplayer.helpers.Constants.ACTION_PLAY;
import static com.example.mediaplayer.helpers.Constants.ACTION_PREVIOUS;
import static com.example.mediaplayer.helpers.Constants.ACTION_STOP;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_CLOSE_NOTIFICATION;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_NEXT;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PAUSE;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PLAY;
import static com.example.mediaplayer.helpers.Constants.BROADCAST_PREVIOUS;
import static com.example.mediaplayer.helpers.Constants.CHANNEL_ID;
import static com.example.mediaplayer.helpers.Constants.CHANNEL_NAME;
import static com.example.mediaplayer.helpers.Constants.NOTIFICATION_ID;


public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener,
        SeekBarListener,
        MediaStateListener {

    private static final String TAG = "MediaPlayerService";
    private MediaPlayer mediaPlayer;

    private int resumePosition;

    private AudioManager audioManager;

    public PlaybackStatus status;

    private final IBinder iBinder = new LocalBinder();

    private boolean hasACall = false;

    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio;

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    private float playbackSpeed = 1.0f;
    private boolean repeating = false;
    private boolean playingBeforeCall = false;

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        registerBecomingNoisyReceiver();
        registerPlayNewAudio();
        registerVolumeChange();
    }

    @Override
    //1/Initialize storage
    //2/Request audio focus or stop the service
    //3/Initialize media session
    //4/Initialize media player
    //5/Build notification
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            repeating = storage.getRepeatingState();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            status = PlaybackStatus.PLAYING;
            buildNotification(PlaybackStatus.PLAYING);

        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
//        return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public int getCurrentPosition() {
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getCurrentPosition() / 1000;
    }

    public int getDuration() {
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getDuration() / 1000;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //this can be used for infinity repeat.
        if (repeating) {
            mp.start();
        } else {
            //it works but the only issue is that you have to press the start button twice so that it plays again.
            stopMedia();
//            buildNotification(PlaybackStatus.PAUSED);
//            status = PlaybackStatus.PAUSED;
//            Intent intent = new Intent(BROADCAST_PAUSE);
//            sendBroadcast(intent);
            //stopSelf();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (hasACall) {
                    hasACall = false;
                } else {
                    // resume playback
                    if (mediaPlayer == null) {
                        initMediaPlayer();
//                        Toast.makeText(this, "Some", Toast.LENGTH_SHORT).show();
                    } else if (!mediaPlayer.isPlaying()) {
                        if (status == PlaybackStatus.PLAYING){
//                            Toast.makeText(this, "Thing", Toast.LENGTH_SHORT).show();
                            mediaPlayer.start();
                        }

                    }
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }

                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
//                    Intent intent = new Intent(BROADCAST_PAUSE);
//                    sendBroadcast(intent);
                }
//                mediaPlayer.stop();
//                mediaPlayer.release();
//                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                //Like when you start a WhatsApp message
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    public void onSeekBarChanges(int currentPosition) {
        mediaPlayer.seekTo(currentPosition * 1000);
    }



    public long getAlbumIdToUpdateMediaImage() {
        if (activeAudio == null) {
            return 0;
        }
        return activeAudio.getAlbumId();
    }

    public boolean getRepeatingState() {
        return repeating;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    @Override
    public void onPreviousButtonClicked() {
        //Notification is correct but it still plays
        skipToPrevious();
//        if (mediaPlayer.isPlaying()) {
//            buildNotification(PlaybackStatus.PLAYING);
//        } else {
//            buildNotification(PlaybackStatus.PAUSED);
//        }
//        Toast.makeText(this, "PreviousClicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNextButtonClicked() {
        //Notification is correct but it still plays
        skipToNext();
//        Toast.makeText(this, "NextClicked", Toast.LENGTH_SHORT).show();
//        if (mediaPlayer.isPlaying()) {
//            buildNotification(PlaybackStatus.PLAYING);
//        } else {
//            buildNotification(PlaybackStatus.PAUSED);
//        }
    }

    @Override
    public void onPauseButtonClicked() {
        pauseMedia();
        buildNotification(PlaybackStatus.PAUSED);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onPlayButtonClicked() {
        playMedia();
        buildNotification(PlaybackStatus.PLAYING);
    }

    @Override
    public void onForwardMediaButtonClicked() {
        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 5000);
    }

    @Override
    public void onBackwardMediaButtonClicked() {
        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 5000);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onChangeMediaPlayerSpeedClicked() {
        if (status == PlaybackStatus.PLAYING) {
            if (playbackSpeed == 3.0) {
                playbackSpeed = .25f;
            } else {
                playbackSpeed += .25;
            }
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
        }
    }

    @Override
    public void onRepeatMediaImageButtonClicked() {
        repeating = !repeating;
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //Focus gained
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        //Could not gain focus
    }

    private void removeAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
//        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
//                status = PlaybackStatus.PLAYING;
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
//                status = PlaybackStatus.PAUSED;
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
//                Toast.makeText(MediaPlayerService.this, "OnSkpNxt", Toast.LENGTH_SHORT).show();
                skipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
//                Toast.makeText(MediaPlayerService.this, "OnSkpPrvs", Toast.LENGTH_SHORT).show();
                skipToPrevious();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    public PlaybackStatus getStatus() {
        return status;
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();
        //MUST be called before prepareAsync()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            Log.i(TAG, "KOKO initMediaPlayer: " + activeAudio);
            mediaPlayer.setDataSource(activeAudio.getData());
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

//        mediaPlayer.prepareAsync();
    }

    ///////////////////////////////////////

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            status = PlaybackStatus.PLAYING;
            buildNotification(PlaybackStatus.PLAYING);
            Intent intent = new Intent(BROADCAST_PLAY);
            sendBroadcast(intent);
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        status = PlaybackStatus.PAUSED;
        buildNotification(PlaybackStatus.PAUSED);
        Intent intent = new Intent(BROADCAST_PAUSE);
        sendBroadcast(intent);
    }

    private void pauseMedia() {
        //if the internal player engine has not been
        //     * initialized or has been released.
        //Solution ==> I removed the check if it is playing and it worked fine.
//        if (mediaPlayer.isPlaying()) {
        mediaPlayer.pause();
        status = PlaybackStatus.PAUSED;
//            buildNotification(PlaybackStatus.PAUSED);
        resumePosition = mediaPlayer.getCurrentPosition();
//        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    private void skipToNext() {

        //if last --> begin from 0
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            activeAudio = audioList.get(++audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();

    }

    private void skipToPrevious() {
        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();

    }

    ///////////////////////////////////////

    private void buildNotification(PlaybackStatus playbackStatus) {

        //1 Create the channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);

            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_description));

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }


        int notificationAction = R.drawable.ic_pause;//needs to be initialized
        PendingIntent playPauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            //create the pause action
            playPauseAction = playbackAction(1);
        }
        else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.ic_play_button_arrow;
            //create the play action
            playPauseAction = playbackAction(0);
        }

        Bitmap largeIcon = Utils.getLogoBitmap(this, activeAudio.getAlbumId());
        Log.i(TAG, "MAWZY buildNotification: " + largeIcon);
        if (largeIcon == null){
            largeIcon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_notification_logo);
        }
        Intent playingActivityIntent = new Intent(this, PlayingActivity.class);

        playingActivityIntent.putExtra("albumId", activeAudio.getAlbumId());
        playingActivityIntent.putExtra("index", audioIndex);
        playingActivityIntent.putExtra("fromService", 1);
        if (status == PlaybackStatus.PLAYING) {
            playingActivityIntent.putExtra("status", 1);
        } else {
            playingActivityIntent.putExtra("status", 0);
        }
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(playingActivityIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_NAME)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .setChannelId(CHANNEL_ID)
                .setAutoCancel(false)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.ic_logo)
//                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentTitle(activeAudio.getTitle())
                .setContentInfo(activeAudio.getTitle())
                .setContentIntent(resultPendingIntent)
                // Add playback actions
                .addAction(R.drawable.ic_previous, "Previous", playbackAction(3))
                .addAction(notificationAction, "Pause", playPauseAction)
                .addAction(R.drawable.ic_next_, "Next", playbackAction(2))
                .addAction(R.drawable.ic_cancel, "Exit", playbackAction(4));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackActionIntent = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackActionIntent.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 1:
                // Pause
                playbackActionIntent.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 2:
                // Next track
                playbackActionIntent.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 3:
                // Previous track
                playbackActionIntent.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 4:
                // Exit
                playbackActionIntent.setAction(ACTION_EXIT);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            default:
                break;
        }
        return null;
    }


    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            //this is considered with the notification itself
            transportControls.play();
            status = PlaybackStatus.PLAYING;
            //this is considered with the UI (PlayingActivity)
            Intent intent = new Intent(BROADCAST_PLAY);
            sendBroadcast(intent);
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
            status = PlaybackStatus.PAUSED;
            Intent intent = new Intent(BROADCAST_PAUSE);
            sendBroadcast(intent);
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
            Intent intent = new Intent(BROADCAST_NEXT);
            sendBroadcast(intent);

        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
            Intent intent = new Intent(BROADCAST_PREVIOUS);
            sendBroadcast(intent);
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        } else if (actionString.equalsIgnoreCase(ACTION_EXIT)) {
            stopMedia();
            stopForeground(true);
            stopSelf();
            Intent finishIntent = new Intent(BROADCAST_CLOSE_NOTIFICATION);
            sendBroadcast(finishIntent);
        }

    }


    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            Intent intent = new Intent(BROADCAST_PAUSE);
                            sendBroadcast(intent);
                            hasACall = true;
                        }
                        break;
//                    case TelephonyManager.CALL_STATE_IDLE:
//                        // Phone idle. Start playing.
//                        if (mediaPlayer != null) {
//                            if (hasACall) {
////                                hasACall = false;
////                                pauseMedia();
////                                Intent intent = new Intent(BROADCAST_PAUSE);
////                                sendBroadcast(intent);
////                                Intent intent = new Intent(BROADCAST_PLAY);
////                                sendBroadcast(intent);
////                                resumeMedia();
//                            }
//                        }
//                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    //Becoming noisy (Detach Head phone)
    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    //VolumeChange
    private final BroadcastReceiver volumeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int volume = (Integer)intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");
            if (volume == 0){
                status = PlaybackStatus.PAUSED;
                Intent pauseMediaIntent = new Intent(BROADCAST_PAUSE);
                sendBroadcast(pauseMediaIntent);
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }
        }
    };

    private final BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            //updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void registerPlayNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private void registerVolumeChange() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(Constants.ACTION_VOLUME_CHANGE);
        registerReceiver(volumeChangeReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }

        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(volumeChangeReceiver);
        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }

}