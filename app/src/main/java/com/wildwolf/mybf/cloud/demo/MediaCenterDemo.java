package com.wildwolf.mybf.cloud.demo;


import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.Video;
import bf.cloud.android.modules.p2p.BFStream.StreamStoreMode;
import bf.cloud.android.modules.p2p.Video.VideoListener;
import bf.cloud.android.playutils.VideoManager;
import bf.cloud.android.playutils.VideoService;
import bf.cloud.android.playutils.VideoManager.PlayListener;
import bf.cloud.android.playutils.VideoManager.StreamType;
import bf.cloud.android.playutils.VideoService.VideoBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.wildwolf.mybf.R;

public class MediaCenterDemo extends Activity implements SurfaceHolder.Callback{
	private Button btStart = null;
	private Button btStop = null;
	private SurfaceView mSurfaceView = null;
	private boolean mIsViewPrepared = false;
	private SurfaceHolder holder = null;
	private Video mVideo = null;
	private BFStream mStream = null;
	private String mDataSource = "servicetype=1&uid=4995606&fid=D754D209A442A6787962AB1552FF9412";
	private String mToken = null;
	private String mCurrentDefinition = null; //预制清晰度，如果使用默认清晰度，可以直接为null
	private String mLocalPlayUrl = null; //MediaPlayer播放使用的地址，一般为http://127.0.0.1:port.
	private MediaPlayer mPlayer = null;
	private VideoService mVideoService = null;
	private ServiceConnection mConnection = null;
	
	private static final int MSG_VIDEO_SERVICE_READY = 1000;
	private static final int MSG_VIDEO_INFO_PREPARED = 1001;
	private static final int MSG_READY_TO_PLAY = 1002;
	
	private Handler mHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			//如果需要在进入Activity的时候直接播放，可以在这里进行start操作
			case MSG_VIDEO_SERVICE_READY:
				
				break;
			case MSG_VIDEO_INFO_PREPARED:
				mStream = mVideoService.getStream(mVideo, mCurrentDefinition,
						StreamType.STREAM_FOR_PLAY, StreamStoreMode.MEDIA_STREAM_STORE_MEMORY);
				mCurrentDefinition = mStream.getCurrentDefinition();
				mVideoService.startPlay(mStream); // 开启"播放流"服务，等待EVENT_PLAY_STARTED的回调信息
				break;
			case MSG_READY_TO_PLAY:
				try {
					if (!mIsViewPrepared){
						return false;
					}
					mPlayer = new MediaPlayer();
					mPlayer.setDisplay(holder);
					mPlayer.setDataSource(mLocalPlayUrl);
					mPlayer.setOnPreparedListener(new OnPreparedListener() {
						
						@Override
						public void onPrepared(MediaPlayer mp) {
							mp.start();
						}
					});
					mPlayer.prepareAsync();
					Toast.makeText(MediaCenterDemo.this, "建立MediaPlayer并开始播放，请耐心等待", 
							Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					Log.e("bfcloud", e.toString());
				}
				break;

			default:
				break;
			}
			return false;
		}
	});
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.media_center_demo);
		init();
	}

	private void init() {
		mConnection = new ServiceConnection() {
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mVideoService = ((VideoBinder)service).getService();
				mHandler.sendEmptyMessageDelayed(MSG_VIDEO_SERVICE_READY, 50);
			}
		};
		Intent intent = new Intent();
		intent.setClass(MediaCenterDemo.this, VideoService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		btStart = (Button) findViewById(R.id.start);
		btStop = (Button) findViewById(R.id.stop);
		mSurfaceView = (SurfaceView) findViewById(R.id.video_view);
		mSurfaceView.getHolder().addCallback(this);
		btStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stop();
				start();
			}
		});
		
		btStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stop();
			}
		});
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("bfcloud", "surfaceCreated");
		this.holder = holder;
		mIsViewPrepared = true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	
	private void start(){
		Toast.makeText(this, "开始建立本地播放流..", Toast.LENGTH_SHORT).show();
		//注册播放观察者
		mVideoService.registPlayListener(new PlayListener() {

			@Override
			public void onPlayError(BFStream stream, int error) {
				
			}

			@Override
			public void onPlayEvent(BFStream stream, int event) {
				switch (event) {
				case VideoManager.EVENT_PLAY_STARTED:
					mLocalPlayUrl = stream.getStreamUrl();
					mHandler.sendEmptyMessage(MSG_READY_TO_PLAY);
					break;

				default:
					break;
				}
			}
		});
		//创建video
		mVideoService.createVideo(mDataSource, mToken, new VideoListener() {
			
			@Override
			/**
			 * video获取之后，由成员变量接收，异步通知本地此消息
			 */
			public void onVideoPrepared(Video video) {
				mVideo = video;
				mHandler.sendEmptyMessage(MSG_VIDEO_INFO_PREPARED);
			}

			@Override
			public void onQueryError(Video video, int error) {
				mVideoService.destoryVideo(video);
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		stop();
		super.onPause();
	}
	
	private void stop(){
		//停止和销毁本地播放流服务
		if (mStream != null){
			mVideoService.stopPlay(mStream);
			mStream = null;
		}
		//停止和销毁播放器
		if (mPlayer != null){
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		if (mVideo != null){
			mVideoService.destoryVideo(mVideo);
			mVideo = null;
		}
	}
}
