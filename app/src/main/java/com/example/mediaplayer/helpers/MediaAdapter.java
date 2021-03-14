package com.example.mediaplayer.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.Utils;
import com.example.mediaplayer.models.Audio;
import com.example.mediaplayer.R;
import com.example.mediaplayer.listeners.MediaClickListener;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
    private static final String TAG = "MediaAdapter";
    List<Audio> list;
    Context context;
    MediaClickListener mediaClickListener;
    private int itemLayout;

    public MediaAdapter(List<Audio> list, Context context, MediaClickListener listener, int itemLayout) {
        this.list = list;
        this.context = context;
        this.mediaClickListener = listener;
        this.itemLayout = itemLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)  {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new ViewHolder(v);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Audio audio = list.get(holder.getAdapterPosition());
        holder.title.setText("  "+list.get(position).getTitle()+"  ");
        Utils.setMediaImage(context, audio.getAlbumId(), holder.mediaImageView);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title;
        ImageView mediaImageView;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            title = itemView.findViewById(R.id.title);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
        }

        @Override
        public void onClick(View v) {
            Audio currentAudio = list.get(getAdapterPosition());
            mediaClickListener.onMediaClicked(getAdapterPosition(), currentAudio);
        }
    }

}

