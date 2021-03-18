package com.example.mediaplayer.helpers;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.mediaplayer.R;

import java.io.FileDescriptor;

public class Utils {
    public static Bitmap getLogoBitmap(Context context, Long albumId) {
        Bitmap bm = null;
        try
        {
            final Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");

            Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);

            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(uri, "r");

            if (pfd != null)
            {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (Exception e) {
        }
        return bm;
    }

    public static void setMediaImage(Context context, long albumId, ImageView view){
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
        Glide.with(context).load(uri).placeholder(R.drawable.ic_logo).into(view);
    }
}
