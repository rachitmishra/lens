package com.practo.lens.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class CropView extends View {

	private static final int CORNER_ONE = 0;

	private static final int CORNER_TWO = 1;

	private static final int CORNER_THREE = 2;

	private static final int CORNER_FOUR = 3;
	
	private double defaultOffset, defaultCornerRadius, defaultCornerOffset;

	private Point touchPoint;

	private Context context;

	private static boolean init = true;

	private boolean warn;

	private int touchIndex;

	private double canvasWidth, canvasHeight;

	private Bitmap sourceBitmap;

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
		defaultCornerRadius = Helper.getCornerRadius(context);
		defaultOffset = Helper.getOffset(context);
		defaultCornerOffset = Helper.getCornerOffset(context);
		canvasHeight = getHeight();
		canvasWidth = getWidth();
		touchHandles.clear();
		touchHandles.add(new Point(defaultCornerOffset, defaultCornerOffset));
		touchHandles.add(new Point(canvasWidth - defaultCornerOffset, defaultCornerOffset));
		touchHandles.add(new Point(canvasWidth - defaultCornerOffset, canvasHeight - defaultCornerOffset));
		touchHandles.add(new Point(defaultCornerOffset, canvasHeight - defaultCornerOffset));
		setTouchHandles();
	}

	public void setTouchCorners(ArrayList<Point> corners) {
		touchHandles = corners;
		setTouchHandles();
	}

	public ArrayList<Point> getTouchCorners() {
		return touchHandles;
	}

	public void setSourceBitmap(Bitmap sourceBitmap) {
		this.sourceBitmap = sourceBitmap;
	}

	public Bitmap getSourceBitmap() {
		return sourceBitmap;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (init) {
			init();
			init = false;
		}

		updateCorners(canvas);
		// updateSnapView(canvas);
		updateLines(canvas);

		drawBackground(canvas);
	}

