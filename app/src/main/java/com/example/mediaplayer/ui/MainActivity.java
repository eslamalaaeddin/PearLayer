package com.example.mediaplayer.ui;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.R;
import com.example.mediaplayer.helpers.MediaAdapter;
import com.example.mediaplayer.listeners.MediaClickListener;
import com.example.mediaplayer.models.Audio;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MediaClickListener {
    private static final String TAG="MainActivity";
    private final ArrayList<Audio> audioList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        grantPermissionsAndLoadAudio();

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                loadAudio();
                initRecyclerView();
            } else {
                Toast.makeText(MainActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onMediaClicked(int index, Audio audio) {
        playAudio(index, audio);
    }

    private void grantPermissionsAndLoadAudio(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE},
                    1);
        }
        else {
            loadAudio();
            initRecyclerView();
        }
    }

    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " DESC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                }
                String artist = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                }
                long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));


                // Save to audioList
                audioList.add(new Audio(data, title, album, artist, albumId));
                Log.i(TAG, "ISLAM loadAudio: "+audioList.get(0));
            }
        }
        if (cursor != null){
            cursor.close();
        }
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        if (audioList.size() > 0) {
            int itemLayout;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                itemLayout = R.layout.item_layout_portrait;
//                recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
            }
            else {
                itemLayout = R.layout.item_layout_landscape;
            }

            MediaAdapter adapter = new MediaAdapter(audioList, getApplication(), this, itemLayout);
            recyclerView.setAdapter(adapter);

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    switch (newState) {
                        case RecyclerView.SCROLL_STATE_IDLE:
                            Log.i(TAG, "OOO onScrollStateChanged: The RecyclerView is not scrolling");
                            break;
                        case RecyclerView.SCROLL_STATE_DRAGGING:
                            Log.i(TAG, "OOO onScrollStateChanged: Scrolling now");
                            break;
                        case RecyclerView.SCROLL_STATE_SETTLING:
                            Log.i(TAG, "OOO onScrollStateChanged: Scroll Settling");
                            break;

                    }
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dx > 0) {
                        Log.i(TAG, "OOO onScrolled: Right");
                    } else if (dx < 0) {
                        Log.i(TAG, "OOO onScrolled: Left");
                    } else {
                        Log.i(TAG, "OOO onScrolled: No horizontal scroll");
                    }

                    if (dy > 0) {
                        Log.i(TAG, "OOO onScrolled: Downward");
                    } else if (dy < 0) {
                        Log.i(TAG, "OOO onScrolled: Upwards");
                    } else {
                        Log.i(TAG, "OOO onScrolled: No vertical scroll");
                    }
                }
            });

        }
        else {
            Snackbar snackbar = Snackbar
                    .make(recyclerView, "No media found", Snackbar.LENGTH_INDEFINITE);
            snackbar.show();

        }
    }

    private void playAudio(int audioIndex, Audio audio) {
        Intent tempIntent = new Intent(this, PlayingActivity.class);
        Gson gson = new Gson();
        String jsonArrayList = gson.toJson(audioList);
        Log.i(TAG, "HHH playAudio: " + audio);
        tempIntent.putExtra("index", audioIndex);
        tempIntent.putExtra("albumId", audio.getAlbumId());
        tempIntent.putExtra("jsonArrayList", jsonArrayList);

        startActivity(tempIntent);
    }

}