package com.wildwolf.mybf.cloud.demo;

import android.app.Activity;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

import com.wildwolf.mybf.cloud.BFMediaPlayerControllerVod;

import bf.cloud.android.modules.p2p.MediaCenter.StreamDataMode;
import bf.cloud.android.playutils.BasePlayer.PLAYER_TYPE;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.VodPlayer;

public class VodLocalPlayDemo extends Activity {
	private BFMediaPlayerControllerVod mMediaController = null;
	private VodPlayer mVodPlayer = null;
	private String mUrl = null;
	private String mToken = null;
	private String mDefinition = null;
	private Handler mHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case START_PLAY:
				mVodPlayer.stop();
				if (mHistory > 0)
					mVodPlayer.start((int) mHistory);
				else{
					mVodPlayer.start();
				}
				break;

			default:
				break;
			}
			return false;
		}
	});
	private long mHistory = -1;
	private static final int START_PLAY = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mMediaController = new BFMediaPlayerControllerVod(this, true);
		mVodPlayer  = (VodPlayer) mMediaController.getPlayer();
//		mVodPlayer.setPlayerType(PLAYER_TYPE.FULL_SIGHT);
		//mVodPlayer.setStreamDataMode(StreamDataMode.STREAM_MP4_MODE);
		//mVodPlayer.setDecodeMode(DecodeMode.MEDIAPLYAER);
		mMediaController.rebuildPlayerControllerFrame();
		mMediaController.enableBackToPortrait(false);
		mMediaController.setAutoChangeScreen(false);
		setContentView(mMediaController);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Bundle bundle = this.getIntent().getExtras();
		mUrl = bundle.getString("url");
		mToken = bundle.getString("token");
		mDefinition = bundle.getString("definition");
		init();
	}

	private void init() {
		mVodPlayer.setDataSource(mUrl, mToken);
		mVodPlayer.setDefinition(mDefinition);
		mVodPlayer.setIsDownload(true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onStart() {
		mHandler.sendEmptyMessageDelayed(START_PLAY, 300);
		super.onStart();
	}
	
	@Override
	protected void onPause() {
		mHistory  = mVodPlayer.getCurrentPosition();
		mVodPlayer.stop();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		mVodPlayer.release();
		try {
			mMediaController.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}
}
