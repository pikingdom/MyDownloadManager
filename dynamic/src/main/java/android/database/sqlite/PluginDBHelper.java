package android.database.sqlite;

import java.io.File;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Environment;
import android.util.Log;

import com.android.dynamic.Exception.SDcardNotFoundException;

/**
 * 插件开发数据库时,需要继承此类
 * 
 * @ClassName: PluginDBHelper
 * @author lytjackson@gmail.com
 * @date 2013-11-1
 * 
 */
public abstract class PluginDBHelper extends SQLiteOpenHelper {

	private static final String SDPATH = Environment.getExternalStorageDirectory().getPath();
	private static final String DATAPATH = "/data/data/";

	private SQLiteDatabase mDatabase;
	private final Context mContext;
	private final String mName;
	private final int mNewVersion;
	private boolean mIsInitializing = false;
	private boolean mIsSaveInSdcard = true;

	/**
	 * 存放在sd卡的数据库目录
	 */
	private static final String DBPATH_IN_SDCARD = SDPATH + "/db";
	private File mDBPathInSdcard = new File(DBPATH_IN_SDCARD);
	private File mDbFileInSdcard;
	/**
	 * 存放在/data/data包名目录下
	 */
	private File mDBPathInData;
	private File mDbFileInData;

	/**
	 * 
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 * @param saveInSdcard
	 *            插件数据库存放位置 true - sdcard false - /data/data/加载端包名/databases/
	 *            当不存在sdcard,又传入true,抛出异常
	 */
	public PluginDBHelper(Context context, String name, CursorFactory factory, int version, boolean saveInSdcard) {
		super(context, name, factory, version);
		// super(context, "plugin.db." + context.getPackageName() + "." + name,
		// factory, version);
		mContext = context;
		// mName = "plugin.db." + context.getPackageName() + "." + name;
		mName = name;
		mNewVersion = version;
		if (saveInSdcard && !isSDCardExist()) {
			throw new SDcardNotFoundException("SDcard not found,please change param to false,create databases on" + DATAPATH + context.getPackageName() + "/databases");
		}
		mIsSaveInSdcard = saveInSdcard;

		mDbFileInSdcard = new File(DBPATH_IN_SDCARD + "/" + mName);

		mDBPathInData = new File(DATAPATH + context.getApplicationContext().getPackageName() + "/databases");
		mDbFileInData = new File(mDBPathInData.getAbsolutePath() + "/" + mName);
	}

