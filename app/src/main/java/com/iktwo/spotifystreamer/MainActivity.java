package com.iktwo.spotifystreamer;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements ArtistInteractionListener {
    public static final String ARTIST_SONGS_ACTION = "com.iktwo.spotifystreamer.ARTIST_SONGS";

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mTwoPane;

    private TopTracksFragment mTopTracksFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.frame_layout_details) != null) {
            /// TODO: implement two pane mode in stage 2
             mTwoPane = true;

            FragmentManager fragmentManager = getSupportFragmentManager();
            mTopTracksFragment = new TopTracksFragment();
            fragmentManager.beginTransaction().replace(R.id.frame_layout_details, mTopTracksFragment).commit();

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            // ((ItemListFragment) getSupportFragmentManager()
            // .findFragmentById(R.id.item_list))
            // .setActivateOnItemClick(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mTwoPane) {
            /// TODO: if dual pane, put results here, if single pane start searchReusltsActivity
        } else {
            if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
                Intent resultIntent = new Intent(this, SearchResultsActivity.class);
                resultIntent.putExtras(intent.getExtras());
                resultIntent.setAction(intent.getAction());

                startActivity(resultIntent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getResources().getString(R.string.artist_search_hint));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistSelected(String artistId, String artistName) {
        if (mTwoPane) {
            /// TODO: replace main fragment with the songs fragment
        } else {
            Intent resultIntent = new Intent(this, ArtistSongsActivity.class);
            resultIntent.putExtra("artistId", artistId);
            resultIntent.putExtra("artistName", artistName);
            resultIntent.setAction(ARTIST_SONGS_ACTION);

            startActivity(resultIntent);
        }
    }
}
