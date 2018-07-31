package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.util.Iterator;
import java.util.Vector;

import android.content.Context;

import com.nd.hilauncherdev.core.DownloadTelephoneUtil;


/** 
 * 用于支持23G的后台静默下载
 */
public class Silent23GDownloadWorkerSupervisor {
	
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
	 * 添加下载任务
	 */
	static synchronized boolean addDownloadTask(Context context, BaseDownloadInfo info, boolean enable23g) {
		if (processAddTaskIfInQueue(context, info, enable23g)) {
			return true;
		}
		
		info.setSilent();
		info.set23GEnable(enable23g);
		info.initState(context);
		
		if (!isInQueue(info.getIdentification())) {
			info.start(context);
			return true ;
		} else {
			return false ;
		}
	}
	
	private static long sLastStart = 0;
	public static synchronized boolean startImmediately(Context appContext, BaseDownloadInfo info) {
		if (info == null) {
			return false;
		}

		long tmpLastStart = sLastStart;
		long curTime = System.currentTimeMillis();
		if (downloadingQueue.size() >= MAX_DOWNLOAD_THREAD_COUNT) {
			if (tmpLastStart != 0 && (curTime - tmpLastStart < 1000)) {
				return false;
			}
			
			BaseDownloadInfo runningInfo = downloadingQueue.get(downloadingQueue.size()-1);
			pause(runningInfo.getIdentification());
		}
		
		if (addRunningQueue(info)) {
			sLastStart = curTime;
			info.setState(info.getDownloadingState());
			return true;
		}
		
		return false;
	}
	
	/**
	 * 如果已经存在与队列中，立马开始下载
	 */
	public static synchronized boolean processAddTaskIfInQueue(Context context, BaseDownloadInfo info, boolean enable23g) {
		if (info == null || info.getIdentification() == null) {
			return false;
		}
		
		BaseDownloadInfo tmpInfo = new BaseDownloadInfo(info.getIdentification());
		int index = downloadingQueue.indexOf(tmpInfo);
		if (index != -1) {
			BaseDownloadInfo downloadingInfo = downloadingQueue.get(index);
			if (downloadingInfo != null && downloadingInfo.get23GEnable() != enable23g) {
				downloadingInfo.set23GEnable(enable23g);
				processNetworkChange(context);
			}
			return true;
		}
		index = waitingQueue.indexOf(tmpInfo);
		if (index != -1) {
			BaseDownloadInfo waitingInfo = waitingQueue.remove(index);
			waitingInfo.set23GEnable(enable23g);
			waitingInfo.start(context);
			return true;
		}
		
		return false;
	}
	
	/**
	 * <p>移除下载线程</p>
	 * <p>date: 2012-9-17 下午06:35:04
	 * @param identification 下载唯一标识
	 */
	public static synchronized void remove(String identification){
		
		waitingQueue.remove(new BaseDownloadInfo(identification));
		downloadingQueue.remove(new BaseDownloadInfo(identification));
	}
	
	/**
	 * 马上下载还是进入等待序列
	 */
	public static synchronized boolean shouldRunImmediately(Context context, BaseDownloadInfo info) {
		if (info == null 
			|| downloadingQueue.size() >= MAX_DOWNLOAD_THREAD_COUNT
			|| info.mIsPausingByHand) {
			return false;
		}
		
		return (info.get23GEnable() ? DownloadTelephoneUtil.isNetworkAvailable(context) : DownloadTelephoneUtil.isWifiEnable(context));
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
		if (!waitingQueue.isEmpty()) {
			for (int i=0; i<waitingQueue.size(); i++) {
				BaseDownloadInfo info = waitingQueue.get(i);
				if (shouldRunImmediately(context, info)) {
					return waitingQueue.remove(i);
				}
			}
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
		if (identification == null) {
			return null;
		}
		
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
	 */
	public static synchronized void pause(final String identification) {
		pause(identification, false);
	}
	
	/**
	 * 暂停下载
	 * @param identification 下载唯一标识
	 * @param isByHand 是否用户手动暂停
	 */
	public static synchronized void pause(final String identification, boolean isByHand) {
		BaseDownloadInfo downloadInfo = getDownloadInfo(identification);
		if (downloadInfo != null) {
			downloadInfo.mIsPausingByHand = isByHand;
			downloadInfo.pause();
		}
	}
	
	/**
	 * 恢复下载
	 */
	public static synchronized void continute(final Context context, final String identification) {
		BaseDownloadInfo downloadInfo = getDownloadInfo(identification);
		if (downloadInfo != null) {
			downloadInfo.mIsPausingByHand = false;
			processAddTaskIfInQueue(context, downloadInfo, downloadInfo.get23GEnable());
		}	
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
		final BaseDownloadInfo info = popWaitingTask(context);
		if (info != null) {
			info.start(context);
		}
	}
	
	public static synchronized void processServiceCreate(Context context) {
		start(context);
	}
	
	public static synchronized void processNetworkChange(Context context) {
		boolean isNetworkAvailable = DownloadTelephoneUtil.isNetworkAvailable(context);
		boolean isWifiEnable = DownloadTelephoneUtil.isWifiEnable(context);
		
		Iterator<BaseDownloadInfo> iter = downloadingQueue.iterator();
		while (iter.hasNext()) {
			BaseDownloadInfo info = (BaseDownloadInfo)iter.next();
			if ((info.get23GEnable() ? !isNetworkAvailable : !isWifiEnable)) {
				pause(info.getIdentification());
			}
		}
		
		final BaseDownloadInfo info = popWaitingTask(context);
		if (info != null) {
			info.start(context);
		}
	}
	
	public static void controlSilent23GTask(Context context, String identification, boolean enable23g) {
		if (identification == null) {
			return;
		}
		
		BaseDownloadInfo info = getDownloadInfo(identification);
		if (info != null) {
			info.set23GEnable(enable23g);
			processNetworkChange(context);
		}
	}
}