	private boolean isSDCardExist() {
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			return true;
		} else
			return false;
	}

	// public void execSQL(String sql) throws SQLException {
	// mDatabase.execSQL(sql);
	// }

	private void createDbFileIfNeeded() {
		if (mIsSaveInSdcard) {
			if (!mDBPathInSdcard.exists()) {
				mDBPathInSdcard.mkdirs();
			}
			if (!mDbFileInSdcard.exists()) {
				try {
					mDbFileInSdcard.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			if (!mDBPathInData.exists()) {
				mDBPathInData.mkdirs();
			}
			if (!mDbFileInData.exists()) {
				try {
					mDbFileInData.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		// TODO Auto-generated method stub
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openOrCreateDatabase(mIsSaveInSdcard ? mDbFileInSdcard : mDbFileInData, null);
			handleDowngrade(db);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != db) {
				db.close();
			}
		}

		if (!mIsSaveInSdcard) {
			this.mDatabase = super.getWritableDatabase();
			return this.mDatabase;
		}
		if (mDatabase != null && mDatabase.isOpen() && !mDatabase.isReadOnly()) {
			return mDatabase; // The database is already open for business
		}
		if (mIsInitializing) {
			throw new IllegalStateException("getWritableDatabase called recursively");
		}
		// If we have a read-only database open, someone could be using it
		// (though they shouldn't), which would cause a lock to be held on
		// the file, and our attempts to open the database read-write would
		// fail waiting for the file lock. To prevent that, we acquire the
		// lock on the read-only database, which shuts out other users.
		boolean success = false;
		if (mDatabase != null)
			mDatabase.lock();
		try {
			mIsInitializing = true;
			createDbFileIfNeeded();
			db = SQLiteDatabase.openOrCreateDatabase(mIsSaveInSdcard ? mDbFileInSdcard : mDbFileInData, null);
			int version = db.getVersion();
			if (version != mNewVersion) {
				db.beginTransaction();
				try {
					if (version == 0) {
						onCreate(db);
					} else {
						if (version > mNewVersion) {
							onDowngrade(db, version, mNewVersion);
						} else {
							onUpgrade(db, version, mNewVersion);
						}
					}
					db.setVersion(mNewVersion);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
			onOpen(db);
			success = true;
			this.mDatabase = db;
			return db;
		} finally {
			mIsInitializing = false;
			if (success) {
				if (mDatabase != null) {
					try {
						mDatabase.close();
					} catch (Exception e) {
					}
					mDatabase.unlock();
				}
				mDatabase = db;
			} else {
				if (mDatabase != null)
					mDatabase.unlock();
				if (db != null)
					db.close();
			}
		}
	}

	private void handleDowngrade(SQLiteDatabase db) {
		// 适配api level小于11,无onDowngrade方法的手机
		if (null == db) {
			return;
		}
		int version = db.getVersion();
		if (version > mNewVersion || version == 0) {
			// 降级
			mContext.deleteDatabase(mName);
		}
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	@Override
	public synchronized SQLiteDatabase getReadableDatabase() {
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openOrCreateDatabase(mIsSaveInSdcard ? mDbFileInSdcard : mDbFileInData, null);
			handleDowngrade(db);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != db) {
				db.close();
			}
		}

		if (!mIsSaveInSdcard) {
			this.mDatabase = super.getReadableDatabase();
			return this.mDatabase;
		}
		if (mDatabase != null && mDatabase.isOpen()) {
			return mDatabase; // The database is already open for business
		}
		if (mIsInitializing) {
			throw new IllegalStateException("getReadableDatabase called recursively");
		}
		try {
			return getWritableDatabase();
		} catch (SQLiteException e) {
			if (mName == null)
				throw e; // Can't open a temp database read-only!
			Log.e("PluginDBHelper", "Couldn't open " + mName + " for writing (will try read-only):", e);
		}

		try {
			mIsInitializing = true;
			String path = mContext.getDatabasePath(mName).getPath();
			File file = new File(path);
			db = SQLiteDatabase.openOrCreateDatabase(file, null);
			// db = SQLiteDatabase.openOrCreateDatabase(mIsSaveInSdcard
			// ?mDbFileInSdcard:mDbFileInData, null);
			if (db.getVersion() != mNewVersion) {
				throw new SQLiteException("Can't upgrade read-only database from version " + db.getVersion() + " to " + mNewVersion + ": " + path);
			}
			onOpen(db);
			Log.e("PluginDBHelper", "Opened " + mName + " in read-only mode");
			mDatabase = db;
			return mDatabase;
		} finally {
			mIsInitializing = false;
			if (db != null && db != mDatabase)
				db.close();
		}
	}

	/**
	 * Close any open database object.
	 */
	public synchronized void close() {
		if (mIsInitializing)
			throw new IllegalStateException("Closed during initialization");

		if (mDatabase != null && mDatabase.isOpen()) {
			mDatabase.close();
			mDatabase = null;
		}
	}

	public Cursor query(String sql) {
		getReadableDatabaseSure();
		return this.mDatabase.rawQuery(sql, null);
	}

	public Cursor query(String sql, String[] selectionArgs) {
		getReadableDatabaseSure();
		return this.mDatabase.rawQuery(sql, selectionArgs);
	}

	public Cursor query(String table, String key, String value, String sort) {
		getReadableDatabaseSure();
		Cursor cursor = this.mDatabase.query(table, null, key + "=?", new String[] { value }, null, null, sort);
		return cursor;
	}

	public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
		getReadableDatabaseSure();
		Cursor cursor = this.mDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		return cursor;
	}

	private void getReadableDatabaseSure() {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			this.mDatabase = getReadableDatabase();
		} else if (!this.mDatabase.isReadOnly()) {
			this.mDatabase.close();
			this.mDatabase = getReadableDatabase();
		}
	}

	public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
		makesureWriteable();
		return this.mDatabase.update(table, values, whereClause, whereArgs);
	}

	public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
		makesureWriteable();
		return this.mDatabase.insertOrThrow(table, nullColumnHack, values);
	}

	public boolean execSQL(String sql) {
		makesureWriteable();
		try {
			this.mDatabase.execSQL(sql);
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		}
		return false;
	}

	public boolean delete(String table) {
		makesureWriteable();
		try {
			this.mDatabase.delete(table, null, null);
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		}
		return false;
	}

	public boolean delete(String table, String where, String[] whereValue) {
		makesureWriteable();
		try {
			this.mDatabase.delete(table, where, whereValue);
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		}
		return false;
	}

	private void makesureWriteable() {
		if ((this.mDatabase != null) && (this.mDatabase.isOpen())) {
			if (this.mDatabase.isReadOnly()) {
				this.mDatabase.close();
				this.mDatabase = getWritableDatabase();
			}
		} else
			this.mDatabase = getWritableDatabase();
	}

	public boolean execSQL(String sql, Object[] obj) {
		makesureWriteable();
		try {
			this.mDatabase.execSQL(sql, obj);
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		}
		return false;
	}

	public boolean execBatchSQL(String[] sqls, boolean transaction) {
		makesureWriteable();
		if (transaction) {
			this.mDatabase.beginTransaction();
		}

		for (String sql : sqls) {
			try {
				if (sql != null)
					this.mDatabase.execSQL(sql);
			} catch (Exception e) {
				e.printStackTrace();
				if (transaction) {
					this.mDatabase.endTransaction();
				}
				return false;
			}
		}

		if (transaction) {
			this.mDatabase.setTransactionSuccessful();
			this.mDatabase.endTransaction();
		}

		return true;
	}

	public long add(String table, ContentValues values) {
		makesureWriteable();
		return this.mDatabase.insert(table, null, values);
	}

	public void beginTransaction() {
		makesureWriteable();
		this.mDatabase.beginTransaction();
	}

	public void endTransaction() {
		makesureWriteable();
		this.mDatabase.setTransactionSuccessful();
		this.mDatabase.endTransaction();
	}

	public void endTransactionByException() {
		makesureWriteable();
		this.mDatabase.endTransaction();
	}

}
