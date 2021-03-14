package com.example.mediaplayer.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.models.Audio;
import com.example.mediaplayer.helpers.MediaAdapter;
import com.example.mediaplayer.MediaPlayerService;
import com.example.mediaplayer.R;
import com.example.mediaplayer.listeners.MediaClickListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MediaClickListener {
    private static final String TAG="MainActivity";
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.mediaplayer.PlayNewAudio";
    // Change to your package name
    private MediaPlayerService player;
    boolean serviceBound = false;
    static ArrayList<Audio> audioList;
    private int itemLayout;


    //Binding this Client to the AudioPlayer Service
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }


        else if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    1);
        }
        else {
            loadAudio();
            initRecyclerView();
        }


//        if (getIntent().getData()!= null){
//            Uri data = getIntent().getData();
//            MediaPlayer mediaPlayer = MediaPlayer.create(this, data);
//            mediaPlayer.start();
//            //content://0@media/external/audio/media/33207
//        }




    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                   ){
                loadAudio();
                initRecyclerView();
            } else {
                Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Get Audios from phone, and add them to list
    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " DESC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));


                // Save to audioList
                audioList.add(new Audio(data, title, album, artist, albumId));
                Log.i(TAG, "ISLAM loadAudio: "+audioList.get(0));
            }
        }
        assert cursor != null;
        cursor.close();
    }

    private void initRecyclerView() {
        if (audioList.size() > 0) {
            RecyclerView recyclerView = findViewById(R.id.recyclerview);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                itemLayout = R.layout.item_layout_portrait;
//                recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
            }
            else {
                itemLayout = R.layout.item_layout_landscape;
            }
            MediaAdapter adapter = new MediaAdapter(audioList, getApplication(), this, itemLayout);
            recyclerView.setAdapter(adapter);

        }
    }

    private void playAudio(int audioIndex, Audio audio) {
        //Check is service is active
//        StorageUtil storage = new StorageUtil(getApplicationContext());
//        if (!serviceBound) {
//            //Store Serializable audioList to SharedPreferences
//            storage.storeAudio(audioList);
//            storage.storeAudioIndex(audioIndex);
//
//            Intent playerIntent = new Intent(this, MediaPlayerService.class);
////            startService(playerIntent);
//
//            ContextCompat.startForegroundService(this, playerIntent);
//            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//        } else {
//            //Store the new audioIndex to SharedPreferences
//            storage.storeAudioIndex(audioIndex);
//            //Service is active
//            //Send a broadcast to the service -> PLAY_NEW_AUDIO
//            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
//            sendBroadcast(broadcastIntent);
//        }
        Intent tempIntent = new Intent(this, PlayingActivity.class);
        tempIntent.putExtra("index", audioIndex);
        tempIntent.putExtra("albumId", audio.getAlbumId());
        startActivity(tempIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
//            player.stopSelf();
        }
    }




    @Override
    public void onMediaClicked(int index, Audio audio) {
//        Intent audioIntent = new Intent(this, PlayingActivity.class);
//        //temp, pass the whole object via intent
//        audioIntent.putExtra("data", audio.getData());
//        audioIntent.putExtra("title", audio.getTitle());
//        audioIntent.putExtra("album", audio.getAlbum());
//        audioIntent.putExtra("artist", audio.getArtist());
//        audioIntent.putExtra("albumId", audio.getAlbumId());
//        audioIntent.putExtra("audioIndex", index);
//
//        startActivity(audioIntent);
        playAudio(index, audio);
    }

}