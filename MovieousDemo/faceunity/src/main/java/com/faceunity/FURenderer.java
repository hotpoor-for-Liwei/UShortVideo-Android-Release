package com.faceunity;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.faceunity.entity.Effect;
import com.faceunity.entity.Filter;
import com.faceunity.entity.Makeup;
import com.faceunity.utils.Constant;
import com.faceunity.wrapper.faceunity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.faceunity.wrapper.faceunity.FU_ADM_FLAG_FLIP_X;

/**
 * 一个基于Faceunity Nama SDK的简单封装，方便简单集成，理论上简单需求的步骤：
 * <p>
 * 1.通过OnEffectSelectedListener在UI上进行交互
 * 2.合理调用FURenderer构造函数
 * 3.对应的时机调用onSurfaceCreated和onSurfaceDestroyed
 * 4.处理图像时调用onDrawFrame
 * <p>
 * 如果您有更高级的定制需求，Nama API文档请参考http://www.faceunity.com/technical/android-api.html
 */
public class FURenderer implements OnFUControlListener {
    private static final String TAG = FURenderer.class.getSimpleName();

    public static final int FU_ADM_FLAG_EXTERNAL_OES_TEXTURE = faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;

    private Context mContext;

    /**
     * 目录assets下的 *.bundle为程序的数据文件。
     * 其中 v3.bundle：人脸识别数据文件，缺少该文件会导致系统初始化失败；
     * face_beautification.bundle：美颜和美型相关的数据文件；
     * anim_model.bundle：优化表情跟踪功能所需要加载的动画数据文件；适用于使用Animoji和avatar功能的用户，如果不是，可不加载
     * ardata_ex.bundle：高精度模式的三维张量数据文件。适用于换脸功能，如果没用该功能可不加载
     * fxaa.bundle：3D绘制抗锯齿数据文件。加载后，会使得3D绘制效果更加平滑。
     * 目录effects下是我们打包签名好的道具
     */
    public static final String BUNDLE_v3 = "v3.bundle";
    public static final String BUNDLE_anim_model = "anim_model.bundle";
    public static final String BUNDLE_face_beautification = "face_beautification.bundle";
    public static final String BUNDLE_hair = "hair/hair_color.bundle";
    public static final String BUNDLE_HAIR_GRADIENT = "hair/hair_gradient.bundle";
    public static final String BUNDLE_ardata_ex = "ardata_ex.bundle";
    public static final String BUNDLE_tongue = "tongue.bundle";
    public static final String BUNDLE_animoji_3d = "fxaa.bundle";
    public static final String BUNDLE_poster_face = "change_face.bundle";
    public static final String BUNDLE_TOON_FILTER = "fuzzytoonfilter.bundle";

    public static final int HAIR_NORMAL = 1;
    public static final int HAIR_GRADIENT = 2;

    //美颜和滤镜的默认参数
    private boolean isNeedUpdateFaceBeauty = true;
    private static float mFilterLevel = 1.0f;//滤镜强度

    private static float mSkinDetect = 1.0f;//精准磨皮
    private static float mHeavyBlur = 0.0f;//美肤类型
    private static float mBlurLevel = 0.7f;//磨皮
    private static float mColorLevel = 0.2f;//美白
    private static float mRedLevel = 0.0f;//红润
    private static float mEyeBright = 0.0f;//亮眼
    private static float mToothWhiten = 0.0f;//美牙

    private static float mFaceShape = 4.0f;//脸型
    private static float mFaceShapeLevel = 1.0f;//程度
    private static float mEyeEnlarging = 0.4f;//大眼
    private static float mCheekThinning = 0.4f;//瘦脸
    private static float mIntensityChin = 0.3f;//下巴
    private static float mIntensityForehead = 0.3f;//额头
    private static float mIntensityNose = 0.5f;//瘦鼻
    private static float mIntensityMouth = 0.4f;//嘴形

    private int mFrameId = 0;

    private static final int ITEM_ARRAYS_FACE_BEAUTY_INDEX = 0;
    private static final int ITEM_ARRAYS_EFFECT = 1;
    private static final int ITEM_ARRAYS_EFFECT_HAIR = 2;
    private static final int ITEM_ARRAYS_EFFECT_ABIMOJI_3D = 3;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_EYE_SHADOW_INDEX = 4;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_EYE_LINER_INDEX = 5;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_EYELASH_INDEX = 6;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_CONTACT_LENS_INDEX = 7;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_EYEBROW_INDEX = 8;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_LIPSTICK_INDEX = 9;
    private static final int ITEM_ARRAYS_FACE_MAKEUP_BLUSHER_INDEX = 10;
    private static final int ITEM_ARRAYS_POSTER_FACE_INDEX = 11;
    private static final int ITEM_ARRAYS_EFFECT_HAIR_GRADIENT = 12;
    private static final int ITEM_ARRAYS_ANIMOJI_FILTER = 13;

    private static final int ITEM_ARRAYS_COUNT = 14;

    private static Filter mFilterName = new Filter("origin");
    // 海报换脸 track 50次
    private static final int MAX_TRACK_COUNT = 50;
    //美颜和其他道具的handle数组
    private final int[] mItemsArray = new int[ITEM_ARRAYS_COUNT];
    //用于和异步加载道具的线程交互
    private HandlerThread mFuItemHandlerThread;
    private Handler mFuItemHandler;

    private boolean isNeedBeautyHair = false;
    private boolean isNeedFaceBeauty = true;
    private boolean isNeedAnimoji3D = false;
    private boolean isNeedPosterFace = false;
    private boolean isNeedAnimFilter = false;
    private Effect mDefaultEffect;//默认道具（同步加载）
    private int mMaxFaces = 4; //同时识别的最大人脸
    private boolean mIsCreateEGLContext; //是否需要手动创建EGLContext
    private int mInputTextureType = 0; //输入的图像texture类型，Camera提供的默认为EXTERNAL OES
    private int mInputImageFormat = 0;
    private boolean mNeedReadBackImage = false; //将传入的byte[]图像复写为具有道具效果的

    private int mInputImageOrientation = 0;
    private int mCurrentCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;

