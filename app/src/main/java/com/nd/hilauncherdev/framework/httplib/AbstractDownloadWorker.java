package com.nd.hilauncherdev.framework.httplib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.nd.hilauncherdev.core.DownloadFileUtil;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.AbstractDownloadCallback;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadInfo;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadWorkerSupervisor;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadDBManager;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.DownloadServerService;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.Silent23GDownloadWorkerSupervisor;
import com.nd.hilauncherdev.webconnect.downloadmanage.model.SilentDownloadWorkerSupervisor;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadBroadcastExtra;
import com.nd.hilauncherdev.webconnect.downloadmanage.util.DownloadState;

/**
 * 通用下载抽象类，可支持断点续传，自动连接
 * 下载成功后的文件路径可通过回调函数onDownloadCompleted(String,String)获得
 */
public abstract class AbstractDownloadWorker extends Thread {

	protected BaseDownloadInfo downloadInfo;

	/**
	 * 下载url
	 */
	public String url;
	/**
	 * 下载文件的保存路径
	 */
	public String savePath;
	/**
	 * 如果有通知显示，用于通知中心的title显示
	 */
	public String tipName;

	protected Context mAppContext;

	/**
	 * 指定的下载文件名
	 */
	protected String specifyFileName;

	/**
	 * 下载完成后的文件全路径，eg. /sdcard/pandahome2/XXXX.apt
	 */
	private String saveFile;

	/**
	 * 下载百分比
	 */
	private int progress;

	private long totalSize;
	private long listTotalSize;
	private long listCurSize;

	protected AbstractDownloadCallback mDownloadCallback;


	/**
	 * 下载临时文件后缀
	 */
//	public static final String POSTFIX_FILE_NAME = ".temp";

	/**
	 * @see {@link HttpConstants#HTTP_REQUEST_PAUSE},
	 *      {@link HttpConstants#HTTP_REQUEST_CANCLE}
	 */
	private int requestType = HttpConstants.HTTP_REQUEST_NONE;


	private static final int MAX_REQUEST_RETRY_COUNTS = 3;
	private static final int RETRY_SLEEP_TIME = 2000;
	private static final int BUFFER_SIZE = 2048; // 缓冲区大小
	private static final String CHARSET_UTF_8 = org.apache.http.protocol.HTTP.UTF_8;

	/**
	 * 构造下载线程 由此构造的下载线程将统一由{@link CommonDownloadWorkerSupervisor}管理
	 *
	 * @param url
	 * @param savePath
	 *            以"/"结束，如"/sdcard/PandaHome3/Theme/"
	 * @param specifyFileName
	 *            指定下载文件名,不包含路径，不能为空，如：com.nd.ui.apk
	 * @param tipName
	 *            提示名称
	 */
	public AbstractDownloadWorker(Context context, String url, String savePath, String specifyFileName, String tipName, AbstractDownloadCallback callback) {
		this.url = utf8URLencode(url);
		this.savePath = savePath;
		this.tipName = tipName;
		this.specifyFileName = specifyFileName;
		if (this.tipName == null)
			this.tipName = "";
		mAppContext = context.getApplicationContext();
		maybeInitDir(savePath);

		mDownloadCallback = callback;
	}

