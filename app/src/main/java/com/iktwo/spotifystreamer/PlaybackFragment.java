package com.iktwo.spotifystreamer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlaybackFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    private static final String ARG_THUMBNAIL_URL = "thumbnail_url";
    private static final String ARG_PARAM2 = "param2";
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;
    private static final String TAG = PlaybackFragment.class.getSimpleName();

    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();

    private TextView mTextViewArtist;
    private TextView mTextViewTrackName;
    private ImageButton mImageButtonPlayPause;
    private ImageView thumbnail;
    private SeekBar mSeekBar;
    private ScheduledFuture<?> mScheduleFuture;
    private Handler mHandler = new Handler();

    private PlaybackStateCompat mLastPlaybackState;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private OnPlaybackFragmentInteractionListener mListener;

    public PlaybackFragment() {
    }

    // TODO: Rename and change types and number of parameters
    public static PlaybackFragment newInstance(String param1, String param2) {
        PlaybackFragment fragment = new PlaybackFragment();
        Bundle args = new Bundle();
        args.putString(ARG_THUMBNAIL_URL, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_THUMBNAIL_URL);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private void stopSeekBarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekBarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playback, container, false);

        mTextViewTrackName = (TextView) view.findViewById(R.id.text_view_song_name);
        mTextViewArtist = (TextView) view.findViewById(R.id.text_view_artist);

        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                /// TODO: update text here
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onSeekTo(seekBar.getProgress());
            }
        });

        thumbnail = (ImageView) view.findViewById(R.id.image_view_thumbnail);

        Picasso.with(getActivity())
                .load(R.drawable.placeholder_artist)
                .into(thumbnail);

        mImageButtonPlayPause = (ImageButton) view.findViewById(R.id.button_playpause);
        mImageButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonPressed();
            }
        });

        (view.findViewById(R.id.button_next)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextClicked();
            }
        });

        (view.findViewById(R.id.button_previous)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPreviousClicked();
            }
        });

        return view;
    }

    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onFragmentInteraction();
        }
    }

    public void onNextClicked() {
        if (mListener != null) {
            mListener.onNextClicked();
        }
    }

    public void onPreviousClicked() {
        if (mListener != null) {
            mListener.onPreviousClicked();
        }
    }

    public void onSeekTo(int position) {
        if (mListener != null) {
            mListener.onSeekTo(position);

            scheduleSeekbarUpdate();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnPlaybackFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setTrackArt(String url) {
        Picasso.with(getActivity())
                .load(url)
                .placeholder(R.drawable.placeholder_artist)
                .into(thumbnail);
    }

    public void setPlaybackState(int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING:
                if (mImageButtonPlayPause != null)
                    mImageButtonPlayPause.setImageResource(R.drawable.ic_pause_white);

                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                if (mImageButtonPlayPause != null)
                    mImageButtonPlayPause.setImageResource(R.drawable.ic_play_arrow_white);

                stopSeekBarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                stopSeekBarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                stopSeekBarUpdate();
                break;
            default:
                Log.d(TAG, "Unhandled state " + state);
        }
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        Log.d(TAG, "setMetadata" + metadata.keySet().toString());

        String string = "Bundle{";

        for (String key : metadata.keySet()) {
            string += " " + key + " - ";
        }

        Log.d(TAG, "BUNDLE metadata: " + string);

        if (metadata.getDescription().getExtras() != null) {
            Log.d(TAG, "SetMetadata: extras" + metadata.getDescription().getExtras().keySet().toString());
        }

        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) != null)
            mTextViewArtist.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));

        if (metadata.getDescription().getTitle() != null)
            mTextViewTrackName.setText(metadata.getDescription().getTitle());

        if (metadata.getDescription().getIconUri() != null) {
            Picasso.with(getActivity())
                    .load(metadata.getDescription().getIconUri())
                    .placeholder(R.drawable.placeholder_artist)
                    .into(thumbnail);
        }

        mSeekBar.setMax(30000);
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            Log.w(TAG, "updateProgress mLastPlaybackState is null");
            return;
        }

        long currentPosition = mLastPlaybackState.getPosition();

        if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            long timeDelta = SystemClock.elapsedRealtime() - mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }

        mSeekBar.setProgress((int) currentPosition);
    }

    public void setOldPlaybackState(PlaybackStateCompat state) {
        mLastPlaybackState = state;
    }

    public interface OnPlaybackFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction();

        void onNextClicked();

        void onPreviousClicked();

        void onSeekTo(int progress);
    }
}
