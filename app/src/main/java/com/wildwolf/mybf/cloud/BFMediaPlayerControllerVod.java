package com.wildwolf.mybf.cloud;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.wildwolf.mybf.R;

import bf.cloud.android.base.BFVRConst.ControlMode;
import bf.cloud.android.base.BFVRConst.RenderMode;
import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.BasePlayer.PLAYER_TYPE;
import bf.cloud.android.playutils.BasePlayer.STATE;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.PlayTaskType;
import bf.cloud.android.playutils.VodPlayer;

public class BFMediaPlayerControllerVod extends BFMediaPlayerControllerBase {
	private VodPlayer mVodPlayer = null;
	private RelativeLayout mPlayCompleteFrame = null;
	// 当前播放时间
	private TextView mControllerCurentPlayTime = null;
	// 总时长
	private TextView mControllerDuration = null;
	// 播放结束层的按钮
	private View btPlayCompleteFrameStart = null;
	// 开始暂停播放按钮
	private ImageButton mControllerPlayPause = null;
	// 提示语
	private TextView tvPlayCompleteFrameMessage = null;
	private StringBuilder mFormatBuilder = null;
	private Formatter mFormatter = null;
	private boolean mDragging = false;
	private SeekBar mControllerProgressBar = null;
	private static final int MSG_HIDE_DEFINITION_PANEL = 30008;
	private static final int MSG_SHOW_DEFINITION_PANEL = 30009;
	private static final int MEG_UPDATE_PROGRESS = 10000;
	protected final static int DELAY_TIME_LONG = 10 * 1000; // ms
	private DefinitionPanel mDefinitionPanel = null;
	private ArrayList<String> mDefinitions = null;
	private String mCurrentDefinition = null;
	private long mCurrnetPosition = -1;
	// 清晰度图标
	private TextView mControllerDefinition = null;
	private Handler mProgressHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			if (mVodPlayer.getState() == STATE.IDLE)
				return false;
			mProgressHandler.removeMessages(MEG_UPDATE_PROGRESS);
			mProgressHandler.sendEmptyMessageDelayed(MEG_UPDATE_PROGRESS, 1000);
			updateProgressBar();
			return false;
		}
	});

	public BFMediaPlayerControllerVod(Context context, boolean fullScreen) {
		super(context, fullScreen);
		initViews();
		attachPlayer(mVodPlayer);
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs) {
		super(context, attrs);
		initViews();
		attachPlayer(mVodPlayer);
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initViews();
		attachPlayer(mVodPlayer);
	}

	@Override
	public void onError(int errorCode) {
		if (mVodPlayer.getState() != STATE.IDLE)
			mCurrnetPosition = mVodPlayer.getCurrentPosition();
		super.onError(errorCode);
		// 子类处理个别错误码
	}

	@Override
	public void onEvent(int eventCode) {
		switch (eventCode) {
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_ENDED:
			showPlayCompleteFrame(true);
			updateButtonUI();
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_START:
			showPlayCompleteFrame(false);
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_STARTED:
			if (mVodPlayer == null) {
				Log.d(TAG, "mVodPlayer is invailid");
				return;
			}
			mProgressHandler.sendEmptyMessage(MEG_UPDATE_PROGRESS);
			mDefinitions = mVodPlayer.getAllDefinitions();
			mCurrentDefinition = mVodPlayer.getCurrentDefinition();
			if (mControllerDefinition != null)
				mControllerDefinition.setText(mCurrentDefinition);
			if (mControllerVideoTitle != null)
				mControllerVideoTitle.setText(mVodPlayer.getVideoName());

			updateButtonUI();
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_PAUSE:
			updateButtonUI();
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_RESUME:
			updateButtonUI();
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
		mVodPlayer = (VodPlayer) mLayoutInflater.inflate(
				R.layout.vp_video_vod_player, this, false);
		mVodPlayer.setBackgroundColor(Color.BLACK);
		addView(mVodPlayer, layoutParams);
		// 播放结束层
		mPlayCompleteFrame = (RelativeLayout) mLayoutInflater.inflate(
				R.layout.vp_play_complete, this, false);
		initPlayCompleteFrame();
		addView(mPlayCompleteFrame, layoutParams);
		// 公共层
		super.initViews();
	}

	private String stringForTime(long timeMs) {
		long totalSeconds = timeMs / 1000;

		long seconds = totalSeconds % 60;
		long minutes = (totalSeconds / 60) % 60;
		long hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	public void rebuildPlayerControllerFrame() {
		if (mPlayerController != null) {
			removeView(mPlayerController);
			mPlayerController = null;
		}

		if (mIsFullScreen) {
			mPlayerController = (FrameLayout) mLayoutInflater.inflate(
					R.layout.vp_media_controller_landscape_vod, this, false);
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
			if (mVodPlayer != null)
				mControllerVideoTitle.setText(mVodPlayer.getVideoName());
			// init bottom section
			mControllerDefinition = (TextView) mPlayerController
					.findViewById(R.id.definition);
			mControllerDefinition.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showDefinitionPanel(true);
				}
			});
			if (mCurrentDefinition != null
					&& mControllerDefinition.length() != 0)
				mControllerDefinition.setText(mCurrentDefinition);
			mChangeFullSightRenderMode = (ImageView) mPlayerController
					.findViewById(R.id.change_fullsight_render_type);
			mChangeFullSightControlMode = (ImageView) mPlayerController
					.findViewById(R.id.change_fullsight_control_type);
			if (mVodPlayer != null
					&& mVodPlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT) {// 初始化图标,注册对应的点击事件
				if (mVodPlayer.getFullSightRenderMode() == RenderMode.FULLVIEW)
					mChangeFullSightRenderMode
							.setBackgroundResource(R.drawable.full_sight2);
				else
					mChangeFullSightRenderMode
							.setBackgroundResource(R.drawable.full_sight3);
				
				if (mVodPlayer.getFullSightControlMode() == ControlMode.TOUCH)
					mChangeFullSightControlMode
							.setBackgroundResource(R.drawable.full_sight1);
				else
					mChangeFullSightControlMode
							.setBackgroundResource(R.drawable.full_sight0);
				
				mChangeFullSightRenderMode.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								if (mVodPlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT){
									if (mVodPlayer.getFullSightRenderMode() == RenderMode.FULLVIEW) {
										changeFullSightMode(RenderMode.FULLVIEW3D);
										mChangeFullSightRenderMode
												.setBackgroundResource(R.drawable.full_sight3);
									} else if (mVodPlayer.getFullSightRenderMode() == RenderMode.FULLVIEW3D) {
										changeFullSightMode(RenderMode.FULLVIEW);
										mChangeFullSightRenderMode
												.setBackgroundResource(R.drawable.full_sight2);
									}
								}
								mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
							}
				});
				mChangeFullSightControlMode.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (mVodPlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT){
							if (mVodPlayer.getFullSightControlMode() == ControlMode.TOUCH) {
								changeFullSightMode(ControlMode.GYROSCOPE);
								mChangeFullSightControlMode
										.setBackgroundResource(R.drawable.full_sight0);
							} else if (mVodPlayer.getFullSightControlMode() == ControlMode.GYROSCOPE) {
								changeFullSightMode(ControlMode.TOUCH);
								mChangeFullSightControlMode
										.setBackgroundResource(R.drawable.full_sight1);
							}
						}
						mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
					}
				});
			}
			
		} else {
			mPlayerController = (FrameLayout) mLayoutInflater.inflate(
					R.layout.vp_media_controller_potrait_vod, this, false);
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

		// init bottom section common
		mControllerPlayPause = (ImageButton) mPlayerController
				.findViewById(R.id.pause_play_button);
		mControllerPlayPause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mVodPlayer == null)
					return;
				STATE playerState = mVodPlayer.getState();
				if (playerState == STATE.PLAYING)
					mVodPlayer.pause();
				else
					mVodPlayer.resume();
			}
		});
		updateButtonUI();
		mControllerCurentPlayTime = (TextView) mPlayerController
				.findViewById(R.id.time_current);
		mControllerDuration = (TextView) mPlayerController
				.findViewById(R.id.time_duration);
		mControllerProgressBar = (SeekBar) mPlayerController
				.findViewById(R.id.mediacontroller_progress);
		mControllerProgressBar.setVisibility(View.VISIBLE);
		if (mControllerProgressBar != null) {
			if (mControllerProgressBar instanceof SeekBar) {
				SeekBar seeker = (SeekBar) mControllerProgressBar;
				seeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						Log.d(TAG, "onStartTrackingTouch");
						mDragging = true;
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						Log.d(TAG, "onStopTrackingTouch");
						long duration = mVodPlayer.getDuration();
						long newposition = (duration * seekBar.getProgress())
								/ seekBar.getMax();
						mVodPlayer.seekTo((int) newposition);
						mDragging = false;
						mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
					}
				});
			}
			mControllerProgressBar.setMax(1000);
		}

		mPlayerController.setVisibility(View.GONE);
		addView(mPlayerController, mLayoutParams);
		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
	}

	private void showDefinitionPanel(boolean flag) {
		Log.d(TAG, "showDefinitionPanel");
		if (mDefinitionPanel == null) {
			mDefinitionPanel = new DefinitionPanel(mContext);
			mDefinitionPanel
					.registOnClickListener(new DefinitionPanel.OnDefinitionClickListener() {

						@Override
						public void onItemClick(int position) {
							mDefinitionPanel.dismiss();
							mMessageHandler
									.sendEmptyMessage(MSG_HIDE_CONTROLLER);
							if (mDefinitions != null)
								mVodPlayer.setDefinition(mDefinitions
										.get(position));
						}
					});
		}
		mDefinitionPanel.setDatas(mDefinitions, mCurrentDefinition);
		mDefinitionPanel.showAsPullUp(mControllerDefinition);
		mMessageHandler.sendEmptyMessage(MSG_SHOW_DEFINITION_PANEL);
	}

	private void initPlayCompleteFrame() {
		btPlayCompleteFrameStart = mPlayCompleteFrame
				.findViewById(R.id.play_button);
		btPlayCompleteFrameStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mVodPlayer != null) {
					mVodPlayer.stop();
					mVodPlayer.start();
				}
			}
		});
		tvPlayCompleteFrameMessage = (TextView) mPlayCompleteFrame
				.findViewById(R.id.message_textview);
		tvPlayCompleteFrameMessage.setText("播放已结束:)");
		mPlayCompleteFrame.setVisibility(View.INVISIBLE);
	}

	@Override
	public BasePlayer getPlayer() {
		return mVodPlayer;
	}

	@Override
	protected void showErrorFrame(int errorCode) {
		mPlayErrorManager.setErrorCode(errorCode);
		TextView tipsTv = (TextView) mErrorFrame
				.findViewById(R.id.error_message_textview);
		String tips = mPlayErrorManager.getErrorShowTips(PlayTaskType.VOD);
		tipsTv.setText(tips);
		TextView codeTv = (TextView) mErrorFrame
				.findViewById(R.id.error_code_textview);
		String codeText = "错误代码：" + errorCode;
		codeTv.setText(codeText);
		mErrorFrame.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onClickPlayButton() {
		if (mVodPlayer != null) {
			mVodPlayer.stop();
			// 如果希望无论在什么网络下都播放视频，就设置这个标志
			mVodPlayer.setForceStartFlag(true);
			mVodPlayer.start((int) mCurrnetPosition);
		}
	}

	private void showPlayCompleteFrame(boolean flag) {
		if (flag) {
			if (mPlayCompleteFrame != null)
				mPlayCompleteFrame.setVisibility(View.VISIBLE);
		} else {
			if (mPlayCompleteFrame != null)
				mPlayCompleteFrame.setVisibility(View.INVISIBLE);
		}
	}

	private void updateProgressBar() {
		if (mVodPlayer == null || mDragging || mIsBuffering) {
			return;
		}
		long duration = mVodPlayer.getDuration();
		long position = mVodPlayer.getCurrentPosition();
		if (duration != 0 && position > duration) {
			position = duration;
		}
		if (mControllerProgressBar != null) {
			if (duration > 0 && position > 0) {
				long pos = mControllerProgressBar.getMax() * position
						/ duration;
				int currentProgree = mControllerProgressBar.getProgress();
				if (Math.abs(currentProgree - pos) > 100){//防止进度条跳跃
					return;
				}
				mControllerProgressBar.setProgress((int) pos);
				int secPro = mControllerProgressBar.getMax() / 100 * mVodPlayer.getBufferPercentage();
				mControllerProgressBar.setSecondaryProgress(secPro);
			}
		}

		if (mControllerCurentPlayTime != null)
			mControllerCurentPlayTime.setText(stringForTime(position));
		if (mControllerDuration != null) {
			mControllerDuration.setText(stringForTime(duration));
		}
	}

	private void updateButtonUI() {
		if (mVodPlayer == null)
			return;
		STATE state = mVodPlayer.getState();
		if (state == STATE.PLAYING)
			mControllerPlayPause.setBackgroundResource(R.drawable.vp_pause);
		else
			mControllerPlayPause.setBackgroundResource(R.drawable.vp_play);

	}

	@Override
	protected void doMoveLeft() {
		if (moveDirection == MOVE_NONE || moveDistanceX < 0
				|| mVodPlayer == null) {
			Log.d(TAG, "invailid params during dealing doMoveLeft");
			return;
		}
		STATE state = mVodPlayer.getState();
		if (state == STATE.PLAYING || state == STATE.PAUSED) {
			int newPosition = (int) (mVodPlayer.getCurrentPosition() - moveDistanceX
					* mVodPlayer.getDuration() / (mScreenWidth * DIVISION));
			mVodPlayer.seekTo(newPosition);
		}
	}

	@Override
	protected void doMoveRight() {
		if (moveDirection == MOVE_NONE || moveDistanceX < 0
				|| mVodPlayer == null) {
			Log.d(TAG, "invailid params during dealing doMoveLeft");
			return;
		}
		STATE state = mVodPlayer.getState();
		if (state == STATE.PLAYING || state == STATE.PAUSED) {
			int newPosition = (int) (mVodPlayer.getCurrentPosition() + moveDistanceX
					* mVodPlayer.getDuration() / (mScreenWidth * DIVISION));
			mVodPlayer.seekTo(newPosition);
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		int what = msg.what;
		switch (what) {
		case MSG_HIDE_DEFINITION_PANEL:
			mMessageHandler.removeMessages(MSG_HIDE_DEFINITION_PANEL);
			mMessageHandler.removeMessages(MSG_SHOW_DEFINITION_PANEL);
			mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
			mDefinitionPanel.dismiss();
			return true;
		case MSG_SHOW_DEFINITION_PANEL:
			mMessageHandler.removeMessages(MSG_HIDE_DEFINITION_PANEL);
			mMessageHandler.removeMessages(MSG_SHOW_DEFINITION_PANEL);
			mMessageHandler.removeMessages(MSG_SHOW_CONTROLLER);
			mMessageHandler.removeMessages(MSG_HIDE_CONTROLLER);
			showController(true);
			mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_DEFINITION_PANEL,
					DELAY_TIME_LONG);
			return true;
		case MSG_NETWORK_CHANGED:
			if (mVodPlayer.getState() != STATE.IDLE)
				mCurrnetPosition = mVodPlayer.getCurrentPosition();
			break;
		case MSG_CHANGE_DECODE_MODE_FROM_EXO_TO_SOFT:
			mCurrnetPosition = mVodPlayer.getCurrentPosition();
			mVodPlayer.stop();
			mVodPlayer.setForceStartFlag(true);
			mVodPlayer.setDecodeMode(DecodeMode.SOFT);
			mVodPlayer.start((int) mCurrnetPosition);
			return true;
		}
		return super.handleMessage(msg);
	}

	@Override
	public void changeToPortrait() {
		if (mContext == null)
			return;
		// 隐藏清晰度图标
		if (mDefinitionPanel != null)
			mDefinitionPanel.dismiss();
		super.changeToPortrait();
	}

	@Override
	public void changeToLandscape() {
		if (mContext == null)
			return;
		if (mDefinitionPanel != null)
			mDefinitionPanel.dismiss();
		super.changeToLandscape();
	}
}
