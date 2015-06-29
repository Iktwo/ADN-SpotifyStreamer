package com.iktwo.spotifystreamer;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class ArtistSongsActivity extends AppCompatActivity implements ArtistSongsFragment.OnArtistSongFragmentInteractionListener {
    private ArtistSongsFragment mArtistSongsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_songs);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mArtistSongsFragment = (ArtistSongsFragment) getFragmentManager().findFragmentById(R.id.artist_songs_fragment);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().getAction().equals(MainActivity.ARTIST_SONGS_ACTION)) {
            mArtistSongsFragment.getSongsForArtist(getIntent().getStringExtra("artistId"));
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
    public void onArtistSongFragmentInteractionListener(Uri uri) {

    }
}
