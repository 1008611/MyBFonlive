package com.wildwolf.mybf.cloud.hybrid;

public class ShowVideoThread extends Thread {

	private String method;
	private HybridActivity activity;
	private String url;
	private int mode;

	@Override
	public void run() {
		// if ("showCaptor".equals(method)) {
		// activity.showCaptor();
		// } else {
		activity.showVideo(url, mode);
		// }
	}

	public HybridActivity getActivity() {
		return activity;
	}

	public void setActivity(HybridActivity activity) {
		this.activity = activity;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

}
