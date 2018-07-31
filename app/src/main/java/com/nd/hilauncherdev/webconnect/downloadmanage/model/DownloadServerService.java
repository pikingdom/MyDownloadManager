package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.nd.hilauncherdev.core.DownloadFileUtil;
import com.nd.hilauncherdev.core.DownloadHiBroadcastReceiver;
import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.kitset.util.OpenRootUtil;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;

/**
 * 下载管理进程向外提供的进程服务，供第三方进程调用
 * 
 * @author pdw
 * @date 2013-1-8 上午10:51:40
 */
public class DownloadServerService extends Service {

	private final String TAG = "DownloadServerService";

	public static final String ACTION_ADD_NEW = "com.nd.android.pandahome2.downloadmanager.ADD_NEW";
	public static final String EXTRA_SHOW_TYPE = "SHOW_TYPE";
	public static final String RECOMMEND_PREFIX = "recommend-";
	public static final String EXTRA_DB_CLASS = "DB_CLASS";
	public static final int SHOW_TYPE_APK = 0;
	public static final int SHOW_TYPE_RING = 1;
	public static final int SHOW_TYPE_FONT = 2;
	public static final int SHOW_TYPE_THEME = 3;
	public static final int SHOW_TYPE_WALLPAPER = 4;
	public static final int SHOW_TYPE_LOCK = 5;
	public static final int SHOW_TYPE_ICON = 6;
	public static final int SHOW_TYPE_INPUT = 7;
	public static final int SHOW_TYPE_SMS = 8;
	public static final int SHOW_TYPE_WEATHER = 9;
	public static final int SHOW_TYPE_STYLE = 10;
	public static final int SHOW_TYPE_ALL = 11;
	
	private boolean mIsServiceAlive = false;

	private Context mContext;
	/**
	 * 总的队列，包括正在下载，等待、已下载
	 */
	public Map<String, BaseDownloadInfo> mAllDownloadTasks = null;

	/**
	 * 正在安装的APK的标记，可以知道哪个apk是否正在安装，
	 */
	private Set<String> mInstallingSet = new HashSet<String>();

	/**
	 * 线程池
	 */
	private ExecutorService executorService;

	/**
	 * 新应用安装
	 */
	private BroadcastReceiver mNewAppInstallReceiver;
	
	private BroadcastReceiver mNetworkChangeReceiver;

	static AbstractDownloadCallback sDownloadCallback = null;
	static String sDbClass = null;
	public static String sBroadcastAction = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return mDownloadServiceImpl;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = getApplicationContext();
//		if (mAllDownloadTasks == null) {
//			mAllDownloadTasks = DownloadDBManager.getDownloadLoadTask(getApplicationContext());
//			
//			SilentDownloadWorkerSupervisor.processServiceCreate(this.getApplication());
//			Silent23GDownloadWorkerSupervisor.processServiceCreate(this.getApplication());
//		}

		/**
		 * 线程池
		 */
		executorService = Executors.newFixedThreadPool(1);
		mIsServiceAlive = true;
		try {

			// 注册软件安装广播监听，有新软件安装，要过滤掉
			if (mNewAppInstallReceiver == null) {
				mNewAppInstallReceiver = new NewAppInstallReceiver();
				IntentFilter itFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
				itFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
				itFilter.addDataScheme("package");
				registerReceiver(mNewAppInstallReceiver, itFilter);
			}

			if (mNetworkChangeReceiver == null) {
				mNetworkChangeReceiver = new NetworkChangeReceiver();
				IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
				registerReceiver(mNetworkChangeReceiver, filter);
			}
			
		} catch (Exception e) {
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		try {
			if (mNewAppInstallReceiver != null) {
				unregisterReceiver(mNewAppInstallReceiver);
				mNewAppInstallReceiver = null;
			}

			if (mNetworkChangeReceiver != null) {
				unregisterReceiver(mNetworkChangeReceiver);
				mNetworkChangeReceiver = null;
			}
		} catch (Exception e) {
		}

		if (mAllDownloadTasks != null) {
			mAllDownloadTasks.clear();
			mAllDownloadTasks = null;
		}
		
		mIsServiceAlive = false;
		sDbClass = null;
		sDownloadCallback = null;
	}

