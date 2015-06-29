package com.iktwo.spotifystreamer;

import kaaes.spotify.webapi.android.models.Artist;

public class ArtistResult {
    Artist artist;

    private transient int backgroundColor = 0;
    private transient int titleColor = 0;
    private transient int textColor = 0;

    public ArtistResult(Artist artist) {
        this.artist = artist;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
}
