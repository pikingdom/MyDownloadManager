package com.android.dynamic.plugin;

public abstract interface DynamicWidgetListener {

	/**
	 * 小插件被添加至桌面时自动调用,可在该方法中完成初始化工作, 该方法会被桌面多次调用,比如桌面重启,插件拖动到其他屏等操作.
	 * 
	 * @Title: onLoad
	 * @author lytjackson@gmail.com
	 * @date 2013-12-24
	 * @param widgetId
	 *            每个小插件的唯一标识
	 */
	public abstract void onLoad(int widgetId);

	/**
	 * 小插件从桌面上被删除时自动调用，用于小部件本身的清理工作
	 * 
	 * @Title: onDestory
	 * @author lytjackson@gmail.com
	 * @date 2013-12-24
	 * @param widgetId
	 *            每个小插件的唯一标识
	 */
	public abstract void onDestory(int widgetId);
}