package com.iktwo.spotifystreamer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
        Log.d(TAG, "calling get view");
        final ViewHolder holder;
        final Artist artist = (Artist) getItem(position);

        if (convertView == null && inflater != null) {
            convertView = inflater.inflate(R.layout.search_result_delegate, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.text_view_title);
            holder.thumbnail = (SquareImageViewByWidth) convertView.findViewById(R.id.thumbnail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(artist.name);

        if (!artist.images.isEmpty()) {
            Picasso.with(mContext)
                    .load(artist.images.get(0).url)
                    .into(holder.thumbnail);
        } else {
            Picasso.with(mContext)
                    .load(R.drawable.placeholder_artist)
                    .into(holder.thumbnail);

            Log.d(TAG, "NO IMAGE :( for " + artist.name);
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
    }
}
