package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ArtistSongsFragment extends Fragment {
    private static final String TAG = ArtistSongsFragment.class.getSimpleName();

    private OnArtistSongFragmentInteractionListener mListener;

    private ArtistSongsAdapter mArtistSongsAdapter;
    private ProgressBar busyIndicator;
    private TextView mTextViewNoResults;

    private boolean hasFinishedFetching = false;
    private boolean noResults = false;

    public ArtistSongsFragment() {
    }

    public static ArtistSongsFragment newInstance() {
        ArtistSongsFragment fragment = new ArtistSongsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mArtistSongsAdapter = new ArtistSongsAdapter(getActivity(), new ArrayList<Track>() {
        });

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

        return view;
    }

    public void onSongClicked(Integer index, List<Track> songs) {
        if (mListener != null) {
            mListener.onSongClicked(index, songs);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnArtistSongFragmentInteractionListener) activity;
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

    public void getSongsForArtist(String artistId) {
        Log.d("ArtistSong", "getSongsForArtist: " + artistId);

        hasFinishedFetching = false;
        busyIndicator.setVisibility(View.VISIBLE);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        spotify.getArtistTopTrack(artistId, "US", new Callback<Tracks>() {
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

    public interface OnArtistSongFragmentInteractionListener {
        void onSongClicked(Integer index, List<Track> songs);
    }
}
