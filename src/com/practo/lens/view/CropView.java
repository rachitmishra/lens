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
	private Pointer mCornerHandle;

	// Current corner handle index
	private int mCornerHandleIndex;

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
	private List<Pointer> cornerHandles = new ArrayList<Pointer>();

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
		cornerHandles.clear();
		cornerHandles.add(new Pointer(mDefaultCornerHandleOffset, mDefaultCornerHandleOffset));
		cornerHandles.add(new Pointer(mWidth - mDefaultCornerHandleOffset, mDefaultCornerHandleOffset));
		cornerHandles.add(new Pointer(mWidth - mDefaultCornerHandleOffset, mHeight - mDefaultCornerHandleOffset));
		cornerHandles.add(new Pointer(mDefaultCornerHandleOffset, mHeight - mDefaultCornerHandleOffset));
		setCornerHandles();
	}

	/**
	 * Set touch corners.
	 * 
	 * @param corners
	 */
	public void setCornerHandles(List<Pointer> corners) {
		
		cornerHandles.clear();
		sortPoints(corners);
		cornerHandles = corners;
		setCornerHandles();
		
		Log.w("Tagged", "Corners detected " + corners.toString());
	}
	
	/**
	 * Sort points.
	 * @param corners
	 * @return
	 */
	public List<Pointer> sortPoints(List<Pointer> corners){
		
		List<Pointer> top =  new ArrayList<Pointer>(), bottom =  new ArrayList<Pointer>();
		double cX = 0, cY = 0;

        for(Pointer pointer : corners) {
        	cX += pointer.getX();
        	cY += pointer.getY();
        }
        
        Pointer cPoint = new Pointer(cX / corners.size(), cY / corners.size());
		
		for (Pointer pointer : corners) {
			if (pointer.getY() < cPoint.getY())
                top.add(pointer);
            else
                bottom.add(pointer);
		}

        Pointer tl = top.get(0).getX() > top.get(1).getX() ? top.get(1) : top.get(0);
        Pointer tr = top.get(0).getX() > top.get(1).getX() ? top.get(0) : top.get(1);
        Pointer bl = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(1) : bottom.get(0);
        Pointer br =  bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(0) : bottom.get(1);

        corners.clear();
        
        corners.add(tl);
        corners.add(tr);
        corners.add(br);
        corners.add(bl);
		return corners;
	}

	/**
	 * Initialize touch corners .
	 */
	public void setCornerHandles() {

		if (cornerHandles.isEmpty()) {
			return;
		}

		cornerHandles.get(CORNER_ONE).setNext(cornerHandles.get(CORNER_TWO));
		cornerHandles.get(CORNER_TWO).setNext(cornerHandles.get(CORNER_THREE));
		cornerHandles.get(CORNER_THREE).setNext(cornerHandles.get(CORNER_FOUR));
		cornerHandles.get(CORNER_FOUR).setNext(cornerHandles.get(CORNER_ONE));
	}

	/**
	 * Get touch corners.
	 * 
	 * @return
	 */
	public List<Pointer> getCornerHandles() {
		return cornerHandles;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// if restart re-initialize crop window.
		if (init) {
			init();
			init = false;
		}

		updateCornerHandles(canvas);
		// updateSnapView(canvas);
		updateBorderLines(canvas);
		updateShaderBackground(canvas);
	}

	/**
	 * Update (Draw) corner handles (or circles).
	 * 
	 * @param canvas
	 */
	private void updateCornerHandles(Canvas canvas) {

		if (cornerHandles.isEmpty()) {
			return;
		}

		for (Pointer point : cornerHandles) {
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()), (float) mDefaultCornerHandleRadius * 2,
					Helper.getCornerPaint(context));
			canvas.drawCircle((float) (point.getX()), (float) (point.getY()),
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
		if (cornerHandles.isEmpty()) {
			return;
		}

		Paint paint = Helper.getCornerLightPaint(context);

		Path linePath = new Path();

		// top
		linePath.moveTo(0, 0);
		linePath.lineTo((float) mWidth, 0);
		linePath.lineTo((float) mWidth, (float) (cornerHandles.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (cornerHandles.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) cornerHandles.get(CORNER_TWO).getX(),
				(float) (cornerHandles.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) cornerHandles.get(CORNER_ONE).getX(),
				(float) (cornerHandles.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo(0, (float) (cornerHandles.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// left
		linePath.reset();
		linePath.moveTo(0, (float) (cornerHandles.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (cornerHandles.get(CORNER_ONE).getX() - mDefaultOffset / 4), (float) (cornerHandles
				.get(CORNER_ONE).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (cornerHandles.get(CORNER_FOUR).getX() - mDefaultOffset / 4), (float) (cornerHandles
				.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) (cornerHandles.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// right
		linePath.reset();
		linePath.moveTo((float) (cornerHandles.get(CORNER_TWO).getX() + mDefaultOffset / 4), (float) (cornerHandles
				.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (cornerHandles.get(CORNER_TWO).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (cornerHandles.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) (cornerHandles.get(CORNER_THREE).getX() + mDefaultOffset / 4), (float) (cornerHandles
				.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// bottom
		linePath.reset();
		linePath.moveTo(0, (float) (cornerHandles.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) (cornerHandles.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) cornerHandles.get(CORNER_THREE).getX(),
				(float) (cornerHandles.get(CORNER_THREE).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) cornerHandles.get(CORNER_FOUR).getX(),
				(float) (cornerHandles.get(CORNER_FOUR).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

	}

	/**
	 * Update (Draw) border lines of crop window
	 * 
	 * @param canvas
	 */
	private void updateBorderLines(Canvas canvas) {

		if (cornerHandles.isEmpty()) {
			return;
		}

		Path linePath = new Path();

		// top
		linePath.moveTo((float) (cornerHandles.get(CORNER_ONE).getX()), (float) (cornerHandles.get(CORNER_ONE).getY()));

		// right
		linePath.lineTo((float) (cornerHandles.get(CORNER_TWO).getX()), (float) (cornerHandles.get(CORNER_TWO).getY()));

		// bottom
		linePath.lineTo((float) (cornerHandles.get(CORNER_THREE).getX()),
				(float) (cornerHandles.get(CORNER_THREE).getY()));

		// left
		linePath.lineTo((float) (cornerHandles.get(CORNER_FOUR).getX()),
				(float) (cornerHandles.get(CORNER_FOUR).getY()));

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
			onTouchDown(new Pointer(event.getX(), event.getY()));
			return true;

		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < event.getHistorySize(); i++) {
				onTouchMove(new Pointer(event.getHistoricalX(i), event.getHistoricalY(i)));
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
	private void onTouchDown(Pointer p) {

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
	private Pointer getPressedCornerHandle(Pointer p) {

		Pointer currentTouchPoint = null;

		for (Pointer c : cornerHandles) {

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
	private void onTouchMove(Pointer p) {

		if (mCornerHandle == null)
			return;

		updateCropWindow(p);
	}

	/**
	 * Update crop window (corner handles).
	 * 
	 * @param p
	 */
	private void updateCropWindow(Pointer p) {

		if (isOverlapping(p) || isOutbound(p)) {
			return;
		}

		if (cornerHandles.indexOf(mCornerHandle) == -1) {
			return;
		}

		// updateSnapView()

		mCornerHandleIndex = cornerHandles.indexOf(mCornerHandle);
		cornerHandles.get(mCornerHandleIndex).setXY(p.getX(), p.getY());

		invalidate();
	}

	/**
	 * If current corner handle overlaps to other.
	 * 
	 * @param p
	 * @return
	 */
	private boolean isOverlapping(Pointer p) {
		for (Pointer point : cornerHandles) {
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
	private boolean isOutbound(Pointer p) {
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
	private boolean isDiagonal(Pointer p) {

		Pointer diagonalPoint = mCornerHandle.getNext().getNext();
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
	public class Pointer {

		// X coordinate
		private double x;

		// Y coordinate
		private double y;

		// Next point to current point.
		private Pointer next;
		
		/**
		 * Default Constructor.
		 */
		public Pointer() {
			this(0, 0);
		}
		
		/**
		 * Constructor
		 * @param points
		 */
		public Pointer(double[] points) {
			this();
			set(points);
		}
		
		/**
		 * Constructor
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
		public Pointer(double x, double y) {
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
		public void setNext(Pointer p) {
			this.next = p;
		}

		/**
		 * Get next point to this point.
		 * 
		 * @return next
		 */
		public Pointer getNext() {
			return next;
		}

		/**
		 * Get distance from this point.
		 * 
		 * @param p
		 * @return distance
		 */
		public double getDistance(Pointer p) {
			return Math.sqrt(Math.pow((p.getX() - this.x), 2) + Math.pow((p.getY() - this.y), 2));
		}
		
		/**
		 * Clone this point.
		 * @return point.
		 */
		public Point clone() {
			return new Point(x, y);
		}

		/**
		 * Check equality with other point.
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
			final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					DEFAULT_LINE_THICKNESS, context.getResources().getDisplayMetrics());

			final Paint borderPaint = new Paint();
			borderPaint.setColor(DEFAULT_LINE_COLOR);
			borderPaint.setStrokeWidth(lineThicknessPx);
			borderPaint.setStyle(Paint.Style.STROKE);
			borderPaint.setAntiAlias(true);
			return borderPaint;
		}

		public static Paint getLineErrorPaint(Context context) {
			final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					DEFAULT_LINE_THICKNESS, context.getResources().getDisplayMetrics());

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
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CIRCLE_RADIUS, context.getResources()
					.getDisplayMetrics());
		}

		public static float getLineThickness(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_LINE_THICKNESS, context
					.getResources().getDisplayMetrics());
		}

		public static float getOffset(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_OFFSET, context.getResources()
					.getDisplayMetrics());
		}

		public static float getCornerOffset(Context context) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CORNER_OFFSET, context.getResources()
					.getDisplayMetrics());
		}

	}

}
