package com.wildwolf.mybf.cloud;

import java.text.DecimalFormat;
import java.util.List;

import bf.cloud.android.modules.p2p.P2pDataDetail;
import bf.cloud.android.modules.p2p.P2pDataDetail.Listener;
import bf.cloud.android.modules.p2p.P2pDataDetail.Task;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wildwolf.mybf.R;

public class P2pDataFrame extends FrameLayout {
	private Context mContext = null;
	private TextView mTvGcid = null;
	private String mGcid = null;
	private TextView mTvDlLimit = null;
	private String mDlLimit = null;
	private TextView mTvDownloadPos = null;
	private String mDownloadPos = null;
	private TextView mTvDlSpeed = null;
	private String mDlSpeed = null;
	private TextView mTvDlRatio = null;
	private String mDlRatio = null;
	private TextView mTvFileSize = null;
	private String mFileSize = null;
	private TextView mTvType = null;
	private String mType = null;
	private TextView mTvUrgentStart = null;
	private String mUrgentStart = null;
	private TextView mTvUrgentEnd = null;
	private String mUrgentEnd = null;
	private TextView mTvTotalDlLimit = null;
	private String mTotalDlLimit = null;
	private TextView mTvTotalUpLimit = null;
	private String mTotalUpLimit = null;
	private P2pDataDetail mDataDetail = null;
	
	private TextView mDownloadedRanges = null;
	private TextView mOverLapRanges = null;
	private TextView mUploadableRanges = null;
	private TextView mVerifiedRanges = null;
	private TextView mStreamDragPos = null;
	private TextView mStreamSendPos = null;
	
	private DecimalFormat mDecimalFormat = new DecimalFormat("0.00");

	public P2pDataFrame(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public P2pDataFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public P2pDataFrame(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	private void init() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.p2p_data_show_frame, this,
				false);
		addView(layout);

		mTvGcid = (TextView) layout.findViewById(R.id.gcid);
		mTvDlLimit = (TextView) layout.findViewById(R.id.dl_limit);
		mTvDownloadPos = (TextView) layout.findViewById(R.id.download_pos);
		mTvDlSpeed = (TextView) layout.findViewById(R.id.dl_speed);
		mTvDlRatio = (TextView) layout.findViewById(R.id.dl_ratio);
		mTvFileSize = (TextView) layout.findViewById(R.id.file_size);
		mTvType = (TextView) layout.findViewById(R.id.type);
		mTvUrgentStart = (TextView) layout.findViewById(R.id.urgent_start);
		mTvUrgentEnd = (TextView) layout.findViewById(R.id.urgent_end);
		mTvTotalDlLimit = (TextView) layout.findViewById(R.id.total_dl_limit);
		mTvTotalUpLimit = (TextView) layout.findViewById(R.id.total_up_limit);
		
		mDownloadedRanges = (TextView) findViewById(R.id.downloaded_ranges);
		mOverLapRanges = (TextView) findViewById(R.id.overlap_dl_ranges);
		mUploadableRanges = (TextView) findViewById(R.id.uploadable_ranges);
		mVerifiedRanges = (TextView) findViewById(R.id.verified_ranges);
		mStreamDragPos = (TextView) findViewById(R.id.stream_drag_pos);
		mStreamSendPos = (TextView) findViewById(R.id.stream_send_pos);
		
		mDataDetail = new P2pDataDetail();
		mDataDetail.registListener(new Listener() {

			@Override
			public void onPerSecond(List<Task> tasks, long total_dl_limit, long total_up_limit) {
				if (tasks.size() > 1 || tasks.size() == 0)
					return;
				Task task = tasks.get(0);
				mTvGcid.setText(task.gcid);
				String dlLimit = mDecimalFormat.format((float)task.dl_limit/1024/1024);
				mTvDlLimit.setText(dlLimit + "MB");
				String downloadPost = mDecimalFormat.format((float)task.download_pos/1024/1024);
				mTvDownloadPos.setText(downloadPost + "MB");
				String speed = mDecimalFormat.format((float)task.download_speed / 1024 /1024);
				mTvDlSpeed.setText(speed + "MB");
				mTvDlRatio.setText("%" + task.downloaded_ratio);
				String fileSize = mDecimalFormat.format((float)task.file_size/1024/1024);
				mTvFileSize.setText(fileSize + "MB");
				mTvType.setText(task.type + "");
				String start = mDecimalFormat.format((float)task.urgent_start / 1024 /1024);
				mTvUrgentStart.setText(start + "MB");
				String end = mDecimalFormat.format((float)task.urgent_end / 1024 /1024);
				mTvUrgentEnd.setText(end + "MB");
				String tDownloadLimit = mDecimalFormat.format((float)total_dl_limit / 1024 /1024);
				mTvTotalDlLimit.setText(tDownloadLimit + "MB");
				String tUploadLimit = mDecimalFormat.format((float)total_up_limit / 1024 /1024);
				mTvTotalUpLimit.setText(tUploadLimit + "MB");
				
				String downloadRanges = "";
				for (long[] range : task.downloaded_ranges){
					downloadRanges += "[";
					downloadRanges += range[0] + ",";
					downloadRanges += range[1] + "]";
				}
				mDownloadedRanges.setText(downloadRanges);
				String overlapRanges = "";
				for (long[] range : task.overlap_download_ranges){
					overlapRanges += "[";
					overlapRanges += range[0] + ",";
					overlapRanges += range[1] + "]";
				}
				mOverLapRanges.setText(overlapRanges);
				
				String uploadableRanges = "";
				for (long[] range : task.uploadable_ranges){
					uploadableRanges += "[";
					uploadableRanges += range[0] + ",";
					uploadableRanges += range[1] + "]";
				}
				mUploadableRanges.setText(uploadableRanges);
				String verifiedRanges = "";
				for (long[] range : task.verified_ranges){
					verifiedRanges += "[";
					verifiedRanges += range[0] + ",";
					verifiedRanges += range[1] + "]";
				}
				mVerifiedRanges.setText(verifiedRanges);
				String dragPos = "";
				for (long pos : task.stream_drag_pos){
					dragPos += pos + ",";
				}
				mStreamDragPos.setText(dragPos);
				
				String sendPos = "";
				for (long pos : task.stream_send_pos){
					sendPos += pos + ",";
				}
				mStreamSendPos.setText(sendPos);
			}
		});
	}
	
	public void startShow(){
		mDataDetail.start();
		setVisibility(View.VISIBLE);
	}
	
	public void stopShow(){
		mDataDetail.stop();
		setVisibility(View.GONE);
	}
}
