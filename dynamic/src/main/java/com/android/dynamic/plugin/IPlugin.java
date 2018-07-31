package com.android.dynamic.plugin;

import android.content.Context;

public interface IPlugin {
	/**
	 * 注入框架为插件生成的context
	 * 
	 * @Title: initContext
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param ctx
	 *            框架端的上下文
	 * @return void
	 * @throws
	 */
	public void initContext(Context ctx);

	/**
	 * 提供给插件使用,判断自身的模式是安装还是动态加载
	 * 
	 * @Title: isDynamicMode
	 * @author lytjackson@gmail.com
	 * @date 2014-3-26
	 * @return
	 */
	public boolean isDynamicMode();
}