package com.movieous.media.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.movieous.media.R;
import com.movieous.media.base.BaseActivity;
import com.movieous.media.ui.fragment.VideoEditFragment;
import com.movieous.media.utils.AppUtils;
import com.movieous.media.utils.GetPathFromUri;
import com.movieous.media.utils.StringUtils;

public class VideoEditActivity extends BaseActivity {
    public static final String PATH = "Path";
    public static final int REQUEST_CODE_PICK_MEDIA_FILE = 1;

    private VideoEditFragment mVideoEditFragment;
    private String mVideoPath;

    public static void start(Activity activity, String mp4Path) {
        Intent intent = new Intent(activity, VideoEditActivity.class);
        intent.putExtra(PATH, mp4Path);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mVideoPath = getIntent().getStringExtra(PATH);
        super.onCreate(savedInstanceState);
        if (StringUtils.isEmpty(mVideoPath)) {
            startActivityForResult(Intent.createChooser(AppUtils.Companion.getMediaIntent(true), getString(R.string.select_media_file_tip)), VideoEditActivity.REQUEST_CODE_PICK_MEDIA_FILE);
        }
    }

    @Override
    public int layoutId() {
        return StringUtils.isEmpty(mVideoPath) ? 0 : R.layout.activity_base;
    }

    @Override
    public void initData() {
    }

    @Override
    public void initView() {
        if (mVideoEditFragment == null) {
            mVideoEditFragment = VideoEditFragment.getInstance(mVideoPath);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mVideoEditFragment)
                .commit();
    }

    @Override
    public void start() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != VideoEditActivity.REQUEST_CODE_PICK_MEDIA_FILE) {
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            String selectedFilepath = GetPathFromUri.getPath(this, data.getData());
            if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                start(VideoEditActivity.this, selectedFilepath);
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
