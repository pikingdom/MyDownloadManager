package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.nd.hilauncherdev.framework.httplib.AbstractDownloadWorker;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.DownloadStateDownloading;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.DownloadStateFinished;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.DownloadStateInstalled;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.DownloadStateInstalling;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.DownloadStatePause;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.DownloadStateWaiting;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.state.IDownloadState;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadBroadcastExtra;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.SizeFormater;

/**
 * 基本下载信息
 * 
 * @author pdw
 * @version
 * @date 2012-9-17 下午03:28:14
 */
public class BaseDownloadInfo implements Serializable, Parcelable {

	public static final int FILE_TYPE_NONE = -1;
	public static final int FILE_TYPE_APK = 0;
	public static final int FILE_TYPE_WALLPAPER = 3;
	public static final int FILE_TYPE_THEME_SERIES = 15;
	/** 天天视频壁纸下载 */
	public static final int FILE_VIDEO_WALLPAPER_SERIES = 18;
	/** 视频壁纸下载，包括预览视频和高清视频*/
	public static final int FILE_VIDEO_PAPER = 19;

	public static final String THEME_LIST_URL = "http://91.com";
	public static final String THEME_LIST_SAVE_NAME = "list.apt";
	
	private static final long serialVersionUID = 1L;
	private static final String KEY_SILENT = "silent";
	private static final String KEY_NOTIFICATION = "notification";
	private static final String KEY_23G = "23g";
	private static final String KEY_PRIORITY = "priority";
	public static final String KEY_PKG_NAME = "pkgName";
	public static final String KEY_PKG_VER_CODE = "pkgVerCode";
	//下载完成后是否在下载管理界面被点击过
	private static final String KEY_CLICKED_AFTER_COMPLETE = "cfc";
	//下载完成时间
	private static final String KEY_COMPLETE_TIME = "ct";
	//下载开始时间
	private static final String KEY_BEGIN_TIME = "bt";
	//需要打点统计
	public static final String KEY_STAT = "stat";
	//分发统计渠道号
	public static final String KEY_DIS_SP = "dis_sp";
	//分发统计标识
	public static final String KEY_DIS_ID = "dis_id";
	
	public static final String SUB_DOWNLOAD_INFO_LIST = "sub_download_info_list";
	public static final String FINISH_INDEX = "finish_index";
	public static final String LIST_SIZE_ARR = "list_size_arr";
	public static final String LIST_TOTAL_SIZE = "list_total_size";
	public static final String LIST_CUR_SIZE = "list_cur_size";

	public static final int PRIORITY_DEFAULT = 0;
	public static final int PRIORITY_HITH_1 = -1;

	/**
	 * 用于客户端唯一标识下载应用的字段，由调用者提供，可采用包名+版本号的形式
	 */
	private String mIdentification;
	
	/**
	 * 文件类型
	 */
	private int mFileType = FILE_TYPE_NONE;

	/**
	 * 下载地址
	 */
	private String mDownloadUrl;

	/**
	 * 标题（用于下载管理界面中显示）
	 */
	private String mTitle;
	
	/**
	 * 下载保存路径
	 */
	private String mSavedDir;
	
	/**
	 * 下载保存的文件名
	 */
	private String mSavedName;
	
	/**
	 * 图标路径<br>
	 * 1、本地图标; /sdcard/pandahome2/download/icon/xxx.png<br>
	 * 2、程序打包资源文件; drawable: + ResId,如 drawable:logo
	 */
	private String mIconPath;
	
	/**
	 * 下载大小,单位Byte
	 */
	public long size;

	/**
	 * 下载进度
	 */
	public int progress;

	/**
	 * 下载线程
	 */
	public AbstractDownloadWorker downloadWorker;

	/**
	 * 文件总大小
	 */
	public String totalSize;

	/**
	 * 包名，不能外部设置
	 */
	private String packageName;

	/**
	 * 版本号，不能外部设置
	 */
	private int versionCode = -1;

