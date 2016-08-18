package com.wildwolf.mybf.cloud.demo;

import bf.cloud.android.mxlbarrage.MxlBarrage;
import bf.cloud.android.mxlbarrage.MxlBarrage.BarrageListener;
import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.LivePlayer;
import bf.cloud.android.playutils.BasePlayer.RATIO_TYPE;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.wildwolf.mybf.R;
import com.wildwolf.mybf.cloud.BFMediaPlayerControllerLive;

public class LiveDemo extends Activity {
	private final String TAG = LiveDemo.class.getSimpleName();
	private LivePlayer mLivePlayer = null;
	private BFMediaPlayerControllerLive mMediaController = null;
	private String mUrl = "servicetype=2&uid=10279577&fid=41BA62731D5855EFF05C05852514EA098FC7F7BE";
	private String mGcid = "41BA62731D5855EFF05C05852514EA098FC7F7BE";
	private EditText mInputUrl = null;
	private EditText mInputToken = null;
	private MxlBarrage mBarrage = null;

	private Toast mNotice = null;
	private Handler handler;
	private boolean mP2pShowFlag = false;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_live);
		init();
	}

	private void init() {
		mMediaController = (BFMediaPlayerControllerLive) findViewById(R.id.media_controller_live);
		mInputUrl = (EditText) findViewById(R.id.play_url);
		mInputToken = (EditText) findViewById(R.id.play_token);
		mLivePlayer = (LivePlayer) mMediaController.getPlayer();
		mLivePlayer.setDataSource(mUrl);
		mBarrage = new MxlBarrage(mGcid);
		mBarrage.registBarrageListener(new BarrageListener() {
			
			@Override
			public void onMessage(String channelId, String msg) {
				if (mMediaController != null)
					mMediaController.addDanmaku(false, msg);
			}
		});
	}
	
	@Override
	protected void onResume() {
		if (mBarrage != null && !mBarrage.getIsWorking())
			mBarrage.start();
		super.onResume();
	}
	
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.change_decode_mode: {
			String[] items = { "硬解", "软解", "系统原生解码(MediaPlayer)"};
			int checkedItem = -1;
			Log.d(TAG,
					"mVodPlayer.getDecodeMode():" + mLivePlayer.getDecodeMode());
			if (mLivePlayer.getDecodeMode() == DecodeMode.HARD) {
				checkedItem = 0;
			} else if (mLivePlayer.getDecodeMode() == DecodeMode.SOFT){
				checkedItem = 1;
			} else 
				checkedItem = 2;
			new AlertDialog.Builder(this)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									mLivePlayer.stop();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mLivePlayer
												.setDecodeMode(DecodeMode.HARD);
									} else if (position == 1) {
										mLivePlayer
												.setDecodeMode(DecodeMode.SOFT);
									} else{
										mLivePlayer.setDecodeMode(DecodeMode.MEDIAPLYAER);
									}
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		}
		case R.id.change_live_delay_mode: {
			String[] items = { "普通直播", "低延时直播" };
			int checkedItem = -1;
			if (mLivePlayer.getLowLatency()) {
				checkedItem = 1;
			} else {
				checkedItem = 0;
			}
			new AlertDialog.Builder(this)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									mLivePlayer.stop();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mLivePlayer.setLowLatency(false);
									} else if (position == 1) {
										mLivePlayer.setLowLatency(true);
									}
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		}
		case R.id.start: {
			Log.d(TAG, "Start onClick");
			mLivePlayer.stop();
			String inputUrl = mInputUrl.getText().toString();
			String inputToken = mInputToken.getText().toString();
			if (inputUrl != null && inputUrl.length() != 0)
				mLivePlayer.setDataSource(inputUrl, inputToken);
			else {
				mLivePlayer.setDataSource(mUrl, inputToken);
				if (mNotice != null)
					mNotice.cancel();
				mNotice = Toast.makeText(this, "开始播放默认视频", Toast.LENGTH_SHORT);
				mNotice.show();
			}
			mLivePlayer.start();
			break;
		}
		case R.id.stop:
			mLivePlayer.stop();
			break;
		case R.id.inc_volume:
			mLivePlayer.incVolume();
			break;
		case R.id.dec_volume:
			mLivePlayer.decVolume();
			break;
		case R.id.get_current_volume: {
			int value = mLivePlayer.getCurrentVolume();
			Toast.makeText(LiveDemo.this, "" + value, Toast.LENGTH_SHORT)
					.show();
			break;
		}
		case R.id.get_max_volume: {
			int value = mLivePlayer.getMaxVolume();
			Toast.makeText(LiveDemo.this, "" + value, Toast.LENGTH_SHORT)
					.show();
			break;
		}
		case R.id.auto_screen: {
			String[] items = { "是", "否" };
			int checkedItem = -1;
			if (mMediaController.getAutoChangeScreen()) {
				checkedItem = 0;
			} else {
				checkedItem = 1;
			}
			new AlertDialog.Builder(this)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mMediaController
												.setAutoChangeScreen(true);
									} else if (position == 1) {
										mMediaController
												.setAutoChangeScreen(false);
									}
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		}
		case R.id.changed_ratio:{
			String[] items = { "16:9", "4:3", "原视频输出"};
			int checkedItem = 0;
			if (mLivePlayer.getVideoAspectRatio() == RATIO_TYPE.TYPE_16_9) {
				checkedItem = 0;
			} else if (mLivePlayer.getVideoAspectRatio() == RATIO_TYPE.TYPE_4_3){
				checkedItem = 1;
			} else if (mLivePlayer.getVideoAspectRatio() == RATIO_TYPE.TYPE_ORIGENAL){
				checkedItem = 2;
			}
			new AlertDialog.Builder(this)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mLivePlayer.setVideoAspectRatio(RATIO_TYPE.TYPE_16_9);
									} else if (position == 1) {
										mLivePlayer.setVideoAspectRatio(RATIO_TYPE.TYPE_4_3);
									} else if (position == 2) {
										mLivePlayer.setVideoAspectRatio(RATIO_TYPE.TYPE_ORIGENAL);
									}
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		}
		case R.id.add_danmaku:{
			mBarrage.sendMsg("研发测试弹幕!@#$%^&*()_<>?:\"{}");
			mMediaController.addDanmaku(false, "发送测试弹幕信息");
			break;
		}
		case R.id.p2p_data_show:
			if (mP2pShowFlag)
				mP2pShowFlag = false;
			else
				mP2pShowFlag = true;
			mMediaController.showP2pData(mP2pShowFlag);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		if (mBarrage != null && mBarrage.getIsWorking())
			mBarrage.stop();
		mLivePlayer.stop();
		super.onPause();
	}

	@Override
	protected void onStart() {
		// mLivePlayer.start();
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		mLivePlayer.stop();
		mLivePlayer.release();
		try {
			mMediaController.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}
}
