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
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DrawView extends View {
	// Canvas描画用
	private Paint mCnvsPaint = new Paint();
	// 描画色
	private int mDrawColor = Color.WHITE;
	// 描画線の幅
	private int mStrokeWidth = 5;

	// 描画位置
	private ArrayList<Point> mDrawPoint = new ArrayList<Point>();

	private Handler mHandler;

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

		Point back = new Point(-1, -1);
		// 描画する
		for(Point item : mDrawPoint){
			if (item.x >= 0) {
				if (back.x < 0) {
					back = item;
				}
				// 点と点を直線で結び、連続しているように見せる
				canvas.drawLine(back.x, back.y, item.x, item.y, mCnvsPaint);

				int circle = mStrokeWidth/2;
				// 点は円描画することで滑らかな線に見せる
				canvas.drawCircle(item.x, item.y, circle, mCnvsPaint);
			}
			back = item;
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
		Point point = new Point((int)event.getX(),(int)event.getY());
		// タッチした座標を格納する
		mDrawPoint.add(point);

		// タッチを止めた場合(画面から離した場合)
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mDrawPoint.add(new Point(-1, -1));
		}

		// 再描画
		invalidate();

		SendMessage(TOUCH_POINT, point);
		return true;
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
	 * 描画をクリアする
	 */
	public void ClearView(){
		mDrawPoint.clear();
		invalidate();
	}
}
