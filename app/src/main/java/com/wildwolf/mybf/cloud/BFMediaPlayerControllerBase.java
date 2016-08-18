package com.wildwolf.mybf.cloud;

import java.util.HashMap;

import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wildwolf.mybf.R;

import bf.cloud.android.base.BFVRConst.ControlMode;
import bf.cloud.android.base.BFVRConst.RenderMode;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.BasePlayer.PLAYER_TYPE;
import bf.cloud.android.playutils.BasePlayer.PlayErrorListener;
import bf.cloud.android.playutils.BasePlayer.PlayEventListener;
import bf.cloud.android.playutils.VideoManager;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.utils.Utils;

/**
 * @author wang Note: You should change your project to UTF8
 */
public abstract class BFMediaPlayerControllerBase extends FrameLayout implements PlayErrorListener,
		PlayEventListener, View.OnClickListener, View.OnTouchListener, Handler.Callback {
	protected final String TAG = BFMediaPlayerControllerBase.class.getSimpleName();
	protected final static int DIVISION = 4;
	private final static int DELAY_TIME_STANDARD = 5000; // ms
	private final static int DELAY_TIME_SHORT = 3000; // ms
	protected LayoutInflater mLayoutInflater = null;

	protected Context mContext = null;
	protected FrameLayout mPlaceHoler = null;
	protected RelativeLayout mErrorFrame = null;
	protected FrameLayout mStatusController = null;
	protected FrameLayout mDanmakuFrame = null;
	protected IDanmakuView mDanmakuView = null;
	protected P2pDataFrame mP2pDataFrame = null;
	protected BaseDanmakuParser mParser = null;
	protected DanmakuContext mDanmakuContext = null;
	private ProgressBar mProgressBarBuffering = null;
	private ImageView mImageViewIcon = null;
	private LinearLayout mBrightnessLayer = null;
	private TextView mBrightnessPercent = null;
	private LinearLayout mVolumeLayer = null;
	private TextView mVolumePercent = null;
	private boolean mIsControllerVisible = false;
	private EventHandler mEventHandler = new EventHandler();
	private ErrorHandler mErrorHandler = new ErrorHandler();
	protected PlayErrorManager mPlayErrorManager = null;
	protected FrameLayout mPlayerController = null;
	protected ImageView mControllerBack = null;
	protected TextView mControllerVideoTitle = null;
	// 切换屏幕
	protected Button mControllerChangeScreen = null;
	// 是否允许按返回键直接转到非全屏播放
	protected boolean mEnableBackToPortrait = true;
	// 全屏标志
	protected boolean mIsFullScreen = false;
	// 自适应屏幕
	private boolean mIsAutoScreen = true;
	// 屏幕旋转观察者
	protected PlayerOrientationMessageListener mPlayerOrientationMessageListener = null;
	// 切换全景渲染类型
	protected ImageView mChangeFullSightRenderMode = null;
	// 切换全景控制类型
	protected ImageView mChangeFullSightControlMode = null;
	protected int mScreenWidth = -1;
	protected int mScreenHeight = -1;
	protected int mDisplayWidth = -1;
	protected int mDisplayHeight = -1;
	protected int mVideoFrameOrigenalWidth = -1;
	protected int mVideoFrameOrigenalHeight = -1;
	private BasePlayer mPlayer = null;
	protected boolean mIsBuffering = false;

	protected static final int MSG_SHOW_CONTROLLER = 20000;
	protected static final int MSG_HIDE_CONTROLLER = 20001;
	protected static final int MSG_SHOW_BRIGHTNESS = 20002;
	protected static final int MSG_HIDE_BRIGHTNESS = 20003;
	protected static final int MSG_SHOW_VOLUME = 20004;
	protected static final int MSG_HIDE_VOLUME = 20005;
	protected static final int MSG_CHANGE_SCREEN_PORTRAIT = 20006;
	protected static final int MSG_CHANGE_SCREEN_LANDSCAPE = 20007;
	protected static final int MSG_NETWORK_CHANGED = 20008;
	protected static final int MSG_ADJUST_ORIENTATION = 5;
	protected static final int MSG_CHANGE_DECODE_MODE_FROM_EXO_TO_SOFT = 20009;

	protected Handler mMessageHandler = null;
	private BFYNetworkReceiver mNetworkReceiver = null;
	protected LayoutParams mLayoutParams = null;

	public BFMediaPlayerControllerBase(Context context, boolean fullScreen) {
		super(context);
		mIsFullScreen = fullScreen;
		mContext = context;
		init();
	}

	protected void showController(boolean flag) {
		if (mPlayerController == null)
			return;

		if (flag) {
			mPlayerController.setVisibility(View.VISIBLE);
			mIsControllerVisible = true;
		} else {
			mPlayerController.setVisibility(View.INVISIBLE);
			mIsControllerVisible = false;
		}
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	protected abstract void showErrorFrame(int errorCode);

	protected abstract void onClickPlayButton();

	public abstract void rebuildPlayerControllerFrame();

	protected abstract void doMoveLeft();

	protected abstract void doMoveRight();

	protected abstract BasePlayer getPlayer();

	private void init() {
		if (mContext == null)
			throw new NullPointerException("context is invarilid");
		mMessageHandler = new MyHandler(this);
		setOnTouchListener(this);
		// 注册网络监听器
		mNetworkReceiver = BFYNetworkReceiver.getInstance(mContext);
		mNetworkReceiver.registNetStateChangedListener(new BFYNetworkReceiver.NetStateChangedListener() {

			@Override
			public void onChanged(int lastNetState, int CurrentNetState) {
				Message msg = new Message();
				msg.what = MSG_NETWORK_CHANGED;
				msg.arg1 = lastNetState;
				msg.arg2 = CurrentNetState;
				mMessageHandler.sendMessage(msg);
			}
		});
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mNetworkReceiver, filter);
		mPlayerOrientationMessageListener = new PlayerOrientationMessageListener(mContext, this);
		if (mIsAutoScreen)
			mPlayerOrientationMessageListener.start();
		getAllSize();

		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPlayErrorManager = new PlayErrorManager();
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
	}

	public void setAutoChangeScreen(boolean flag) {
		if (flag) {
			mPlayerOrientationMessageListener.start();
		} else {
			mPlayerOrientationMessageListener.stop();
		}
		mIsAutoScreen = flag;
	}

	public boolean getAutoChangeScreen() {
		return mIsAutoScreen;
	}

	@SuppressLint("NewApi")
	private void getAllSize() {
		if (mContext == null) {
			throw new NullPointerException("you should get the Context first");
		}
		WindowManager windowManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			windowManager.getDefaultDisplay().getRealMetrics(metrics);
		} else {
			windowManager.getDefaultDisplay().getMetrics(metrics);
		}
		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
		mCenterX = mScreenWidth / 2;
		Log.d(TAG, "mScreenWidth:" + mScreenWidth + "/mScreenHeight:" + mScreenHeight);
		mMinX = Utils.dip2px(mContext, mMinMovementDipX);
		mMinY = Utils.dip2px(mContext, mMinMovementDipY);

		int w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		int h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		measure(w, h);
		mVideoFrameOrigenalWidth = getWidth();
		mVideoFrameOrigenalHeight = getHeight();
		Log.d(TAG, "mVideoFrameOrigenalWidth:" + mVideoFrameOrigenalWidth);
	}

	/**
	 * 初始化common图层
	 */
	protected void initViews() {
		mLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		mLayoutParams.gravity = Gravity.CENTER;

		// 图标层
		mStatusController = (FrameLayout) mLayoutInflater.inflate(R.layout.vp_status_controller,
				this, false);
		mStatusController.setVisibility(View.VISIBLE);
		initStatusFrame();
		addView(mStatusController, mLayoutParams);
		// 遮挡层
		mPlaceHoler = (FrameLayout) mLayoutInflater.inflate(R.layout.vp_place_holder, this, false);
		mPlaceHoler.setVisibility(View.INVISIBLE);
		addView(mPlaceHoler, mLayoutParams);

		RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		// 错误提示层
		mErrorFrame = (RelativeLayout) mLayoutInflater
				.inflate(R.layout.vp_error_frame, this, false);
		mErrorFrame.setVisibility(View.INVISIBLE);
		initErrorFrame();
		addView(mErrorFrame, layoutParams1);
		// 播放控制层
		rebuildPlayerControllerFrame();
		// 弹幕层
		mDanmakuFrame = (FrameLayout) mLayoutInflater.inflate(R.layout.danmaku_frame, this, false);
		mDanmakuView = (IDanmakuView) mDanmakuFrame.findViewById(R.id.danmaku_view);
		addView(mDanmakuFrame, mLayoutParams);
		initDanmaku();
		//p2p 数据展示层
		mP2pDataFrame = new P2pDataFrame(mContext);
		mP2pDataFrame.setVisibility(View.GONE);
		addView(mP2pDataFrame, mLayoutParams);
	}
	
	public void resumeDanmaku(){
		if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused())
			mDanmakuView.resume();
	}
	
	public void pauseDanmaku(){
		if (mDanmakuView != null && mDanmakuView.isPrepared())
			mDanmakuView.pause();
	}
	
	public void addDanmaku(boolean islive, String msg){
		BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        danmaku.text = msg;
        danmaku.padding = 5;
        danmaku.priority = 0;
        danmaku.isLive = islive;
        danmaku.time = mDanmakuView.getCurrentTime() + 1200;
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = Color.WHITE;
        danmaku.borderColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);
	}
	
	public void initDanmaku(){
		Log.d(TAG, "initDanmaku");
		mDanmakuContext = DanmakuContext.create();
		HashMap<Integer, Integer> maxLine = new HashMap<>();
		maxLine.put(BaseDanmaku.TYPE_SCROLL_RL, BFYConst.DEFAULT_DANMAKU_MAX_LINE_NUMBERS);
		// 重叠设置
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
		mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(1.2f)
                .setScaleTextSize(1.2f)
                .setMaximumLines(maxLine)
                .preventOverlapping(overlappingEnablePair);
		if (mDanmakuView != null){
			mDanmakuView.setCallback(new Callback() {
				
				@Override
				public void updateTimer(DanmakuTimer timer) {
				}
				
				@Override
				public void prepared() {
					mDanmakuView.start();
				}
				
				@Override
				public void drawingFinished() {
				}
				
				@Override
				public void danmakuShown(BaseDanmaku danmaku) {
					
				}
			});
			mParser = new BaseDanmakuParser() {
				
				@Override
				protected IDanmakus parse() {
					return new Danmakus();
				}
			};
			mDanmakuView.prepare(mParser, mDanmakuContext);
		}
	}

	/**
	 * you must destroy Danmaku
	 */
	public void destoryDanmaku(){
		if (mDanmakuView != null){
			if (mDanmakuView.isPaused())
				mDanmakuView.pause();
			mDanmakuView.release();
			mDanmakuView = null;
		}
	}
	
	private void initStatusFrame() {
		mProgressBarBuffering = (ProgressBar) mStatusController.findViewById(R.id.progressBar);
		mProgressBarBuffering.setVisibility(View.INVISIBLE);
		mImageViewIcon = (ImageView) mStatusController.findViewById(R.id.icon);
		mImageViewIcon.setVisibility(View.INVISIBLE);
		mBrightnessLayer = (LinearLayout) mStatusController.findViewById(R.id.brightness_layout);
		mBrightnessPercent = (TextView) mStatusController.findViewById(R.id.brightness_percent);
		mBrightnessPercent.setText("");
		showBrightnessLayer(false);
		mVolumeLayer = (LinearLayout) mStatusController.findViewById(R.id.volume_layout);
		mVolumePercent = (TextView) mStatusController.findViewById(R.id.volume_percent);
		showVolumeLayer(false);
	}

	private void initErrorFrame() {
		ImageButton ibPlay = (ImageButton) mErrorFrame.findViewById(R.id.error_play_button);
		ibPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onClickPlayButton();
			}
		});
		ImageView ibBack = (ImageView) mErrorFrame.findViewById(R.id.error_backButton);
		ibBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				((Activity) mContext).finish();
			}
		});
	}

	protected void attachPlayer(BasePlayer bp) {
		if (bp == null) {
			Log.d(TAG, "mBasePlayer is null");
			throw new NullPointerException("mBasePlayer is null");
		}
		mPlayer = bp;
		// attach Listeners
		bp.registPlayEventListener(this);
		bp.registPlayErrorListener(this);
		// attach functions

	}

	private void restoreOrigenalVideoFrameSize() {
		if (mVideoFrameOrigenalWidth <= 0 || mVideoFrameOrigenalHeight <= 0) {
			mVideoFrameOrigenalWidth = getWidth();
			mVideoFrameOrigenalHeight = getHeight();
		}
	}

	/**
	 * 竖屏
	 */
	public void changeToPortrait() {
		if (null == mContext)
			return;
		restoreOrigenalVideoFrameSize();
		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = mVideoFrameOrigenalHeight;
		params.width = mVideoFrameOrigenalWidth;
		Activity act = (Activity) mContext;
		act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mIsFullScreen = false;
		rebuildPlayerControllerFrame();
		// 显示全屏图标
		mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
	}

	/**
	 * 横屏
	 */
	public void changeToLandscape() {
		Log.d(TAG, "landscape");
		if (null == mContext)
			return;
		restoreOrigenalVideoFrameSize();
		int newOrientation;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
		} else {
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}
		Activity act = (Activity) mContext;
		act.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		act.setRequestedOrientation(newOrientation);
		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = mScreenWidth;
		params.width = mScreenHeight;
		// 隐藏全屏图标
		mControllerChangeScreen.setVisibility(View.GONE);
		mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
		Log.d(TAG, "landscape end");
		mIsFullScreen = true;
		rebuildPlayerControllerFrame();
	}

	private class EventHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			showBuffering(false);
			int what = msg.what;
			switch (what) {
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_ENDED:

				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_BUFFERING:
				mIsBuffering = true;
				showBuffering(true);
				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_READY:
				mIsBuffering = false;
				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_PREPARING:

				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_START:
				hideErrorFrame();
				showPlaceHolder(false);
				showBuffering(false);
				showIcon(true);
				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_STARTED:
				showIcon(false);
				mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_STOP:
				showIcon(false);
				break;

			default:
				break;
			}
		}
	}

	private class ErrorHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case VideoManager.ERROR_EXOPLAYER_DECODE_FAILED:
				if (mPlayer.getPlayerType() != PLAYER_TYPE.FULL_SIGHT) {
					mMessageHandler.sendEmptyMessage(MSG_CHANGE_DECODE_MODE_FROM_EXO_TO_SOFT);
					return;
				} else {
					break;
				}
			default:
				break;
			}
			showErrorFrame(msg.what);
		}
	}

	protected void showBuffering(boolean flag) {
		if (flag)
			mProgressBarBuffering.setVisibility(View.VISIBLE);
		else
			mProgressBarBuffering.setVisibility(View.GONE);
	}

	private void showIcon(boolean flag) {
		if (mImageViewIcon == null)
			return;
		if (flag)
			mImageViewIcon.setVisibility(View.VISIBLE);
		else
			mImageViewIcon.setVisibility(View.INVISIBLE);
	}

	private void showBrightnessLayer(boolean flag) {
		if (mBrightnessLayer == null)
			return;
		if (flag)
			mBrightnessLayer.setVisibility(View.VISIBLE);
		else
			mBrightnessLayer.setVisibility(View.INVISIBLE);
	}

	private void showVolumeLayer(boolean flag) {
		if (mVolumeLayer == null)
			return;
		if (flag)
			mVolumeLayer.setVisibility(View.VISIBLE);
		else
			mVolumeLayer.setVisibility(View.INVISIBLE);
	}

	private void setBrightPercent(int percent) {
		if (mBrightnessPercent == null)
			return;
		mBrightnessPercent.setText(percent + "%");
		mMessageHandler.sendEmptyMessage(MSG_SHOW_BRIGHTNESS);
	}

	private void setVolumePercent(int percent) {
		if (mVolumePercent == null)
			return;
		mVolumePercent.setText(percent + "%");
		mMessageHandler.sendEmptyMessage(MSG_SHOW_VOLUME);
	}

	public void sendMessage(int msgType) {
		mMessageHandler.sendEmptyMessage(msgType);
	}

	public void sendMessageDelayed(Message msg, int ms) {
		mMessageHandler.sendMessageDelayed(msg, ms);
	}

	public void removeMessage(int msgType) {
		mMessageHandler.removeMessages(msgType);
	}

	protected void showPlaceHolder(boolean flag) {
		if (flag)
			mPlaceHoler.setVisibility(View.VISIBLE);
		else
			mPlaceHoler.setVisibility(View.INVISIBLE);
	}

	protected void hideErrorFrame() {
		mErrorFrame.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onError(int errorCode) {
		Log.d(TAG, "onError errorCode:" + errorCode);
		mErrorHandler.sendEmptyMessage(errorCode);
	}

	@Override
	public void onEvent(int eventCode) {
		Log.d(TAG, "onEvent eventCode:" + eventCode);
		mEventHandler.sendEmptyMessage(eventCode);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick");
	}

	private int mCenterX;
	private final int mMinMovementDipX = 30;
	private final int mMinMovementDipY = 30;
	private float mMinY;
	private float mMinX;
	protected static final int MOVE_NONE = -1;
	protected static final int MOVE_LEFT = 1;
	protected static final int MOVE_RIGHT = 2;
	protected static final int MOVE_UP = 3;
	protected static final int MOVE_DOWN = 4;

	protected float preMoveX = -1.0f;
	protected float preMoveY = -1.0f;
	protected MotionEvent motionEvent = null;

	protected int moveDirection = MOVE_NONE;
	protected float moveDistanceX = 0.0f;
	protected float moveDistanceY = 0.0f;
	protected MotionEvent mLastMotionEvent = null; // 用于滑屏距离测量
	// 屏幕滑动控制音量的灵敏度，数值越大，灵敏度越高
	private static final int VOLUME_SENSITIVITY = 40;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			moveDirection = MOVE_NONE;
			preMoveX = event.getRawX();
			preMoveY = event.getRawY();
			moveDistanceX = 0.0f;
			moveDistanceY = 0.0f;
			break;
		case MotionEvent.ACTION_MOVE: {// 左侧滑动更改亮度,右侧滑动调节音量,其它符合要求的滑动调节播放进度
			float afterMoveX = event.getRawX();
			float afterMoveY = event.getRawY();
			moveDistanceX = Math.abs(preMoveX - afterMoveX);
			moveDistanceY = Math.abs(preMoveY - afterMoveY);

			if (mPlayer.getPlayerType() == PLAYER_TYPE.NORMAL) {
				if (moveDistanceX < mMinX && moveDistanceY < mMinY) {// 移动距离太小,就忽略这个消息
					return false;
				} else if (moveDistanceX >= mMinX && moveDistanceY >= mMinY) {// 横向和纵向如果都超过预置距离,则整体忽略
					moveDirection = MOVE_NONE;
					return false;
				} else if (moveDistanceX > mMinX && moveDistanceY < mMinY) {// 横向滑动
					moveDirection = preMoveX > afterMoveX ? MOVE_LEFT : MOVE_RIGHT;
					return false;
				} else if (moveDistanceX < mMinX && moveDistanceY > mMinY) {// 纵向滑动
					moveDirection = preMoveY > afterMoveY ? MOVE_UP : MOVE_DOWN;
				}

				if (preMoveX < mCenterX) {// 靠左,调节屏幕亮度
					onPortraitMove(event, TYPE_BRIGHTNESS);
				} else if (preMoveX > mCenterX) { // 靠右,调节音量
					onPortraitMove(event, TYPE_VOLUME);
				} else { // 在中间,忽略
					return false;
				}
			}
			break;
		}
		case MotionEvent.ACTION_UP:
			if (mPlayer.getPlayerType() == PLAYER_TYPE.NORMAL)
				onMoveEventActionUp();
			if (moveDistanceX < 60.0 && moveDistanceY < 60.0) {
				if (mIsControllerVisible) {
					mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
				} else {
					mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
				}
			}
			break;

		default:
			break;
		}

		if (mPlayer.getPlayerType() == PLAYER_TYPE.FULL_SIGHT) {
			mPlayer.onTouch(event);
		}
		return true;
	}

	private void onPortraitMove(MotionEvent event, int type) {
		if (mContext == null || mPlayer == null)
			return;
		if (mLastMotionEvent == null) {
			mLastMotionEvent = MotionEvent.obtain(event);
			return;
		}

		if (type == TYPE_BRIGHTNESS) {
			float offset = event.getRawY() - mLastMotionEvent.getRawY();
			int value = Utils.getBrightness((Activity) mContext);
			Log.d(TAG, "onPortraitMove,current brightness=" + value);
			if (offset < 0) {
				value += Math.abs((int) (offset
						* (BFYConst.MAX_BRIGHTNESS - BFYConst.MIN_BRIGHTNESS) / mScreenHeight));
			} else if (offset > 0) {
				value -= Math.abs((int) (offset
						* (BFYConst.MAX_BRIGHTNESS - BFYConst.MIN_BRIGHTNESS) / mScreenHeight));
			}
			if (value < BFYConst.MIN_BRIGHTNESS) {
				value = (int) BFYConst.MIN_BRIGHTNESS;
			} else if (value > BFYConst.MAX_BRIGHTNESS) {
				value = (int) BFYConst.MAX_BRIGHTNESS;
			}
			Utils.effectBrightness((Activity) mContext, value);
			int percent = (int) (value * 100 / BFYConst.MAX_BRIGHTNESS);
			setBrightPercent(percent);
		} else if (type == TYPE_VOLUME) {
			float offset = event.getRawY() - mLastMotionEvent.getRawY();
			if (ignoreIt(Math.abs(offset), mScreenHeight)) {
				return;
			}
			if (offset < 0) {
				mPlayer.incVolume();
			} else if (offset > 0) {
				mPlayer.decVolume();
			}

			int percent = mPlayer.getCurrentVolume() * 100 / mPlayer.getMaxVolume();
			setVolumePercent(percent);
		}

		mLastMotionEvent.recycle();
		mLastMotionEvent = MotionEvent.obtain(event);
	}

	private boolean ignoreIt(float distance, int wholeDistance) {
		if (distance < wholeDistance / VOLUME_SENSITIVITY)
			return true;
		return false;
	}

	private void onMoveEventActionUp() {
		Log.d(TAG, "onMoveEventActionUp moveDirection:" + moveDirection);
		mLastMotionEvent = null;
		switch (moveDirection) {
		case MOVE_LEFT:
			doMoveLeft();
			break;
		case MOVE_RIGHT:
			doMoveRight();
			break;

		default:
			break;
		}
		moveDirection = MOVE_NONE;
	}

	protected void backToPortrait() {
		if (mIsFullScreen)
			changeToPortrait();
		else
			((Activity) mContext).finish();
	}

	protected final static int TYPE_VOLUME = 0;
	protected final static int TYPE_BRIGHTNESS = 1;
	protected int mType = TYPE_VOLUME;

	@Override
	public boolean performClick() {
		Log.d(TAG, "performClick");
		return super.performClick();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyDown,keyCode=" + keyCode);
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0
				&& mEnableBackToPortrait) {
			backToPortrait();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void enableBackToPortrait(boolean enable) {
		mEnableBackToPortrait = enable;
	}

	@Override
	public boolean handleMessage(Message msg) {
		int what = msg.what;
		int arg1 = msg.arg1;
		int arg2 = msg.arg2;
		switch (what) {
		case MSG_SHOW_CONTROLLER:
			mMessageHandler.removeMessages(MSG_SHOW_CONTROLLER);
			mMessageHandler.removeMessages(MSG_HIDE_CONTROLLER);
			// 5s后，自动隐藏
			mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROLLER, DELAY_TIME_STANDARD);
			showController(true);
			break;
		case MSG_HIDE_CONTROLLER:
			mMessageHandler.removeMessages(MSG_SHOW_CONTROLLER);
			mMessageHandler.removeMessages(MSG_HIDE_CONTROLLER);
			showController(false);
			break;
		case MSG_SHOW_BRIGHTNESS:
			mMessageHandler.removeMessages(MSG_HIDE_BRIGHTNESS);
			mMessageHandler.removeMessages(MSG_SHOW_BRIGHTNESS);
			showVolumeLayer(false);
			showBrightnessLayer(true);
			mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_BRIGHTNESS, DELAY_TIME_SHORT);
			break;
		case MSG_HIDE_BRIGHTNESS:
			mMessageHandler.removeMessages(MSG_HIDE_BRIGHTNESS);
			mMessageHandler.removeMessages(MSG_SHOW_BRIGHTNESS);
			showBrightnessLayer(false);
			break;
		case MSG_SHOW_VOLUME:
			mMessageHandler.removeMessages(MSG_SHOW_VOLUME);
			mMessageHandler.removeMessages(MSG_HIDE_VOLUME);
			showBrightnessLayer(false);
			showVolumeLayer(true);
			mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_VOLUME, DELAY_TIME_SHORT);
			break;
		case MSG_HIDE_VOLUME:
			mMessageHandler.removeMessages(MSG_SHOW_VOLUME);
			mMessageHandler.removeMessages(MSG_HIDE_VOLUME);
			showVolumeLayer(false);
			break;
		case MSG_ADJUST_ORIENTATION:
			mMessageHandler.removeMessages(MSG_ADJUST_ORIENTATION);
			int currentOrientation = mPlayerOrientationMessageListener.getCurrentOrigentation();

			if (currentOrientation == PlayerOrientationMessageListener.ORIENTATION_LEFT
					|| currentOrientation == PlayerOrientationMessageListener.ORIENTATION_RIGHT)
				changeToLandscape();
			else if (currentOrientation == PlayerOrientationMessageListener.ORIENTATION_BOTTOM
					|| currentOrientation == PlayerOrientationMessageListener.ORIENTATION_TOP)
				changeToPortrait();
			break;
		case MSG_NETWORK_CHANGED:
			 if (mPlayer == null || mPlayer.getIsDownload())
				 break;
			if (arg2 == BFYNetworkReceiver.NET_STATE_CONNECTION_MOBILE) {
				mPlayer.stop();
				onError(VideoManager.ERROR_MOBILE_NO_PLAY);
			} else if (arg2 == BFYNetworkReceiver.NET_STATE_CONNECTION_NONE) {
				mPlayer.stop();
				onError(VideoManager.ERROR_NO_NETWORK);
			}
			break;
		default:
			Log.d(TAG, "invailid msg");
			break;
		}
		return true;
	}

	@Override
	public void finalize() throws Throwable {
		if (mNetworkReceiver != null && mContext != null){
			mNetworkReceiver.release();
			mContext.unregisterReceiver(mNetworkReceiver);
			mContext = null;
			mNetworkReceiver = null;
		}
		if (mPlayerOrientationMessageListener != null){
			mPlayerOrientationMessageListener.release();
		}
		if (mMessageHandler != null){
			mMessageHandler.removeCallbacksAndMessages(null);
			mMessageHandler = null;
		}
		destoryDanmaku();
		showP2pData(false);
		super.finalize();
	}

	protected void changeFullSightMode(RenderMode mode) {
		if (mPlayer.getPlayerType() == PLAYER_TYPE.NORMAL || mPlayer == null)
			return;
		mPlayer.changedFullSightMode(mode);
	}

	protected void changeFullSightMode(ControlMode control) {
		if (mPlayer.getPlayerType() == PLAYER_TYPE.NORMAL || mPlayer == null)
			return;
		mPlayer.changedFullSightMode(control);
	}
	
	public void showP2pData(boolean flag){
		if (flag){
			if (mP2pDataFrame != null)
				mP2pDataFrame.startShow();
		} else {
			if (mP2pDataFrame != null)
				mP2pDataFrame.stopShow();
		}
	}
	
	private static class MyHandler extends BFYWeakReferenceHandler<BFMediaPlayerControllerBase>{
		
		public MyHandler(BFMediaPlayerControllerBase reference) {
			super(reference);
		}

		@Override
		protected void handleMessage(BFMediaPlayerControllerBase reference, Message msg) {
			reference.handleMessage(msg);
		}
		
	}
}
