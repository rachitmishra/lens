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
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.practo.lens.helpers.Helper;
import com.practo.lens.helpers.Poynt;
import com.practo.lens.view.LensView;

public class Home extends Activity {

	private LensView sourceView, resultView;

	private Mat sourceMatrix;

	private Bitmap sourceBitmap, resultBitmap;

	private Button crop, cancel, edge, capture, select;

	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private static final int REQUEST_IMAGE_SELECT = 11;

	private List<Poynt> detectedCorners = new ArrayList<Poynt>();

	private static final String TAG = "Tagged";

	private double mDefaultAspectRatio;

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
		sourceView = (LensView) findViewById(R.id.sourceV);
		resultView = (LensView) findViewById(R.id.resultV);
		crop = (Button) findViewById(R.id.crop);
		cancel = (Button) findViewById(R.id.cancel);
		edge = (Button) findViewById(R.id.edge);
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

		edge.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				try {
					getPicture();

					sourceMatrix = new Mat(mHeight, mWidth, CvType.CV_8UC4);

					Utils.bitmapToMat(sourceBitmap, sourceMatrix);

					doEdgeDetect(sourceMatrix);

					sourceView.setImageBitmap(sourceBitmap);

					sourceView.setCorners(detectedCorners);

				} catch (Exception e) {

					if (sourceBitmap == null) {
						Toast.makeText(Home.this, "Please select or click a picture !", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(Home.this, "Awesome. You crashed me.", Toast.LENGTH_SHORT).show();
					}

					e.printStackTrace();
				}

			}

		});

		crop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				try {

				} catch (Exception e) {
					if (sourceBitmap == null) {
						Toast.makeText(Home.this, "Please select or click a picture !", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(Home.this, "Awesome. You crashed me.", Toast.LENGTH_SHORT).show();
					}

					e.printStackTrace();

				}

			}

		});

		capture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

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
				sourceView.setDefault(true);
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
		sourceView.setDefault(true);
		int layoutWidth = sourceView.getWidth();
		int layoutHeight = sourceView.getHeight();

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
			sourceView.setImageBitmap(bitmap);
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
			sourceView.setImageBitmap(bitmap);
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

	public void doEdgeDetect(Mat tempMatrix) {

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

		resultBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);

		Imgproc.cvtColor(tempMatrix, tempMatrix, Imgproc.COLOR_BGR2GRAY);

		// Imgproc.threshold(tempMatrix, tempMatrix, 155, 255,
		// Imgproc.THRESH_BINARY_INV);

		Imgproc.Canny(tempMatrix, tempMatrix, 50, 50);

		Utils.matToBitmap(tempMatrix, resultBitmap);

		resultView.setImageBitmap(resultBitmap);

		// Imgproc.blur(tempMatrix, tempMatrix, tempMatrix.size());

		// Imgproc.dilate(tempMatrix, tempMatrix, new Mat(), new Point(-1, -1),
		// 1);

		Imgproc.findContours(tempMatrix, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		double bigContour = -1;

		MatOfPoint tempContour = contours.get(0);
		MatOfPoint2f approxCurveMatrix = new MatOfPoint2f();

		for (int idx = 0; idx < contours.size(); idx++) {

			tempContour = contours.get(idx);
			double contourArea = Imgproc.contourArea(tempContour);

			if (contourArea > bigContour) {

				MatOfPoint2f newCurveMatrix = new MatOfPoint2f(tempContour.toArray());
				int contourSize = (int) tempContour.total();
				MatOfPoint2f tempCurveMatrix = new MatOfPoint2f();

				Imgproc.approxPolyDP(newCurveMatrix, tempCurveMatrix, contourSize * 0.05, true);
				if (tempCurveMatrix.total() == 4) {
					bigContour = contourArea;
					approxCurveMatrix = tempCurveMatrix;
				}
			}
		}

		double[] tempDouble = approxCurveMatrix.get(0, 0);
		Poynt pA = new Poynt(tempDouble[0], tempDouble[1]);

		tempDouble = approxCurveMatrix.get(1, 0);
		Poynt pB = new Poynt(tempDouble[0], tempDouble[1]);

		tempDouble = approxCurveMatrix.get(2, 0);
		Poynt pC = new Poynt(tempDouble[0], tempDouble[1]);

		tempDouble = approxCurveMatrix.get(3, 0);
		Poynt pD = new Poynt(tempDouble[0], tempDouble[1]);

		detectedCorners.add(pA);
		detectedCorners.add(pB);
		detectedCorners.add(pC);
		detectedCorners.add(pD);
	}

	public Mat deSkew(Mat tempMatrix, double angle) {
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(tempMatrix.cols() / 2F, tempMatrix.rows() / 2F), angle, 1);
		Imgproc.warpAffine(tempMatrix, tempMatrix, rotationMatrix, tempMatrix.size(), Imgproc.INTER_CUBIC);
		return tempMatrix;
	}

	public Mat doPerspectiveCrop(Mat tempMatrix) {

		List<Poynt> cropCorners = new ArrayList<Poynt>();
		double skewAngle = 0;

		cropCorners = sourceView.getPointsOnImage(Poynt.sortPoynts(sourceView.getCropCorners()));
		skewAngle = Poynt.getSkewAngle(sourceView.getCropCorners());

		Log.w("Tagged", "Skew angle " + skewAngle);

		List<Point> inputMatrixPoints = new ArrayList<Point>();

		Point iPointTopLeft = new Point(cropCorners.get(0).getX(), cropCorners.get(0).getY());
		Point iPointTopRight = new Point(cropCorners.get(1).getX(), cropCorners.get(1).getY());
		Point iPointBottomRight = new Point(cropCorners.get(2).getX(), cropCorners.get(2).getY());
		Point iPointBottomLeft = new Point(cropCorners.get(3).getX(), cropCorners.get(3).getY());

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

		Imgproc.warpPerspective(tempMatrix, resultMatrix, transformationMatrix, resultMatrix.size());

		// resultMatrix = deSkew(resultMatrix, skewAngle);

		return resultMatrix;
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
