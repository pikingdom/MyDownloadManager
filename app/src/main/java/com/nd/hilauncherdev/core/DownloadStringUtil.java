package com.nd.hilauncherdev.core;

/**
 * Created by linliangbin on 2016/10/17.
 */

public class DownloadStringUtil {

    public static boolean isEmpty(CharSequence s) {
        return s == null || s.length() <= 0;
    }


    public static String filtrateInsertParam(CharSequence srcParam) {
        if(isEmpty(srcParam)) {
            return "";
        } else {
            String result = srcParam.toString().replace("\'", "\'\'");
            result = result.replace("?", "");
            return result;
        }
    }

}
