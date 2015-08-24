package com.iktwo.spotifystreamer;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class MainActivity extends AppCompatActivity implements ArtistInteractionListener, ArtistSongsFragment.ArtistSongFragmentInteractionListener {
    public static final String ARTIST_SONGS_ACTION = "com.iktwo.spotifystreamer.ARTIST_SONGS";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SEARCH_RESULTS_FRAGMENT_TAG = "SearchResultsFragment";
    private static final String ARTIST_SONGS_FRAGMENT_TAG = "ArtistSongsFragment";
    private static final String TOP_TRACKS_FRAGMENT_TAG = "TopTracksFragment";

    private boolean mTwoPane;

    private TopTracksFragment mTopTracksFragment;

    private String mArtistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.frame_layout_details_container) != null) {
            mTwoPane = true;

            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_details_container, TopTracksFragment.newInstance(true)).commit();
        } else {
            mTwoPane = false;
        }

        SwitchDrawerItem itemS = new SwitchDrawerItem()
                .withName(R.string.toggle_notification)
                .withChecked(true)
                .withIdentifier(1)
                .withOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(IDrawerItem iDrawerItem, CompoundButton compoundButton, boolean b) {
                MediaNotificationManager.notificationEnabled = b;
            }
        });

        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        itemS
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem.getIdentifier() == 1) {
                            Log.d(TAG, "Switched" + ((SwitchDrawerItem) drawerItem));
                        }

                        return true;
                    }
                })
                .build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mTwoPane) {
            if (getSupportFragmentManager().findFragmentById(R.id.fragment_search) != null) {
                SearchResultsFragment searchResultsFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_search);
                searchResultsFragment.search(intent.getStringExtra(SearchManager.QUERY));
            }
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

        if (id == R.id.action_country_selection) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(getString(R.string.action_country));
            final String[] types = getResources().getStringArray(R.array.available_countries);
            b.setItems(types, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    CountryCode.country = types[which];
                }
            });

            b.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSongClicked(Integer index, List<Track> songs) {
        Log.d(TAG, "onSongClicked" + Integer.toString(index));
        Intent resultIntent = new Intent(this, PlaybackActivity.class);
        resultIntent.putExtra("displayAsDialog", true);


        resultIntent.putExtra("artistName", mArtistName);
        resultIntent.putExtra("index", index);
        resultIntent.putParcelableArrayListExtra("songs", new ArrayList<Track>(songs));

        startActivity(resultIntent);
    }

    @Override
    public void onArtistSelected(String artistId, String artistName) {
        if (mTwoPane) {
            mArtistName = artistName;
            ArtistSongsFragment artistSongsFragment = ArtistSongsFragment.newInstance(artistId, artistName, true);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.frame_layout_details_container, artistSongsFragment);

            transaction.commit();
        } else {
            Intent resultIntent = new Intent(this, ArtistSongsActivity.class);
            resultIntent.putExtra("artistId", artistId);
            resultIntent.putExtra("artistName", artistName);
            resultIntent.setAction(ARTIST_SONGS_ACTION);

            startActivity(resultIntent);
        }
    }
}
