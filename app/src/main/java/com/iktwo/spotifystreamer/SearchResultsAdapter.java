package com.iktwo.spotifystreamer;

import android.content.Context;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.florent37.picassopalette.PicassoPalette;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

public class SearchResultsAdapter extends BaseAdapter {
    public String TAG = "SearchResultsAdapter";

    private List<Artist> mData = new ArrayList<Artist>();
    private Context mContext;
    private LayoutInflater inflater;

    public SearchResultsAdapter(Context context) {
        mContext = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final Artist artist = (Artist) getItem(position);

        if (convertView == null && inflater != null) {
            convertView = inflater.inflate(R.layout.artist_delegate, parent, false);
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

            Picasso.with(mContext)
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
            Picasso.with(mContext)
                    .load(R.drawable.placeholder_artist)
                    .into(holder.thumbnail);

            holder.background.setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimary));
            holder.title.setTextColor(mContext.getResources().getColor(R.color.abc_primary_text_material_dark));
        }

        return convertView;
    }

    public void setData(List<Artist> data) {
        mData = data;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        public TextView title;
        public SquareImageViewByWidth thumbnail;
        public View background;
    }
}