	/**
	 * 已下载的大小
	 */
	public String downloadSize;

	/** 下载成功后反馈的地址，如果没提供，则不反馈 */
	public String feedbackUrl;
	
	/**
	 * 附加信息
	 */
	public String mAdditionInfo;
		
	boolean mIsPausingByHand = false;
	
	/**
	 * 下载状态
	 */
	private IDownloadState state;
	
	/**
	 * 子任务列表
	 */
	private ArrayList<BaseDownloadInfo> subBaseDownloadInfoS = new ArrayList<BaseDownloadInfo>();
	private String subBaseDownloadInfoListStr = "";
	public int finishIndex = 0;
	
	private IDownloadState stateDownloading;
	private IDownloadState statePause;
	private IDownloadState stateWaiting;
	private IDownloadState stateFinished;
	private IDownloadState stateInstalled;
	private IDownloadState stateInstalling;
	
	//是否被选中，用于下载管理界面
	public boolean mIsSelected;
	public boolean mNeedRedownload = false;
	private long mBeginTime = -1;
	
	BaseDownloadInfo() {
	}
	/**
	 * 仅用于集合查找构造
	 * 
	 * @param indetification
	 */
	BaseDownloadInfo(String indetification) {
		this(indetification, FILE_TYPE_NONE, null, null, null, null, null);
	}

	/**
	 * @param identification 标识
	 * @param fileType 文件类型
	 * @param downloadUrl 下载地址
	 * @param title 标题
	 * @param savedDir 保存路径
	 * @param savedName 保存文件名
	 * @param iconPath 图标途径
	 */
	public BaseDownloadInfo(String identification, int fileType, String downloadUrl, String title, String savedDir, String savedName, String iconPath) {
		mIdentification = identification;
		mFileType = fileType;
		mDownloadUrl = downloadUrl;
		mTitle = title;
		mSavedDir = savedDir;
		mSavedName = (savedName == null ? "" : savedName);
		mIconPath = iconPath;
		totalSize = "0.0MB";
		if (fileType == FILE_TYPE_THEME_SERIES || fileType == FILE_VIDEO_WALLPAPER_SERIES
				|| fileType == FILE_VIDEO_PAPER) {
			finishIndex = 0;
			putAdditionInfo(FINISH_INDEX, "0");
		}
	}

