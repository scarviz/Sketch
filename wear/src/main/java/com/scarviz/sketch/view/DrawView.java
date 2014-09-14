package com.scarviz.sketch.view;

/**
 * Created by scarviz on 2014/09/13.
 */
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.scarviz.sketch.model.PointData;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class DrawView extends View {
	// Canvas描画用
	private Paint mCnvsPaint = new Paint();
	// 描画色
	private int mDrawColor = Color.WHITE;
	// 描画線の幅
	private int mStrokeWidth = 5;

	private Handler mHandler;

	private Timer mTimer;
	private Handler mTimerHandler = new Handler();
	private final static int TIMER_PERIOD = 10000;

	public final static int TOUCH_POINT = 1001;

	/**
	 * コンストラクタ
	 *
	 * @param context
	 * @param attrs
	 */
	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Canvas描画の設定
		// アンチエイリアスを有効にする
		mCnvsPaint.setAntiAlias(true);
		// 色の設定
		mCnvsPaint.setColor(mDrawColor);
		// 描画線の幅の設定
		mCnvsPaint.setStrokeWidth(mStrokeWidth);
		// 描画スタイルを塗りつぶしモードで設定
		mCnvsPaint.setStyle(Paint.Style.FILL);

		// 有効期限切れの座標情報を破棄するタイマー
		mTimer = new Timer(true);
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				ReSetPointList();
				mTimerHandler.post(new Runnable() {
					@Override
					public void run() {
						invalidate();
					}
				});
			}
		}, PointData.LIMIT, TIMER_PERIOD);
	}

	/**
	 * ファイナライザ
	 * @throws Throwable
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
		} finally {
			if(mTimer != null) {
				Log.d("finalize", "Stop Timer");
				// タイマーの停止処理
				mTimer.cancel();
				mTimer = null;
			}
		}
	}

	/**
	 * 描画処理
	 *
	 * @param canvas canvas
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		ArrayList<PointData> list = GetPointList();
		Point back = new Point(-1, -1);
		// 描画する
		for(PointData item : list){
			// 有効時間を過ぎていたら次を見る
			if(!item.IsEnable()){ continue; }

			Point point = item.GetPoint();
			if (point.x >= 0) {
				if (back.x < 0) {
					back = point;
				}
				// 点と点を直線で結び、連続しているように見せる
				canvas.drawLine(back.x, back.y, point.x, point.y, mCnvsPaint);

				int circle = mStrokeWidth/2;
				// 点は円描画することで滑らかな線に見せる
				canvas.drawCircle(point.x, point.y, circle, mCnvsPaint);
			}
			back = point;
		}
	}

	/**
	 * タッチイベント処理
	 *
	 * @param event event
	 * @return 成否
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d("onTouchEvent", String.valueOf(event.getAction()));
		// 途切れたかどうか
		boolean isPause = false;
		switch (event.getAction()){
			// 動いている場合
			case MotionEvent.ACTION_MOVE:
				isPause = false;
				break;
			// タッチされた、離した場合
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
				isPause = true;
				break;
		}

		Point point = new Point((int)event.getX(),(int)event.getY());
		DrawPoint(point.x, point.y, isPause);

		SendMessage(TOUCH_POINT, point);
		return true;
	}

	/**
	 * Pointを描画する
	 * @param x
	 * @param y
	 * @param isPause
	 */
	private void DrawPoint(int x, int y, boolean isPause){
		// 止めた場合
		if (isPause) {
			SetPoint(new PointData(-1, -1));
		} else {
			// タッチした座標を格納する
			SetPoint(new PointData(x, y));
		}

		// 再描画
		invalidate();
	}

	/**
	 * メッセージを送る
	 * @param id
	 * @param obj
	 */
	private void SendMessage(int id, Object obj){
		if(mHandler == null){
			return;
		}

		Message mes = Message.obtain();
		mes.what = id;
		mes.obj = obj;
		mHandler.sendMessage(mes);
	}

	/**
	 * Handlerの設定
	 * @param handler
	 */
	public void SetHandler(Handler handler) {
		mHandler = handler;
	}

	/**
	 * Pointを設定する
	 * @param point
	 */
	public void SetPoint(Point point){
		if(point == null){ return; }
		DrawPoint(point.x, point.y, false);
	}

	/** 描画位置 */
	private ArrayList<PointData> _drawPoint = new ArrayList<PointData>();
	/**
	 * 描画位置リストを取得する(DeepCopy)
	 * @return
	 */
	private ArrayList<PointData> GetPointList(){
		synchronized(this){
			ArrayList<PointData> list = new ArrayList<PointData>();
			for(PointData item : _drawPoint){
				PointData data = (PointData)item.clone();
				if(data == null){ continue; }
				list.add(data);
			}
			return list;
		}
	}
	/**
	 * 描画位置を設定する
	 * @param value
	 */
	private void SetPoint(PointData value){
		synchronized(this){
			_drawPoint.add(value);
		}
	}

	/**
	 * 描画位置リストをクリアする
	 */
	public void ClearPointList(){
		synchronized(this){
			_drawPoint.clear();
		}
		invalidate();
	}

	/**
	 * 描画位置リストを有効時間を見て再設定する
	 */
	private void ReSetPointList(){
		ArrayList<PointData> list = GetPointList();
		synchronized(this){
			_drawPoint.clear();

			for(PointData item : list){
				// 有効時間を過ぎていた場合、次を見る
				if(!item.IsEnable()){ continue; }

				PointData data = (PointData)item.clone();
				if(data == null){ continue; }
				_drawPoint.add(data);
			}
		}
	}
}
