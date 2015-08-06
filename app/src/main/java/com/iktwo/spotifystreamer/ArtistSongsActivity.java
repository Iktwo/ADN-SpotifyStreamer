package com.iktwo.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class ArtistSongsActivity extends AppCompatActivity implements ArtistSongsFragment.OnArtistSongFragmentInteractionListener {
    private static final String TAG = ArtistSongsActivity.class.getName();

    private boolean mSearched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_songs);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(getIntent().getStringExtra("artistName"));

        ArtistSongsFragment mArtistSongsFragment = (ArtistSongsFragment) getSupportFragmentManager().findFragmentById(R.id.artist_songs_fragment);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mSearched = (null != savedInstanceState) && savedInstanceState.getBoolean("has-searched");

        if (getIntent().getAction().equals(MainActivity.ARTIST_SONGS_ACTION) && !mSearched) {
            mArtistSongsFragment.getSongsForArtist(getIntent().getStringExtra("artistId"));
            mSearched = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_songs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSongClicked(Integer index, List<Track> songs) {
        Intent resultIntent = new Intent(this, PlaybackActivity.class);
        resultIntent.putExtra("index", index);
        resultIntent.putParcelableArrayListExtra("songs", new ArrayList<Track>(songs));
        startActivity(resultIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("has-searched", mSearched);
    }
}
