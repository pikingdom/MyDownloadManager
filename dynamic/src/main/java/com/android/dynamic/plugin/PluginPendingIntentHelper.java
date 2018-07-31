package com.android.dynamic.plugin;

import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.dynamic.Exception.MethodInvokeErrorException;

/**
 * 获取通知栏PendingIntent的辅助类 注意: 插件中定义的静态广播无法收到,不支持隐式intent
 * 
 * @ClassName: PluginPendingIntentHelper
 * @Description: 获取通知栏PendingIntent
 * @author lytjackson@gmail.com
 * @date 2014-3-12
 * 
 */
public class PluginPendingIntentHelper {

	private static final String NOTIFY_TYPE = "notify_type";
	/**
	 * 服务中使用PendingIntent时，需要在intent中传入插件包名的数据，使用此key	
	 */
	public static final String NOTIFY_PLUGIN_PKGNAME = "notify_plugin_pkgname";
	/**
	 * 转发通知的广播接收器的action
	 */
	private static final String NOTIFICATION_RECEIVER_ACTION = "com.baidu.android.action.RECEIVE_NOTIFICATION";
	private static final int PENDING_TYPE_ACTIVITY = 1;
	private static final int PENDING_TYPE_SERVICE = 2;

	/**
	 * 获取activity类型PendingIntent, context必须是继承sdk中定义的各类型Activity,否则抛出异常.
	 * eg:PluginActivity
	 * ,PluginListActivity,PluginActivityGroup,PluginTabActivity
	 * 
	 * @Title: getActivity
	 * @author lytjackson@gmail.com
	 * @date 2014-3-12
	 * @param context
	 *            The Context in which this PendingIntent should start the
	 *            activity.
	 * @param requestCode
	 *            The Context in which this PendingIntent should start the
	 *            activity.
	 * @param intent
	 *            The Context in which this PendingIntent should start the
	 *            activity.
	 * @param flags
	 *            May be FLAG_ONE_SHOT, FLAG_NO_CREATE, FLAG_CANCEL_CURRENT,
	 *            FLAG_UPDATE_CURRENT, or any of the flags as supported by
	 *            Intent.fillIn() to control which unspecified parts of the
	 *            intent that can be supplied when the actual send happens.
	 * @return Returns an existing or new PendingIntent matching the given
	 *         parameters. May return null only if FLAG_NO_CREATE has been
	 *         supplied.
	 */
	public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
		List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		PendingIntent pi = null;
		if ((resolveInfo == null) || (resolveInfo.size() <= 0)) {
			Intent newIntent = assembleCommonIntent(context, intent);
			newIntent.putExtra(NOTIFY_TYPE, PENDING_TYPE_ACTIVITY);
			pi = PendingIntent.getBroadcast(context, requestCode, newIntent, flags);
		} else {
			pi = PendingIntent.getActivity(context, requestCode, intent, flags);
		}
		return pi;
	}

	/**
	 * 获取service类型PendingIntent, context必须是继承sdk中定义的各类型Activity,否则抛出异常.
	 * eg:PluginActivity
	 * ,PluginListActivity,PluginActivityGroup,PluginTabActivity
	 * 
	 * @Title: getService
	 * @author lytjackson@gmail.com
	 * @date 2014-3-12
	 * @param context
	 *            The Context in which this PendingIntent should start the
	 *            service.
	 * @param requestCode
	 *            Private request code for the sender (currently not used).
	 * @param intent
	 *            An Intent describing the service to be started.
	 * @param flags
	 *            May be FLAG_ONE_SHOT, FLAG_NO_CREATE, FLAG_CANCEL_CURRENT,
	 *            FLAG_UPDATE_CURRENT, or any of the flags as supported by
	 *            Intent.fillIn() to control which unspecified parts of the
	 *            intent that can be supplied when the actual send happens.
	 * @return Returns an existing or new PendingIntent matching the given
	 *         parameters. May return null only if FLAG_NO_CREATE has been
	 *         supplied.
	 */
	public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
		List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY);
		PendingIntent pi = null;
		if ((resolveInfo == null) || (resolveInfo.size() <= 0)) {
			Intent newIntent = assembleCommonIntent(context, intent);
			newIntent.putExtra(NOTIFY_TYPE, PENDING_TYPE_SERVICE);
			pi = PendingIntent.getBroadcast(context, requestCode, newIntent, flags);
		} else {
			pi = PendingIntent.getService(context, requestCode, intent, flags);
		}
		return pi;
	}

	/**
	 * 获取broadcast类型PendingIntent 插件中定义的静态广播无法收到
	 * 
	 * @Title: getBroadcast
	 * @author lytjackson@gmail.com
	 * @date 2014-3-12
	 * @param context
	 *            The Context in which this PendingIntent should perform the
	 *            broadcast.
	 * @param requestCode
	 *            Private request code for the sender (currently not used).
	 * @param intent
	 *            The Intent to be broadcast.
	 * @param flags
	 *            May be FLAG_ONE_SHOT, FLAG_NO_CREATE, FLAG_CANCEL_CURRENT,
	 *            FLAG_UPDATE_CURRENT, or any of the flags as supported by
	 *            Intent.fillIn() to control which unspecified parts of the
	 *            intent that can be supplied when the actual send happens.
	 * @return Returns an existing or new PendingIntent matching the given
	 *         parameters. May return null only if FLAG_NO_CREATE has been
	 *         supplied.
	 */
	public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
		return PendingIntent.getBroadcast(context, requestCode, intent, flags);
	}

	private static Intent assembleCommonIntent(Context context, Intent intent) {
		if (!(context instanceof IActivityPlugin)) {
			if (null == intent || "".equals(intent.getStringExtra(NOTIFY_PLUGIN_PKGNAME))) {
				throw new MethodInvokeErrorException();
			}
		}
		Intent newIntent;
		if (intent == null) {
			newIntent = new Intent(NOTIFICATION_RECEIVER_ACTION);
		} else {
			newIntent = (Intent) intent.clone();
			newIntent.setAction(NOTIFICATION_RECEIVER_ACTION);
			newIntent.setComponent(null);
		}
		newIntent.putExtra(PluginActivity.IS_EXPLICIT_INTENT, true);
		newIntent.putExtra(PluginActivity.PLUGIN_PKG_NAME, tryToGetPluginPkgName(context, intent));
		newIntent.putExtra("pluginLoaderActivity.MainClassName", null == intent.getComponent() ? "" : intent.getComponent().getClassName());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return newIntent;
	}

	private static String tryToGetPluginPkgName(Context context, Intent intent) {
		if (context instanceof IActivityPlugin) {
			return ((IActivityPlugin) context).getPluginPackageName();
		} else {
			return intent.getStringExtra(NOTIFY_PLUGIN_PKGNAME);
		}
	}
}
