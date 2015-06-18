package com.iktwo.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class TopTracksAdapter  extends BaseAdapter implements HttpAsyncRequest.AsyncResponse {
    public String TAG = "TopTracksAdapter";

    public List<ItunesSong> songs = new ArrayList<ItunesSong>();

    private LayoutInflater inflater;

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
        ViewHolder holder;
        ItunesSong song = (ItunesSong) getItem(position);

        if (convertView == null && inflater != null) {
            convertView = inflater.inflate(R.layout.top_tracks_delegate, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.textViewTitle);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.imageViewThumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(song.name.label);

        if (!song.image.isEmpty())
            holder.thumbnail.setTag(song.image.get(0).label);

        /*if (context != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            // Log.d(TAG, "WIDTH: " + Integer.toString(width) + " - HEIGHT: " + Integer.toString(height));

            int padding = Math.round((float) 8 * mDensity);
            width = width - padding * 2;

            if (height >= width) {
                height = Math.round((width / 16) * 9);
            } else {
                width = Math.round(width / 2);
                height = Math.round((width / 16) * 9);
            }

            Picasso.with(context).load(item.thumbnailUrl).resize(width, height).placeholder(R.drawable.logo).resize(width, height).error(R.drawable.logo).resize(width, height).centerCrop().into(holder.thumbnail);
        }*/

        return convertView;
    }

    public TopTracksAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        new HttpAsyncRequest(this).execute("https://itunes.apple.com/us/rss/topsongs/limit=10/json");
    }

    @Override
    public void processFinish(ArrayList<String> reply) {
        ItunesTopTracks tracks = new Gson().fromJson(reply.get(1), ItunesTopTracks.class);

        songs = tracks.feed.entry;
        notifyDataSetChanged();

        /*for (int i = 0; i < tracks.feed.entry.size(); i++) {
            Log.d(TAG, "I " + Integer.toString(i) + ": " + tracks.feed.entry.get(i).name.label);

            if (!tracks.feed.entry.get(i).image.isEmpty())
                Log.d(TAG, "Image: " + tracks.feed.entry.get(i).image.get(0).label);
        }

        Log.d(TAG, "Results: " + tracks.feed.entry.size());*/
    }

    private class ItunesTopTracks {
        public Feed feed;

        class Feed {
            public List<ItunesSong> entry = new ArrayList<ItunesSong>();
        }
    }

    static class ViewHolder {
        public TextView title;
        public ImageView thumbnail;
    }
}
