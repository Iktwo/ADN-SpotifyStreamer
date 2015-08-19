package com.iktwo.spotifystreamer;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MusicService extends Service implements MusicPlayback.Callback {

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";

    public static final String ACTION_CMD = "com.iktwo.spotifystreamer.ACTION_CMD";
    public static final String CMD = "CMD";
    public static final String CMD_PAUSE = "CMD_PAUSE";
    private static final int STOP_DELAY = 30000;

    private static final String TAG = MusicService.class.getName();

    private int trackIndex = 0;
    private ArrayList<Track> tracks;

    private ComponentName mEventReceiver;

    private MusicBinder mMusicBinder;
    private PendingIntent mMediaPendingIntent;
    private MediaSessionCompat mSession;
    private MusicPlayback mMusicPlayback;
    private MediaRouter mMediaRouter;
    private Bundle mSessionExtras;
    private MediaNotificationManager mMediaNotificationManager;


    private boolean mServiceStarted;

    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);

    public void onCreate() {
        super.onCreate();

        mMusicBinder = new MusicBinder();

        // Log.d(TAG, "STATE: when creating act" + Integer.toString(mMusicBinder.getService().getPlaybackState()));


        // mPlayingQueue = new ArrayList<>();
        // mMusicProvider = new MusicProvider();

        mEventReceiver = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mEventReceiver);
        mMediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

        mSession = new MediaSessionCompat(this, "MusicService", mEventReceiver, mMediaPendingIntent);

        final MediaSessionCallback cb = new MediaSessionCallback();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final MediaSession session = (MediaSession) mSession.getMediaSession();
            session.setCallback(new MediaSessionCallbackProxy(cb));
        } else {
            mSession.setCallback(cb);
        }

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        /// TODO: add provider mMusicProvider
        mMusicPlayback = new MusicPlayback(this);
        mMusicPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mMusicPlayback.setCallback(this);


        mSessionExtras = new Bundle();

        mSession.setExtras(mSessionExtras);

        updatePlaybackState(null);

        mMediaNotificationManager = new MediaNotificationManager(this);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
    }

    public MediaSessionCompat.Token getSessionToken() {
        return mSession.getSessionToken();
    }

    public MediaSessionCompat getSession() {
        return mSession;
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;

        /// TODO: check this
//        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
//            return actions;
//        }
        if (mMusicPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
//        if (mCurrentIndexOnQueue > 0) {
//            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
//        }
//        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
//            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
//        }
        return actions;
    }

    public int getPlaybackState() {
        return mMusicPlayback.getState();
    }

    private void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mMusicPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mMusicPlayback != null) {
            position = mMusicPlayback.getCurrentStreamPosition();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        // setCustomAction(stateBuilder);
        int state = mMusicPlayback.getState();

        if (error != null) {
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        /// TODO: check this
//        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
//            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
//            stateBuilder.setActiveQueueItemId(item.getQueueId());
//        }

        mSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotificationManager.startNotification();
        }
    }


    private void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=" + mMusicPlayback.getState());

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            Log.v(TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        MediaSessionCompat.QueueItem q;
        q = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder().build(), 1);
        mMusicPlayback.play(q);
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void onMetadataChanged(String mediaId) {
        /// TODO: check this
        Log.d(TAG, "onMetadataChanged" + mediaId);
    }

    public void playSong() {
        Track playSong = tracks.get(trackIndex);

        handlePlayRequest();
    }

    public void setList(ArrayList<Track> tracks) {
        this.tracks = tracks;
    }

    public void setSong(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMusicBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void pauseSong() {
        mMusicPlayback.pause();
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicService> mWeakReference;

        private DelayedStopHandler(MusicService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = mWeakReference.get();
            if (service != null && service.mMusicPlayback != null) {
                if (service.mMusicPlayback.isPlaying()) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                Log.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private final class MediaSessionCallbackProxy extends MediaSession.Callback {

        private final MediaSessionCallback mMediaSessionCallback;

        public MediaSessionCallbackProxy(MediaSessionCallback cb) {
            mMediaSessionCallback = cb;
        }

        @Override
        public void onPlay() {
            mMediaSessionCallback.onPlay();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            mMediaSessionCallback.onSkipToQueueItem(queueId);
        }

        @Override
        public void onSeekTo(long position) {
            mMediaSessionCallback.onSeekTo(position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mMediaSessionCallback.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPause() {
            mMediaSessionCallback.onPause();
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            mMediaSessionCallback.onPlayFromSearch(query, extras);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            mMediaSessionCallback.onCustomAction(action, extras);
        }

        @Override
        public void onSkipToPrevious() {
            mMediaSessionCallback.onSkipToPrevious();
        }

        @Override
        public void onSkipToNext() {
            mMediaSessionCallback.onSkipToNext();
        }

        @Override
        public void onStop() {
            mMediaSessionCallback.onStop();
        }
    }


    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "play");

            /// TODO: verify this
                handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "OnSkipToQueueItem:" + queueId);
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "onSeekTo:" + Long.toString(position));
        }

        @Override
        public void onPause() {
            Log.d(TAG, "pause current state=" + mMusicPlayback.getState());
            handlePauseRequest();
        }

        private void handlePauseRequest() {
            Log.d(TAG, "handlePauseRequest: mState=" + mMusicPlayback.getState());
            mMusicPlayback.pause();
            // reset the delayed stop handler.
            mDelayedStopHandler.removeCallbacksAndMessages(null);
            mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop");
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "skipToNext");
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "skipToPrevious");
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            Log.d(TAG, "playFromSearch  query=" + query + " extras=" + extras);
        }
    }

}
