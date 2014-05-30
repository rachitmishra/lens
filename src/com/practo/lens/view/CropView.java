package com.practo.lens.view;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class CropView extends View {

	// Corner index one
	private static final int TOP_LEFT = 0;

	// Corner index two
	private static final int TOP_RIGHT = 1;

	// Corner index three
	private static final int BOTTOM_RIGHT = 2;

	// Corner index four
	private static final int BOTTOM_LEFT = 3;

	// Default offset
	private double mDefaultOffset;

	// Default corner handle radius
	private double mDefaultCornerRadius;

	// Default corner handle offset
	private double mDefaultCornerOffset;

	// Current corner handle
	private Poynt mCorner;

	// Current corner handle index
	private int mCornerIndex;

	// Application context
	private Context context;

	// Variable to warn user if cropping not possible
	private boolean warn;

	// Current width of this view
	private double mWidth;

	// Current height of this view
	private double mHeight;

	// List to hold the corner handles
	private List<Poynt> corners = new ArrayList<Poynt>();

	// List to hold the corner handles
	private boolean mDefault = true;

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mDefault) {
			setDefaultCorners();
			mDefault = false;
		}

		updateCorners(canvas);
		updateCenters(canvas);
		// updateSnapView(canvas);
		updateEdges(canvas);
		updateShaderBackground(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {

		case MotionEvent.ACTION_UP:
			onTouchUp();
			return true;

		case MotionEvent.ACTION_DOWN:
			onTouchDown(new Poynt(event.getX(), event.getY()));
			return true;

		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < event.getHistorySize(); i++) {
				onTouchMove(new Poynt(event.getHistoricalX(i), event.getHistoricalY(i)));
			}
			return true;

		default:
			return false;
		}
	}

	/**
	 * Default constructor
	 * 
	 * @param context
	 */
	public CropView(Context context) {
		super(context);
		this.context = context;
	}

	/**
	 * 
	 * @param context
	 * @param attrs
	 */
	public CropView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	/**
	 * Set default.
	 */
	public void setDefault(boolean d) {
		this.mDefault = d;
	}

	/**
	 * Set default corners.
	 */
	public void setDefaultCorners() {

		mDefaultCornerRadius = Helper.getCornerRadius(context);
		mDefaultOffset = Helper.getOffset(context);
		mDefaultCornerOffset = Helper.getCornerOffset(context);
		mHeight = getHeight();
		mWidth = getWidth();

		corners.clear();
		corners.add(new Poynt(mDefaultCornerOffset, mDefaultCornerOffset));
		corners.add(new Poynt(mWidth - mDefaultCornerOffset, mDefaultCornerOffset));
		corners.add(new Poynt(mWidth - mDefaultCornerOffset, mHeight - mDefaultCornerOffset));
		corners.add(new Poynt(mDefaultCornerOffset, mHeight - mDefaultCornerOffset));

		setCorners();
	}

	/**
	 * Set touch corners.
	 * 
	 * @param corners
	 */
	public void setCorners(List<Poynt> corners) {

		Log.w("Tagged", "Detected corners " + corners.toString());

		this.corners.clear();

		sortCorners(corners);

		this.corners = corners;

		setCorners();
	}

	/**
	 * Sort points.
	 * 
	 * @param corners
	 * @return
	 */
	public List<Poynt> sortCorners(List<Poynt> corners) {

		List<Poynt> top = new ArrayList<Poynt>(), bottom = new ArrayList<Poynt>();
		double cX = 0, cY = 0;

		for (Poynt pointer : corners) {
			cX += pointer.getX();
			cY += pointer.getY();
		}

		Poynt cPoint = new Poynt(cX / corners.size(), cY / corners.size());

		for (Poynt pointer : corners) {
			if (pointer.getY() < cPoint.getY()) {
				top.add(pointer);
			} else {
				bottom.add(pointer);
			}
		}

		Poynt topLeft = top.get(0).getX() > top.get(1).getX() ? top.get(1) : top.get(0);
		Poynt topRight = top.get(0).getX() > top.get(1).getX() ? top.get(0) : top.get(1);
		Poynt bottomLeft = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(1) : bottom.get(0);
		Poynt bottomRight = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(0) : bottom.get(1);

		this.corners.clear();

		corners.add(topLeft);
		corners.add(topRight);
		corners.add(bottomRight);
		corners.add(bottomLeft);
		return corners;
	}

	/**
	 * Initialize touch corners .
	 */
	public void setCorners() {

		Log.w("Tagged", "Setting next corners " + corners.toString());

		if (corners.isEmpty()) {
			return;
		}

		corners.get(TOP_LEFT).setNext(corners.get(TOP_RIGHT));
		corners.get(TOP_RIGHT).setNext(corners.get(BOTTOM_RIGHT));
		corners.get(BOTTOM_RIGHT).setNext(corners.get(BOTTOM_LEFT));
		corners.get(BOTTOM_LEFT).setNext(corners.get(TOP_LEFT));
	}

	/**
	 * Get touch corners.
	 * 
	 * @return
	 */
	public List<Poynt> getCorners() {
		return corners;
	}

	/**
	 * Get skew angle for top and bottom lines.
	 * 
	 * @param edges
	 * @return
	 */
	public double getSkewAngle(List<Poynt> corners) {
		double skewAngle = 0;

		skewAngle += Math.atan2(corners.get(TOP_RIGHT).getY() - corners.get(TOP_LEFT).getY(), corners.get(TOP_RIGHT).getX()
				- corners.get(TOP_LEFT).getX());
		skewAngle += Math.atan2(corners.get(BOTTOM_RIGHT).getX() - corners.get(BOTTOM_LEFT).getX(),
				corners.get(BOTTOM_RIGHT).getX() - corners.get(BOTTOM_LEFT).getX());

		skewAngle /= 2;

		return skewAngle;
	}

	/**
	 * Update (Draw) corner handles (or circles).
	 * 
	 * @param canvas
	 */
	private void updateCorners(Canvas canvas) {

		Log.w("Tagged", "Updating Corners " + corners.toString());

		if (corners.isEmpty()) {
			return;
		}

		for (Poynt point : corners) {
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) mDefaultCornerRadius * 2, Helper.getCornerPaint(context));
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) (mDefaultCornerRadius * 0.5),
					Helper.getCornerLightPaint(context));
		}
	}

	/**
	 * Update (Draw) corner handles (or circles).
	 * 
	 * @param canvas
	 */
	private void updateCenters(Canvas canvas) {

		Log.w("Tagged", "Updating Centers " + corners.toString());

		if (corners.isEmpty()) {
			return;
		}

		for (Poynt point : corners) {
			canvas.drawCircle((float) (point.getCenter(point.getNext()).getX()), (float) (point.getCenter(point.getNext()).getY()),
					(float) (mDefaultCornerRadius * 0.5), Helper.getCornerLightPaint(context));
		}
	}

	/**
	 * Update (Draw) zoom window for current image.
	 * 
	 * @param canvas
	 */
	private void updateSnapView(Canvas canvas) {
		// sourceBitmap = Bitmap.createBitmap(1000, 1000,
		// Bitmap.Config.ARGB_8888);
		// BitmapShader shader = new BitmapShader(sourceBitmap, TileMode.CLAMP,
		// TileMode.CLAMP);
		// Paint shaderPaint = Helper.getCornerLightPaint(context);
		// canvas.drawCircle((float)
		// touchHandles.get(0).getNext().getNext().getX(),
		// (float) touchHandles.get(0).getNext().getNext().getY(), 60.0f,
		// shaderPaint);
	}

	/**
	 * Update (Draw) shader background.
	 * 
	 * @param canvas
	 */
	private void updateShaderBackground(Canvas canvas) {

		Log.w("Tagged", "Updating shader " + corners.toString());

		if (corners.isEmpty()) {
			return;
		}

		Paint paint = Helper.getCornerLightPaint(context);

		Path linePath = new Path();

		// top
		linePath.moveTo(0, 0);
		linePath.lineTo((float) mWidth, 0);
		linePath.lineTo((float) mWidth, (float) (corners.get(TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(TOP_RIGHT).getX(), (float) (corners.get(TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(TOP_LEFT).getX(), (float) (corners.get(TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo(0, (float) (corners.get(TOP_LEFT).getY() - mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// left
		linePath.reset();
		linePath.moveTo(0, (float) (corners.get(TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(TOP_LEFT).getX() - mDefaultOffset / 4), (float) (corners.get(TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(BOTTOM_LEFT).getX() - mDefaultOffset / 4),
				(float) (corners.get(BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) (corners.get(BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// right
		linePath.reset();
		linePath.moveTo((float) (corners.get(TOP_RIGHT).getX() + mDefaultOffset / 4), (float) (corners.get(TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(BOTTOM_RIGHT).getX() + mDefaultOffset / 4),
				(float) (corners.get(BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// bottom
		linePath.reset();
		linePath.moveTo(0, (float) (corners.get(BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) (corners.get(BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(BOTTOM_RIGHT).getX(), (float) (corners.get(BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(BOTTOM_LEFT).getX(), (float) (corners.get(BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

	}

	/**
	 * Update (Draw) border lines of crop window
	 * 
	 * @param canvas
	 */
	private void updateEdges(Canvas canvas) {

		Log.w("Tagged", "Updating edges " + corners.toString());

		if (corners.isEmpty()) {
			return;
		}

		Path linePath = new Path();

		// top
		linePath.moveTo((float) (corners.get(TOP_LEFT).getX()), (float) (corners.get(TOP_LEFT).getY()));

		// right
		linePath.lineTo((float) (corners.get(TOP_RIGHT).getX()), (float) (corners.get(TOP_RIGHT).getY()));

		// bottom
		linePath.lineTo((float) (corners.get(BOTTOM_RIGHT).getX()), (float) (corners.get(BOTTOM_RIGHT).getY()));

		// left
		linePath.lineTo((float) (corners.get(BOTTOM_LEFT).getX()), (float) (corners.get(BOTTOM_LEFT).getY()));

		linePath.close();

		Paint paint = Helper.getLinePaint(context);
		if (warn) {
			paint = Helper.getLineErrorPaint(context);
		}

		canvas.drawPath(linePath, paint);
	}

	/**
	 * On touch up.
	 */
	private void onTouchUp() {
		if (mCorner == null) {
			return;
		}

		mCorner = null;
		mCornerIndex = -1;
		invalidate();
	}

	/**
	 * On touch down.
	 * 
	 * @param p
	 */
	private void onTouchDown(Poynt p) {

		mCorner = getPressedCornerHandle(p);

		if (mCorner == null) {
			return;
		}

		invalidate();
	}

	/**
	 * Get the current touched corner handle.
	 * 
	 * @param p
	 * @return
	 */
	private Poynt getPressedCornerHandle(Poynt p) {

		Poynt currentTouchPoint = null;

		for (Poynt c : corners) {

			double differenceSquare = Math.pow(p.getX() - c.getX(), 2) + Math.pow(p.getY() - c.getY(), 2);
			double radiusSquare = Math.pow(mDefaultOffset * 5, 2);

			if (differenceSquare <= radiusSquare) {
				currentTouchPoint = c;
			}

		}

		return currentTouchPoint;
	}

	/**
	 * On touch move.
	 * 
	 * @param p
	 */
	private void onTouchMove(Poynt p) {

		if (mCorner == null)
			return;

		updateCropWindow(p);
	}

	/**
	 * Update crop window (corner handles).
	 * 
	 * @param p
	 */
	private void updateCropWindow(Poynt p) {

		if (isOverlapping(p) || isOutbound(p)) {
			return;
		}

		if (corners.indexOf(mCorner) == -1) {
			return;
		}

		// updateSnapView()

		mCornerIndex = corners.indexOf(mCorner);
		corners.get(mCornerIndex).setXY(p.getX(), p.getY());

		invalidate();
	}

	/**
	 * If current corner handle overlaps to other.
	 * 
	 * @param p
	 * @return
	 */
	private boolean isOverlapping(Poynt p) {
		for (Poynt point : corners) {
			if (p.getDistance(point) <= mDefaultOffset * 2) {
				warn = true;
				return true;
			}
		}

		return false;
	}

	/**
	 * If current corner handle is out of bounds.
	 * 
	 * @param p
	 * @return
	 */
	private boolean isOutbound(Poynt p) {
		double x = p.getX();
		double y = p.getY();

		if ((x > mWidth - mDefaultOffset || x < mDefaultOffset) || (y > mHeight - mDefaultOffset || y < mDefaultOffset)) {
			return true;
		}

		return false;
	}

	/**
	 * If diagonal length is less.
	 * 
	 * @param p
	 * @return
	 */
	private boolean isDiagonal(Poynt p) {

		Poynt diagonalPoint = mCorner.getNext().getNext();
		double minimumLength = 2 * (mCorner.getDistance(diagonalPoint) / 3);

		if (p.getDistance(diagonalPoint) > minimumLength) {
			warn = true;
			return true;
		}

		return false;
	}

	/**
	 * Wrapper class to hold a point.
	 * 
	 * @author x
	 * 
	 */
	public class Poynt {

		// X coordinate
		private double x;

		// Y coordinate
		private double y;

		// Next point to current point.
		private Poynt next;

		// Center point to current point.
		private Poynt center;

		/**
		 * Default Constructor.
		 */
		public Poynt() {
			this(0, 0);
		}

		/**
		 * Constructor
		 * 
		 * @param points
		 */
		public Poynt(double[] points) {
			this();
			set(points);
		}

		/**
		 * Constructor
		 * 
		 * @param points
		 */
		public void set(double[] points) {
			if (points != null) {
				x = points.length > 0 ? points[0] : 0;
				y = points.length > 1 ? points[1] : 0;
			} else {
				x = 0;
				y = 0;
			}
		}

		/**
		 * Constructor
		 * 
		 * @param x
		 * @param y
		 */
		public Poynt(double x, double y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * Set X coordinate for this point.
		 * 
		 * @param x
		 */
		public void setX(double x) {
			this.x = x;
		}

		/**
		 * Set Y coordinate for this point.
		 * 
		 * @param y
		 */
		public void setY(double y) {
			this.y = y;
		}

		/**
		 * Set Y coordinate for this point.
		 * 
		 * @param y
		 */
		public void setXY(double x, double y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * Get X coordinate for this point.
		 * 
		 * @return
		 */
		public double getX() {
			return x;
		}

		/**
		 * Get Y coordinate for this point.
		 * 
		 * @return
		 */
		public double getY() {
			return y;
		}

		/**
		 * Set next point to this point.
		 * 
		 * @param p
		 */
		public void setNext(Poynt p) {
			this.next = p;
		}

		/**
		 * Get next point to this point.
		 * 
		 * @return next
		 */
		public Poynt getNext() {
			return next;
		}

		/**
		 * Set center point to this point.
		 * 
		 * @param p
		 */
		public void setCenter(Poynt p) {
			this.center = p;
		}

		/**
		 * Get center point to this point.
		 * 
		 * @return next
		 */
		public Poynt getCenter() {
			return center;
		}

		/**
		 * Get distance from this point.
		 * 
		 * @param p
		 * @return distance
		 */
		public double getDistance(Poynt p) {
			return Math.sqrt(Math.pow((p.getX() - this.x), 2) + Math.pow((p.getY() - this.y), 2));
		}

		/**
		 * Get distance from this point.
		 * 
		 * @param p
		 * @return distance
		 */
		public Poynt getCenter(Poynt p) {
			return new Poynt((x + p.getX()) / 2, (y + p.getY()) / 2);
		}

		/**
		 * Clone this point.
		 * 
		 * @return point.
		 */
		public Point clone() {
			return new Point(x, y);
		}

		/**
		 * Check equality with other point.
		 * 
		 * @return equal
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Point))
				return false;
			Point it = (Point) obj;
			return x == it.x && y == it.y;
		}

		/**
		 * Print this point.
		 */
		@Override
		public String toString() {
			return "{" + x + ", " + y + "}";
		}

	}

	/**
	 * Helper class for common data.
	 * 
	 * @author x
	 * 
	 */
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

		public static void log(String message) {
			Log.i("Tagged", message);
		}

	}

}
