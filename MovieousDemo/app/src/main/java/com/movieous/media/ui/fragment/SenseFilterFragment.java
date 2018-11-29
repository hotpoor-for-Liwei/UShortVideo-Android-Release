package com.movieous.media.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.movieous.media.R;
import com.movieous.media.api.sensesdk.utils.FileUtils;
import com.movieous.media.api.sensesdk.view.StickerItem;
import com.movieous.media.mvp.contract.SenseFilterChangedListener;
import com.movieous.media.ui.adapter.SenseStickerAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 贴纸选择页面
 */
public class SenseFilterFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "SenseFilterFragment";

    private View mContentView;
    private Activity mActivity;
    private LayoutInflater mInflater;
    private RecyclerView mStickersRecycleView;
    private SenseStickerAdapter mStickerAdapter;
    private SenseFilterChangedListener mFilterChangedListener;
    private HashMap<String, ArrayList<StickerItem>> mStickerLists = new HashMap<>();
    private int mCurrentStickerPosition = -1;

    public void setFilterChangedListener(SenseFilterChangedListener listener) {
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
        mContentView = inflater.inflate(R.layout.fragment_effect, container, false);
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initStickerList();
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
    public void onClick(View v) {
        if (mFilterChangedListener != null) {
            int position = Integer.parseInt(v.getTag().toString());
            if (mCurrentStickerPosition == position) {
                mCurrentStickerPosition = -1;
                mFilterChangedListener.onRemoveAllStickers();
            } else {
                mCurrentStickerPosition = position;
                mFilterChangedListener.onChangeSticker(mStickerLists.get("sticker_new").get(position).path);
            }
        }
    }

    private void initView(View view) {
        mStickersRecycleView = view.findViewById(R.id.fu_effect_recycler);
        mStickersRecycleView.setLayoutManager(new GridLayoutManager(view.getContext(), 3));
        mStickersRecycleView.addItemDecoration(new SpaceItemDecoration(0));
        mStickerAdapter = new SenseStickerAdapter(mStickerLists.get("sticker_new"), mActivity);
        mStickerAdapter.setClickStickerListener(this);
        mStickersRecycleView.setAdapter(mStickerAdapter);
    }

    private void initStickerList() {
        mStickerLists.put("sticker_new", FileUtils.getStickerFiles(mActivity, "sensetime"));
        mStickerLists.put("sticker_avatar", FileUtils.getStickerFiles(mActivity, "avatar"));
        mStickerLists.put("sticker_3d", FileUtils.getStickerFiles(mActivity, "3D"));
        mStickerLists.put("sticker_hand_action", FileUtils.getStickerFiles(mActivity, "hand_action"));
        mStickerLists.put("sticker_bg_segment", FileUtils.getStickerFiles(mActivity, "segment"));
        mStickerLists.put("sticker_deformation", FileUtils.getStickerFiles(mActivity, "deformation"));
        mStickerLists.put("sticker_face_morph", FileUtils.getStickerFiles(mActivity, "face_morph"));
        mStickerLists.put("sticker_particle", FileUtils.getStickerFiles(mActivity, "particle"));
        mStickerLists.put("sticker_new_engine", FileUtils.getStickerFiles(mActivity, "newEngine"));
        mStickerLists.put("sticker_test", FileUtils.getStickerFiles(mActivity, "test"));
    }

    // 分隔间距 继承RecyclerView.ItemDecoration
    public static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.top = space;
            }
        }
    }
}
