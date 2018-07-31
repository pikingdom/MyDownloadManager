package com.android.dynamic.plugin;

import android.app.LocalActivityManager;

public interface IActivityGroupPlugin extends IActivityPlugin {

	/**
	 * 组件对象为TabActivity、ActivityGroup时,注入框架为插件生成的自定义LocalActivityManager
	 * @Title: setActivityManager
	 * @author lytjackson@gmail.com
	 * @date 2014-2-26
	 * @param activityManager
	 */
	public void setActivityManager(LocalActivityManager activityManager);

}