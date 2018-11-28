package com.movieous.media.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.movieous.media.R;
import com.movieous.media.api.sensesdk.view.StickerItem;

import java.util.ArrayList;

public class SenseStickerAdapter extends RecyclerView.Adapter {

    ArrayList<StickerItem> mStickerList;
    private View.OnClickListener mOnClickStickerListener;
    private int mSelectedPosition = -1;
    Context mContext;

    public SenseStickerAdapter(ArrayList<StickerItem> list, Context context) {
        mStickerList = list;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter, null);
        return new FilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final FilterViewHolder viewHolder = (FilterViewHolder) holder;
        boolean isSelected = mSelectedPosition == position;
        viewHolder.imageView.setImageBitmap(mStickerList.get(position).icon);
        viewHolder.imageView.setBackgroundResource(isSelected ? R.drawable.control_filter_select : 0);
        holder.itemView.setSelected(isSelected);
        if (mOnClickStickerListener != null) {
            holder.itemView.setTag(position);
            holder.itemView.setOnClickListener(mOnClickStickerListener);
            holder.itemView.setSelected(isSelected);
        }
    }

    public void setClickStickerListener(View.OnClickListener listener) {
        mOnClickStickerListener = listener;
    }

    @Override
    public int getItemCount() {
        return mStickerList.size();
    }

    static class FilterViewHolder extends RecyclerView.ViewHolder {

        View view;
        ImageView imageView;

        public FilterViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            imageView = itemView.findViewById(R.id.icon);
        }
    }

    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
    }
}