//	private void updateSnapView(Canvas canvas) {
//		sourceBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
//		BitmapShader shader = new BitmapShader(sourceBitmap, TileMode.CLAMP, TileMode.CLAMP);
//		Paint shaderPaint = Helper.getCornerLightPaint(context);
//		canvas.drawCircle((float) touchHandles.get(0).getNext().getNext().getX(), (float) touchHandles.get(0).getNext().getNext().getY(), 60.0f,
//				shaderPaint);
//	}

	private void drawBackground(Canvas canvas) {
		if (touchHandles.isEmpty()) {
			return;
		}
		
		Paint paint = Helper.getCornerLightPaint(context);
		
		Path linePath = new Path();
		
		// top 
		linePath.moveTo(0, 0);
		linePath.lineTo((float) canvasWidth, 0);
		linePath.lineTo((float) canvasWidth, (float) (touchHandles.get(CORNER_ONE).getY() - defaultOffset/4));
		linePath.lineTo((float) canvasWidth, (float) (touchHandles.get(CORNER_TWO).getY() - defaultOffset/4));
		linePath.lineTo((float) touchHandles.get(CORNER_TWO).getX(), (float) (touchHandles.get(CORNER_TWO).getY() - defaultOffset/4));
		linePath.lineTo((float) touchHandles.get(CORNER_ONE).getX(), (float) (touchHandles.get(CORNER_ONE).getY() - defaultOffset/4));
		linePath.lineTo(0, (float) (touchHandles.get(CORNER_ONE).getY() - defaultOffset/4));
		canvas.drawPath(linePath, paint);
		
		// left
		linePath.reset();
		linePath.moveTo(0, (float) (touchHandles.get(CORNER_ONE).getY()-defaultOffset/4));
		linePath.lineTo((float) (touchHandles.get(CORNER_ONE).getX() -defaultOffset/4), (float) (touchHandles.get(CORNER_ONE).getY() -defaultOffset/4));
		linePath.lineTo((float) (touchHandles.get(CORNER_FOUR).getX()-defaultOffset/4), (float) (touchHandles.get(CORNER_FOUR).getY()+defaultOffset/4));
		linePath.lineTo(0, (float) (touchHandles.get(CORNER_FOUR).getY()+defaultOffset/4));
		canvas.drawPath(linePath, paint);
		
		// right
		linePath.reset();
		linePath.moveTo((float) (touchHandles.get(CORNER_TWO).getX() +defaultOffset/4), (float) (touchHandles.get(CORNER_TWO).getY() -defaultOffset/4) );
		linePath.lineTo((float) canvasWidth, (float) (touchHandles.get(CORNER_TWO).getY() -defaultOffset/4));
		linePath.lineTo((float) canvasWidth, (float) (touchHandles.get(CORNER_THREE).getY()+defaultOffset/4));
		linePath.lineTo((float) (touchHandles.get(CORNER_THREE).getX()+defaultOffset/4), (float) (touchHandles.get(CORNER_THREE).getY()+defaultOffset/4));
		canvas.drawPath(linePath, paint);
		
		
		// bottom
		linePath.reset();
		linePath.moveTo(0, (float) (touchHandles.get(CORNER_FOUR).getY() + defaultOffset/4));
		linePath.lineTo(0, (float) canvasHeight);
		linePath.lineTo((float) canvasWidth, (float) canvasHeight);
		linePath.lineTo((float) canvasWidth, (float) (touchHandles.get(CORNER_THREE).getY() + defaultOffset/4));
		linePath.lineTo((float) touchHandles.get(CORNER_THREE).getX(), (float) (touchHandles.get(CORNER_THREE).getY() + defaultOffset/4));
		linePath.lineTo((float) touchHandles.get(CORNER_FOUR).getX(), (float) (touchHandles.get(CORNER_FOUR).getY() + defaultOffset/4));
		canvas.drawPath(linePath, paint);


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

	private void updateLines(Canvas canvas) {

		if (touchHandles.isEmpty()) {
			return;
		}

		Path linePath = new Path();

		// top
		linePath.moveTo((float) (touchHandles.get(CORNER_ONE).getX()), (float) (touchHandles.get(CORNER_ONE).getY()));

		// right
		linePath.lineTo((float) (touchHandles.get(CORNER_TWO).getX()), (float) (touchHandles.get(CORNER_TWO).getY()));

		// bottom
		linePath.lineTo((float) (touchHandles.get(CORNER_THREE).getX()), (float) (touchHandles.get(CORNER_THREE).getY()));

		// left
		linePath.lineTo((float) (touchHandles.get(CORNER_FOUR).getX()), (float) (touchHandles.get(CORNER_FOUR).getY()));

		linePath.close();

		Paint paint = Helper.getLinePaint(context);
		if (warn) {
			paint = Helper.getLineErrorPaint(context);
		}

		canvas.drawPath(linePath, paint);
	}

	private void updateCorners(Canvas canvas) {

		if (touchHandles.isEmpty()) {
			return;
		}
		
		for (Point point : touchHandles) {
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) defaultCornerRadius * 2, Helper.getCornerPaint(context));
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) (defaultCornerRadius * 0.5), Helper.getCornerLightPaint(context));
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
			onTouchUp();
			return true;

		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < event.getHistorySize(); i++) {
				onTouchMove(new Point(event.getHistoricalX(i), event.getHistoricalY(i)));
			}
			return true;

		default:
			return false;
		}
	}

	private void onTouchDown(Point p) {

		touchPoint = getPressedHandled(p);

		if (touchPoint == null) {
			return;
		}

		invalidate();
	}

	private Point getPressedHandled(Point p) {

		Point touchPoint = null;

		for (Point c : touchHandles) {

			double differenceSquare = Math.pow(p.getX() - c.getX(), 2) + Math.pow(p.getY() - c.getY(), 2);
			double radiusSquare = Math.pow(defaultOffset * 5, 2);

			if (differenceSquare <= radiusSquare) {
				touchPoint = c;
			}
		}

		return touchPoint;
	}

	private void onTouchUp() {
		if (touchPoint == null) {
			return;
		}

		touchPoint = null;
		touchIndex = -1;
		invalidate();
	}

	private void onTouchMove(Point p) {

		if (touchPoint == null)
			return;

		updateCropWindow(p);
	}

	private void updateCropWindow(Point p) {

		if (isOverlapping(p) || isOutbound(p)) {
			return;
		}

		if (touchHandles.indexOf(touchPoint) == -1) {
			return;
		}

		// updateSnapView()

		touchIndex = touchHandles.indexOf(touchPoint);
		touchHandles.get(touchIndex).setX(p.getX());
		touchHandles.get(touchIndex).setY(p.getY());

		invalidate();
	}

	private boolean isOverlapping(Point p) {
		for (Point point : touchHandles) {
			if (p.getDistance(point) <= defaultOffset * 4) {
				warn = true;
				return true;
			}
		}

		return false;
	}

	private boolean isOutbound(Point p) {
		double x = p.getX();
		double y = p.getY();

		if ((x > canvasWidth - defaultOffset || x < defaultOffset) || (y > canvasHeight - defaultOffset || y < defaultOffset)) {
			return true;
		}

		return false;
	}

