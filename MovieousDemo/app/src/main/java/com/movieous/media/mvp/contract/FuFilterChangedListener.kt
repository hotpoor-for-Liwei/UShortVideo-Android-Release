package com.movieous.media.mvp.contract

import com.faceunity.entity.Effect
import com.faceunity.entity.Filter
import com.movieous.media.mvp.model.BeautyEnum

interface FuFilterChangedListener {
    /**
     * Triggered when the effect filter is changed
     */
    fun onEffectSelected(position: Int, effect: Effect)

    /**
     * Triggered when music filter time is changed
     */
    fun onMusicFilterTime(time: Long)

    /**
     * Triggered when face beauty param vale is changed
     */
    fun onBeautyValueChanged(value: Float, beautyType: BeautyEnum)

    /**
     * Triggered when beauty filter is changed
     */
    fun onFilterNameSelected(filterName: Filter)
}
