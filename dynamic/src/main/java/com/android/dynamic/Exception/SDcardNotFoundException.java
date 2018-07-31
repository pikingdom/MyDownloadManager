package com.android.dynamic.Exception;

/**
 * 插件apk或jar文件丢失情况抛出,建议后续进入插件修复
 * 
 * @ClassName: SDcardNotFoundException
 * @author lytjackson@gmail.com
 * @date 2014-4-22
 * 
 */
public class SDcardNotFoundException extends RuntimeException {

	/**
	 * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)
	 */
	private static final long serialVersionUID = -751919343986182342L;

	public SDcardNotFoundException() {
		super();
	}

	public SDcardNotFoundException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public SDcardNotFoundException(String detailMessage) {
		super(detailMessage);
	}

	public SDcardNotFoundException(Throwable throwable) {
		super(throwable);
	}

}
