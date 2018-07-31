package com.nd.hilauncherdev.webconnect.downloadmanage.model.state;

import java.io.File;

import android.content.Context;

import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadInfo;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadWorker;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadWorkerSupervisor;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadDBManager;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;

/** 
 * 等待状态 
 * 
 * @author pdw 
 * @version 
 * @date 2012-9-19 下午04:32:05 
 */
public class DownloadStateWaiting implements IDownloadState {
	
	private static final long serialVersionUID = 1L;

	private transient Context mContext;
	
	private transient BaseDownloadInfo downloadInfo ;
	
	private final int state=DownloadState.STATE_WAITING;
	
	public DownloadStateWaiting(Context context, BaseDownloadInfo info) {
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
		BaseDownloadWorkerSupervisor.remove(downloadInfo.getIdentification());
		downloadInfo.setState(downloadInfo.getPauseState());
		AbstractDownloadWorker.sendStateBroadcast(mContext, downloadInfo, DownloadState.STATE_PAUSE);
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
		DownloadDBManager.deleteDownloadLog(mContext, downloadInfo);
		BaseDownloadWorkerSupervisor.remove(downloadInfo.getIdentification());
		
		if (downloadInfo.getIsSilent()) {
			final File file = new File(downloadInfo.getFilePath() + AbstractDownloadWorker.getTempSuffix(downloadInfo.getIsSilent()));
			if (file.exists()) {
				file.delete();
			}
		}
		
//		DownloadNotification.downloadCancelledNotification(mApplication, Math.abs(downloadInfo.getDownloadUrl().hashCode()));
		BaseDownloadWorker.sendCancleDownloadLogBroadcast(mContext, downloadInfo.getIdentification(), downloadInfo.getDownloadUrl());
	}

	@Override
	public int getState() {
		return state ;
	}

}
