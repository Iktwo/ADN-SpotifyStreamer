package com.iktwo.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MediaNotificationManager extends BroadcastReceiver {
    public static final String ACTION_PAUSE = "com.iktwo.spotifystreamer.pause";
    public static final String ACTION_PLAY = "com.iktwo.spotifystreamer.play";
    public static final String ACTION_PREV = "com.iktwo.spotifystreamer.prev";
    public static final String ACTION_NEXT = "com.iktwo.spotifystreamer.next";
    private static final String TAG = MediaNotificationManager.class.getSimpleName();
    private static final int NOTIFICATION_ID = 8;
    private static final int REQUEST_CODE = 100;

    private final MusicService mService;

    private boolean mStarted = false;

    private PlaybackStateCompat mPlaybackState;
    private NotificationManager mNotificationManager;
    private MediaSessionCompat.Token mSessionToken;
    private MediaMetadataCompat mMetadata;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;

    private PendingIntent mPauseIntent;
    private PendingIntent mPlayIntent;
    private PendingIntent mPreviousIntent;
    private PendingIntent mNextIntent;

    private int mNotificationColor;
    private String mIconUrl;
    private Bitmap mAlbumArtBitmap;

    private NotificationCompat.Builder mNotificationBuilder;
    private ArrayList<Track> mTracks;
    private int mTrackIndex;
    private String mArtistName;
    private Target albumArtTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Log.d(TAG, "onBitmapLoaded");

            mAlbumArtBitmap = bitmap;
            mNotificationManager.notify(NOTIFICATION_ID, createNotification());
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };
    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state" + state);

            if (state != null && (state.getState() == PlaybackStateCompat.STATE_STOPPED || state.getState() == PlaybackStateCompat.STATE_NONE)) {
                stopNotification();
            } else {
                Notification notification = createNotification();

                if (notification != null)
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Log.d(TAG, "Received new metadata " + metadata);
            mMetadata = metadata;
            Notification notification = createNotification();

            if (notification != null)
                mNotificationManager.notify(NOTIFICATION_ID, notification);
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

    public MediaNotificationManager(MusicService service) {
        mService = service;
        updateSessionToken();

        mNotificationColor = R.color.colorPrimaryDark;

        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        mNotificationManager.cancelAll();
    }

    private void updateSessionToken() {
        try {
            MediaSessionCompat.Token freshToken = mService.getSessionToken();
            if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
                if (mController != null) {
                    mController.unregisterCallback(mCb);
                }
                mSessionToken = freshToken;
                mController = new MediaControllerCompat(mService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (mStarted) {
                    mController.registerCallback(mCb);
                }
            }
        } catch (RemoteException ex) {

        }
    }

    public void stopNotification() {
        Log.d(TAG, "stopping notification");
        if (mStarted) {
            mStarted = false;
            mController.unregisterCallback(mCb);

            try {
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }

            mService.stopForeground(true);
        }

        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action;

        try {
            action = intent.getAction();
        } catch (Exception e) {
            action = "";
        }

        Log.d(TAG, "Received intent with action: " + action);

        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
            default:
                Log.w(TAG, "Unknown intent ignored. Action: " + action);
        }
    }

    public void startNotification() {
        if (!mStarted) {
            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            Notification notification = createNotification();
            if (notification != null) {
                mController.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                mService.registerReceiver(this, filter);

                mService.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }
    }

    private Notification createNotification() {
        Log.d(TAG, "updateNotificationMetadata. mMetadata=" + mMetadata);

        if (mMetadata == null) {
            MediaMetadataCompat.Builder b = new MediaMetadataCompat.Builder();
            mMetadata = b.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Unknown Song Playing").build();
        }

        if (mPlaybackState == null) {
            return null;
        }

        mNotificationBuilder = new NotificationCompat.Builder(mService);
        int playPauseButtonPosition = 0;

        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            mNotificationBuilder.addAction(R.drawable.ic_skip_previous_white,
                    mService.getString(R.string.label_previous), mPreviousIntent);

            playPauseButtonPosition = 1;
        }

        addPlayPauseAction(mNotificationBuilder);

        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            mNotificationBuilder.addAction(R.drawable.ic_skip_next_white,
                    mService.getString(R.string.label_next), mNextIntent);
        }

        MediaDescriptionCompat description = mMetadata.getDescription();

        if (mAlbumArtBitmap == null)
            mAlbumArtBitmap = BitmapFactory.decodeResource(mService.getResources(), R.drawable.placeholder_artist);

        if (description.getIconUri() != null) {
            if (mIconUrl == null || !mIconUrl.equals(description.getIconUri().toString())) {
                mIconUrl = description.getIconUri().toString();
                Picasso.with(mService).load(description.getIconUri()).into(albumArtTarget);
            }
        }

        mNotificationBuilder
                .setStyle(new NotificationCompat.MediaStyle().setMediaSession(mSessionToken).setShowActionsInCompactView(new int[]{playPauseButtonPosition}))
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(mAlbumArtBitmap);

        setNotificationPlaybackState(mNotificationBuilder);

        Log.d(TAG, "building notification");
        return mNotificationBuilder.build();
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        /// TODO: check this, might not work in dual pane mode
        Intent openUI = new Intent(mService, PlaybackActivity.class);

        if (mTracks != null)
            openUI.putParcelableArrayListExtra("songs", new ArrayList<Track>(mTracks));

        openUI.putExtra("index", mTrackIndex);

        if (mArtistName != null)
            openUI.putExtra("artistName", mArtistName);

        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        Log.d(TAG, "updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (mPlaybackState == null || !mStarted) {
            Log.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING
                && mPlaybackState.getPosition() >= 0) {
            Log.d(TAG, "updateNotificationPlaybackState. updating playback position to " +
                    Long.toString((System.currentTimeMillis() - mPlaybackState.getPosition()) / 1000) + " seconds");
            builder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            Log.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        builder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        Log.d(TAG, "updatePlayPauseAction");

        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_white,
                    mService.getString(R.string.label_pause),
                    mPauseIntent));
        } else {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_play_arrow_white,
                    mService.getString(R.string.label_play)
                    , mPlayIntent));
        }
    }

    public void setTrackList(ArrayList<Track> tracks) {
        mTracks = tracks;
    }

    public void setTrackIndex(int trackIndex) {
        mTrackIndex = trackIndex;
    }

    public void setArtistName(String artistName) {
        mArtistName = artistName;
    }
}
