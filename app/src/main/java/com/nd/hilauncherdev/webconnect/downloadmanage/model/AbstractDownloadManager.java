package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.io.File;
import java.util.ArrayList;

import com.nd.hilauncherdev.framework.db.AbstractDataBase;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

public abstract class AbstractDownloadManager {

	private IDownloadManagerService mService;
	private DownloadServiceConnection mConnection;
	private Handler mHandler;
	private Context mAppContext;
	
	public static interface ResultCallback {
		void getResult(Object result);
	}
	
	public AbstractDownloadManager(Context context) {
		mAppContext = context.getApplicationContext();
		mConnection = new DownloadServiceConnection();
		mHandler = new Handler(mAppContext.getMainLooper());
		bindService();
	}
	
	protected abstract Class<? extends AbstractDataBase> getDownloadDb();
	
	protected abstract Class<? extends AbstractDownloadCallback> getDownloadCallback();
	
	protected abstract String getBroadcastAction();
	
	private void bindService() {
		if (mService != null) {
			return;
		}
		
		try {
			mAppContext.bindService(new Intent(mAppContext, DownloadServerService.class),
					 			    mConnection, 
					 			    Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void callService(final Runnable runnable) {
		callService(runnable, false);
	}
	
	private void callService(final Runnable runnable, final boolean runUseHandler) {
		callService(runnable, 0, runUseHandler);
	}
	
	private void callService(final Runnable runnable, final int count, final boolean runUseHandler) {
		if (mService != null) {
			if (runUseHandler) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						runnable.run();
					}
				});
			} else {
				runnable.run();
			}
		} else {
			if (count >= 3) {
				return;
			}
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					callService(runnable, count+1, runUseHandler);
				}
			}, 1500);
		}
	}
	
	/**
	 * 添加前台任务
	 * @param info 下载任务信息
	 */
	public void addNormalTask(final BaseDownloadInfo info, final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				if (mService == null) {
					return;
				}
				
				try {
					boolean result = mService.addDownloadTask(info);
					if (callback != null) {
						callback.getResult(Boolean.valueOf(result));
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}, true);
	}
	
	/**
	 * 批量添加前台任务
	 * @param infos 下载任务信息
	 */
	public void addNormalTask(final ArrayList<BaseDownloadInfo> infos) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					for (BaseDownloadInfo info : infos) {
						if (info == null) {
							continue;
						}
						mService.addDownloadTask(info);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}, true);
	}
	
	/**
	 * 暂停前台任务
	 * @param identification 任务标识
	 * @return Boolean
	 */
	public void pauseNormalTask(final String identification, final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					boolean result = mService.pause(identification);
					if (callback != null) {
						callback.getResult(Boolean.valueOf(result));
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 取消前台任务
	 * @param identification 任务标识
	 */
	public void cancelNormalTask(final String identification, final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					boolean result = mService.cancel(identification);
					if (callback != null) {
						callback.getResult(Boolean.valueOf(result));
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 继续前台任务
	 * @param identification 任务标识
	 */
	public void continueNormalTask(final String identification, final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					boolean result = mService.continueDownload(identification);
					if (callback != null) {
						callback.getResult(Boolean.valueOf(result));
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 获取前台任务数
	 * @return Integer
	 */
	public void getTaskCount(final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					Object result = mService.getTaskCount();
					if (callback != null) {
						callback.getResult(result);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 获取所有前台任务
	 * @return Map<String, BaseDownloadInfo>
	 */
	public void getNormalDownloadTasks(final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					Object result = mService.getDownloadTasks();
					if (callback != null) {
						callback.getResult(result);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 获取前台任务
	 * @param identification 标识
	 * @return BaseDownloadInfo
	 */
	public void getNormalDownloadTask(final String identification, final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					Object result = mService.getDownloadTask(identification);
					if (callback != null) {
						callback.getResult(result);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 是否正在安装
	 * @param packageName 包名
	 * @return Boolean
	 */
	public void isApkInstalling(final String packageName, final ResultCallback callback) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					Object result = mService.isApkInstalling(packageName);
					if (callback != null) {
						callback.getResult(result);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * 静默安装
	 * @param apkFile
	 */
	public void installAppSilent(final File apkFile) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.installAppInThread(apkFile.getAbsolutePath());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 添加后台任务
	 * @param info 下载任务信息
	 */
	public void addSilentTask(final BaseDownloadInfo info) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.addSilentDownloadTask(info);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 添加可在23G下进行下载的后台任务
	 * @param info 下载任务信息
	 * @param enable23G 该任务是否开启23G下载
	 */
	public void addSilent23GTask(final BaseDownloadInfo info, final boolean enable23G) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.addSilent23GTask(info, enable23G);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 批量添加可在23G下进行下载的后台任务
	 * @param infos 下载任务信息
	 * @param enable23G 该任务是否开启23G下载
	 */
	public void addSilent23GTask(final ArrayList<BaseDownloadInfo> infos, final boolean enable23G) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					for (BaseDownloadInfo info : infos) {
						if (info == null) {
							continue;
						}
						mService.addSilent23GTask(info, enable23G);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 控制可在23G下进行下载的后台任务
	 * @param identification 任务标识
	 * @param enable23G 该任务是否开启23G下载
	 */
	public void controlSilent23GTask(final String identification, final boolean enable23G) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.controlSilent23GTask(identification, enable23G);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});	
	}
	
	/**
	 * 暂停后台任务
	 * @param identification 任务标识
	 * @param is23GTask 是否为支持23g的任务
	 */
	public void pauseSilentTask(final String identification, final boolean is23GTask) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.pauseSilentTask(identification, is23GTask);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});	
	}
	
	/**
	 * 继续后台任务
	 * @param identification 任务标识
	 * @param is23GTask 是否为支持23g的任务
	 */
	public void continuteSilentTask(final String identification, final boolean is23GTask) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.continuteSilentTask(identification, is23GTask);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});	
	}
	
	/**
	 * 取消后台任务
	 * @param identification 任务标识
	 * @param is23GTask 是否为支持23g的任务
	 */
	public void cancelSilentTask(final String identification, final boolean is23GTask) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.cancelSilentTask(identification, is23GTask);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});	
	}
	
	/**
	 * 修改任务的附加信息
	 */
	public void modifyAdditionInfo(final BaseDownloadInfo info) {
		callService(new Runnable() {
			@Override
			public void run() {
				try {
					mService.modifyAdditionInfo(info);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});	
	}
	
	private class DownloadServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IDownloadManagerService.Stub.asInterface(service);
			
			try {
				mService.setDownloadCallback(getDownloadCallback().getName());
				mService.setDownloadDb(getDownloadDb().getName());
				mService.setBroadcastAction(getBroadcastAction());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	}
}
