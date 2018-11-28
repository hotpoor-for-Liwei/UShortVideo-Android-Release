package com.movieous.media.view.transition;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.media.view.TransitionTextView;
import com.movieous.shortvideo.USaveFileListener;
import com.movieous.shortvideo.UTransitionManager;

public class TransitionBase implements View.OnClickListener {
    protected static final int DURATION = 2500;

    private ViewGroup mViewGroup;

    protected UTransitionManager mTransitionMaker;
    protected TransitionListener mTransitionListener;
    protected Context mContext;
    protected TransitionTextView mTitle;
    protected TransitionTextView mSubtitle;
    protected int mWidth;
    protected int mHeight;

    public TransitionBase(ViewGroup viewGroup, UVideoEncodeParam setting) {
        mViewGroup = viewGroup;
        mContext = viewGroup.getContext();
        mWidth = mViewGroup.getWidth();
        mHeight = mViewGroup.getHeight();
        mTransitionMaker = new UTransitionManager(mViewGroup, setting);
        init();
    }

    public interface TransitionListener {
        void onTitleClick();
    }

    public void setTransitionListener(TransitionListener listener) {
        mTransitionListener = listener;
    }

    public TransitionTextView getTitle() {
        return mTitle;
    }

    public TransitionTextView getSubtitle() {
        return mSubtitle;
    }

    public void init() {
        mTransitionMaker.setDuration(DURATION);
        mTransitionMaker.setBackgroundColor(Color.BLACK);

        initViews();
        initPosAndTrans();
    }

    public void save(String dstFilePath, USaveFileListener saveListener) {
        mTransitionMaker.save(dstFilePath, saveListener);
    }

    public void setVisibility(int visibility) {
        mViewGroup.setVisibility(visibility);
    }

    public void play() {
        mTransitionMaker.play();
    }

    public void stop() {
        mTransitionMaker.stop();
    }

    public void destroy() {
        mTransitionMaker.destroy();
    }

    public void cancelSave() {
        mTransitionMaker.cancelSave();
    }

    public void updateTransitions() {
        mTransitionMaker.removeAllResource();
        addViews();
        initPosAndTrans();
    }

    protected void initViews() {
    }

    protected void initPosAndTrans() {
        if (mTitle != null) {
            mTitle.setOnClickListener(this);
        }
        if (mSubtitle != null) {
            mSubtitle.setOnClickListener(this);
        }
    }

    protected void addViews() {
        if (mTitle != null) {
            mTransitionMaker.addText(mTitle);
        }
        if (mSubtitle != null) {
            mTransitionMaker.addText(mSubtitle);
        }
    }

    protected void setViewsVisible(int visible) {
        if (mTitle != null) {
            mTitle.setVisibility(visible);
        }
        if (mSubtitle != null) {
            mSubtitle.setVisibility(visible);
        }
    }

    @Override
    public void onClick(View v) {
        if (mTransitionListener != null) {
            mTransitionListener.onTitleClick();
        }
    }
}