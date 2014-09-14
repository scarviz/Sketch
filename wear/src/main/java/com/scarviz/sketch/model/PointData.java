package com.scarviz.sketch.model;

import android.graphics.Point;

/**
 * 座標情報
 */
public class PointData implements Cloneable {
	/** X座標 */
	private int X;
	/** Y座標 */
	private int Y;
	/** タイムスタンプ */
	private long TimeStamp;

	/** 有効時間(ms) */
	public final static int LIMIT = 4000;

	/**
	 * コンストラクタ
	 * @param x X座標
	 * @param y Y座標
	 */
	public PointData(int x, int y){
		X = x;
		Y = y;
		TimeStamp = System.currentTimeMillis();
	}

	/**
	 * Point取得
	 * @return
	 */
	public Point GetPoint(){
		return new Point(X, Y);
	}

	/**
	 * X座標
	 * @return X座標
	 */
	public int PointX(){
		return X;
	}

	/**
	 * Y座標
	 * @return Y座標
	 */
	public int PointY(){
		return Y;
	}

	/**
	 * 有効時間内かどうか
	 * @return
	 */
	public boolean IsEnable(){
		long now = System.currentTimeMillis();
		// 有効時間内かどうか
		return (now - TimeStamp < LIMIT);
	}

	/**
	 * ディープコピー
	 * @return
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
