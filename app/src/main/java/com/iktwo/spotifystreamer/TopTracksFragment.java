package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

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

public class TopTracksFragment extends Fragment implements HttpAsyncRequest.AsyncResponse {
    private static final String TAG = "TopTracksFragment";

    private int itemsToRequest = 30;

    private TopTracksAdapter mTopTracksAdapter;

    private ArtistInteractionListener mListener;

    private ProgressBar busyIndicator;

    public TopTracksFragment() {
    }

    public static TopTracksFragment newInstance() {
        TopTracksFragment fragment = new TopTracksFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTopTracksAdapter = new TopTracksAdapter(getActivity());

        /// TODO: Store this on a DB and query if empty or last insertion is old
        new HttpAsyncRequest(this).execute("https://itunes.apple.com/us/rss/topsongs/limit=" + Integer.toString(itemsToRequest) + "/json");

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        GridView gridView = (GridView) view.findViewById(R.id.gridView);
        gridView.setAdapter(mTopTracksAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(((ArtistResult) (mTopTracksAdapter.getItem(i))).artist.id);
            }
        });

        busyIndicator = (ProgressBar) view.findViewById(R.id.busy_indicator);

        return view;
    }

    public void onItemClicked(String artistId) {
        if (mListener != null) {
            mListener.onArtistSelected(artistId);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ArtistInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

                            int positionToAdd = mTopTracksAdapter.getCount();

                            if (!order.isEmpty()) {
                                for (int j = order.size() - 1; j >= 0; j--) {
                                    if (order.get(j) > index)
                                        positionToAdd = j;
                                }
                            }

                            mTopTracksAdapter.add(positionToAdd, new ArtistResult(a));
                            order.add(positionToAdd, index);
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                busyIndicator.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        // Fail silently
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                busyIndicator.setVisibility(View.GONE);
                            }
                        });
                    }
                });
            }
        } else {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getActivity(),
                            "Error downloading top artists", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class ItunesTopTracks {
        public Feed feed;

        class Feed {
            public List<ItunesSong> entry = new ArrayList<>();
        }
    }
}
