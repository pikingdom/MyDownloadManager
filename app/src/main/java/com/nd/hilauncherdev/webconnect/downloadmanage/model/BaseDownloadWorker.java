package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import android.content.Context;
import android.content.Intent;

import com.nd.hilauncherdev.core.DownloadFileUtil;
import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.framework.httplib.HttpConstants;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadBroadcastExtra;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.SizeFormater;

/** 
 * 应用下载器
 * 
 * @author pdw 
 * @version 1.1
 * @date 2012-9-17 下午05:32:11 
 */
public class BaseDownloadWorker extends AbstractDownloadWorker {
	
	// 通知位置
	private int noticePosition;

	public BaseDownloadWorker(Context context, 
			                     String url, 
			                     String savePath,
			                     String specifyFileName, 
			                     String tipName,
			                     AbstractDownloadCallback callback) {
		super(context, url, savePath, specifyFileName, tipName, callback);
		noticePosition = Math.abs(url.hashCode());
	}
	
	public BaseDownloadWorker(Context context, BaseDownloadInfo info, AbstractDownloadCallback callback) {
		this(context, info.getDownloadUrl(), info.getSavedDir(), info.getSavedName(), info.getTitle(), callback);
		info.downloadWorker = this ;
		downloadInfo = info ;
	}

	@Override
	protected void onHttpReqeust(String identification, String url, int requestType, long totalSize, long downloadSize) {
		final BaseDownloadInfo info = BaseDownloadWorkerSupervisor.getDownloadInfo(identification);
		if (info == null && requestType == HttpConstants.HTTP_REQUEST_CANCLE) {
			sendStateBroatcast(identification,-1,-1,0, DownloadState.STATE_CANCLE);
			beginNewDownload(downloadInfo);
			return ;
		}
		if (requestType == HttpConstants.HTTP_REQUEST_PAUSE) {
			//更新进度
			DownloadDBManager.updateProgress(mAppContext, info);
			sendStateBroatcast(identification, downloadSize, totalSize, info.progress, DownloadState.STATE_PAUSE);
		} else if (requestType == HttpConstants.HTTP_REQUEST_CANCLE) {
			sendStateBroatcast(identification,-1,-1,0, DownloadState.STATE_CANCLE);
		}
		BaseDownloadWorkerSupervisor.remove(identification);
		beginNewDownload(downloadInfo);
	}

	@Override
	protected void onDownloadWorking(String identification, String url, long totalSize, long downloadSize, int progress) {
		if (BaseDownloadWorkerSupervisor.getDownloadInfo(identification) == null)
			return ;
		sendNotice(identification,downloadSize,totalSize, progress, DownloadState.STATE_DOWNLOADING);
	}
	
	@Override
	protected void onDownloadCompleted(String identification, String url, String file, long totalSize) {
		if (downloadInfo == null) {
			return;
		}
		
		downloadInfo.progress = 100 ;
		downloadInfo.downloadSize = downloadInfo.totalSize ;
		if (DownloadDBManager.hasLogDownloadRecord(mAppContext, downloadInfo)) {
			DownloadDBManager.updateProgress(mAppContext, downloadInfo);
		} else {
			DownloadDBManager.insertLog(mAppContext, downloadInfo);
		}
		BaseDownloadWorkerSupervisor.remove(identification);
		beginNewDownload(downloadInfo);
		sendSuccessNotice(identification,totalSize,file);
		
		downloadInfo.setCompleteTime(mAppContext, System.currentTimeMillis());
	}

	@Override
	protected void onBeginDownload(String identification, String url, long downloadSize, int progress) {
		final BaseDownloadInfo info = BaseDownloadWorkerSupervisor.getDownloadInfo(identification);
		if (info == null)
			return ;
				
		/**
		 * 发送开始通知
		 */
		sendBeginNotice(identification,downloadSize,0,progress,DownloadState.STATE_START);
		sendBeginNotice(identification,downloadSize,0,progress,DownloadState.STATE_DOWNLOADING);
	}

	@Override
	protected void onDownloadFailed(String identification, String url) {
		BaseDownloadWorkerSupervisor.remove(identification);
		sendFailNotice();
		beginNewDownload(downloadInfo);
	}
	
