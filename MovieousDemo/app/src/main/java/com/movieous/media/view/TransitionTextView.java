package com.movieous.media.view;

import android.content.Context;
import android.util.AttributeSet;
import com.movieous.shortvideo.widget.UTextView;

public class TransitionTextView extends UTextView {
    public TransitionTextView(Context context) {
        this(context, null);
    }

    public TransitionTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setClickable(false);
        setPadding(0, 0, 0, 0);
    }
}
