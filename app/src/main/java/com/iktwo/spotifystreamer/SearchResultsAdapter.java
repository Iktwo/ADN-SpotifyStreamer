package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.florent37.picassopalette.PicassoPalette;
import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

public class SearchResultsAdapter extends ArrayAdapter<Artist> {
    private static final String TAG = SearchResultsAdapter.class.getSimpleName();

    public SearchResultsAdapter(Activity context, List<Artist> songs) {
        super(context, 0, songs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final Artist artist = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.search_result_delegate, parent, false);
            holder = new ViewHolder();
            holder.background = convertView.findViewById(R.id.background);
            holder.title = (TextView) convertView.findViewById(R.id.text_view_artist);
            holder.thumbnail = (SquareImageViewByWidth) convertView.findViewById(R.id.image_view_thumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(artist.name);

        if (!artist.images.isEmpty()) {
            String url = artist.images.get(0).url;

            Picasso.with(getContext())
                    .load(url)
                    .placeholder(R.drawable.placeholder_artist)
                    .into(holder.thumbnail,
                            PicassoPalette.with(url, holder.thumbnail)
                                    .use(PicassoPalette.Profile.VIBRANT)
                                    .intoBackground(holder.background, PicassoPalette.Swatch.RGB)
                                    .intoTextColor(holder.title, PicassoPalette.Swatch.TITLE_TEXT_COLOR)
                                    .intoCallBack(
                                            new PicassoPalette.CallBack() {
                                                @Override
                                                public void onPaletteLoaded(Palette palette) {
                                                    Palette.Swatch s = palette.getVibrantSwatch();

                                                    // If there is a vibrant color, do nothing
                                                    if (s != null)
                                                        return;

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
                                                            holder.title.setTextColor(s.getTitleTextColor());

                                                            return;
                                                        }
                                                    }
                                                }
                                            }));
        } else {
            Picasso.with(getContext())
                    .load(R.drawable.placeholder_artist)
                    .into(holder.thumbnail);

            holder.background.setBackgroundColor(getContext().getResources().getColor(R.color.colorPrimary));
            holder.title.setTextColor(getContext().getResources().getColor(R.color.abc_primary_text_material_dark));
        }

        return convertView;
    }

    static class ViewHolder {
        public TextView title;
        public SquareImageViewByWidth thumbnail;
        public View background;
    }
}
