package com.iktwo.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
        new HttpAsyncRequest(this).execute("https://itunes.apple.com/us/rss/topsongs/limit=10/json");
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

        if (!song.image.isEmpty()) {
            holder.thumbnail.setTag(song.image.get(0).label);
            Picasso.with(mContext).load(song.image.get(0).label.replace("55x55", "400x400")).resize(400, 400).into(holder.thumbnail);
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
        public ImageView thumbnail;
    }

    private class ItunesTopTracks {
        public Feed feed;

        class Feed {
            public List<ItunesSong> entry = new ArrayList<ItunesSong>();
        }
    }
}