	private IDownloadManagerService.Stub mDownloadServiceImpl = new IDownloadManagerService.Stub() {

		/**
		 * 判断服务是否活动中
		 */
		public boolean isServiceAlive() {
			return mIsServiceAlive;
		}

		/**
		 * 添加到下载队列
		 */
		@Override
		public boolean addDownloadTask(BaseDownloadInfo info) throws RemoteException {
			if (!isInfoValid(info))
				return false;
			
			if (info.getFileType() == BaseDownloadInfo.FILE_TYPE_THEME_SERIES
					|| info.getFileType() == BaseDownloadInfo.FILE_VIDEO_WALLPAPER_SERIES
					|| info.getFileType() == BaseDownloadInfo.FILE_VIDEO_PAPER) {
				info.initSubDownloadInfoList();
			}
			
			BaseDownloadInfo duplicatedInfo = getDuplicatedTask(info);
			if (duplicatedInfo != null) {				
				info = duplicatedInfo;
			} else {
				info.setBeginTime(getApplicationContext(), System.currentTimeMillis());
				
				SilentDownloadWorkerSupervisor.cancelSimilarTask(info);
				info.initState(getApplicationContext());
			}
						
			Intent intent = new Intent(ACTION_ADD_NEW);
			sendBroadcast(intent);
			
			boolean addsuccess = BaseDownloadWorkerSupervisor.addDownloadTask(getApplicationContext(), info);
			if (addsuccess && duplicatedInfo == null) {
				mAllDownloadTasks.put(info.getIdentification(), info);
			}
			return addsuccess;
		}

		private BaseDownloadInfo getDuplicatedTask(BaseDownloadInfo info) {
			return mAllDownloadTasks.get(info.getIdentification());
		}
				
		private boolean isInfoValid(BaseDownloadInfo info) {
			if (info == null
				|| info.getIdentification() == null
				|| info.getDownloadUrl() == null
				|| info.getDownloadUrl().equals(""))
				return false;
			
			return true;
		}
		
		@Override
		public boolean addSilentDownloadTask(BaseDownloadInfo info) throws RemoteException {
			if (!isInfoValid(info))
				return false;
			
			if (mAllDownloadTasks != null) {
				Collection<BaseDownloadInfo> c = mAllDownloadTasks.values();
		        Iterator<BaseDownloadInfo> it = c.iterator();
		        while(it.hasNext()) {
		        	BaseDownloadInfo tmpInfo = it.next();
					if (tmpInfo.getIdentification().equals(info.getIdentification()) 
						|| tmpInfo.getFilePath().equals(info.getFilePath())) {
						return false;
					}
		        }
			}
			
			return SilentDownloadWorkerSupervisor.addDownloadTask(getApplicationContext(), info);
		}
		
		@Override
		public boolean addSilent23GTask(BaseDownloadInfo info, boolean enable23g) throws RemoteException {
			if (!isInfoValid(info))
				return false;

			return Silent23GDownloadWorkerSupervisor.addDownloadTask(getApplicationContext(), info, enable23g);
		}
		
		@Override
		public void controlSilent23GTask(String identification, boolean enable23g) throws RemoteException {
			Silent23GDownloadWorkerSupervisor.controlSilent23GTask(getApplicationContext(), identification, enable23g);
		}
		
		@Override
		public void pauseSilentTask(String identification, boolean is23gTask) throws RemoteException {
			if (is23gTask) {
				Silent23GDownloadWorkerSupervisor.pause(identification, true);
			}	
		}

		@Override
		public void continuteSilentTask(String identification, boolean is23gTask) throws RemoteException {
			if (is23gTask) {
				Silent23GDownloadWorkerSupervisor.continute(getApplicationContext(), identification);
			}
		}

		@Override
		public void cancelSilentTask(String identification, boolean is23gTask) throws RemoteException {
			if (is23gTask) {
				Silent23GDownloadWorkerSupervisor.cancel(getApplicationContext(), identification);
			}
		}
		
		/**
		 * 暂停
		 */
		@Override
		public boolean pause(String identification) throws RemoteException {
			boolean b = BaseDownloadWorkerSupervisor.pause(identification);
			return b;
		}

		/**
		 * 继续下载
		 * 
		 * @param identification
		 * @return
		 * @throws RemoteException
		 */
		public boolean continueDownload(String identification) throws RemoteException {
			BaseDownloadInfo info = mAllDownloadTasks.get(identification);
			if (info == null)
				return false;
			return addDownloadTask(info);
		}

		/**
		 * 取消
		 */
		@Override
		public boolean cancel(String identification) throws RemoteException {

			boolean cancelSuccess = false;
			try {
				cancelSuccess = BaseDownloadWorkerSupervisor.cancel(getApplicationContext(), identification);
				if (cancelSuccess) {
					BaseDownloadInfo dlInfo = mAllDownloadTasks.remove(identification);
					if (dlInfo != null) {
						// 删除文件、临时文件
						if (dlInfo.getFileType() != BaseDownloadInfo.FILE_TYPE_WALLPAPER) {
							DownloadFileUtil.delFile(dlInfo.getFilePath());
						}
						
						DownloadFileUtil.delFile(dlInfo.getFilePath() + AbstractDownloadWorker.getTempSuffix(dlInfo.getIsSilent()));
						dlInfo.deleteFileList();
						// 暂停状态取消通知栏，并发广播
						// 下载完未安装状态，发送取消广播 add by huangmin
						if (dlInfo.getState() == DownloadState.STATE_PAUSE || dlInfo.getState() == DownloadState.STATE_FINISHED) {
//							DownloadNotification.downloadCancelledNotification(mContext, Math.abs(dlInfo.getDownloadUrl().hashCode()));
							BaseDownloadWorker.sendCancleDownloadLogBroadcast(mContext, dlInfo.getIdentification(), dlInfo.getDownloadUrl());
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return cancelSuccess;
		}

		/**
		 * 清除总队列
		 */
		public void clearAllDownloadTask() {
			BaseDownloadWorkerSupervisor.clearWaitingQueue();
			mAllDownloadTasks.clear();
		}

		/**
		 * 获取下载总队列
		 * 
		 * @return
		 */
		public Map<String, BaseDownloadInfo> getDownloadTasks() {
			return mAllDownloadTasks;
		}

		/**
		 * 获取单个下载任务
		 * @param identification 标识
		 */
		public BaseDownloadInfo getDownloadTask(String identification) {
			BaseDownloadInfo info = null;
			if (identification != null && mAllDownloadTasks != null) {
				info = mAllDownloadTasks.get(identification);
			}
			return info;
		}
		
		/**
		 * 返回任务数，包括所有状态的
		 * 
		 * @return
		 */
		public int getTaskCount() {
			int count = 0;
			Collection<BaseDownloadInfo> values = mAllDownloadTasks.values();
			for (BaseDownloadInfo apkInfo : values) {
				switch (apkInfo.getState()) {
				case DownloadState.STATE_DOWNLOADING:
				case DownloadState.STATE_PAUSE:
				case DownloadState.STATE_WAITING:
					count++;
					break;
				}
			}

			return count;
		}

		/**
		 * 是否应用正安装中
		 * 
		 * @param identification
		 * @return
		 */
		public boolean isApkInstalling(String packageName) {
			return mInstallingSet.contains(packageName);
		}

		/**
		 * 启动安装线程
		 * 
		 * @param context
		 * @param apkFile
		 */
		public void installAppInThread(final String apkPath) {
			final File apkFile = new File(apkPath);
			final PackageInfo info = getApkFilePackageInfo(mContext, apkFile.getAbsolutePath());
			if (isApkInstalling(info.packageName))
				return;

			installStart(info.packageName, apkFile.getAbsolutePath());
			executorService.execute(new Runnable() {

				@Override
				public void run() {
					boolean installResult = installApplicationSilent(apkFile);
					installFinish(installResult, info.packageName, apkFile.getAbsolutePath());
					if(!installResult)
						installApplicationNormal(mContext, apkFile);
				}
			});
		}

		/**
		 * 安装应用程序,普通安装方式
		 * @param ctx
		 * @param mainFile
		 * @return boolean
		 */
		private boolean installApplicationNormal(Context ctx, File mainFile) {
			try {
				Uri data = Uri.fromFile(mainFile);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setDataAndType(data, "application/vnd.android.package-archive");
				ctx.startActivity(intent);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		
		/**
		 * 获取apk包的PackageInfo
		 * @param context
		 * @param apkFilePath
		 * @return 没有获取到或异常时返回null
		 */
		private PackageInfo getApkFilePackageInfo(Context context, String apkFilePath) {
			try {
				PackageManager pm = context.getPackageManager();
				PackageInfo info = pm.getPackageArchiveInfo(apkFilePath, 0);
				return info;
			} catch (Exception e) {
			}
			return null;
		}
		
		/**
		 * 获取指定identification的下载状态
		 */
		public BaseDownloadInfo getDownloadState(String identification) {
			BaseDownloadInfo info = mAllDownloadTasks.get(identification);
			return info;
		}

		/**
		 * 静默安装
		 * 
		 * @param apkFile
		 * @return
		 */
		private synchronized boolean installApplicationSilent(File apkFile) {
			Log.d(TAG, "start install " + apkFile.getName());
			Process process = null;
			InputStream errorIs = null;
			InputStream successIs = null;
			try {
				process = OpenRootUtil.getSuperProcess("export LD_LIBRARY_PATH=/vendor/lib:/system/lib \n",
													   "/system/bin/pm install -r " + apkFile.getAbsolutePath() + "\n",
													   "exit \n");

				// 启动线程，清理进程管道中的缓存，防止阻塞主进程
				new ProcessClearStream(process.getInputStream(), "INFO").start();
				new ProcessClearStream(process.getErrorStream(), "ERROR").start();

				// 等待执行返回
				int resCode = process.waitFor();
				StringBuffer resultSb = new StringBuffer();
				BufferedReader br = null;
				String line = null;
				if (resCode != 0) // 安装失败
				{
					errorIs = process.getErrorStream();
					if (errorIs != null) {
						br = new BufferedReader(new InputStreamReader(errorIs));
						while ((line = br.readLine()) != null) {
							resultSb.append(line).append("\n");
						}
						br.close();
						Log.e(TAG, "Install apk " + apkFile.getName() + " failed:" + resultSb.toString());
						return false;
					}
				} else {
					// 安装成功
					Log.d(TAG, "Install apk " + apkFile.getName() + " success");
				}
				return true;

			} catch (Exception e) {
				Log.w(TAG, "install apk failed!", e);
			} finally {
				try {
					if (errorIs != null)
						errorIs.close();
					if (successIs != null)
						successIs.close();
					if (process != null)
						process.destroy();
				} catch (Exception e2) {
				}

			}

			return false;
		}

		// TODO linqiang form ApkInstaller.INSTALL_STATE_INSTALLING
		/**
		 * 开始安装事件
		 * 
		 * @param context
		 * @param apkFile
		 */
		private void installStart(String packageName, String apkFilePath) {
			mInstallingSet.add(packageName);
			Intent intent = new Intent(DownloadState.RECEIVER_APP_SILENT_INSTALL);
			intent.putExtra(DownloadState.EXTRA_APP_INSTALL_STATE, DownloadState.INSTALL_STATE_INSTALLING);
			intent.putExtra(DownloadState.EXTRA_APP_INSTALL_PACAKGE_NAME, packageName);
			intent.putExtra(DownloadState.EXTRA_APP_INSTALL_APK_PATH, apkFilePath);
			mContext.sendBroadcast(intent);
		}

		/**
		 * 安装结束事件
		 * 
		 * @param context
		 * @param installSuccess
		 * @param apkFile
		 */
		private void installFinish(boolean installResult, String packageName, String apkFilePath) {
			mInstallingSet.remove(packageName);
			Intent intent = new Intent(DownloadState.RECEIVER_APP_SILENT_INSTALL);
			if (installResult == true) {
				intent.putExtra(DownloadState.EXTRA_APP_INSTALL_STATE, DownloadState.INSTALL_STATE_INSTALL_SUCCESS);
			} else {
				/*mUIHandler.post(new Runnable() {
					
					@Override
					public void run() {
						MessageUtils.makeShortToast(mContext, R.string.app_market_install_failed);
					}
				});*/
				
				intent.putExtra(DownloadState.EXTRA_APP_INSTALL_STATE, DownloadState.INSTALL_STATE_INSTALL_FAILED);
			}

			intent.putExtra(DownloadState.EXTRA_APP_INSTALL_PACAKGE_NAME, packageName);
			intent.putExtra(DownloadState.EXTRA_APP_INSTALL_APK_PATH, apkFilePath);

			mContext.sendBroadcast(intent);
		}

		@Override
		public void modifyAdditionInfo(BaseDownloadInfo info) throws RemoteException {
			if (info == null) {
				return;
			}
			BaseDownloadInfo toModifyInfo = getDownloadTask(info.getIdentification());
			if (toModifyInfo == null) {
				return;
			}
			
			toModifyInfo.modifyAdditionInfo(getApplicationContext(), info.getAdditionInfo());
		}

		@Override
		public void setDownloadCallback(String callbackCls) throws RemoteException {
			if (sDownloadCallback == null) {
				try {
					Class cls = Class.forName(callbackCls);
					sDownloadCallback = (AbstractDownloadCallback) cls.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void setDownloadDb(String dbCls) throws RemoteException {
			sDbClass = dbCls;
			if (mAllDownloadTasks == null) {
				mAllDownloadTasks = DownloadDBManager.getDownloadLoadTask(getApplicationContext());
					
				SilentDownloadWorkerSupervisor.processServiceCreate(getApplicationContext());
				Silent23GDownloadWorkerSupervisor.processServiceCreate(getApplicationContext());
			}
		}

		@Override
		public void setBroadcastAction(String action) throws RemoteException {
			sBroadcastAction = action;	
		}

	};// end IDownloadManagerService.Stub

	
	/**
	 * 清理管道缓存线程，可以实时输出安装过程
	 */
	public class ProcessClearStream extends Thread {
		private InputStream inputStream;
		//private String type;

		ProcessClearStream(InputStream inputStream, String type) {
			this.inputStream = inputStream;
			//this.type = type;
		}

		public void run() {
			try {
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader br = new BufferedReader(inputStreamReader);
				// 打印信息
				/*
				 * String line = null; while ((line = br.readLine()) != null) {
				 * Log.d(TAG, type + ">" + line); }
				 */
				// 不打印信息
				while (br.readLine() != null)
					;
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}// end ProcessClearStream

	/**
	 * 新应用安装监听
	 */
	private class NewAppInstallReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context content, Intent intent) {

			final String packageName = intent.getData().getSchemeSpecificPart();
			final String action = intent.getAction();
			if (packageName == null || action == null)
				return;

			if (action.equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED) && mInstallingSet != null)
				mInstallingSet.remove(packageName);

		}// end onReceiver

	}// end class NewAppInstallReceiver

	private class NetworkChangeReceiver extends DownloadHiBroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (null == context || null == intent)
				return;
			
			SilentDownloadWorkerSupervisor.processNetworkChange(getApplicationContext());
			Silent23GDownloadWorkerSupervisor.processNetworkChange(getApplicationContext());
		}
	}
}
