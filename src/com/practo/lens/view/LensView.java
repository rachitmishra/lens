package com.practo.lens.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.practo.lens.R;
import com.practo.lens.helpers.Poynt;

public class LensView extends FrameLayout {

	private ImageView mImageView;

	private CropView mCropView;

	public LensView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.view_image_crop, this, true);

		mImageView = (ImageView) getChildAt(0);
		mCropView = (CropView) getChildAt(1);
		mCropView.setImageView(mImageView);
	}

	public void setImageBitmap(Bitmap b) {
		mImageView.setImageBitmap(b);
		mCropView.setBitmap(b);
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
	
	public List<Poynt> getCropCorners() {
		return mCropView.getCorners();
	}
	
	public List<Poynt> getPointsOnImage(List<Poynt> corners) {
		List<Poynt> temp = new ArrayList<Poynt>();
		Matrix inverse = new Matrix();
		mImageView.getImageMatrix().invert(inverse);

		for (Poynt pointer : corners) {
			float[] newCorner = new float[] { (float) pointer.getX(), (float) pointer.getY() };
			inverse.mapPoints(newCorner);
			temp.add(new Poynt(newCorner[0], newCorner[1]));
		}

		return temp;
	}

	public Poynt getPointsOnImage(Poynt p, ImageView imageView) {
		Poynt temp = new Poynt();
		Matrix inverse = new Matrix();
		imageView.getImageMatrix().invert(inverse);

		float[] newCorner = new float[] { (float) p.getX(), (float) p.getY() };
		inverse.mapPoints(newCorner);

		temp.setXY(newCorner[0], newCorner[1]);

		return temp;
	}
}
