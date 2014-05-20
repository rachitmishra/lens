package com.practo.lens.view;

import java.util.ArrayList;

import org.apache.http.client.CircularRedirectException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class CropView extends View {

	private static final int CORNER_ONE = 0;

	private static final int CORNER_TWO = 1;

	private static final int CORNER_THREE = 2;

	private static final int CORNER_FOUR = 3;

	private Pair<Float, Float> mTouchOffset;

	// private Corner mPressedCorner;

	private Context context;

	private int touchIndex = 2;

	private ArrayList<Point> touchHandles = new ArrayList<CropView.Point>();

	public CropView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public CropView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	private void init() {
		touchHandles.add(new Point(10, 10));
		touchHandles.add(new Point(10, 100));
		touchHandles.add(new Point(100, 100));
		touchHandles.add(new Point(100, 10));

		setTouchHandles();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		drawCorners(canvas);
		// drawLines(canvas);
		// drawBackground(canvas);
	}

	private void drawBackground(Canvas canvas) {
		// Get background paint
	}

	public void setTouchHandles() {

		if (touchHandles.isEmpty()) {
			return;
		}

		touchHandles.get(CORNER_ONE).setPrevious(touchHandles.get(CORNER_FOUR));
		touchHandles.get(CORNER_ONE).setNext(touchHandles.get(CORNER_TWO));
		touchHandles.get(CORNER_TWO).setPrevious(touchHandles.get(CORNER_ONE));
		touchHandles.get(CORNER_TWO).setNext(touchHandles.get(CORNER_THREE));
		touchHandles.get(CORNER_THREE).setPrevious(touchHandles.get(CORNER_TWO));
		touchHandles.get(CORNER_THREE).setNext(touchHandles.get(CORNER_FOUR));
		touchHandles.get(CORNER_FOUR).setPrevious(touchHandles.get(CORNER_THREE));
		touchHandles.get(CORNER_FOUR).setNext(touchHandles.get(CORNER_ONE));
	}

	private void drawLines(Canvas canvas) {

		if (touchHandles.isEmpty()) {
			return;
		}

		Path linePath = new Path();
		linePath.moveTo(touchHandles.get(CORNER_ONE).getX(), touchHandles.get(CORNER_ONE).getY());
		linePath.lineTo(touchHandles.get(CORNER_ONE).getNext().getX(), touchHandles.get(CORNER_ONE).getNext().getY());
		linePath.lineTo(touchHandles.get(CORNER_ONE).getNext().getNext().getX(), touchHandles.get(CORNER_ONE).getNext().getNext().getY());
		linePath.lineTo(touchHandles.get(CORNER_ONE).getNext().getNext().getNext().getX(), touchHandles.get(CORNER_ONE).getNext().getNext().getNext()
				.getY());
		linePath.lineTo(touchHandles.get(CORNER_ONE).getX(), touchHandles.get(CORNER_ONE).getY());
		linePath.close();

		canvas.drawPath(linePath, Helper.getLinePaint(context));
	}

	private void drawCorners(Canvas canvas) {

		if (touchHandles.isEmpty()) {
			return;
		}

		for (Point point : touchHandles) {
			canvas.drawCircle(point.getX(), point.getY(), Helper.getCornerRadius(), Helper.getCornerPaint(context));
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			onTouchDown(new Point(event.getX(), event.getY()));
			return true;

		case MotionEvent.ACTION_UP:
			getParent().requestDisallowInterceptTouchEvent(false);
			onTouchUp();
			return true;

		case MotionEvent.ACTION_MOVE:
			onTouchMove(new Point(event.getX(), event.getY()));
			getParent().requestDisallowInterceptTouchEvent(true);
			return true;

		default:
			return false;
		}
	}

	private void onTouchDown(Point p) {

		Log.w("Lens", "Touch Down " + p.getX() + " --- " + p.getY());
		getPressedHandled(p);
		updateCropWindow(p);
		invalidate();
	}

	private void getPressedHandled(Point p) {
		for (Point c : touchHandles) {
			setTouchCorner(p, c);
			offsetCorner(p, c);
		}
	}

	private void setTouchCorner(Point p, Point c) {
		if (Math.sqrt(p.getX() - c.getX()) + Math.sqrt(p.getY() - c.getY()) < Math.sqrt(Helper.getCornerRadius() + 10)) {
			touchIndex = touchHandles.indexOf(p);

		}
	}

	private void offsetCorner(Point p, Point c) {
		if (Math.sqrt(p.getX() - c.getX()) + Math.sqrt(p.getY() - c.getY()) < Math.sqrt(Helper.getCornerRadius() + 10)) {
			Log.w("Lens", "Lies in circle");
			touchIndex = touchHandles.indexOf(p);
		}
	}

	private void onTouchUp() {
		invalidate();
	}

	private void onTouchMove(Point p) {

		Log.w("Lens", "Touch Move " + p.getX() + " --- " + p.getY());

		updateCropWindow(p);
	}

	private void updateCropWindow(Point p) {
		if (touchIndex == -1) {
			return;
		}

		Log.w("Lens", "Update window " + touchIndex);
		touchHandles.set(touchIndex, p);
		invalidate();
	}

	public class Point {

		private float x;
		private float y;
		private Point next, previous;

		public Point(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public void setX(float x) {
			this.x = x;
		}

		public void setY(float y) {
			this.y = y;
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}

		public void setNext(Point p) {
			this.next = p;
		}

		public Point getNext() {
			return next;
		}

		public void setPrevious(Point p) {
			this.previous = p;
		}

		public Point getPrevious() {
			return previous;
		}

	}

	public static class Helper {

		public static final int DEFAULT_CIRCLE_RADIUS = 10;

		public static final int DEFAULT_SNAP_RADIUS = 5;

		public static final int DEFAULT_LINE_THICKNESS = 5;

		public static final int DEFAULT_CORNER_COLOR = 0x900099CC;

		public static final int DEFAULT_LINE_COLOR = 0x450099CC;

		public static final int DEFAULT_BACKGROUND_COLOR = 0x150099CC;

		public static final int DEFAULT_CORNER_OFFSET = 10;

		public static Paint getCornerPaint(Context context) {
			final Paint cornerPaint = new Paint();
			cornerPaint.setColor(DEFAULT_CORNER_COLOR);
			cornerPaint.setStyle(Paint.Style.FILL);
			return cornerPaint;
		}

		public static Paint getLinePaint(Context context) {
			final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LINE_THICKNESS, context.getResources()
					.getDisplayMetrics());

			final Paint borderPaint = new Paint();
			borderPaint.setColor(DEFAULT_LINE_COLOR);
			borderPaint.setStrokeWidth(lineThicknessPx);
			borderPaint.setStyle(Paint.Style.STROKE);
			return borderPaint;
		}

		public static Paint getBackgroundPaint() {
			final Paint backgroundPaint = new Paint();
			backgroundPaint.setColor(DEFAULT_BACKGROUND_COLOR);
			backgroundPaint.setStyle(Paint.Style.FILL);
			return backgroundPaint;
		}

		public static float getCornerRadius() {
			return DEFAULT_CIRCLE_RADIUS;
		}

		public static float getLineThickness() {
			return DEFAULT_LINE_THICKNESS;
		}

	}

}
