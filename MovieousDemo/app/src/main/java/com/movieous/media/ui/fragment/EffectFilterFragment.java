package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.faceunity.entity.Effect;
import com.faceunity.entity.Filter;
import com.movieous.media.R;
import com.movieous.media.api.sensesdk.utils.FileUtils;
import com.movieous.media.api.sensesdk.view.StickerItem;
import com.movieous.media.mvp.contract.FuFilterChangedListener;
import com.movieous.media.mvp.contract.SenseFilterChangedListener;
import com.movieous.media.mvp.model.BeautyEnum;
import com.movieous.media.mvp.model.EffectEnum;
import com.movieous.media.ui.adapter.EffectFilterAdapter;
import com.movieous.media.ui.adapter.SenseStickerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 贴纸选择页面
 */
public class EffectFilterFragment extends Fragment implements View.OnClickListener, FuFilterChangedListener {
    private static final String TAG = "EffectFilterFragment";
    public static final int SHOW_TYPE_ALL = 0;
    public static final int SHOW_TYPE_STICK = 1;
    public static final int SHOW_TYPE_EFFECT = 2;

    private View mContentView;
    private LinearLayout mLayoutStickerTitle;
    private LinearLayout mLayoutStickerContent;
    private LayoutInflater mInflater;
    private Activity mActivity;
    private Button mBackBtn;
    private Button mSeneseTimeTitle;
    private Button mNormalFilterTitle;
    private Button mArFilterTitle;
    private Button mFaceWrapFilterTitle;
    private Button mFaceChangeFilterTitle;
    private Button mBackgroundFilterTitle;
    private Button mMusicFilterTitle;
    private Button mExpressionTitle;
    private EffectFragmentListener mEffectFragmentListener;
    private FuFilterChangedListener mFilterChangedListener;
    private RecyclerView mFuEffectRecycler;
    private EffectFilterAdapter mEffectFilterAdapter;
    private SenseStickerAdapter mSenseStickerAdapter;
    // sense time
    private HashMap<String, ArrayList<StickerItem>> mStickerLists = new HashMap<>();
    private int mCurrentStickerPosition = -1;
    private SenseFilterChangedListener mSTFilterChangedListener;

    private int mEffectType;
    private int mShowType = SHOW_TYPE_ALL;
    private int[] mEffectTypeShowIndex = new int[]{0, 0, 0, 0, 0, 0, 0, 0};

    public static EffectFilterFragment getInstance(int showType, FuFilterChangedListener listener) {
        EffectFilterFragment fragment = new EffectFilterFragment();
        fragment.mShowType = showType;
        fragment.mFilterChangedListener = listener;
        return fragment;
    }

    public Effect getCurrentEffect() {
        return (mEffectFilterAdapter != null) ? mEffectFilterAdapter.getSelectEffect() : null;
    }

    public interface EffectFragmentListener {
        void onBackButtonPressed();
    }

    public void setEffectFragmentListener(EffectFragmentListener listener) {
        mEffectFragmentListener = listener;
    }

