package com.nd.hilauncherdev.webconnect.downloadmanage.model;

/**
 * TODO linqiang form AppMarketUtil.CommonCallBack
 * 描述:
 * @author linqiang(866116)
 * @Since 2013-1-28
 * @param <E>
 */
public interface CommonCallBack<E> {
	public void invoke(final E... arg);
}
