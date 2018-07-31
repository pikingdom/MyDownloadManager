package com.nd.hilauncherdev.webconnect.downloadmanage.model;

public abstract class AbstractDownloadCallback {

    public abstract void onHttpReqeust(BaseDownloadInfo downloadInfo, int requestType);

    public abstract void onDownloadWorking(BaseDownloadInfo downloadInfo);

    public abstract void onDownloadCompleted(BaseDownloadInfo downloadInfo, boolean fileExist);

    public abstract void onBeginDownload(BaseDownloadInfo downloadInfo);

    public abstract void onDownloadFailed(BaseDownloadInfo downloadInfo, Exception e);
}
