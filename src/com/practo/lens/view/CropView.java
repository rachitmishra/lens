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

	private double defaultOffset;

	private Point touchPoint;

	private Context context;

	private static boolean init = true;

	private double canvasWidth, canvasHeight;

	private ArrayList<Point> touchHandles = new ArrayList<CropView.Point>();

	public CropView(Context context) {
		super(context);
		this.context = context;
		init = true;
	}

	public CropView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init = true;
	}

	public void init() {
		defaultOffset = Helper.getCornerRadius(context);
		canvasHeight = getHeight();
		canvasWidth = getWidth();
		touchHandles.clear();
		touchHandles.add(new Point(defaultOffset, defaultOffset));
		touchHandles.add(new Point(canvasWidth - defaultOffset, defaultOffset));
		touchHandles.add(new Point(canvasWidth - defaultOffset, canvasHeight
				- defaultOffset));
		touchHandles
				.add(new Point(defaultOffset, canvasHeight - defaultOffset));
		setTouchHandles();
	}

	public void init(ArrayList<Point> corners) {
		touchHandles = corners;
		setTouchHandles();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (init) {
			init();
			init = false;
		}
		drawCorners(canvas);
		drawLines(canvas);
		// drawBackground(canvas);
	}

	private void drawBackground(Canvas canvas) {
		if (touchHandles.isEmpty()) {
			return;
		}
	}

	public void setTouchHandles() {

		if (touchHandles.isEmpty()) {
			return;
		}

		touchHandles.get(CORNER_ONE).setNext(touchHandles.get(CORNER_TWO));
		touchHandles.get(CORNER_TWO).setNext(touchHandles.get(CORNER_THREE));
		touchHandles.get(CORNER_THREE).setNext(touchHandles.get(CORNER_FOUR));
		touchHandles.get(CORNER_FOUR).setNext(touchHandles.get(CORNER_ONE));
	}

	private void drawLines(Canvas canvas) {

		if (touchHandles.isEmpty()) {
			return;
		}

		Path linePath = new Path();

		// Move to Point 1
		linePath.moveTo(
				(float) (touchHandles.get(CORNER_ONE).getX() + defaultOffset),
				(float) (touchHandles.get(CORNER_ONE).getY()));

		// Line to Point 2
		linePath.lineTo(
				(float) (touchHandles.get(CORNER_ONE).getNext().getX() - defaultOffset),
				(float) (touchHandles.get(CORNER_ONE).getNext().getY()));

		// Move to Point 2
		linePath.moveTo(
				(float) (touchHandles.get(CORNER_ONE).getNext().getX()),
				(float) (touchHandles.get(CORNER_ONE).getNext().getY() + defaultOffset));

		// Line to Point 3
		linePath.lineTo((float) (touchHandles.get(CORNER_ONE).getNext()
				.getNext().getX()), (float) (touchHandles.get(CORNER_ONE)
				.getNext().getNext().getY() - defaultOffset));

		// Move to Point 3
		linePath.moveTo(
				(float) (touchHandles.get(CORNER_ONE).getNext().getNext().getX() - defaultOffset),
				(float) (touchHandles.get(CORNER_ONE).getNext().getNext().getY()));

		// Line to Point 4
		linePath.lineTo((float) (touchHandles.get(CORNER_ONE).getNext()
				.getNext().getNext().getX() + defaultOffset), (float) (touchHandles.get(CORNER_ONE)
				.getNext().getNext().getY()));

		// Move to Point 4
		linePath.moveTo(
				(float) (touchHandles.get(CORNER_ONE).getNext().getNext().getNext().getX()),
				(float) (touchHandles.get(CORNER_ONE).getNext().getNext().getNext().getY() - defaultOffset));

		// Line to Point 1
		linePath.lineTo((float) (touchHandles.get(CORNER_ONE).getX()), (float) (touchHandles.get(CORNER_ONE).getY()+defaultOffset));

		linePath.close();

		canvas.drawPath(linePath, Helper.getLinePaint(context));
	}

	private void drawCorners(Canvas canvas) {

		if (touchHandles.isEmpty()) {
			return;
		}

		for (Point point : touchHandles) {
			canvas.drawCircle((float) point.getX(), (float) point.getY(),
					Helper.getCornerRadius(context),
					Helper.getCornerPaint(context));
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

		Log.w("Tagged", "Touch Down " + p.getX() + " --- " + p.getY());
		getPressedHandled(p);
		updateCropWindow(p);
		invalidate();
	}

	private void getPressedHandled(Point p) {
		for (Point c : touchHandles) {
			setTouchCorner(p, c);
		}
	}

	private void setTouchCorner(Point p, Point c) {
		
		// check if touch circle overlaps with any of the corners.
		double differenceSquare   = Math.pow(p.getX()-c.getX(),2) +  Math.pow(p.getX()-c.getX(),2);
		double radiusSquare = Math.pow(defaultOffset+defaultOffset, 2);
		
		if ( 0<= differenceSquare && differenceSquare <= radiusSquare) {
			touchPoint = c;
		}
	}

	private void onTouchUp() {

		touchPoint = null;
		invalidate();
	}

	private void onTouchMove(Point p) {

		if (touchPoint == null)
			return;

		updateCropWindow(p);
	}

	private void updateCropWindow(Point p) {
		if (touchPoint == null) {
			return;
		}

		if (isOverlapping(p) || isOutbound(p)) {
			return;
		}

		touchHandles.remove(touchPoint);
		touchHandles.add(p);
		invalidate();
	}

	private boolean isOverlapping(Point p) {
		for (Point point : touchHandles) {
			if (p.getDistance(point) <= Helper.getCornerRadius(context) * 2) {
				return true;
			}
		}

		return false;
	}

	private boolean isOutbound(Point p) {
		double x = p.getX();
		double y = p.getY();

		if ((x > canvasWidth - defaultOffset || x < defaultOffset)
				|| (y > canvasHeight - defaultOffset || y < defaultOffset)) {
			return true;
		}

		return false;
	}

	public class Point {

		private double x;
		private double y;
		private Point next;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public void setX(double x) {
			this.x = x;
		}

		public void setY(double y) {
			this.y = y;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public void setNext(Point p) {
			this.next = p;
		}

		public Point getNext() {
			return next;
		}

		public double getDistance(Point p) {
			return Math.sqrt(Math.pow((p.getX() - this.x), 2)
					+ Math.pow((p.getY() - this.y), 2));
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
			final float lineThicknessPx = TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LINE_THICKNESS,
					context.getResources().getDisplayMetrics());

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

		public static float getCornerRadius(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					DEFAULT_CIRCLE_RADIUS, context.getResources()
							.getDisplayMetrics());
		}

		public static float getLineThickness(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					DEFAULT_LINE_THICKNESS, context.getResources()
							.getDisplayMetrics());
		}

	}

}
