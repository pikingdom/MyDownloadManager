package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import android.content.Context;
import android.content.Intent;

import com.nd.hilauncherdev.core.DownloadTelephoneUtil;
import com.nd.hilauncherdev.core.DownloadFileUtil;
import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.framework.httplib.HttpConstants;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadBroadcastExtra;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.SizeFormater;

/** 
 *
 */
public class Silent23GDownloadWorker extends AbstractDownloadWorker {
	
	public Silent23GDownloadWorker(Context context, 
			                           String url, 
			                           String savePath, 
			                           String specifyFileName, 
			                           String tipName,
			                           AbstractDownloadCallback callback) {
		super(context, url, savePath, specifyFileName, tipName, callback);
	}
	
	public Silent23GDownloadWorker(Context context, BaseDownloadInfo info, AbstractDownloadCallback callback) {
		this(context, info.getDownloadUrl(), info.getSavedDir(), info.getSavedName(), info.getTitle(), callback);
		info.downloadWorker = this ;
		downloadInfo = info ;
	}

	@Override
	protected void onHttpReqeust(String identification, String url, int requestType, long totalSize, long downloadSize) {
		Silent23GDownloadWorkerSupervisor.remove(identification);
		if (requestType != HttpConstants.HTTP_REQUEST_CANCLE) {
			Silent23GDownloadWorkerSupervisor.addWaitingPool(downloadInfo);
		}
		
		beginNewDownload();
	}

	@Override
	protected void onDownloadWorking(String identification, String url, long totalSize, long downloadSize, int progress) {		
		sendNotice(identification, downloadSize, totalSize, progress, DownloadState.STATE_DOWNLOADING);
	}

	@Override
	protected void onDownloadCompleted(String identification, String url, String file, long totalSize) {
		DownloadDBManager.deleteDownloadLog(mAppContext, downloadInfo);
		Silent23GDownloadWorkerSupervisor.remove(identification);

		beginNewDownload();
		sendSuccessNotice(identification, totalSize, file);
	}

	@Override
	protected void onBeginDownload(String identification, String url, long downloadSize, int progress) {	
		sendBeginNotice(identification, downloadSize, 0, progress, DownloadState.STATE_START);
		sendBeginNotice(identification, downloadSize, 0, progress, DownloadState.STATE_DOWNLOADING);
	}

	@Override
	protected void onDownloadFailed(String identification, String url) {
		Silent23GDownloadWorkerSupervisor.remove(identification);
		//在网络可用的情况下，下载失败，则不再将其加入等待队列
		boolean shouldAddWaiting = !(downloadInfo.get23GEnable() ? DownloadTelephoneUtil.isNetworkAvailable(mAppContext) : DownloadTelephoneUtil.isWifiEnable(mAppContext));
		if (shouldAddWaiting) {
			Silent23GDownloadWorkerSupervisor.addWaitingPool(downloadInfo);
		}
		
		sendFailNotice(identification);
		beginNewDownload();
	}
	
	/**
	 * 从等待队列中获取下载项进行下载
	 */
	private void beginNewDownload() {			
		final BaseDownloadInfo info = Silent23GDownloadWorkerSupervisor.popWaitingTask(mAppContext);
		if (info != null) {
			info.start(mAppContext);
		}
	}
		
	@Override
	public final void cancle() {
		//从下载队列、等待队列中移队除
		Silent23GDownloadWorkerSupervisor.remove(this.downloadInfo.getIdentification());
		super.cancle();
	}
	
	private void sendNotice(String identification,long downloadSize,long totalSize,int progress,int state) {
		sendStateBroatcast(identification, downloadSize, totalSize, progress, state);
	}
	
	private void sendBeginNotice(String identification,long downloadSize,long totalSize,int progress,int state) {
		sendStateBroatcast(identification, downloadSize, totalSize, progress, state);
	}
	
	private void sendSuccessNotice(String identification,long downloadSize,String file) {
		sendStateBroatcast(identification, downloadSize, 0, 100, DownloadState.STATE_FINISHED);
	}
	
	private void sendFailNotice(String identification) {
		sendStateBroatcast(identification, 0, 0, 0, DownloadState.STATE_FAILED);
	}
	
	private void sendStateBroatcast(String identification, long downloadSize,long totalSize, int progress, int state) {
		if (DownloadServerService.sBroadcastAction == null) {
			return;
		}
		
		Intent intent = null ;
		intent = new Intent(DownloadServerService.sBroadcastAction);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_IDENTIFICATION, identification);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_DOWNLOAD_URL, this.url);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_PROGRESS, progress);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_STATE, state);
		if (downloadSize != -1) {
			intent.putExtra(DownloadBroadcastExtra.EXTRA_DOWNLOAD_SIZE, SizeFormater.getDownloadSize(mAppContext, downloadSize));
		}
		
		if(totalSize>0)
			intent.putExtra(DownloadBroadcastExtra.EXTRA_TOTAL_SIZE, DownloadFileUtil.getMemorySizeString(totalSize));
		
		mAppContext.sendBroadcast(intent);
	}
}
