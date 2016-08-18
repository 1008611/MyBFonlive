package com.wildwolf.mybf.cloud;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.wildwolf.mybf.R;

import bf.cloud.android.base.BFVRConst.ControlMode;
import bf.cloud.android.base.BFVRConst.RenderMode;
import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.BasePlayer.PLAYER_TYPE;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.LivePlayer;
import bf.cloud.android.playutils.PlayTaskType;


public class BFMediaPlayerControllerLive extends BFMediaPlayerControllerBase {
	private LivePlayer mLivePlayer = null;

	public BFMediaPlayerControllerLive(Context context, boolean fullScreen) {
		super(context, fullScreen);
		initViews();
		attachPlayer(mLivePlayer);
	}

	public BFMediaPlayerControllerLive(Context context, AttributeSet attrs) {
		super(context, attrs);
		initViews();
		attachPlayer(mLivePlayer);
	}

	public BFMediaPlayerControllerLive(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initViews();
		attachPlayer(mLivePlayer);
	}

	@Override
	public void onError(int errorCode) {
		super.onError(errorCode);
		// 子类处理个别错误码
	}

	@Override
	public void onEvent(int eventCode) {
		switch (eventCode) {
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_STARTED:
			if (mLivePlayer == null) {
				Log.d(TAG, "mLivePlayer is invailid");
				return;
			}
			if (mControllerVideoTitle != null)
				mControllerVideoTitle.setText(mLivePlayer.getVideoName());
			break;
		default:
			break;
		}
		super.onEvent(eventCode);
		// 子类处理个别事件
	}

	@Override
	protected void initViews() {
		removeAllViews();
		LayoutParams layoutParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		// 视频层在最下层
		mLivePlayer = (LivePlayer) mLayoutInflater.inflate(
				R.layout.vp_video_live_player, this, false);
		mLivePlayer.setBackgroundColor(Color.BLACK);
		addView(mLivePlayer, layoutParams);
		// 公共层
		super.initViews();
	}

	public void rebuildPlayerControllerFrame() {
		if (mPlayerController != null) {
			removeView(mPlayerController);
			mPlayerController = null;
		}
		if (mIsFullScreen) {
			mPlayerController = (FrameLayout) mLayoutInflater.inflate(
					R.layout.vp_media_controller_landscape_live, this, false);
			// init head section
			mControllerBack = (ImageView) mPlayerController
					.findViewById(R.id.back_button);
			mControllerBack.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mEnableBackToPortrait)
						backToPortrait();
					else{
						new Thread(){
							@Override
							public void run() {
								try {
									Instrumentation ins = new Instrumentation();
									ins.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
								} catch (Exception e) {
									
								}
							};
						}.start();
					}
				}
			});
			mControllerVideoTitle = (TextView) mPlayerController
					.findViewById(R.id.videoTitle);
			mChangeFullSightRenderMode = (ImageView) mPlayerController
					.findViewById(R.id.change_fullsight_render_type);
			mChangeFullSightControlMode = (ImageView) mPlayerController
					.findViewById(R.id.change_fullsight_control_type);
			if (mLivePlayer != null
					&& mLivePlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT) {
				if (mLivePlayer.getFullSightRenderMode() == RenderMode.FULLVIEW)
					mChangeFullSightRenderMode
							.setBackgroundResource(R.drawable.full_sight2);
				else
					mChangeFullSightRenderMode
							.setBackgroundResource(R.drawable.full_sight3);

				if (mLivePlayer.getFullSightControlMode() == ControlMode.TOUCH)
					mChangeFullSightControlMode
							.setBackgroundResource(R.drawable.full_sight1);
				else
					mChangeFullSightControlMode
							.setBackgroundResource(R.drawable.full_sight0);

				mChangeFullSightRenderMode
						.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								if (mLivePlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT){
									if (mLivePlayer.getFullSightRenderMode() == RenderMode.FULLVIEW) {
										changeFullSightMode(RenderMode.FULLVIEW3D);
										mChangeFullSightRenderMode
												.setBackgroundResource(R.drawable.full_sight3);
									} else if (mLivePlayer.getFullSightRenderMode() == RenderMode.FULLVIEW3D) {
										changeFullSightMode(RenderMode.FULLVIEW);
										mChangeFullSightRenderMode
												.setBackgroundResource(R.drawable.full_sight2);
									}
								}
								mMessageHandler
										.sendEmptyMessage(MSG_SHOW_CONTROLLER);
							}
						});
				mChangeFullSightControlMode
						.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								if (mLivePlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT){
									if (mLivePlayer.getFullSightControlMode() == ControlMode.TOUCH) {
										changeFullSightMode(ControlMode.GYROSCOPE);
										mChangeFullSightControlMode
												.setBackgroundResource(R.drawable.full_sight0);
									} else if (mLivePlayer.getFullSightControlMode() == ControlMode.GYROSCOPE) {
										changeFullSightMode(ControlMode.TOUCH);
										mChangeFullSightControlMode
												.setBackgroundResource(R.drawable.full_sight1);
									}
								}
								mMessageHandler
										.sendEmptyMessage(MSG_SHOW_CONTROLLER);
							}
						});
			}

		} else {
			mPlayerController = (FrameLayout) mLayoutInflater.inflate(
					R.layout.vp_media_controller_potrait_live, this, false);
			// init head section
			mControllerVideoTitle = (TextView) mPlayerController
					.findViewById(R.id.videoTitle);
			// init bottom section
			mControllerChangeScreen = (Button) mPlayerController
					.findViewById(R.id.full_screen);
			mControllerChangeScreen.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					changeToLandscape();
					mIsFullScreen = true;
				}
			});
		}
		mPlayerController.setVisibility(View.GONE);
		addView(mPlayerController, mLayoutParams);
	}

	@Override
	public BasePlayer getPlayer() {
		return mLivePlayer;
	}

	@Override
	protected void showErrorFrame(int errorCode) {
		mPlayErrorManager.setErrorCode(errorCode);
		TextView tipsTv = (TextView) mErrorFrame
				.findViewById(R.id.error_message_textview);
		String tips = mPlayErrorManager.getErrorShowTips(PlayTaskType.LIVE);
		tipsTv.setText(tips);
		TextView codeTv = (TextView) mErrorFrame
				.findViewById(R.id.error_code_textview);
		String codeText = "错误代码：" + errorCode;
		codeTv.setText(codeText);
		mErrorFrame.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onClickPlayButton() {
		if (mLivePlayer != null) {
			mLivePlayer.stop();
			// 如果希望无论在什么网络下都播放视频，就设置这个标志
			mLivePlayer.setForceStartFlag(true);
			mLivePlayer.start();
		}
	}

	@Override
	protected void doMoveLeft() {
	}

	@Override
	protected void doMoveRight() {
	}

	@Override
	public boolean handleMessage(Message msg) {
		int what = msg.what;
		switch (what) {
		case MSG_CHANGE_DECODE_MODE_FROM_EXO_TO_SOFT:
			mLivePlayer.stop();
			mLivePlayer.setDecodeMode(DecodeMode.SOFT);
			mLivePlayer.start();
			break;

		default:
			break;
		}
		return super.handleMessage(msg);
	}

	@Override
	public void changeToPortrait() {
		if (mContext == null)
			return;
		super.changeToPortrait();
	}

	@Override
	public void changeToLandscape() {
		if (mContext == null)
			return;
		super.changeToLandscape();
	}
}
