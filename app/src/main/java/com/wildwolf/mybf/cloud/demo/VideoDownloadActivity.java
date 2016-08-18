package com.wildwolf.mybf.cloud.demo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.wildwolf.mybf.R;
import com.wildwolf.mybf.cloud.BFSimpleListView;

import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.BFStream.StreamDownloadState;
import bf.cloud.android.modules.p2p.BFStream.StreamStoreMode;
import bf.cloud.android.modules.p2p.Video;
import bf.cloud.android.modules.p2p.Video.VideoListener;
import bf.cloud.android.playutils.VideoManager;
import bf.cloud.android.playutils.VideoService;
import bf.cloud.android.playutils.VideoManager.DownloadListener;
import bf.cloud.android.playutils.VideoManager.LocalVideoListener;
import bf.cloud.android.playutils.VideoManager.StreamType;
import bf.cloud.android.playutils.VideoService.VideoBinder;

public class VideoDownloadActivity extends Activity implements DownloadListener {
	private static final String TAG = VideoDownloadActivity.class
			.getSimpleName();
	private Context mContext = null;
	private EditText etUrl = null;
	private EditText etToken = null;
	private Button btStartDownload = null;
	private Button btDelete = null;
	private BFSimpleListView lvLocalVideos = null;
	private ServiceConnection mConnection = null;
	private VideoService mVideoService = null;
	private Bundle mParams = null;
	
	private static final int MSG_VIDEO_SERVICE_READY = 106;
	private static final int MSG_VIDEO_INFO_PREPARED = 100;
	private static final int MSG_DOWNLOAD_STARTED = 101;
	private static final int MSG_DOWNLOAD_COMPLETED = 102;
	private static final int MSG_DOWNLOAD_SIZE_CHANGED = 104;
	private static final int MSG_DELETE_SUCCEED = 103;
	private static final int MSG_REFRESH_LIST = 105;
	private Handler mUIHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_VIDEO_SERVICE_READY:
				initList();
				break;
			case MSG_VIDEO_INFO_PREPARED:
				Video video = (Video) msg.obj;
				List<String> allDefinitions = video.getDefinitions();
				String[] items = new String[allDefinitions.size()];
				for (int i = 0; i < allDefinitions.size(); i++) {
					items[i] = allDefinitions.get(i);
				}
				showDefinitionTips(items, allDefinitions, video);
				
