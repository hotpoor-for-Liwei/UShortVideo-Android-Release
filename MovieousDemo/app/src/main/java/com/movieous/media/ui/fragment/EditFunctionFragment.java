package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.*;
import com.faceunity.entity.Filter;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.media.Constants;
import com.movieous.media.R;
import com.movieous.media.mvp.contract.FuFilterChangedListener;
import com.movieous.media.mvp.model.BeautyEnum;
import com.movieous.media.mvp.model.FilterManager;
import com.movieous.media.ui.activity.ShowCoverActivity;
import com.movieous.media.ui.adapter.BeautyFilterAdapter;
import com.movieous.media.ui.adapter.SdkFilterAdapter;
import com.movieous.media.ui.adapter.GridViewAdapter;
import com.movieous.media.utils.AppUtils;
import com.movieous.media.utils.GetPathFromUri;
import com.movieous.media.view.FrameListView;
import com.movieous.media.view.FrameSelectorView;
import com.movieous.media.view.SaveProgressDialog;
import com.movieous.media.view.TextIcon;
import com.movieous.shortvideo.UMediaFile;
import com.movieous.shortvideo.UMediaMerge;
import com.movieous.shortvideo.USaveFileListener;
import com.movieous.shortvideo.UVideoEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.movieous.media.ExtensionsKt.showToast;

/**
 * 编辑功能页
 */
