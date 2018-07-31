package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.util.Iterator;
import java.util.Vector;

import android.content.Context;
import android.text.TextUtils;

import com.nd.hilauncherdev.core.DownloadTelephoneUtil;


/** 
 * 用于后台静默下载
 */
public class SilentDownloadWorkerSupervisor {
	
	/**
	 * 最大线程下载数
	 */
	private static final int MAX_DOWNLOAD_THREAD_COUNT = 1 ;

	/**
	 * 下载队列
	 */
	private static final Vector<BaseDownloadInfo> downloadingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * 等待队列
	 */
	private static final Vector<BaseDownloadInfo> waitingQueue = new Vector<BaseDownloadInfo>();
	
	/**
	 * <p>添加正在下载队列，添加后进入下载队列</p>
	 * <p>date: 2012-9-17 下午06:34:44
	 * @param worker
	 * @return
	 */
	public static synchronized boolean addRunningQueue(BaseDownloadInfo info){
		if (!downloadingQueue.contains(info)){
			downloadingQueue.add(info);
			return true ;
		} else {
			return false ;
		}
	}
	
	/**
	 * <p>添加下载任务，添加后进入等待队列</p>
	 * <p>date: 2012-9-20 下午03:29:07
	 * @param infos
	 * @return
	 */
	static synchronized boolean addDownloadTask(Context context, BaseDownloadInfo info) {
		info.setSilent();
		info.initState(context);
		
		if (!isInQueue(info.getIdentification())) {
			info.start(context);
			return true ;
		} else {
			return false ;
		}
	}
	
	public static synchronized void cancelSimilarTask(BaseDownloadInfo info) {
		if (TextUtils.isEmpty(info.getIdentification())
			|| TextUtils.isEmpty(info.getSavedDir())
			|| TextUtils.isEmpty(info.getSavedName())) {
			return;
		}
		
		for (BaseDownloadInfo tmpInfo : waitingQueue) {
			if (tmpInfo.getIdentification().equals(info.getIdentification()) || tmpInfo.getFilePath().equals(info.getFilePath())) {
				tmpInfo.cancel();
			}
		}
		
		for (BaseDownloadInfo tmpInfo : downloadingQueue) {
			if (tmpInfo.getIdentification().equals(info.getIdentification()) || tmpInfo.getFilePath().equals(info.getFilePath())) {
				tmpInfo.cancel();
			}
		}
	}
	
	/**
	 * 移除下载线程
	 * @param identification 下载唯一标识
	 */
	public static synchronized void remove(String identification){
		waitingQueue.remove(new BaseDownloadInfo(identification));
		downloadingQueue.remove(new BaseDownloadInfo(identification));
	}
	
	/**
	 * 查找正在下载队列中优先级最低的任务
	 */
	private static BaseDownloadInfo findLowestPriorityRunningTask() {
		int index = -1;
		int prioritySize = BaseDownloadInfo.PRIORITY_DEFAULT;
		BaseDownloadInfo runningInfo = null;
		for (int i=0; i<downloadingQueue.size(); i++) {
			runningInfo = downloadingQueue.get(i);
			if (i == 0 || runningInfo.getPrioritySize() > prioritySize) {
				prioritySize = runningInfo.getPrioritySize();
				index = i;
			}
		}
		
		return ((index != -1) ? downloadingQueue.get(index) : null);
	}
	
	/**
	 * 查找等待队列中优先级最高的任务
	 */
	private static BaseDownloadInfo findHighestPriorityWaittingTask() {
		int index = -1;
		int prioritySize = BaseDownloadInfo.PRIORITY_DEFAULT;
		BaseDownloadInfo runningInfo = null;
		for (int i=0; i<waitingQueue.size(); i++) {
			runningInfo = waitingQueue.get(i);
			if (i == 0 || runningInfo.getPrioritySize() < prioritySize) {
				prioritySize = runningInfo.getPrioritySize();
				index = i;
			}
		}
		
		return ((index != -1) ? waitingQueue.remove(index) : null);
	}
	