				break;
			case MSG_DOWNLOAD_STARTED:{
				BFStream stream = (BFStream) msg.obj;
				lvLocalVideos.itemAdd(stream);
				break;
			}
			case MSG_DOWNLOAD_COMPLETED:{
				BFStream stream = (BFStream) msg.obj;
				lvLocalVideos.itemChanged(stream);
				Toast.makeText(VideoDownloadActivity.this,
						stream.getVideo().getVideoName() + "下载完毕", Toast.LENGTH_SHORT).show();
				break;
			}
			case MSG_DOWNLOAD_SIZE_CHANGED:{
				BFStream stream = (BFStream) msg.obj;
				lvLocalVideos.itemChanged(stream);
				break;
			}
			case MSG_REFRESH_LIST:
				refreshList();
				break;
			default:
				break;
			}
			return false;
		}
	});
	
	private void showDefinitionTips(String[] items, final List<String> allDefinitions, final Video video) {
		new AlertDialog.Builder(mContext)
			.setSingleChoiceItems(items, items.length - 1, null)
			.setPositiveButton("下载",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						int position = ((AlertDialog) dialog)
								.getListView()
								.getCheckedItemPosition();
						String definition = allDefinitions
								.get(position);
						BFStream stream = mVideoService.getStream(video, definition, 
								StreamType.STREAM_FOR_DOWNLOAD, StreamStoreMode.MEDIA_STREAM_STORE_FILE);
						mVideoService.startDownload(stream);
					}
				})
				.setNegativeButton("取消",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {

					}
				}).show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);
		Intent intent = getIntent();
		if (intent != null){
			mParams = intent.getExtras();
		}
		mContext = this;
		init();
	}

	private void init() {
		mConnection = new ServiceConnection() {
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mVideoService = ((VideoBinder)service).getService();
				mVideoService.registLocalVideoListener(new LocalVideoListener() {
					
					@Override
					public void onChanged(List<Video> list) {
						mUIHandler.sendEmptyMessage(MSG_REFRESH_LIST);
					}
				});
				mVideoService.registDownloadListener(VideoDownloadActivity.this);
				mUIHandler.sendEmptyMessage(MSG_VIDEO_SERVICE_READY);
			}
		};
		Intent intent = new Intent();
		intent.setClass(this, VideoService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		etUrl = (EditText) findViewById(R.id.url);
		if (mParams != null && mParams.getString("url") != null)
			etUrl.setText(mParams.getString("url"));
		else
			etUrl.setText("servicetype=1&uid=4995606&fid=D754D209A442A6787962AB1552FF9412");
		etToken = (EditText) findViewById(R.id.token);
		btStartDownload = (Button) findViewById(R.id.start_download);
		btStartDownload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String url = etUrl.getText().toString();
				String token = etToken.getText().toString();
				//调用接口，等待回调。获取清晰度列表、视频信息等.见onDownloadEvent
				mVideoService.createVideo(url, token, new VideoListener() {
					
					@Override
					public void onVideoPrepared(Video video) {
						Message msg = new Message();
						msg.what = MSG_VIDEO_INFO_PREPARED;
						msg.obj = video;
						mUIHandler.sendMessage(msg);
					}

					@Override
					public void onQueryError(Video video, int error) {
						Log.d(TAG, "onQueryError eror:" + error);
						mVideoService.destoryVideo(video);
					}
				});
			}
		});
		btDelete = (Button) findViewById(R.id.delete);
		btDelete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String url = etUrl.getText().toString();
				mVideoService.removeLocalVideo(url);
			}
		});
		findViewById(R.id.start_all_download).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mVideoService.startAllPendingDownloadTask();
			}
		});
		findViewById(R.id.stop_all_download).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mVideoService.stopAllDownloadingTask();
			}
		});
		// 列举已下载的视频
		lvLocalVideos = (BFSimpleListView) findViewById(R.id.local_videos);
	}
	
	private void initList(){
		SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.bf_list_view_layout, 
				BFSimpleListView.getFromItems(), BFSimpleListView.getToItems());
		lvLocalVideos.setAdapter(adapter);
		lvLocalVideos.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int index, long arg3) {
				SimpleAdapter adapter = (SimpleAdapter) parent.getAdapter();
				Map<String, Object> map = (Map<String, Object>) adapter.getItem(index);
				Log.d(TAG, "onItemClick");
				BFStream stream = (BFStream) map.get("stream");
				if (stream.isDownloadCompleted()){
					Bundle params = new Bundle();
					params.putString("url", stream.getVideo().getUrl());
					params.putString("token", stream.getVideo().getToken());
					params.putString("definition", stream.getCurrentDefinition());
					Intent intent = new Intent();
					intent.putExtras(params);
					intent.setClass(VideoDownloadActivity.this, VodLocalPlayDemo.class);
					startActivity(intent);
				} else {
					if (stream.getDownloadState() == StreamDownloadState.IDLE)
						mVideoService.startDownload(stream);
					else
						mVideoService.stopDownload(stream);
					return;
				}
			}
		});
	}

	private void refreshList() {
		SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.bf_list_view_layout, 
				BFSimpleListView.getFromItems(), BFSimpleListView.getToItems());
		lvLocalVideos.setAdapter(adapter);
		lvLocalVideos.invalidate();
	}

	@Override
	protected void onDestroy() {
		mVideoService.unregistDownloadListener();
		mVideoService.unregistLocalVideoListener();
		unbindService(mConnection);
		mConnection = null;
		mVideoService = null;
		super.onDestroy();
	}

	@Override
	public void onDownloadError(BFStream stream, int error) {
		Log.d(TAG, "error:" + error);
	}

	@Override
	public void onDownloadEvent(BFStream stream, int event) {
		Message msg = new Message();
		msg.obj = stream;
		switch (event) {
		case VideoManager.EVENT_DOWNLOAD_STARTED:
			msg.what = MSG_DOWNLOAD_STARTED;
			mUIHandler.sendMessage(msg);
			break;
		case VideoManager.EVENT_DOWNLOAD_COMPLETED:
			msg.what = MSG_DOWNLOAD_COMPLETED;
			mUIHandler.sendMessage(msg);
			break;
		case VideoManager.EVENT_DOWNLOAD_DOWNLOAD_SIZE_CHANGED:
			msg.what = MSG_DOWNLOAD_SIZE_CHANGED;
			mUIHandler.sendMessage(msg);
			break;

		default:
			break;
		}
	}
	
	private List<Map<String, Object>> getData(){
		List<Map<String, Object>> list = lvLocalVideos.getData();
		list.clear();
		CopyOnWriteArrayList<Video> localVideos = mVideoService.getLocalVideoList();
		DecimalFormat decimalFormat = new DecimalFormat("0.0");
		for (Video video : localVideos){
			CopyOnWriteArrayList<BFStream> streams = video.getLocalStreamList();
			for (BFStream stream : streams){
				Map<String, Object> item = new HashMap<>();
				item.put("stream", stream);
				item.put("videoName", stream.getVideo().getVideoName());
				item.put("definition", stream.getCurrentDefinition());
				item.put("downloadSize", decimalFormat.format((stream.getDownloadSize()/ 1024f / 1024f)));
				item.put("fileSize", decimalFormat.format(stream.getFileSize() / 1024f / 1024f));
				item.put("speed", decimalFormat.format(stream.getDownloadSpeed() / 1024f / 1024f));
				list.add(item);
			}
		}
		return list;
	}
}
