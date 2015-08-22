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

public class PlaybackActivity extends AppCompatActivity implements PlaybackFragment.OnPlaybackFragmentInteractionListener {
    private static final String TAG = PlaybackActivity.class.getName();

    private MusicService musicService;
    private Intent playbackIntent;
    private int playbackIndex;
    private boolean musicServiceBound = false;
    private ArrayList<Track> tracks;
    private String mArtistName;

    private MediaControllerCompat mMediaController;

    private MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) {
                return;
            }

            setPlaybackState(state.getState());
            setOldPlaybackState(state);

            Log.d(TAG, "Received playback state change to state " + state.toString());
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            Log.d(TAG, "Received metadata state change to mediaId= " +
                    metadata.getDescription().getMediaId() +
                    " song=" + metadata.getDescription().getTitle());

            setMetadata(metadata);
        }
    };

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            musicService = binder.getService();
            musicService.setList(tracks);
            musicService.setArtistName(mArtistName);
            musicServiceBound = true;

            if (musicService.getCurrentTrackIndex() != playbackIndex)
                musicService.setSong(playbackIndex);

            setOldPlaybackState(musicService.getPlaybackStateTest());
            setPlaybackState(musicService.getPlaybackState());
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
            mMediaController.registerCallback(mCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playbackIntent == null) {
            playbackIntent = new Intent(this, MusicService.class);
            playbackIntent.putExtra("index", playbackIndex);
            bindService(playbackIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playbackIntent);
        }
    }

    public void setPlaybackState(int state) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playback_fragment);

        if (playbackFragment != null)
            playbackFragment.setPlaybackState(state);
    }

    public void setOldPlaybackState(PlaybackStateCompat state) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playback_fragment);

        if (playbackFragment != null)
            playbackFragment.setOldPlaybackState(state);

    }

    private void setMetadata(MediaMetadataCompat metadata) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playback_fragment);

        if (playbackFragment != null)
            playbackFragment.setMetadata(metadata);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mArtistName = getIntent().getStringExtra("artistName");

        if (getSupportActionBar() != null && mArtistName != null) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_playback) + " - " + mArtistName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();

        if (intent != null) {
            playbackIndex = intent.getIntExtra("index", 0);
            tracks = intent.getParcelableArrayListExtra("songs");

            PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playback_fragment);

            if (tracks != null && tracks.size() > playbackIndex) {
                Track t = tracks.get(playbackIndex);
                if (!t.album.images.isEmpty())
                    playbackFragment.setTrackArt(t.album.images.get(0).url);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (musicConnection != null)
            unbindService(musicConnection);
    }

    @Override
    public void onFragmentInteraction() {
        if (musicService != null) {
            if (musicService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING)
                musicService.pauseSong();
            else
                musicService.playSong();
        }
    }

    @Override
    public void onNextClicked() {
        musicService.nextTrack();
    }

    @Override
    public void onPreviousClicked() {
        musicService.previousTrack();
    }

    @Override
    public void onSeekTo(int position) {
        musicService.seekTo(position);
    }
}
