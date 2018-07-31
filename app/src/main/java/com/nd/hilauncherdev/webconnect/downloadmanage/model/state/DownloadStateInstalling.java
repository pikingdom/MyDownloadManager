package com.nd.hilauncherdev.webconnect.downloadmanage.model.state;

import java.io.File;

import android.content.Context;

import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadInfo;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadDBManager;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;

/**
 * 正在安装状态，应用于静默安装
 * 
 * @author zhuchenghua
 * 
 */
public class DownloadStateInstalling implements IDownloadState {

	private static final long serialVersionUID = 1L;

	private transient Context mContext;

	private transient BaseDownloadInfo downloadInfo;

	private final int state = DownloadState.INSTALL_STATE_INSTALLING;

	public DownloadStateInstalling(Context context, BaseDownloadInfo info) {
		mContext = context;
		this.downloadInfo = info;
	}

	@Override
	public void cancel() {

		if (downloadInfo.downloadWorker != null) {
			downloadInfo.downloadWorker.cancle();
		}
		try {
			DownloadDBManager.deleteDownloadLog(mContext, downloadInfo);
//			DownloadNotification.downloadCancelledNotification(mApplication, Math.abs(downloadInfo.getDownloadUrl().hashCode()));
			final File file = new File(downloadInfo.getFilePath() + AbstractDownloadWorker.getTempSuffix(downloadInfo.getIsSilent()));
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void continueDownload() {

	}

	@Override
	public void downloadFinished() {

	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public void pause() {

	}

	@Override
	public void popInDownloading() {

	}

}
