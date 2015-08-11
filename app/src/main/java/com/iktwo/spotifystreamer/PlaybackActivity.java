package com.iktwo.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class PlaybackActivity extends AppCompatActivity implements PlaybackFragment.OnFragmentInteractionListener {
    private static final String TAG = PlaybackActivity.class.getName();

    private MusicService musicService;
    private Intent playbackIntent;
    private boolean musicServiceBound = false;
    private ArrayList<Track> tracks;
    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            musicService = binder.getService();
            musicService.setList(tracks);
            musicServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicServiceBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        if (playbackIntent == null) {
            playbackIntent = new Intent(this, MusicService.class);
            bindService(playbackIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playbackIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_playback) + " - " + getIntent().getStringExtra("artistName"));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();

        if (intent != null) {
            Integer playIndex = intent.getIntExtra("index", -1);
            tracks = intent.getParcelableArrayListExtra("songs");

            PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playback_fragment);
            if (tracks.size() > playIndex) {
                Track t = tracks.get(playIndex);
                if (!t.album.images.isEmpty())
                    playbackFragment.setTrackArt(t.album.images.get(0).url);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (musicConnection != null) {
            unbindService(musicConnection);
        }
    }

    @Override
    public void onFragmentInteraction() {
        musicService.playSong();
    }
}
