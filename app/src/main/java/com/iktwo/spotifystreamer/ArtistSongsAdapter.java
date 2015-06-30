package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class ArtistSongsAdapter extends ArrayAdapter<Track> {
    private static final String TAG = ArtistSongsAdapter.class.getSimpleName();

    public ArtistSongsAdapter(Activity context, List<Track> songs) {
        super(context, 0, songs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final Track track = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.song_delegate, parent, false);

            holder = new ViewHolder();
            holder.song = (TextView) convertView.findViewById(R.id.text_view_name);
            holder.album = (TextView) convertView.findViewById(R.id.text_view_album);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.image_view_thumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.song.setText(track.name);
        holder.album.setText(track.album.name);

        if (!track.album.images.isEmpty()) {
            String url = track.album.images.get(0).url;

            holder.thumbnail.setTag(url);

            Picasso.with(getContext())
                    .load(url)
                    .placeholder(R.drawable.placeholder_artist)
                    .into(holder.thumbnail);
        } else {
            Picasso.with(getContext())
                    .load(R.drawable.placeholder_artist)
                    .into(holder.thumbnail);
        }

        return convertView;
    }

    static class ViewHolder {
        public TextView song;
        public TextView album;
        public ImageView thumbnail;
    }
}
