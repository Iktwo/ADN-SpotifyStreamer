package com.iktwo.spotifystreamer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class RectangularLinearLayout extends LinearLayout {
    public RectangularLinearLayout(Context context) {
        super(context);
    }

    public RectangularLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RectangularLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth() / 4);
    }
}