	public BaseDownloadInfo(BaseDownloadInfo srcInfo, Context context) {		
		this(srcInfo.getIdentification(), 
			  srcInfo.getFileType(), 
			  srcInfo.getDownloadUrl(),
			  srcInfo.getTitle(),
			  srcInfo.getSavedDir(),
			  srcInfo.getSavedName(),
			  srcInfo.getIconPath());
		
		initState(context.getApplicationContext());
		this.downloadSize = srcInfo.downloadSize;
		this.downloadWorker = srcInfo.downloadWorker;
		this.feedbackUrl = srcInfo.feedbackUrl;
		this.progress = srcInfo.progress;
		this.size = srcInfo.size;
		switch (srcInfo.getState()) {
		case DownloadState.STATE_DOWNLOADING:
			this.state = stateDownloading;
			break;
		case DownloadState.STATE_FINISHED:
			this.state = stateFinished;
			break;
		case DownloadState.STATE_INSTALLED:
			this.state = stateInstalled;
			break;
		case DownloadState.STATE_PAUSE:
			this.state = statePause;
			break;
		case DownloadState.STATE_WAITING:
			this.state = stateWaiting;
			break;
		case DownloadState.INSTALL_STATE_INSTALLING:
			this.state = stateInstalling;
			break;
		}
		this.totalSize = srcInfo.totalSize;
		this.mAdditionInfo = srcInfo.mAdditionInfo;
		if (mFileType == FILE_TYPE_THEME_SERIES || mFileType == FILE_VIDEO_WALLPAPER_SERIES
				|| mFileType == FILE_VIDEO_PAPER) {
			finishIndex = Integer.valueOf(getAdditionInfo().get(FINISH_INDEX));
			initSubDownloadInfoList();
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mDownloadUrl);
		dest.writeString(mIdentification);
		dest.writeString(mTitle == null ? "" : mTitle);
		dest.writeString(mSavedName == null ? "" : mSavedName);
		dest.writeString(totalSize == null ? "" : totalSize);
		dest.writeString(mSavedDir == null ? "" : mSavedDir);
		dest.writeString(packageName == null ? "" : packageName);
		dest.writeInt(versionCode);
		dest.writeLong(size);
		dest.writeInt(progress);
		dest.writeString(downloadSize == null ? "" : downloadSize);
		dest.writeString(mIconPath == null ? "" : mIconPath);
		dest.writeString(feedbackUrl == null ? "" : feedbackUrl);
		dest.writeString(mAdditionInfo == null ? "" : mAdditionInfo);
		dest.writeInt(mFileType);
		dest.writeSerializable(state);
		dest.writeString(subBaseDownloadInfoListStr);
	}
	
	
	public String getSubBaseInfoListStr() {
		JSONObject subListJo = new JSONObject();
		JSONArray ja = new JSONArray();
		try {
			for (BaseDownloadInfo info : subBaseDownloadInfoS) {
				JSONObject jo = new JSONObject();
				jo.put("mDownloadUrl", info.mDownloadUrl);
				jo.put("mIdentification", info.mIdentification);
				jo.put("mTitle", info.mTitle == null ? "" : info.mTitle);
				jo.put("mSavedName", info.mSavedName == null ? "" : info.mSavedName);
				jo.put("totalSize", info.totalSize == null ? "" : info.totalSize);
				jo.put("mSavedDir", info.mSavedDir == null ? "" : info.mSavedDir);
				jo.put("packageName", info.packageName == null ? "" : info.packageName);
				jo.put("versionCode", info.versionCode);
				jo.put("size", info.size);
				jo.put("progress", info.progress);
				jo.put("downloadSize", info.downloadSize == null ? "" : info.downloadSize);
				jo.put("mIconPath", info.mIconPath == null ? "" : info.mIconPath);
				jo.put("feedbackUrl", info.feedbackUrl == null ? "" : info.feedbackUrl);
				jo.put("mAdditionInfo", info.mAdditionInfo == null ? "" : info.mAdditionInfo);
				jo.put("mFileType", info.mFileType);
				jo.put("state", info.state);
				ja.put(jo);
			}

			subListJo.put(SUB_DOWNLOAD_INFO_LIST, ja.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return subListJo.optString(SUB_DOWNLOAD_INFO_LIST);
	}

	protected void readFromParcel(Parcel source) {
		mDownloadUrl = source.readString();
		mIdentification = source.readString();
		mTitle = source.readString();
		mSavedName = source.readString();
		totalSize = source.readString();
		mSavedDir = source.readString();
		packageName = source.readString();
		versionCode = source.readInt();
		size = source.readLong();
		progress = source.readInt();
		downloadSize = source.readString();
		mIconPath = source.readString();
		feedbackUrl = source.readString();
		mAdditionInfo = source.readString();
		mFileType = source.readInt();
		state = (IDownloadState) source.readSerializable();
		subBaseDownloadInfoListStr = source.readString();
		if (state == null)
			state = stateWaiting;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BaseDownloadInfo))
			return false;
		final BaseDownloadInfo one = (BaseDownloadInfo) o;
		return mIdentification.equalsIgnoreCase(one.getIdentification());
	}
	
	/**
	 * 获取文件的全路径
	 */
	public String getFilePath() {
		File file = new File(mSavedDir, mSavedName);
		String path = file.getAbsolutePath();
		file = null;
		return path;
	}