public class EditFunctionFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final int REQUEST_CODE_PICK_AUDIO_MIX_FILE = 0;

    private String mVideoPath;
    private Activity mActivity;
    private TextView mTvTitle;
    private View mContentView;
    private View mCutRectView;
    private LayoutInflater mInflater;
    private TextView mSpeedTextView;
    private LinearLayout mLayoutTitle;
    private LinearLayout mLayoutContent;
    private LinearLayout mFunctionList;
    private LinearLayout mLayoutAudioVolume;
    private LinearLayout mLayoutBeauty;
    private LinearLayout mLayoutFilter;
    private LinearLayout mLayoutVideoCut;
    private LinearLayout mLayoutCover;
    private FrameListView mFrameListViewCut;
    private FrameSelectorView mSelectorView;
    private FrameListView mFrameListViewCover;
    private EditFunctionListener mFunctionListener;
    private List<Integer> mSelectedFrameIndex;
    private SaveProgressDialog mProcessingDialog;

    private FuFilterChangedListener mFilterChangedListener;
    private UVideoEdit mVideoEditor;
    private String mFilterSelected;

    private boolean mIsAssetFilter;
    Map<Integer, Double> mRecordSpeed;

    public static EditFunctionFragment getInstance(UVideoEdit videoEdit, String videoPath, FuFilterChangedListener listener, EditFunctionListener editFunctionListener) {
        EditFunctionFragment fragment = new EditFunctionFragment();
        fragment.mVideoEditor = videoEdit;
        fragment.mVideoPath = videoPath;
        fragment.mFilterChangedListener = listener;
        fragment.mFunctionListener = editFunctionListener;
        return fragment;
    }

    public void onPlayPositionChanged(int position) {
        if (mTvTitle == null || mTvTitle.getTag() == null) {
            return;
        }
        int index = (int) mTvTitle.getTag();
        if (index < 0) {
            return;
        }
        FunctionList functionList = FunctionList.values()[index];
        switch (functionList) {
            case CUT_VIDEO:
                if (mFrameListViewCut != null) {
                    mFrameListViewCut.scrollToTime(position);
                }
                break;
            case COVER:
                if (mFrameListViewCover != null) {
                    mFrameListViewCover.scrollToTime(position);
                }
                break;
        }
    }

    public interface EditFunctionListener {
        void onStartPlay();

        void onPausePlay();

        void onEffectViewShow();
    }

    private void initView(View view) {
        mLayoutTitle = view.findViewById(R.id.layout_title);
        mLayoutContent = view.findViewById(R.id.layout_content);
        mTvTitle = view.findViewById(R.id.tv_title);
        Button mBtnBack = view.findViewById(R.id.back_button);
        Button mBtnNext = view.findViewById(R.id.next_button);
        mBtnNext.setVisibility(View.INVISIBLE);
        mBtnBack.setOnClickListener(this);
        showFunctionList();
    }

    private void showFunctionLayout(String titleName, int functionIndex) {
        showContentLayout(titleName, FunctionList.values()[functionIndex]);
    }

    private void showContentLayout(String titleName, FunctionList index) {
        if (mInflater == null || mLayoutContent == null || mActivity == null) {
            return;
        }
        if (index == FunctionList.EFFECT) {
            showEffect();
            return;
        }
        mLayoutContent.removeAllViews();
        mLayoutTitle.setVisibility(View.VISIBLE);
        mTvTitle.setText(titleName);
        mTvTitle.setTag(index.ordinal());
        switch (index) {
            case AUDIO_VOLUME:
                showAudioVolume();
                break;
            case BEAUTY:
                showBeauty();
                break;
            case FILTER:
                showFilter();
                break;
            case CUT_VIDEO:
                showVideoCut();
                break;
            case COVER:
                showCover();
                break;
            default:
                break;
        }
    }

    private void showFunctionList() {
        if (mFunctionList == null) {
            mFunctionList = (LinearLayout) mInflater.inflate(R.layout.fragment_function_list, null);
            initFunctionListView(mFunctionList);
        }
        mTvTitle.setTag(-1);
        mLayoutTitle.setVisibility(View.GONE);
        mLayoutContent.removeAllViews();
        mLayoutContent.addView(mFunctionList);
    }

    private void initFunctionListView(View view) {
        view.findViewById(R.id.layout_title).setVisibility(View.GONE);
        GridView gridView = view.findViewById(R.id.grid_button);
        ArrayList<TextIcon> functionList = new ArrayList<>();
        functionList.add(new TextIcon(R.drawable.sound_set, getString(R.string.audio_volume_title)));
        functionList.add(new TextIcon(R.drawable.face_beauty_set, getString(R.string.beauty_adjust_title)));
        functionList.add(new TextIcon(R.drawable.filter, getString(R.string.btn_preview_filter)));
        functionList.add(new TextIcon(R.drawable.cut, getString(R.string.cut_clip)));
        functionList.add(new TextIcon(R.drawable.facial_effects, getString(R.string.effect_title)));
        functionList.add(new TextIcon(R.drawable.cover, getString(R.string.cover_title)));
        GridViewAdapter adapter = new GridViewAdapter(functionList, R.layout.item_grid) {
            @Override
            public void bindView(ViewHolder holder, Object obj) {
                holder.setImageResource(R.id.icon, ((TextIcon) obj).getId());
                holder.setText(R.id.name, ((TextIcon) obj).getName());
            }
        };
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener((parent, v, position, id) -> showFunctionLayout(functionList.get(position).getName(), position));
    }

    private void showAudioVolume() {
        if (mLayoutAudioVolume == null) {
            mLayoutAudioVolume = (LinearLayout) mInflater.inflate(R.layout.view_audio_volume, null);
            mLayoutAudioVolume.findViewById(R.id.mute).setOnClickListener(this);
            mLayoutAudioVolume.findViewById(R.id.select_music).setOnClickListener(this);
            ((SeekBar) mLayoutAudioVolume.findViewById(R.id.value_progress_origin)).setOnSeekBarChangeListener(this);
            ((SeekBar) mLayoutAudioVolume.findViewById(R.id.value_progress_music)).setOnSeekBarChangeListener(this);
        }
        mLayoutContent.addView(mLayoutAudioVolume);
    }

    private void showBeauty() {
        if (mLayoutBeauty == null) {
            mLayoutBeauty = (LinearLayout) mInflater.inflate(R.layout.view_beauty, null);
            ((SeekBar) mLayoutBeauty.findViewById(R.id.value_progress_blur)).setOnSeekBarChangeListener(this);
            ((SeekBar) mLayoutBeauty.findViewById(R.id.value_progress_face)).setOnSeekBarChangeListener(this);
            ((SeekBar) mLayoutBeauty.findViewById(R.id.value_progress_eye)).setOnSeekBarChangeListener(this);
        }
        mLayoutContent.addView(mLayoutBeauty);
    }

    private void showFilter() {
        if (mLayoutFilter == null) {
            mLayoutFilter = (LinearLayout) mInflater.inflate(R.layout.view_filter, null);
            initFilterAdapter();
        }
        mLayoutContent.addView(mLayoutFilter);
    }

    private void initFilterAdapter() {
        RecyclerView recyclerView = mLayoutFilter.findViewById(R.id.preview_filter_list);
        GridLayoutManager layoutManager = new GridLayoutManager(mActivity, 3);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        if (BeautyFragment.SDK_BEAUTY) {
            FilterManager filterManager = new FilterManager();
            SdkFilterAdapter filterAdapter = new SdkFilterAdapter(filterManager.getFilterList(mActivity));
            filterAdapter.setFilterSelectedListener((path, isAssetFile) -> setVideoFilter(path, isAssetFile));
            recyclerView.setAdapter(filterAdapter);
        } else {
            BeautyFilterAdapter fuFilterAdapter = new BeautyFilterAdapter(mFilterChangedListener);
            fuFilterAdapter.setFilterType(Filter.FILTER_TYPE_BEAUTY_FILTER);
            recyclerView.setAdapter(fuFilterAdapter);
        }
    }

    private void showVideoCut() {
        if (mLayoutVideoCut == null) {
            mLayoutVideoCut = (LinearLayout) mInflater.inflate(R.layout.view_video_cut, null);
            mSpeedTextView = mLayoutVideoCut.findViewById(R.id.normal_speed_text);
            mLayoutVideoCut.findViewById(R.id.normal_speed_text).setOnClickListener(this);
            mLayoutVideoCut.findViewById(R.id.fast_speed_text).setOnClickListener(this);
            mLayoutVideoCut.findViewById(R.id.super_fast_speed_text).setOnClickListener(this);
            mLayoutVideoCut.findViewById(R.id.slow_speed_text).setOnClickListener(this);
            mLayoutVideoCut.findViewById(R.id.super_slow_speed_text).setOnClickListener(this);
            setVideoDuration(0, 0);
            initCutFrameList();
        }
        mLayoutContent.addView(mLayoutVideoCut);
    }

    private void showSelectorView() {
        if (mSelectorView == null) {
            mSelectorView = mFrameListViewCut.addSelectorView();
            mSelectorView.setSelectorChangedListener(() -> {
                mFrameListViewCut.removeRectView(mCutRectView);
                mCutRectView = mFrameListViewCut.addSelectedRect(mSelectorView);
                if (mCutRectView == null) {
                    mSelectorView = null;
                    setVideoDuration(0, 0);
                } else {
                    mCutRectView.setOnTouchListener(new RectViewTouchListener());
                    FrameListView.ClipItem item = mFrameListViewCut.getClipByRectView(mCutRectView);
                    if (item != null) {
                        setVideoDuration(item.getStartTime(), item.getEndTime());
                    }
                }
            });
        }
    }

    private void setVideoDuration(long beginMs, long endMs) {
        mVideoEditor.setVideoDuration(beginMs, endMs);
        int durationS = (int) ((endMs - beginMs) / 1000);
        ((TextView) mLayoutVideoCut.findViewById(R.id.tip)).setText(String.format(getString(R.string.select_clip_tip), durationS));

    }

    private void initCutFrameList() {
        if (mFrameListViewCut == null) {
            mFrameListViewCut = mLayoutVideoCut.findViewById(R.id.frame_list_view);
            mFrameListViewCut.setVideoPath(mVideoPath);
            mFrameListViewCut.setOnVideoFrameScrollListener(timeMs -> {
                onVideoFrameScrollChanged(timeMs);
                if (mCutRectView == null) {
                    showSelectorView();
                } else {
                    mSelectorView.setVisibility(View.GONE);
                }
            });
        }
    }

    private class RectViewTouchListener implements View.OnTouchListener {
        private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mFrameListViewCut.showSelectorByRectView(mSelectorView, mCutRectView);
                return super.onSingleTapUp(e);
            }
        };
        private GestureDetector mGestureDetector = new GestureDetector(mActivity, mSimpleOnGestureListener);

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            return true;
        }
    }

    private void showEffect() {
        if (mFunctionListener != null) {
            mFunctionListener.onEffectViewShow();
        }
    }

    private void showCover() {
        if (mLayoutCover == null) {
            mLayoutCover = (LinearLayout) mInflater.inflate(R.layout.view_cover, null);
            mLayoutCover.findViewById(R.id.make_cover).setOnClickListener(this);
            mSelectedFrameIndex = new ArrayList<>();
            initCoverFrameList();
        }
        mLayoutContent.addView(mLayoutCover);
    }

    private void initCoverFrameList() {
        if (mFrameListViewCover == null) {
            mFrameListViewCover = mLayoutCover.findViewById(R.id.frame_list_view);
            mFrameListViewCover.setGetFrameMode(true, false);
            mFrameListViewCover.setVideoPath(mVideoPath);
            mFrameListViewCover.setOnVideoFrameScrollListener(this::onVideoFrameScrollChanged);
            mFrameListViewCover.setOnBindViewListener(holder -> holder.mImageView.setOnClickListener(this));
        }
    }

    private void setVideoFilter(String path, boolean isAssetFile) {
        if (mFunctionListener != null) {
            mFunctionListener.onStartPlay();
        }
        mFilterSelected = path;
        mIsAssetFilter = isAssetFile;
        mVideoEditor.setFilterFile(path, isAssetFile);
    }

    private void pausePlay() {
        if (mFunctionListener != null) {
            mFunctionListener.onPausePlay();
        }
    }

    private void onVideoFrameScrollChanged(long timeMs) {
        if (mFunctionListener != null) {
            mFunctionListener.onPausePlay();
        }
        if (timeMs > 0) {
            mVideoEditor.seekTo((int) timeMs);
        }
    }

    private void onSpeedClicked(View view) {
        if (mSpeedTextView != null) {
            mSpeedTextView.setTextColor(getResources().getColor(R.color.speedTextNormal));
        }
        TextView textView = (TextView) view;
        textView.setTextColor(getResources().getColor(R.color.app_default_color));
        mSpeedTextView = textView;
        if (mRecordSpeed ==  null) {
            mRecordSpeed = new HashMap<>();
            mRecordSpeed.put(R.id.super_slow_speed_text, Constants.VIDEO_SPEED_SUPER_SLOW);
            mRecordSpeed.put(R.id.slow_speed_text, Constants.VIDEO_SPEED_SLOW);
            mRecordSpeed.put(R.id.normal_speed_text, (double) Constants.VIDEO_SPEED_NORMAL);
            mRecordSpeed.put(R.id.fast_speed_text, (double) Constants.VIDEO_SPEED_FAST);
            mRecordSpeed.put(R.id.super_fast_speed_text, (double) Constants.VIDEO_SPEED_SUPER_FAST);
        }
        mVideoEditor.setVideoSpeed(mRecordSpeed.get(view.getId()));
    }

    private void onClickThumbnail(View view) {
        pausePlay();
        ImageView imageView = (ImageView) view;
        if (imageView.getTag() == null) {
            imageView.setTag(false);
        }
        int keyframeIndex = (int) imageView.getTag(imageView.getId());
        boolean isSelected = !((imageView.getTag() != null) && (boolean) imageView.getTag());
        int padding = isSelected ? 5 : 0;
        imageView.setTag(isSelected);
        imageView.setPadding(padding, padding, padding, padding);
        if (mSelectedFrameIndex.contains(keyframeIndex)) {
            mSelectedFrameIndex.remove(mSelectedFrameIndex.indexOf(keyframeIndex));
        } else {
            mSelectedFrameIndex.add(keyframeIndex);
        }
    }

    private void showProcessingDialog() {
        if (mProcessingDialog == null) {
            mProcessingDialog = new SaveProgressDialog(mActivity);
            mProcessingDialog.setOnCancelListener(dialog -> mVideoEditor.cancelSave());
        }
        mProcessingDialog.show();
    }

    private void onClickMakeCover(View view) {
        UMediaFile mediaFile = new UMediaFile(mVideoPath);
        UMediaMerge gifMerge = new UMediaMerge(view.getContext());
        if (mSelectedFrameIndex.size() <= 0) {
            showToast(mActivity, getString(R.string.select_frame_tip));
            return;
        }

        showProcessingDialog();
        new Thread(() -> {
            ArrayList<Bitmap> bitmaps = new ArrayList<>();
            for (int i = 0; i < mSelectedFrameIndex.size(); i++) {
                bitmaps.add(mediaFile.getVideoFrameByIndex(mSelectedFrameIndex.get(i), true).toBitmap());
            }
            mProcessingDialog.setCancelable(true);
            mProcessingDialog.setOnCancelListener(dialogInterface -> gifMerge.cancelMergeToGIF());
            gifMerge.mergeToGIF(bitmaps, 500, true, Constants.COVER_FILE_PATH, new USaveFileListener() {
                @Override
                public void onSaveFileSuccess(String destFile, UVideoEncodeParam videoEncodeParam) {
                    mActivity.runOnUiThread(() -> {
                        mProcessingDialog.dismiss();
                        startActivity(new Intent(mActivity, ShowCoverActivity.class));
                    });
                }

                @Override
                public void onSaveFileFailed(int errorCode) {
                    mActivity.runOnUiThread(() -> {
                        mProcessingDialog.dismiss();
                        showToast(mActivity, "Save file failed, error coed = " + errorCode);
                    });
                }

                @Override
                public void onSaveFileCanceled() {
                    mActivity.runOnUiThread(() -> showToast(mActivity, "Canceled"));
                }

                @Override
                public void onSaveFileProgress(float percentage) {
                    mActivity.runOnUiThread(() -> mProcessingDialog.setProgress((int) (100 * percentage)));
                }
            });
        }).start();
    }

    private void onClickMute(View view) {
        mVideoEditor.muteOriginAudio(true);
        ((SeekBar) mLayoutAudioVolume.findViewById(R.id.value_progress_origin)).setProgress(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        mVideoEditor.setFilterFile(mFilterSelected, mIsAssetFilter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
        mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_function_edit, container, false);
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView(mContentView);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String selectedFilepath = GetPathFromUri.getPath(mActivity, data.getData());
            if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                mVideoEditor.setAudioMixFile(selectedFilepath);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.thumbnail:
                onClickThumbnail(v);
                break;
            case R.id.back_button:
                showFunctionList();
                break;
            case R.id.mute:
                onClickMute(v);
                break;
            case R.id.select_music:
                startActivityForResult(Intent.createChooser(AppUtils.Companion.getMediaIntent(false), getString(R.string.select_music_file_tip)), REQUEST_CODE_PICK_AUDIO_MIX_FILE);
                break;
            case R.id.make_cover:
                onClickMakeCover(v);
                break;
            case R.id.normal_speed_text:
            case R.id.slow_speed_text:
            case R.id.super_slow_speed_text:
            case R.id.fast_speed_text:
            case R.id.super_fast_speed_text:
                onSpeedClicked(v);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int viewId = seekBar.getId();
        switch (viewId) {
            case R.id.value_progress_origin:
                mVideoEditor.setAudioVolume(progress / 100f);
                break;
            case R.id.value_progress_music:
                mVideoEditor.setAudioMixVolume(progress / 100f);
                break;
            case R.id.value_progress_blur:
            case R.id.value_progress_face:
            case R.id.value_progress_eye:
                BeautyEnum beautyType = (viewId == R.id.value_progress_blur) ? BeautyEnum.FACE_BLUR : (viewId == R.id.value_progress_eye) ? BeautyEnum.EYE_ENLARGE : BeautyEnum.CHEEK_THINNING;
                mFilterChangedListener.onBeautyValueChanged(1.0f * progress / seekBar.getMax(), beautyType);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private enum FunctionList {
        AUDIO_VOLUME,
        BEAUTY,
        FILTER,
        CUT_VIDEO,
        EFFECT,
        COVER,
    }

}
