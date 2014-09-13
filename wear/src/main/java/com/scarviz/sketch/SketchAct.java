package com.scarviz.sketch;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.scarviz.sketch.common.Define;
import com.scarviz.sketch.view.DrawView;

import java.lang.ref.WeakReference;

public class SketchAct extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
	private static final String TAG = "SketchAct";

	private SketchHandler mSketchHandler;
	private DrawView mDrawview;

	private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sketch);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

				mSketchHandler = new SketchHandler(SketchAct.this);
				// カスタムViewクラス
				mDrawview = (DrawView)stub.findViewById(R.id.drawview);
				mDrawview.SetHandler(mSketchHandler);
            }
        });

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Wearable.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();
    }

	@Override
	protected void onResume() {
		super.onResume();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Wearable.DataApi.removeListener(mGoogleApiClient, this);
		mGoogleApiClient.disconnect();
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d(TAG, "Connected");
		Wearable.DataApi.addListener(mGoogleApiClient, this);
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d(TAG, "Connection Suspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "Connection Failed");
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		Log.d(TAG, "Data Changed");
		for (DataEvent event : dataEvents) {
			if (event.getType() == DataEvent.TYPE_DELETED) {
				Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
			} else if (event.getType() == DataEvent.TYPE_CHANGED) {
				Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
				SetPoint(event.getDataItem().getUri().getPath(), event.getDataItem());
			}
		}
	}

	/**
	 * Pointを設定する
	 * @param path
	 * @param dataItem
	 */
	private void SetPoint(String path, DataItem dataItem){
		Log.d(TAG, "DataItem changed: " + path);
		if (Define.DATA_MAP_POINT_PATH.equals(path)) {
			DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
			final int point_x = dataMapItem.getDataMap().getInt(Define.POINT_X);
			final int point_y = dataMapItem.getDataMap().getInt(Define.POINT_Y);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Point point = new Point(point_x, point_y);
					mDrawview.SetPoint(point);
				}
			});
		}
	}

	/**
	 * Sketchのハンドラ
	 */
	private static class SketchHandler extends Handler {
		WeakReference<SketchAct> ref;

		public SketchHandler(SketchAct r) {
			ref = new WeakReference<SketchAct>(r);
		}

		@Override
		public void handleMessage(Message msg) {
			final SketchAct act = ref.get();
			if (act == null) {
				return;
			}

			switch (msg.what) {
				// タッチ座標
				case DrawView.TOUCH_POINT:
					if(msg.obj != null) {
						Point point = (Point)msg.obj;
						if(point == null){
							Log.d(act.TAG, "point is null");
							break;
						}

						Log.d(act.TAG, "putDataItem point_x :" + point.x + " ,point_y:" + point.y);
						PutDataMapRequest dataMap = PutDataMapRequest.create(Define.DATA_MAP_POINT_PATH);
						dataMap.getDataMap().putInt(Define.POINT_X, point.x);
						dataMap.getDataMap().putInt(Define.POINT_Y, point.y);
						PutDataRequest request = dataMap.asPutDataRequest();
						PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
								.putDataItem(act.mGoogleApiClient, request);
					}
					break;
			}
		}
	}
}
