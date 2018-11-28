package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.faceunity.FURenderer;
import com.faceunity.entity.Effect;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.filter.UVideoFrameListener;
import com.movieous.filter.UWatermarkParam;
import com.movieous.media.Constants;
import com.movieous.media.R;
import com.movieous.media.api.fusdk.FuSDKManager;
import com.movieous.media.api.sensesdk.SenseSDKManager;
import com.movieous.media.ui.activity.PlaybackActivity;
import com.movieous.media.ui.adapter.GridViewAdapter;
import com.movieous.media.utils.AppUtils;
import com.movieous.media.utils.GetPathFromUri;
import com.movieous.media.view.TextIcon;
import com.movieous.media.view.TransitionEditView;
import com.movieous.media.view.transition.TransitionBase;
import com.movieous.media.view.transition.TransitionTail;
import com.movieous.media.view.transition.TransitionTitle;
import com.movieous.shortvideo.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频编辑页面
 */
public class VideoEditFragment extends PreviewFragment implements View.OnClickListener,
        EditFunctionFragment.EditFunctionListener, EffectFilterFragment.EffectFragmentListener, UVideoPlayListener {
    private static final String TAG = "VideoEditFragment";
    private static final int REQUEST_CODE_PICK_AUDIO_MIX_FILE = 0;

    private UVideoEdit mVideoEditor;
    private UWatermarkParam mWatermarkParam;
    private VideoEditorState mEditorState = VideoEditorState.Idle;
    private String mVideoPath;

    private View mContentView;
    private Fragment mDisplayFragment;
    private EditFunctionFragment mEditFunctionFragment;
    private EffectFilterFragment mFuStickFragment;
    private EffectFilterFragment mFuEffectFragment;
    private SenseFilterFragment mSenseFilterFragment;
    private ImageButton mVideoDivide;
    private ImageButton mTextEffect;
    private ImageButton mStickerEffect;
    private ImageButton mAddMusic;
    private FrameLayout mContainerLayout;
    private LinearLayout mLayoutTextContainer;
    private TransitionBase mVideoTitle;
    private TransitionBase mVideoTail;
    private TransitionEditView mTransitionEditView;
    private ImageButton mPausePlaybackButton;
    private int mCurrentClickId;
    private boolean mIsEffectShowing = false;
    private boolean mHaveVideoTitle;
    private boolean mHaveVideoTail;

    public synchronized static VideoEditFragment getInstance(String videoPath) {
        VideoEditFragment fragment = new VideoEditFragment();
        fragment.mVideoPath = videoPath;
        return fragment;
    }

    public void onBackPressed() {
        new AlertDialog.Builder(mActivity)
                .setMessage(R.string.back_pressed_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> mActivity.finish())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private void startPlayback() {
        if (mEditorState == VideoEditorState.Idle) {
            mVideoEditor.setVideoFrameListener(this);
            mVideoEditor.startPlay();
            mEditorState = VideoEditorState.Playing;
        } else if (mEditorState == VideoEditorState.Paused) {
            mVideoEditor.resumePlay();
            mEditorState = VideoEditorState.Playing;
        }
        mPausePlaybackButton.setImageResource(R.drawable.btn_pause);
    }

    private void stopPlayback() {
        mVideoEditor.stopPlay();
        mEditorState = VideoEditorState.Idle;
        mPausePlaybackButton.setImageResource(R.drawable.btn_play);
    }

    private void pausePlayback() {
        mVideoEditor.pausePlay();
        mEditorState = VideoEditorState.Paused;
        mPausePlaybackButton.setImageResource(R.drawable.btn_play);
    }

    private boolean isPlaying() {
        return mEditorState == VideoEditorState.Playing;
    }

    private void showFragment(Fragment fragment, Class<?> cls) {
        hideFragment(mDisplayFragment);
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (fragment == null) {
            fragment = getFragmentInstance(cls);
            ft.add(R.id.fragment_container, fragment);
        } else {
            ft.show(fragment);
        }
        ft.commit();
        mDisplayFragment = fragment;
    }

    private void hideFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.hide(fragment);
            ft.commit();
        }
        if (mLayoutTextContainer != null && mLayoutTextContainer.getVisibility() == View.VISIBLE) {
            mContainerLayout.removeView(mLayoutTextContainer);
            if (mVideoTail != null) {
                mVideoTail.setVisibility(View.GONE);
            }
            if (mVideoTitle != null) {
                mVideoTitle.setVisibility(View.GONE);
            }
            if (mTransitionEditView != null) {
                mTransitionEditView.setVisibility(View.GONE);
            }
            mPreview.setVisibility(View.VISIBLE);
            startPlayback();
        }
    }

    private Fragment getFragmentInstance(Class<?> cls) {
        if (cls == EditFunctionFragment.class) {
            mEditFunctionFragment = EditFunctionFragment.getInstance(mVideoEditor, mVideoPath, this, this);
            return mEditFunctionFragment;
        } else if (cls == EffectFilterFragment.class) {
            mFuStickFragment = EffectFilterFragment.getInstance(EffectFilterFragment.SHOW_TYPE_STICK, this);
            mFuStickFragment.setFilterChangedListener(this);
            return mFuStickFragment;
        } else if (cls == SenseFilterFragment.class) {
            mSenseFilterFragment = new SenseFilterFragment();
            mSenseFilterFragment.setFilterChangedListener(this);
            return mSenseFilterFragment;
        } else {
            return new Fragment();
        }
    }

    private void initWatermarkParam() {
        mWatermarkParam = new UWatermarkParam();
        mWatermarkParam.setResourceId(R.drawable.movieous);
        mWatermarkParam.setSize(0.06f, 0.03f);
        mWatermarkParam.setPosition(0.08f, 0.04f);
        mWatermarkParam.setAlpha(128);
    }

    private void initVideoEditor() {
        Log.i(TAG, "media file: " + mVideoPath);
        UVideoEditParam videoEditParam = new UVideoEditParam();
        videoEditParam.setMediaFilepath(mVideoPath);
        videoEditParam.setSaveFilepath(Constants.EDIT_FILE_PATH);
        mVideoEditor = new UVideoEdit(mPreview, videoEditParam);
        mVideoEditor.setVideoFrameListener(this);
        mVideoEditor.setVideoPlayListener(this);
        mVideoEditor.setVideoSaveListener(this);
        onClick(mContentView.findViewById(R.id.divide_video));
    }

    private void onClickTogglePlayback() {
        if (isPlaying()) {
            pausePlayback();
        } else {
            startPlayback();
        }
    }

    @Override
    protected void initProcessingDialog() {
        super.initProcessingDialog();
        mProcessingDialog.setOnCancelListener(dialog -> mVideoEditor.cancelSave());
    }

    private void setTitleColor(int viewId) {
        mVideoDivide.setSelected(mVideoDivide.getId() == viewId);
        mTextEffect.setSelected(mTextEffect.getId() == viewId);
        mStickerEffect.setSelected(mStickerEffect.getId() == viewId);
        mAddMusic.setSelected(mAddMusic.getId() == viewId);
    }

    private void updateSaveMusicFilterTime(FURenderer fuFilterEngin, Effect effect, long time) {
        if (effect != null && effect.effectType() == Effect.EFFECT_TYPE_MUSIC_FILTER) {
            fuFilterEngin.onMusicFilterTime(time);
        }
    }

    private void onSaveVideoFile() {
        stopPlayback();
        showProcessingDialog();

        UVideoFrameListener listener = new UVideoFrameListener() {
            @Override
            public void onSurfaceCreated() {
                if (mIsSenseSDK && mSenseSDKManager == null) {
                    mSenseSDKManager = new SenseSDKManager(mActivity);
                    mSenseSDKManager.onSurfaceCreated();
                }
                FURenderer fuFilterEngine = mFuSDKManager.getSaveFilterEngine();
                fuFilterEngine.loadItems();
                if (mCurrentEffect != null) {
                    fuFilterEngine.onEffectSelected(mCurrentEffect);
                }
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                if (mIsSenseSDK) {
                    if (mRGBABuffer == null) {
                        mRGBABuffer = ByteBuffer.allocate(width * height * 4);
                    }
                    mVideoEditor.setRGBABuffer(mRGBABuffer);
                    mVideoEditor.setVerticalFlip(true);
                    mSenseSDKManager.onSurfaceChanged(width, height);
                    mSenseSDKManager.enableSticker(true);
                    mSenseSDKManager.changeSticker(mCurrentSticker);
                }
            }

            @Override
            public void onSurfaceDestroy() {
                if (mSenseSDKManager != null) {
                    mSenseSDKManager.onDestroy();
                    mSenseSDKManager = null;
                }
                if (mFuSDKManager != null) {
                    mFuSDKManager.destroySaveFilterEngine();
                }
            }

            @Override
            public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
                int outTexId = texId;
                if (mFuSDKManager != null) {
                    FURenderer fuFilterEngine = mFuSDKManager.getSaveFilterEngine();
                    updateSaveMusicFilterTime(fuFilterEngine, mCurrentEffect, timestampNs / 1000 / 1000);
                    outTexId = (mCurrentEffect != null) ? fuFilterEngine.onDrawFrame(texId, texWidth, texHeight) : texId;
                }
                if (mIsSenseSDK) {
                    outTexId = mSenseSDKManager.onDrawFrame(outTexId, texWidth, texHeight, mRGBABuffer);
                }
                return outTexId;
            }
        };
        mVideoEditor.save(listener);
    }

    private void onSaveTransition() {
        showProcessingDialog();
        USaveFileListener listener = new USaveFileListener() {
            @Override
            public void onSaveFileSuccess(String destFile, UVideoEncodeParam videoEncodeParam) {
                Log.i(TAG, "save success: " + destFile);
                if ((int) mLayoutTextContainer.getTag() == 0) {
                    mHaveVideoTitle = true;
                } else {
                    mHaveVideoTail = true;
                }
                mActivity.runOnUiThread(() -> mProcessingDialog.dismiss());
            }

            @Override
            public void onSaveFileFailed(int errorCode) {
            }

            @Override
            public void onSaveFileCanceled() {
            }

            @Override
            public void onSaveFileProgress(final float percentage) {
                mActivity.runOnUiThread(() -> mProcessingDialog.setProgress((int) (100 * percentage)));
            }
        };
        if ((int) mLayoutTextContainer.getTag() == 0) {
            mVideoTitle.save(Constants.TITLE_FILE_PATH, listener);
        } else {
            mVideoTail.save(Constants.TAIL_FILE_PATH, listener);
        }
    }

    private void mergeTitleTail(boolean haveTitle, boolean haveTail, UVideoEncodeParam encodeParam) {
        if (!haveTitle && !haveTail) {
            return;
        }
        List<String> videos = new ArrayList<>();
        if (haveTitle) {
            videos.add(Constants.TITLE_FILE_PATH);
        }
        videos.add(Constants.EDIT_FILE_PATH);
        if (haveTail) {
            videos.add(Constants.TAIL_FILE_PATH);
        }
        UMediaMerge mediaMerge = new UMediaMerge(mActivity);
        mediaMerge.mergeVideos(videos, Constants.MERGE_FILE_PATH, encodeParam, this);
    }

    private void showTextEffect() {
        hideFragment(mDisplayFragment);
        pausePlayback();
        mPreview.setVisibility(View.INVISIBLE);
        if (mLayoutTextContainer == null) {
            initTextEffectView();
            initTransitions();
        } else {
            if ((int) mLayoutTextContainer.getTag() == 0) {
                mVideoTitle.setVisibility(View.VISIBLE);
            } else {
                mVideoTail.setVisibility(View.VISIBLE);
            }
        }
        mContainerLayout.addView(mLayoutTextContainer);
    }

    private void initTextEffectView() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        mLayoutTextContainer = (LinearLayout) inflater.inflate(R.layout.fragment_function_list, null);
        mLayoutTextContainer.findViewById(R.id.layout_title).setVisibility(View.GONE);
        mTransitionEditView = mContentView.findViewById(R.id.transition_edit_view);
        mLayoutTextContainer.setTag(0);
        GridView gridView = mLayoutTextContainer.findViewById(R.id.grid_button);
        gridView.setNumColumns(2);
        ArrayList<TextIcon> functionList = new ArrayList<>();
        functionList.add(new TextIcon(R.drawable.video_title, getString(R.string.video_file_title)));
        functionList.add(new TextIcon(R.drawable.video_tail, getString(R.string.video_file_tail)));
        GridViewAdapter adapter = new GridViewAdapter(functionList, R.layout.item_grid) {
            @Override
            public void bindView(ViewHolder holder, Object obj) {
                holder.setImageResource(R.id.icon, ((TextIcon) obj).getId());
                holder.setText(R.id.name, ((TextIcon) obj).getName());
            }
        };
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener((parent, v, position, id) -> {
            mTransitionEditView.setVisibility(View.GONE);
            mLayoutTextContainer.setTag(position);
            mVideoTitle.setVisibility((position == 0) ? View.VISIBLE : View.INVISIBLE);
            mVideoTail.setVisibility((position == 1) ? View.VISIBLE : View.INVISIBLE);
            mTransitionEditView.setTransition(position == 0 ? mVideoTitle : mVideoTail);
        });
    }

    private void initTransitions() {
        UVideoEncodeParam setting = new UVideoEncodeParam(mActivity);
        setting.setEncodeSizeLevel(UVideoEncodeParam.VIDEO_ENCODE_SIZE_LEVEL.SIZE_720P_16_9);
        ViewGroup viewGroupTitle = mContentView.findViewById(R.id.transition_container_title);
        TransitionBase.TransitionListener listener = () -> mTransitionEditView.setVisibility(View.VISIBLE);
        viewGroupTitle.post(() -> {
            mVideoTitle = new TransitionTitle(viewGroupTitle, setting);
            mVideoTitle.setTransitionListener(listener);
            mVideoTitle.setVisibility(View.VISIBLE);
            mTransitionEditView.setTransition(mVideoTitle);
        });
        final ViewGroup viewGroupTail = mContentView.findViewById(R.id.transition_container_tail);
        viewGroupTail.post(() -> {
            mVideoTail = new TransitionTail(viewGroupTail, setting);
            mVideoTail.setTransitionListener(listener);
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(getLayoutId(), container, false);
        return mContentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initVideoEditor();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(mVideoPath)) {
            startPlayback();
        }
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
        stopPlayback();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContentView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContentView = null;
        mProcessingDialog = null;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_video_edit;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String selectedFilepath = GetPathFromUri.getPath(mActivity, data.getData());
            Log.i(TAG, "Select file: " + selectedFilepath);
            if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                mVideoEditor.setAudioMixFile(selectedFilepath);
            }
        }
    }

    @Override
    public void initView() {
        Log.d(TAG, "initView");
        mFuSDKManager = new FuSDKManager(mActivity);
        mPausePlaybackButton = mContentView.findViewById(R.id.pause_playback);
        mContainerLayout = mContentView.findViewById(R.id.fragment_container);
        mPausePlaybackButton.setOnClickListener(this);
        mPreview = mContentView.findViewById(R.id.preview);
        mPreview.setOnClickListener(v -> {
            mPausePlaybackButton.setVisibility(mPausePlaybackButton.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
        });
        mVideoDivide = mContentView.findViewById(R.id.divide_video);
        mVideoDivide.setOnClickListener(this);
        mTextEffect = mContentView.findViewById(R.id.add_text);
        mTextEffect.setOnClickListener(this);
        mStickerEffect = mContentView.findViewById(R.id.face_stick);
        mStickerEffect.setOnClickListener(this);
        mAddMusic = mContentView.findViewById(R.id.add_music);
        mAddMusic.setOnClickListener(this);
        mContentView.findViewById(R.id.back_button).setOnClickListener(this);
        mContentView.findViewById(R.id.next_button).setOnClickListener(this);
        setTitleColor(mVideoDivide.getId());
    }

    @Override
    public void lazyLoad() {
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pause_playback:
                onClickTogglePlayback();
                break;
            case R.id.divide_video:
                if (mIsEffectShowing) {
                    onEffectViewShow();
                } else {
                    showFragment(mEditFunctionFragment, EditFunctionFragment.class);
                }
                setTitleColor(R.id.divide_video);
                break;
            case R.id.add_text:
                showTextEffect();
                setTitleColor(R.id.add_text);
                break;
            case R.id.face_stick:
                showFragment(mFuStickFragment, EffectFilterFragment.class);
                onEffectSelected(0, mFuStickFragment.getCurrentEffect());
                setTitleColor(R.id.face_stick);
                break;
            case R.id.add_music:
                setTitleColor(R.id.add_music);
                startActivityForResult(Intent.createChooser(AppUtils.Companion.getMediaIntent(false), getString(R.string.select_music_file_tip)), REQUEST_CODE_PICK_AUDIO_MIX_FILE);
                break;
            case R.id.back_button:
                mActivity.finish();
                break;
            case R.id.next_button:
                if (mCurrentClickId == R.id.add_text) { // 生成片头片尾
                    onSaveTransition();
                    return;
                } else {
                    onSaveVideoFile();
                }
                break;
            default:
                break;

        }
        mCurrentClickId = v.getId();
    }

    // EditFunctionListener
    @Override
    public void onStartPlay() {
        startPlayback();
    }

    @Override
    public void onPausePlay() {
        pausePlayback();
    }

    @Override
    public void onEffectViewShow() {
        mIsEffectShowing = true;
        hideFragment(mDisplayFragment);
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (mFuEffectFragment == null) {
            mFuEffectFragment = EffectFilterFragment.getInstance(EffectFilterFragment.SHOW_TYPE_EFFECT, this);
            mFuEffectFragment.setFilterChangedListener(this);
            ft.add(R.id.fragment_container, mFuEffectFragment);
        } else {
            ft.show(mFuEffectFragment);
        }
        ft.commit();
        onEffectSelected(0, mFuEffectFragment.getCurrentEffect());
        mDisplayFragment = mFuEffectFragment;
        mFuEffectFragment.setEffectFragmentListener(this);
    }

    @Override
    public void onBackButtonPressed() {
        mIsEffectShowing = false;
        mContentView.findViewById(R.id.divide_video).performClick();
    }

    // USaveListener
    @Override
    public void onSaveFileSuccess(String destFile, UVideoEncodeParam videoEncodeParam) {
        if (mHaveVideoTitle || mHaveVideoTail) {
            mProcessingDialog.setMessage(getString(R.string.merge_title_tail_tip));
            mergeTitleTail(mHaveVideoTitle, mHaveVideoTail, videoEncodeParam);
            mHaveVideoTitle = false;
            mHaveVideoTail = false;
        } else {
            Log.i(TAG, "save edit success filePath: " + destFile);
            mProcessingDialog.dismiss();
            PlaybackActivity.start(mActivity, destFile);
        }
    }

    // UVideoPlayListener
    @Override
    public void onPositionChanged(int position) {
        if (mEditFunctionFragment != null && isPlaying() && mDisplayFragment instanceof EditFunctionFragment) {
            mEditFunctionFragment.onPlayPositionChanged(position);
        }
    }

    @Override
    public void onCompletion() {
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(width * height * 4);
        }
        mVideoEditor.setRGBABuffer(mRGBABuffer);
        mVideoEditor.setVerticalFlip(true);
        if (mIsSenseSDK) {
            onChangeSticker(mCurrentSticker);
        }
        super.onSurfaceChanged(width, height);
    }

    private enum VideoEditorState {
        Idle,
        Playing,
        Paused,
    }

}
