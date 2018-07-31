package android.app;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.dynamic.plugin.IActivityGroupPlugin;
import com.android.dynamic.plugin.PluginActivity;
import com.android.dynamic.plugin.PluginUtil;

/**
 * 插件开发ActivityGroup时,需要继承此类
 * 
 * @ClassName: PluginActivityGroup
 * @author lytjackson@gmail.com
 * @date 2014-2-27
 * 
 */
public class PluginActivityGroup extends ActivityGroup implements IActivityGroupPlugin {
	private Context mPluginContext = null;
	private Activity mPluginLoaderActivity;
	private View mPluginContentView;
	private LayoutInflater mLayoutInflater;
	private boolean mPluginMode = false;
	protected String mPluginPackageName = "";

	private static final String STATES_KEY = "android:states";

	/**
	 * 提交打点行为统计
	 * @Title: submitEvent
	 * @author lytjackson@gmail.com
	 * @date 2014-4-22
	 * @param context
	 * @param eventId
	 */
	public void submitEvent(Context context, int eventId) {
		submitEvent(mPluginContext, eventId, "");
	}

	/**
	 * 提交打点行为统计
	 * @Title: submitEvent
	 * @author lytjackson@gmail.com
	 * @date 2014-4-22
	 * @param context
	 * @param eventId
	 * @param label
	 */
	public void submitEvent(Context context, int eventId, String label) {
		PluginUtil.invokeSubmitEvent(mPluginLoaderActivity, eventId, label);
	}
	
	@Override
	public String getPluginPackageName() {
		return mPluginPackageName;
	}
	
	@Override
	public boolean isDynamicMode() {
		return mPluginMode;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (mPluginMode) {
			// do anything, but do not call super.onCaeat() !!
		} else {
			super.onCreate(savedInstanceState);
		}
		Bundle states = savedInstanceState != null ? (Bundle) savedInstanceState.getBundle(STATES_KEY) : null;
		mLocalActivityManager.dispatchCreate(states);
	}

	@Override
	protected void onResume() {
		if (mPluginMode) {
			mLocalActivityManager.dispatchResume();
		} else {
			super.onResume();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Bundle state = mLocalActivityManager.saveInstanceState();
		if (state != null) {
			outState.putBundle(STATES_KEY, state);
		}
	}

	@Override
	protected void onPause() {
		if (mPluginMode) {
			mLocalActivityManager.dispatchPause(isFinishing());
		} else {
			super.onPause();
		}
	}

	@Override
	protected void onStop() {
		if (mPluginMode) {
			mLocalActivityManager.dispatchStop();
		} else {
			super.onStop();
		}
	}

	@Override
	protected void onDestroy() {
		if (mPluginMode) {
			mLocalActivityManager.dispatchDestroy(isFinishing());
		} else {
			super.onDestroy();
		}
	}

	@Override
	public HashMap<String, Object> onRetainNonConfigurationChildInstances() {
		return mLocalActivityManager.dispatchRetainNonConfigurationInstance();
	}

	public Activity getCurrentActivity() {
		return mLocalActivityManager.getCurrentActivity();
	}

	@Override
	void dispatchActivityResult(String who, int requestCode, int resultCode, Intent data) {
		if (who != null) {
			Activity act = mLocalActivityManager.getActivity(who);
			if (act != null) {
				act.onActivityResult(requestCode, resultCode, data);
				return;
			}
		}
		super.dispatchActivityResult(who, requestCode, resultCode, data);
	}

	@Override
	public void setParentContext(Activity activity) {
		this.mPluginLoaderActivity = activity;
	}

	@Override
	public void setActivityManager(LocalActivityManager activityManager) {
		this.mLocalActivityManager = activityManager;
	}

	@Override
	public void initContext(Context ctx) {
		mPluginContext = ctx;
		mPluginMode = true;
		attachBaseContext(ctx);
	}

	@Override
	public View findViewById(int id) {
		View view = null;
		if (mPluginContentView != null && mPluginMode) {
			view = mPluginContentView.findViewById(id);
		} else {
			view = super.findViewById(id);
		}
		return view;
	}

	@Override
	public void finish() {
		if (mPluginMode) {
			mPluginLoaderActivity.finish();
		} else {
			super.finish();
		}
	}

	@Override
	public int getChangingConfigurations() {
		if (mPluginMode) {
			return mPluginLoaderActivity.getChangingConfigurations();
		} else {
			return super.getChangingConfigurations();
		}
	}

	@Override
	public LayoutInflater getLayoutInflater() {
		if (mPluginMode) {
			return getDynamicInflate(mPluginContext);
		} else {
			return super.getLayoutInflater();
		}
	}

	@Override
	public MenuInflater getMenuInflater() {
		if (mPluginContext != null && mPluginMode) {
			return new MenuInflater(mPluginContext);
		} else {
			return super.getMenuInflater();
		}
	}

	// @Override
	// protected void onCreate(Bundle savedInstanceState) {
	// if (mPluginMode) {
	// // do anything, but do not call super.onCaeat() !!
	// } else {
	// super.onCreate(savedInstanceState);
	// }
	// }

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		if (mPluginMode) {
			return mPluginContext.getSharedPreferences(name, mode);
		} else {
			return super.getSharedPreferences(name, mode);
		}
	}

