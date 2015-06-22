package com.iktwo.spotifystreamer;

import android.content.Context;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.florent37.picassopalette.PicassoPalette;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class TopTracksAdapter extends BaseAdapter implements HttpAsyncRequest.AsyncResponse {
    public String TAG = "TopTracksAdapter";
    public List<ItunesSong> songs = new ArrayList<ItunesSong>();
    private Context mContext;
    private LayoutInflater inflater;

    public TopTracksAdapter(Context context) {
        mContext = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        /// Store this on a DB and query if empty or last insertion is old
        new HttpAsyncRequest(this).execute("https://itunes.apple.com/us/rss/topsongs/limit=30/json");
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final ItunesSong song = (ItunesSong) getItem(position);

        if (convertView == null && inflater != null) {
            convertView = inflater.inflate(R.layout.top_tracks_delegate, parent, false);
            holder = new ViewHolder();
            holder.background = convertView.findViewById(R.id.background);
            holder.title = (TextView) convertView.findViewById(R.id.text_view_title);
            holder.artist = (TextView) convertView.findViewById(R.id.text_view_artist);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.image_view_thumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(song.name.label);
        holder.artist.setText(song.artist.label);

        if (!song.image.isEmpty()) {
            String url = song.image.get(0).label.replace("55x55", "400x400");
            holder.thumbnail.setTag(song.image.get(0).label);
            Picasso.with(mContext)
                    .load(url)
                    .into(holder.thumbnail,
                            PicassoPalette.with(url, holder.thumbnail)
                                    .use(PicassoPalette.Profile.VIBRANT)
                                    .intoBackground(holder.background, PicassoPalette.Swatch.RGB)
                                    .intoTextColor(holder.artist, PicassoPalette.Swatch.TITLE_TEXT_COLOR)
                                    .intoTextColor(holder.title, PicassoPalette.Swatch.BODY_TEXT_COLOR)
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
                                                            holder.artist.setTextColor(s.getTitleTextColor());
                                                            holder.title.setTextColor(s.getBodyTextColor());
                                                            return;
                                                        }
                                                    }
                                                }
                                            }));
        }

        return convertView;
    }

    @Override
    public void processFinish(ArrayList<String> reply) {
        ItunesTopTracks tracks = new Gson().fromJson(reply.get(1), ItunesTopTracks.class);

        songs = tracks.feed.entry;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        public TextView title;
        public TextView artist;
        public ImageView thumbnail;
        public View background;
    }

    private class ItunesTopTracks {
        public Feed feed;

        class Feed {
            public List<ItunesSong> entry = new ArrayList<ItunesSong>();
        }
    }
}