	/**
	 * 从包里解析包名
	 * 
	 * @param context
	 * @return
	 */
	public final String getPacakgeName(Context context) {
		if (TextUtils.isEmpty(packageName)) {
			HashMap<String, String> addition = getAdditionInfo();
			if (addition != null && addition.containsKey(KEY_PKG_NAME)) {
				packageName = addition.get(KEY_PKG_NAME);
			}
		}
			
		if (TextUtils.isEmpty(packageName)) {
			try {
				PackageManager pm = context.getPackageManager();
				PackageInfo info = pm.getPackageArchiveInfo(getFilePath(), 0);
				packageName = info.packageName;
			} catch (Exception e) {
			}
		}

		packageName = TextUtils.isEmpty(packageName) ? "" : packageName;

		return packageName;
	}

	/**
	 * 从包里解析版本号
	 * 
	 * @param context
	 * @return
	 */
	public final int getVersionCode(Context context) {
		if (versionCode == -1) {
			HashMap<String, String> addition = getAdditionInfo();
			if (addition != null && addition.containsKey(KEY_PKG_VER_CODE)) {
				try {
					versionCode = Integer.parseInt(addition.get(KEY_PKG_VER_CODE));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (versionCode == -1) {
			try {
				PackageManager pm = context.getPackageManager();
				PackageInfo info = pm.getPackageArchiveInfo(getFilePath(), 0);
				versionCode = info.versionCode;
			} catch (Exception e) {
			}
		}

		return versionCode;
	}

	/**
	 * 获取apk包的唯一标识，包名+版本号 未下载完成的返回空
	 * 
	 * @return
	 */
	public final String getPackageExclusiveKey(Context context) {
		if (TextUtils.isEmpty(packageName))
			getPacakgeName(context);

		if (versionCode == -1)
			getVersionCode(context);

		return packageName + versionCode;

	}// end getPackageExclusiveKey

	/**
	 * 初始化状态
	 */
	public void initState(Context context) {
		if (downloadSize == null || downloadSize.equals("")) {
			downloadSize = "0.0MB" ;
		}
		if (totalSize == null || totalSize.equals("")) {
			totalSize = "0.0MB" ;
		}
		
		stateDownloading = new DownloadStateDownloading(context, this);
		statePause = new DownloadStatePause(context, this);
		stateWaiting = new DownloadStateWaiting(context, this);
		stateInstalled = new DownloadStateInstalled(context, this);
		stateFinished = new DownloadStateFinished(context, this);
		stateInstalling = new DownloadStateInstalling(context, this);
		state = stateWaiting;
	}

	/**
	 * 构照设置完属性后初始化下载状态
	 */
	public void initDownloadState() {

		if (downloadSize == null || totalSize == null || mSavedName == null || mSavedDir == null)
			return;
		if (downloadSize.equalsIgnoreCase(totalSize) && progress == 100) {
			state = stateFinished;
			/*
			 * final Context ctx = Global.getApplicationContext(); if
			 * (ApkTools.hasInstallApk(ctx, downloadDir + apkFile)){ state =
			 * finishedInstalled ; } else { state = finishedUninstalled ; }
			 */

		} else {
			state = statePause;
		}
	}

	/**
	 * @return 正在下载状态
	 */
	public IDownloadState getDownloadingState() {
		return stateDownloading;
	}

	/**
	 * @return 暂停状态
	 */
	public IDownloadState getPauseState() {
		return statePause;
	}

	/**
	 * @return 等待状态
	 */
	public IDownloadState getWaitingState() {
		return stateWaiting;
	}

	/**
	 * @return 下载完成但未安装状态
	 */
	public IDownloadState getFinishedUninstalled() {
		return stateFinished;
	}

	/**
	 * @return 下载完成已安装状态
	 */
	public IDownloadState getFinishedInstalled() {
		return stateInstalled;
	}

	/**
	 * @return 正在安装状态
	 */
	public IDownloadState getInstallingState() {
		return stateInstalling;
	}

	/**
	 * 设置状态
	 * 
	 * @param state
	 */
	public void setState(IDownloadState state) {
		this.state = state;
	}

	/**
	 * 下载项的功能操作，暂停，继续，等待
	 */
//	public boolean action(Context ctx, ViewHolder viewHolder, DownloadServerServiceConnection downloadService) {
//		if (state != null) {
//			return state.action(ctx, viewHolder, downloadService);
//		}
//		
//		return true;
//	}

	/**
	 * 开始下载任务
	 */
	public void start(Context context) {
		AbstractDownloadCallback callback = DownloadServerService.sDownloadCallback;
		
		AbstractDownloadWorker worker = null;
		if (getIsSilent()) {
			worker = is23GEnableTask() ? new Silent23GDownloadWorker(context, this, callback) :  new SilentDownloadWorker(context, this, callback);
		} else {
			worker = new BaseDownloadWorker(context, this, callback);
		}
		worker.start();
	}

	/**
	 * 取消该下载项
	 */
	public void cancel() {
		if (state != null)
			state.cancel();
	}

	/**
	 * 暂停该下载项
	 */
	public void pause() {
		if (state != null)
			state.pause();
	}

	/**
	 * 获取状态
	 */
	public int getState() {
		if (state != null) {
			return state.getState();
		}
		return DownloadState.STATE_NONE;
	}

	/**
	 * 初始化视图
	 */
//	public void initView(ViewHolder viewHolder) {
//		if (state != null)
//			state.initView(viewHolder);
//	}

	/**
	 * 是否已下载完成 根据已下载的大小和进度来判断
	 * 
	 * @return
	 */
	public boolean hasDownloaded() {
		if (downloadSize != null && totalSize != null && downloadSize.equals(totalSize) && progress == 100)
			return true;
		return false;
	}

	/**
	 * 计算已下载部分
	 * 
	 * <p>
	 * date: 2012-9-21 下午03:12:20
	 * 
	 * @return
	 */
	public String resetDownloadSize(Context context) {
		if (downloadWorker == null) {
			return "0.00MB";
		}
		
		final long totalSize = downloadWorker.getTotalSize();
		if (totalSize == 0)
			return "0.00MB";
		progress = downloadWorker.getProgress();
		downloadSize = SizeFormater.getDownloadSize(context, totalSize, progress);
		return downloadSize;
	}

	public String getIdentification() {
		return mIdentification;
	}
	
	public int getFileType() {
		return mFileType;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getDownloadUrl() {
		return mDownloadUrl;
	}
	
	public String getSavedDir() {
		return mSavedDir;
	}
	
	public String getSavedName() {
		return mSavedName;
	}
	
	public String getIconPath() {
		return mIconPath;
	}
	
	public void setSilent() {
		putAdditionInfo(KEY_SILENT, "true");
	}
	
	public boolean getIsSilent() {
		HashMap<String, String> addition = getAdditionInfo();
		return (addition != null && addition.containsKey(KEY_SILENT));
	}
	
	public void setNoNotification() {
		putAdditionInfo(KEY_NOTIFICATION, "false");
	}
	
	public boolean getIsNoNotification() {
		HashMap<String, String> addition = getAdditionInfo();
		return (addition != null && addition.containsKey(KEY_NOTIFICATION));
	}
	
	public void set23GEnable(boolean enable) {
		putAdditionInfo(KEY_23G, (enable ? "true" : "false"));
	}
	
	public boolean is23GEnableTask() {
		HashMap<String, String> addition = getAdditionInfo();
		return (addition != null && addition.containsKey(KEY_23G));
	}
	
	public boolean get23GEnable() {
		HashMap<String, String> addition = getAdditionInfo();
		return (addition != null && addition.containsKey(KEY_23G) && addition.get(KEY_23G).equals("true"));
	}
	
	/**
	 * 设置为需要打点统计
	 */
	public void setNeedStat() {
		putAdditionInfo(KEY_STAT, "true");
	}
	
	public boolean isNeedStat() {
		HashMap<String, String> addition = getAdditionInfo();
		return (addition != null && addition.containsKey(KEY_STAT) && addition.get(KEY_STAT).equals("true"));
	}
	
	/**
	 * 设置分发统计渠道号
	 */
	public void setDisSp(int sp) {
		putAdditionInfo(KEY_DIS_SP, String.valueOf(sp));
	}
	
	public int getDisSp() {
		int sp = -1;
		HashMap<String, String> addition = getAdditionInfo();
		if (addition != null && addition.containsKey(KEY_DIS_SP)) {
			try {
				sp = Integer.parseInt(addition.get(KEY_DIS_SP));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return sp;
	}
	
	/**
	 * 设置分发统计标识
	 */
	public void setDisId(String id) {
		putAdditionInfo(KEY_DIS_ID, id);
	}
	
	public String getDisId() {
		String id = null;
		HashMap<String, String> addition = getAdditionInfo();
		if (addition != null && addition.containsKey(KEY_DIS_ID)) {
			id = addition.get(KEY_DIS_ID);
		}
		
		return id;
	}
	
	/**
	 * 下载完成后是否在下载管理界面被点击过
	 */
	public void setClickedAfterComplete(Context context) {
		putAdditionInfo(KEY_CLICKED_AFTER_COMPLETE, "true");
	}
	
	public boolean isClickedAfterComplete() {
		HashMap<String, String> addition = getAdditionInfo();
		return (addition != null 
				 && addition.containsKey(KEY_CLICKED_AFTER_COMPLETE) 
				 && addition.get(KEY_CLICKED_AFTER_COMPLETE).equals("true"));
	}
	
	/**
	 * 下载完成时间
	 */
	public void setCompleteTime(Context context, long time) {
		putAdditionInfo(KEY_COMPLETE_TIME, String.valueOf(time));
		
		DownloadDBManager.updateAdditionInfo(context, this);
		
		if (DownloadServerService.sBroadcastAction != null) {
			Intent intent = new Intent(DownloadServerService.sBroadcastAction);
			intent.putExtra(DownloadBroadcastExtra.EXTRA_IDENTIFICATION, mIdentification);
			intent.putExtra(DownloadBroadcastExtra.EXTRA_ADDITION, mAdditionInfo);
			context.sendBroadcast(intent);
		}
	}
	
	public long getCompleteTime() {
		long time = -1;
		HashMap<String, String> addition = getAdditionInfo();
		if (addition != null && addition.containsKey(KEY_COMPLETE_TIME)) {
			try {
				time = Long.parseLong(addition.get(KEY_COMPLETE_TIME));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return time;
	}
	
	/**
	 * 下载开始时间
	 */
	public void setBeginTime(Context context, long time) {
		putAdditionInfo(KEY_BEGIN_TIME, String.valueOf(time));
	}
	
	public long getBeginTime() {
		if (mBeginTime != -1) {
			return mBeginTime;
		}
		
		HashMap<String, String> addition = getAdditionInfo();
		if (addition != null && addition.containsKey(KEY_BEGIN_TIME)) {
			try {
				mBeginTime = Long.parseLong(addition.get(KEY_BEGIN_TIME));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			mBeginTime = 0;
		}
		
		return mBeginTime;
	}
	
	public void putAdditionInfo(String key, String value) {
		if (key == null || value == null) {
			return;
		}
		
		HashMap<String, String> addition = getAdditionInfo();
		if (addition == null) {
			addition = new HashMap<String, String>();
		}
		addition.put(key, value);
		setAdditionInfo(addition);
	}
	
	/**
	 * 设置附加信息
	 */
	public void setAdditionInfo(HashMap<String, String> info) {
		if (info == null || info.size() <= 0) {
			return;
		}
		
		JSONObject object = new JSONObject(info);
		mAdditionInfo = object.toString();
	}

	/**
	 * 设置附加信息
	 */
	public void setAdditionInfo(String info) {
		mAdditionInfo = info;
	}
	
	/**
	 * 修改附加信息
	 */
	void modifyAdditionInfo(Context context, HashMap<String, String> info) {
		if (context == null || info == null || info.size() <= 0) {
			return;
		}
		
		HashMap<String, String> oldInfo = getAdditionInfo();
		if (oldInfo == null) {
			oldInfo = new HashMap<String, String>();
		}
		
		Iterator<String> iterator = info.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			oldInfo.put(key, info.get(key));
		}
		setAdditionInfo(oldInfo);
		DownloadDBManager.updateAdditionInfo(context, this);
		
		if (DownloadServerService.sBroadcastAction != null) {
			Intent intent = new Intent(DownloadServerService.sBroadcastAction);
			intent.putExtra(DownloadBroadcastExtra.EXTRA_IDENTIFICATION, mIdentification);
			intent.putExtra(DownloadBroadcastExtra.EXTRA_ADDITION, mAdditionInfo);
			context.sendBroadcast(intent);
		}
	}
	
	/**
	 * 获取附加信息
	 */
	public HashMap<String, String> getAdditionInfo() {
		HashMap<String, String> result = new HashMap<String, String>();
		try {
			if (null != mAdditionInfo && !mAdditionInfo.equals("")) {
			    result = new HashMap<String, String>();
			    JSONObject o = new JSONObject(mAdditionInfo);
			    Iterator i = o.keys();
			    String key;
			    String value;
			    while(i.hasNext()) {
			        key = (String)i.next();
			        value = (String)o.get(key);
			        result.put(key, value);
			    }
			}
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * 根据文件大小（MB）安排优先级，文件越小的优先级越高
	 */
	public void setPrioritySize(int prioritySize) {
		HashMap<String, String> addition = getAdditionInfo();
		if (addition == null) {
			addition = new HashMap<String, String>();
		}
		addition.put(KEY_PRIORITY, String.valueOf(prioritySize));
		setAdditionInfo(addition);
	}
	
	public int getPrioritySize() {
		HashMap<String, String> addition = getAdditionInfo();
		if (addition != null && addition.containsKey(KEY_PRIORITY)) {
			try {
				return Integer.parseInt(addition.get(KEY_PRIORITY));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return PRIORITY_DEFAULT;
	}
	
	private BaseDownloadInfo(Parcel source) {
		readFromParcel(source);
	}

	public static final Parcelable.Creator<BaseDownloadInfo> CREATOR = new Creator<BaseDownloadInfo>() {
		public BaseDownloadInfo createFromParcel(Parcel source) {

			return new BaseDownloadInfo(source);
		}

		public BaseDownloadInfo[] newArray(int size) {
			return new BaseDownloadInfo[size];
		}
	};
	
	public boolean fileExists() {
		String filePath = getFilePath();
		if (filePath != null) {
			File file = new File(filePath);
			return file.exists();
		}
	
		return false;
	}
	
	public void setSubBaseDownloadInfoS(ArrayList<BaseDownloadInfo> subBaseDownloadInfoS) {
		this.subBaseDownloadInfoS = subBaseDownloadInfoS;
		subBaseDownloadInfoListStr = getSubBaseInfoListStr();
		putAdditionInfo(FINISH_INDEX, "0");
		putAdditionInfo(SUB_DOWNLOAD_INFO_LIST, subBaseDownloadInfoListStr);
	}

	public ArrayList<BaseDownloadInfo> getInnerSubBaseDownloadInfoS() {
		return subBaseDownloadInfoS;
	}

	public void initSubDownloadInfoList() {
		if (mFileType != FILE_TYPE_THEME_SERIES && mFileType != FILE_VIDEO_WALLPAPER_SERIES
				&& mFileType != FILE_VIDEO_PAPER) {
			return;
		}

		if (TextUtils.isEmpty(subBaseDownloadInfoListStr)) {
			finishIndex = Integer.valueOf(getAdditionInfo().get(FINISH_INDEX));
			subBaseDownloadInfoListStr = getAdditionInfo().get(SUB_DOWNLOAD_INFO_LIST);
		}

		subBaseDownloadInfoS = new ArrayList<BaseDownloadInfo>();
		try {
			JSONArray ja = new JSONArray(subBaseDownloadInfoListStr);
			for (int i = 0; i < ja.length(); i++) {
				JSONObject infoJo = ja.getJSONObject(i);
				BaseDownloadInfo info = new BaseDownloadInfo();
				info.mDownloadUrl = infoJo.optString("mDownloadUrl");
				info.mIdentification = infoJo.optString("mIdentification");
				info.mTitle = infoJo.optString("mTitle");
				info.mSavedName = infoJo.optString("mSavedName");
				info.mSavedDir = infoJo.optString("mSavedDir");
				info.totalSize = infoJo.optString("totalSize");
				info.packageName = infoJo.optString("packageName");
				info.versionCode = infoJo.optInt("versionCode");
				info.size = infoJo.optLong("size");
				info.progress = infoJo.optInt("progress");
				info.downloadSize = infoJo.optString("downloadSize");
				info.mIconPath = infoJo.optString("mIconPath");
				info.feedbackUrl = infoJo.optString("feedbackUrl");
				info.mAdditionInfo = infoJo.optString("mAdditionInfo");
				info.mFileType = infoJo.optInt("mFileType");
				subBaseDownloadInfoS.add(info);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getTotalSubDownloadListSize() {
		return subBaseDownloadInfoS.size();
	}
	
	public int getFinishIndex() {
		return Integer.valueOf(finishIndex);
	}
	
	public boolean isFileListExists() {
		try {
			for (BaseDownloadInfo subInfo : subBaseDownloadInfoS) {
				String path = subInfo.getFilePath();
				if (!new File(path).exists()) {
					return false;
				}
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public long[] getListSizeArr() {
		long[] listSizeArr = new long[subBaseDownloadInfoS.size()];
		String sizeArrStr = getAdditionInfo().get(LIST_SIZE_ARR);
		try {
			if (!TextUtils.isEmpty(sizeArrStr)) {
				String[] sizeArr = sizeArrStr.split(",");
				for (int i = 0; i < sizeArr.length; i++) {
					listSizeArr[i] = Long.parseLong(sizeArr[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return listSizeArr;
	}
	
	public void setListSizeArr(long[] sizeArr) {
		String sizeArrStr = "";
		int listToatlSize = 0;
		for (long size : sizeArr) {
			listToatlSize += size;
			sizeArrStr += size + ",";
		}

		putAdditionInfo(LIST_SIZE_ARR, sizeArrStr);
		putAdditionInfo(LIST_TOTAL_SIZE, String.valueOf(listToatlSize));
	}
	
	public long getListTotalSize() {
		String listToatlSizeStr = getAdditionInfo().get(LIST_TOTAL_SIZE);
		long listToatlSize = 0;
		try {
			if (!TextUtils.isEmpty(listToatlSizeStr) && TextUtils.isDigitsOnly(listToatlSizeStr)) {
				listToatlSize = Long.parseLong(listToatlSizeStr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return listToatlSize;
	}
	
	public void deleteFileList() {
		if (getFileType() != FILE_TYPE_THEME_SERIES && getFileType() != FILE_VIDEO_WALLPAPER_SERIES
				&& getFileType() != FILE_VIDEO_PAPER) {
			return;
		}

		for (int i = 0; i < getInnerSubBaseDownloadInfoS().size(); i++) {
			try {
				BaseDownloadInfo curDownloadInfo = getInnerSubBaseDownloadInfoS().get(i);
				File file = new File(curDownloadInfo.getSavedDir() + curDownloadInfo.getSavedName());
				if (file.exists()) {
					file.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
