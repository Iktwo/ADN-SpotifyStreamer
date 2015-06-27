package com.iktwo.spotifystreamer;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ItunesSong {
    @SerializedName("im:name")
    public Name name;
    @SerializedName("im:image")
    public List<Image> image = new ArrayList<Image>();
    @SerializedName("im:artist")
    public Artist artist;

    private transient int backgroundColor = 0;
    private transient int titleColor = 0;
    private transient int textColor = 0;

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

    class Name {
        public String label;
    }

    class Image {
        public String label;
    }

    class Artist {
        public String label;
    }
}