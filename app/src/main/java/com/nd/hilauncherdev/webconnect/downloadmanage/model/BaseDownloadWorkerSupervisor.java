package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.util.Vector;

import android.content.Context;
import android.util.Log;

import com.nd.hilauncherdev.core.DownloadStringUtil;


/**
 * 下载线程管理
 */
public class BaseDownloadWorkerSupervisor {
	
	/**
	 * 最大线程下载数
	 */
	private static final int MAX_DOWNLOAD_THREAD_COUNT = 1 ;

	private static final int MAX_DOWNLOAD_THREAD_COUNT_FOR_THEME = 1;

	private static final int MAX_DOWNLOAD_THREAD_COUNT_FOR_APK = 3;

	private static final int MAX_DOWNLOAD_THREAD_COUNT_FOR_VIDEOPAPER_ = 5;

	/**
	 * Apk专用下载队列
	 */
	private static final Vector<BaseDownloadInfo> mApkDownloadingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * Apk专用等待队列
	 */
	private static final Vector<BaseDownloadInfo> mApkWaitingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * List专用下载队列
	 */
	private static final Vector<BaseDownloadInfo> mListDownloadingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * List专用等待队列
	 */
	private static final Vector<BaseDownloadInfo> mListWaitingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * 下载队列
	 */
	private static final Vector<BaseDownloadInfo> mOtherDownloadingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * 等待队列
	 */
	private static final Vector<BaseDownloadInfo> mOtherWaitingQueue = new Vector<BaseDownloadInfo>();
	
	private static boolean useApkQueue(int fileType) {
		return fileType == BaseDownloadInfo.FILE_TYPE_APK;
	}
	
	/**
	 * 加入下载队列
	 */
	public static synchronized boolean addRunningQueue(BaseDownloadInfo info) {
		if (info == null) {
			return false;
		}
		Vector<BaseDownloadInfo> downloadingQueue = getDownloadingQueueByFileType(info.getFileType());		
		if (!downloadingQueue.contains(info)){
			downloadingQueue.add(info);
			return true ;
		} else {
			return false ;
		}
	}
	
	static synchronized Vector<BaseDownloadInfo> getDownloadingQueueByFileType(int fileType) {
		if (useApkQueue(fileType)) {
			return mApkDownloadingQueue;
		} else if (fileType == BaseDownloadInfo.FILE_TYPE_THEME_SERIES
				|| fileType == BaseDownloadInfo.FILE_VIDEO_WALLPAPER_SERIES
				|| fileType == BaseDownloadInfo.FILE_VIDEO_PAPER) {
			return mListDownloadingQueue;
		} else {
			return mOtherDownloadingQueue;
		}
	}
	
	static synchronized Vector<BaseDownloadInfo> getWaitingQueueByFileType(int fileType) {
		if (useApkQueue(fileType)) {
			return mApkWaitingQueue;
		} else if (fileType == BaseDownloadInfo.FILE_TYPE_THEME_SERIES
				|| fileType == BaseDownloadInfo.FILE_VIDEO_WALLPAPER_SERIES
				|| fileType == BaseDownloadInfo.FILE_VIDEO_PAPER) {
			return mListWaitingQueue;
		} else {
			return mOtherWaitingQueue;
		}
	}
	
	/**
	 * 添加下载任务
	 */
	static synchronized boolean addDownloadTask(Context context, BaseDownloadInfo info) {
		if (!isInQueue(info.getIdentification())) {
			info.start(context);
			return true ;
		} else {
			Log.e("http", "download task has been in the queue -> "+info.getDownloadUrl());
			return false ;
		}
	}
	
	/**
	 * 移除下载任务
	 * @param identification 下载唯一标识
	 */
	public static synchronized void remove(String identification) {
		BaseDownloadInfo info = getDownloadInfo(identification);
		if (info == null) {
			return;
		}

		Vector<BaseDownloadInfo> downloadingQueue = getDownloadingQueueByFileType(info.getFileType());
		Vector<BaseDownloadInfo> waitingQueue = getWaitingQueueByFileType(info.getFileType());
		waitingQueue.remove(new BaseDownloadInfo(identification));
		downloadingQueue.remove(new BaseDownloadInfo(identification));
	}
	
	/**
	 * 马上下载还是进入等待序列
	 */
	public static synchronized boolean shouldRunImmediately(BaseDownloadInfo info) {
		if (info == null) {
			return false;
		}
		int fileType = info.getFileType();
		Vector<BaseDownloadInfo> downloadingQueue = getDownloadingQueueByFileType(fileType);
		if(fileType == BaseDownloadInfo.FILE_TYPE_APK){
			if (downloadingQueue.size() < MAX_DOWNLOAD_THREAD_COUNT_FOR_APK) {
				return true;
			}else{
				return false;
			}
		}

		if(fileType == BaseDownloadInfo.FILE_VIDEO_PAPER){
			if (downloadingQueue.size() < MAX_DOWNLOAD_THREAD_COUNT_FOR_VIDEOPAPER_) {
				return true;
			}else{
				return false;
			}
		}

		if(fileType == BaseDownloadInfo.FILE_TYPE_THEME_SERIES
		  || fileType == BaseDownloadInfo.FILE_VIDEO_WALLPAPER_SERIES
				|| fileType == BaseDownloadInfo.FILE_VIDEO_PAPER){
			if (downloadingQueue.size() < MAX_DOWNLOAD_THREAD_COUNT_FOR_THEME) {
				return true;
			}else{
				return false;
			}
		}

		if (downloadingQueue.size() < MAX_DOWNLOAD_THREAD_COUNT) {
			return true;
		}
		
//		for (int i=0; i<downloadingQueue.size(); i++) {
//			BaseDownloadInfo runningInfo = downloadingQueue.get(i);
//			if (info.getPrioritySize() < runningInfo.getPrioritySize()) {
//				pause(CommonGlobal.getApplicationContext(), runningInfo.getIdentification());
//				addHeadOfWaitingPool(runningInfo);
//				return true;
//			}
//		}
		
		return false;
	}
	
