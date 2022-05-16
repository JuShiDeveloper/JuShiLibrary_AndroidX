package com.jushi.library.http;


import androidx.annotation.NonNull;

import com.jushi.library.base.BaseApplication;
import com.jushi.library.base.BaseManager;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;


/**
 * 提供给自定义类继承的HTTP请求基类
 */
public abstract class BaseHttpRequester<Data> extends BaseHttp {
    private OnHttpResponseListener<Data> responseListener;

    public BaseHttpRequester(@NonNull OnHttpResponseListener<Data> listener) {
        responseListener = listener;
    }

    @Override
    protected String onHttpUrl() {
        return HttpUrlConfig.BASE_URL + onRequestRouter();
    }

    @Override
    protected void onRequestSuccess(int code, String message, JSONObject jsonObject, Response response) throws JSONException {
        JSONObject dataObj = null;
        if (code == 401) {//未登录或登录信息过期
            loginOverdue(response.request().url().toString());
            return;
        }
        if (jsonObject.has("data")) {
            dataObj = jsonObject.getJSONObject("data");
        }
        JSONObject finalDataObj = dataObj == null ? jsonObject : dataObj;
        BaseApplication.getInstance().getHandler().post(() -> {
            try {
                if (code == 200) {
                    responseListener.onHttpRequesterResponse(code, onRequestRouter(), message, onDumpData(finalDataObj));
                } else {
                    onError(code, message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                onError(-1, "Json 转换异常");
            }
        });
    }

    @Override
    protected void onError(int code, String errorMsg) {
        BaseApplication.getInstance().getHandler().post(() -> {
            responseListener.onHttpRequesterError(code, onRequestRouter(), errorMsg);
        });
    }

    /**
     * 登录失效或未登录
     *
     * @param s
     */
    protected void loginOverdue(String s) {
    }

    /**
     * 返回请求的服务器路由(或者方法)
     *
     * @return 例： "/login"
     */
    protected abstract String onRequestRouter();

    /**
     * 请求成功 子类在该方法中做数据解析操作
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    protected abstract Data onDumpData(JSONObject jsonObject) throws JSONException;

    /**
     * 请求失败  子类在该方法中做数据解析操作
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    protected abstract Data onDumpDataError(JSONObject jsonObject) throws JSONException;

    protected <V extends BaseManager> V getManager(Class<V> cls) {
        return BaseApplication.getInstance().getManager(cls);
    }
}
