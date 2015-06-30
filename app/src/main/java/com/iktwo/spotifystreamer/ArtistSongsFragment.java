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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
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

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private String mParam1;

    private OnArtistSongFragmentInteractionListener mListener;

    private ArtistSongsAdapter mArtistSongsAdapter;
    private ProgressBar busyIndicator;

    private boolean hasFinishedFetching = false;

    public ArtistSongsFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment ArtistSongsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ArtistSongsFragment newInstance(String param1, String param2) {
        ArtistSongsFragment fragment = new ArtistSongsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }

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
                onSongClicked(((Track) (mArtistSongsAdapter.getItem(i))));
            }
        });

        busyIndicator = (ProgressBar) view.findViewById(R.id.busy_indicator);

        if (hasFinishedFetching)
            busyIndicator.setVisibility(View.GONE);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onSongClicked(Track song) {
        if (mListener != null) {
            mListener.onSongClicked(song);
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
        Log.d("ArtistSong", "getSongsForArtist");

        hasFinishedFetching = false;
        busyIndicator.setVisibility(View.VISIBLE);

        /// TODO: implement this
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        Map<String, Object> options = new HashMap<>();
        options.put("country", "US");

        spotify.getArtistTopTrack(artistId, options, new Callback<Tracks>() {
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
        // TODO: Update argument type and name
        public void onSongClicked(Track song);
    }
}
