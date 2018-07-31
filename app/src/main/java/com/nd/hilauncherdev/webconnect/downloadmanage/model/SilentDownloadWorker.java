package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import android.content.Context;

import com.nd.hilauncherdev.core.DownloadTelephoneUtil;
import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.framework.httplib.HttpConstants;

/** 
 *
 */
public class SilentDownloadWorker extends AbstractDownloadWorker {
	
	public SilentDownloadWorker(Context context, 
			                        String url, 
			                        String savePath, 
			                        String specifyFileName, 
			                        String tipName,
			                        AbstractDownloadCallback callback) {
		super(context, url, savePath, specifyFileName, tipName, callback);
	}
	
	public SilentDownloadWorker(Context context, BaseDownloadInfo info, AbstractDownloadCallback callback) {
		this(context, info.getDownloadUrl(), info.getSavedDir(), info.getSavedName(), info.getTitle(), callback);
		info.downloadWorker = this ;
		downloadInfo = info ;
	}

	@Override
	protected void onHttpReqeust(String identification, String url, int requestType, long totalSize, long downloadSize) {
		SilentDownloadWorkerSupervisor.remove(identification);
		if (requestType != HttpConstants.HTTP_REQUEST_CANCLE) {
			SilentDownloadWorkerSupervisor.addWaitingPool(downloadInfo);
		}
		
		beginNewDownload();
	}

	@Override
	protected void onDownloadWorking(String identification, String url, long totalSize, long downloadSize, int progress) {		
	}

	@Override
	protected void onDownloadCompleted(String identification, String url, String file, long totalSize) {
		DownloadDBManager.deleteDownloadLog(mAppContext, downloadInfo);
		SilentDownloadWorkerSupervisor.remove(identification);

		beginNewDownload();
	}

	@Override
	protected void onBeginDownload(String identification, String url, long downloadSize, int progress) {	
	}

	@Override
	protected void onDownloadFailed(String identification, String url) {
		SilentDownloadWorkerSupervisor.remove(identification);
		if (!DownloadTelephoneUtil.isWifiEnable(mAppContext)) { //在wifi可用的情况下，下载失败，则不再将其加入等待队列
			SilentDownloadWorkerSupervisor.addWaitingPool(downloadInfo);
		}
		
		beginNewDownload();
	}
	
	/**
	 * 从等待队列中获取下载项进行下载
	 */
	private void beginNewDownload() {			
		final BaseDownloadInfo info = SilentDownloadWorkerSupervisor.popWaitingTask(mAppContext);
		if (info != null) {
			info.start(mAppContext);
		}
	}
		
	@Override
	public final void cancle() {
		//从下载队列、等待队列中移队除
		SilentDownloadWorkerSupervisor.remove(this.downloadInfo.getIdentification());
		super.cancle();
	}
}
