package com.practo.lens.helpers;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.util.TypedValue;

/**
 * Helper class for common data.
 * 
 * @author x
 * 
 */
public class Helper {

	public static final int DEFAULT_CORNER_RADIUS = 10;

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
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CORNER_RADIUS, context.getResources().getDisplayMetrics());
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
		Log.i("LensApp", message);
	}
	
	public class Corner {
		// Corner index one
		public static final int TOP_LEFT = 0;

		// Corner index two
		public static final int TOP_RIGHT = 1;

		// Corner index three
		public static final int BOTTOM_RIGHT = 2;

		// Corner index four
		public static final int BOTTOM_LEFT = 3;
	}
}
