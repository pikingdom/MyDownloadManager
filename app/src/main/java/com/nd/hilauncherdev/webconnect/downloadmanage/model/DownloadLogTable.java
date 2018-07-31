package com.nd.hilauncherdev.webconnect.downloadmanage.model;


import com.nd.hilauncherdev.core.DownloadStringUtil;

/**
 * 下载记录表结构
 * 
 * @author pdw
 * @date 2012-9-19 下午06:59:01
 */
public class DownloadLogTable {

	public DownloadLogTable() {

	}

	public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS 'log_download' " + "('_id' VARCHAR(32) PRIMARY KEY  NOT NULL ,'download_url' VARCHAR(32) NOT NULL ,"
			+ "'progress' INTEGER NOT NULL  DEFAULT (0) ,'total_size' VARCHAR(32) NOT NULL  DEFAULT ('0.0MB') ," + "'title' VARCHAR(8) NOT NULL ,'icon_path' VARCHAR(32) ,"
			+ "'download_size' VARCHAR(32) NOT NULL  DEFAULT ('0.0MB') , 'file_path' VARCHAR(32) ,"
			+ "'file_type' INTEGER NOT NULL  DEFAULT (0) , addition_info)";
	
	/**
	 * 用于3.5.2版本的数据库表结构升级
	 */
	public static final String CREATE_TABLE_4 = "CREATE TABLE IF NOT EXISTS 'log_download' " + "('_id' VARCHAR(32) PRIMARY KEY  NOT NULL ,'download_url' VARCHAR(32) NOT NULL ,"
			+ "'progress' INTEGER NOT NULL  DEFAULT (0) ,'total_size' VARCHAR(32) NOT NULL  DEFAULT ('0.0MB') ," + "'title' VARCHAR(8) NOT NULL ,'icon_path' VARCHAR(32) ,"
			+ "'download_size' VARCHAR(32) NOT NULL  DEFAULT ('0.0MB') , 'file_path' VARCHAR(32))";

	/**
	 * 3.5.2版本的数据库表结构升级，修改"_id"字段的类型为varchar(32)型
	 */
	public final String[] ALTER_TABLE = { "ALTER TABLE log_download RENAME TO log_download_old", CREATE_TABLE_4, "INSERT INTO log_download SELECT * FROM log_download_old",
			"DROP TABLE IF EXISTS log_download_old" };

	/**
	 * 5.1版本，修改通用的下载管理模块
	 */
	public final String[] ALTER_TABLE_7 = { "ALTER TABLE log_download ADD 'file_type' INTEGER NOT NULL  DEFAULT (0)", 
			                                  "ALTER TABLE log_download ADD 'addition_info'"};
	
	private static String INSERT_LOG = "INSERT INTO log_download(_id,download_url,title,icon_path,download_size,file_path,progress,total_size,file_type,addition_info) " + "VALUES ('%s','%s','%s','%s','%s','%s',%d,'%s',%d,'%s')";

	/**更新下载记录*/
	private static String UPDATE_LOG = "UPDATE log_download set _id='%s',download_url='%s',title='%s',icon_path='%s',download_size='%s',file_path='%s',progress='%s',total_size='%s' where _id='%s' ";
	
	private static String UPDATE_PROGRESS = "UPDATE log_download set progress = %d ,download_size = '%s' where _id = '%s'";

	private static String UPDATE_ADDITION = "UPDATE log_download set addition_info = '%s' where _id = '%s'";

	private static String DELETE_ALL = "DELETE FROM log_download";

	private static String DELETE = "DELETE FROM log_download where _id = '%s'";

	private static String SELECT_ALL = "select _id,download_url,title,icon_path,download_size,file_path,progress,total_size,file_type,addition_info from log_download order by progress";

	private static String SELECT_ONE = "select _id,download_url,title,icon_path,download_size,file_path,progress,total_size,file_type,addition_info from log_download where _id = '%s'";

	public static int INDEX_ID = 0;

	public static int INDEX_DOWNLOAD_URL = 1;

	public static int INDEX_TITLE = 2;

	public static int INDEX_ICON_PATH = 3;

	public static int INDEX_DOWNLOAD_SIZE = 4;

	public static int INDEX_FILE_PATH = 5;

	public static int INDEX_PROGRESS = 6;

	public static int INDEX_TOTAL_SIZE = 7;
	
	public static int INDEX_FILE_TYPE = 8;

	public static int INDEX_ADDITION_INFO = 9;

	public static String getInsertLogSql(BaseDownloadInfo info) {
		return String.format(INSERT_LOG, info.getIdentification(), 
				                          info.getDownloadUrl(), 
				                          DownloadStringUtil.filtrateInsertParam(info.getTitle()),
				                          info.getIconPath(), 
				                          info.downloadSize, 
				                          info.getSavedDir() + info.getSavedName(),
				                          info.progress, 
				                          info.totalSize, 
				                          info.getFileType(), 
				                          info.mAdditionInfo);
	}

	public static String getUpdateProgressSql(BaseDownloadInfo info) {
		return String.format(UPDATE_PROGRESS, info.progress, info.downloadSize, info.getIdentification());
	}

	public static String getDeleteAllSql() {
		return DELETE_ALL;
	}

	public static String getDeleteSql(BaseDownloadInfo info) {
		return String.format(DELETE, info.getIdentification());
	}

	public static String getSelectAllSql() {
		return SELECT_ALL;
	}

	public static String getSelectOneSql(BaseDownloadInfo info) {
		return String.format(SELECT_ONE, info.getIdentification());
	}
	
	/**
	 * 获取更新记录的SQL
	 * @param info
	 * @return
	 */
	public static String getUpdateLogSql(BaseDownloadInfo info) {
		return String.format(UPDATE_LOG, 
							info.getIdentification(), 
							info.getDownloadUrl(), 
							DownloadStringUtil.filtrateInsertParam(info.getTitle()),
							info.getIconPath(), 
							info.downloadSize, 
							info.getSavedDir() + info.getSavedName(),
							info.progress, 
							info.totalSize,
							info.getIdentification());
	}

	public static String getUpdateAdditionInfoSql(BaseDownloadInfo info) {
		return String.format(UPDATE_ADDITION, (info.mAdditionInfo != null ? info.mAdditionInfo : ""), info.getIdentification());
	}
}
