package com.iktwo.spotifystreamer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageViewByWidth extends ImageView {
    public SquareImageViewByWidth(Context context) {
        super(context);
    }

    public SquareImageViewByWidth(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageViewByWidth(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}
