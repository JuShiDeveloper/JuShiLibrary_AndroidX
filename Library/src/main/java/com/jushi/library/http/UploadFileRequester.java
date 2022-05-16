package com.jushi.library.http;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jushi.library.base.BaseApplication;
import com.jushi.library.manager.UserManager;
import com.jushi.library.utils.LogUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * 文件上传 带上传进度
 */
public class UploadFileRequester implements Callback {
    private static final OkHttpClient httpClient;
    private OnUploadListener onUploadListener;
    protected UserManager userManager = BaseApplication.getInstance().getManager(UserManager.class);
    private String url;

    static {
        httpClient = new OkHttpClient().newBuilder()
                .sslSocketFactory(createSSLSocketFactory(),new TrustAllCerts())
                .hostnameVerifier(new TrustAllHostnameVerifier())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (onUploadListener != null)
                onUploadListener.onProgress(msg.arg1);
            return false;
        }
    });

    //自定义SS验证相关类
    private static class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
//            SSLContext sc = SSLContext.getInstance("TLS");
//            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
            ssfFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
        }
        return ssfFactory;
    }

    public void uploadFile(String url, String filePath, OnUploadListener onUploadListener) {
        this.onUploadListener = onUploadListener;
        this.url = url;
        LogUtil.v("文件上传：" + url);
        RequestBody fileBody = getStreamBody(filePath);
        MultipartBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("file", new File(filePath).getName(),fileBody)
//                .addFormDataPart("avatar", file.getName(), RequestBody.create(MediaType.parse(getMimeType(file.getName())), file))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .headers(Headers.of(getHeaders()))
                .post(requestBody)
                .build();
        httpClient.newCall(request).enqueue(this);
    }

    /**
     * 请求头参数
     *
     * @return
     */
    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (userManager.getUserInfo() != null)
            headers.put("Authorization", userManager.getUserInfo().getToken());
        headers.put("Content-Type", "multipart/form-data");
        return headers;
    }

    private RequestBody getStreamBody(final String filePath) {
        return new RequestBody() {

            @Override
            public long contentLength() throws IOException {
                return new File(filePath).length();//若是断点续传则返回剩余的字节数
            }

            @Override
            public MediaType contentType() {
                return MediaType.parse("application/octet-stream");
//                return MediaType.parse("image/png");
                //这个根据上传文件的后缀变化，要是不知道用application/octet-stream
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                //方式一：
                FileInputStream fis = new FileInputStream(new File(filePath));
                fis.skip(0);//跳到指定位置，断点续传
                long writeLength = 0;
                int length;
                byte[] buffer = new byte[2048];
                OutputStream outputStream = sink.outputStream();
                while ((length = fis.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, length);
                    //或者
                    sink.write(buffer, 0, length);
                    writeLength += length;
                    Message message = handler.obtainMessage();
                    message.arg1 = (int) (writeLength * 1.0f / contentLength() * 100);
                    handler.sendMessage(message);
                }
            }
        };
    }


    /**
     * 上传多个文件
     *
     * @param url
     * @param fileNames
     */
    public void uploadFiles(String url, List<String> fileNames) {
        this.url = url;
        LogUtil.v("文件上传：" + url);
        Call call = httpClient.newCall(getRequest(url, fileNames));
        call.enqueue(this);
    }

    /**
     * 获得Request实例
     *
     * @param url
     * @param fileNames 完整的文件路径
     * @return
     */
    private Request getRequest(String url, List<String> fileNames) {
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .headers(Headers.of(getHeaders()))
                .post(getRequestBody(fileNames));
        return builder.build();
    }

    /**
     * 通过上传的文件的完整路径生成RequestBody
     *
     * @param fileNames 完整的文件路径
     * @return
     */
    private RequestBody getRequestBody(List<String> fileNames) {
        //创建MultipartBody.Builder，用于添加请求的数据
        MultipartBody.Builder builder = new MultipartBody.Builder();
        for (int i = 0; i < fileNames.size(); i++) {
            File file = new File(fileNames.get(i));
            String fileType = getMimeType(file.getName());
            builder.addFormDataPart( //给Builder添加上传的文件
                    "avatar",  //请求的名字
                    file.getName(), //文件的名字，服务器端用来解析的
                    RequestBody.create(MediaType.parse(fileType), file) //创建RequestBody，把上传的文件放入
            );
        }
        return builder.build();
    }

    private static String getMimeType(String name) {
        return name.substring(name.lastIndexOf("."), name.length());
    }


    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        onUploadListener.onError(e.getMessage());
        LogUtil.v("文件上传结果： url = " + url + " result = " + e.getMessage());
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        try {
            JSONObject jsonObject = new JSONObject(response.body().string());
            LogUtil.v("文件上传结果： url = " + url + " result = " + jsonObject.toString());
            int code = jsonObject.getInt("code");
            if (code == 200) {
                JSONObject obj = new JSONObject();
                obj.put("data",jsonObject.getString("data"));
                onUploadListener.onSuccess(obj);
            } else {
                onUploadListener.onError(jsonObject.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            onUploadListener.onError(e.getMessage());
        }
    }

    public interface OnUploadListener {
        void onProgress(int progress);

        void onSuccess(JSONObject jsonObject) throws JSONException;

        void onError(String msg);
    }

}
