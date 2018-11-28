package com.movieous.media.api.sensesdk;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import com.movieous.media.api.sensesdk.glutils.GlUtil;
import com.movieous.media.api.sensesdk.utils.FileUtils;
import com.sensetime.stmobile.*;
import com.sensetime.stmobile.model.STHumanAction;

import java.nio.ByteBuffer;

public class SenseSDKManager {
    private static final String TAG = "SenseSDKManager";

    private Context mContext;
    private Object mImageDataLock = new Object();
    private Object mHumanActionHandleLock = new Object();
    private STMobileStickerNative mStStickerNative = new STMobileStickerNative();
    private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
    private STMobileFaceAttributeNative mSTFaceAttributeNative = new STMobileFaceAttributeNative();
    private STMobileObjectTrackNative mSTMobileObjectTrackNative = new STMobileObjectTrackNative();
    private String mCurrentSticker;
    private long mDetectConfig;
    private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO;
    private int mRotation;
    private int[] mTextureOutId;
    private byte[] mImageData;
    private byte[] mNv21ImageData;
    private boolean mNeedSticker = false;
    private boolean mIsCreateHumanActionHandleSucceeded;

    public SenseSDKManager(Context context) {
        mContext = context.getApplicationContext();
        initHumanAction();
    }

    public void enableSticker(boolean needSticker) {
        mNeedSticker = needSticker;
        //reset humanAction config
        setHumanActionDetectConfig(mNeedSticker, mStStickerNative.getTriggerAction());
    }

    public void changeSticker(String sticker) {
        mCurrentSticker = sticker;
        int result = mStStickerNative.changeSticker(mCurrentSticker);
        Log.i(TAG, "change sticker result: " + result + ", path = " + sticker);
        setHumanActionDetectConfig(mNeedSticker, mStStickerNative.getTriggerAction());
    }

    public void removeAllStickers() {
        mStStickerNative.removeAllStickers();
        setHumanActionDetectConfig(mNeedSticker, mStStickerNative.getTriggerAction());
    }

    public boolean onPreviewFrame(byte[] data, int width, int height, int rotation, int format, long timestampNs) {
        if (mImageData == null || mImageData.length != width * height * 3 / 2) {
            mImageData = new byte[width * height * 3 / 2];
            mRotation = rotation;
        }
        synchronized (mImageDataLock) {
            System.arraycopy(data, 0, mImageData, 0, data.length);
        }
        return true;
    }

    public void onPause() {
        Log.i(TAG, "onPause");
        mSTHumanActionNative.reset();
        mStStickerNative.removeAvatarModel();
        mStStickerNative.destroyInstance();
        mNv21ImageData = null;
        if (mTextureOutId != null) {
            GLES20.glDeleteTextures(1, mTextureOutId, 0);
            mTextureOutId = null;
        }
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view创建的时候调用
     */
    public void onSurfaceCreated() {
        Log.i(TAG, "onSurfaceCreated");
        //初始化GL相关的句柄，包括美颜，贴纸，滤镜
        initSticker();
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view尺寸改变的时候调用
     *
     * @param width
     * @param height
     */
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        synchronized (mHumanActionHandleLock) {
            mSTHumanActionNative.destroyInstance();
        }
        mSTFaceAttributeNative.destroyInstance();
        mSTMobileObjectTrackNative.destroyInstance();
    }

    /**
     * 工作在opengl线程, 具体渲染的工作函数
     */
    public int onDrawFrame(int texId, int width, int height, ByteBuffer buffer) {
        int texOutId = texId;
        int orientation = STRotateType.ST_CLOCKWISE_ROTATE_0;

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mTextureOutId == null) {
            mTextureOutId = new int[1];
            GlUtil.initEffectTexture(width, height, mTextureOutId, GLES20.GL_TEXTURE_2D);
        }

        if (mNeedSticker) {
            STHumanAction humanAction = null;
            if (mIsCreateHumanActionHandleSucceeded) {
                if (buffer == null) { // TODO 双输入模式
                    if (mImageData == null || mImageData.length != width * height * 3 / 2) {
                        return texOutId;
                    }
                    synchronized (mImageDataLock) {
                        if (mNv21ImageData == null || mNv21ImageData.length != width * height * 3 / 2) {
                            mNv21ImageData = new byte[width * height * 3 / 2];
                        }
                        if (mImageData != null && mNv21ImageData.length >= mImageData.length) {
                            System.arraycopy(mImageData, 0, mNv21ImageData, 0, mImageData.length);
                        }
                    }
                    humanAction = mSTHumanActionNative.humanActionDetect(mNv21ImageData, STCommon.ST_PIX_FMT_NV21, mDetectConfig, STRotateType.ST_CLOCKWISE_ROTATE_270, height, width);
                    humanAction = STHumanAction.humanActionRotateAndMirror(humanAction, width, height, 1, mRotation);
                } else { // 单输入模式
                    humanAction = mSTHumanActionNative.humanActionDetect(buffer.array(), STCommon.ST_PIX_FMT_RGBA8888, mDetectConfig, orientation, width, height);
                }
            }
            int result = mStStickerNative.processTexture(texId, humanAction, orientation, STRotateType.ST_CLOCKWISE_ROTATE_0, width, height, false, null, mTextureOutId[0]);
            if (result == 0) {
                texOutId = mTextureOutId[0];
            }
        }

        return texOutId;
    }

    private void initHumanAction() {
        new Thread(() -> {
            synchronized (mHumanActionHandleLock) {
                //从asset资源文件夹读取model到内存，再使用底层st_mobile_human_action_create_from_buffer接口创建handle
                int result = mSTHumanActionNative.createInstanceFromAssetFile(FileUtils.getActionModelName(), mHumanActionCreateConfig, mContext.getAssets());
                Log.i(TAG, "the result for createInstance for human_action is " + result);

                if (result == 0) {
                    mIsCreateHumanActionHandleSucceeded = true;
                    mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_PARAM_BACKGROUND_BLUR_STRENGTH, 0.35f);

                    //for test face morph
                    result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.MODEL_NAME_FACE_EXTRA, mContext.getAssets());
                    Log.i(TAG, "add face extra model result: " + result);

                    //for test avatar
                    result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.MODEL_NAME_EYEBALL_CONTOUR, mContext.getAssets());
                    Log.i(TAG, "add eyeball contour model result: " + result);
                }
            }
        }).start();
    }

    private void initSticker() {
        int result = mStStickerNative.createInstance(mContext);
        //从资源文件加载Avatar模型
        mStStickerNative.loadAvatarModelFromAssetFile(FileUtils.MODEL_NAME_AVATAR_CORE, mContext.getAssets());
        setHumanActionDetectConfig(mNeedSticker, mStStickerNative.getTriggerAction());
        Log.i(TAG, "the result for createInstance for human_action is " + result);
    }

    /**
     * human action detect的配置选项,根据Sticker的TriggerAction和是否需要美颜配置
     *
     * @param needFaceDetect 是否需要开启face detect
     * @param config         sticker的TriggerAction
     */
    private void setHumanActionDetectConfig(boolean needFaceDetect, long config) {
        if (!mNeedSticker || mCurrentSticker == null) {
            config = 0;
        }

        if (needFaceDetect) {
            mDetectConfig = config | STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
        } else {
            mDetectConfig = config;
        }
    }

}
