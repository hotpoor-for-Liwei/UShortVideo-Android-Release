package com.movieous.media.view.transition;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.media.R;
import com.movieous.media.view.TransitionTextView;
import com.movieous.shortvideo.transition.UFadeTransition;
import com.movieous.shortvideo.transition.UPositionTransition;

public class TransitionTitle extends TransitionBase {
    private static final int MOVE_DISTANCE = 100;

    public TransitionTitle(ViewGroup viewGroup, UVideoEncodeParam setting) {
        super(viewGroup, setting);
    }

    @Override
    protected void initPosAndTrans() {
        //you should init positions and transitions in post runnable , because the view has been layout at that moment.
        mSubtitle.post(() -> {
            super.initPosAndTrans();
            initPosition();
            initTransitions();
        });
    }

    private void initTransitions() {
        UFadeTransition fadeTransition = new UFadeTransition(0, DURATION / 2, 0, 1);
        mTransitionMaker.addTransition(mTitle, fadeTransition);
        UPositionTransition positionTransition = new UPositionTransition(0, DURATION / 2, (int) mTitle.getX(), (int) mTitle.getY(), (int) mTitle.getX(), (int) mTitle.getY() - MOVE_DISTANCE);
        mTransitionMaker.addTransition(mTitle, positionTransition);

        fadeTransition = new UFadeTransition(0, DURATION / 2, 0, 1);
        mTransitionMaker.addTransition(mSubtitle, fadeTransition);
        positionTransition = new UPositionTransition(0, DURATION / 2, (int) mSubtitle.getX(), (int) mSubtitle.getY(), (int) mSubtitle.getX(), (int) mSubtitle.getY() - MOVE_DISTANCE);
        mTransitionMaker.addTransition(mSubtitle, positionTransition);

        mTransitionMaker.play();
        setViewsVisible(View.VISIBLE);
    }

    private void initPosition() {
        int titleX = mWidth / 2 - mTitle.getWidth() / 2;
        int titleY = mHeight / 2 - mTitle.getHeight() + MOVE_DISTANCE;
        mTitle.setTranslationX(titleX);
        mTitle.setTranslationY(titleY);

        int subtitleX = mWidth / 2 - mSubtitle.getWidth() / 2;
        int subtitleY = titleY + mTitle.getHeight();
        mSubtitle.setTranslationX(subtitleX);
        mSubtitle.setTranslationY(subtitleY);
    }

    @Override
    protected void initViews() {
        Typeface textTypeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lobster-1.4.otf");
        Typeface descTypeFace = Typeface.createFromAsset(mContext.getAssets(), "fonts/FZLanTingHeiS-L-GB-Regular.TTF");
        mTitle = new TransitionTextView(mContext);
        mTitle.setText("Movieous");
        mTitle.setPadding(0, 0, 0, 0);
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(textTypeface);
        mTitle.setTextSize(26);
        mTitle.setTag(0);

        mSubtitle = new TransitionTextView(mContext);
        mSubtitle.setTag(1);
        mSubtitle.setText(R.string.movieous_slogan);
        mSubtitle.setTypeface(descTypeFace);
        mSubtitle.setPadding(0, 0, 0, 0);
        mSubtitle.setTextColor(Color.parseColor("#eed2b9"));
        mSubtitle.setTextSize(15);

        addViews();
        setViewsVisible(View.INVISIBLE);
    }

}
