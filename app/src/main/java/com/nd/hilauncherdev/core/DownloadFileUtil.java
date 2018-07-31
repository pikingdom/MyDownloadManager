package com.nd.hilauncherdev.core;

import java.io.File;
import java.math.BigDecimal;

/**
 * Created by Administrator on 2016/10/20.
 */
public class DownloadFileUtil {


    public DownloadFileUtil() {
    }


    public static void delFile(String path) {
        try {
            File e = new File(path);
            if (e.exists()) {
                e.delete();
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }


    public static String getMemorySizeString(long size) {
        float result = (float) size;
        BigDecimal temp;
        if (result < 1024.0F) {
            temp = new BigDecimal((double) result);
            temp = temp.setScale(2, 4);
            return temp + "Bytes";
        } else {
            result /= 1024.0F;
            if (result < 1024.0F) {
                temp = new BigDecimal((double) result);
                temp = temp.setScale(2, 4);
                return temp + "KB";
            } else {
                result /= 1024.0F;
                if (result < 1024.0F) {
                    temp = new BigDecimal((double) result);
                    temp = temp.setScale(2, 4);
                    return temp + "MB";
                } else {
                    result /= 1024.0F;
                    if (result < 1024.0F) {
                        temp = new BigDecimal((double) result);
                        temp = temp.setScale(2, 4);
                        return temp + "GB";
                    } else {
                        result /= 1024.0F;
                        temp = new BigDecimal((double) result);
                        temp = temp.setScale(2, 4);
                        return temp + "TB";
                    }
                }
            }
        }
    }


}
