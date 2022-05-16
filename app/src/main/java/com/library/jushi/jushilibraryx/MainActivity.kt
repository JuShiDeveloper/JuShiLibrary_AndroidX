package com.library.jushi.jushilibraryx

import android.content.Intent
import android.util.Log
import com.jushi.library.base.BaseFragmentActivity
import com.jushi.library.customView.messageInput.MessageInputView
import com.jushi.library.http.DownloadFileRequester
import com.jushi.library.http.UploadFileRequester
import kotlinx.android.synthetic.main.activity_main1.*
import org.json.JSONException
import org.json.JSONObject


class MainActivity : BaseFragmentActivity(), MessageInputView.OnMessageInputListener {

    override fun getLayoutResId(): Int {
        setSystemBarStatus(true, true, true)
        return R.layout.activity_main1
    }

    override fun initView() {
//        setSystemBarViewLayoutParamsL(v_system_bar)
        Log.v("yufei", "activity initView()")
    }

    override fun initData() {
//        Log.v("yufei", DateUtil.compare("2012-12-01 12:12:12", "2012-12-01 12:12:11").toString())
//        Log.v("yufei", DateUtil.getDateYMD(DateUtil.dateCurrentLong()))
//        Log.v("yufei", DateUtil.getDateYMDHMS(DateUtil.dateCurrentLong()))
//        Log.v("yufei", DateUtil.getDateYMDHM(DateUtil.dateCurrentLong()))
//        Log.v("yufei", DateUtil.getDateLong(Date()).toString())
//        Log.v("yufei", DateUtil.getDateLong("2012-12-01 12:12:12").toString())
        Log.v("yufei", "activity initialize()")
        //
        val fileName = "/storage/emulated/0/MagazineUnlock/magazine-unlock-03-2.3.5162-6273E3E4F9C121EBE427369BC2A3434F.jpg"
        val jsonObject = JSONObject()
        try {
            jsonObject.put("op_type", 3015)
            jsonObject.put("user_id", 1008930)
            jsonObject.put("task_id", "")
            jsonObject.put("c_type", 1)
            jsonObject.put("pid", 5)
            jsonObject.put("sid", "db576a7d2453575f29eab4bac787b919")
            jsonObject.put("c_ver", "9010")
            jsonObject.put("act_type", 1)
            jsonObject.put("ext", "jpg")
            jsonObject.put("gender", 1)
            jsonObject.put("file_size", 160702)
            jsonObject.put("file_name", "" + System.currentTimeMillis())
            jsonObject.put("buss_type", 2)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val url = "http://test.guijk.com/hfs/?json=$jsonObject"
        val list = ArrayList<String>()
        list.add(fileName)
        btn.setOnClickListener {
            tv_progress.text = "0%"
            UploadFileRequester().uploadFile(url, fileName, object : UploadFileRequester.OnUploadListener {
                override fun onSuccess(jsonObject: JSONObject?) {
                    Log.v("yufei", "onSucess")
                } //文件上传
                override fun onProgress(progress: Int) {
                    runOnUiThread { tv_progress.text = "$progress%" }
                    Log.v("yufei", "$progress%")
                }

                override fun onError(msg: String?) {
                    Log.v("yufei", "onError  $msg")
                }
            })
        }

        downLoadFile()
    }

    private fun downLoadFile() { //文件下载
        Log.v("yufei", "文件下载")
        var url = "http://test.guijk.com/hfs/3015/5/1598580272286732/jpg/2/1008930/o"
        var savePath = externalCacheDir.path + "/download"
        var fileName = "testDownload.jpg"
        DownloadFileRequester().download(url, savePath, fileName, object : DownloadFileRequester.OnDownloadListener {
            override fun onSuccess(filePath: String?) {
                Log.v("yufei", "onSucess")
            }

            override fun onProgress(progress: Int) {
                Log.v("yufei", "$progress%")
            }

            override fun onError(code: Int, msg: String?) {
                Log.v("yufei", "onError  $msg")
            }
        })
    }

    override fun setListener() {
        test_float.setOnClickListener { v ->
            showToast("点击")
            startActivity(Intent(this, MainActivity::class.java))
        }

        val images = java.util.ArrayList<Int>()
        images.add(R.drawable.ic_replay_press)
        images.add(R.drawable.ic_recommend_press)
        images.add(R.drawable.ic_msg_referral_press)
        images.add(R.drawable.ic_prescription_light)

        val titles = java.util.ArrayList<String>()
        titles.add("模板")
        titles.add("推荐")
        titles.add("转诊")
        titles.add("开处方")
        MessageInputView.setMoreFunction(images, titles)
        MessageInputView.setOnMessageInputListener(this)
    }

    override fun onSend(msg: String) {
        showToast("发送消息：$msg")
    }

    override fun onMoreBtnClick() {
        hideSoftInput()
    }

    override fun onRecordAudio() {
        showToast("录音")
    }

    override fun onMoreFunctionClick(functionTitle: String) {
        showToast("点击：$functionTitle")
    }
}
