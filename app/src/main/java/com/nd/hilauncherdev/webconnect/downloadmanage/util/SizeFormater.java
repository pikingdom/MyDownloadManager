package com.nd.hilauncherdev.webconnect.downloadmanage.util;

import android.content.Context;
import android.text.format.Formatter;

/** 
 * 格式工具类
 * @author pdw 
 * @version 
 * @date 2012-9-21 下午02:38:43 
 */
public class SizeFormater {
	
	public static String getDownloadSize(Context context, long totalSize,int progress) {
		try{
			float downloadSize = totalSize * progress / 100f ;
			return Formatter.formatFileSize(context, (long)downloadSize).replace(" ", "") ;
		} catch (Exception ex) {
			ex.printStackTrace();
			return "0.00MB" ;
		}
	}
	
	public static String getDownloadSize(Context context, long downloadSize) {
		try{
			return Formatter.formatFileSize(context, downloadSize).replace(" ", "") ;
		} catch (Exception ex) {
			ex.printStackTrace();
			return "0.00MB" ;
		}
	}
	
}
