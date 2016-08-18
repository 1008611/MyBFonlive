package com.wildwolf.mybf.cloud;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import bf.cloud.android.modules.p2p.BFStream;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.wildwolf.mybf.R;

public class BFSimpleListView extends ListView{
	private static final String[] mFromItems = new String[]{
		"videoName", 
		"definition","downloadSize", "fileSize", "speed"
	};
	private static final int[] mToItems = new int[]{
		R.id.video_name,
		R.id.definition, R.id.downloadSize, R.id.fileSize, R.id.speed
	};
	private List<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
	private SimpleAdapter mAdapter = null;
	private DecimalFormat mDecimalFormat = new DecimalFormat("0.0");
	
	public BFSimpleListView(Context context) {
		super(context);
		init();
	}
	
	public BFSimpleListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BFSimpleListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		// TODO Auto-generated method stub
		
	}
	
	public static String[] getFromItems(){
		return mFromItems;
	}
	
	public static int[] getToItems(){
		return mToItems;
	}
	
	public List<Map<String, Object>> getData(){
		return mData;
	}
	
	public void itemChanged(BFStream stream){
		for (Map<String, Object> item : mData){
			if (item.get("stream") == stream){
				item.put("stream", stream);
				item.put("videoName", stream.getVideo().getVideoName());
				item.put("definition", stream.getCurrentDefinition());
				item.put("downloadSize", mDecimalFormat.format((stream.getDownloadSize()/ 1024f / 1024f)));
				item.put("fileSize", mDecimalFormat.format(stream.getFileSize() / 1024f / 1024f));
				item.put("speed", mDecimalFormat.format(stream.getDownloadSpeed() / 1024f / 1024f));
			}
		}
		SimpleAdapter adapter = (SimpleAdapter) getAdapter();
		adapter.notifyDataSetChanged();
	}
	
	public void itemAdd(BFStream stream){
		if (streamExist(stream)){
			SimpleAdapter adapter = (SimpleAdapter) getAdapter();
			adapter.notifyDataSetChanged();
			return;
		}
		Map<String, Object> item = new HashMap<String, Object>();
		item.put("stream", stream);
		item.put("videoName", stream.getVideo().getVideoName());
		item.put("definition", stream.getCurrentDefinition());
		item.put("downloadSize", mDecimalFormat.format((stream.getDownloadSize()/ 1024f / 1024f)));
		item.put("fileSize", mDecimalFormat.format(stream.getFileSize() / 1024f / 1024f));
		item.put("speed", mDecimalFormat.format(stream.getDownloadSpeed() / 1024f / 1024f));
		mData.add(item);
		SimpleAdapter adapter = (SimpleAdapter) getAdapter();
		adapter.notifyDataSetChanged();
	}
	
	private boolean streamExist(BFStream stream){
		for (Map<String, Object> item : mData){
			if (item.get("stream") == stream)
				return true;
		}
		return false;
	}
}
