package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.faceunity.entity.Effect;
import com.faceunity.entity.Filter;
import com.movieous.media.R;
import com.movieous.media.mvp.contract.FuFilterChangedListener;
import com.movieous.media.mvp.model.BeautyEnum;
import com.movieous.media.mvp.model.FilterManager;
import com.movieous.media.ui.adapter.BeautyFilterAdapter;
import com.movieous.media.ui.adapter.SdkFilterAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * 美颜设置页面
 */
public class BeautyFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, FuFilterChangedListener {
    private static final String TAG = "BeautyFragment";
    public static final boolean SDK_BEAUTY = false;
    public static final int SHOW_TYPE_BEAUTY = 1;
    public static final int SHOW_TYPE_FILTER = 2;
    public static float sBlurLevel = 0.7f;//磨皮
    public static float sEyeEnlarging = 0.4f;//大眼
    public static float sCheekThinning = 0.4f;//瘦脸

    private int mTitleButtonIndex = SHOW_TYPE_BEAUTY;
    private View mContentView;
    private Button mBtnBeauty;
    private Button mBtnFilter;
    private LinearLayout mLayoutContent;
    private LinearLayout mLayoutBeauty;
    private LinearLayout mLayoutFilter;
    private RecyclerView mFilterRecyclerView;
    private GridLayoutManager mFilterLayoutManager;
    private SdkFilterAdapter mSdkFilterAdapter;
    private FilterManager mFilterManager;
    private LayoutInflater mInflater;
    private Activity mActivity;

    // FU beauty
    private BeautyFilterAdapter mFuFilterAdapter;
    private FuFilterChangedListener mFilterChangedListener;

    public void setFilterChangedListener(FuFilterChangedListener listener) {
        mFilterChangedListener = listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
        mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_filter_edit, container, false);
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView(mContentView);
    }

    private void initView(View view) {
        mLayoutContent = view.findViewById(R.id.layout_content);
        mBtnBeauty = view.findViewById(R.id.btn_preview_beauty);
        mBtnFilter = view.findViewById(R.id.btn_preview_filter);
        mBtnBeauty.setOnClickListener(this);
        mBtnFilter.setOnClickListener(this);
        showContentLayout(mTitleButtonIndex);
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
        int id = v.getId();
        if (id == R.id.btn_preview_beauty) {
            showContentLayout(SHOW_TYPE_BEAUTY);
        } else if (id == R.id.btn_preview_filter) {
            showContentLayout(SHOW_TYPE_FILTER);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int viewId = seekBar.getId();
        BeautyEnum beautyType = (viewId == R.id.value_progress_blur) ? BeautyEnum.FACE_BLUR : (viewId == R.id.value_progress_eye) ? BeautyEnum.EYE_ENLARGE : BeautyEnum.CHEEK_THINNING;
        onBeautyValueChanged(1.0f * progress / seekBar.getMax(), beautyType);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void showContentLayout(int index) {
        mTitleButtonIndex = index;
        resetTitle();
        if (index == SHOW_TYPE_BEAUTY) {
            showBeautyLayout();
        } else if (index == SHOW_TYPE_FILTER) {
            showFilterLayout();
        }
    }

    private void resetTitle() {
        mBtnBeauty.setSelected(mTitleButtonIndex == SHOW_TYPE_BEAUTY);
        mBtnFilter.setSelected(mTitleButtonIndex == SHOW_TYPE_FILTER);
    }

    /**
     * 显示美颜视图布局
     */
    private void showBeautyLayout() {
        if (mLayoutBeauty == null) {
            mLayoutBeauty = (LinearLayout) mInflater.inflate(R.layout.view_beauty, null);
            SeekBar blurSeekbar = mLayoutBeauty.findViewById(R.id.value_progress_blur);
            blurSeekbar.setOnSeekBarChangeListener(this);
            blurSeekbar.setProgress((int) (sBlurLevel * 100));
            SeekBar faceSeekbar = mLayoutBeauty.findViewById(R.id.value_progress_face);
            faceSeekbar.setOnSeekBarChangeListener(this);
            faceSeekbar.setProgress((int) (sCheekThinning * 100));
            SeekBar eyeSeekbar = mLayoutBeauty.findViewById(R.id.value_progress_eye);
            eyeSeekbar.setOnSeekBarChangeListener(this);
            eyeSeekbar.setProgress((int) (sEyeEnlarging * 100));
        }
        mLayoutContent.removeAllViews();
        mLayoutContent.addView(mLayoutBeauty);
    }

    /**
     * 显示滤镜布局
     */
    private void showFilterLayout() {
        if (mLayoutFilter == null) {
            mLayoutFilter = (LinearLayout) mInflater.inflate(R.layout.view_filter, null);
            mFilterRecyclerView = mLayoutFilter.findViewById(R.id.preview_filter_list);
            mFilterLayoutManager = new GridLayoutManager(mActivity, 3);
            mFilterLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mFilterRecyclerView.setLayoutManager(mFilterLayoutManager);
            if (SDK_BEAUTY) {
                if (mFilterManager == null) {
                    mFilterManager = new FilterManager();
                }
                mSdkFilterAdapter = new SdkFilterAdapter(mFilterManager.getFilterList(mActivity));
                mFilterRecyclerView.setAdapter(mSdkFilterAdapter);
            } else {
                mFuFilterAdapter = new BeautyFilterAdapter(this);
                mFuFilterAdapter.setFilterType(Filter.FILTER_TYPE_BEAUTY_FILTER);
                mFilterRecyclerView.setAdapter(mFuFilterAdapter);
            }
        }
        mLayoutContent.removeAllViews();
        mLayoutContent.addView(mLayoutFilter);
    }

    @Override
    public void onEffectSelected(int position, @NotNull Effect effect) {
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