package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.movieous.capture.UCameraFocusListener;
import com.movieous.capture.UCameraParam;
import com.movieous.capture.UMicrophoneParam;
import com.movieous.codec.UAudioEncodeParam;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.filter.UWatermarkParam;
import com.movieous.media.Constants;
import com.movieous.media.R;
import com.movieous.media.api.fusdk.FuSDKManager;
import com.movieous.media.mvp.model.EffectEnum;
import com.movieous.media.ui.activity.PlaybackActivity;
import com.movieous.media.ui.activity.VideoEditActivity;
import com.movieous.media.utils.GetPathFromUri;
import com.movieous.media.utils.StringUtils;
import com.movieous.media.view.CameraFocusIndicator;
import com.movieous.media.view.HorizontalIndicatorView;
import com.movieous.media.view.RecordTimer;
import com.movieous.media.view.ShutterButton;
import com.movieous.shortvideo.URecordParam;
import com.movieous.shortvideo.URecordStateListener;
import com.movieous.shortvideo.UVideoRecord;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.movieous.media.ExtensionsKt.showToast;

/**
 * 相机预览页面
 */
public class VideoRecordFragment extends PreviewFragment implements View.OnClickListener, HorizontalIndicatorView.OnIndicatorListener,
        URecordStateListener, UCameraFocusListener {
    private static final String TAG = "VideoRecordFragment";
    private static final VideoRecordFragment mInstance = new VideoRecordFragment();

    private UVideoRecord mVideoRecorder;
    private URecordParam mRecordParam;
    private UWatermarkParam mWatermarkParam;
    private UVideoEncodeParam mVideoEncodeParam;

    private View mContentView;
    private TextView mTvCountDown;
    private ShutterButton mBtnShutter;
    private Button mBtnDeleteClip;
    private Button mBtnMergeClip;
    private Button mBtnStickers;
    private Button mBtnVideoEdit;
    private HorizontalIndicatorView mBottomIndicator;
    private TextView mSpeedTextView;
    private LinearLayout mSpeedPanel;
    private EffectFilterFragment mFuFilterFragment;
    private SenseFilterFragment mSenseFilterFragment;
    private BeautyFragment mBeautyFragment;

    private RecordTimer mRecordTimer;
    private GestureDetector mGestureDetector;
    private CameraFocusIndicator mFocusIndicator;
    private int mFocusIndicatorX;
    private int mFocusIndicatorY;
    private List<String> mIndicatorText = new ArrayList<>();

    private boolean mIsEditVideo = false;
    private boolean mIsShowingEffect = false;
    private boolean mIsShowingBeauty = false;
    private double mRecordSpeed;

    public static VideoRecordFragment getInstance() {
        return mInstance;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(getLayoutId(), container, false);
        return mContentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initVideoRecordManager();
        initTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVideoRecorder.resume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mProcessingDialog = null;
        mActivity = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSenseSDKManager != null) {
            mPreview.queueEvent(() -> mSenseSDKManager.onPause());
        }
        mVideoRecorder.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContentView = null;
        mFuFilterFragment = null;
        mSenseFilterFragment = null;
        mBeautyFragment = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContentView = null;
        mVideoRecorder.destroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String selectedFilepath = GetPathFromUri.getPath(mActivity, data.getData());
            Log.i(TAG, "Select file: " + selectedFilepath);
            if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                mVideoRecorder.setMusicFile(selectedFilepath);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_camera:
                onClickSwitchCamera(v);
                break;
            case R.id.close_window:
                onClickClose();
                break;
            case R.id.audio_mix_button:
                onClickAddMixAudio(v);
                break;
            case R.id.record_speed:
                onClickSpeed(v);
                break;
            case R.id.face_beauty:
                onClickBeauty(v);
                break;
            case R.id.count_down:
                // TODO 倒计时几秒后自动开始拍摄
                break;
            case R.id.btn_stickers:
                showFuFilterFragment();
                break;
            case R.id.btn_video_edit:
                mActivity.startActivity(new Intent(mActivity, VideoEditActivity.class));
                break;
            case R.id.btn_merge_clip:
                onMergeClick(v);
                break;
            case R.id.btn_delete_clip:
                onDeleteClip(false);
                break;
            case R.id.btn_shutter:
                break;
            case R.id.normal_speed_text:
            case R.id.slow_speed_text:
            case R.id.super_slow_speed_text:
            case R.id.fast_speed_text:
            case R.id.super_fast_speed_text:
                onSpeedClicked(v);
                break;
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_record;
    }

    @Override
    public void lazyLoad() {
    }

    @Override
    public void initView() {
        mPreview = mContentView.findViewById(R.id.preview);
        mTvCountDown = mContentView.findViewById(R.id.tv_countdown);
        mFocusIndicator = mContentView.findViewById(R.id.focus_indicator);
        mSpeedTextView = mContentView.findViewById(R.id.normal_speed_text);
        mSpeedPanel = mContentView.findViewById(R.id.record_speed_panel);
        mSpeedPanel.setVisibility(View.GONE);

        mContentView.findViewById(R.id.close_window).setOnClickListener(this);
        mContentView.findViewById(R.id.audio_mix_button).setOnClickListener(this);
        mContentView.findViewById(R.id.switch_camera).setOnClickListener(this);
        mContentView.findViewById(R.id.record_speed).setOnClickListener(this);
        mContentView.findViewById(R.id.face_beauty).setOnClickListener(this);
        mContentView.findViewById(R.id.count_down).setOnClickListener(this);// TODO
        mContentView.findViewById(R.id.count_down).setVisibility(View.GONE);
        mContentView.findViewById(R.id.normal_speed_text).setOnClickListener(this);
        mContentView.findViewById(R.id.fast_speed_text).setOnClickListener(this);
        mContentView.findViewById(R.id.super_fast_speed_text).setOnClickListener(this);
        mContentView.findViewById(R.id.slow_speed_text).setOnClickListener(this);
        mContentView.findViewById(R.id.super_slow_speed_text).setOnClickListener(this);

        String[] galleryIndicator = getResources().getStringArray(R.array.gallery_indicator);
        mBottomIndicator = mContentView.findViewById(R.id.bottom_indicator);
        mIndicatorText.addAll(Arrays.asList(galleryIndicator));
        mBottomIndicator.setIndicators(mIndicatorText);
        mBottomIndicator.addIndicatorListener(this);

        mBtnShutter = mContentView.findViewById(R.id.btn_shutter);
        mBtnShutter.setOnShutterListener(mShutterListener);
        mBtnShutter.setOnClickListener(this);
        mBtnShutter.setIsRecorder(true);
        mBtnShutter.setProgressMax(Constants.DEFAULT_MAX_RECORD_DURATION);

        mBtnDeleteClip = mContentView.findViewById(R.id.btn_delete_clip);
        mBtnDeleteClip.setOnClickListener(this);
        mBtnMergeClip = mContentView.findViewById(R.id.btn_merge_clip);
        mBtnMergeClip.setOnClickListener(this);
        mBtnStickers = mContentView.findViewById(R.id.btn_stickers);
        mBtnStickers.setOnClickListener(this);
        mBtnVideoEdit = mContentView.findViewById(R.id.btn_video_edit);
        mBtnVideoEdit.setOnClickListener(this);

        mGestureDetector = new GestureDetector(mActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mIsShowingEffect) {
                    hideEffectView();
                } else if (mIsShowingBeauty) {
                    hideBeautyView();
                } else {
                    mFocusIndicatorX = (int) e.getX() - mFocusIndicator.getWidth() / 2;
                    mFocusIndicatorY = (int) e.getY() - mFocusIndicator.getHeight() / 2;
                    mVideoRecorder.manualCameraFocus(mFocusIndicator.getWidth(), mFocusIndicator.getHeight(), (int) e.getX(), (int) e.getY());
                }
                return false;
            }
        });
        mPreview.setOnTouchListener((view, motionEvent) -> {
            mGestureDetector.onTouchEvent(motionEvent);
            return true;
        });
    }

    @Override
    protected void initProcessingDialog() {
        super.initProcessingDialog();
        mProcessingDialog.setOnCancelListener(dialog -> mVideoRecorder.cancelMerge());
    }

    /**
     * 显示动态贴纸页面
     */
    private void showFuFilterFragment() {
        mIsShowingEffect = true;
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (mFuFilterFragment == null) {
            mFuFilterFragment = EffectFilterFragment.getInstance(EffectFilterFragment.SHOW_TYPE_ALL, this);
            mFuFilterFragment.setFilterChangedListener(this);
            ft.add(R.id.fragment_container, mFuFilterFragment);
        } else {
            ft.show(mFuFilterFragment);
        }
        ft.commit();
        hideBottomLayout();
    }

    private void showSenseFilterFragment() {
        mIsShowingEffect = true;
        onEffectSelected(0, EffectEnum.getEffectsByEffectType(0).get(0));
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (mSenseFilterFragment == null) {
            mSenseFilterFragment = new SenseFilterFragment();
            mSenseFilterFragment.setFilterChangedListener(this);
            ft.add(R.id.fragment_container, mSenseFilterFragment);
        } else {
            ft.show(mSenseFilterFragment);
        }
        ft.commit();
        hideBottomLayout();
    }

    private void hideFilterFragment(Fragment fragment) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.hide(fragment);
        ft.commit();
    }

    /**
     * 隐藏动态贴纸页面
     */
    private void hideEffectView() {
        if (mIsShowingEffect) {
            mIsShowingEffect = false;
            if (mFuFilterFragment != null) {
                hideFilterFragment(mFuFilterFragment);
            }
            if (mSenseFilterFragment != null) {
                hideFilterFragment(mSenseFilterFragment);
            }
        }
        resetBottomLayout();
    }

    /**
     * 显示滤镜页面
     */
    private void showBeautyView() {
        if (mIsShowingEffect) {
            hideEffectView();
        }
        mIsShowingBeauty = true;
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (mBeautyFragment == null) {
            mBeautyFragment = new BeautyFragment();
            ft.add(R.id.fragment_container, mBeautyFragment);
        } else {
            ft.show(mBeautyFragment);
        }
        ft.commit();
        mBeautyFragment.setFilterChangedListener(this);
        hideBottomLayout();
    }

    /**
     * 隐藏滤镜页面
     */
    private void hideBeautyView() {
        if (mIsShowingBeauty) {
            mIsShowingBeauty = false;
            if (mBeautyFragment != null) {
                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.hide(mBeautyFragment);
                ft.commit();
            }
        }
        resetBottomLayout();
    }

    /**
     * 隐藏底部布局按钮
     */
    private void hideBottomLayout() {
        mBtnVideoEdit.setVisibility(View.GONE);
        mBtnStickers.setVisibility(View.GONE);
        mBottomIndicator.setVisibility(View.GONE);
        mBtnShutter.setVisibility(View.GONE);
        mTvCountDown.setVisibility(View.GONE);
        mBtnMergeClip.setVisibility(View.GONE);
        mBtnDeleteClip.setVisibility(View.GONE);
    }

    /**
     * 恢复底部布局
     */
    private void resetBottomLayout() {
        ViewGroup.LayoutParams layoutParams = mBtnShutter.getLayoutParams();
        layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, mActivity.getResources().getDisplayMetrics());
        layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, mActivity.getResources().getDisplayMetrics());
        boolean isRecording = mBtnShutter.getProgress() > 0;
        mBtnShutter.setLayoutParams(layoutParams);
        mBtnShutter.setVisibility(View.VISIBLE);
        mBtnStickers.setVisibility(View.VISIBLE);
        mBtnVideoEdit.setVisibility(isRecording ? View.GONE : View.VISIBLE);
        int isVisible = isRecording ? View.VISIBLE : View.INVISIBLE;
        mTvCountDown.setVisibility(isVisible);
        mBtnMergeClip.setVisibility(isVisible);
        mBtnDeleteClip.setVisibility(isVisible);
    }

    private void initTimer() {
        long countDownInterval = 50;
        mRecordTimer = new RecordTimer(countDownInterval) {
            @Override
            public void onTick(long progress) {
                if (mActivity == null) return;
                mActivity.runOnUiThread(() -> {
                    mBtnShutter.setProgress(progress, false);
                    mTvCountDown.setText(StringUtils.generateTime((long) mBtnShutter.getProgress()));
                });
            }
        };
    }

    private void initVideoRecordManager() {
        mFuSDKManager = new FuSDKManager(mActivity);
        mVideoRecorder = new UVideoRecord();
        mVideoRecorder.setRecordStateListener(this);

        UCameraParam cameraParam = new UCameraParam();
        UCameraParam.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        cameraParam.setCameraId(facingId);
        cameraParam.setCameraPreviewSizeRatio(UCameraParam.CAMERA_PREVIEW_SIZE_RATIO.RATIO_16_9);
        cameraParam.setCameraPreviewSizeLevel(UCameraParam.CAMERA_PREVIEW_SIZE_LEVEL.SIZE_720P);

        UMicrophoneParam microphoneParam = new UMicrophoneParam();
        UAudioEncodeParam audioEncodeParam = new UAudioEncodeParam();

        mVideoEncodeParam = new UVideoEncodeParam(getContext());
        mVideoEncodeParam.setEncodeSizeLevel(UVideoEncodeParam.VIDEO_ENCODE_SIZE_LEVEL.SIZE_720P_16_9);
        mVideoEncodeParam.setEncodeBitrate(Constants.DEFAULT_ENCODING_BITRATE);

        mRecordParam = new URecordParam();
        mRecordParam.setMaxRecordDuration(Constants.DEFAULT_MAX_RECORD_DURATION);
        mRecordParam.setClipCacheDir(Constants.VIDEO_STORAGE_DIR);
        mRecordParam.setRecordFilePath(Constants.RECORD_FILE_PATH);

        mVideoRecorder.init(mPreview, cameraParam, microphoneParam, mVideoEncodeParam, audioEncodeParam, mRecordParam);
        mVideoRecorder.setVideoSpeed(mRecordSpeed);
        mVideoRecorder.setVideoFrameListener(this);
        mVideoRecorder.setCameraFocusListener(this);
    }

    private void initWatermarkParam() {
        mWatermarkParam = new UWatermarkParam();
        mWatermarkParam.setResourceId(R.drawable.movieous);
        mWatermarkParam.setSize(0.06f, 0.03f);
        mWatermarkParam.setPosition(0.08f, 0.04f);
        mWatermarkParam.setAlpha(128);
    }

    private UCameraParam.CAMERA_FACING_ID chooseCameraFacingId() {
        if (UCameraParam.hasCameraFacing(UCameraParam.CAMERA_FACING_ID.THIRD)) {
            return UCameraParam.CAMERA_FACING_ID.THIRD;
        } else if (UCameraParam.hasCameraFacing(UCameraParam.CAMERA_FACING_ID.FRONT)) {
            return UCameraParam.CAMERA_FACING_ID.FRONT;
        } else {
            return UCameraParam.CAMERA_FACING_ID.BACK;
        }
    }

    private void onStartRecord() {
        mActivity.runOnUiThread(() -> {
            mBtnMergeClip.setVisibility(View.GONE);
            mBtnDeleteClip.setVisibility(View.GONE);
            mTvCountDown.setVisibility(View.VISIBLE);
            mBtnShutter.addSplitView();
            mRecordTimer.start();
        });
    }

    private void onStopRecord() {
        mActivity.runOnUiThread(() -> {
            mRecordTimer.cancel();
            mBtnMergeClip.setVisibility(View.VISIBLE);
            mBtnDeleteClip.setVisibility(View.VISIBLE);
            mBtnVideoEdit.setVisibility(View.GONE);
        });
    }

    /**
     * 删除已录制的视频
     *
     * @param clearAll
     */
    private void onDeleteClip(boolean clearAll) {
        // 处于删除模式，则删除文件
        if (mBtnShutter.isDeleteMode()) {
            // 删除视频，判断是否清除所有
            if (clearAll) {
                // 清除所有分割线
                mBtnShutter.cleanSplitView();
                mVideoRecorder.removeAllClips();
            } else {
                // 删除分割线
                mBtnShutter.deleteSplitView();
                mVideoRecorder.removeLastClip();
            }
        } else { // 没有进入删除模式则进入删除模式
            mBtnShutter.setDeleteMode(true);
        }
    }

    private void showChooseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getString(R.string.if_edit_video));
        builder.setPositiveButton(getString(R.string.dlg_yes), (dialog, which) -> {
            mIsEditVideo = true;
            mVideoRecorder.mergeClips(this);
        });
        builder.setNegativeButton(getString(R.string.dlg_no), (dialog, which) -> {
            mIsEditVideo = false;
            mVideoRecorder.mergeClips(this);
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void onMergeClick(View v) {
        showProcessingDialog();
        showChooseDialog();
    }

    private void onClickClose() {
        mActivity.onBackPressed();
    }

    private void onClickSwitchCamera(View v) {
        mVideoRecorder.switchCamera();
        mFocusIndicator.focusCancel();
    }

    private void onClickAddMixAudio(View v) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
        }
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_music_file_tip)), 0);
    }

    private void onClickSpeed(View v) {
        mSpeedPanel.setVisibility(mSpeedPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void onClickBeauty(View view) {
        if (mIsShowingBeauty) {
            hideBeautyView();
        } else {
            showBeautyView();
        }
    }

    private void onSpeedClicked(View view) {
        Log.d(TAG, "onSpeedClicked");
        if (!mVideoEncodeParam.isFixFrameRateEnabled() || !mRecordParam.isChangeRecordSpeedEnabled()) {
            if (mBtnShutter.isOpen()) {
                showToast(mActivity, getString(R.string.change_speed_error_tip));
                return;
            }
        }

        if (mSpeedTextView != null) {
            mSpeedTextView.setTextColor(getResources().getColor(R.color.speedTextNormal));
        }

        TextView textView = (TextView) view;
        textView.setTextColor(getResources().getColor(R.color.app_default_color));
        mSpeedTextView = textView;

        switch (view.getId()) {
            case R.id.super_slow_speed_text:
                mRecordSpeed = Constants.VIDEO_SPEED_SUPER_SLOW;
                break;
            case R.id.slow_speed_text:
                mRecordSpeed = Constants.VIDEO_SPEED_SLOW;
                break;
            case R.id.normal_speed_text:
                mRecordSpeed = Constants.VIDEO_SPEED_NORMAL;
                break;
            case R.id.fast_speed_text:
                mRecordSpeed = Constants.VIDEO_SPEED_FAST;
                break;
            case R.id.super_fast_speed_text:
                mRecordSpeed = Constants.VIDEO_SPEED_SUPER_FAST;
                break;
        }

        mVideoRecorder.setVideoSpeed(mRecordSpeed);
        mRecordParam.setMaxRecordDuration(Constants.DEFAULT_MAX_RECORD_DURATION);
    }

    // shutter state listener
    private ShutterButton.OnShutterListener mShutterListener = new ShutterButton.OnShutterListener() {

        @Override
        public void onStartRecord() {
            Log.i(TAG, "onStartRecord");
            mVideoRecorder.start();
        }

        @Override
        public void onStopRecord() {
            Log.i(TAG, "onStopRecord");
            mVideoRecorder.stop();
        }

        @Override
        public void onProgressOver() {
            Log.i(TAG, "onProgressOver");
            mVideoRecorder.stop();
        }
    };

    // indicator
    @Override
    public void onIndicatorChanged(int currentIndex) {
    }

    // record state
    @Override
    public void onReady() {
        showToast(mActivity, getString(R.string.start_record_tip));
    }

    @Override
    public void onError(int i) {
    }

    @Override
    public void onRecordStarted() {
        onStartRecord();
    }

    @Override
    public void onRecordStopped() {
        onStopRecord();
    }

    @Override
    public void onClipCountChanged(long totalDuration, int clipCount) {
        Log.d(TAG, "onClipIncreased: totalDuration = " + totalDuration + ", clip count = " + clipCount);
        mActivity.runOnUiThread(() -> {
            mBtnShutter.setProgress(totalDuration, true);
            mTvCountDown.setText(StringUtils.generateTime(totalDuration));
            if (clipCount <= 0) {
                mTvCountDown.setText("");
                mBtnDeleteClip.setVisibility(View.GONE);
                mBtnMergeClip.setVisibility(View.GONE);
                mBtnVideoEdit.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onRecordFinish() {
    }

    // manual focus
    @Override
    public void onManualFocusStart(boolean result) {
        if (result) {
            Log.i(TAG, "manual focus begin success");
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFocusIndicator.getLayoutParams();
            lp.leftMargin = mFocusIndicatorX;
            lp.topMargin = mFocusIndicatorY;
            mFocusIndicator.setLayoutParams(lp);
            mFocusIndicator.focus();
        } else {
            mFocusIndicator.focusCancel();
            Log.i(TAG, "manual focus not supported");
        }
    }

    @Override
    public void onManualFocusStop(boolean result) {
        Log.i(TAG, "manual focus end result: " + result);
        if (result) {
            mFocusIndicator.focusSuccess();
        } else {
            mFocusIndicator.focusFail();
        }
    }

    @Override
    public void onManualFocusCancel() {
        Log.i(TAG, "manual focus canceled");
        mFocusIndicator.focusCancel();
    }

    @Override
    public void onAutoFocusStart() {
        Log.i(TAG, "auto focus start");
    }

    @Override
    public void onAutoFocusStop() {
        Log.i(TAG, "auto focus stop");
    }

    // USaveListener
    @Override
    public void onSaveFileSuccess(String destFile, UVideoEncodeParam videoEncodeParam) {
        Log.i(TAG, "merge clips success filePath: " + destFile);
        mActivity.runOnUiThread(() -> {
            mProcessingDialog.dismiss();
            if (mIsEditVideo) {
                VideoEditActivity.start(mActivity, destFile);
            } else {
                PlaybackActivity.start(mActivity, destFile);
            }
        });
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(width * height * 4);
        }
        mVideoRecorder.setRGBABuffer(mRGBABuffer);
        mVideoRecorder.setVerticalFlip(true);
        super.onSurfaceChanged(width, height);
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
        return super.onDrawFrame(texId, texWidth, texHeight, timestampNs, transformMatrix);
    }

}
