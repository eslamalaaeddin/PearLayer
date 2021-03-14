package com.example.mediaplayer.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Audio implements Serializable {
    private String data;
    private String title;
    private String album;
    private String artist;
    private long albumId;

    public Audio(String data, String title, String album, String artist, long albumId) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.albumId = albumId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @NonNull
    @Override
    public String toString() {
        return "Audio{" +
                "data='" + data + '\'' +
                ", title='" + title + '\'' +
                ", album='" + album + '\'' +
                ", artist='" + artist + '\'' +
                '}';
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }
}
