package com.nd.hilauncherdev.webconnect.downloadmanage.model;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.database.Cursor;

import com.nd.hilauncherdev.core.DownloadStringUtil;
import com.nd.hilauncherdev.framework.db.AbstractDataBase;

/**
 * 下载db操作
 * 
 * @author pdw
 * @version
 * @date 2012-9-19 下午08:05:00
 */
public class DownloadDBManager {
	
	private static AbstractDataBase getDataBase(Context context) {
		AbstractDataBase db = null;
		try {
			if (DownloadStringUtil.isEmpty(DownloadServerService.sDbClass)) {
				return null;
			}
			
			@SuppressWarnings("rawtypes")
			Class cls = Class.forName(DownloadServerService.sDbClass);
			Object[] arg = new Object[] { context };
			Method method = cls.getDeclaredMethod("getInstance", new Class[] { Context.class });
			db = (AbstractDataBase) method.invoke(cls, arg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return db;
	}
	
	public static ArrayList<BaseDownloadInfo> loadDownloadLog(Context ctx) {
		AbstractDataBase db = null;
		Cursor cursor = null;
		ArrayList<BaseDownloadInfo> list = new ArrayList<BaseDownloadInfo>();
		try {
			String sql = DownloadLogTable.getSelectAllSql();
			db = getDataBase(ctx);
			cursor = db.query(sql);
			if (cursor != null) {
				boolean hasNext = cursor.moveToFirst();
				while (hasNext) {
					String file = cursor.getString(DownloadLogTable.INDEX_FILE_PATH);
					final int index = file.lastIndexOf("/");
					String savedDir = file.substring(0, index + 1);
					String savedName = file.substring(index + 1);
					BaseDownloadInfo info = new BaseDownloadInfo(cursor.getString(DownloadLogTable.INDEX_ID),
																 cursor.getInt(DownloadLogTable.INDEX_FILE_TYPE),
																 cursor.getString(DownloadLogTable.INDEX_DOWNLOAD_URL),
																 cursor.getString(DownloadLogTable.INDEX_TITLE), 
																 savedDir,
																 savedName,
																 cursor.getString(DownloadLogTable.INDEX_ICON_PATH));
					info.initState(ctx);
					info.downloadSize = cursor.getString(DownloadLogTable.INDEX_DOWNLOAD_SIZE);
					info.totalSize = cursor.getString(DownloadLogTable.INDEX_TOTAL_SIZE);
					info.progress = cursor.getInt(DownloadLogTable.INDEX_PROGRESS);
					info.mAdditionInfo = cursor.getString(DownloadLogTable.INDEX_ADDITION_INFO);
					info.initSubDownloadInfoList();
					info.initDownloadState();
					if (info.getIsSilent()) {
						if (info.is23GEnableTask()) {
							Silent23GDownloadWorkerSupervisor.addWaitingPool(info);
						} else {
							SilentDownloadWorkerSupervisor.addWaitingPool(info);
						}
					} else {
						list.add(info);
					}
					hasNext = cursor.moveToNext();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null)
				cursor.close();
			if (db != null)
				db.close();
		}
		return list;
	}

	/**
	 * <p>
	 * 清除下载记录
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-19 下午08:11:44
	 * 
	 * @param info
	 * @return
	 */
	public static boolean deleteDownloadLog(Context ctx, BaseDownloadInfo info) {
		final String sql = DownloadLogTable.getDeleteSql(info);
		return ddl(ctx, false, sql);
	}

	/**
	 * <p>
	 * 清除下载记录
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-19 下午08:11:44
	 * 
	 * @param info
	 * @return
	 */
	public static boolean deleteDownloadLog(Context ctx, ArrayList<BaseDownloadInfo> infos) {
		if (infos.size() > 0) {
			final String[] sqls = new String[infos.size()];
			int index = 0;
			for (BaseDownloadInfo info : infos) {
				sqls[index++] = DownloadLogTable.getDeleteSql(info);
			}
			return ddl(ctx, false, sqls);
		}
		return false;
	}

	/**
	 * <p>
	 * 清除所有下载记录
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-19 下午08:11:13
	 * 
	 * @param ctx
	 * @return
	 */
	public static boolean deleteAllDownloadLog(Context ctx) {
		final String sql = DownloadLogTable.getDeleteAllSql();
		return ddl(ctx, false, sql);
	}

	/**
	 * <p>
	 * 更新进度
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-19 下午08:09:48
	 * 
	 * @param ctx
	 * @param info
	 * @return
	 */
	public static boolean updateProgress(Context ctx, BaseDownloadInfo info) {
		final String sql = DownloadLogTable.getUpdateProgressSql(info);
		return ddl(ctx, false, sql);
	}

	/**
	 * <p>
	 * 插入下载log
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-19 下午08:07:59
	 * 
	 * @param ctx
	 * @param info
	 * @return
	 */
	public static boolean insertLog(Context ctx, BaseDownloadInfo info) {
		final String sql = DownloadLogTable.getInsertLogSql(info);
		return ddl(ctx, false, sql);
	}

	private static boolean ddl(Context ctx, boolean transaction, String... sqls) {
		if (sqls == null || sqls.length < 1)
			return false;

		AbstractDataBase db = null;
		try {
			db = getDataBase(ctx);
			if (sqls.length == 1) {
				db.execSQL(sqls[0]);
			} else {
				db.execBatchSQL(sqls, transaction);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		} finally {
			if (db != null)
				db.close();
		}
		return true;
	}

	/**
	 * <p>
	 * 获取下载任务列表，包括已下载完成
	 * </p>
	 * 
	 * <p>
	 * date: 2012-9-23 下午04:53:33
	 * 
	 * @param ctx
	 * @return
	 */
	public static Map<String, BaseDownloadInfo> getDownloadLoadTask(Context ctx) {

		Map<String, BaseDownloadInfo> downloadTasks = new ConcurrentHashMap<String, BaseDownloadInfo>();

		// 下载队列
		Vector<BaseDownloadInfo> queue = BaseDownloadWorkerSupervisor.getDownloadingQueue();
		for (BaseDownloadInfo info : queue) {
			downloadTasks.put(info.getIdentification(), info);
		}

		// 等待队列
		queue = BaseDownloadWorkerSupervisor.getWaitingQueue();
		for (BaseDownloadInfo info : queue) {
			if (!downloadTasks.containsKey(info.getIdentification())) {
				downloadTasks.put(info.getIdentification(), info);
			}
		}

		/**
		 * 过滤掉下载队列和等待队列的记录
		 */
		ArrayList<BaseDownloadInfo> dbLog = DownloadDBManager.loadDownloadLog(ctx);
		ArrayList<BaseDownloadInfo> deleteList = new ArrayList<BaseDownloadInfo>();
		File file = null;
		for (BaseDownloadInfo info : dbLog) {
//			if (info.hasDownloaded()) { // 过滤掉被用户删除安装文件的记录，将其置为未下载，保持一致
//				file = new File(info.getApkPath());
//				if (!file.exists()) {
//					deleteList.add(info);
//					continue;
//				}
//			}
			if (!downloadTasks.containsKey(info.getIdentification())) {
				downloadTasks.put(info.getIdentification(), info);
			}
		}

		if (deleteList.size() > 0)
			deleteDownloadLog(ctx, deleteList);

		return downloadTasks;
	}

	/**
	 * 是否存在下载记录
	 * 
	 * @param ctx
	 * @param info
	 * @return
	 */
	public static boolean hasLogDownloadRecord(Context ctx, BaseDownloadInfo info) {
		AbstractDataBase db = null;
		Cursor cursor = null;

		try {
			db = getDataBase(ctx);
			cursor = db.query(DownloadLogTable.getSelectOneSql(info));
			if (cursor != null)
				return cursor.getCount() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}
		return false;
	}

	/**
	 * 获取下载进度
	 * 
	 * @param identification
	 * @return
	 */
	public static int getDownloadProgress(Context ctx, BaseDownloadInfo info) {
		if (info == null)
			return 0;
		AbstractDataBase db = null;
		Cursor cursor = null;
		try {
			db = getDataBase(ctx);
			cursor = db.query(DownloadLogTable.getSelectOneSql(info));
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getInt(DownloadLogTable.INDEX_PROGRESS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}
		return 0;
	}
	
	/**
	 * 更新下载记录
	 * @param ctx
	 * @param info
	 * @return
	 */
	public static boolean updateLog(Context ctx, BaseDownloadInfo info) {
		final String sql = DownloadLogTable.getUpdateLogSql(info);
		return ddl(ctx, false, sql);
	}

	/**
	 * 更新附加信息
	 */
	public static boolean updateAdditionInfo(Context ctx, BaseDownloadInfo info) {
		final String sql = DownloadLogTable.getUpdateAdditionInfoSql(info);
		return ddl(ctx, false, sql);
	}
}
