package com.iktwo.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class PlaybackActivity extends AppCompatActivity implements PlaybackFragment.OnFragmentInteractionListener {
    private static final String TAG = PlaybackActivity.class.getName();

    private MusicService musicService;
    private Intent playbackIntent;
    private boolean musicServiceBound = false;
    private ArrayList<Track> tracks;

    private MediaControllerCompat mMediaController;
    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) {
                return;
            }

            setPlaybackState(state.getState());

            Log.d(TAG, "Received playback state change to state " + state.getState());
            // PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            Log.d(TAG, "Received metadata state change to mediaId=" +
                    metadata.getDescription().getMediaId() +
                    " song=" + metadata.getDescription().getTitle());
            // PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };
    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            musicService = binder.getService();
            musicService.setList(tracks);
            musicServiceBound = true;

            setPlaybackState(musicService.getPlaybackState());

            Log.d(TAG, "????? connected to service");

            connectToSession(musicService.getSessionToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicServiceBound = false;

            if (mMediaController != null)
                mMediaController.unregisterCallback(mCallback);
        }
    };

    private void connectToSession(MediaSessionCompat.Token token) {
        try {
            mMediaController = new MediaControllerCompat(getApplicationContext(), token);

            if (mMediaController != null) {
                mMediaController.registerCallback(mCallback);
            }
        } catch (RemoteException e) {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playbackIntent == null) {
            playbackIntent = new Intent(this, MusicService.class);
            bindService(playbackIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playbackIntent);
        }

        if (musicService != null)
            Log.d(TAG, "NOT NULL HERE :D");


    }

    public void setPlaybackState(int state) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playback_fragment);

        if (playbackFragment != null)
            playbackFragment.setPlaybackState(state);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (musicService != null)
            Log.d(TAG, "SERVICE IS NOT NULL");
        else
            Log.d(TAG, "service is null");

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
