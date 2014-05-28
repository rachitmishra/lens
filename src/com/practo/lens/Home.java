package com.practo.lens;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.practo.lens.view.CropView;
import com.practo.lens.view.CropView.Helper;
import com.practo.lens.view.CropView.Lyne;
import com.practo.lens.view.CropView.Poynt;

public class Home extends Activity {

	private CropView captureCropView, edgeCropView;

	private ImageView captureImageView;

	private ImageView cropImageView;

	private ImageView edgeImageView;

	private Mat sourceMatrix, resultMatrix;

	private Bitmap sourceBitmap, resultBitmap;

	private Button crop, cancel, edge, capture, select;

	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private static final int REQUEST_IMAGE_SELECT = 11;

	private List<Poynt> cropCorners = new ArrayList<Poynt>();
	
	private List<Lyne> cropLines = new ArrayList<Lyne>();

	private List<Poynt> detectedCropCorners = new ArrayList<Poynt>();

	private static final String TAG = "Tagged";

	private double mDefaultAspectRatio, mWidthRatio, mHeightRatio;

	private double mIntrinsicOffset;

	private int mHeight;

	private int mWidth;

	private File photoFile;

	private String mCurrentPhotoPath;
	
	public BaseLoaderCallback mOpenCVLoaderCallback = new BaseLoaderCallback(this) {

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV Manager connected.");
				break;
			case LoaderCallbackInterface.INIT_FAILED:
				Log.i(TAG, "Init failed.");
				break;
			default:
				super.onManagerConnected(status);

			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupUI();
		setupListeners();
	}

	private void setupUI() {
		captureImageView = (ImageView) findViewById(R.id.imageView);
		captureCropView = (CropView) findViewById(R.id.cropView);
		edgeCropView = (CropView) findViewById(R.id.edgeCropView);
		edgeImageView = (ImageView) findViewById(R.id.edgeImageView);
		cropImageView = (ImageView) findViewById(R.id.cropImageView);
		crop = (Button) findViewById(R.id.crop);
		cancel = (Button) findViewById(R.id.cancel);
		//edge = (Button) findViewById(R.id.edge);
		capture = (Button) findViewById(R.id.capture);
		select = (Button) findViewById(R.id.select);
	}

	private void setupListeners() {

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Home.this.finish();
			}

		});

