package com.nd.hilauncherdev.framework.httplib;

import android.content.Context;

/** 
 * 公共下载类，支持断点下载
 */
public class CommonDownloadWorker extends AbstractDownloadWorker {
	
	/**
	 * 构造下载线程
	 * 由此构造的下载线程将统一由{@link CommonDownloadWorkerSupervisor}管理
	 * 
	 * @param url
	 * @param savePath 以"/"结束，如"/sdcard/PandaHome3/Theme/"
	 * @param specifyFileName 指定下载文件名
	 * @param tipName 提示名称
	 */
	public CommonDownloadWorker(Context context, String url, String savePath, String specifyFileName ,String tipName) {
		super(context, url, savePath, specifyFileName ,tipName, null);
	}

	@Override
	protected void onHttpReqeust(String identification, String url, final int requestType, long totalSize, long downloadSize) {
		CommonDownloadWorkerSupervisor.remove(url);
	}

	@Override
	protected void onDownloadWorking(String identification, String url,long totalSize,long downloadSize, final int progress) {

	}

	@Override
	protected void onDownloadCompleted(String identification, String url,String file,long totalSize) {
		CommonDownloadWorkerSupervisor.remove(url);
	}

	@Override
	protected void onBeginDownload(String identification, String url,long downloadSize,int progress) {
		
	}

	@Override
	protected void onDownloadFailed(String identification, String url) {
		CommonDownloadWorkerSupervisor.remove(url);
	}
}
