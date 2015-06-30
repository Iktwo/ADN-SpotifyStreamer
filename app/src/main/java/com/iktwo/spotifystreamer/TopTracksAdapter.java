package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.florent37.picassopalette.PicassoPalette;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

// This will fetch top tracks from itunes rss and then search those artist in Spotify as there
// doesn't seem to be any official API to get top artists or tracks in Spotify
// this is done just so the initial screen is not empty waiting for you to search
public class TopTracksAdapter extends ArrayAdapter<ArtistResult> {
    private static final String TAG = TopTracksAdapter.class.getSimpleName();

    public TopTracksAdapter(Activity context, List<ArtistResult> songs) {
        super(context, 0, songs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final ArtistResult artistResult = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.artist_delegate, parent, false);
            holder = new ViewHolder();
            holder.background = convertView.findViewById(R.id.background);
            holder.artist = (TextView) convertView.findViewById(R.id.text_view_artist);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.image_view_thumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.artist.setText(artistResult.artist.name);

        if (!artistResult.artist.images.isEmpty()) {
            String url = artistResult.artist.images.get(0).url.replace("55x55", "400x400");
            holder.thumbnail.setTag(artistResult.artist.images.get(0).url);

            if (artistResult.getBackgroundColor() == 0) {
                Picasso.with(getContext())
                        .load(url)
                        .into(holder.thumbnail,
                                PicassoPalette.with(url, holder.thumbnail)
                                        .use(PicassoPalette.Profile.VIBRANT)
                                        .intoBackground(holder.background, PicassoPalette.Swatch.RGB)
                                        .intoTextColor(holder.artist, PicassoPalette.Swatch.TITLE_TEXT_COLOR)
                                        .intoCallBack(
                                                new PicassoPalette.CallBack() {
                                                    @Override
                                                    public void onPaletteLoaded(Palette palette) {
                                                        Palette.Swatch s = palette.getVibrantSwatch();

                                                        // If there is a vibrant color, do nothing
                                                        if (s != null) {
                                                            artistResult.setBackgroundColor(s.getRgb());
                                                            artistResult.setTextColor(s.getBodyTextColor());
                                                            artistResult.setTitleColor(s.getTitleTextColor());
                                                            return;
                                                        }

                                                        for (int i = 0; i < 5; i++) {
                                                            switch (i) {
                                                                case 0:
                                                                    s = palette.getDarkVibrantSwatch();
                                                                    break;
                                                                case 1:
                                                                    s = palette.getLightVibrantSwatch();
                                                                    break;
                                                                case 2:
                                                                    s = palette.getMutedSwatch();
                                                                    break;
                                                                case 3:
                                                                    s = palette.getDarkMutedSwatch();
                                                                    break;
                                                                case 4:
                                                                    s = palette.getLightMutedSwatch();
                                                                    break;
                                                            }

                                                            if (s != null) {
                                                                holder.background.setBackgroundColor(s.getRgb());
                                                                holder.artist.setTextColor(s.getTitleTextColor());

                                                                artistResult.setBackgroundColor(s.getRgb());
                                                                artistResult.setTextColor(s.getBodyTextColor());
                                                                artistResult.setTitleColor(s.getTitleTextColor());

                                                                return;
                                                            }
                                                        }
                                                    }
                                                }));
            } else {
                Picasso.with(getContext())
                        .load(url)
                        .into(holder.thumbnail);

                holder.background.setBackgroundColor(artistResult.getBackgroundColor());
                holder.artist.setTextColor(artistResult.getTextColor());
            }
        }

        return convertView;
    }

    static class ViewHolder {
        public TextView artist;
        public ImageView thumbnail;
        public View background;
    }
}
