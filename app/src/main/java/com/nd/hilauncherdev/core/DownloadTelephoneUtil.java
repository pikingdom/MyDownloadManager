package com.nd.hilauncherdev.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * 获取手机相关参数的工具类
 */
public class DownloadTelephoneUtil {

    /**
     * 网络是否可用
     *
     * @param context
     * @return boolean
     */
    public synchronized static boolean isNetworkAvailable(Context context) {
        boolean result = false;
        if (context == null) {
            return result;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (null != connectivityManager) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
            if (null != networkInfo && networkInfo.isAvailable() && networkInfo.isConnected()) {
                result = true;
            }
        }
        return result;
    }

    /**
     * wifi是否启动
     *
     * @param ctx
     * @return boolean
     */
    public static boolean isWifiEnable(Context ctx) {
        if (ctx == null) {
            return false;
        }
        try {
            return isWifiNetwork(ctx);
        } catch (Exception e) {
            e.printStackTrace();
            return isWifiOpen(ctx);
        }
    }

    private static boolean isWifiOpen(Context ctx) {
        try {
            Object obj = ctx.getSystemService(Context.WIFI_SERVICE);
            if (obj == null)
                return false;

            WifiManager wifiManager = (WifiManager) obj;
            return wifiManager.isWifiEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isWifiNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }


}
