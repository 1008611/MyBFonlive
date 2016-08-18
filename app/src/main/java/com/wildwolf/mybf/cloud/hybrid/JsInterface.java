package com.wildwolf.mybf.cloud.hybrid;

import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.wildwolf.mybf.R;


public class JsInterface {  
    private Handler handler = null;  
    private WebView webView = null;
    private HybridActivity htmlActivity;
  
    public JsInterface(HybridActivity htmlActivity, Handler handler) {  
    	this.htmlActivity = htmlActivity;
        this.webView = (WebView)htmlActivity.findViewById(R.id.webView);
        this.handler = handler;  
    }  
    
    @JavascriptInterface
    public void init(){
        //通过handler来确保init方法的执行在主线程中  
        handler.post(new Runnable() {  
            public void run() {
                //调用客户端setContactInfo方法  
                webView.loadUrl("javascript:initOk('ok')");  
            }  
        });  
    }  
    
    
    @JavascriptInterface
    public void showVideo(String url, int mode){
    	ShowVideoThread thread = new ShowVideoThread();
    	thread.setActivity(this.htmlActivity);
    	thread.setUrl(url);
    	thread.setMode(mode);
    	handler.post(thread);  
    }
    @JavascriptInterface
    public void showCaptor(){
    	ShowVideoThread thread = new ShowVideoThread();
    	thread.setActivity(this.htmlActivity);
    	thread.setMethod("showCaptor");
    	handler.post(thread);  
    }
}  