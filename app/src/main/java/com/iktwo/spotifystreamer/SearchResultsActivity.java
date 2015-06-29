package com.iktwo.spotifystreamer;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SearchResultsActivity extends AppCompatActivity implements SearchResultsFragment.OnSearchResultsFragmentInteractionListener {

    private static final String TAG = "SearchResultsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().getAction().equals(Intent.ACTION_SEARCH)) {
            String searchTerm = getIntent().getStringExtra(SearchManager.QUERY);
            SearchResultsFragment searchResulstFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.search_results_fragment);
            searchResulstFragment.search(searchTerm);

            /*

            FragmentManager fm = getFragmentManager();
            mSearchResultsFragment = (SearchResultsFragment) fm.findFragmentByTag(TAG_SEARCH_RESULTS_FRAGMENT);
            if (mSearchResultsFragment == null) {
                mSearchResultsFragment = new SearchResultsFragment();
                Bundle bundle = new Bundle();
                bundle.putString("query", query);
                mSearchResultsFragment.setArguments(bundle);
                fm.beginTransaction().replace(R.id.container, mSearchResultsFragment, TAG_SEARCH_RESULTS_FRAGMENT).commit();
            }

             */
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.  The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistSelected(String artistId) {
        Intent resultIntent = new Intent(this, ArtistSongsActivity.class);
        resultIntent.putExtra("artistId", artistId);
        resultIntent.setAction(MainActivity.ARTIST_SONGS_ACTION);

        startActivity(resultIntent);
    }
}
