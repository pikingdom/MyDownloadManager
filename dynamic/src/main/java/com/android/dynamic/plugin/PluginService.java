package com.android.dynamic.plugin;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

/**
 * 插件开发服务需要继承此类
 * 
 * @ClassName: PluginService
 * @author lytjackson@gmail.com
 * @date 2014-4-22
 * 
 */
public class PluginService extends Service implements IServicePlugin {
	private Context mPluginContext = null;
	protected Service parentContext;
	private boolean mPluginMode = false;
	protected Handler parentHandler;

	@Override
	public void initContext(Context ctx) {
		mPluginContext = ctx;
		mPluginMode = true;
		attachBaseContext(mPluginContext);
	}

	@Override
	public boolean isDynamicMode() {
		return mPluginMode;
	}

	@Override
	public void setParentContext(Context ctx) {
		this.parentContext = (Service) ctx;
	}

	/**
	 * 方法废弃,动态插件不支持bindService,返回null
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		if (mPluginMode) {

		} else {
			super.onCreate();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mPluginMode) {
			return 9999;
		} else {
			return super.onStartCommand(intent, flags, startId);
		}
	}

	@Override
	public void onDestroy() {
		if (mPluginMode) {

		} else {
			super.onDestroy();
		}
	}

	@Override
	public ComponentName startService(Intent service) {
		if (mPluginMode) {
			return mPluginContext.startService(service);
		} else {
			return super.startService(service);
		}
	}

	@Override
	public boolean stopService(Intent name) {
		if (mPluginMode) {
			return mPluginContext.stopService(name);
		} else {
			return super.stopService(name);
		}
	}
}
