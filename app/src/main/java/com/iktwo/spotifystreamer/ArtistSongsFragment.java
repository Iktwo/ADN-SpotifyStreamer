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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ArtistSongsFragment extends Fragment {
    private static final String TAG = ArtistSongsFragment.class.getSimpleName();

    private ArtistSongFragmentInteractionListener mListener;

    private ArtistSongsAdapter mArtistSongsAdapter;
    private ProgressBar busyIndicator;
    private TextView mTextViewNoResults;

    private boolean hasFinishedFetching = false;
    private boolean noResults = false;
    private boolean hasToGetSongs = false;
    private String mArtistId;
    private String mArtistName;

    public ArtistSongsFragment() {
    }

    public static ArtistSongsFragment newInstance(String artistId, String artistName, boolean skipRetainInstance) {
        ArtistSongsFragment fragment = new ArtistSongsFragment();
        Bundle args = new Bundle();
        args.putString("artistId", artistId);
        args.putString("artistName", artistName);
        args.putBoolean("SkipRetainInstance", skipRetainInstance);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean skipRetainInstance = false;

        if (getArguments() != null) {
            if (getArguments().getString("artistId") != null)
                mArtistId = getArguments().getString("artistId");

            if (getArguments().getString("mArtistName") != null)
                mArtistName = getArguments().getString("mArtistName");

            hasToGetSongs = true;

            skipRetainInstance = getArguments().getBoolean("SkipRetainInstance");
        }

        mArtistSongsAdapter = new ArtistSongsAdapter(getActivity(), new ArrayList<Track>() {
        });


        if (skipRetainInstance)
            setRetainInstance(false);
        else
            setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_songs, container, false);

        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setAdapter(mArtistSongsAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onSongClicked(i, mArtistSongsAdapter.getSongs());
            }
        });

        busyIndicator = (ProgressBar) view.findViewById(R.id.busy_indicator);

        if (hasFinishedFetching)
            busyIndicator.setVisibility(View.GONE);

        mTextViewNoResults = (TextView) view.findViewById(R.id.text_view_no_results);

        if (noResults)
            mTextViewNoResults.setVisibility(View.VISIBLE);

        if (hasToGetSongs)
            getSongsForArtist(mArtistId);

        return view;
    }

    public void onSongClicked(Integer index, List<Track> songs) {
        if (mListener != null)
            mListener.onSongClicked(index, songs);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ArtistSongFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ArtistSongFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void getSongsForArtist(String artistId) {
        Log.d("ArtistSong", "getSongsForArtist: " + artistId);

        hasFinishedFetching = false;
        busyIndicator.setVisibility(View.VISIBLE);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        spotify.getArtistTopTrack(artistId, CountryCode.country, new Callback<Tracks>() {
            @Override
            public void success(final Tracks tracks, Response response) {
                for (int i = 0; i < tracks.tracks.size(); i++) {
                    final int index = i;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                mArtistSongsAdapter.add(tracks.tracks.get(index));
                            }
                        });
                    }
                }

                if (tracks.tracks.isEmpty() && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            Log.d(TAG, "No results");
                            noResults = true;
                            mTextViewNoResults.setVisibility(View.VISIBLE);
                            Toast.makeText(getActivity(), "No results", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            hasFinishedFetching = true;
                            busyIndicator.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "Could not get tracks for artist",
                                    Toast.LENGTH_LONG).show();

                            hasFinishedFetching = true;
                            busyIndicator.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    public interface ArtistSongFragmentInteractionListener {
        void onSongClicked(Integer index, List<Track> songs);
    }
}
