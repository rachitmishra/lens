package in.ceeq.lens.views;

import in.ceeq.lens.R;
import in.ceeq.lens.commons.Poynt;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class CropFrame extends FrameLayout {

	private ImageView mImageView;

	private CropView mCropView;

	public CropFrame(Context context) {
		super(context);
		init(context);
	}

	public CropFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CropFrame(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void init(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.view_crop, this, true);

		mImageView = (ImageView) view.findViewById(R.id.imageView);
		mCropView = (CropView) view.findViewById(R.id.cropView);
		mCropView.setImageView(mImageView);
		setDefault(true);
	}

	public void setCropViewVisible(boolean visible) {
		if (visible) {
			mCropView.setVisibility(View.VISIBLE);
		} else {
			mCropView.setVisibility(View.GONE);
		}
	}

	public void setImageBitmap(Bitmap b) {
		mImageView.setImageBitmap(b);
		mCropView.setBitmap(b);
		setCropViewVisible(true);
	}

	public void setCorners(List<Poynt> corners) {
		mCropView.setCorners(corners);
		mCropView.invalidate();
	}

	public void setDefault(boolean mDefault) {
		mCropView.setDefault(mDefault);
		mCropView.invalidate();
	}

	public void setCropVisible(boolean mDefault) {
		if (mDefault) {
			mCropView.setVisibility(View.VISIBLE);
		} else {
			mCropView.setVisibility(View.GONE);
		}
	}

	public ImageView getImageView() {
		return mImageView;
	}

	public List<Poynt> getCropCorners() {
		return mCropView.getCorners();
	}
}
