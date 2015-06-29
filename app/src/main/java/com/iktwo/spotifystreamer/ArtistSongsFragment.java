package com.iktwo.spotifystreamer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ArtistSongsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private String mParam1;

    private OnArtistSongFragmentInteractionListener mListener;

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

    public ArtistSongsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_artist_songs, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onArtistSongFragmentInteractionListener(uri);
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
        /// TODO: implement this
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        spotify.getArtistTopTrack(artistId, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                for (int i = 0; i < tracks.tracks.size(); i++) {
                    Log.d("AristSong", Integer.toString(i) + ": " + tracks.tracks.get(i).toString());
                }
                /// TODO: add to adapter here
            }

            @Override
            public void failure(RetrofitError error) {
                /// TODO: add toast here ???
            }
        });
    }

    public interface OnArtistSongFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onArtistSongFragmentInteractionListener(Uri uri);
    }

}
