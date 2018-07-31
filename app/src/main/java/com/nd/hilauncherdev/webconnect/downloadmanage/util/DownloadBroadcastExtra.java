package com.nd.hilauncherdev.webconnect.downloadmanage.util;

import android.content.Context;
import android.content.Intent;

import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadServerService;

/**
 * 下载广播常量说明
 * 
 * @author pdw
 * @version
 * @date 2012-9-20 下午01:41:23
 */
public class DownloadBroadcastExtra {

	/**
	 * 下载唯一标识
	 */
	public static final String EXTRA_IDENTIFICATION = "identification";

	/**
	 * 下载地址参数
	 */
	public static final String EXTRA_DOWNLOAD_URL = "download_url";

	/**
	 * 下载进度
	 */
	public static final String EXTRA_PROGRESS = "progress";

	/**
	 * 已下载字节数
	 */
	public static final String EXTRA_DOWNLOAD_SIZE = "download_size";

	/**
	 * 下载状态
	 */
	public static final String EXTRA_STATE = "state";

	/**
	 * 总大小
	 */
	public static final String EXTRA_TOTAL_SIZE="total_size";
	
	/**
	 * 附加信息
	 */
	public static final String EXTRA_ADDITION = "addition";
	
	/**
	 * 文件类型
	 */
	public static final String EXTRA_FILE_TYPE = "file_type";
	
	/**
	 * <p>
	 * 发送取消下载通知
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-24 下午07:19:47
	 * 
	 * @param ctx
	 * @param url
	 */
	public void sendCancelDownloadBroadcast(Context ctx, String url) {
		if (DownloadServerService.sBroadcastAction != null) {
			Intent intent = new Intent(DownloadServerService.sBroadcastAction);
			intent.putExtra(EXTRA_STATE, DownloadState.STATE_CANCLE);
			intent.putExtra(EXTRA_DOWNLOAD_URL, url);
			ctx.sendBroadcast(intent);
		}
	}
}
