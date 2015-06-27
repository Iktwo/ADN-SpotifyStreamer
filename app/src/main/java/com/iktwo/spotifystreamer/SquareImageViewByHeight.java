package com.iktwo.spotifystreamer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageViewByHeight extends ImageView {
    public SquareImageViewByHeight(Context context) {
        super(context);
    }

    public SquareImageViewByHeight(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageViewByHeight(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredHeight());
    }
}
