package com.scarviz.sketch;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.scarviz.sketch.common.Define;

public class SketchAct extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
	private static final String TAG = "SketchAct";

	private TextView mTextView;

	private boolean mResolvingError = false;
	private GoogleApiClient mGoogleApiClient;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch);

		mTextView = (TextView)findViewById(R.id.txtView);

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Wearable.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sketch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	protected void onStart() {
		super.onStart();
		if (!mResolvingError) {
			Log.d(TAG, "GoogleApiClient Connect");
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onStop() {
		if (!mResolvingError) {
			Log.d(TAG, "GoogleApiClient Disconnect");
			Wearable.DataApi.removeListener(mGoogleApiClient, this);
			mGoogleApiClient.disconnect();
		}
		super.onStop();
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d(TAG, "Connected");
		Wearable.DataApi.addListener(mGoogleApiClient, this);
		mResolvingError = false;
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d(TAG, "Connection Suspended");

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "Connection Failed");
		mResolvingError = true;
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		Log.d(TAG, "Data Changed");
		for (DataEvent event : dataEvents) {
			if (event.getType() == DataEvent.TYPE_DELETED) {
				final String path = event.getDataItem().getUri().getPath();
				Log.d(TAG, "DataItem deleted: " + path);
			} else if (event.getType() == DataEvent.TYPE_CHANGED) {
				final String path = event.getDataItem().getUri().getPath();
				Log.d(TAG, "DataItem changed: " + path);
				if (Define.DATA_MAP_POINT_PATH.equals(path)) {
					DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
					final int point_x = dataMapItem.getDataMap().getInt(Define.POINT_X);
					final int point_y = dataMapItem.getDataMap().getInt(Define.POINT_Y);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							SetText("point x : " + point_x + ", point y : " + point_y);
						}
					});
				}
			}
		}
	}

	private void SetText(String mes){
		StringBuilder sb = new StringBuilder();
		sb.append(mes);

		String str = mTextView.getText().toString();
		if(str != null && !str.isEmpty()){
			sb.append('\n');
			sb.append(str);
		}

		mTextView.setText(sb.toString());
	}
}
