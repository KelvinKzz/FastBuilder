package com.gaoding.fastbuilder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * @description:
 * Created by zhisui on 2022/4/8
 * E-Mail Address: zhisui@gaoding.com
 */
class PhotoGalleryBottomView : ConstraintLayout {

    interface OnPhotoGalleryListener {
        fun openPhotoGallery()
        fun closePhotoGallery()
    }

    private var mTvOpenGallery: TextView? = null
    private var mIvOpenGallery: ImageView? = null

    private var mPhotoGalleryListener: OnPhotoGalleryListener? = null;

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        LayoutInflater.from(context).inflate(R.layout.layout_photo_pick_bottom, this, true)
        mTvOpenGallery = findViewById(R.id.tv_open_gallery)
        mIvOpenGallery = findViewById(R.id.iv_open_gallery)
        setOnClickListener {
            if (mTvOpenGallery?.visibility == View.VISIBLE) {
                mPhotoGalleryListener?.openPhotoGallery()
                updatePhotoGalleryState(true)
            } else {
                mPhotoGalleryListener?.closePhotoGallery()
                updatePhotoGalleryState(false)
            }
        }
    }

    fun setPhotoGalleryListener(photoGalleryListener: OnPhotoGalleryListener?) {
        mPhotoGalleryListener = photoGalleryListener
    }

    fun setPhotoGalleryName(photoGalleryName: String) {
        mTvOpenGallery?.text = photoGalleryName
    }

    fun updatePhotoGalleryState(open: Boolean) {
        if (open) {
            mTvOpenGallery?.visibility = View.GONE
            mIvOpenGallery?.setImageResource(R.drawable.ic_photo_picker_close)
        } else {
            mTvOpenGallery?.visibility = View.VISIBLE
            mIvOpenGallery?.setImageResource(R.drawable.ic_photo_picker_arrow_up)
        }

    }

}