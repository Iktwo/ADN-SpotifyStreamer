package com.iktwo.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import static android.media.AudioManager.OnAudioFocusChangeListener;
import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.MediaPlayer.OnSeekCompleteListener;

public class MusicPlayback implements OnAudioFocusChangeListener, OnCompletionListener, OnPreparedListener, OnErrorListener, OnSeekCompleteListener {
    public static final float VOLUME_DUCK = 0.2f;
    public static final float VOLUME_NORMAL = 1.0f;

    private static final String TAG = MusicPlayback.class.getSimpleName();

    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;

    private final MusicService mService;
    private final WifiManager.WifiLock mWifiLock;

    private volatile String mCurrentMediaId;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile int mCurrentPosition;

    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;

    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private int mState;

    private Callback mCallback;

    private boolean mResumePlaybackOnFocusGain;

    private IntentFilter mAudioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.d(TAG, "Headphones disconnected.");

                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD, MusicService.CMD_PAUSE);

                    mService.startService(i);

                }
            }
        }
    };

    // TODO: add , MusicProvider musicProvider
    public MusicPlayback(MusicService service) {
        this.mService = service;
        this.mAudioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        this.mWifiLock = ((WifiManager) service.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "wifi_lock");
    }

    public boolean isPlaying() {
        return mResumePlaybackOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        this.mState = state;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion from MediaPlayer");

        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
        if (mCallback != null) {
            mCallback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared from MediaPlayer");

        configMediaPlayerState();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onSeekComplete from MediaPlayer:" + mediaPlayer.getCurrentPosition());
        mCurrentPosition = mediaPlayer.getCurrentPosition();
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }


    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    public void setCurrentStreamPosition(int pos) {
        this.mCurrentPosition = pos;
    }

    public void play(MediaSessionCompat.QueueItem item) {
        mResumePlaybackOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false);

            /*MediaMetadataCompat track = mMusicProvider.getMusic(
                    MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));

            String source = track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE); */

            String source = "http://www.stephaniequinn.com/Music/Allegro%20from%20Duet%20in%20C%20Major.mp3";

            try {
                Log.d(TAG, "going to create mp if null");
                createMediaPlayerIfNull();

                mState = PlaybackStateCompat.STATE_BUFFERING;

                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(source);

                mMediaPlayer.prepareAsync();

                mWifiLock.acquire();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage() + "Exception playing song");
                if (mCallback != null) {
                    mCallback.onError(ex.getMessage());
                }
            }
        }
    }

    private void tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void createMediaPlayerIfNull() {
        Log.d(TAG, "createMediaPlayerIfNull. null: " + Boolean.toString(mMediaPlayer == null));

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setWakeMode(mService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }


    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mService.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mService.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        mService.stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    public void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }

            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
            giveUpAudioFocus();
        }

        mState = PlaybackStateCompat.STATE_PAUSED;

        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }

        unregisterAudioNoisyReceiver();
    }

    private void configMediaPlayerState() {
        Log.d(TAG, "configMediaPlayerState. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mResumePlaybackOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "configMediaPlayerState startMediaPlayer. seeking to " + mCurrentPosition);
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                mResumePlaybackOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    @Override
    public void onAudioFocusChange(int newAudioFocusChange) {
        Log.d(TAG, "onAudioFocusChange. focusChange=" + newAudioFocusChange);
        if (newAudioFocusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mAudioFocus = AUDIO_FOCUSED;
        } else if (newAudioFocusChange == AudioManager.AUDIOFOCUS_LOSS || newAudioFocusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || newAudioFocusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            boolean canDuck = newAudioFocusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                mResumePlaybackOnFocusGain = true;
            }
        } else {
            Log.e(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + newAudioFocusChange);
        }

        configMediaPlayerState();
    }

    interface Callback {
        void onCompletion();

        void onPlaybackStatusChanged(int state);

        void onError(String error);

        void onMetadataChanged(String mediaId);
    }
}
