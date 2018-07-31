package com.android.dynamic.plugin;

import android.content.Context;

public interface IServicePlugin extends IPlugin {

	/**
	 * 注入框架Service上下文给插件端
	 * @Title: setParentContext
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param ctx
	 *            框架端的上下文
	 * @return void
	 * @throws
	 */
	public void setParentContext(Context ctx);

}