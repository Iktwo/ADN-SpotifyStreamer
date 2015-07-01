package com.iktwo.spotifystreamer;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class SearchResultsActivity extends AppCompatActivity implements ArtistInteractionListener {
    private static final String TAG = SearchResultsActivity.class.getSimpleName();
    private boolean mSearched = false;
    private String mSearchTerm = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSearched = (null != savedInstanceState) && savedInstanceState.getBoolean("has-searched");

        if (savedInstanceState != null && savedInstanceState.getString("searchterm") != null)
            mSearchTerm = savedInstanceState.getString("searchterm");


        if (getIntent().getAction().equals(Intent.ACTION_SEARCH) && !mSearched) {
            mSearchTerm = getIntent().getStringExtra(SearchManager.QUERY);
            SearchResultsFragment searchResulstFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.search_results_fragment);
            searchResulstFragment.search(mSearchTerm);
            mSearched = true;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mSearchTerm);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistSelected(String artistId, String artistName) {
        Intent resultIntent = new Intent(this, ArtistSongsActivity.class);
        resultIntent.putExtra("artistId", artistId);
        resultIntent.putExtra("artistName", artistName);
        resultIntent.setAction(MainActivity.ARTIST_SONGS_ACTION);

        startActivity(resultIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("has-searched", mSearched);
        state.putString("searchterm", mSearchTerm);
    }
}
