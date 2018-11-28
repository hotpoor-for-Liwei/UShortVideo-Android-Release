package com.movieous.media.ui.adapter;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.faceunity.entity.Effect;
import com.movieous.media.R;
import com.movieous.media.mvp.contract.FuFilterChangedListener;
import com.movieous.media.mvp.model.EffectEnum;
import com.movieous.media.view.CircleImageView;

import java.io.IOException;
import java.util.List;

public class EffectFilterAdapter extends RecyclerView.Adapter<EffectFilterAdapter.HomeRecyclerHolder> {
    private static final String TAG = EffectFilterAdapter.class.getSimpleName();

    private Context mContext;
    private int mEffectType;
    private List<Effect> mEffects;
    private int mPositionSelect = -1;
    private FuFilterChangedListener mFilterChangedListener;

    public EffectFilterAdapter(Context context, int effectType, int position, FuFilterChangedListener listener) {
        mContext = context;
        mEffectType = effectType;
        mPositionSelect = position;
        mFilterChangedListener = listener;
        mEffects = EffectEnum.getEffectsByEffectType(mEffectType);
    }

    @Override
    public HomeRecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HomeRecyclerHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_effect_recycler, parent, false));
    }

    @Override
    public void onBindViewHolder(HomeRecyclerHolder holder, final int position) {
        holder.effectImg.setImageResource(mEffects.get(position).resId());
        holder.effectImg.setBackgroundResource(mPositionSelect == position ? R.drawable.effect_select : 0);
        holder.effectImg.setOnClickListener(v -> {
            if (mPositionSelect == position) {
                mPositionSelect = -1;
                mFilterChangedListener.onEffectSelected(0, EffectEnum.getEffectsByEffectType(0).get(0));
            } else {
                Effect effect = mEffects.get(mPositionSelect = position);
                mFilterChangedListener.onEffectSelected(position, effect);
                playMusic(effect);
            }
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return mEffects.size();
    }

    class HomeRecyclerHolder extends RecyclerView.ViewHolder {

        CircleImageView effectImg;

        public HomeRecyclerHolder(View itemView) {
            super(itemView);
            effectImg = itemView.findViewById(R.id.effect_recycler_img);
        }
    }

    public void onResume() {
        playMusic(mEffects.get(mPositionSelect));
    }

    public void onPause() {
        stopMusic();
    }

    public Effect getSelectEffect() {
        return mEffects.get(mPositionSelect);
    }

    private MediaPlayer mediaPlayer;
    private Handler mMusicHandler;
    private static final int MUSIC_TIME = 50;
    private Runnable mMusicRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mFilterChangedListener.onMusicFilterTime(mediaPlayer.getCurrentPosition());
            }
            mMusicHandler.postDelayed(mMusicRunnable, MUSIC_TIME);
        }
    };

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            mMusicHandler.removeCallbacks(mMusicRunnable);

        }
    }

    public void playMusic(Effect effect) {
        if (mEffectType != Effect.EFFECT_TYPE_MUSIC_FILTER) {
            return;
        }
        stopMusic();

        if (effect.effectType() != Effect.EFFECT_TYPE_MUSIC_FILTER) {
            return;
        }
        mediaPlayer = new MediaPlayer();
        mMusicHandler = new Handler();

        /**
         * mp3
         */
        try {
            AssetFileDescriptor descriptor = mContext.getAssets().openFd("musicfilter/" + effect.bundleName() + ".mp3");
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
            });
            mediaPlayer.setOnPreparedListener(mp -> {
                // 装载完毕回调
                //mediaPlayer.setVolume(1f, 1f);
                mediaPlayer.setLooping(false);
                mediaPlayer.seekTo(0);
                mediaPlayer.start();

                mMusicHandler.postDelayed(mMusicRunnable, MUSIC_TIME);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            });
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer = null;
        }
    }

}