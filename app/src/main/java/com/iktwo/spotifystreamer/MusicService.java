package com.iktwo.spotifystreamer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener {

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";

    private static final String TAG = MusicService.class.getName();

    private MediaPlayer mMediaPlayer;
    private int trackIndex = 0;
    private ArrayList<Track> tracks;

    private final IBinder musicBind = new MusicBinder();

    private BroadcastReceiver broadcastMediaControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                switch (intent.getAction()) {
                    case "pause": {
                        Log.d(TAG, "Got pause, is it playing? " + mMediaPlayer.isPlaying());
                        /// TODO: change notification here
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                        }
                    }
                    break;
                    case "play": {
                        if (!mMediaPlayer.isPlaying())
                            mMediaPlayer.start();
                    }
                    break;
                }
            }
            Log.d(TAG, "received " + intent);
        }
    };

    public void onCreate() {
        super.onCreate();
        trackIndex = 0;
        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        mMediaPlayer.reset();

        Track playSong = tracks.get(trackIndex);

        try{
            mMediaPlayer.setDataSource(playSong.preview_url);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        mMediaPlayer.prepareAsync();
    }

    public void setList(ArrayList<Track> tracks) {
        this.tracks = tracks;
    }

    public void setSong(int trackIndex){
        this.trackIndex = trackIndex;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        mMediaPlayer.stop();
        mMediaPlayer.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }
}