    // 渐变从第三个开始
    private int mHairColorIndex = 3;
    private float mHairColorStrength = 0.6f;
    private int mHairColorType = HAIR_GRADIENT;

    private float[] landmarksData = new float[150];
    private float[] expressionData = new float[46];
    private float[] rotationData = new float[4];
    private float[] pupilPosData = new float[2];
    private float[] rotationModeData = new float[1];
    private float[] faceRectData = new float[4];

    private double[] posterFaceLandmark = new double[150];
    private double[] posterFaceLandmark2 = new double[150];

    private List<Runnable> mEventQueue;

    /**
     * 全局加载相应的底层数据包
     */
    public static void initFURenderer(Context context) {
        try {
            //获取faceunity SDK版本信息
            Log.e(TAG, "fu sdk version " + faceunity.fuGetVersion());

            /**
             * fuSetup faceunity初始化
             * 其中 v3.bundle：人脸识别数据文件，缺少该文件会导致系统初始化失败；
             *      authpack：用于鉴权证书内存数组。若没有,请咨询support@faceunity.com
             * 首先调用完成后再调用其他FU API
             */
            InputStream v3 = context.getAssets().open(BUNDLE_v3);
            byte[] v3Data = new byte[v3.available()];
            v3.read(v3Data);
            v3.close();
            faceunity.fuSetup(v3Data, null, authpack.A());

            /**
             * 加载优化表情跟踪功能所需要加载的动画数据文件anim_model.bundle；
             * 启用该功能可以使表情系数及avatar驱动表情更加自然，减少异常表情、模型缺陷的出现。该功能对性能的影响较小。
             * 启用该功能时，通过 fuLoadAnimModel 加载动画模型数据，加载成功即可启动。该功能会影响通过fuGetFaceInfo获取的expression表情系数，以及通过表情驱动的avatar模型。
             * 适用于使用Animoji和avatar功能的用户，如果不是，可不加载
             */
            InputStream animModel = context.getAssets().open(BUNDLE_anim_model);
            byte[] animModelData = new byte[animModel.available()];
            animModel.read(animModelData);
            animModel.close();
            faceunity.fuLoadAnimModel(animModelData);

            /**
             * 加载高精度模式的三维张量数据文件ardata_ex.bundle。
             * 适用于换脸功能，如果没用该功能可不加载；如果使用了换脸功能，必须加载，否则会报错
             */
            InputStream ar = context.getAssets().open(BUNDLE_ardata_ex);
            byte[] arDate = new byte[ar.available()];
            ar.read(arDate);
            ar.close();
            faceunity.fuLoadExtendedARData(arDate);

            InputStream tongue = context.getAssets().open(BUNDLE_tongue);
            byte[] tongueDate = new byte[tongue.available()];
            tongue.read(tongueDate);
            tongue.close();
            faceunity.fuLoadTongueModel(tongueDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取faceunity sdk 版本库
     */
    public static String getVersion() {
        return faceunity.fuGetVersion();
    }

    /**
     * 获取证书相关的权限码
     */
    public static int getModuleCode() {
        return faceunity.fuGetModuleCode(0);
    }

    /**
     * FURenderer构造函数
     */
    private FURenderer(Context context, boolean isCreateEGLContext) {
        this.mContext = context;
        this.mIsCreateEGLContext = isCreateEGLContext;
    }

    /**
     * 创建及初始化faceunity相应的资源
     */
    public void onSurfaceCreated() {
        Log.e(TAG, "onSurfaceCreated");
        onSurfaceDestroyed();

        mEventQueue = Collections.synchronizedList(new ArrayList<Runnable>());

        mFuItemHandlerThread = new HandlerThread("FUItemHandlerThread");
        mFuItemHandlerThread.start();
        mFuItemHandler = new FUItemHandler(mFuItemHandlerThread.getLooper());

        /**
         * fuCreateEGLContext 创建OpenGL环境
         * 适用于没OpenGL环境时调用
         * 如果调用了fuCreateEGLContext，在销毁时需要调用fuReleaseEGLContext
         */
        if (mIsCreateEGLContext)
            faceunity.fuCreateEGLContext();

        mFrameId = 0;
        /**
         *fuSetExpressionCalibration 控制表情校准功能的开关及不同模式，参数为0时关闭表情校准，2为被动校准。
         * 被动校准：该种模式下会在整个用户使用过程中逐渐进行表情校准，用户对该过程没有明显感觉。
         *
         * 优化后的SDK只支持被动校准功能，即fuSetExpressionCalibration接口只支持0（关闭）或2（被动校准）这两个数字，设置为1时将不再有效果。
         */
        faceunity.fuSetExpressionCalibration(2);
        faceunity.fuSetMaxFaces(mMaxFaces);//设置多脸，目前最多支持8人。
        faceunity.fuSetDefaultOrientation(0);

        //加载默认道具
        if (mDefaultEffect != null) {
            mItemsArray[ITEM_ARRAYS_EFFECT] = mDefaultEffect.effectType() == Effect.EFFECT_TYPE_NONE ? 0 : loadItem(mDefaultEffect.path());
            updateEffectItemParams(mDefaultEffect, mItemsArray[ITEM_ARRAYS_EFFECT]);
            setMaxFaces(mDefaultEffect.maxFace());
        }

        if (isNeedFaceBeauty) {
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_FACE_BEAUTY_INDEX);
        }
        if (isNeedBeautyHair) {
            if (mHairColorType == HAIR_NORMAL) {
                mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_EFFECT_HAIR);
            } else {
                mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_EFFECT_HAIR_GRADIENT);
            }
        }
        if (isNeedAnimoji3D) {
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_EFFECT_ABIMOJI_3D);
        }
        if (isNeedPosterFace) {
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_POSTER_FACE_INDEX);
        }
        if (isNeedAnimFilter) {
            mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEM_ARRAYS_ANIMOJI_FILTER, true));
        }
    }

    /**
     * 单输入接口(fuRenderToNV21Image)
     *
     * @param img NV21数据
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrame(byte[] img, int w, int h) {
        if (img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 单输入接口(fuRenderToNV21Image)，自定义画面数据需要回写到的byte[]
     *
     * @param img         NV21数据
     * @param w
     * @param h
     * @param readBackImg 画面数据需要回写到的byte[]
     * @param readBackW
     * @param readBackH
     * @return
     */
    public int onDrawFrame(byte[] img, int w, int h, byte[] readBackImg, int readBackW, int readBackH) {
        if (img == null || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags,
                readBackW, readBackH, readBackImg);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 双输入接口(fuDualInputToTexture)(处理后的画面数据并不会回写到数组)，由于省去相应的数据拷贝性能相对最优，推荐使用。
     *
     * @param img NV21数据
     * @param tex 纹理ID
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrame(byte[] img, int tex, int w, int h) {
        if (tex <= 0 || img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType | mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 双输入接口(fuDualInputToTexture)，自定义画面数据需要回写到的byte[]
     *
     * @param img         NV21数据
     * @param tex         纹理ID
     * @param w
     * @param h
     * @param readBackImg 画面数据需要回写到的byte[]
     * @param readBackW
     * @param readBackH
     * @return
     */
    public int onDrawFrame(byte[] img, int tex, int w, int h, byte[] readBackImg, int readBackW, int readBackH) {
        if (tex <= 0 || img == null || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType | mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray,
                readBackW, readBackH, readBackImg);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 单输入接口(fuRenderToTexture)
     *
     * @param tex 纹理ID
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrame(int tex, int w, int h) {
        if (tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 单美颜接口(fuBeautifyImage)，将输入的图像数据，送入SDK流水线进行全图美化，并输出处理之后的图像数据。
     * 该接口仅执行图像层面的美化处 理（包括滤镜、美肤），不执行人脸跟踪及所有人脸相关的操作（如美型）。
     * 由于功能集中，相比 fuDualInputToTexture 接口执行美颜道具，该接口所需计算更少，执行效率更高。
     *
     * @param tex 纹理ID
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameBeautify(int tex, int w, int h) {
        if (tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType;

        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuBeautifyImage(tex, flags, w, h, mFrameId++, mItemsArray);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 使用 fuTrackFace + fuAvatarToTexture 的方法组合绘制画面，该组合没有camera画面绘制，适用于animoji等相关道具的绘制。
     * fuTrackFace 获取识别到的人脸信息
     * fuAvatarToTexture 依据人脸信息绘制道具
     *
     * @param img 数据格式可由 flags 定义
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameAvatar(byte[] img, int w, int h) {
        if (img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrameAvatar date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputImageFormat;
        if (mNeedBenchmark)
            mFuCallStartTime = System.nanoTime();
        faceunity.fuTrackFace(img, flags, w, h);

        int isTracking = faceunity.fuIsTracking();

        Arrays.fill(landmarksData, 0.0f);
        Arrays.fill(rotationData, 0.0f);
        Arrays.fill(expressionData, 0.0f);
        Arrays.fill(pupilPosData, 0.0f);
        Arrays.fill(rotationModeData, 0.0f);

        if (isTracking > 0) {
            /**
             * landmarks 2D人脸特征点，返回值为75个二维坐标，长度75*2
             */
            faceunity.fuGetFaceInfo(0, "landmarks", landmarksData);
            /**
             *rotation 人脸三维旋转，返回值为旋转四元数，长度4
             */
            faceunity.fuGetFaceInfo(0, "rotation", rotationData);
            /**
             * expression  表情系数，长度46
             */
            faceunity.fuGetFaceInfo(0, "expression", expressionData);
            /**
             * pupil pos 人脸朝向，0-3分别对应手机四种朝向，长度1
             */
            faceunity.fuGetFaceInfo(0, "pupil_pos", pupilPosData);
            /**
             * rotation mode
             */
            faceunity.fuGetFaceInfo(0, "rotation_mode", rotationModeData);
        } else {
            rotationData[3] = 1.0f;
            rotationModeData[0] = (360 - mInputImageOrientation) / 90;
        }

        int tex = faceunity.fuAvatarToTexture(pupilPosData, expressionData, rotationData, rotationModeData,
                0, w, h, mFrameId++, mItemsArray, isTracking);
        if (mNeedBenchmark)
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return tex;
    }

    public float[] getRotationData() {
        Arrays.fill(rotationData, 0.0f);
        faceunity.fuGetFaceInfo(0, "rotation", rotationData);
        return rotationData;
    }

    /**
     * 销毁faceunity相关的资源
     */
    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed");
        if (mFuItemHandlerThread != null) {
            mFuItemHandlerThread.quitSafely();
            mFuItemHandlerThread = null;
            mFuItemHandler = null;
        }
        if (mEventQueue != null) {
            mEventQueue.clear();
            mEventQueue = null;
        }

        mFrameId = 0;
        isNeedUpdateFaceBeauty = true;
        Arrays.fill(mItemsArray, 0);
        faceunity.fuDestroyAllItems();
        faceunity.fuOnDeviceLost();
        faceunity.fuDone();
        if (mIsCreateEGLContext)
            faceunity.fuReleaseEGLContext();
    }

    public float[] getLandmarksData(int faceId) {
        int isTracking = faceunity.fuIsTracking();
        Arrays.fill(landmarksData, 0.0f);
        if (isTracking > 0) {
            faceunity.fuGetFaceInfo(faceId, "landmarks", landmarksData);
        }
        return Arrays.copyOf(landmarksData, landmarksData.length);
    }

    public int trackFace(byte[] img, int w, int h) {
        faceunity.fuOnCameraChange();
        int flags = mInputImageFormat;
        for (int i = 0; i < MAX_TRACK_COUNT; i++) {
            faceunity.fuTrackFace(img, flags, w, h);
        }
        return faceunity.fuIsTracking();
    }

    public float[] getFaceRectData(int i) {
        Arrays.fill(faceRectData, 0.0f);
        faceunity.fuGetFaceInfo(i, "face_rect", faceRectData);
        return faceRectData;
    }

    /**
     * 每帧处理画面时被调用
     */
    private void prepareDrawFrame() {
        //计算FPS等数据
        benchmarkFPS();

        //获取人脸是否识别，并调用回调接口
        int isTracking = faceunity.fuIsTracking();
        if (mOnTrackingStatusChangedListener != null && mTrackingStatus != isTracking) {
            mOnTrackingStatusChangedListener.onTrackingStatusChanged(mTrackingStatus = isTracking);
        }

        //获取faceunity错误信息，并调用回调接口
        int error = faceunity.fuGetSystemError();
        if (error != 0)
            Log.e(TAG, "fuGetSystemErrorString " + faceunity.fuGetSystemErrorString(error));
        if (mOnSystemErrorListener != null && error != 0) {
            mOnSystemErrorListener.onSystemError(faceunity.fuGetSystemErrorString(error));
        }

        //修改美颜参数
        if (isNeedUpdateFaceBeauty && mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX] != 0) {
            //filter_level 滤镜强度 范围0~1 SDK默认为 1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "filter_level", mFilterLevel);
            //filter_name 滤镜
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "filter_name", mFilterName.filterName());

            //skin_detect 精准美肤 0:关闭 1:开启 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "skin_detect", mSkinDetect);
            //heavy_blur 美肤类型 0:清晰美肤 1:朦胧美肤 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "heavy_blur", mHeavyBlur);
            //blur_level 磨皮 范围0~6 SDK默认为 6
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "blur_level", 6 * mBlurLevel);
            //blur_blend_ratio 磨皮结果和原图融合率 范围0~1 SDK默认为 1
//          faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "blur_blend_ratio", 1);

            //color_level 美白 范围0~1 SDK默认为 1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "color_level", mColorLevel);
            //red_level 红润 范围0~1 SDK默认为 1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "red_level", mRedLevel);
            //eye_bright 亮眼 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "eye_bright", mEyeBright);
            //tooth_whiten 美牙 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "tooth_whiten", mToothWhiten);


            //face_shape_level 美型程度 范围0~1 SDK默认为1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "face_shape_level", mFaceShapeLevel);
            //face_shape 脸型 0：女神 1：网红 2：自然 3：默认 4：自定义（新版美型） SDK默认为 3
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "face_shape", mFaceShape);
            //eye_enlarging 大眼 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "eye_enlarging", mEyeEnlarging);
            //cheek_thinning 瘦脸 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "cheek_thinning", mCheekThinning);
            //intensity_chin 下巴 范围0~1 SDK默认为 0.5    大于0.5变大，小于0.5变小
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_chin", mIntensityChin);
            //intensity_forehead 额头 范围0~1 SDK默认为 0.5    大于0.5变大，小于0.5变小
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_forehead", mIntensityForehead);
            //intensity_nose 鼻子 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_nose", mIntensityNose);
            //intensity_mouth 嘴型 范围0~1 SDK默认为 0.5   大于0.5变大，小于0.5变小
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_mouth", mIntensityMouth);
            isNeedUpdateFaceBeauty = false;
        }

        //queueEvent的Runnable在此处被调用
        while (!mEventQueue.isEmpty()) {
            mEventQueue.remove(0).run();
        }
    }

    //--------------------------------------对外可使用的接口----------------------------------------

    /**
     * 类似GLSurfaceView的queueEvent机制
     */
    public void queueEvent(Runnable r) {
        if (mEventQueue == null)
            return;
        mEventQueue.add(r);
    }

    /**
     * 类似GLSurfaceView的queueEvent机制,保护在快速切换界面时进行的操作是当前界面的加载操作
     */
    private void queueEventItemHandle(Runnable r) {
        if (mFuItemHandlerThread == null || Thread.currentThread().getId() != mFuItemHandlerThread.getId())
            return;
        queueEvent(r);
    }

    /**
     * 设置需要识别的人脸个数
     *
     * @param maxFaces
     */
    public void setMaxFaces(final int maxFaces) {
        if (mMaxFaces != maxFaces && maxFaces > 0) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mMaxFaces = maxFaces;
                    faceunity.fuSetMaxFaces(maxFaces);
                }
            });
        }
    }

    private int mDefaultOrientation = 0;

    /**
     * camera切换时需要调用
     *
     * @param currentCameraType     前后置摄像头ID
     * @param inputImageOrientation
     */
    public void onCameraChange(final int currentCameraType, final int inputImageOrientation) {
        if (mCurrentCameraType == currentCameraType && mInputImageOrientation == inputImageOrientation)
            return;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mFrameId = 0;
                mCurrentCameraType = currentCameraType;
                mInputImageOrientation = inputImageOrientation;
                setRotMode(mItemsArray[ITEM_ARRAYS_EFFECT]);
                faceunity.fuOnCameraChange();
                updateEffectItemParams(mDefaultEffect, mItemsArray[ITEM_ARRAYS_EFFECT]);
            }
        });
    }

    public void setTrackOrientation(final int rotation) {
        if (mDefaultOrientation != rotation) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mDefaultOrientation = rotation;
                    setRotMode(mItemsArray[ITEM_ARRAYS_EFFECT]);
                }
            });
        }
    }

    private void setRotMode(int item) {
        int mode;
        if (mInputImageOrientation == 270) {
            if (mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mode = mDefaultOrientation / 90;
            } else {
                mode = (mDefaultOrientation - 180) / 90;
            }
        } else {
            if (mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mode = (mDefaultOrientation + 180) / 90;
            } else {
                mode = (mDefaultOrientation) / 90;
            }
        }
        Log.d(TAG, "setTrackOrientation: rot:" + mDefaultOrientation + ", mode:" + mode);
        faceunity.fuSetDefaultOrientation(mDefaultOrientation / 90);//设置识别人脸默认方向，能够提高首次识别的速度
        faceunity.fuItemSetParam(item, "rotMode", mode);
    }

    public void changeInputType() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mFrameId = 0;
            }
        });
    }

    public void setDefaultEffect(Effect defaultEffect) {
        mDefaultEffect = defaultEffect;
    }

    //--------------------------------------美颜参数与道具回调----------------------------------------

    @Override
    public void onMusicFilterTime(final long time) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT], "music_time", time);
            }
        });
    }

    @Override
    public void onEffectSelected(Effect effectItemName) {
        mDefaultEffect = effectItemName;
        if (mDefaultEffect == null)
            return;
        if (mFuItemHandler == null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mFuItemHandler.removeMessages(ITEM_ARRAYS_EFFECT);
                    mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEM_ARRAYS_EFFECT, mDefaultEffect));
                }
            });
        } else {
            mFuItemHandler.removeMessages(ITEM_ARRAYS_EFFECT);
            mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEM_ARRAYS_EFFECT, mDefaultEffect));
        }
    }

    @Override
    public void onFilterLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mFilterLevel = progress;
    }

    @Override
    public void onFilterNameSelected(Filter filterName) {
        isNeedUpdateFaceBeauty = true;
        mFilterName = filterName;
    }

    @Override
    public void onHairSelected(int type, int hairColorIndex, float hairColorLevel) {
//        Log.d(TAG, "onHairSelected: type:" + type + ", index:" + hairColorIndex + ", level:" + hairColorLevel);
        mHairColorType = type;
        mHairColorIndex = hairColorIndex;
        mHairColorStrength = hairColorLevel;
        if (type == HAIR_NORMAL) {
            mFuItemHandler.removeMessages(ITEM_ARRAYS_EFFECT_HAIR);
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_EFFECT_HAIR);
        } else if (type == HAIR_GRADIENT) {
            mFuItemHandler.removeMessages(ITEM_ARRAYS_EFFECT_HAIR_GRADIENT);
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_EFFECT_HAIR_GRADIENT);
        }
    }

    @Override
    public void onHairLevelSelected(final int type, int hairColorIndex, float hairColorLevel) {
//        Log.d(TAG, "onHairLevelSelected: type:" + type + ", index:" + hairColorIndex + ", level:" + hairColorLevel);
        mHairColorIndex = hairColorIndex;
        mHairColorStrength = hairColorLevel;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (type == HAIR_NORMAL) {
                    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR], "Index", mHairColorIndex);
                    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR], "Strength", mHairColorStrength);
                } else if (type == HAIR_GRADIENT) {
                    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT], "Index", mHairColorIndex);
                    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT], "Strength", mHairColorStrength);
                }
            }
        });
    }

    @Override
    public void onMakeupSelected(final Makeup makeup) {
        if (makeup == null)
            return;
        final int what = getIndexMakeup(makeup.makeupType());
        if (mFuItemHandler == null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mFuItemHandler.removeMessages(what);
                    mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, what, makeup));
                }
            });
        } else {
            mFuItemHandler.removeMessages(what);
            mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, what, makeup));
        }
    }

    @Override
    public void onMakeupLevelSelected(final int makeupType, final float level) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                faceunity.fuItemSetParam(mItemsArray[getIndexMakeup(makeupType)], "makeup_intensity", level);
            }
        });
    }

    @Override
    public void onSkinDetectSelected(float isOpen) {
        isNeedUpdateFaceBeauty = true;
        mSkinDetect = isOpen;
    }

    @Override
    public void onHeavyBlurSelected(float isOpen) {
        isNeedUpdateFaceBeauty = true;
        mHeavyBlur = isOpen;
    }

    @Override
    public void onBlurLevelSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mBlurLevel = level;
    }

    @Override
    public void onColorLevelSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mColorLevel = level;
    }


    @Override
    public void onRedLevelSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mRedLevel = level;
    }

    @Override
    public void onEyeBrightSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mEyeBright = level;
    }

    @Override
    public void onToothWhitenSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mToothWhiten = level;
    }

    @Override
    public void onFaceShapeSelected(float faceShape) {
        isNeedUpdateFaceBeauty = true;
        mFaceShape = faceShape;
    }

    @Override
    public void onEyeEnlargeSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mEyeEnlarging = level;
    }

    @Override
    public void onCheekThinningSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mCheekThinning = level;
    }

    @Override
    public void onIntensityChinSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mIntensityChin = level;
    }

    @Override
    public void onIntensityForeheadSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mIntensityForehead = level;
    }

    @Override
    public void onIntensityNoseSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mIntensityNose = level;
    }

    @Override
    public void onIntensityMouthSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mIntensityMouth = level;
    }

    @Override
    public void onPosterTemplateChanged() {
        if (mFuItemHandler != null) {
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_POSTER_FACE_INDEX);
        }
    }

    @Override
    public void onPosterTemplateSelected(final int tempWidth, final int tempHeight, final byte[] temp, final float[] landmark) {
        Log.d(TAG, "onPosterTemplateSelected() called with: tempWidth = [" + tempWidth + "], tempHeight = ["
                + tempHeight + ", temp:" + temp.length + "], handle = [" + mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX]);
        Arrays.fill(posterFaceLandmark, 0);
        for (int i = 0; i < landmark.length; i++) {
            posterFaceLandmark[i] = landmark[i];
        }
        faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "template_width", tempWidth);
        faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "template_height", tempHeight);
        faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "template_face_points", posterFaceLandmark);
        faceunity.fuItemSetParamu8v(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "tex_template", temp, temp.length);
    }

    @Override
    public void onPosterInputPhoto(final int inputWidth, final int inputHeight, final byte[] input, final float[] landmark) {
        Log.d(TAG, "onPosterInputPhoto() called with: inputWidth = [" + inputWidth + "], inputHeight = ["
                + inputHeight + ", input:" + input.length + "], handle = [" + mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX]);
        Arrays.fill(posterFaceLandmark2, 0);
        for (int i = 0; i < landmark.length; i++) {
            posterFaceLandmark2[i] = landmark[i];
        }
        faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "input_width", inputWidth);
        faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "input_height", inputHeight);
        faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "input_face_points", posterFaceLandmark2);
        faceunity.fuItemSetParamu8v(mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX], "tex_input", input, input.length);
    }

    @Override
    public void onLoadAnimFilter(final boolean enable) {
        isNeedAnimFilter = enable;
        if (mDefaultEffect == null)
            return;
        if (mFuItemHandler == null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mFuItemHandler.removeMessages(ITEM_ARRAYS_ANIMOJI_FILTER);
                    mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEM_ARRAYS_ANIMOJI_FILTER, enable));
                }
            });
        } else {
            mFuItemHandler.removeMessages(ITEM_ARRAYS_ANIMOJI_FILTER);
            mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEM_ARRAYS_ANIMOJI_FILTER, enable));
        }
    }

    //--------------------------------------IsTracking（人脸识别回调相关定义）----------------------------------------

    private int mTrackingStatus = 0;

    public interface OnTrackingStatusChangedListener {
        void onTrackingStatusChanged(int status);
    }

    private OnTrackingStatusChangedListener mOnTrackingStatusChangedListener;

    //--------------------------------------FaceUnitySystemError（faceunity错误信息回调相关定义）----------------------------------------

    public interface OnSystemErrorListener {
        void onSystemError(String error);
    }

    private OnSystemErrorListener mOnSystemErrorListener;

    //--------------------------------------FPS（FPS相关定义）----------------------------------------

    private static final float NANO_IN_ONE_MILLI_SECOND = 1000000.0f;
    private static final float TIME = 5f;
    private int mCurrentFrameCnt = 0;
    private long mLastOneHundredFrameTimeStamp = 0;
    private long mOneHundredFrameFUTime = 0;
    private boolean mNeedBenchmark = true;
    private long mFuCallStartTime = 0;

    private OnFUDebugListener mOnFUDebugListener;

    public interface OnFUDebugListener {
        void onFpsChange(double fps, double renderTime);
    }

    private void benchmarkFPS() {
        if (!mNeedBenchmark)
            return;
        if (++mCurrentFrameCnt == TIME) {
            mCurrentFrameCnt = 0;
            long tmp = System.nanoTime();
            double fps = (1000.0f * NANO_IN_ONE_MILLI_SECOND / ((tmp - mLastOneHundredFrameTimeStamp) / TIME));
            mLastOneHundredFrameTimeStamp = tmp;
            double renderTime = mOneHundredFrameFUTime / TIME / NANO_IN_ONE_MILLI_SECOND;
            mOneHundredFrameFUTime = 0;

            if (mOnFUDebugListener != null) {
                mOnFUDebugListener.onFpsChange(fps, renderTime);
            }
        }
    }

    //--------------------------------------道具（异步加载道具）----------------------------------------

    private int getIndexMakeup(int makeType) {
        switch (makeType) {
            case Makeup.MAKEUP_TYPE_LIPSTICK:
                return ITEM_ARRAYS_FACE_MAKEUP_LIPSTICK_INDEX;
            case Makeup.MAKEUP_TYPE_EYEBROW:
                return ITEM_ARRAYS_FACE_MAKEUP_EYEBROW_INDEX;
            case Makeup.MAKEUP_TYPE_EYE_SHADOW:
                return ITEM_ARRAYS_FACE_MAKEUP_EYE_SHADOW_INDEX;
            case Makeup.MAKEUP_TYPE_EYE_LINER:
                return ITEM_ARRAYS_FACE_MAKEUP_EYE_LINER_INDEX;
            case Makeup.MAKEUP_TYPE_EYELASH:
                return ITEM_ARRAYS_FACE_MAKEUP_EYELASH_INDEX;
            case Makeup.MAKEUP_TYPE_CONTACT_LENS:
                return ITEM_ARRAYS_FACE_MAKEUP_CONTACT_LENS_INDEX;
            case Makeup.MAKEUP_TYPE_BLUSHER:
                return ITEM_ARRAYS_FACE_MAKEUP_BLUSHER_INDEX;
            default:
                return 0;
        }
    }

    /**
     * fuCreateItemFromPackage 加载道具
     *
     * @param bundle（Effect本demo定义的道具实体类）
     * @return 大于0时加载成功
     */
    private int loadItem(String bundle) {
        int item = 0;
        try {
            if (TextUtils.isEmpty(bundle)) {
                item = 0;
            } else {
                InputStream is = bundle.startsWith(Constant.filePath) ? new FileInputStream(new File(bundle)) : mContext.getAssets().open(bundle);
                byte[] itemData = new byte[is.available()];
                int len = is.read(itemData);
                is.close();
                item = faceunity.fuCreateItemFromPackage(itemData);
                Log.e(TAG, bundle + " len " + len + ", handle:" + item);
            }
        } catch (IOException e) {
            e.printStackTrace();
            item = 0;
        }
        return item;
    }

    /**
     * 设置对道具设置相应的参数
     *
     * @param itemHandle
     */
    private void updateEffectItemParams(Effect effect, final int itemHandle) {
        if (effect == null || itemHandle == 0)
            return;
        faceunity.fuItemSetParam(itemHandle, "isAndroid", 1.0);

        if (effect.effectType() == Effect.EFFECT_TYPE_NORMAL) {
            //rotationAngle 参数是用于旋转普通道具
            faceunity.fuItemSetParam(itemHandle, "rotationAngle", 360 - mInputImageOrientation);
        }
        if (effect.effectType() == Effect.EFFECT_TYPE_ANIMOJI || effect.effectType() == Effect.EFFECT_TYPE_PORTRAIT_DRIVE) {
            //is3DFlipH 参数是用于对3D道具的镜像
            faceunity.fuItemSetParam(itemHandle, "is3DFlipH", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
            //isFlipExpr 参数是用于对人像驱动道具的镜像
            faceunity.fuItemSetParam(itemHandle, "isFlipExpr", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
            //这两句代码用于识别人脸默认方向的修改，主要针对animoji道具的切换摄像头倒置问题
            faceunity.fuItemSetParam(itemHandle, "camera_change", 1.0);
            faceunity.fuSetDefaultRotationMode((360 - mInputImageOrientation) / 90);
        }
        if (effect.effectType() == Effect.EFFECT_TYPE_GESTURE) {
            //loc_y_flip与loc_x_flip 参数是用于对手势识别道具的镜像
            faceunity.fuItemSetParam(itemHandle, "is3DFlipH", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
            faceunity.fuItemSetParam(itemHandle, "loc_y_flip", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
            faceunity.fuItemSetParam(itemHandle, "loc_x_flip", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
        }
    }

    /**
     * FURenderer Builder
     */
    public static class Builder {

        private boolean createEGLContext = false;
        private Effect defaultEffect;
        private int maxFaces = 1;
        private Context context;
        private int inputTextureType = 0;
        private boolean needReadBackImage = false;
        private int inputImageFormat = 0;
        private int inputImageRotation = 270;
        private boolean isNeedAnimoji3D = false;
        private boolean isNeedBeautyHair = false;
        private boolean isNeedFaceBeauty = true;
        private boolean isNeedPosterFace = false;
        private int currentCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;

        private OnFUDebugListener onFUDebugListener;
        private OnTrackingStatusChangedListener onTrackingStatusChangedListener;
        private OnSystemErrorListener onSystemErrorListener;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        /**
         * 是否需要自己创建EGLContext
         *
         * @param createEGLContext
         * @return
         */
        public Builder createEGLContext(boolean createEGLContext) {
            this.createEGLContext = createEGLContext;
            return this;
        }

        /**
         * 是否需要立即加载道具
         *
         * @param defaultEffect
         * @return
         */
        public Builder defaultEffect(Effect defaultEffect) {
            this.defaultEffect = defaultEffect;
            return this;
        }

        /**
         * 识别最大人脸数
         *
         * @param maxFaces
         * @return
         */
        public Builder maxFaces(int maxFaces) {
            this.maxFaces = maxFaces;
            return this;
        }

        /**
         * 传入纹理的类型（传入数据没有纹理则无需调用）
         * camera OES纹理：1
         * 普通2D纹理：2
         *
         * @param textureType
         * @return
         */
        public Builder inputTextureType(int textureType) {
            this.inputTextureType = textureType;
            return this;
        }

        /**
         * 是否需要把处理后的数据回写到byte[]中
         *
         * @param needReadBackImage
         * @return
         */
        public Builder needReadBackImage(boolean needReadBackImage) {
            this.needReadBackImage = needReadBackImage;
            return this;
        }

        /**
         * 输入的byte[]数据类型
         *
         * @param inputImageFormat
         * @return
         */
        public Builder inputImageFormat(int inputImageFormat) {
            this.inputImageFormat = inputImageFormat;
            return this;
        }

        /**
         * 输入的画面数据方向
         *
         * @param inputImageRotation
         * @return
         */
        public Builder inputImageOrientation(int inputImageRotation) {
            this.inputImageRotation = inputImageRotation;
            return this;
        }

        /**
         * 是否需要3D道具的抗锯齿功能
         *
         * @param needAnimoji3D
         * @return
         */
        public Builder setNeedAnimoji3D(boolean needAnimoji3D) {
            this.isNeedAnimoji3D = needAnimoji3D;
            return this;
        }

        /**
         * 是否需要美发功能
         *
         * @param needBeautyHair
         * @return
         */
        public Builder setNeedBeautyHair(boolean needBeautyHair) {
            isNeedBeautyHair = needBeautyHair;
            return this;
        }

        /**
         * 是否需要美颜效果
         *
         * @param needFaceBeauty
         * @return
         */
        public Builder setNeedFaceBeauty(boolean needFaceBeauty) {
            isNeedFaceBeauty = needFaceBeauty;
            return this;
        }

        /**
         * 是否需要美颜效果
         *
         * @param needPosterFace
         * @return
         */
        public Builder setNeedPosterFace(boolean needPosterFace) {
            isNeedPosterFace = needPosterFace;
            return this;
        }

        /**
         * 当前的摄像头（前后置摄像头）
         *
         * @param cameraType
         * @return
         */
        public Builder setCurrentCameraType(int cameraType) {
            currentCameraType = cameraType;
            return this;
        }

        /**
         * 设置debug数据回调
         *
         * @param onFUDebugListener
         * @return
         */
        public Builder setOnFUDebugListener(OnFUDebugListener onFUDebugListener) {
            this.onFUDebugListener = onFUDebugListener;
            return this;
        }

        /**
         * 设置是否检查到人脸的回调
         *
         * @param onTrackingStatusChangedListener
         * @return
         */
        public Builder setOnTrackingStatusChangedListener(OnTrackingStatusChangedListener onTrackingStatusChangedListener) {
            this.onTrackingStatusChangedListener = onTrackingStatusChangedListener;
            return this;
        }

        /**
         * 设置SDK使用错误回调
         *
         * @param onSystemErrorListener
         * @return
         */
        public Builder setOnSystemErrorListener(OnSystemErrorListener onSystemErrorListener) {
            this.onSystemErrorListener = onSystemErrorListener;
            return this;
        }

        public FURenderer build() {
            FURenderer fuRenderer = new FURenderer(context, createEGLContext);
            fuRenderer.mMaxFaces = maxFaces;
            fuRenderer.mInputTextureType = inputTextureType;
            fuRenderer.mNeedReadBackImage = needReadBackImage;
            fuRenderer.mInputImageFormat = inputImageFormat;
            fuRenderer.mInputImageOrientation = inputImageRotation;
            fuRenderer.mDefaultEffect = defaultEffect;
            fuRenderer.isNeedAnimoji3D = isNeedAnimoji3D;
            fuRenderer.isNeedBeautyHair = isNeedBeautyHair;
            fuRenderer.isNeedFaceBeauty = isNeedFaceBeauty;
            fuRenderer.isNeedPosterFace = isNeedPosterFace;
            fuRenderer.mCurrentCameraType = currentCameraType;

            fuRenderer.mOnFUDebugListener = onFUDebugListener;
            fuRenderer.mOnTrackingStatusChangedListener = onTrackingStatusChangedListener;
            fuRenderer.mOnSystemErrorListener = onSystemErrorListener;
            return fuRenderer;
        }

    }

    private static boolean isInit;

    public void loadItems() {
        if (!isInit) {
            isInit = true;
            initFURenderer(mContext);
        }
        onSurfaceCreated();
    }

    public void destroyItems() {
        onSurfaceDestroyed();
    }

//--------------------------------------Builder----------------------------------------

    class FUItemHandler extends Handler {

        FUItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //加载道具
                case ITEM_ARRAYS_EFFECT: {
                    final Effect effect = (Effect) msg.obj;
                    if (effect == null)
                        break;
                    final int finalItem = effect.effectType() == Effect.EFFECT_TYPE_NONE ? 0 : loadItem(effect.path());
                    queueEventItemHandle(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemsArray[ITEM_ARRAYS_EFFECT] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT]);
                            }
                            if (finalItem > 0) {
                                updateEffectItemParams(effect, finalItem);
                                setMaxFaces(effect.maxFace());
                                if (mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER] > 0) {
                                    faceunity.fuItemSetParam(finalItem, "{\"thing\":\"<global>\",\"param\":\"follow\"}", 1);
                                }
                                setRotMode(finalItem);
                            }
                            mItemsArray[ITEM_ARRAYS_EFFECT] = finalItem;
                        }
                    });
                    break;
                }
                //加载美颜bundle
                case ITEM_ARRAYS_FACE_BEAUTY_INDEX: {
                    final int itemBeauty = loadItem(BUNDLE_face_beautification);
                    queueEventItemHandle(new Runnable() {
                        @Override
                        public void run() {
                            mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX] = itemBeauty;
                            isNeedUpdateFaceBeauty = true;
                        }
                    });
                    break;
                }
                //加载美发bundle
                case ITEM_ARRAYS_EFFECT_HAIR: {
                    final int hairItem = loadItem(BUNDLE_hair);
                    queueEventItemHandle(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemsArray[ITEM_ARRAYS_EFFECT_HAIR] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR]);
                                mItemsArray[ITEM_ARRAYS_EFFECT_HAIR] = 0;
                            }
                            mItemsArray[ITEM_ARRAYS_EFFECT_HAIR] = hairItem;
                            if (mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT]);
                                mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT] = 0;
                            }
