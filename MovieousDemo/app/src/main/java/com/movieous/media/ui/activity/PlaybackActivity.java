package com.movieous.media.ui.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import cn.ucloud.ufilesdk.UFileUploadManager;
import cn.ucloud.ufilesdk.UFileUploadStateListener;
import com.movieous.media.Constants;
import com.movieous.media.R;
import com.movieous.media.utils.AppUtils;
import com.movieous.media.utils.GetPathFromUri;
import com.movieous.media.utils.StringUtils;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;
import org.json.JSONException;
import org.json.JSONObject;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.movieous.media.ExtensionsKt.showToast;

public class PlaybackActivity extends AppCompatActivity implements UFileUploadStateListener {
    private static final String TAG = "PlaybackActivity";
    private static final String MP4_PATH = "MP4_PATH";

    private StandardGSYVideoPlayer mVideoView;
    private static String mVideoPath;
    private Button mUploadBtn;
    private UFileUploadManager mVideoUploadManager;
    private ProgressBar mProgressBarDeterminate;
    private boolean mIsUpload = false;

    public static void start(Activity activity, String mp4Path) {
        mVideoPath = mp4Path;
        Intent intent = new Intent(activity, PlaybackActivity.class);
        intent.putExtra(MP4_PATH, getLocalFilePath(mp4Path));
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        String videoPath = getIntent().getStringExtra(MP4_PATH);
        if (StringUtils.isEmpty(videoPath)) {
            startActivityForResult(Intent.createChooser(AppUtils.Companion.getMediaIntent(true), getString(R.string.select_media_file_tip)), VideoEditActivity.REQUEST_CODE_PICK_MEDIA_FILE);
            return;
        }
        setContentView(R.layout.activity_playback);
        mVideoView = findViewById(R.id.video_view);
        mVideoView.getBackButton().setOnClickListener(v -> PlaybackActivity.this.onBackPressed());
        mVideoView.getFullscreenButton().setVisibility(View.GONE);
        setVideo(videoPath);

        mVideoUploadManager = new UFileUploadManager(this);
        mUploadBtn = findViewById(R.id.btn_upload);
        mUploadBtn.setText(R.string.upload);
        mUploadBtn.setOnClickListener(new UploadOnClickListener());
        mProgressBarDeterminate = findViewById(R.id.progressBar);
        mProgressBarDeterminate.setMax(100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null) {
            getCurPlay().onVideoPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            getCurPlay().onVideoResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            GSYVideoManager.releaseAllVideos();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == VideoEditActivity.REQUEST_CODE_PICK_MEDIA_FILE) {
                String selectedFilepath = GetPathFromUri.getPath(this, data.getData());
                if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                    start(PlaybackActivity.this, selectedFilepath);
                    finish();
                }
            }
        } else {
            finish();
        }
    }

    /**
     * 设置播放视频 URL
     */
    private void setVideo(String url) {
        Log.d(TAG, "play url = " + url);
        mVideoView.setUp(url, false, "");
        mVideoView.setLooping(true);
        //开始自动播放
        mVideoView.startPlayLogic();
    }

    private GSYVideoPlayer getCurPlay() {
        if (mVideoView.getFullWindowPlayer() != null) {
            return mVideoView.getFullWindowPlayer();
        } else {
            return mVideoView;
        }
    }

    private static String getLocalFilePath(String videoPath) {
        return "file:/" + videoPath;
    }

    private class UploadOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!mIsUpload) {
                mVideoUploadManager.startUpload(mVideoPath, Constants.BUCKET, Constants.PROXY_SUFFIX, Constants.PUBLIC_TOKEN, Constants.PRIVATE_TOKEN, Constants.MEDIA_FILE_PREFIX);
                mProgressBarDeterminate.setVisibility(View.VISIBLE);
                mUploadBtn.setText(R.string.cancel_upload);
                mIsUpload = true;
            } else {
                mVideoUploadManager.stopUpload();
                mProgressBarDeterminate.setVisibility(View.INVISIBLE);
                mUploadBtn.setText(R.string.upload);
                mIsUpload = false;
            }
        }
    }

    @Override
    public void onUploadProgress(int percent) {
        mProgressBarDeterminate.setProgress(percent);
        if (1.0 == percent) {
            mProgressBarDeterminate.setVisibility(View.INVISIBLE);
        }
    }

    public void copyToClipboard(String filePath) {
        ClipData clipData = ClipData.newPlainText("VideoFilePath", filePath);
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    public void onUploadSuccess(JSONObject response) {
        try {
            final String filePath = "http://" + Constants.DOMAIN + "/" + response.getString("ETag");
            copyToClipboard(filePath);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(PlaybackActivity.this, "文件上传成功，" + filePath + "已复制到粘贴板");
                }
            });
            mUploadBtn.setVisibility(View.INVISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUploadFail(final JSONObject response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showToast(PlaybackActivity.this, "Upload failed, error = " + response.toString());
            }
        });
    }
}