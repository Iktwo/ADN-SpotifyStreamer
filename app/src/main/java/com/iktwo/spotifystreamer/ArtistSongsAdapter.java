package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
            holder.artist = (TextView) convertView.findViewById(R.id.text_view_name);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.image_view_thumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.artist.setText(track.name);

        /* if (!artistResult.artist.images.isEmpty()) {
            String url = artistResult.artist.images.get(0).url.replace("55x55", "400x400");
            holder.thumbnail.setTag(artistResult.artist.images.get(0).url);

            if (artistResult.getBackgroundColor() == 0) {
                Picasso.with(mContext)
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
                Picasso.with(mContext)
                        .load(url)
                        .into(holder.thumbnail);

                holder.background.setBackgroundColor(artistResult.getBackgroundColor());
                holder.artist.setTextColor(artistResult.getTextColor());
            }
        } */

        return convertView;
    }

    static class ViewHolder {
        public TextView artist;
        public ImageView thumbnail;
    }
}
