package com.android.dynamic.plugin;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

/**
 * 插件开发Application需要继承此类
 * 
 * @ClassName: PluginApplication
 * @author lytjackson@gmail.com
 * @date 2014-4-22
 * 
 */
public class PluginApplication extends Application implements IPlugin {
	private Context mPluginContext = null;
	private boolean mPluginMode = false;

	@Override
	public void onCreate() {
		if (mPluginMode) {
		} else {
			super.onCreate();
		}
	}

	@Override
	public boolean isDynamicMode() {
		return mPluginMode;
	}

	@Override
	public void onTerminate() {
		if (mPluginMode) {
		} else {
			super.onTerminate();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mPluginMode) {
		} else {
			super.onConfigurationChanged(newConfig);
		}
	}

	@Override
	public void onLowMemory() {
		if (mPluginMode) {
		} else {
			super.onLowMemory();
		}
	}

	@Override
	public Context getApplicationContext() {
		if (mPluginMode) {
			return mPluginContext.getApplicationContext();
		} else {
			return super.getApplicationContext();
		}
	}

	@Override
	public void initContext(Context ctx) {
		mPluginContext = ctx;
		mPluginMode = true;
		attachBaseContext(ctx);
	}
}
