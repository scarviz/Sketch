package com.scarviz.sketch;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.scarviz.sketch.common.Define;

import java.lang.ref.WeakReference;
import java.util.Set;

public class SketchAct extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
	private static final String TAG = "SketchAct";

	private BtProcHandler mBtProcHandler;
	private BluetoothHelper mBtHelper;
	private DeviceListAdapter mDeviceListAdapter;
	private final String UUID = "bc2a448a-d62f-46f4-ac27-5788a8cb0db4";
	private static final int REQUEST_DISCOVERABLE_BT = 2001;
	private static final int DURATION = 300;

	private TextView mTextView;
	private Button mBtnDiscoverable, mBtnStartServer, mBtnSearch;
	private ListView mDeviceList;

	private boolean mResolvingError = false;
	private GoogleApiClient mGoogleApiClient;

	private final static String COMMMA = ",";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch);

		mTextView = (TextView)findViewById(R.id.txtView);
		mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

		mBtnDiscoverable = (Button) findViewById(R.id.btnDiscoverable);
		mBtnStartServer = (Button) findViewById(R.id.btnStartServer);
		mBtnSearch = (Button) findViewById(R.id.btnSearch);
		mDeviceList = (ListView) findViewById(R.id.list);

		mBtnDiscoverable.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mBtHelper == null){
					SetText("Service Not Bound");
					return;
				} else if(!mBtHelper.IsEnabledBluetooth()){
					SetText("BlueTooth Not Enable");
					return;
				}
				StartDiscoverable();
				SetText("BlueTooth Discoverable Started");
			}
		});

		mBtnStartServer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mBtHelper == null){
					SetText("Service Not Bound");
					return;
				} else if(!mBtHelper.IsEnabledBluetooth()){
					SetText("BlueTooth Not Enable");
					return;
				}

				mBtHelper.StartServer();
				SetText("BlueTooth Server Started");
			}
		});

		mBtnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mBtHelper == null){
					SetText("Service Not Bound");
					return;
				} else if (!mBtHelper.IsEnabledBluetooth()) {
					SetText("BlueTooth Not Enable");
					return;
				}

				mDeviceListAdapter.clear();
				mDeviceListAdapter.notifyDataSetChanged();

				mBtHelper.ScanDevice(DevieFoundReceiver);
			}
		});

		mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if (mBtHelper == null){
					SetText("Service Not Bound");
					return;
				} else if (!mBtHelper.IsEnabledBluetooth()) {
					SetText("BlueTooth Not Enable");
					return;
				}

				BluetoothDevice device = mDeviceListAdapter.getDevice(i);
				mBtHelper.Connect(device.getAddress());
				SetText("BlueTooth Connected");
			}
		});

		mBtProcHandler = new BtProcHandler(this);
		mBtHelper = new BluetoothHelper(this, UUID, mBtProcHandler);

		mDeviceListAdapter = new DeviceListAdapter(this);
		mDeviceList.setAdapter(mDeviceListAdapter);

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
		SetPairedDevices();
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
	public void onDestroy() {
		Log.d("BTService", "onDestroy");
		if(mBtHelper != null) {
			mBtHelper.Cancel();
		}
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
					SendData(point_x, point_y);
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

	private void SendData(int point_x, int point_y){
		if(mBtHelper == null || !mBtHelper.IsConnected()){
			Log.d("BTService.SendNotification", "BlueTooth Not Connected");
			return;
		}

		String pointStr = point_x + COMMMA + point_y;
		mBtHelper.sendMessage(pointStr);
	}

	/**
	 * Bluetooth機器として検出されるようにする
	 */
	public void StartDiscoverable() {
		Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DURATION);
		startActivityForResult(intent, REQUEST_DISCOVERABLE_BT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("requestCode:" + requestCode + " resultCode:" + resultCode + " data:" + data);
		if (requestCode == REQUEST_DISCOVERABLE_BT) {
			if (resultCode == DURATION) {
				// 「はい」が選択された
			}
		}
	}

	/**
	 * ペアリング済み機器を一覧に設定する
	 */
	private void SetPairedDevices(){
		Set<BluetoothDevice> pairedDevices = mBtHelper.GetPairedDevices();
		if (pairedDevices != null && 0 < pairedDevices.size()){
			for(BluetoothDevice device : pairedDevices) {
				mDeviceListAdapter.addDevice(device);
				mDeviceListAdapter.notifyDataSetChanged();
			}
		}
	}

	/**
	 * Bluetooth機器のスキャンのコールバック
	 */
	private final BroadcastReceiver DevieFoundReceiver = new BroadcastReceiver(){
		//検出されたデバイスからのブロードキャストを受ける
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			String dName = null;
			BluetoothDevice foundDevice;

			if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						SetText("BlueTooth Device Scan ...");
					}
				});
			}

			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				//デバイスが検出された
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if((dName = foundDevice.getName()) != null){
					Log.d("ACTION_FOUND", dName);

					mDeviceListAdapter.addDevice(foundDevice);
					mDeviceListAdapter.notifyDataSetChanged();

					////接続したことのないデバイスのみアダプタに詰める
					//if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
					//}
				}
			}

			if(BluetoothDevice.ACTION_NAME_CHANGED.equals(action)){
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if((dName = foundDevice.getName()) != null) {
					//名前が検出された
					Log.d("ACTION_NAME_CHANGED", dName);

					mDeviceListAdapter.addDevice(foundDevice);
					mDeviceListAdapter.notifyDataSetChanged();

					////接続したことのないデバイスのみアダプタに詰める
					//if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
					//}
				}
			}

			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						SetText("Stop Scan");
					}
				});
			}
		}
	};

	/**
	 * Bluetooth通信処理のハンドラ
	 */
	private static class BtProcHandler extends Handler {
		WeakReference<SketchAct> ref;

		public BtProcHandler(SketchAct r) {
			ref = new WeakReference<SketchAct>(r);
		}

		@Override
		public void handleMessage(Message msg) {
			final SketchAct act = ref.get();
			if (act == null) {
				return;
			}

			switch (msg.what) {
				case BluetoothHelper.RES_HANDL_ID:
					String mes = (String)msg.obj;
					if(mes == null || mes.isEmpty()){
						return;
					}

					String[] pointStr = mes.split(COMMMA);
					if(pointStr == null || pointStr.length < 2){
						return;
					}
					int point_x = Integer.parseInt(pointStr[0]);
					int point_y = Integer.parseInt(pointStr[1]);

					Log.d(TAG, "sending point x:" +point_x + ", y:" + point_y);
					PutDataMapRequest dataMap = PutDataMapRequest.create(Define.DATA_MAP_POINT_PATH);
					dataMap.getDataMap().putInt(Define.POINT_X, point_x);
					dataMap.getDataMap().putInt(Define.POINT_Y, point_y);
					PutDataRequest request = dataMap.asPutDataRequest();
					PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
							.putDataItem(act.mGoogleApiClient, request);
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	}
}