//		edge.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//
//				try {
//					getPicture();
//
//					mWidthRatio = mWidth / captureImageView.getWidth();
//					mHeightRatio = mHeight / captureImageView.getHeight();
//
//					sourceMatrix = new Mat(mHeight, mWidth, CvType.CV_8UC4);
//
//					Utils.bitmapToMat(sourceBitmap, sourceMatrix);
//
//					// doEdgeDetect(sourceMatrix);
//
//					edgeImageView.setImageBitmap(sourceBitmap);
//
//					edgeCropView.setCornerHandles(detectedCropCorners);
//
//					edgeCropView.invalidate();
//
//				} catch (Exception e) {
//
//					if (sourceBitmap == null) {
//						Toast.makeText(Home.this, "Please select or click a picture !", Toast.LENGTH_SHORT).show();
//					} else {
//						Toast.makeText(Home.this, "Whoa ! Lens crashed !", Toast.LENGTH_SHORT).show();
//					}
//
//					e.printStackTrace();
//				}
//
//			}
//
//		});

		crop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				try {
					getPicture();

					mWidthRatio = mWidth / captureImageView.getWidth();
					mHeightRatio = mHeight / captureImageView.getHeight();

					sourceMatrix = new Mat(mHeight, mWidth, CvType.CV_8UC4);

					Utils.bitmapToMat(sourceBitmap, sourceMatrix);

					resultMatrix = doPerspectiveCrop(sourceMatrix);

					Utils.matToBitmap(resultMatrix, resultBitmap);

					Helper.log("Output bitmap width " + mWidth + " & height " + mHeight);
					Helper.log("Capture imageview width " + captureImageView.getWidth() + " & height " + captureImageView.getHeight());
					Helper.log("Ratio width " + mWidthRatio + " & height " + mHeightRatio);

					cropImageView.setImageBitmap(resultBitmap);

				} catch (Exception e) {
					if (sourceBitmap == null) {
						Toast.makeText(Home.this, "Please select or click a picture !", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(Home.this, "Whoa ! Lens crashed !", Toast.LENGTH_SHORT).show();
					}

					e.printStackTrace();

				}

			}

		});

		capture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				captureCropView.setVisibility(View.VISIBLE);
				captureCropView.init();
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

					try {
						photoFile = createImageFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
					takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
					startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
				}

			}

		});

		select.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				captureCropView.setVisibility(View.VISIBLE);
				captureCropView.init();
				Intent selectPictureIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

				if (selectPictureIntent.resolveActivity(getPackageManager()) != null) {

					startActivityForResult(selectPictureIntent, REQUEST_IMAGE_SELECT);
				}

			}

		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && (requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT)) {
			setPicture(requestCode, data);
		}
	}

	private File createImageFile() throws IOException {

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";

		File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

		File image = File.createTempFile(imageFileName, ".jpg", storageDir);

		mCurrentPhotoPath = image.getAbsolutePath();

		return image;
	}

	private void getPicture() {
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		sourceBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);

		mWidth = bmOptions.outWidth;
		mHeight = bmOptions.outHeight;

		Log.w(TAG, "Input bitmap width " + mWidth + " & height " + mHeight);
	}

	private void setPicture(int requestCode, Intent data) {
		captureCropView.setVisibility(View.VISIBLE);
		captureCropView.init();
		int layoutWidth = captureImageView.getWidth();
		int layoutHeight = captureImageView.getHeight();

		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		int photoWidth = 0;
		int photoHeight = 0;

		Bitmap bitmap = null;

		switch (requestCode) {
		case REQUEST_IMAGE_CAPTURE:

			BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

			photoWidth = bmOptions.outWidth;
			photoHeight = bmOptions.outHeight;

			mDefaultAspectRatio = Math.min(photoWidth / layoutWidth, photoHeight / layoutHeight);

			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = (int) mDefaultAspectRatio;
			bmOptions.inPurgeable = true;

			bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
			captureImageView.setImageBitmap(bitmap);
			break;
		case REQUEST_IMAGE_SELECT:
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			mCurrentPhotoPath = cursor.getString(columnIndex);

			BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

			photoWidth = bmOptions.outWidth;
			photoHeight = bmOptions.outHeight;

			mDefaultAspectRatio = Math.min(photoWidth / layoutWidth, photoHeight / layoutHeight);

			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = (int) mDefaultAspectRatio;
			bmOptions.inPurgeable = true;

			cursor.close();
			bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
			captureImageView.setImageBitmap(bitmap);
			break;

		default:
			break;
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mOpenCVLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager.");
		}
	}

	// public Mat doEdgeDetect(Mat tempMatrix) {
	//
	// List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	//
	// Mat lineMatrix = new Mat();
	//
	// resultBitmap = Bitmap.createBitmap(mWidth, mHeight,
	// Bitmap.Config.ARGB_8888);
	//
	// Imgproc.medianBlur(tempMatrix, tempMatrix, 3);
	//
	// Imgproc.cvtColor(tempMatrix, tempMatrix, Imgproc.COLOR_BGR2GRAY);
	//
	// Imgproc.Canny(tempMatrix, tempMatrix, 200, 200);
	//
	// Imgproc.dilate(tempMatrix, tempMatrix, new Mat(), new Point(-1, -1), 1);
	//
	// Utils.matToBitmap(tempMatrix, resultBitmap);
	//
	// cropImageView.setImageBitmap(resultBitmap);
	//
	// //Imgproc.filter2D(tempMatrix, tempMatrix, CvType.CV_8U, tempMatrix.t());
	//
	// // Imgproc.HoughLinesP(tempMatrix, lineMatrix, 1, Math.PI / 180, 200,
	// // 50, 20);
	//
	// Log.w("Tagged", "" + lineMatrix.size());
	//
	// Imgproc.findContours(tempMatrix, contours, new Mat(), Imgproc.RETR_LIST,
	// Imgproc.CHAIN_APPROX_SIMPLE);
	//
	// double bigContour = -1;
	//
	// MatOfPoint tempContour = contours.get(0);
	// MatOfPoint2f approxCurveMatrix = new MatOfPoint2f();
	//
	// for (int idx = 0; idx < contours.size(); idx++) {
	//
	//
	// tempContour = contours.get(idx);
	// double contourArea = Imgproc.contourArea(tempContour);
	//
	// if (contourArea > bigContour) {
	//
	// MatOfPoint2f newCurveMatrix = new MatOfPoint2f(tempContour.toArray());
	// int contourSize = (int) tempContour.total();
	// MatOfPoint2f tempCurveMatrix = new MatOfPoint2f();
	//
	// Imgproc.approxPolyDP(newCurveMatrix, tempCurveMatrix, contourSize * 0.05,
	// true);
	// if (tempCurveMatrix.total() == 4) {
	// bigContour = contourArea;
	// approxCurveMatrix = tempCurveMatrix;
	// }
	// }
	// }
	//
	// double[] tempDouble = approxCurveMatrix.get(0, 0);
	// Pointer pA = captureCropView.new Pointer(tempDouble[0] / mWidthRatio,
	// tempDouble[1] / mHeightRatio);
	//
	// tempDouble = approxCurveMatrix.get(1, 0);
	// Pointer pB = captureCropView.new Pointer(tempDouble[0] / mWidthRatio,
	// tempDouble[1] / mHeightRatio);
	//
	// tempDouble = approxCurveMatrix.get(2, 0);
	// Pointer pC = captureCropView.new Pointer(tempDouble[0] / mWidthRatio,
	// tempDouble[1] / mHeightRatio);
	//
	// tempDouble = approxCurveMatrix.get(3, 0);
	// Pointer pD = captureCropView.new Pointer(tempDouble[0] / mWidthRatio,
	// tempDouble[1] / mHeightRatio);
	//
	// detectedCropCorners.add(pA);
	// detectedCropCorners.add(pB);
	// detectedCropCorners.add(pC);
	// detectedCropCorners.add(pD);
	//
	// return null;
	// }

	public void computeSkew(Mat tempMatrix) {
		Imgproc.cvtColor(tempMatrix, tempMatrix, Imgproc.COLOR_BGR2GRAY);
		Core.bitwise_not(tempMatrix, tempMatrix);
	}

	public Mat doPerspectiveCrop(Mat sourceMatrix) {

		if (captureCropView != null) {
			cropCorners = captureCropView.sortPoints(captureCropView.getCorners());
		}

		List<Point> inputMatrixPoints = new ArrayList<Point>();

		mIntrinsicOffset = 8;
		Point iPointTopLeft = new Point((cropCorners.get(0).getX() - mIntrinsicOffset), (cropCorners.get(0).getY() * (mHeightRatio * 1.1)));
		Point iPointTopRight = new Point(((cropCorners.get(1).getX() - mIntrinsicOffset) * (mWidthRatio * 1.16)),
				(cropCorners.get(1).getY() * (mHeightRatio * 1.1)));
		Point iPointBottomRight = new Point(((cropCorners.get(2).getX() - mIntrinsicOffset) * (mWidthRatio * 1.16)),
				(cropCorners.get(2).getY() * (mHeightRatio)));
		Point iPointBottomLeft = new Point((cropCorners.get(3).getX() - mIntrinsicOffset), (cropCorners.get(3).getY() * (mHeightRatio)));

		inputMatrixPoints.add(iPointTopLeft);
		inputMatrixPoints.add(iPointTopRight);
		inputMatrixPoints.add(iPointBottomRight);
		inputMatrixPoints.add(iPointBottomLeft);

		Log.w(TAG, "Input points " + inputMatrixPoints.toString());

		Mat inputMatrix = Converters.vector_Point2f_to_Mat(inputMatrixPoints);

		double widthTop = cropCorners.get(0).getDistance(cropCorners.get(1));
		double widthBottom = cropCorners.get(2).getDistance(cropCorners.get(3));
		int maxWidth = (int) (((widthTop > widthBottom) ? widthTop : widthBottom));

		double heightLeft = cropCorners.get(0).getDistance(cropCorners.get(3));
		double heightRight = cropCorners.get(1).getDistance(cropCorners.get(2));
		int maxHeight = (int) (((heightLeft > heightRight) ? heightLeft : heightRight));

		resultBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);

		Mat resultMatrix = new Mat(maxHeight, maxWidth, CvType.CV_8UC3);

		List<Point> outputMatrixPoints = new ArrayList<Point>();

		Point oPointTopLeft = new Point(0, 0);
		Point oPointTopRight = new Point(resultMatrix.cols(), 0);
		Point oPointBottomRight = new Point(resultMatrix.cols(), resultMatrix.rows());
		Point oPointBottomLeft = new Point(0, resultMatrix.rows());

		outputMatrixPoints.add(oPointTopLeft);
		outputMatrixPoints.add(oPointTopRight);
		outputMatrixPoints.add(oPointBottomRight);
		outputMatrixPoints.add(oPointBottomLeft);

		Helper.log("Output points " + outputMatrixPoints.toString());
		Helper.log("Output width " + maxWidth + " & height " + maxHeight);

		Mat outputMatrix = Converters.vector_Point2f_to_Mat(outputMatrixPoints);

		Mat transformationMatrix = Imgproc.getPerspectiveTransform(inputMatrix, outputMatrix);

		Imgproc.warpPerspective(sourceMatrix, resultMatrix, transformationMatrix, resultMatrix.size(), Imgproc.INTER_BITS2);

		return brighten(resultMatrix);
	}

	public Mat sharpen(Mat tempMatrix) {
		Imgproc.GaussianBlur(tempMatrix, tempMatrix, new Size(0, 0), 3);
		Core.addWeighted(tempMatrix, 1.5, tempMatrix, -0.5, 0, tempMatrix);
		return tempMatrix;
	}

	public Mat equalize(Mat tempMatrix) {
		Imgproc.equalizeHist(tempMatrix, tempMatrix);
		return tempMatrix;
	}

	public Mat brighten(Mat tempMatrix) {
		Imgproc.cvtColor(tempMatrix, tempMatrix, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(tempMatrix, tempMatrix);
		tempMatrix.convertTo(tempMatrix, -1, 1.5, 40);
		return tempMatrix;
	}

}
