package com.android.dynamic.Exception;

/**
 * 方法反射调用失败时抛出
 * 
 * @ClassName: MethodInvokeErrorException
 * @author lytjackson@gmail.com
 * @date 2014-4-22
 * 
 */
public class MethodInvokeErrorException extends RuntimeException {

	/**
	 * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)
	 */
	private static final long serialVersionUID = 4406048741013758632L;
	private static final String INVOKE_ERROR = "Method invoke error, please check the api!!";

	public MethodInvokeErrorException() {
		super(INVOKE_ERROR);
	}

	public MethodInvokeErrorException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public MethodInvokeErrorException(String detailMessage) {
		super(detailMessage);
	}

	public MethodInvokeErrorException(Throwable throwable) {
		super(throwable);
	}

}
