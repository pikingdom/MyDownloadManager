package com.nd.hilauncherdev.webconnect.downloadmanage.model ;

import com.nd.hilauncherdev.webconnect.downloadmanage.model.BaseDownloadInfo;

interface IDownloadManagerService {

	/**
	 * 添加下载任务<br>
	 * true 添加成功<br>
	 * false 添加失败;已在下载列表
	 */
	boolean addDownloadTask(in BaseDownloadInfo info);
	
	/**
	 * 暂停任务
	 * @param identification 下载唯一标识 
	 * @return true 下载队列存在对应的下载任务，等待暂停广播处理<br>
	 *         false 下载队列不存在对应的下载任务，自行处理ui
	 */
	boolean pause(String identification);	 
	
	/**
	 * 取消任务
	 * @param identification 下载唯一标识 
	 * @return true 下载队列存在对应的下载任务，等待取消广播处理<br>
	 *         false 下载队列不存在对应的下载任务，自行处理ui
	 */
	boolean cancel(String identification);
	
	/**
	 * 清除总队列
	 */
	void clearAllDownloadTask();
	
	/**
	 * 获取下载总队列
	 * @return
	 */
	Map getDownloadTasks();
	
	/**
	 * 返回任务数，包括所有状态的
	 * @return
	 */
	int getTaskCount();
	
	/**
	* 静默安装
	*/
	void installAppInThread(String apkPath);
	
	/**
	* 是否正在安装
	*/
	boolean isApkInstalling(String packageName);
	
	/**
	* 判断服务是否活动中
	*/
	boolean isServiceAlive();
	
	/**
	* 获取指定Key的下载状态
	*/
	BaseDownloadInfo getDownloadState(String identification);
	
	/**
	 * 继续下载
	 */
	boolean continueDownload(String identification);
		
    /**
     * 添加静默下载任务<br>
     * true 添加成功<br>
     * false 添加失败;已在下载列表
     */
    boolean addSilentDownloadTask(in BaseDownloadInfo info);
    
    /**
     * 获取单个下载任务
     * @param identification 下载唯一标识 
     */
    BaseDownloadInfo getDownloadTask(String identification);
    
    /**
     * 添加可在23g下进行下载的静默下载任务
     * true 添加成功
     * false 添加失败;已在下载列表
     */
    boolean addSilent23GTask(in BaseDownloadInfo info, boolean enable23G);
    
    /**
     * 控制可在23G下进行下载的后台任务
     * @param identification 任务标识
     * @param enable23G 该任务是否开启23G下载
     */
    void controlSilent23GTask(String identification, boolean enable23G);
    
    /**
     * 暂停后台下载任务
     */
    void pauseSilentTask(String identification, boolean is23GTask);
    
    /**
     * 继续后台下载任务
     */
    void continuteSilentTask(String identification, boolean is23GTask);
    
    /**
     * 取消后台下载任务
     */
    void cancelSilentTask(String identification, boolean is23GTask);
    
    /**
     * 修改附加信息
     */
    void modifyAdditionInfo(in BaseDownloadInfo info);
    
    /**
     * 设置下载回调
     */
    void setDownloadCallback(String callbackCls);
    
    /**
     * 设置数据库
     */
    void setDownloadDb(String dbCls);
    
    /**
     * 设置下载广播的action
     */
    void setBroadcastAction(String action);
}