	/**
	 * URL进行utf8编码
	 * @param url
	 * @return String
	 */
	private static String utf8URLencode(String url) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < url.length(); i++) {
			char c = url.charAt(i);
			if ((c >= 0) && (c <= 255)) {
				result.append(c);
			} else {
				byte[] b = new byte[0];
				try {
					b = Character.toString(c).getBytes(CHARSET_UTF_8);
				} catch (Exception ex) {
				}
				for (int j = 0; j < b.length; j++) {
					int k = b[j];
					if (k < 0)
						k += 256;
					result.append("%" + Integer.toHexString(k).toUpperCase());
				}
			}
		}
		return result.toString();
	}

	protected void maybeInitDir(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
//		if (specifyFileName != null && !specifyFileName.endsWith(".apk")) {
//			specifyFileName += ".apk";
//		}
		saveFile = new File(this.savePath, specifyFileName).getAbsolutePath();
	}

	@Override
	public void run() {
//		NetworkAccess access = NetworkAccess.getInstance();
//		Result result = access.execute(new Runnable() {
//			@Override
//			public void run() {
//				download();
//			}
//		});
//		if (result.code != Result.Code.SUCCEED) {
//			onDownloadFailedWrap(getIdentification(), this.url);
//		}
		if (downloadInfo.getFileType() != BaseDownloadInfo.FILE_TYPE_THEME_SERIES
				&& downloadInfo.getFileType() != BaseDownloadInfo.FILE_VIDEO_WALLPAPER_SERIES
				&& downloadInfo.getFileType() != BaseDownloadInfo.FILE_VIDEO_PAPER) {
			download();
		} else {
			downloadList();
		}
	}

	private void downloadList() {
		final String identification = getIdentification();
		byte[] buf = new byte[BUFFER_SIZE];
		int size = 0; // read byte size per io
		totalSize = 0; // total size of file
		File file = null;
		String tempFile;
		RandomAccessFile accessFile = null;
		InputStream in = null;
		DefaultHttpClient httpClient = null;
		HttpEntity httpEntity = null;
		// get list size arr
		long[] listSizeArr = new long[downloadInfo.getInnerSubBaseDownloadInfoS().size()];
		listCurSize = 0;
		initListSizeArr(listSizeArr);
		downloadInfo.totalSize = DownloadFileUtil.getMemorySizeString(listTotalSize);
		DownloadDBManager.updateLog(mAppContext, downloadInfo);
		boolean isBeginDownloadCalled = false;
		for (int i = downloadInfo.finishIndex; i < downloadInfo.getInnerSubBaseDownloadInfoS().size(); i++) {
			BaseDownloadInfo curDownloadInfo = downloadInfo.getInnerSubBaseDownloadInfoS().get(i);
			this.savePath = curDownloadInfo.getSavedDir();
			this.specifyFileName = curDownloadInfo.getSavedName();
			this.url = curDownloadInfo.getDownloadUrl();
			int retryCount = 0;
			boolean shouldConn = true;
			long currentSize; // current size of file
			maybeInitDir(curDownloadInfo.getSavedDir());
			if (saveFile != null) {
				// 下载过程先生成临时文件
				tempFile = saveFile + getTempSuffix(downloadInfo.getIsSilent());
			} else {
				onDownloadFailedWrap(identification, this.url, new FileNotFoundException());
				return;
			}

			try {
				file = new File(saveFile);
				if (file.exists()) {
					listCurSize += listSizeArr[i];
					downloadInfo.finishIndex = i + 1;
					downloadInfo.putAdditionInfo(BaseDownloadInfo.FINISH_INDEX, String.valueOf(i + 1));
					DownloadDBManager.updateAdditionInfo(mAppContext, downloadInfo);
					onOneDownloadCompletedWrap(true);
					continue;
				}

				file = new File(tempFile);
				if (!file.exists()) {
					if (!(file.getParentFile().exists())) {
						file.getParentFile().mkdirs();
					}

					file.createNewFile();
				}

				accessFile = new RandomAccessFile(tempFile, "rw");
				currentSize = file.length();
				int progress = DownloadDBManager.getDownloadProgress(mAppContext, downloadInfo);
				if (!isBeginDownloadCalled) {
					onBeginDownloadWrap(identification, this.url, listCurSize + currentSize, progress);
					isBeginDownloadCalled = true;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				onDownloadFailedWrap(identification, this.url, ex);
				try {
					if (accessFile != null) {
						accessFile.close();
					}
				} catch (Exception x) {
					x.printStackTrace();
				}
				return;
			}

			while (shouldConn && retryCount < MAX_REQUEST_RETRY_COUNTS) {
				try {
					httpClient = getHttpClient();
					String getUrl = this.url;
					getUrl = trySetRetry(getUrl, retryCount, downloadInfo);
					HttpGet httpGet = new HttpGet(getUrl);
					httpGet.setHeader(
							"Accept",
							"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
					httpGet.setHeader("Charset", "UTF-8");
					httpGet.setHeader("Connection", "Keep-Alive");
					String sProperty = "bytes=" + currentSize + "-";
					httpGet.setHeader("Range", sProperty);
                    String userAgent = getUserAgent();
                    if (!TextUtils.isEmpty(userAgent)) {
                        httpGet.setHeader("User-Agent", userAgent);
                    }
					HttpResponse response = httpClient.execute(httpGet);
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_PARTIAL_CONTENT) {
						retryCount++;
						if (retryCount >= MAX_REQUEST_RETRY_COUNTS) {
							onDownloadFailedWrap(identification, this.url, new Exception("retryCount >= MAX_REQUEST_RETRY_COUNTS"));
							return;
						}

						continue;
					}

					HttpEntity entity = response.getEntity();
					if (entity.getContentLength() == -1) {
						onDownloadFailedWrap(identification, this.url, new Exception("entity.getContentLength() == -1"));
						return;
					}

					onDownloadWorkingWrap(identification, this.url, listTotalSize, listCurSize + currentSize, (int) ((listCurSize + currentSize) * 100 / listTotalSize));
					accessFile.seek(currentSize);
					in = entity.getContent();
					// begin to download
					int circle = 0;
					while ((size = in.read(buf)) != -1) {
						if (isInterrupted()) {
							if (requestType == HttpConstants.HTTP_REQUEST_PAUSE) {
								// do nothing
							} else if (requestType == HttpConstants.HTTP_REQUEST_CANCLE) {
								downloadInfo.deleteFileList();
							}

							onHttpRequestWrap(identification, this.url, requestType, listTotalSize, listCurSize + currentSize);
							return;
						}

						accessFile.write(buf, 0, size);
						currentSize += size;
						if (circle % 50 == 0) {
							progress = (int) ((listCurSize + currentSize) * 100 / listTotalSize);
							onDownloadWorkingWrap(identification, this.url, listTotalSize, listCurSize + currentSize, progress);
						}
						circle++;
					}

					renameFile(file, saveFile);
					downloadInfo.finishIndex = i + 1;
					downloadInfo.putAdditionInfo(BaseDownloadInfo.FINISH_INDEX, String.valueOf(i + 1));
					DownloadDBManager.updateAdditionInfo(mAppContext, downloadInfo);
					onOneDownloadCompletedWrap(true);
					listCurSize += listSizeArr[i];
					shouldConn = false;
				} catch (Exception ex) {
					ex.printStackTrace();
					shouldConn = true;
					retryCount++;
					try {
						Thread.sleep(RETRY_SLEEP_TIME); // sleep 2 seconds
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (retryCount == MAX_REQUEST_RETRY_COUNTS) {
						onDownloadFailedWrap(identification, this.url, ex);
						return;
					}
				} finally {
					try {
						if (in != null) {
							in.close();
						}

						if (httpEntity != null) {
							httpEntity.consumeContent();
						}

						if (httpClient != null)
							httpClient.getConnectionManager().shutdown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		onDownloadCompletedWrap(identification, this.url, saveFile, listTotalSize, false);
	}

	private void initListSizeArr(long[] listSizeArr) {
		listTotalSize = downloadInfo.getListTotalSize();
		if (listTotalSize <= 0 || listSizeArr.length == 0 || listSizeArr[0] == 0) {
			listTotalSize = 0;
			for (int i = 0; i < downloadInfo.getInnerSubBaseDownloadInfoS().size(); i++) {
				try {
					DefaultHttpClient httpClient = getHttpClient();
					BaseDownloadInfo curDownloadInfo = downloadInfo.getInnerSubBaseDownloadInfoS().get(i);
					HttpGet httpGet = new HttpGet(curDownloadInfo.getDownloadUrl());
					httpGet.setHeader(
							"Accept",
							"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
					httpGet.setHeader("Charset", "UTF-8");
					httpGet.setHeader("Connection", "Keep-Alive");
					String sProperty = "bytes=0-1";
					httpGet.setHeader("Range", sProperty);

                    String userAgent = getUserAgent();
                    if (!TextUtils.isEmpty(userAgent)) {
                        httpGet.setHeader("User-Agent", userAgent);
                    }

					HttpResponse response = httpClient.execute(httpGet);
					String range = response.getHeaders("Content-Range")[0].getValue();
					int index = range.lastIndexOf('/');
					String length = range.substring(index + 1);
					listSizeArr[i] = Long.parseLong(length);
					listTotalSize += listSizeArr[i];
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			downloadInfo.setListSizeArr(listSizeArr);
			DownloadDBManager.updateAdditionInfo(mAppContext, downloadInfo);
		} else {
			listSizeArr = downloadInfo.getListSizeArr();
		}

		for (int i = 0; i < downloadInfo.getInnerSubBaseDownloadInfoS().size(); i++) {
			BaseDownloadInfo curDownloadInfo = downloadInfo.getInnerSubBaseDownloadInfoS().get(i);
			if (!new File(curDownloadInfo.getSavedDir() + curDownloadInfo.getSavedName()).exists()) {
				downloadInfo.finishIndex = i;
				break;
			}

			if (i == downloadInfo.getInnerSubBaseDownloadInfoS().size() - 1 && new File(curDownloadInfo.getSavedDir() + curDownloadInfo.getSavedName()).exists()) {
				downloadInfo.finishIndex = downloadInfo.getInnerSubBaseDownloadInfoS().size();
				break;
			}
		}

		for (int i = 0; i < downloadInfo.finishIndex; i++) {
			listCurSize += listSizeArr[i];
		}

		totalSize = listTotalSize;
	}


	private String getUserAgent() {
		HashMap<String, String> additionalInfo = downloadInfo.getAdditionInfo();
		if (additionalInfo != null) {
			String userAgent = additionalInfo.get("user_agent");
			return userAgent;
		}
		return null;
	}

	public static String getTempSuffix(boolean isSilent) {
		return (isSilent ? ".stemp" : ".temp");
	}

	private void download() {

		final String identification = getIdentification();
		int retryCount = 0;
		boolean shouldConn = true;
		byte[] buf = new byte[BUFFER_SIZE];
		int size = 0; // read byte size per io
		long currentSize; // current size of file
		totalSize = 0; // total size of file
		File file = null;
		String tempFile;

		RandomAccessFile accessFile = null;

		InputStream in = null;
//		HttpURLConnection httpConn = null;
		DefaultHttpClient httpClient = null;
		HttpEntity httpEntity = null;

		if (saveFile != null) {
			// 下载过程先生成临时文件
			tempFile = saveFile + getTempSuffix(downloadInfo.getIsSilent());
		} else {
			onDownloadFailedWrap(identification, this.url, new FileNotFoundException());
			return;
		}

		try {
			file = new File(saveFile);
			if (file.exists()) {
				onDownloadCompletedWrap(identification, this.url, saveFile, file.length(), true);
				return;
			}
			file = new File(tempFile);
			if (!file.exists()) {
				if (!(file.getParentFile().exists())) {
					file.getParentFile().mkdirs();
				}
				Log.i("llbeing","createNews:"+file.getAbsolutePath());
				file.createNewFile();
			}
			accessFile = new RandomAccessFile(tempFile, "rw");
			currentSize = file.length();
			int progress = DownloadDBManager.getDownloadProgress(mAppContext, downloadInfo);
			onBeginDownloadWrap(identification, this.url, currentSize, progress);
		} catch (Exception ex) {
			ex.printStackTrace();
			onDownloadFailedWrap(identification, this.url, ex);
			try {
				if (accessFile != null) {
					accessFile.close();
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
			return;
		}

		/**
		 * 保存的文件名通过构造器传入，不需要处理302跳转 String url =
		 * HttpCommon.getRedirectionURL(this.url);
		 *
		 * if (url == null) { Log.e(Global.TAG, "url is illegal ->"+this.url);
		 * onDownloadFailedWrap(identification,this.url); return; }
		 */
		while (shouldConn && retryCount < MAX_REQUEST_RETRY_COUNTS) {
			try {
//				httpConn = getConnection(this.url);

				httpClient = getHttpClient();
				String getUrl = this.url;
				getUrl = trySetRetry(getUrl, retryCount, downloadInfo);
				HttpGet httpGet = new HttpGet(getUrl);
				httpGet.setHeader(
						"Accept",
						"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
				httpGet.setHeader("Charset", "UTF-8");
				// conn.setRequestProperty("User-Agent",
				// "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
				httpGet.setHeader("Connection", "Keep-Alive");
				String userAgent = getUserAgent();
                if (!TextUtils.isEmpty(userAgent)) {
                    httpGet.setHeader("User-Agent", userAgent);
                }
                String sProperty = "bytes=" + currentSize + "-";
				// set break point
//				httpConn.setRequestProperty("Range", sProperty);
//				httpConn.connect();
//				totalSize = httpConn.getContentLength();

				httpGet.setHeader("Range", sProperty);
				HttpResponse response = httpClient.execute(httpGet);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_PARTIAL_CONTENT) {
					retryCount++;
					if (retryCount >= MAX_REQUEST_RETRY_COUNTS) {
						onDownloadFailedWrap(identification, this.url, new Exception("retryCount >= MAX_REQUEST_RETRY_COUNTS"));
						return;
					}
					continue;
				}

				HttpEntity entity = response.getEntity();
				totalSize = entity.getContentLength();

				if (totalSize == -1) {
					onDownloadFailedWrap(identification, this.url, new Exception("totalSize == -1"));
					return;
				}

				if (totalSize == currentSize) { // there is a trap here
					onDownloadCompletedWrap(identification, this.url, saveFile, totalSize, false);
					break;
				} else {
					totalSize += currentSize;
				}

				//更新真实的文件总大小
				if(downloadInfo!=null)
				{
					downloadInfo.totalSize= DownloadFileUtil.getMemorySizeString(totalSize);
					DownloadDBManager.updateLog(mAppContext, downloadInfo);
				}

				// onBeginDownloadWrap(identification,this.url, currentSize,
				// (int) (currentSize * 100 / totalSize));
				onDownloadWorkingWrap(identification, this.url, totalSize, currentSize, (int) (currentSize * 100 / totalSize));
				accessFile.seek(currentSize);
//				in = httpConn.getInputStream();
				in = entity.getContent();
				// begin to download
				int circle = 0;
				while ((size = in.read(buf)) != -1) {
					if (isInterrupted()) {
						if (requestType == HttpConstants.HTTP_REQUEST_PAUSE) {
							// do nothing
						} else if (requestType == HttpConstants.HTTP_REQUEST_CANCLE) {
							if (file.exists()) {
								file.delete();
							}
						}
						onHttpRequestWrap(identification, this.url, requestType, totalSize, currentSize);
						return;
					}
					accessFile.write(buf, 0, size);
					currentSize += size;
					if (circle % 50 == 0) {
						progress = (int) (currentSize * 100 / totalSize);
						onDownloadWorkingWrap(identification, this.url, totalSize, currentSize, progress);
					}
					circle++;
				}
				renameFile(file, saveFile);
				onDownloadCompletedWrap(identification, this.url, saveFile, totalSize, false);
				shouldConn = false;
			} catch (Exception ex) {
				ex.printStackTrace();
				shouldConn = true;
				retryCount++;
				try {
					Thread.sleep(RETRY_SLEEP_TIME); // sleep 2
																// seconds
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (retryCount == MAX_REQUEST_RETRY_COUNTS) {
					onDownloadFailedWrap(identification, this.url, ex);
				}
			} finally {
				try {
					if (in != null)
						in.close();

//					if (httpConn != null)
//						httpConn.disconnect();

					if (httpEntity != null) {
						httpEntity.consumeContent();
					}

					if (httpClient != null)
						httpClient.getConnectionManager().shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String trySetRetry(String url, int retryCount, BaseDownloadInfo info) {
		int sp = downloadInfo.getDisSp();
		if (retryCount > 0 && sp != -1) {
			boolean needAppend = true;
			if (url.indexOf("pandahome.ifjing.com") < 0) {
				needAppend = false;
			}

			if (needAppend) {
				if (url.indexOf("?") > 0) {
					url += "&softretrydownload=1";
				} else {
					url += "?softretrydownload=1";
				}
			}
		}

		return url;
	}

	private void renameFile(File from, String to) {
		File toFile = new File(to);
		if (!toFile.exists()) {
			from.renameTo(toFile);
		}
		toFile = null;
	}

	/**
	 * 取消或者暂停下载，需实现者自己管理下载线程的托管，需调用
	 * {@link CommonDownloadWorkerSupervisor#remove(String)}移除当前线程的托管
	 * @param identification 下载唯一标识
	 * @param url
	 * @param requestType
	 *            {@link HttpConstants#HTTP_REQUEST_PAUSE} or
	 *            {@link HttpConstants#HTTP_REQUEST_CANCLE}
	 */
	protected abstract void onHttpReqeust(String identification, String url, int requestType, long totalSize, long downloadSize);

	/**
	 * 通知下载进度
	 * @param identification 下载唯一标识
	 * @param url
	 * @param totalSize 总字节数
	 * @param downloadSize 已下载字节数
	 * @param progress 百分比数字，如 80,90,100
	 */
	protected abstract void onDownloadWorking(String identification, String url, long totalSize, long downloadSize, int progress);

	/**
	 * 下载完成
	 * 需实现者自己管理下载线程的托管，需调用{@link CommonDownloadWorkerSupervisor#remove(String)}
	 * 移除当前线程的托管
	 * @param identification 下载唯一标识
	 * @param url
	 * @param file 下载成功后的文件路径，绝对路径
	 * @param totalSize 文件大小
	 */
	protected abstract void onDownloadCompleted(String identification, String url, String file, long totalSize);

	/**
	 * 开始下载
	 * @param identification 下载唯一标识
	 * @param url
	 * @param downloadSize 已下载大小
	 * @param progress 进度
	 */
	protected abstract void onBeginDownload(String identification, String url, long downloadSize, int progress);

	/**
	 * 下载失败
	 * 需实现者自己管理下载线程的托管，需调用{@link CommonDownloadWorkerSupervisor#remove(String)}
	 * 移除当前线程的托管
	 * @param identification 下载唯一标识
	 * @param url
	 */
	protected abstract void onDownloadFailed(String identification, String url);

	/**
	 * 删除临时文件，慎用，导致不能断点下载 ,或者下载异常
	 * 启动线程之前调用
	 */
	public void deleteTempFile() {
		File file = new File(saveFile);
		if (file.exists()) {
			file.delete();
		}
		if (!CommonDownloadWorkerSupervisor.isDownloading(url)) {
			try {
				file = new File(saveFile + getTempSuffix(downloadInfo.getIsSilent()));
				if (file.exists())
					file.delete();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 获取文件名
	 * @param url
	 * @return 文件名
	 */
	public static String getFileName(String url) {
		int lastIndex = url.lastIndexOf('/');
		try {
			return url.substring(lastIndex + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static final int CONNECTION_TIME_OUT = 15000;

	private static final int SOCKET_TIME_OUT = 30000;

	/**
	 * 获取网络连接
	 * @param urlStr
	 * @return HttpURLConnection
	 * @throws Exception
	 */
	private DefaultHttpClient getHttpClient() throws Exception {
		BasicHttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIME_OUT);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIME_OUT);
		DefaultHttpClient client = new DefaultHttpClient(params);
		return client;
	}
//	private HttpURLConnection getConnection(String urlStr) throws Exception {
//		URL url = null;
//		HttpURLConnection conn = null;
//		url = new URL(HttpCommon.utf8URLencode(urlStr));
//		conn = (HttpURLConnection) url.openConnection();
//		conn.setRequestMethod("GET");
//		conn.setDoInput(true);
//		conn.setConnectTimeout(15000);// break if it can't fetch connection
//										// after 15 seconds
//		conn.setReadTimeout(30000);// break connection if reading no data
//									// after 30 seconds
//		conn.setRequestProperty(
//				"Accept",
//				"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
//		conn.setRequestProperty("Charset", "UTF-8");
//		// conn.setRequestProperty("User-Agent",
//		// "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
//		conn.setRequestProperty("Connection", "Keep-Alive");
//		return conn;
//	}

	/**
	 * 获取网络连接
	 *
	 * @param urlString
	 * @return
	 * @throws Exception
	 */
	private HttpURLConnection getConnection(String urlStr) throws Exception {
		URL url = null;
		HttpURLConnection conn = null;
		url = new URL(utf8URLencode(urlStr));
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.setConnectTimeout(15000);// break if it can't fetch connection
										// after 15 seconds
		conn.setReadTimeout(30000);// break connection if reading no data
									// after 30 seconds
		conn.setRequestProperty(
				"Accept",
				"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
		conn.setRequestProperty("Charset", "UTF-8");
		// conn.setRequestProperty("User-Agent",
		// "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
		conn.setRequestProperty("Connection", "Keep-Alive");

        String userAgent = getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) {
			conn.setRequestProperty("User-Agent", userAgent);
        }
		return conn;
	}

	/**
	 * 暂停下载线程，注意：暂停后会销毁掉线程，继续下载需重新构造下载对象。
	 * 调用完后不要再调用{@link #cancle()}否则会引起异常或者不可预知的错误
	 * 暂停下载会保留已下载的文件缓存
	 */
	public final void pause() {
		requestType = HttpConstants.HTTP_REQUEST_PAUSE;
		interrupt();
	}

	/**
	 * 取消下载线程，注意：取消后会销毁掉线程，继续下载需重新构造下载对象。
	 * 调用完后不要再调用{@link #pause()}否则会引起异常或者不可预知的错误
	 * 取消下载会删除已下载的文件缓存
	 */
	public void cancle() {
		requestType = HttpConstants.HTTP_REQUEST_CANCLE;
		interrupt();
	}

	/**
	 * 获取下载的进度，百分比
	 * @return 百分比
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * 设置指定下载文件名
	 * @param specifyFileName 下载文件名
	 */
	public void setSpecifyFileName(String specifyFileName) {
		this.specifyFileName = specifyFileName;
	}

	@Override
	public synchronized void start() {
		if (downloadInfo == null && CommonDownloadWorkerSupervisor.add(url, this)) {
			super.start();
		} else if (downloadInfo != null) {
			if (!DownloadDBManager.hasLogDownloadRecord(mAppContext, downloadInfo)) {
				DownloadDBManager.insertLog(mAppContext, downloadInfo);
			}

			if (isFileExist(downloadInfo)) {
				onDownloadCompletedWrap(downloadInfo.getIdentification(), this.url, saveFile, totalSize, true);
				return;
			} else {
				if (downloadInfo.getState() == DownloadState.STATE_FINISHED
					|| downloadInfo.getState() == DownloadState.STATE_INSTALLED) {
					downloadInfo.progress = 0;
					downloadInfo.downloadSize = "0.0MB";
					DownloadDBManager.updateProgress(mAppContext, downloadInfo);
				}
			}

			if (downloadInfo.getIsSilent()) {
				if (downloadInfo.is23GEnableTask()) {
					startSilent23G();
				} else {
					startSilent();
				}
			} else {
				startNormal();
			}
		}
	}

	private boolean isFileExist(BaseDownloadInfo info) {
		final File file = new File(info.getSavedDir(), info.getSavedName());
		if (file.exists()) {
			return true;
		}

		return false;
	}

	/**
	 * 正常任务
	 */
	private void startNormal() {
		if (BaseDownloadWorkerSupervisor.isInQueue(downloadInfo.getIdentification()))
			return;

		downloadInfo.setState(downloadInfo.getWaitingState());
		sendStateBroadcast(mAppContext, downloadInfo, DownloadState.STATE_WAITING);

//		if (BaseDownloadWorkerSupervisor.shouldRunImmediately(downloadInfo)) {
		if (BaseDownloadWorkerSupervisor.shouldRunImmediately(downloadInfo)) {
			if (BaseDownloadWorkerSupervisor.addRunningQueue(downloadInfo)) {
				downloadInfo.setState(downloadInfo.getDownloadingState());
				super.start();
			}
		} else { // 进入等待队列
			downloadInfo.setState(downloadInfo.getWaitingState());
			BaseDownloadWorkerSupervisor.addWaitingPool(downloadInfo);
		}
	}

	/**
	 * 仅支持wifi的后台任务
	 */
	private void startSilent() {
		if (SilentDownloadWorkerSupervisor.start(mAppContext, downloadInfo)) {
			super.start();
		}
	}

	/**
	 * 可支持wifi与23g的下载任务
	 */
	private void startSilent23G() {
		if (Silent23GDownloadWorkerSupervisor.startImmediately(mAppContext, downloadInfo)) {
			super.start();
		} else { // 进入等待队列
			downloadInfo.setState(downloadInfo.getWaitingState());
			Silent23GDownloadWorkerSupervisor.addWaitingPool(downloadInfo);
		}
	}

	public static void sendStateBroadcast(Context context, BaseDownloadInfo info, int state) {
		if (context == null || info == null || DownloadServerService.sBroadcastAction == null)
			return;

		Intent intent = new Intent(DownloadServerService.sBroadcastAction);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_IDENTIFICATION, info.getIdentification());
		intent.putExtra(DownloadBroadcastExtra.EXTRA_DOWNLOAD_URL, info.getDownloadUrl());
		intent.putExtra(DownloadBroadcastExtra.EXTRA_PROGRESS, info.progress);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_DOWNLOAD_SIZE, info.downloadSize);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_TOTAL_SIZE, info.totalSize);
		intent.putExtra(DownloadBroadcastExtra.EXTRA_STATE, state);
		context.sendBroadcast(intent);
	}

	public long getTotalSize() {
		return totalSize;
	}

	/**
	 * 获取下载唯一值
	 * @return String
	 */
	private String getIdentification() {
		if (downloadInfo == null) {
			return this.url;
		} else {
			return downloadInfo.getIdentification();
		}
	}

	/**
	 * <p>
	 * 开始下载
	 * </p>
	 *
	 * @author zhuchenghua
	 * @param identification
	 *            下载唯一标识
	 * @param url
	 * @param downloadSize
	 *            已下载大小
	 * @param progress
	 *            进度
	 */
	private void onBeginDownloadWrap(String identification, String url, long downloadSize, int progress) {
		if (downloadInfo != null) {
			downloadInfo.setState(downloadInfo.getDownloadingState());
			downloadInfo.progress = progress;

//			int sp = downloadInfo.getDisSp();
//			if (sp >= 0) {
//				AppDistributeUtil.logAppDisDownloadStart(ctx, downloadInfo.getDisId(), sp);
//			}
		}

		onBeginDownload(identification, url, downloadSize, progress);

		if (mDownloadCallback != null) {
			try {
				mDownloadCallback.onBeginDownload(downloadInfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 下载完成
	 * </p>需实现者自己管理下载线程的托管，需调用{@link CommonDownloadWorkerSupervisor#remove(String)}
	 * 移除当前线程的托管</p>
	 *
	 * @author zhuchenghua
	 * @param identification 下载唯一标识
	 * @param url
	 * @param file
	 *            下载成功后的文件路径，绝对路径
	 * @param totalSize
	 *            文件大小
	 */
	private void onDownloadCompletedWrap(String identification, String url, String file, long totalSize, boolean fileExist) {
		if (downloadInfo != null) {
			downloadInfo.setState(downloadInfo.getFinishedUninstalled());
		}

		onDownloadCompleted(identification, url, file, totalSize);

		if (mDownloadCallback != null) {
			try {
				mDownloadCallback.onDownloadCompleted(downloadInfo, fileExist);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void onOneDownloadCompletedWrap(boolean fileExist) {
		try {
			sleep(20);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mDownloadCallback != null) {
			try {
				if (downloadInfo.getFinishIndex() != downloadInfo.getInnerSubBaseDownloadInfoS().size()) {
					mDownloadCallback.onDownloadCompleted(downloadInfo, fileExist);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}



	/**
	 * <p>
	 * 通知下载进度
	 * </p>
	 *
	 * @author zhuchenghua
	 * @param identification
	 *            下载唯一标识
	 * @param url
	 * @param totalSize
	 *            总字节数
	 * @param downloadSize
	 *            已下载字节数
	 * @param progress
	 *            百分比数字，如 80,90,100
	 */
	private void onDownloadWorkingWrap(String identification, String url, long totalSize, long downloadSize, int progress) {
		if (downloadInfo != null) {
			downloadInfo.setState(downloadInfo.getDownloadingState());
			downloadInfo.progress = progress;
		}

		onDownloadWorking(identification, url, totalSize, downloadSize, progress);

		if (mDownloadCallback != null) {
			try {
				mDownloadCallback.onDownloadWorking(downloadInfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * <p>
	 * 下载失败
	 * </p>
	 * </p>需实现者自己管理下载线程的托管，需调用{@link CommonDownloadWorkerSupervisor#remove(String)}
	 * 移除当前线程的托管</p>
	 *
	 * @author zhuchenghua
	 * @param identification
	 *            下载唯一标识
	 * @param url
	 */
	private void onDownloadFailedWrap(String identification, String url, Exception exp) {
		if (downloadInfo != null) {
			downloadInfo.setState(downloadInfo.getPauseState());
		}

		onDownloadFailed(identification, url);

		if (mDownloadCallback != null) {
			try {
				mDownloadCallback.onDownloadFailed(downloadInfo, exp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * </p>取消或者暂停下载</p> </p>需实现者自己管理下载线程的托管，需调用
	 * {@link CommonDownloadWorkerSupervisor#remove(String)}移除当前线程的托管</p>
	 *
	 * @author zhuchenghua
	 * @param identification
	 *            下载唯一标识
	 * @param url
	 * @param requestType
	 *            {@link HttpConstants#HTTP_REQUEST_PAUSE} or
	 *            {@link HttpConstants#HTTP_REQUEST_CANCLE}
	 */
	private void onHttpRequestWrap(String identification, String url, int requestType, long totalSize, long downloadSize) {

		if (requestType == HttpConstants.HTTP_REQUEST_PAUSE && downloadInfo != null)
			// 暂停
			downloadInfo.setState(downloadInfo.getPauseState());

		onHttpReqeust(identification, url, requestType, totalSize, downloadSize);

		if (mDownloadCallback != null) {
			try {
				mDownloadCallback.onHttpReqeust(downloadInfo, requestType);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
