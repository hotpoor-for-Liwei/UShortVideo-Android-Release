package com.movieous.media.view

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.View

class PreviewGLSurfaceView(context: Context, attrs: AttributeSet) : GLSurfaceView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)
    }
}