//                            Log.i(TAG, "run: setParams hairIndex:" + mHairColorIndex + ", strength:" + mHairColorStrength
//                                    + ", handle " + mItemsArray[ITEM_ARRAYS_EFFECT_HAIR]);
                            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR], "Index", mHairColorIndex);
                            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR], "Strength", mHairColorStrength);
                        }
                    });
                    break;
                }
                //加载渐变美发bundle
                case ITEM_ARRAYS_EFFECT_HAIR_GRADIENT: {
                    final int hairItem = loadItem(BUNDLE_HAIR_GRADIENT);
                    queueEventItemHandle(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT]);
                                mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT] = 0;
                            }
                            mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT] = hairItem;
                            if (mItemsArray[ITEM_ARRAYS_EFFECT_HAIR] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR]);
                                mItemsArray[ITEM_ARRAYS_EFFECT_HAIR] = 0;
                            }
//                            Log.i(TAG, "run: setParams hairGradientIndex:" + mHairColorIndex +
//                                    ", strength:" + mHairColorStrength + ", handle:" + mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT]);
                            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT], "Index", mHairColorIndex);
                            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT_HAIR_GRADIENT], "Strength", mHairColorStrength);
                        }
                    });
                    break;
                }
                //加载animoji道具3D抗锯齿bundle
                case ITEM_ARRAYS_EFFECT_ABIMOJI_3D: {
                    final int itemAnimoji = loadItem(BUNDLE_animoji_3d);
                    queueEventItemHandle(new Runnable() {
                        @Override
                        public void run() {
                            mItemsArray[ITEM_ARRAYS_EFFECT_ABIMOJI_3D] = itemAnimoji;
                        }
                    });
                    break;
                }
                // 加载 animoji 风格滤镜
                case ITEM_ARRAYS_ANIMOJI_FILTER: {
                    final boolean enable = (boolean) msg.obj;
                    if (enable) {
                        if (mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER] <= 0) {
                            mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER] = loadItem(BUNDLE_TOON_FILTER);
                        }
                        queueEventItemHandle(new Runnable() {
                            @Override
                            public void run() {
                                faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT], "{\"thing\":\"<global>\",\"param\":\"follow\"}", 1);
//                                int supportGLVersion = GlUtil.getSupportGLVersion(mContext);
//                                faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER], "glVer", supportGLVersion);
                            }
                        });
                    } else {
                        if (mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER] > 0) {
                            queueEventItemHandle(new Runnable() {
                                @Override
                                public void run() {
                                    faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER]);
                                    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT], "{\"thing\":\"<global>\",\"param\":\"follow\"}", 0);
                                    mItemsArray[ITEM_ARRAYS_ANIMOJI_FILTER] = 0;
                                }
                            });
                        }
                    }
                    break;
                }
                // 加载海报换脸的 bundle
                case ITEM_ARRAYS_POSTER_FACE_INDEX: {
                    final int itemHandle = loadItem(BUNDLE_poster_face);
                    final int oldHandle = mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX];
                    mItemsArray[ITEM_ARRAYS_POSTER_FACE_INDEX] = itemHandle;
                    queueEventItemHandle(new Runnable() {
                        @Override
                        public void run() {
                            faceunity.fuDestroyItem(oldHandle);
                        }
                    });
                    break;
                }
                //加载美妆bundle
                default: {
                    final Makeup makeup = (Makeup) msg.obj;
                    if (TextUtils.isEmpty(makeup.path())) {
                        queueEventItemHandle(new Runnable() {
                            @Override
                            public void run() {
                                faceunity.fuItemSetParam(mItemsArray[getIndexMakeup(makeup.makeupType())], "makeup_intensity", 0);
                            }
                        });
                    } else {
                        final int item = loadItem(makeup.path());
                        queueEventItemHandle(new Runnable() {
                            @Override
                            public void run() {
                                mItemsArray[getIndexMakeup(makeup.makeupType())] = item;
                                faceunity.fuItemSetParam(item, "makeup_intensity", makeup.getLevel());
                            }
                        });
                    }
                    break;
                }
            }
        }
    }
}
