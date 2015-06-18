package com.iktwo.spotifystreamer;

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