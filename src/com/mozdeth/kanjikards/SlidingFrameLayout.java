package com.mozdeth.kanjikards;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SlidingFrameLayout extends FrameLayout
{
    @SuppressWarnings("unused")
    private static final String TAG = SlidingFrameLayout.class.getName();

    public SlidingFrameLayout(Context context)
    {
        super(context);
    }

    public SlidingFrameLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public float getXFraction()
    {
        int width = getWidth();
        return (width == 0) ? 0 : getX() / (float) width;
    }

    public void setXFraction(float xFraction)
    {
        int width = getWidth();
        setX((width > 0) ? (xFraction * width) : 0);
    }
    
    public float getYFraction()
    {
        int height = getHeight();
        return (height == 0) ? 0 : getY() / (float) height;
    }

    public void setYFraction(float yFraction)
    {
        int height = getHeight();
        setY((height > 0) ? (yFraction * height) : 0);
    }
}
