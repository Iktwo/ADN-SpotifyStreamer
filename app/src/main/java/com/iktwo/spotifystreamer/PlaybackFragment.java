package com.iktwo.spotifystreamer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

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
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
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
        // mLastPlaybackState = state;

        if (state == PlaybackStateCompat.STATE_PLAYING && mImageButtonPlayPause != null)
            mImageButtonPlayPause.setImageResource(R.drawable.ic_pause_white);
        else if (state == PlaybackStateCompat.STATE_PAUSED && mImageButtonPlayPause != null)
            mImageButtonPlayPause.setImageResource(R.drawable.ic_play_arrow_white);

    }

    public void setMetadata(MediaMetadataCompat metadata) {
        if (metadata.getDescription().getIconUri() != null) {
            Picasso.with(getActivity())
                    .load(metadata.getDescription().getIconUri())
                    .placeholder(R.drawable.placeholder_artist)
                    .into(thumbnail);
        }
    }


    private void updateProgress() {
    }

    public void setOldPlaybackState(PlaybackStateCompat state) {
        mLastPlaybackState = state;
    }

    public interface OnPlaybackFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction();

        void onNextClicked();

        void onPreviousClicked();
    }
}