//	private boolean isDiagonal(Point p) {
//
//		Point diagonalPoint = touchPoint.getNext().getNext();
//		double minimumLength = 2 * (touchPoint.getDistance(diagonalPoint) / 3);
//
//		if (p.getDistance(diagonalPoint) > minimumLength) {
//			warn = true;
//			return true;
//		}
//
//		return false;
//	}

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
			return Math.sqrt(Math.pow((p.getX() - this.x), 2) + Math.pow((p.getY() - this.y), 2));
		}

	}

	public static class Helper {

		public static final int DEFAULT_CIRCLE_RADIUS = 10;

		public static final int DEFAULT_SNAP_RADIUS = 5;

		public static final int DEFAULT_LINE_THICKNESS = 5;

		public static final int DEFAULT_CORNER_COLOR = 0x900099CC;

		public static final int DEFAULT_CORNER_LIGHT_COLOR = 0x45000000;

		public static final int DEFAULT_LINE_COLOR = 0x900099CC;

		public static final int DEFAULT_LINE_ERROR_COLOR = 0x4500CCEE;

		public static final int DEFAULT_BACKGROUND_COLOR = 0x150099CC;
		
		private static final int DEFAULT_OFFSET = 10;
		
		private static final int DEFAULT_CORNER_OFFSET = 30;

		public static Paint getCornerPaint(Context context) {
			final Paint cornerPaint = new Paint();
			cornerPaint.setColor(DEFAULT_CORNER_COLOR);
			cornerPaint.setStyle(Paint.Style.FILL);
			cornerPaint.setAntiAlias(true);
			return cornerPaint;
		}

		public static Paint getCornerLightPaint(Context context) {
			final Paint cornerPaint = new Paint();
			cornerPaint.setColor(DEFAULT_CORNER_LIGHT_COLOR);
			cornerPaint.setStyle(Paint.Style.FILL);
			cornerPaint.setAntiAlias(true);
			return cornerPaint;
		}

		public static Paint getLinePaint(Context context) {
			final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LINE_THICKNESS, context.getResources()
					.getDisplayMetrics());

			final Paint borderPaint = new Paint();
			borderPaint.setColor(DEFAULT_LINE_COLOR);
			borderPaint.setStrokeWidth(lineThicknessPx);
			borderPaint.setStyle(Paint.Style.STROKE);
			borderPaint.setAntiAlias(true);
			return borderPaint;
		}

		public static Paint getLineErrorPaint(Context context) {
			final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LINE_THICKNESS, context.getResources()
					.getDisplayMetrics());

			final Paint borderPaint = new Paint();
			borderPaint.setColor(DEFAULT_LINE_ERROR_COLOR);
			borderPaint.setStrokeWidth(lineThicknessPx);
			borderPaint.setStyle(Paint.Style.STROKE);
			borderPaint.setAntiAlias(true);
			return borderPaint;
		}

		public static Paint getBackgroundPaint() {
			final Paint backgroundPaint = new Paint();
			backgroundPaint.setColor(DEFAULT_BACKGROUND_COLOR);
			backgroundPaint.setStyle(Paint.Style.FILL);
			return backgroundPaint;
		}

		public static float getCornerRadius(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CIRCLE_RADIUS, context.getResources().getDisplayMetrics());
		}

		public static float getLineThickness(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LINE_THICKNESS, context.getResources().getDisplayMetrics());
		}

		public static float getOffset(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_OFFSET, context.getResources().getDisplayMetrics());
		}
		
		public static float getCornerOffset(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CORNER_OFFSET, context.getResources().getDisplayMetrics());
		}

	}

}
