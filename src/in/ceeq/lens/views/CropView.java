package in.ceeq.lens.views;

import in.ceeq.lens.helpers.Helper;
import in.ceeq.lens.helpers.Poynt;
import in.ceeq.lens.helpers.Helper.Corner;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class CropView extends View {

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

	// Source bitmap to crop.
	private Bitmap mBitmap;

	// Current point under touch.
	private Poynt mPoynt;

	// Current Image view for cropping.
	private ImageView mImageView;

	// Flag for monitoring view touch.
	private boolean mIsTouched;

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mDefault) {
			setDefaultCorners();
			mDefault = false;
		}

		updateCorners(canvas);
		updateCenters(canvas);
		updateEdges(canvas);
		updateShaderBackground(canvas);

		if (mIsTouched) {
			updateSnapView(canvas);
		}
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
	 * Set source bitmap for current crop.
	 * 
	 * @param b
	 */
	public void setBitmap(Bitmap bitmap) {
		this.mBitmap = bitmap;
	}

	/**
	 * Set image view for current crop.
	 * 
	 * @param b
	 */
	public void setImageView(ImageView imageView) {
		this.mImageView = imageView;
	}

	/**
	 * Set touch corners.
	 * 
	 * @param corners
	 */
	public void setCorners(List<Poynt> corners) {

		Log.w("Tagged", "Detected corners " + corners.toString());

		this.corners.clear();

		Poynt.sortPoynts(corners);

		this.corners = corners;

		setCorners();
	}

	/**
	 * Initialize touch corners .
	 */
	public void setCorners() {

		if (corners.isEmpty()) {
			return;
		}

		corners.get(Corner.TOP_LEFT).setNext(corners.get(Corner.TOP_RIGHT));
		corners.get(Corner.TOP_RIGHT).setNext(corners.get(Corner.BOTTOM_RIGHT));
		corners.get(Corner.BOTTOM_RIGHT).setNext(corners.get(Corner.BOTTOM_LEFT));
		corners.get(Corner.BOTTOM_LEFT).setNext(corners.get(Corner.TOP_LEFT));
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

		BitmapShader shader = new BitmapShader(mBitmap, TileMode.CLAMP, TileMode.CLAMP);
		Matrix matrix = new Matrix();
		Paint shaderPaint = new Paint();
		shaderPaint.setShader(shader);

		Poynt mImagePoynt = ((LensView) getParent()).getPointsOnImage(new Poynt(mPoynt.getX(),mPoynt.getY() ), mImageView);
		matrix.reset();
		matrix.postScale(2f, 2f, (float) mImagePoynt.getX(), (float) mImagePoynt.getY());

		shader.setLocalMatrix(matrix);

		canvas.drawCircle(50, 50, 200.0f, shaderPaint);
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
		linePath.lineTo((float) mWidth, (float) (corners.get(Corner.TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(Corner.TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(Corner.TOP_RIGHT).getX(), (float) (corners.get(Corner.TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(Corner.TOP_LEFT).getX(), (float) (corners.get(Corner.TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo(0, (float) (corners.get(Corner.TOP_LEFT).getY() - mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// left
		linePath.reset();
		linePath.moveTo(0, (float) (corners.get(Corner.TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(Corner.TOP_LEFT).getX() - mDefaultOffset / 4), (float) (corners.get(Corner.TOP_LEFT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(Corner.BOTTOM_LEFT).getX() - mDefaultOffset / 4),
				(float) (corners.get(Corner.BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) (corners.get(Corner.BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// right
		linePath.reset();
		linePath.moveTo((float) (corners.get(Corner.TOP_RIGHT).getX() + mDefaultOffset / 4), (float) (corners.get(Corner.TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(Corner.TOP_RIGHT).getY() - mDefaultOffset / 4));
		linePath.lineTo((float) mWidth, (float) (corners.get(Corner.BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) (corners.get(Corner.BOTTOM_RIGHT).getX() + mDefaultOffset / 4),
				(float) (corners.get(Corner.BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		canvas.drawPath(linePath, paint);

		// bottom
		linePath.reset();
		linePath.moveTo(0, (float) (corners.get(Corner.BOTTOM_LEFT).getY() + mDefaultOffset / 4));
		linePath.lineTo(0, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) mHeight);
		linePath.lineTo((float) mWidth, (float) (corners.get(Corner.BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(Corner.BOTTOM_RIGHT).getX(), (float) (corners.get(Corner.BOTTOM_RIGHT).getY() + mDefaultOffset / 4));
		linePath.lineTo((float) corners.get(Corner.BOTTOM_LEFT).getX(), (float) (corners.get(Corner.BOTTOM_LEFT).getY() + mDefaultOffset / 4));
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
		linePath.moveTo((float) (corners.get(Corner.TOP_LEFT).getX()), (float) (corners.get(Corner.TOP_LEFT).getY()));

		// right
		linePath.lineTo((float) (corners.get(Corner.TOP_RIGHT).getX()), (float) (corners.get(Corner.TOP_RIGHT).getY()));

		// bottom
		linePath.lineTo((float) (corners.get(Corner.BOTTOM_RIGHT).getX()), (float) (corners.get(Corner.BOTTOM_RIGHT).getY()));

		// left
		linePath.lineTo((float) (corners.get(Corner.BOTTOM_LEFT).getX()), (float) (corners.get(Corner.BOTTOM_LEFT).getY()));

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

		mIsTouched = false;

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

		mIsTouched = true;

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

		mPoynt = p;

		mIsTouched = true;

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
}
