package com.nd.hilauncherdev.webconnect.downloadmanage.util;

/**
 * 下载状态
 * 
 * @author pdw
 * @version
 * @date 2012-9-20 下午01:52:49
 */
public class DownloadState {
	/**
	 * 正在下载
	 */
	public static final int STATE_DOWNLOADING = 0;

	/**
	 * 暂停
	 */
	public static final int STATE_PAUSE = 1;

	/**
	 * 取消
	 */
	public static final int STATE_CANCLE = 2;

	/**
	 * 完成,但未安装
	 */
	public static final int STATE_FINISHED = 3;

	/**
	 * 等待
	 */
	public static final int STATE_WAITING = 4;

	/**
	 * 已安装
	 */
	public static final int STATE_INSTALLED = 5;

	/**
	 * 无,从未进行过下载
	 */
	public static final int STATE_NONE = 6;

	/**
	 * 下载失败
	 */
	public static final int STATE_FAILED = 7;
	
	/**
	 * 下载开始
	 */
	public static final int STATE_START = 8;
	
	// TODO linqiang form ApkInstaller.INSTALL_STATE_INSTALLING
	/**
	 * 应用安装的广播action
	 */
	public final static String RECEIVER_APP_SILENT_INSTALL = "receiver_app_silent_install";
	/**
	 * Intent传递安装状态
	 */
	public final static String EXTRA_APP_INSTALL_STATE = "extra_app_install_state";

	/**
	 * Intent传递安装应用的包名Key
	 */
	public final static String EXTRA_APP_INSTALL_PACAKGE_NAME = "extra_app_install_pacakge_name";

	/**
	 * Intent传递安装应用的包路径Key
	 */
	public final static String EXTRA_APP_INSTALL_APK_PATH = "extra_app_install_apk_path";

	/** 安装状态--正在安装 */
	public final static int INSTALL_STATE_INSTALLING = 10000;
	/** 安装状态--安装成功 */
	public final static int INSTALL_STATE_INSTALL_SUCCESS = 20000;
	/** 安装状态--安装失败 */
	public final static int INSTALL_STATE_INSTALL_FAILED = 30000;

}
