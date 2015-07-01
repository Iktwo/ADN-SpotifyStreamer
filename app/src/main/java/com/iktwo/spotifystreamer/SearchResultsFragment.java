package com.iktwo.spotifystreamer;

import android.app.Activity;
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

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SearchResultsFragment extends Fragment {
    private static final String TAG = SearchResultsFragment.class.getSimpleName();

    private ProgressBar busyIndicator;
    private SearchResultsAdapter mAdapter;
    private TextView mTextViewNoResults;

    private boolean hasFinishedFetching = false;
    private boolean noResults = false;

    private GridView gridView;

    private ArtistInteractionListener mListener;

    public SearchResultsFragment() {
    }
    public static SearchResultsFragment newInstance() {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);

        gridView = (GridView) view.findViewById(R.id.grid_view);

        busyIndicator = (ProgressBar) view.findViewById(R.id.busy_indicator);

        mTextViewNoResults = (TextView) view.findViewById(R.id.text_view_no_results);

        if (hasFinishedFetching)
            busyIndicator.setVisibility(View.GONE);

        if (mAdapter != null)
            gridView.setAdapter(mAdapter);

        if (noResults)
            mTextViewNoResults.setVisibility(View.VISIBLE);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(mAdapter.getItem(i).id, mAdapter.getItem(i).name);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ArtistInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void search(final String searchTerm) {
        hasFinishedFetching = false;
        busyIndicator.setVisibility(View.VISIBLE);
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        spotify.searchArtists(searchTerm, new Callback<ArtistsPager>() {
            @Override
            public void success(final ArtistsPager artistsPager, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mAdapter = new SearchResultsAdapter(getActivity(), artistsPager.artists.items);
                        gridView.setAdapter(mAdapter);
                        busyIndicator.setVisibility(View.GONE);
                        hasFinishedFetching = true;

                        if (artistsPager.artists.items.isEmpty()) {
                            Log.d(TAG, "No results");
                            noResults = true;
                            mTextViewNoResults.setVisibility(View.VISIBLE);
                            Toast.makeText(getActivity(), "No results", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        busyIndicator.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), "Error", Toast.LENGTH_LONG).show();
                        hasFinishedFetching = true;
                    }
                });
            }
        });
    }
}
