package com.creator.networktest

import android.app.Activity
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.creator.networktest.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

class MainActivity : Activity(), View.OnClickListener {
    private lateinit var info: Info
    private var flag = false
    private var lastDegree = 0
    private var curDegree = 0
    private var testing = false
    private lateinit var mBinding: ActivityMainBinding

    private val handler = Handler(Looper.myLooper()!!) { msg ->
        if (msg.what == 0x123) {
            mBinding.nowSpeed = msg.arg1.toString() + "KB/S"
            mBinding.aveSpeed = msg.arg2.toString() + "KB/S"
            startAnimation(msg.arg1)
        }
        if (msg.what == 0x100) {
            mBinding.nowSpeed = ("0KB/S")
            startAnimation(0)
            mBinding.btnText = "开始测试"
            mBinding.ping = ""
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initData()
        initView()
    }

    private fun initData() {
        info = Info()

    }

    private fun initView() {
        mBinding.onClickListener = this
        mBinding.btnText = "开始测试"
    }

    internal inner class DownloadThread : Thread() {
        override fun run() {
            val urlString = "https://dl.google.com/dl/android/studio/install/3.5.2.0/android-studio-ide-191.5977832-windows.exe"
            val startTime: Long
            var curTime: Long
            val url: URL
            val connection: URLConnection
            val iStream: InputStream
            try {
                url = URL(urlString)
                connection = url.openConnection()
                info.totalByte = connection.contentLength
                iStream = connection.getInputStream()
                startTime = System.currentTimeMillis()
                while (iStream.read() != -1 && flag && testing) {
                    info.hadfinishByte++
                    curTime = System.currentTimeMillis()
                    if (curTime - startTime == 0L) {
                        info.speed = 1000.0
                    } else {
                        info.speed = (info.hadfinishByte / (curTime - startTime) * 1000).toDouble()
                    }
                }
                iStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal inner class GetInfoThread : Thread() {
        override fun run() {
            var sum: Double
            var counter: Double
            var curSpeed: Int
            var aveSpeed: Int
            try {
                sum = 0.0
                counter = 0.0
                while (info.hadfinishByte < info.totalByte && flag && testing) {
                    sleep(1000)
                    sum += info.speed
                    counter++
                    curSpeed = info!!.speed.toInt()
                    aveSpeed = (sum / counter).toInt()
                    checkNetQuality()
                    Log.e("Test", "cur_speed:" + info.speed / 1024 + "KB/S ave_speed:" + aveSpeed / 1024)
                    val msg = Message()
                    msg.arg1 = info.speed.toInt() / 1024
                    msg.arg2 = aveSpeed / 1024
                    msg.what = 0x123
                    handler.sendMessage(msg)
                }
                if (info.hadfinishByte == info.totalByte && flag) {
                    handler.sendEmptyMessage(0x100)
                }
                if (!testing) {
                    handler.sendEmptyMessage(0x100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        flag = false
    }

    override fun onResume() {
        flag = true
        super.onResume()
    }

    private fun startAnimation(cur_speed: Int) {
        curDegree = getDegree(cur_speed.toDouble())
        val rotateAnimation = RotateAnimation(lastDegree.toFloat(), curDegree.toFloat(), Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.fillAfter = true
        rotateAnimation.duration = 1000
        lastDegree = curDegree
        mBinding.ivNeedle.startAnimation(rotateAnimation)
    }

    private fun getDegree(cur_speed: Double): Int {
        var ret = 0
        ret = if (cur_speed in 0.0..512.0) {
            (cur_speed / 512.0 * 60).toInt()
        } else if (cur_speed in 512.0..1024.0) {
            (60 + (cur_speed - 512) / 512 * 30).toInt()
        } else if (cur_speed in 1024.0..1024.0 * 5) {
            (90 + (cur_speed - 1024) / (1024.0 * 4) * 60).toInt()
        } else if (cur_speed in 1024.0 * 5..1024.0 * 10) {
            (150 + (cur_speed - 1024 * 5) / (1024.0 * 5) * 30).toInt()
        } else {
            180
        }
        return ret
    }

    private fun checkNetQuality() {
        object : Thread() {
            override fun run() {
                super.run()
                var delay = String()
                var p: Process? = null
                try {
                    p = Runtime.getRuntime().exec("/system/bin/ping -c 4 " + "118.31.245.20")
                    val buf = BufferedReader(InputStreamReader(p.inputStream))
                    var str = String()
                    while (buf.readLine()?.also { str = it } != null) {
                        if (str.contains("avg")) {
                            val i = str.indexOf("/", 20)
                            val j = str.indexOf(".", i)
                            delay = str.substring(i + 1, j)
                        }
                    }
                    if (!testing) {
                        return
                    }
                    if (delay == "") {
                        val finalDelay1 = delay
                        handler.post { mBinding.ping = finalDelay1 }
                    } else {
                        val finalDelay2 = delay
                        handler.post { mBinding.ping = finalDelay2 }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onClick(v: View?) {
        val id = v!!.id
        if (id == R.id.btn_start) {
            if (testing) {
                testing = false
            } else {
                val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager.activeNetworkInfo
                mBinding.type = networkInfo!!.typeName
                testing = true
                mBinding.btnText = "停止测试"
                info.hadfinishByte = 0
                info.speed = 0.0
                info.totalByte = 1024
                DownloadThread().start()
                GetInfoThread().start()
            }
        }

    }
}