	/**
	 * 开始下载任务
	 * @param info 下载任务
	 * @return true表示添加到下载队列，false表示添加到等待队列
	 */
	public static synchronized boolean start(Context context, BaseDownloadInfo info) {
		boolean runImmediately = false;
		
		if (DownloadTelephoneUtil.isWifiEnable(context)) {
			if (downloadingQueue.size() < MAX_DOWNLOAD_THREAD_COUNT) {
				runImmediately = true;
			} else {
				BaseDownloadInfo runningInfo = findLowestPriorityRunningTask();
				if (runningInfo != null && info.getPrioritySize() < runningInfo.getPrioritySize()) {
					pause(runningInfo.getIdentification());
					addWaitingPool(runningInfo);
					runImmediately = true;
				}
			}
		}
		
		if (runImmediately) {
			if (addRunningQueue(info)) {
				info.setState(info.getDownloadingState());
			}
		} else {
			info.setState(info.getWaitingState());
			addWaitingPool(info);
		}
		
		return runImmediately;
	}
	
	/**
	 * <p>进入等待序列</p>
	 * <p>date: 2012-9-17 下午05:07:05
	 * @param downloadWorker
	 */
	public static synchronized void addWaitingPool(BaseDownloadInfo info) {
		if (!waitingQueue.contains(info)) {
			waitingQueue.add(info);
		}
	}
	
	/**
	 * <p>获取等待任务</p>
	 * <p>date: 2012-9-17 下午06:36:08
	 * @return
	 */
	public static synchronized BaseDownloadInfo popWaitingTask(Context context) {
		if ((downloadingQueue.size() < MAX_DOWNLOAD_THREAD_COUNT && DownloadTelephoneUtil.isWifiEnable(context))) {
			return findHighestPriorityWaittingTask();
		}
		
		return null;
	}
	
	/**
	 * <p>获取下载的相关信息</p>
	 * <p>date: 2012-9-17 下午06:32:15
	 * @param identification 下载唯一标识
	 * @return
	 */
	public static synchronized BaseDownloadInfo getDownloadInfo(String identification) {
		BaseDownloadInfo one = new BaseDownloadInfo(identification);
		int index = downloadingQueue.indexOf(one);
		if (index != -1)
			return downloadingQueue.get(index);
		index = waitingQueue.indexOf(one);
		if (index != -1)
			return waitingQueue.get(index);
		return null ;
	}
	
	/**
	 * 获取下载队列
	 */
	public static synchronized Vector<BaseDownloadInfo> getDownloadingQueue() {
		return downloadingQueue ;
	}
	
	/**
	 * 获取等待队列
	 */
	public static synchronized Vector<BaseDownloadInfo> getWaitingQueue() {
		return waitingQueue ;
	}
	
	/**
	 * <p>清除等待队列</p>
	 * <p>date: 2012-9-25 下午05:29:17
	 * @author pdw
	 */
	public static synchronized void clearWaitingQueue() {
		waitingQueue.clear();
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
	
	/**
	 * 开始后台静默下载
	 */
	private static synchronized void start(Context context) {
		final BaseDownloadInfo info = SilentDownloadWorkerSupervisor.popWaitingTask(context);
		if (info != null) {
			info.start(context);
		}
	}
	
	/**
	 * 停止后台静默下载
	 */
	private static synchronized void stop() {
		Iterator<BaseDownloadInfo> iter = downloadingQueue.iterator();
		while (iter.hasNext()) {
			BaseDownloadInfo info = (BaseDownloadInfo)iter.next();
			pause(info.getIdentification());
		}
	}
	
	public static synchronized void processServiceCreate(Context context) {
		if (!DownloadTelephoneUtil.isWifiEnable(context)) {
			return;
		}
		
		start(context);
	}
	
	public static synchronized void processNetworkChange(Context context) {
		if (DownloadTelephoneUtil.isWifiEnable(context)) {
			start(context);
		} else {
			stop();
		}
	}
}
