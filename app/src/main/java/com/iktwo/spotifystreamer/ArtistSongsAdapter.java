package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.florent37.picassopalette.PicassoPalette;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ArtistSongsAdapter extends BaseAdapter implements HttpAsyncRequest.AsyncResponse {
    public String TAG = "ArtistSongsAdapter";
    private List<ArtistResult> mArtists = new ArrayList<ArtistResult>();
    private Context mContext;
    private LayoutInflater inflater;

    public ArtistSongsAdapter(Context context) {
        mContext = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        /// TODO: Store this on a DB and query if empty or last insertion is old
        new HttpAsyncRequest(this).execute("https://itunes.apple.com/us/rss/topsongs/limit=30/json");
    }

    @Override
    public int getCount() {
        return mArtists.size();
    }

    @Override
    public Object getItem(int position) {
        return mArtists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final ArtistResult artistResult = (ArtistResult) getItem(position);

        if (convertView == null && inflater != null) {
            convertView = inflater.inflate(R.layout.top_tracks_delegate, parent, false);
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
        }

        return convertView;
    }

    @Override
    public void processFinish(ArrayList<String> reply) {
        if (!reply.isEmpty() && !reply.get(0).equals("error")) {
            final ItunesTopTracks tracks = new Gson().fromJson(reply.get(1), ItunesTopTracks.class);

            ArrayList<String> searchedArtist = new ArrayList<String>();
            final ArrayList<Integer> order = new ArrayList<Integer>();

            for (int i = 0; i < tracks.feed.entry.size(); i++) {
                final int index = i;
                final ItunesSong itunesResult = tracks.feed.entry.get(i);

                if (searchedArtist.indexOf(itunesResult.artist.label) != -1)
                    continue;

                searchedArtist.add(itunesResult.artist.label);
                String searchTerm = itunesResult.artist.label;
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                spotify.searchArtists(searchTerm, new Callback<ArtistsPager>() {
                    @Override
                    public void success(final ArtistsPager artistsPager, Response response) {
                        /// If there's a matching artist from Spotify search result, assume
                        /// the first one is the one we are looking for.
                        if (!artistsPager.artists.items.isEmpty()) {
                            Artist a = artistsPager.artists.items.get(0);
                            if (!itunesResult.image.isEmpty()) {
                                Image image = new Image();
                                image.url = itunesResult.image.get(0).label.replace("55x55", "500x500");
                                a.images.add(0, image);
                            }

                            int positionToAdd = mArtists.size();

                            if (!order.isEmpty()) {
                                for (int j = order.size() - 1; j >= 0; j--) {
                                    if (order.get(j) > index)
                                        positionToAdd = j;
                                }
                            }

                            mArtists.add(positionToAdd, new ArtistResult(a));
                            order.add(positionToAdd, index);

                            ((Activity) (mContext)).runOnUiThread(new Runnable() {
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        // Fail silently
                    }
                });
            }
        } else {
            ((Activity) (mContext)).runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mContext,
                            "Error downloading top artists", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    static class ViewHolder {
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
