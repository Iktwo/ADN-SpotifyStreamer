package com.iktwo.spotifystreamer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class TopTracksFragment extends Fragment implements HttpAsyncRequest.AsyncResponse {
    private static final String TAG = TopTracksFragment.class.getSimpleName();

    private int itemsToRequest = 10;
    private int fetchedItems = 0;

    private TopTracksAdapter mTopTracksAdapter;
    private GridView gridView;

    private ArrayList<ArtistResult> mArtists = new ArrayList<>();

    private ArtistInteractionListener mListener;

    private ProgressBar busyIndicator;

    public TopTracksFragment() {
    }

    public static TopTracksFragment newInstance(boolean doNotRetainInstance) {
        TopTracksFragment fragment = new TopTracksFragment();
        Bundle args = new Bundle();
        args.putBoolean("SkipRetainInstance", doNotRetainInstance);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /// TODO: Store this on a DB and query if empty or last insertion is old
        new HttpAsyncRequest(this).execute("https://itunes.apple.com/us/rss/topsongs/limit=" + Integer.toString(itemsToRequest) + "/json");

        boolean skipRetainInstance = false;

        if (getArguments() != null) {
            Log.d(TAG, "getArguments");
            Log.d(TAG, "SkipRetainInstance: " + Boolean.toString(getArguments().getBoolean("SkipRetainInstance")));
            skipRetainInstance = getArguments().getBoolean("SkipRetainInstance");
        } else {
            Log.d(TAG, "getArguments is null");
        }

        if (skipRetainInstance)
            setRetainInstance(false);
        else
            setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        gridView = (GridView) view.findViewById(R.id.gridView);
        busyIndicator = (ProgressBar) view.findViewById(R.id.busy_indicator);

        if (mTopTracksAdapter != null) {
            gridView.setAdapter(mTopTracksAdapter);
            busyIndicator.setVisibility(View.GONE);
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(((ArtistResult) gridView.getAdapter().getItem(i)).artist.id, ((ArtistResult) gridView.getAdapter().getItem(i)).artist.name);
            }
        });

        return view;
    }

    public void onItemClicked(String artistId, String artistName) {
        if (mListener != null) {
            mListener.onArtistSelected(artistId, artistName);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ArtistInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
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

            ArrayList<String> searchedArtist = new ArrayList<>();
            final ArrayList<Integer> order = new ArrayList<>();

            Log.d(TAG, "real amount of items that were fetched from itunes" + Integer.toString(tracks.feed.entry.size()));

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
                        fetchedItems += 1;
                        /// If there's a matching artist from Spotify search result, assume
                        /// the first one is the one we are looking for.
                        if (!artistsPager.artists.items.isEmpty()) {
                            Artist a = artistsPager.artists.items.get(0);

                            int positionToAdd = mArtists.size();

                            if (!order.isEmpty()) {
                                for (int j = order.size() - 1; j >= 0; j--) {
                                    if (order.get(j) > index)
                                        positionToAdd = j;
                                }
                            }

                            mArtists.add(positionToAdd, new ArtistResult(a));
                            order.add(positionToAdd, index);
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    busyIndicator.setVisibility(View.GONE);
                                }
                            });
                        }

                        if (fetchedItems == itemsToRequest) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        mTopTracksAdapter = new TopTracksAdapter(getActivity(), mArtists);
                                        gridView.setAdapter(mTopTracksAdapter);
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        fetchedItems += 1;

                        if (fetchedItems == itemsToRequest) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        mTopTracksAdapter = new TopTracksAdapter(getActivity(), mArtists);
                                        gridView.setAdapter(mTopTracksAdapter);
                                    }
                                });
                            }
                        }

                        /// TODO: Do something if all failed
                        // Fail silently
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    busyIndicator.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                });
            }
        } else {
            Log.e(TAG, "could not download itunes rss");

            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(),
                                "Error downloading top artists",
                                Toast.LENGTH_LONG).show();

                        busyIndicator.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private class ItunesTopTracks {
        public Feed feed;

        class Feed {
            public List<ItunesSong> entry = new ArrayList<>();
        }
    }
}