	@Override
	public File getSharedPrefsFile(String name) {
		if (mPluginMode) {
			return mPluginContext.getSharedPrefsFile(name);
		} else {
			return super.getSharedPrefsFile(name);
		}
	}

	@Override
	public Object getSystemService(String name) {
		if (!mPluginMode) {
			return super.getSystemService(name);
		}
		if ((Context.WINDOW_SERVICE.equals(name)) || (Context.SEARCH_SERVICE.equals(name))) {
			return mPluginLoaderActivity.getSystemService(name);
		} else if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
			return getDynamicInflate(mPluginContext);
		} else {
			return mPluginContext.getSystemService(name);
		}
	}

	@Override
	public Window getWindow() {
		if (mPluginMode) {
			return mPluginLoaderActivity.getWindow();
		} else {
			return super.getWindow();
		}
	}

	@Override
	public WindowManager getWindowManager() {
		if (mPluginMode) {
			return mPluginLoaderActivity.getWindowManager();
		} else {
			return super.getWindowManager();
		}
	}

	@Override
	protected void onStart() {
		if (!mPluginMode) {
			super.onStart();
		}
	}

	// @Override
	// protected void onPause() {
	// if (!mPluginMode) {
	// super.onPause();
	// }
	// }
	//
	// @Override
	// protected void onStop() {
	// if (!mPluginMode) {
	// super.onStop();
	// }
	// }
	//
	// @Override
	// protected void onResume() {
	// if (!mPluginMode) {
	// super.onResume();
	// }
	// }
	//
	// @Override
	// protected void onDestroy() {
	// if (!mPluginMode) {
	// super.onDestroy();
	// }
	// }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mPluginMode) {
			return false;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (mPluginMode) {
			return false;
		} else {
			return super.onKeyMultiple(keyCode, repeatCount, event);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (mPluginMode) {
			return false;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}
	
	@Override
	public View getCurrentFocus() {
		if (mPluginMode) {
			return mPluginLoaderActivity.getCurrentFocus();
		} else {
			return super.getCurrentFocus();
		}
	}
	
	@Override
	public void onBackPressed() {
		if (mPluginMode) {
			mPluginLoaderActivity.finish();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void setContentView(int layoutResID) {
		if (mPluginMode) {
			mPluginContentView = getDynamicInflate(mPluginContext).inflate(layoutResID, null);
			mPluginLoaderActivity.setContentView(mPluginContentView);
		} else {
			super.setContentView(layoutResID);
		}
	}

	private LayoutInflater getDynamicInflate(final Context ctx) {
		if (null != mLayoutInflater) {
			return mLayoutInflater;
		}
		mLayoutInflater = LayoutInflater.from(ctx).cloneInContext(this);
		return mLayoutInflater;
	}

	@Override
	public void setContentView(View view) {
		if (mPluginMode) {
			mPluginContentView = view;
			mPluginLoaderActivity.setContentView(view);
		} else {
			super.setContentView(view);
		}

	}

	@Override
	public void startActivity(Intent intent) {
		if (mPluginMode) {
			this.startActivityForResult(intent, -1);
		} else {
			super.startActivity(intent);
		}
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		if (mPluginMode) {
			List<ResolveInfo> resolveInfo = mPluginLoaderActivity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if ((resolveInfo == null) || (resolveInfo.size() <= 0)) {
				intent.putExtra(PluginActivity.PLUGIN_PKG_NAME, mPluginPackageName);
				intent.putExtra(PluginActivity.IS_EXPLICIT_INTENT, true);
			}
			if (requestCode >= 0) {
				mPluginLoaderActivity.startActivityForResult(intent, requestCode);
			} else {
				mPluginLoaderActivity.startActivity(intent);
			}
		} else {
			super.startActivityForResult(intent, requestCode);
		}
	}

	@Override
	public ComponentName startService(Intent service) {
		if (mPluginMode) {
			List<ResolveInfo> resolveInfo = mPluginLoaderActivity.getPackageManager().queryIntentServices(service, PackageManager.MATCH_DEFAULT_ONLY);
			if ((resolveInfo == null) || (resolveInfo.size() <= 0)) {
				service.putExtra(PluginActivity.PLUGIN_PKG_NAME, mPluginPackageName);
				service.putExtra(PluginActivity.IS_EXPLICIT_INTENT, true);
			}
			return mPluginLoaderActivity.startService(service);
		} else {
			return super.startService(service);
		}
	}

	@Override
	public boolean stopService(Intent name) {
		if (mPluginMode) {
			List<ResolveInfo> resolveInfo = mPluginLoaderActivity.getPackageManager().queryIntentServices(name, PackageManager.MATCH_DEFAULT_ONLY);
			if ((resolveInfo == null) || (resolveInfo.size() <= 0)) {
				name.putExtra(PluginActivity.PLUGIN_PKG_NAME, mPluginPackageName);
				name.putExtra(PluginActivity.IS_EXPLICIT_INTENT, true);
			}
			return mPluginLoaderActivity.stopService(name);
		} else {
			return super.stopService(name);
		}
	}

	// 以下部分为替换activity的某些final方法,统一命名为xxReplaced,插件客户端使用如下api

	/**
	 * 替换系统Activity的managedQuery方法
	 * 
	 * @Title: managedQueryReplaced
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param sortOrder
	 * @return
	 */
	public Cursor managedQueryReplaced(Uri uri, String[] projection, String selection, String sortOrder) {
		if (mPluginMode) {
			return mPluginLoaderActivity.managedQuery(uri, projection, selection, sortOrder);
		} else {
			return super.managedQuery(uri, projection, selection, sortOrder);
		}
	}

	/**
	 * 替换系统Activity的setResult方法
	 * 
	 * @Title: setResultReplaced
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param resultCode
	 * @param data
	 */
	public void setResultReplaced(int resultCode, Intent data) {
		if (mPluginMode) {
			mPluginLoaderActivity.setResult(resultCode, data);
		} else {
			setResult(resultCode, data);
		}
	}

	/**
	 * 替换系统Activity的setResult方法
	 * 
	 * @Title: setResultReplaced
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param resultCode
	 */
	public void setResultReplaced(int resultCode) {
		if (mPluginMode) {
			mPluginLoaderActivity.setResult(resultCode);
		} else {
			setResult(resultCode);
		}
	}

	/**
	 * 替换系统Activity的managedQuery方法
	 * 
	 * @Title: managedQueryReplaced
	 * @author lytjackson@gmail.com
	 * @date 2013-11-1
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param selectionArgs
	 * @param sortOrder
	 * @return
	 */
	public Cursor managedQueryReplaced(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (mPluginMode) {
			return mPluginLoaderActivity.managedQuery(uri, projection, selection, selectionArgs, sortOrder);
		} else {
			return super.managedQuery(uri, projection, selection, selectionArgs, sortOrder);
		}
	}

}
