package com.android.dynamic.plugin;

import android.app.Activity;

public interface IActivityPlugin extends IPlugin {
	/**
	 * 注入框架Activity对象给插件端
	 * 
	 * @Title: setParentContext
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param activity
	 *            框架端的activity实例
	 * @return void
	 * @throws
	 */
	public void setParentContext(Activity activity);

	/**
	 * 获取插件自身的包名
	 * 
	 * @Title: getPluginPackageName
	 * @author lytjackson@gmail.com
	 * @date 2014-4-22
	 * @return
	 */
	public String getPluginPackageName();
}