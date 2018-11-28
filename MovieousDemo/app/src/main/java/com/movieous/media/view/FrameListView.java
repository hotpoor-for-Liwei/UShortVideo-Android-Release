package com.movieous.media.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.movieous.base.UVideoFrame;
import com.movieous.media.Constants;
import com.movieous.media.R;
import com.movieous.shortvideo.UMediaFile;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

public class FrameListView extends FrameLayout {
    private Context mContext;
    private RecyclerView mFrameList;
    private ObservableHorizontalScrollView mScrollView;
    private FrameSelectorView mCurSelectorView;
    private UMediaFile mMediaFile;
    private FrameLayout mScrollViewParent;
    private ViewGroup mFrameListParent;
    private HashMap<ClipItem, View> mClipsMap = new HashMap<>();
    private OnVideoFrameScrollListener mOnVideoFrameScrollListener;
    private OnBindViewListener mOnBindViewListener;

    private boolean mIsKeyFrame = false;
    private boolean mIsTimeMode = true;
    private long mDurationMs;
    private long mShowFrameIntervalMs;
    private int mFrameWidth;
    private int mFrameHeight;

    public FrameListView(@NonNull Context context) {
        super(context);
    }

    public FrameListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        View view = LayoutInflater.from(context).inflate(R.layout.frame_list_view, this);
        mFrameList = view.findViewById(R.id.recycler_frame_list);
        mScrollView = view.findViewById(R.id.scroll_view);
        mScrollViewParent = findViewById(R.id.scroll_view_parent);
        mFrameListParent = findViewById(R.id.recycler_parent);
    }

    private void initFrameList() {
        mFrameList.setAdapter(new FrameListAdapter());
        mFrameList.setItemViewCacheSize(getShowFrameCount());
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        mFrameList.setLayoutManager(layoutManager);
        mScrollView.setOnScrollListener(new OnViewScrollListener());
    }

    public void setGetFrameMode(boolean keyFrame, boolean timeMode) {
        mIsKeyFrame = keyFrame;
        mIsTimeMode = timeMode;
    }

    public void setVideoPath(String path) {
        mMediaFile = new UMediaFile(path);
        mDurationMs = mMediaFile.getDurationMs();
        // if the duration time > 10s, the interval time is 3s, else is 1s
        mShowFrameIntervalMs = (mDurationMs >= 1000 * 10) ? 3000 : 1000;

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mFrameWidth = mFrameHeight = wm.getDefaultDisplay().getWidth() / 6;

        initFrameList();
    }

    private int getTotalScrollLength() {
        return getShowFrameCount() * mFrameWidth;
    }

    private int getShowFrameCount() {
        return mIsTimeMode ?
                (int) Math.ceil((float) mDurationMs / mShowFrameIntervalMs) :
                mMediaFile.getVideoFrameCount(mIsKeyFrame);
    }

    private int getScrollLengthByTime(long time) {
        return (int) ((float) getTotalScrollLength() * time / mDurationMs);
    }

    public void scrollToTime(long time) {
        int scrollLength = getScrollLengthByTime(time);
        mScrollView.smoothScrollTo(scrollLength, 0);
    }

    public FrameSelectorView addSelectorView() {
        mCurSelectorView = new FrameSelectorView(mContext);
        final LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, mFrameListParent.getHeight());
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        mCurSelectorView.setVisibility(View.INVISIBLE);
        mScrollViewParent.addView(mCurSelectorView, layoutParams);

        mCurSelectorView.post(() -> {
            // put mCurSelectorView to the middle of the horizontal
            layoutParams.leftMargin = (mScrollViewParent.getWidth() - mCurSelectorView.getWidth()) / 2;
            mCurSelectorView.setLayoutParams(layoutParams);
            mCurSelectorView.setVisibility(View.VISIBLE);
        });
        return mCurSelectorView;
    }

    private int getHalfGroupWidth() {
        return mFrameWidth * 3;
    }

    public View addSelectedRect(View view) {
        mCurSelectorView = (FrameSelectorView) view;
        if (mCurSelectorView == null) {
            return null;
        }
        int leftBorder = mCurSelectorView.getBodyLeft();
        int rightBorder = mCurSelectorView.getBodyRight();
        int width = mCurSelectorView.getBodyWidth();

        boolean outOfLeft = leftBorder <= getHalfGroupWidth() - mScrollView.getScrollX();
        boolean outOfRight = rightBorder >= getHalfGroupWidth() + (getTotalScrollLength() - mScrollView.getScrollX());

        if (outOfLeft && !outOfRight) {
            leftBorder = getHalfGroupWidth() - mScrollView.getScrollX();
            width = rightBorder - leftBorder;
        } else if (!outOfLeft && outOfRight) {
            width = width - (rightBorder - getHalfGroupWidth() - (getTotalScrollLength() - mScrollView.getScrollX()));
        } else if (outOfLeft && outOfRight) {
            leftBorder = getHalfGroupWidth() - mScrollView.getScrollX();
            width = getTotalScrollLength();
        }

        if (width <= 0) {
            mCurSelectorView.setVisibility(View.GONE);
            return null;
        }

        final View rectView = new View(mContext);
        rectView.setBackground(getResources().getDrawable(R.drawable.frame_selector_rect));
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, mFrameListParent.getHeight());

        int leftPosition = leftBorder + mScrollView.getScrollX();
        int rightPosition = leftPosition + width;

        layoutParams.leftMargin = leftPosition;
        mFrameListParent.addView(rectView, layoutParams);

        mCurSelectorView.setVisibility(View.GONE);

        ClipItem item = addClip(leftPosition, rightPosition);
        rectView.setTag(item);
        mClipsMap.put(item, rectView);

        //TODO
        mCurSelectorView = null;

        return rectView;
    }

    public ClipItem getClipByRectView(View view) {
        return (ClipItem) view.getTag();
    }

    public void showSelectorByRectView(FrameSelectorView selectorView, View rectView) {
        selectorView.setVisibility(VISIBLE);
        int leftPosition = (int) (rectView.getX() - mScrollView.getScrollX() - selectorView.getLeftHandlerWidth());
        selectorView.setBodyLeft(leftPosition);
        selectorView.setBodyWidth(rectView.getWidth());
    }

    public void removeRectView(View view) {
        Iterator<HashMap.Entry<ClipItem, View>> it = mClipsMap.entrySet().iterator();

        while (it.hasNext()) {
            HashMap.Entry<ClipItem, View> entry = it.next();
            if (entry.getValue() == view) {
                View rectView = entry.getValue();
                mFrameListParent.removeView(rectView);
                it.remove();
            }
        }
    }

    public void removeSelectorView(FrameSelectorView selectorView) {
        mScrollViewParent.removeView(selectorView);
    }

    private long getTimeByPosition(int position) {
        position = position - getHalfGroupWidth();
        return (long) ((float) mDurationMs * position / getTotalScrollLength());
    }

    private ClipItem addClip(int leftPosition, int rightPosition) {
        String path = Constants.VIDEO_STORAGE_DIR + "clip-" + System.currentTimeMillis() + ".mp4";
        long startTime = getTimeByPosition(leftPosition);
        long endTime = getTimeByPosition(rightPosition);

        ClipItem clipItem = new ClipItem(startTime, endTime, path);
        return clipItem;
    }

    private class OnViewScrollListener implements ObservableHorizontalScrollView.OnScrollListener {
        @Override
        public void onScrollChanged(ObservableHorizontalScrollView scrollView, final int x, int y, int oldX, int oldY, boolean dragScroll) {
            if (dragScroll && mOnVideoFrameScrollListener != null) {
                int timeMs = (int) (x * mDurationMs / (getShowFrameCount() * mFrameWidth));
                mOnVideoFrameScrollListener.onVideoFrameScrollChanged(timeMs);
            }
        }
    }

    public interface OnVideoFrameScrollListener {
        void onVideoFrameScrollChanged(long timeMs);
    }

    public interface OnBindViewListener {
        void onBindViewHolder(ItemViewHolder holder);
    }

    public void setOnVideoFrameScrollListener(OnVideoFrameScrollListener listener) {
        mOnVideoFrameScrollListener = listener;
    }

    public void setOnBindViewListener(OnBindViewListener listener) {
        mOnBindViewListener = listener;
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.thumbnail);
        }
    }

    private class FrameListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            View contactView = inflater.inflate(R.layout.item_devide_frame, parent, false);
            ItemViewHolder viewHolder = new ItemViewHolder(contactView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, final int position) {
            LayoutParams params = new LayoutParams(mFrameWidth, mFrameHeight);
            params.width = mFrameWidth;
            holder.mImageView.setLayoutParams(params);
            if (mOnBindViewListener != null) {
                mOnBindViewListener.onBindViewHolder(holder);
            }

            // there are 6 dark frames in begin and end sides
            if (position == 0 ||
                    position == 1 ||
                    position == 2 ||
                    position == getShowFrameCount() + 3 ||
                    position == getShowFrameCount() + 4 ||
                    position == getShowFrameCount() + 5) {
                holder.mImageView.setImageBitmap(null);
                holder.mImageView.setBackgroundResource(R.color.transparent);
                return;
            }

            int index = position - 3;
            if (mIsTimeMode) {
                long frameTime = index * mShowFrameIntervalMs;
                new ImageViewTask(holder.mImageView, frameTime, mFrameWidth, mFrameHeight, mMediaFile, mIsKeyFrame).execute();
            } else {
                new ImageViewTask(holder.mImageView, index, mFrameWidth, mFrameHeight, mMediaFile, mIsKeyFrame).execute();
            }
            holder.mImageView.setTag(holder.mImageView.getId(), index);
        }

        @Override
        public int getItemCount() {
            return getShowFrameCount() + 6;
        }
    }

    private static class ImageViewTask extends AsyncTask<Void, Void, UVideoFrame> {
        private WeakReference<ImageView> mImageViewWeakReference;
        private int mIndex;
        private long mFrameTime;
        private int mFrameWidth;
        private int mFrameHeight;
        private boolean mIsKeyFrame;
        private boolean mIsTimeMode;
        private UMediaFile mMediaFile;

        ImageViewTask(ImageView imageView, long frameTime, int frameWidth, int frameHeight, UMediaFile mediaFile, boolean isKeyFrame) {
            mImageViewWeakReference = new WeakReference<>(imageView);
            mFrameTime = frameTime;
            mFrameWidth = frameWidth;
            mFrameHeight = frameHeight;
            mMediaFile = mediaFile;
            mIsKeyFrame = isKeyFrame;
            mIsTimeMode = true;
        }

        ImageViewTask(ImageView imageView, int index, int frameWidth, int frameHeight, UMediaFile mediaFile, boolean isKeyFrame) {
            mImageViewWeakReference = new WeakReference<>(imageView);
            mIndex = index;
            mFrameWidth = frameWidth;
            mFrameHeight = frameHeight;
            mMediaFile = mediaFile;
            mIsKeyFrame = true;
            mIsTimeMode = false;
        }

        @Override
        protected UVideoFrame doInBackground(Void... v) {
            UVideoFrame frame = mIsTimeMode ?
                    mMediaFile.getVideoFrameByTime(mFrameTime, mIsKeyFrame, mFrameWidth, mFrameHeight) :
                    mMediaFile.getVideoFrameByIndex(mIndex, mIsKeyFrame, mFrameWidth, mFrameHeight);
            return frame;
        }

        @Override
        protected void onPostExecute(UVideoFrame frame) {
            super.onPostExecute(frame);
            ImageView imageView = mImageViewWeakReference.get();
            if (imageView == null) {
                return;
            }
            if (frame != null) {
                int rotation = frame.getRotation();
                Bitmap bitmap = frame.toBitmap();
                imageView.setImageBitmap(bitmap);
                imageView.setRotation(rotation);
            }
        }
    }

    public class ClipItem {
        long mStartTime;
        long mEndTime;
        String mVideoPath;

        public ClipItem(long startTime, long endTime, String videoPath) {
            mStartTime = startTime;
            mEndTime = endTime;
            mVideoPath = videoPath;
        }

        public long getStartTime() {
            return mStartTime;
        }

        public long getEndTime() {
            return mEndTime;
        }
    }
}
