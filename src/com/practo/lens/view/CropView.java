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
	private static final int CORNER_ONE = 0;

	// Corner index two
	private static final int CORNER_TWO = 1;

	// Corner index three
	private static final int CORNER_THREE = 2;

	// Corner index four
	private static final int CORNER_FOUR = 3;

	// Default offset
	private double mDefaultOffset;

	// Default corner handle radius
	private double mDefaultCornerHandleRadius;

	// Default corner handle offset
	private double mDefaultCornerHandleOffset;

	// Current corner handle
	private Poynt mCornerHandle;

	// Current corner handle
	private Poynt mCenterHandle;

	// Current corner handle index
	private int mCornerHandleIndex;

	// Current corner handle index
	private int mCenterHandleIndex;

	// Application context
	private Context context;

	// Variable to hold corner initializations
	private static boolean init = true;

	// Variable to warn user if cropping not possible
	private boolean warn;

	// Current width of this view
	private double mWidth;

	// Current height of this view
	private double mHeight;

	// List to hold the corner handles
	private List<Poynt> corners = new ArrayList<Poynt>();
	
	// List to hold the corner handles
	private List<Lyne> edges = new ArrayList<Lyne>();

	/**
	 * Default constructor
	 * 
	 * @param context
	 */
	public CropView(Context context) {
		super(context);
		this.context = context;
		init = true;
	}

	/**
	 * 
	 * @param context
	 * @param attrs
	 */
	public CropView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init = true;
	}

	/**
	 * Default initialization to full width and height.
	 */
	public void init() {
		mDefaultCornerHandleRadius = Helper.getCornerRadius(context);
		mDefaultOffset = Helper.getOffset(context);
		mDefaultCornerHandleOffset = Helper.getCornerOffset(context);
		mHeight = getHeight();
		mWidth = getWidth();
		corners.clear();
		corners.add(new Poynt(mDefaultCornerHandleOffset, mDefaultCornerHandleOffset));
		corners.add(new Poynt(mWidth - mDefaultCornerHandleOffset, mDefaultCornerHandleOffset));
		corners.add(new Poynt(mWidth - mDefaultCornerHandleOffset, mHeight - mDefaultCornerHandleOffset));
		corners.add(new Poynt(mDefaultCornerHandleOffset, mHeight - mDefaultCornerHandleOffset));
		setCorners();
		setEdges();
	}

	/**
	 * Set touch corners.
	 * 
	 * @param corners
	 */
	public void setCornerHandles(List<Poynt> corners) {

		corners.clear();
		sortPoints(corners);
		this.corners = corners;
		setCorners();
		setEdges();
		Log.w("Tagged", "Corners detected " + corners.toString());
	}

	/**
	 * Sort points.
	 * 
	 * @param corners
	 * @return
	 */
	public List<Poynt> sortPoints(List<Poynt> corners) {

		List<Poynt> top = new ArrayList<Poynt>(), bottom = new ArrayList<Poynt>();
		double cX = 0, cY = 0;

		for (Poynt pointer : corners) {
			cX += pointer.getX();
			cY += pointer.getY();
		}

		Poynt cPoint = new Poynt(cX / corners.size(), cY / corners.size());

		for (Poynt pointer : corners) {
			if (pointer.getY() < cPoint.getY())
				top.add(pointer);
			else
				bottom.add(pointer);
		}

		Poynt topLeft = top.get(0).getX() > top.get(1).getX() ? top.get(1) : top.get(0);
		Poynt topRight = top.get(0).getX() > top.get(1).getX() ? top.get(0) : top.get(1);
		Poynt bottomLeft = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(1) : bottom.get(0);
		Poynt bottomRight = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(0) : bottom.get(1);

		corners.clear();

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

		if (corners.isEmpty()) {
			return;
		}

		corners.get(CORNER_ONE).setNext(corners.get(CORNER_TWO));
		corners.get(CORNER_TWO).setNext(corners.get(CORNER_THREE));
		corners.get(CORNER_THREE).setNext(corners.get(CORNER_FOUR));
		corners.get(CORNER_FOUR).setNext(corners.get(CORNER_ONE));
	}
	
	public void setEdges(){
		if (corners.isEmpty()) {
			return;
		}
		
		Lyne top = new Lyne(corners.get(CORNER_ONE),corners.get(CORNER_TWO));
		Lyne right = new Lyne(corners.get(CORNER_TWO),corners.get(CORNER_THREE));
		Lyne bottom =new Lyne(corners.get(CORNER_THREE),corners.get(CORNER_FOUR));
		Lyne left = new Lyne(corners.get(CORNER_FOUR),corners.get(CORNER_ONE));
		
		edges.add(top);
		edges.add(right);
		edges.add(bottom);
		edges.add(left);
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
	 * Get border edges.
	 * 
	 * @return
	 */
	public List<Lyne> getEdges() {
		return edges;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// if restart re-initialize crop window.
		if (init) {
			init();
			init = false;
		}

		updateCorners(canvas);
		updateCenters(canvas);
		// updateSnapView(canvas);
		updateEdges(canvas);
		updateShaderBackground(canvas);
	}

	/**
	 * Update (Draw) corner handles (or circles).
	 * 
	 * @param canvas
	 */
	private void updateCorners(Canvas canvas) {

		if (corners.isEmpty()) {
			return;
		}

		for (Poynt point : corners) {
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) mDefaultCornerHandleRadius * 2, Helper.getCornerPaint(context));
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) (mDefaultCornerHandleRadius * 0.5),
					Helper.getCornerLightPaint(context));
		}
	}

	/**
	 * Update (Draw) corner handles (or circles).
	 * 
	 * @param canvas
	 */
	private void updateCenters(Canvas canvas) {

		if (corners.isEmpty()) {
			return;
		}

		for (Poynt point : corners) {
			canvas.drawCircle((float) (point.getCenter(point.getNext()).getX()), (float) (point.getCenter(point.getNext()).getY()),
					(float) (mDefaultCornerHandleRadius * 0.5), Helper.getCornerLightPaint(context));
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
		if (corners.isEmpty()) {
			return;
		}

		Paint paint = Helper.getCornerLightPaint(context);

		Path linePath = new Path();

		// top
		linePath.moveTo(0, 0);
		linePath.lineTo((float) mWidth, 0);
		linePath.lineTo((float) mWidth, (float) (corners.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(CORNER_TWO).getX(), (float) (corners.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(CORNER_ONE).getX(), (float) (corners.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo(0, (float) (corners.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// left
		linePath.reset();
		linePath.moveTo(0, (float) (corners.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(CORNER_ONE).getX() - mDefaultOffset / 4),
				(float) (corners.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(CORNER_FOUR).getX() - mDefaultOffset / 4),
				(float) (corners.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) (corners.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// right
		linePath.reset();
		linePath.moveTo((float) (corners.get(CORNER_TWO).getX() + mDefaultOffset / 4),
				(float) (corners.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(CORNER_THREE).getX() + mDefaultOffset / 4),
				(float) (corners.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// bottom
		linePath.reset();
		linePath.moveTo(0, (float) (corners.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) (corners.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(CORNER_THREE).getX(), (float) (corners.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(CORNER_FOUR).getX(), (float) (corners.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

	}

	/**
	 * Update (Draw) border lines of crop window
	 * 
	 * @param canvas
	 */
	private void updateEdges(Canvas canvas) {

		if (corners.isEmpty()) {
			return;
		}

		Path linePath = new Path();

		// top
		linePath.moveTo((float) (corners.get(CORNER_ONE).getX()), (float) (corners.get(CORNER_ONE).getY()));

		// right
		linePath.lineTo((float) (corners.get(CORNER_TWO).getX()), (float) (corners.get(CORNER_TWO).getY()));

		// bottom
		linePath.lineTo((float) (corners.get(CORNER_THREE).getX()), (float) (corners.get(CORNER_THREE).getY()));

		// left
		linePath.lineTo((float) (corners.get(CORNER_FOUR).getX()), (float) (corners.get(CORNER_FOUR).getY()));

		linePath.close();

		Paint paint = Helper.getLinePaint(context);
		if (warn) {
			paint = Helper.getLineErrorPaint(context);
		}

		canvas.drawPath(linePath, paint);
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
	 * On touch up.
	 */
	private void onTouchUp() {
		if (mCornerHandle == null) {
			return;
		}

		mCornerHandle = null;
		mCornerHandleIndex = -1;
		invalidate();
	}

	/**
	 * On touch down.
	 * 
	 * @param p
	 */
	private void onTouchDown(Poynt p) {

		mCornerHandle = getPressedCornerHandle(p);

		if (mCornerHandle == null) {
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

		if (mCornerHandle == null)
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

		if (corners.indexOf(mCornerHandle) == -1) {
			return;
		}

		// updateSnapView()

		mCornerHandleIndex = corners.indexOf(mCornerHandle);
		corners.get(mCornerHandleIndex).setXY(p.getX(), p.getY());

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

		Poynt diagonalPoint = mCornerHandle.getNext().getNext();
		double minimumLength = 2 * (mCornerHandle.getDistance(diagonalPoint) / 3);

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

	public class Lyne {

		private Poynt start;

		private Poynt end;

		private double sX;

		private double sY;

		private double eX;

		private double eY;

		public Lyne(Poynt s, Poynt e) {
			this.start = s;
			this.end = s;

			setSX(start.getX());
			setSY(start.getY());
			setEX(end.getX());
			setEY(end.getY());
		}

		public void setSXY(double sX, double sY) {
			setSX(sX);
			setSY(sY);
		}

		public void setEXY(double eX, double eY) {
			setEX(eX);
			setEY(eY);
		}

		public void setStart(Poynt s) {
			this.start = s;
			this.sX = s.getX();
			this.sY = s.getY();
		}

		public Poynt getStart() {
			return this.start;
		}

		public void setEnd(Poynt e) {
			this.end = e;
			this.eX = e.getX();
			this.eY = e.getY();
		}

		public Poynt getEnd() {
			return this.end;
		}

		public void setSX(double sX) {
			this.sX = sX;
			this.start.setX(sX);
		}

		public void setSY(double sY) {
			this.sY = sY;
			this.start.setX(sY);
		}

		public void setEX(double eX) {
			this.eX = eX;
			this.start.setX(sY);
		}

		public void setEY(double eY) {
			this.eY = eY;
			this.end.setX(sY);
		}

		public double getSX() {
			return this.sX;
		}

		public double getSY() {
			return this.sY;
		}

		public double getEX() {
			return this.eX;
		}

		public double getEY() {
			return this.eX;
		}

		public Poynt getIntersection(Lyne l) {
			Poynt iCPoint = new Poynt();
			double nSX = l.getSX();
			double nSY = l.getSY();
			double nEX = l.getEX();
			double nEY = l.getEY();

			float d = (float) (((sX - eX) * (nSY - nEY)) - ((sY - eY) * (nSX - nEX)));

			if (d != 0) {
				iCPoint.setX(((sX * eY - sY * eX) * (nSX - nEX) - (sX - eX) * (nSX * nEY - nSY * nEX)) / d);
				iCPoint.setY(((sX * eY - sY * eX) * (nSY - nEY) - (sY - eY) * (nSX * nEY - nSY * nEX)) / d);
			} else {
				iCPoint.setXY(-1, -1);
			}

			return iCPoint;
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
