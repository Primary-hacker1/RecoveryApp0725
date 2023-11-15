package com.rick.recoveryapp.ui.dialog

import android.app.Dialog
import com.rick.recoveryapp.R
import com.rick.recoveryapp.base.BaseDialogFragment
import com.rick.recoveryapp.databinding.DialogFragmentBinding

/*
* 通用dialogFragment
* */
class DialogFragment : BaseDialogFragment<DialogFragmentBinding>() {

    public var dialogFragment: DialogListener? = null

    override fun start(dialog: Dialog?) {

    }

    fun setOnClickListener(dialogFragment: DialogListener){
        this.dialogFragment = dialogFragment;
    }

    override fun initView() {
    }

    override fun initListener() {
        binding.dialogClose.setOnClickListener {
            dialogFragment?.closeClick()
        }

        binding.dialogSave.setOnClickListener {
            dialogFragment?.saveClick()
        }
    }

    override fun initData() {
    }

    override fun getLayout(): Int {
        return R.layout.dialog_fragment
    }

    interface DialogListener {
        fun saveClick()
        fun closeClick()
    }
}