package com.movieous.media.view.transition;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.media.R;
import com.movieous.media.view.TransitionTextView;
import com.movieous.shortvideo.transition.UFadeTransition;
import com.movieous.shortvideo.transition.UPositionTransition;
import com.movieous.shortvideo.widget.UTextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TransitionTail extends TransitionBase {
    private UTextView mTitleTip;
    private UTextView mSubtitleTip;

    private static final int MOVE_DISTANCE = 100;

    public TransitionTail(ViewGroup viewGroup, UVideoEncodeParam setting) {
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
        //title transitions
        UFadeTransition fadeTransition = new UFadeTransition(0, DURATION / 2, 0, 1);
        mTransitionMaker.addTransition(mTitle, fadeTransition);
        UPositionTransition positionTransition = new UPositionTransition(0, DURATION / 2, (int) mTitle.getX(), (int) mTitle.getY(), (int) mTitle.getX(), (int) mTitle.getY() - MOVE_DISTANCE);
        mTransitionMaker.addTransition(mTitle, positionTransition);

        //subtitle transitions
        fadeTransition = new UFadeTransition(0, DURATION / 2, 0, 1);
        mTransitionMaker.addTransition(mSubtitle, fadeTransition);
        positionTransition = new UPositionTransition(0, DURATION / 2, (int) mSubtitle.getX(), (int) mSubtitle.getY(), (int) mSubtitle.getX(), (int) mSubtitle.getY() - MOVE_DISTANCE);
        mTransitionMaker.addTransition(mSubtitle, positionTransition);

        //title tip transitions
        fadeTransition = new UFadeTransition(0, DURATION / 2, 0, 1);
        mTransitionMaker.addTransition(mTitleTip, fadeTransition);
        positionTransition = new UPositionTransition(0, DURATION / 2, (int) mTitleTip.getX(), (int) mTitleTip.getY(), (int) mTitleTip.getX(), (int) mTitleTip.getY() - MOVE_DISTANCE);
        mTransitionMaker.addTransition(mTitleTip, positionTransition);

        //subtitle tip transitions
        fadeTransition = new UFadeTransition(0, DURATION / 2, 0, 1);
        mTransitionMaker.addTransition(mSubtitleTip, fadeTransition);
        positionTransition = new UPositionTransition(0, DURATION / 2, (int) mSubtitleTip.getX(), (int) mSubtitleTip.getY(), (int) mSubtitleTip.getX(), (int) mSubtitleTip.getY() - MOVE_DISTANCE);
        mTransitionMaker.addTransition(mSubtitleTip, positionTransition);

        mTransitionMaker.play();
        setViewsVisible(View.VISIBLE);
    }

    private void initPosition() {
        int titleX = mWidth / 2 - mTitle.getWidth() / 2;
        int titleY = mHeight / 2 - mTitle.getHeight() + MOVE_DISTANCE;
        mTitle.setTranslationX(titleX);
        mTitle.setTranslationY(titleY);

        int titleTipX = mWidth / 2 - mTitleTip.getWidth() / 2;
        int titleTipY = titleY - mTitleTip.getHeight();
        mTitleTip.setTranslationX(titleTipX);
        mTitleTip.setTranslationY(titleTipY);

        int subtitleTipX = mWidth / 2 - mSubtitleTip.getWidth() / 2;
        int subtitleTipY = mHeight / 2 + MOVE_DISTANCE;
        mSubtitleTip.setTranslationX(subtitleTipX);
        mSubtitleTip.setTranslationY(subtitleTipY);

        int subtitleX = mWidth / 2 - mSubtitle.getWidth() / 2;
        int subtitleY = subtitleTipY + mSubtitleTip.getHeight();
        mSubtitle.setTranslationX(subtitleX);
        mSubtitle.setTranslationY(subtitleY);
    }


    @Override
    protected void initViews() {
        mTitle = new TransitionTextView(mContext);
        mTitle.setText("Movieous");
        mTitle.setPadding(0, 0, 0, 0);
        mTitle.setTextColor(Color.parseColor("#FFCC99"));
        mTitle.setTextSize(16);

        mTitleTip = new TransitionTextView(mContext);
        mTitleTip.setText(R.string.edit_author);
        mTitleTip.setPadding(0, 0, 0, 0);
        mTitleTip.setTextColor(Color.parseColor("#FFFFFF"));
        mTitleTip.setTextSize(16);

        mSubtitleTip = new TransitionTextView(mContext);
        mSubtitleTip.setText(R.string.edit_video_time_address);
        mSubtitleTip.setPadding(0, 0, 0, 0);
        mSubtitleTip.setTextColor(Color.parseColor("#FFFFFF"));
        mSubtitleTip.setTextSize(16);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(mContext.getString(R.string.format_date_pattern));
        Date date = new Date();
        String dateTip = String.format(mContext.getResources().getString(R.string.video_tail_tip_time), simpleDateFormat.format(date));
        mSubtitle = new TransitionTextView(mContext);
        mSubtitle.setText(dateTip);
        mSubtitle.setPadding(0, 0, 0, 0);
        mSubtitle.setTextColor(Color.parseColor("#FFCC99"));
        mSubtitle.setTextSize(16);

        addViews();
        setViewsVisible(View.INVISIBLE);
    }

    @Override
    protected void addViews() {
        super.addViews();
        mTransitionMaker.addText(mTitleTip);
        mTransitionMaker.addText(mSubtitleTip);
    }

    @Override
    protected void setViewsVisible(int visible) {
        super.setViewsVisible(visible);
        mTitleTip.setVisibility(visible);
        mSubtitleTip.setVisibility(visible);
    }
}
