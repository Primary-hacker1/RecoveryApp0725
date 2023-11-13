package com.example.usbprint

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.usbprint.databinding.ActivityMainBinding
import com.example.usbprint.print.*

class MainActivity : AppCompatActivity() {
    private var mFilePath: String? = null
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.bind(
            LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        )!!
        setContentView(binding.root)
        binding.lifecycleOwner = this
        initpm()
//       在application里面初始化util
        PrintUtil.getInstance(this)
        ConfigUtil.getInstance(this)

//        选择打印机
        binding.settingbt.setOnClickListener {
            PrintUtil.findPrintersWithUSB { printerBean: List<PrinterBean> ->
                showPrinterList(
                    printerBean
                )
            }
        }
    }

    //权限申请
    @RequiresApi(Build.VERSION_CODES.M)
    private fun initpm() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 112233
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112233) {
            if (grantResults.isNotEmpty()) {
                showToast("获取文件权限成功")
            }
        }
    }

    private fun showPrinterList(printerBean: List<PrinterBean>) {
        runOnUiThread {
            binding.printList.removeAllViews()
            for (bean in printerBean) {
                val textView = createTextView()
                textView.text = bean.name
                binding.printList.addView(
                    textView,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                textView.setOnClickListener {
                    showDev(
                        bean
                    )
                }
            }
        }
    }

    private fun createTextView(): TextView {
        val textView = TextView(this)
        textView.setPadding(10, 10, 10, 10)
        return textView
    }

    private fun showDev(printerBean: PrinterBean) {
        PrintUtil.findDriver(printerBean) { dev ->
            runOnUiThread {
                binding.driverList.removeAllViews()
                for (driversSearchEntryBean in dev) {
                    val textView = createTextView()
                    textView.text = driversSearchEntryBean.transportType.name
                    binding.driverList.addView(
                        textView,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    textView.setOnClickListener { v: View? ->
                        showDriverDetail(
                            printerBean,
                            driversSearchEntryBean
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDriverDetail(printerBean: PrinterBean, bean: DriversSearchEntryBean) {
        try {
            binding.driverDetailList.removeAllViews()
            for (mDriverHandle in bean.driverHandlesList) {
                val textView = createTextView()
                textView.text = mDriverHandle.printerName.toString() + "(" + mDriverHandle.getId() + ")"
                binding.driverDetailList.addView(
                    textView,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                textView.setOnClickListener { v: View? ->
                    showToast("正在连接,请稍后...")
                    PrintUtil.connectPrinter(printerBean, bean, mDriverHandle) { status ->
                        if (status) {
                            ConfigUtil.setPrinter(printerBean)
                            ConfigUtil.setDriverSearch(bean)
                            ConfigUtil.setDriverSearchHandle(mDriverHandle)
                            ConfigUtil.setIsConnectPrinterSuccess(true)
                            runOnUiThread { showToast("连接打印机设备成功，请测试打印效果吧。") }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showToast(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == 100 && null != data) {
                //文件选择-回调
                val uri = data.data
                mFilePath = getPath(this, uri)
                binding.showFilePath.text = mFilePath
            }
        }
    }

    @SuppressLint("Recycle")
    private fun getPath(context: Context, uri: Uri?): String? {
        if ("content".equals(uri!!.scheme, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor?
            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                val column_index = cursor!!.getColumnIndexOrThrow("_data")
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index)
                }
            } catch (e: Exception) {
                // Eat it
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    //    测试用，选择文件
    fun openFile(view: View?) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" //设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, 100)
    }

    fun doPrintFile(view: View) {
        if (TextUtils.isEmpty(mFilePath)) {
            Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show()
        } else {
            PrintUtil.doPrintingWithUsb(mFilePath) { status ->
                if (status) {
                    MLog.e("开始打印")
                    showToast("开始打印")
                } else {
                    showToast("打印失败,请连接打印机")
                }
            }
        }
    }

}