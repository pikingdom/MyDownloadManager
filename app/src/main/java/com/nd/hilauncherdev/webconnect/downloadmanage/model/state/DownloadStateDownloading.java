package com.nd.hilauncherdev.webconnect.downloadmanage.model.state;

import android.content.Context;

import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadInfo;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadDBManager;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;

import java.io.File;

/** 
 * 正在下载状态
 * 
 * @author pdw  
 * @version 
 * @date 2012-9-19 下午04:31:34 
 */
public class DownloadStateDownloading implements IDownloadState{
	
	private static final long serialVersionUID = 1L;

	private transient Context mContext;
	
	private transient BaseDownloadInfo downloadInfo ;
	
	private final int state=DownloadState.STATE_DOWNLOADING;
	
	public DownloadStateDownloading(Context context, BaseDownloadInfo info) {
		mContext = context;
		this.downloadInfo = info ;
	}

	@Override
	public void pause() {
		downloadInfo.resetDownloadSize(mContext);
		if (downloadInfo.downloadWorker != null) {
			downloadInfo.downloadWorker.pause();
			downloadInfo.downloadWorker = null ;
		}
		downloadInfo.setState(downloadInfo.getPauseState());
		if(downloadInfo.getFileType() == BaseDownloadInfo.FILE_TYPE_APK) {
			//暂停打点
//			NdAnalytics.onEvent(mContext, AnalyticsConstant.APP_DOWNLOAD_MANAGE_APP_DOWNLOAD_OPERATE, "zt");
		}
	}

	@Override
	public void continueDownload() {

	}

	@Override
	public void downloadFinished() {

	}

	@Override
	public void popInDownloading() {

	}

	@Override
	public void cancel() {
		if (downloadInfo.downloadWorker != null) {
			downloadInfo.downloadWorker.cancle();
			if(downloadInfo.getFileType() == BaseDownloadInfo.FILE_TYPE_APK) {
				//取消下载打点
//				NdAnalytics.onEvent(mContext, AnalyticsConstant.APP_DOWNLOAD_MANAGE_APP_DOWNLOAD_OPERATE, "qx");
			}
		}
		try {
			DownloadDBManager.deleteDownloadLog(mContext, downloadInfo);
//			if (!downloadInfo.getIsSilent()) {
//				DownloadNotification.downloadCancelledNotification(mApplication, Math.abs(downloadInfo.getDownloadUrl().hashCode()));
//			}
			final File file = new File(downloadInfo.getFilePath() + AbstractDownloadWorker.getTempSuffix(downloadInfo.getIsSilent()));
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public int getState() {
		return state ;
	}
}