	/**
	 * 加入等待队列
	 */
	public static synchronized void addWaitingPool(BaseDownloadInfo info) {
		if (info == null) {
			return;
		}
		Vector<BaseDownloadInfo> waitingQueue = getWaitingQueueByFileType(info.getFileType());
		if (!waitingQueue.contains(info)) {
			waitingQueue.add(info);
		}
	}
	
	/**
	 * 加入等待队列首位
	 */
	public static synchronized void addHeadOfWaitingPool(BaseDownloadInfo info) {
		if (info == null) {
			return;
		}
		Vector<BaseDownloadInfo> waitingQueue = getWaitingQueueByFileType(info.getFileType());
		if (!waitingQueue.contains(info)) {
			waitingQueue.add(0, info);
		}
	}
	
	/**
	 * 从等待队列中选出一个任务进行下载
	 */
	public static synchronized BaseDownloadInfo popWaitingTask(int lastTaskFileType) {
		Vector<BaseDownloadInfo> waitingQueue = getWaitingQueueByFileType(lastTaskFileType);
		if (waitingQueue.isEmpty()) {
			return null ;
		} else {
			return waitingQueue.remove(0); //获取队列的第一个
		}
	}
	
	/**
	 * 查找下载任务
	 * @param identification 下载唯一标识
	 * @return
	 */
	public static synchronized BaseDownloadInfo getDownloadInfo(String identification) {
		if (DownloadStringUtil.isEmpty(identification)) {
			return null;
		}
		
		for (BaseDownloadInfo info : mApkDownloadingQueue) {
			if (identification.equals(info.getIdentification())) {
				return info;
			}
		}
		
		for (BaseDownloadInfo info : mApkWaitingQueue) {
			if (identification.equals(info.getIdentification())) {
				return info;
			}
		}
		
		for (BaseDownloadInfo info : mListDownloadingQueue) {
			if (identification.equals(info.getIdentification())) {
				return info;
			}
		}
		
		for (BaseDownloadInfo info : mListWaitingQueue) {
			if (identification.equals(info.getIdentification())) {
				return info;
			}
		}
		
		for (BaseDownloadInfo info : mOtherDownloadingQueue) {
			if (identification.equals(info.getIdentification())) {
				return info;
			}
		}
		
		for (BaseDownloadInfo info : mOtherWaitingQueue) {
			if (identification.equals(info.getIdentification())) {
				return info;
			}
		}
		
		return null ;
	}
	
	/**
	 * 获取下载队列
	 */
	public static synchronized Vector<BaseDownloadInfo> getDownloadingQueue() {
		Vector<BaseDownloadInfo> downloadingQueue = new Vector<BaseDownloadInfo>();
		downloadingQueue.addAll(mApkDownloadingQueue);
		downloadingQueue.addAll(mListDownloadingQueue);
		downloadingQueue.addAll(mOtherDownloadingQueue);
		return downloadingQueue;
	}
	
	/**
	 * 获取等待队列
	 */
	public static synchronized Vector<BaseDownloadInfo> getWaitingQueue() {
		Vector<BaseDownloadInfo> waitingQueue = new Vector<BaseDownloadInfo>();
		waitingQueue.addAll(mApkWaitingQueue);
		waitingQueue.addAll(mListWaitingQueue);
		waitingQueue.addAll(mOtherWaitingQueue);
		return waitingQueue ;
	}
	
	/**
	 * 清除等待队列
	 */
	public static synchronized void clearWaitingQueue() {
		mApkWaitingQueue.clear();
		mListWaitingQueue.clear();
		mOtherWaitingQueue.clear();
	}
	
	/**
	 * 是否已存在等待队列或者下载队列
	 * @param identification 下载唯一标识
	 * @return
	 */
	public static synchronized boolean isInQueue(String identification) {
		return getDownloadInfo(identification) != null ;
	}
	
	/**
	 * 暂停下载
	 * @param identification 下载唯一标识
	 * @return true 下载队列存在对应的下载任务，等待暂停广播处理<br>
	 *         false 下载队列不存在对应的下载任务，自行处理ui
	 */
	public static synchronized boolean pause(final String identification) {
		BaseDownloadInfo downloadInfo = getDownloadInfo(identification);
		if (downloadInfo != null) {
			downloadInfo.pause();
		}
		return true ;
	}
	
	/**
	 * 取消下载
	 * @param identification 下载唯一标识
	 * @return true 下载队列存在对应的下载任务，等待取消广播处理<br>
	 *         false 下载队列不存在对应的下载任务，自行处理ui
	 */
	public static synchronized boolean cancel(final Context context, final String identification) {
		BaseDownloadInfo downloadInfo = getDownloadInfo(identification);
		if (downloadInfo != null) { //在队列中
			downloadInfo.cancel();
			return true ;
		} else {
			return DownloadDBManager.deleteDownloadLog(context, new BaseDownloadInfo(identification));
		}
	}
	
}
