package com.nd.hilauncherdev.framework.httplib;

import java.util.Hashtable;

import android.util.Log;

/** 
 * 桌面内置app，widget下载管理
 */
public class CommonDownloadWorkerSupervisor {
	
	private static final Hashtable<String,AbstractDownloadWorker> threadPool = new Hashtable<String,AbstractDownloadWorker>();
	
	public static boolean add(String url,AbstractDownloadWorker worker){
		if(!threadPool.containsKey(url)){
			threadPool.put(url, worker);
			return true ;
		}else{
			Log.e("http", "download mission has been in the queue -> "+url);
			return false ;
		}
	}
	
	public static void remove(String url){
		threadPool.remove(url);
	}
	
	public static boolean isDownloading(String url) {
		return threadPool.containsKey(url);
	}
	
	public static AbstractDownloadWorker getDownloadingThread(String url) {
		return threadPool.get(url);
	}
}