	/**
	 * 从等待队列中获取下载项进行下载
	 * @param lastTask 前一个任务
	 */
	private void beginNewDownload(BaseDownloadInfo lastTask) {
		int fileType = lastTask != null ? lastTask.getFileType() : null;
		final BaseDownloadInfo info = BaseDownloadWorkerSupervisor.popWaitingTask(fileType);
		if (info != null) {
			info.start(mAppContext);
		}
	}
	
//	private Intent createIntent() {
//		Intent intent = new Intent(DownloadServerService.ACTION_SHOW);
//		if (downloadInfo != null) {
//			int showType = DownloadManagerActivity.mapFileTypeToShowType(downloadInfo.getFileType());
//			intent.putExtra(DownloadServerService.EXTRA_SHOW_TYPE, showType);
//			intent.putExtra(DownloadManagerActivity.EXTRA_FROM, DownloadManagerActivity.FROM_TZL);
//		}
//		return intent;
//	}
	
	private void sendNotice(String identification,long downloadSize,long totalSize,int progress,int state) {
//		String str = tipName+mAppContext.getResources().getString(R.string.common_downloading) ;
//		PendingIntent PIntent = PendingIntent.getActivity(mAppContext, 0, createIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
		
//		if (downloadInfo != null 
//			&& downloadInfo.getFileType() != BaseDownloadInfo.FILE_TYPE_NONE) {
//			DownloadNotification.downloadRunningNotificationWithProgress(mAppContext, noticePosition, str, null, PIntent, progress);
//		}
		
		sendStateBroatcast(identification,downloadSize,totalSize,progress,state);
	}
	
	private void sendBeginNotice(String identification,long downloadSize,long totalSize,int progress,int state) {
//		String str = tipName;
//		PendingIntent PIntent = PendingIntent.getActivity(mAppContext, 0, createIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
		
//		if (downloadInfo != null 
//			&& downloadInfo.getFileType() != BaseDownloadInfo.FILE_TYPE_NONE) {
//			DownloadNotification.downloadBeganNotification(mAppContext, noticePosition, str, null, PIntent, progress);
//		}
		
		sendStateBroatcast(identification,downloadSize,totalSize,progress,state);
	}
	
	private void sendSuccessNotice(String identification,long downloadSize,String file) {
//		String str = tipName;
//		PendingIntent pIntent = PendingIntent.getActivity(mAppContext, 0, createIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
		
//		if (downloadInfo != null 
//			&& downloadInfo.getFileType() != BaseDownloadInfo.FILE_TYPE_NONE) {
//			DownloadNotification.downloadCompletedNotification(mAppContext, noticePosition, str, null, pIntent);
//		}
		
		sendStateBroatcast(identification,downloadSize,0,100, DownloadState.STATE_FINISHED);
	}
	
	private void sendFailNotice() {
//		String str = tipName;
//		PendingIntent pIntent = PendingIntent.getActivity(mAppContext, 0, createIntent(), 0);
		
//		if (downloadInfo != null 
//			&& downloadInfo.getFileType() != BaseDownloadInfo.FILE_TYPE_NONE) {
//			DownloadNotification.downloadFailedNotification(mAppContext, noticePosition, str, pIntent);
//		}
		
		if (downloadInfo != null) {
			sendStateBroatcast(downloadInfo.getIdentification(), 0, 0, 0, DownloadState.STATE_FAILED);
		}
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
		
		if (downloadInfo != null) {
			intent.putExtra(DownloadBroadcastExtra.EXTRA_FILE_TYPE, downloadInfo.getFileType());
		}
		
		mAppContext.sendBroadcast(intent);
	}
	
	@Override
	public final void cancle() {
		//从下载队列、等待队列中移队除
		BaseDownloadWorkerSupervisor.remove(this.downloadInfo.getIdentification());
		super.cancle();
	}
	
	/**
	 * 发送取消下载的广播
	 * @author pdw
	 */
	public static void sendCancleDownloadLogBroadcast(Context ctx, String identification, String url) {
		if (DownloadServerService.sBroadcastAction == null) {
			return;
		}
		
		Intent intent = null;
		intent = new Intent(DownloadServerService.sBroadcastAction);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_IDENTIFICATION, identification);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_DOWNLOAD_URL, url);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_STATE, DownloadState.STATE_CANCLE);
		ctx.sendBroadcast(intent);
	}
}
