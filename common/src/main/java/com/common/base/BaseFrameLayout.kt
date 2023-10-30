package com.common.base

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding


abstract class BaseFrameLayout<Vb : ViewDataBinding> : FrameLayout {

    protected val TAG = BaseFrameLayout::class.java.simpleName

    lateinit var baseBinding: Vb
    lateinit var mContext: Context

    protected val _binding get() = baseBinding
    constructor(context: Context) : super(context) {
        mContext = context
        var layoutInflater = LayoutInflater.from(context)
        baseBinding = DataBindingUtil.inflate(layoutInflater, getLayout(), this, true)
        initView()
    }

    constructor(context: Context, attributes: AttributeSet) : super(context, attributes) {
        mContext = context
        var layoutInflater = LayoutInflater.from(context)
        baseBinding = DataBindingUtil.inflate(layoutInflater, getLayout(), this, true)
        initView()
    }

    constructor(context: Context, attributes: AttributeSet, int: Int) : super(
        context,
        attributes,
        int
    ) {
        mContext = context
        var layoutInflater = LayoutInflater.from(context)
        baseBinding = DataBindingUtil.inflate(layoutInflater, getLayout(), this, true)
        initView()
    }


    abstract fun initView()

    @LayoutRes
    protected abstract fun getLayout(): Int

}