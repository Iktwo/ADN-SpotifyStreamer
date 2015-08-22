package com.iktwo.spotifystreamer;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class MusicService extends Service implements MusicPlayback.Callback {
    public static final String ACTION_CMD = "com.iktwo.spotifystreamer.ACTION_CMD";
    public static final String CMD = "CMD";
    public static final String CMD_PAUSE = "CMD_PAUSE";
    private static final int STOP_DELAY = 30000;

    private static final String TAG = MusicService.class.getName();

    private int mTrackIndex = 0;
    private ArrayList<Track> tracks;
    private ArrayList<MediaMetadataCompat> mMetadataForTracks;

    private ComponentName mEventReceiver;

    private MusicBinder mMusicBinder;
    private PendingIntent mMediaPendingIntent;
    private MediaSessionCompat mSession;
    private MusicPlayback mMusicPlayback;
    private MediaRouter mMediaRouter;
    private Bundle mSessionExtras;
    private MediaNotificationManager mMediaNotificationManager;
    private List<MediaSessionCompat.QueueItem> mPlaybackQueue;

    private boolean mServiceStarted;

    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private String mArtistName;

    public MediaMetadataCompat getTrack(int index) {
        return mMetadataForTracks.get(index);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int index = intent.getIntExtra("index", -1);

            if (index != -1)
                mTrackIndex = index;

            try {
                String command = intent.getStringExtra(CMD);

                if (command.equals(CMD_PAUSE))
                    pauseSong();
                else
                    Log.d(TAG, "Received command that was not handled: " + command);

            } catch (Exception e) {
                // Ignore exception
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void onCreate() {
        super.onCreate();

        mPlaybackQueue = new ArrayList<>();
        mMusicBinder = new MusicBinder();
        mMetadataForTracks = new ArrayList<>();

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

        if (mPlaybackQueue == null || mPlaybackQueue.isEmpty())
            return actions;

        if (mMusicPlayback.isPlaying())
            actions |= PlaybackStateCompat.ACTION_PAUSE;

        if (mTrackIndex > 0)
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        if (mTrackIndex < mPlaybackQueue.size() - 1)
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;

        return actions;
    }

    public int getPlaybackState() {
        return mMusicPlayback.getState();
    }

    public PlaybackStateCompat getPlaybackStateTest() {
        return mSession.getController().getPlaybackState();
    }

    private void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mMusicPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mMusicPlayback != null) {
            position = mMusicPlayback.getCurrentStreamPosition();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        int state = mMusicPlayback.getState();

        if (error != null) {
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        mSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotificationManager.setTrackIndex(mTrackIndex);
            mMediaNotificationManager.setTrackList(tracks);
            mMediaNotificationManager.setArtistName(mArtistName);
            mMediaNotificationManager.startNotification();
        }
    }


    private void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=" + mMusicPlayback.getState() + " for index: " + Integer.toString(mTrackIndex));

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            Log.v(TAG, "Starting service");
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        mSession.setMetadata(mMetadataForTracks.get(mTrackIndex));
        updateMetadata();
        mMusicPlayback.play(mPlaybackQueue.get(mTrackIndex));

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    private void handleStopRequest(String withError) {
        Log.d(TAG, "handleStopRequest: mState=" + mMusicPlayback.getState() + " error=" + withError);
        mMusicPlayback.stop(true);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(withError);

        stopSelf();
        mServiceStarted = false;
    }

    private void updateMetadata() {
        MediaSessionCompat.QueueItem queueItem = mPlaybackQueue.get(mTrackIndex);
    }

    @Override
    public void onCompletion() {
        if (mPlaybackQueue != null && !mPlaybackQueue.isEmpty()) {
            mSession.getController().getTransportControls().seekTo(0);

            mTrackIndex++;
            if (mTrackIndex >= mPlaybackQueue.size()) {
                mTrackIndex = 0;
            }
            handlePlayRequest();
        } else {
            handleStopRequest(null);
        }
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
        Log.d(TAG, "onMetadataChanged" + mediaId);
    }

    public void playSong() {
        handlePlayRequest();
    }

    public void nextTrack() {
        mSession.getController().getTransportControls().seekTo(0);

        mTrackIndex++;

        if (mPlaybackQueue != null && mTrackIndex >= mPlaybackQueue.size()) {
            mTrackIndex = 0;
        }

        handlePlayRequest();
    }

    public void previousTrack() {
        mSession.getController().getTransportControls().seekTo(0);

        mTrackIndex--;
        if (mPlaybackQueue != null && mTrackIndex < 0) {
            mTrackIndex = 0;
        }

        handlePlayRequest();
    }

    public void setList(ArrayList<Track> tracks) {
        Log.d(TAG, "Setting list with " + Integer.toString(tracks.size()) + " items");

        this.tracks = tracks;
        mPlaybackQueue.clear();
        mMetadataForTracks.clear();

        for (int i = 0; i < tracks.size(); ++i) {
            Track t = tracks.get(i);

            MediaMetadataCompat.Builder b = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, t.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, t.album.name);

            if (!t.artists.isEmpty()) {
                b.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, t.artists.get(0).name);
            }

            if (!t.album.images.isEmpty())
                b.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, t.album.images.get(0).url);

            MediaMetadataCompat metadata = b.build();

            mMetadataForTracks.add(metadata);

            MediaDescriptionCompat descriptionCompat = new MediaDescriptionCompat
                    .Builder()
                    .setTitle(tracks.get(i).name)
                    .setMediaUri(Uri.parse(tracks.get(i).preview_url))
                    .build();

            MediaSessionCompat.QueueItem q = new MediaSessionCompat.QueueItem(descriptionCompat, i);
            mPlaybackQueue.add(q);
        }

        mSession.setQueue(mPlaybackQueue);
    }

    public void setSong(int trackIndex) {
        mTrackIndex = trackIndex;

        if (mMusicPlayback.getState() == PlaybackStateCompat.STATE_PLAYING) {
            handlePlayRequest();
        } else if (mMusicPlayback.getState() == PlaybackStateCompat.STATE_PAUSED) {
            handleStopRequest(null);
            pauseSong();
        }
    }

    public int getCurrentTrackIndex() {
        return mTrackIndex;
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

    public void setArtistName(String artistName) {
        mArtistName = artistName;
    }

    public void seekTo(int position) {
        //Log.d(TAG, "seekTo " + Integer.toString(position) + " - " + mSession.getController().getTransportControls());
        mSession.getController().getTransportControls().seekTo(position);
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

            if (mPlaybackQueue != null && !mPlaybackQueue.isEmpty())
                handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "OnSkipToQueueItem:" + queueId);
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "onSeekTo:" + Long.toString(position));
            mMusicPlayback.seekTo((int) position);
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
            mTrackIndex++;

            if (mPlaybackQueue != null && mTrackIndex >= mPlaybackQueue.size()) {
                mTrackIndex = 0;
            }

            handlePlayRequest();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "skipToPrevious");
            mTrackIndex--;
            if (mPlaybackQueue != null && mTrackIndex < 0) {
                mTrackIndex = 0;
            }

            handlePlayRequest();
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            Log.d(TAG, "playFromSearch  query=" + query + " extras=" + extras);
        }
    }
}