    public void setFilterChangedListener(SenseFilterChangedListener listener) {
        mSTFilterChangedListener = listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
        mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_effect, container, false);
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView(mContentView);
    }

    @Override
    public void onPause() {
        super.onPause();
        mEffectFilterAdapter.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEffectFilterAdapter.onResume();
    }

    @Override
    public void onDestroyView() {
        mContentView = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            mEffectFragmentListener.onBackButtonPressed();
            return;
        }
        if (v.getId() == R.id.sense_time_filter) {
            mEffectType = Effect.EFFECT_TYPE_SENSETIME;
            showSenseFilter();
            setTitleColor();
            return;
        }
        if (mEffectFilterAdapter != null) {
            mEffectFilterAdapter.onPause();
        }
        switch (v.getId()) {
            case R.id.sense_time_filter:
                break;
            case R.id.fu_normal_filter:
                mEffectType = Effect.EFFECT_TYPE_NORMAL;
                break;
            case R.id.fu_ar_filter:
                mEffectType = Effect.EFFECT_TYPE_AR;
                break;
            case R.id.fu_face_wrap_filter:
                mEffectType = Effect.EFFECT_TYPE_FACE_WARP;
                break;
            case R.id.fu_face_change_filter:
                mEffectType = Effect.EFFECT_TYPE_FACE_CHANGE;
                break;
            case R.id.fu_background_filter:
                mEffectType = Effect.EFFECT_TYPE_BACKGROUND;
                break;
            case R.id.fu_music_filter:
                mEffectType = Effect.EFFECT_TYPE_MUSIC_FILTER;
                break;
            case R.id.expression_filter:
                mEffectType = Effect.EFFECT_TYPE_EXPRESSION;
                break;
        }
        if (mEffectType == Effect.EFFECT_TYPE_SENSETIME) {
            if (mSTFilterChangedListener != null) {
                int position = Integer.parseInt(v.getTag().toString());
                if (mCurrentStickerPosition == position) {
                    mCurrentStickerPosition = -1;
                    mSTFilterChangedListener.onRemoveAllStickers();
                } else {
                    mCurrentStickerPosition = position;
                    mSTFilterChangedListener.onChangeSticker(mStickerLists.get("sticker_new").get(position).path);
                }
                mSenseStickerAdapter.setSelectedPosition(mCurrentStickerPosition);
                mSenseStickerAdapter.notifyDataSetChanged();
            }
        } else {
            int effectIndex = mEffectTypeShowIndex[getEffectIndex(mEffectType)];
            mEffectFilterAdapter = new EffectFilterAdapter(mActivity, mEffectType, effectIndex, this);
            mEffectFilterAdapter.onResume();
            mFuEffectRecycler.setAdapter(mEffectFilterAdapter);
            mFuEffectRecycler.scrollToPosition(effectIndex);
            Effect effect = EffectEnum.getEffectsByEffectType(mEffectType).get(effectIndex);
            onEffectSelected(effectIndex, effect);
            setTitleColor();
        }
    }

    private void initView(View view) {
        mLayoutStickerTitle = view.findViewById(R.id.layout_title);
        mLayoutStickerContent = view.findViewById(R.id.layout_content);
        mNormalFilterTitle = view.findViewById(R.id.fu_normal_filter);
        mArFilterTitle = view.findViewById(R.id.fu_ar_filter);
        mFaceWrapFilterTitle = view.findViewById(R.id.fu_face_wrap_filter);
        mFaceChangeFilterTitle = view.findViewById(R.id.fu_face_change_filter);
        mBackBtn = view.findViewById(R.id.back_button);
        mBackgroundFilterTitle = view.findViewById(R.id.fu_background_filter);
        mMusicFilterTitle = view.findViewById(R.id.fu_music_filter);
        mExpressionTitle = view.findViewById(R.id.expression_filter);
        mSeneseTimeTitle = view.findViewById(R.id.sense_time_filter);

        mNormalFilterTitle.setOnClickListener(this);
        mArFilterTitle.setOnClickListener(this);
        mFaceWrapFilterTitle.setOnClickListener(this);
        mFaceChangeFilterTitle.setOnClickListener(this);
        mBackBtn.setOnClickListener(this);
        mBackgroundFilterTitle.setOnClickListener(this);
        mMusicFilterTitle.setOnClickListener(this);
        mExpressionTitle.setOnClickListener(this);
        mSeneseTimeTitle.setOnClickListener(this);

        mBackBtn.setVisibility(mShowType == SHOW_TYPE_EFFECT ? View.VISIBLE : View.GONE);
        boolean showStick = mShowType == SHOW_TYPE_ALL || mShowType == SHOW_TYPE_STICK;
        mNormalFilterTitle.setVisibility(showStick ? View.VISIBLE : View.GONE);
        mArFilterTitle.setVisibility(showStick ? View.VISIBLE : View.GONE);
        mFaceWrapFilterTitle.setVisibility(showStick ? View.VISIBLE : View.GONE);
        mFaceChangeFilterTitle.setVisibility(showStick ? View.VISIBLE : View.GONE);
        boolean showEffect = mShowType == SHOW_TYPE_ALL || mShowType == SHOW_TYPE_EFFECT;
        mBackgroundFilterTitle.setVisibility(showEffect ? View.VISIBLE : View.GONE);
        mMusicFilterTitle.setVisibility(showEffect ? View.VISIBLE : View.GONE);
        mExpressionTitle.setVisibility(showEffect ? View.VISIBLE : View.GONE);
        mSeneseTimeTitle.setVisibility(showEffect ? View.VISIBLE : View.GONE);

        mEffectType = (mShowType == SHOW_TYPE_ALL || mShowType == SHOW_TYPE_STICK) ? Effect.EFFECT_TYPE_NORMAL : Effect.EFFECT_TYPE_BACKGROUND;
        mFuEffectRecycler = view.findViewById(R.id.fu_effect_recycler);
        mFuEffectRecycler.setLayoutManager(new GridLayoutManager(view.getContext(), 3));
        mFuEffectRecycler.addItemDecoration(new SenseFilterFragment.SpaceItemDecoration(0));
        int effectIndex = mEffectTypeShowIndex[getEffectIndex(mEffectType)];
        mFuEffectRecycler.setAdapter(mEffectFilterAdapter = new EffectFilterAdapter(mActivity, mEffectType, effectIndex, this));
        ((SimpleItemAnimator) mFuEffectRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
        Effect effect = EffectEnum.getEffectsByEffectType(mEffectType).get(0);
        onEffectSelected(0, effect);
        setTitleColor();
    }

    private void initStickerList() {
        mStickerLists.put("sticker_new", FileUtils.getStickerFiles(mActivity, "sensetime"));
    }

    private void showSenseFilter() {
        if (mSenseStickerAdapter == null) {
            initStickerList();
            mSenseStickerAdapter = new SenseStickerAdapter(mStickerLists.get("sticker_new"), mActivity);
            mSenseStickerAdapter.setClickStickerListener(this);
        }
        mFuEffectRecycler.setAdapter(mSenseStickerAdapter);
    }

    private void setTitleColor() {
        mNormalFilterTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_NORMAL);
        mArFilterTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_AR);
        mFaceWrapFilterTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_FACE_WARP);
        mFaceChangeFilterTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_FACE_CHANGE);
        mBackgroundFilterTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_BACKGROUND);
        mMusicFilterTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_MUSIC_FILTER);
        mExpressionTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_EXPRESSION);
        mSeneseTimeTitle.setSelected(mEffectType == Effect.EFFECT_TYPE_SENSETIME);
    }

    private int getEffectIndex(int effectType) {
        int effectIndex = 0;
        switch (effectType) {
            case Effect.EFFECT_TYPE_SENSETIME:
                effectIndex = 100;
                break;
            case Effect.EFFECT_TYPE_NORMAL:
                effectIndex = 0;
                break;
            case Effect.EFFECT_TYPE_AR:
                effectIndex = 1;
                break;
            case Effect.EFFECT_TYPE_FACE_WARP:
                effectIndex = 2;
                break;
            case Effect.EFFECT_TYPE_FACE_CHANGE:
                effectIndex = 3;
                break;
            case Effect.EFFECT_TYPE_BACKGROUND:
                effectIndex = 4;
                break;
            case Effect.EFFECT_TYPE_MUSIC_FILTER:
                effectIndex = 5;
                break;
            case Effect.EFFECT_TYPE_EXPRESSION:
                effectIndex = 6;
                break;
        }
        return effectIndex;
    }

    @Override
    public void onEffectSelected(int position, Effect effect) {
        mEffectTypeShowIndex[getEffectIndex(mEffectType)] = position;
        mFilterChangedListener.onEffectSelected(position, effect);
    }

    @Override
    public void onMusicFilterTime(long time) {
        mFilterChangedListener.onMusicFilterTime(time);
    }

    @Override
    public void onBeautyValueChanged(float value, @NotNull BeautyEnum beautyType) {
        mFilterChangedListener.onBeautyValueChanged(value, beautyType);
    }

    @Override
    public void onFilterNameSelected(@NotNull Filter filterName) {
        mFilterChangedListener.onFilterNameSelected(filterName);
    }

}
