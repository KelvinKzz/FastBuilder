package com.gaoding.fastbuilder

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * @description:半圆矩形 矩形两边是半圆显示
 * Created by zhisui on 2022/4/8
 * E-Mail Address: zhisui@gaoding.com
 */
class RoundRectLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var shadowRadius: Float

    @ColorInt
    private var shadowColor: Int
    private var paint: Paint

    @ColorInt
    private var backgroundColor: Int
    private val path = Path()
    private val mRectF = RectF()

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.RoundRectLayout, 0, 0)

        shadowColor = attributes.getColor(
            R.styleable.RoundRectLayout_rrl_shadow_color,
            resources.getColor(R.color.cardview_shadow_end_color)
        )
        shadowRadius =
            attributes.getDimensionPixelSize(R.styleable.RoundRectLayout_rrl_shadow_radius, 0)
                .toFloat()
        backgroundColor =
            attributes.getColor(R.styleable.RoundRectLayout_rrl_background_color, Color.WHITE)
        attributes.recycle()
        paint = Paint()
        paint.color = backgroundColor
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        paint.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        val width = width
        val height = height
        mRectF[shadowRadius, shadowRadius, width - shadowRadius] = height - shadowRadius
        val minLen = Math.min(width, height)
        canvas.drawRoundRect(mRectF, minLen.toFloat(), minLen.toFloat(), paint)
    }

    override fun setBackgroundColor(@ColorInt backgroundColor: Int) {
        this.backgroundColor = backgroundColor
        invalidate()
    }
}