package com.nd.hilauncherdev.webconnect.downloadmanage.model.state;

import java.io.Serializable;

/** 
 * 状态机接口
 * 
 * @author pdw 
 * @date 2012-9-19 下午03:52:10 
 */
public interface IDownloadState extends Serializable{
	
	/**
	 * 暂停
	 */
	public void pause() ;
	
	/**
	 * 继续下载
	 */
	public void continueDownload() ;
	
	/**
	 * 下载完成
	 */
	public void downloadFinished() ;
	
	/**
	 * 等待进入下载队列
	 */
	public void popInDownloading() ;
	
	/**
	 * 取消下载
	 */
	public void cancel() ;
	
	
	/**
	 * 获取状态
	 */
	public int getState() ;
	
}
