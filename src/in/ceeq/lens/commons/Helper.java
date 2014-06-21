package in.ceeq.lens.commons;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

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

	private static final int DEFAULT_CORNER_OFFSET = 20;

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
	
	public static List<Poynt> getPointsOnImage(List<Poynt> corners, ImageView imageView) {
		List<Poynt> temp = new ArrayList<Poynt>();
		Matrix inverse = new Matrix();
		imageView.getImageMatrix().invert(inverse);

		for (Poynt pointer : corners) {
			float[] newCorner = new float[] { (float) pointer.getX(), (float) pointer.getY() };
			inverse.mapPoints(newCorner);
			temp.add(new Poynt(newCorner[0], newCorner[1]));
		}

		return temp;
	}

	public static Poynt getPointsOnImage(Poynt p, ImageView imageView) {
		Poynt temp = new Poynt();
		Matrix inverse = new Matrix();
		imageView.getMatrix().invert(inverse);

		float[] newCorner = new float[] { (float) p.getX(), (float) p.getY() };
		inverse.mapPoints(newCorner);

		temp.setXY(newCorner[0], newCorner[1]);

		return temp;
	}

	public static void log(String message) {
		Log.i("LensApp", message);
	}
}
