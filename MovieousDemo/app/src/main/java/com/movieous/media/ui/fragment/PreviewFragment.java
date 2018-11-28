package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import com.faceunity.FURenderer;
import com.faceunity.entity.Effect;
import com.faceunity.entity.Filter;
import com.movieous.capture.UCameraPreviewListener;
import com.movieous.codec.UVideoEncodeParam;
import com.movieous.filter.UVideoFrameListener;
import com.movieous.media.R;
import com.movieous.media.api.fusdk.FuSDKManager;
import com.movieous.media.api.sensesdk.SenseSDKManager;
import com.movieous.media.api.sensesdk.utils.FileUtils;
import com.movieous.media.base.BaseFragment;
import com.movieous.media.mvp.contract.FuFilterChangedListener;
import com.movieous.media.mvp.contract.SenseFilterChangedListener;
import com.movieous.media.mvp.model.BeautyEnum;
import com.movieous.media.mvp.model.EffectEnum;
import com.movieous.media.view.SaveProgressDialog;
import com.movieous.shortvideo.USaveFileListener;
import com.orhanobut.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static com.movieous.media.ExtensionsKt.showToast;

public class PreviewFragment extends BaseFragment implements UVideoFrameListener, USaveFileListener, UCameraPreviewListener,
        FuFilterChangedListener, SenseFilterChangedListener {
    protected Activity mActivity;
    protected GLSurfaceView mPreview;
    protected SaveProgressDialog mProcessingDialog;
    protected FuSDKManager mFuSDKManager;
    protected SenseSDKManager mSenseSDKManager;
    protected Effect mCurrentEffect;
    protected ByteBuffer mRGBABuffer;
    protected String mCurrentSticker;
    protected static boolean mIsSenseSDK = false;

    protected void initProcessingDialog() {
        mProcessingDialog = new SaveProgressDialog(mActivity);
    }

    protected void showProcessingDialog() {
        if (mProcessingDialog == null) {
            initProcessingDialog();
        }
        mProcessingDialog.show();
    }

    @Override
    public int getLayoutId() {
        return 0;
    }

    @Override
    public void initView() {
        //copy model file to sdcard
        FileUtils.copyModelFiles(mActivity);
    }

    @Override
    public void lazyLoad() {
    }

    // UVideoFilterListener
    @Override
    public void onSurfaceCreated() {
        mFuSDKManager.getPreviewFilterEngine().loadItems();
        onEffectSelected(0, mCurrentEffect);
        if (mSenseSDKManager == null) {
            mSenseSDKManager = new SenseSDKManager(mActivity);
        }
        mSenseSDKManager.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mSenseSDKManager.onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceDestroy() {
        mSenseSDKManager.onDestroy();
        mSenseSDKManager = null;
        if (mFuSDKManager.getPreviewFilterEngine() != null) {
            mFuSDKManager.getPreviewFilterEngine().destroyItems();
        }
        synchronized (mActivity) {
            mFuSDKManager.destroyPreviewFilterEngine();
        }
        if (mRGBABuffer != null) {
            mRGBABuffer.clear();
            mRGBABuffer = null;
        }
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
        int outTexId = texId;
        if (mFuSDKManager.getPreviewFilterEngine() == null) {
            mFuSDKManager.getPreviewFilterEngine().onSurfaceCreated();
            onEffectSelected(0, mCurrentEffect);
        }
        synchronized (mActivity) {
            if (mFuSDKManager.getPreviewFilterEngine() != null) {
                outTexId = mFuSDKManager.getPreviewFilterEngine().onDrawFrame(texId, texWidth, texHeight);
            }
        }
        if (mIsSenseSDK) {
            outTexId = mSenseSDKManager.onDrawFrame(outTexId, texWidth, texHeight, mRGBABuffer);
        }
        return outTexId;
    }

    // FuFilterChangedListener
    @Override
    public void onEffectSelected(int position, @NotNull Effect effect) {
        if (effect == null) {
            Logger.w("effect is null!");
            return;
        }
        mIsSenseSDK = false;
        mCurrentEffect = effect;
        mFuSDKManager.getPreviewFilterEngine().onEffectSelected(effect);
        if (effect.description() > 0) {
            mActivity.runOnUiThread(() -> showToast(mActivity, getString(effect.description())));
        }
    }

    @Override
    public void onMusicFilterTime(long time) {
        mFuSDKManager.getPreviewFilterEngine().onMusicFilterTime(time);
    }

    @Override
    public void onBeautyValueChanged(float value, @NotNull BeautyEnum beautyType) {
        FURenderer filterEngine = mFuSDKManager.getPreviewFilterEngine();
        switch (beautyType) {
            case FACE_BLUR:
                filterEngine.onBlurLevelSelected(value);
                break;
            case EYE_ENLARGE:
                filterEngine.onEyeEnlargeSelected(value);
                break;
            case CHEEK_THINNING:
                filterEngine.onCheekThinningSelected(value);
                break;
        }
    }

    @Override
    public void onFilterNameSelected(@NotNull Filter filterName) {
        mFuSDKManager.getPreviewFilterEngine().onFilterNameSelected(filterName);
    }

    // USaveFileListener
    @Override
    public void onSaveFileSuccess(String destFile, UVideoEncodeParam videoEncodeParam) {
    }

    @Override
    public void onSaveFileFailed(int errorCode) {
        Logger.e("save edit failed errorCode:" + errorCode);
        mActivity.runOnUiThread(() -> {
            mProcessingDialog.dismiss();
            showToast(mActivity, getString(R.string.save_file_failed_tip) + errorCode);
        });
    }

    @Override
    public void onSaveFileCanceled() {
        mProcessingDialog.dismiss();
    }

    @Override
    public void onSaveFileProgress(float percentage) {
        mActivity.runOnUiThread(() -> mProcessingDialog.setProgress((int) (100 * percentage)));
    }

    @Override
    public boolean onPreviewFrame(byte[] data, int width, int height, int rotation, int format, long timestampNs) {
        return mSenseSDKManager != null ? mSenseSDKManager.onPreviewFrame(data, width, height, rotation, format, timestampNs) : false;
    }

    @Override
    public void onChangeSticker(@NotNull String path) {
        mCurrentSticker = path;
        onEffectSelected(0, EffectEnum.getEffectsByEffectType(0).get(0));
        mIsSenseSDK = true;
        mPreview.queueEvent(() -> {
            if (mSenseSDKManager != null) {
                mSenseSDKManager.enableSticker(true);
                mSenseSDKManager.changeSticker(mCurrentSticker);
            }
        });
    }

    @Override
    public void onRemoveAllStickers() {
        if (mSenseSDKManager != null) {
            mSenseSDKManager.enableSticker(false);
            mSenseSDKManager.removeAllStickers();
        }
    }
}
