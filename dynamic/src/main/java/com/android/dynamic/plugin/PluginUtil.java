package com.android.dynamic.plugin;

import java.lang.reflect.Method;

import android.content.Context;

public class PluginUtil {
	private static final String ND_ANALYTICS = "com.nd.analytics.NdAnalytics";
	private static final String ANALYTICS_METHOD = "onEvent";

	/**
	 * 提交行为统计信息
	 * @Title: invokeSubmitEvent
	 * @author lytjackson@gmail.com
	 * @date 2014-4-22
	 * @param context	上下文
	 * @param eventId	事件id
	 * @param label		事件label
	 */
	public static void invokeSubmitEvent(Context context, int eventId, String label) {
		try {
			Class<?> clazz = context.getClassLoader().loadClass(ND_ANALYTICS);
			Method m = clazz.getDeclaredMethod(ANALYTICS_METHOD, new Class[] { Context.class, Integer.TYPE, String.class });
			m.setAccessible(true);
			m.invoke(clazz, new Object[] { context, eventId, label });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
