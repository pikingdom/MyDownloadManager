package com.nd.hilauncherdev.webconnect.downloadmanage.model.state;

import java.io.File;

import android.content.Context;

import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadInfo;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadWorker;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadDBManager;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;

/**
 * 下载已安装状态，这是下载的最终状态
 * 
 * @author pdw
 * @version
 * @date 2012-9-19 下午04:32:59
 */
public class DownloadStateInstalled implements IDownloadState {

	private static final long serialVersionUID = 1L;

	private transient Context mContext;

	private transient BaseDownloadInfo downloadInfo;
	
	private final int state=DownloadState.STATE_INSTALLED;

	public DownloadStateInstalled(Context context, BaseDownloadInfo info) {
		mContext = context;
		this.downloadInfo = info;
	}

	@Override
	public void pause() {

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
		try {
			DownloadDBManager.deleteDownloadLog(mContext, downloadInfo);
//			DownloadNotification.downloadCancelledNotification(mApplication, Math.abs(downloadInfo.getDownloadUrl().hashCode()));
			BaseDownloadWorker.sendCancleDownloadLogBroadcast(mContext, downloadInfo.getIdentification(), downloadInfo.getDownloadUrl());
			final File file = new File(downloadInfo.getFilePath());
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public int getState() {
		return state;
	}

}
