package com.practo.lens;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.practo.lens.view.CropView;

public class MainActivity extends Activity {

	private ImageView captureView;

	private CropView cropView;

	private Mat sourceMatrix, tempMatrix, resultMatrix;

	private Bitmap sourceBitmap, resultBitmap;

	private Button crop, cancel, edge, capture;

	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private List<Point> source = new ArrayList<Point>();

	private ArrayList<CropView.Point> touchCorners, cornerPoints = new ArrayList<CropView.Point>();

	private static final String TAG = "Tagged";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		captureView = (ImageView) findViewById(R.id.captureView);
		cropView = (CropView) findViewById(R.id.cropView);
		crop = (Button) findViewById(R.id.crop);
		cancel = (Button) findViewById(R.id.cancel);
		edge = (Button) findViewById(R.id.edge);
		capture = (Button) findViewById(R.id.capture);

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				MainActivity.this.finish();
			}

		});

		edge.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				sourceBitmap = ((BitmapDrawable) captureView.getDrawable()).getBitmap();
				
				int width =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sourceBitmap.getWidth(), MainActivity.this.getResources().getDisplayMetrics());

				int height =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,sourceBitmap.getHeight(), MainActivity.this.getResources().getDisplayMetrics());

				Log.w(TAG, "Input bitmap width " + width + " & height " + height);

				sourceMatrix = new Mat(height, width, CvType.CV_8UC4);

				Utils.bitmapToMat(sourceBitmap, sourceMatrix);

				detectCorners(sourceMatrix);

				cropView.setTouchCorners(cornerPoints);

				cropView.invalidate();

			}

		});

		crop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				sourceBitmap = ((BitmapDrawable) captureView.getDrawable()).getBitmap();
				
				int width =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sourceBitmap.getWidth(), MainActivity.this.getResources().getDisplayMetrics());

				int height =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,sourceBitmap.getHeight(), MainActivity.this.getResources().getDisplayMetrics());

				Log.w(TAG, "Input bitmap width " + width + " & height " + height);

				sourceMatrix = new Mat(height, width, CvType.CV_8UC4);

				Utils.bitmapToMat(sourceBitmap, sourceMatrix);

				resultMatrix = warp(sourceMatrix);

				Utils.matToBitmap(resultMatrix, resultBitmap);

				cropView.setVisibility(View.INVISIBLE);

				Log.w(TAG, "Output bitmap width " + resultBitmap.getWidth() + " & height " + resultBitmap.getHeight());

				captureView.setImageBitmap(resultBitmap);

				captureView.setScaleType(ScaleType.FIT_XY);

			}

		});

		capture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				cropView.setVisibility(View.VISIBLE);
				cropView.init();
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
					startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
				}

			}

		});

	}
	
	String mCurrentPhotoPath;
	
	private File createImageFile() throws IOException {
	    
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    // Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = "file:" + image.getAbsolutePath();
	    return image;
	}

	public BaseLoaderCallback mOpenCVLoaderCallback = new BaseLoaderCallback(this) {

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV Manager connected.");

				tempMatrix = sourceMatrix;
				// sourceBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888,
				// true);

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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			Bitmap imageBitmap = (Bitmap) extras.get("data");
			captureView.setImageBitmap(imageBitmap);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mOpenCVLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager.");
		}
	}

	public Mat detectCorners(Mat tempMatrix) {

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

		Imgproc.cvtColor(sourceMatrix, sourceMatrix, Imgproc.COLOR_BGR2GRAY);

		Imgproc.Canny(tempMatrix, tempMatrix, 50, 50);

		Imgproc.GaussianBlur(tempMatrix, tempMatrix, new org.opencv.core.Size(5, 5), 5);

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

		Imgproc.cvtColor(tempMatrix, tempMatrix, Imgproc.COLOR_BayerBG2RGB);

		double[] tempDouble = approxCurveMatrix.get(0, 0);

		Point p1 = new Point(tempDouble[0], tempDouble[1]);
		CropView.Point pA = cropView.new Point(tempDouble[0], tempDouble[1]);

		tempDouble = approxCurveMatrix.get(1, 0);
		Point p2 = new Point(tempDouble[0], tempDouble[1]);
		CropView.Point pB = cropView.new Point(tempDouble[0], tempDouble[1]);

		tempDouble = approxCurveMatrix.get(2, 0);
		Point p3 = new Point(tempDouble[0], tempDouble[1]);
		CropView.Point pC = cropView.new Point(tempDouble[0], tempDouble[1]);

		tempDouble = approxCurveMatrix.get(3, 0);
		Point p4 = new Point(tempDouble[0], tempDouble[1]);
		CropView.Point pD = cropView.new Point(tempDouble[0], tempDouble[1]);

		source.add(p1);
		source.add(p2);
		source.add(p3);
		source.add(p4);

		cornerPoints.add(pA);
		cornerPoints.add(pB);
		cornerPoints.add(pC);
		cornerPoints.add(pD);

		return null;
	}

	public Mat warp(Mat sourceMatrix) {

		if (cropView != null) {
			touchCorners = cropView.getTouchCorners();
		}

		List<Point> input = new ArrayList<Point>();

		Point i1 = new Point(Math.round(touchCorners.get(0).getX()), Math.round(touchCorners.get(0).getY()));
		Point i2 = new Point(Math.round(touchCorners.get(1).getX()), Math.round(touchCorners.get(1).getY()));
		Point i3 = new Point(Math.round(touchCorners.get(2).getX()), Math.round(touchCorners.get(2).getY()));
		Point i4 = new Point(Math.round(touchCorners.get(3).getX()), Math.round(touchCorners.get(3).getY()));

		// Point i1 = new Point(298/2, 245/2);
		// Point i2 = new Point(782/2, 95/2);
		// Point i3 = new Point(890/2, 370/2);
		// Point i4 = new Point(357/2, 564/2);

		input.add(i1);
		input.add(i2);
		input.add(i3);
		input.add(i4);

		Log.w(TAG, "Input points " + input.toString());

		Mat inputMatrix = Converters.vector_Point2f_to_Mat(input);
		
		int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) touchCorners.get(0).getDistance(touchCorners.get(1)), MainActivity.this.getResources().getDisplayMetrics());
		int width2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) touchCorners.get(2).getDistance(touchCorners.get(3)), MainActivity.this.getResources().getDisplayMetrics());
		int maxWidth = (width > width2) ? width : width2;
	
		
		int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) touchCorners.get(0).getDistance(touchCorners.get(3)), MainActivity.this.getResources().getDisplayMetrics());
		int height2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) touchCorners.get(1).getDistance(touchCorners.get(2)), MainActivity.this.getResources().getDisplayMetrics());
		int maxHeight = (height > height2) ? height : height2;

		resultBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
		Mat resultMatrix = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

		List<Point> output = new ArrayList<Point>();

		Point o1 = new Point(0, 0);
		Point o2 = new Point(maxWidth, 0);
		Point o3 = new Point(maxWidth, maxHeight);
		Point o4 = new Point(0, maxHeight);

		output.add(o1);
		output.add(o2);
		output.add(o3);
		output.add(o4);

		Log.w(TAG, "Output points " + output.toString());
		Log.w(TAG, "Output width " + maxWidth + " & height " + maxHeight);

		Mat outputMatrix = Converters.vector_Point2f_to_Mat(output);

		Mat transformationMatrix = Imgproc.getPerspectiveTransform(inputMatrix, outputMatrix);

		Imgproc.warpPerspective(sourceMatrix, resultMatrix, transformationMatrix, new Size(maxWidth, maxHeight), Imgproc.INTER_CUBIC);

		return resultMatrix;
	}

}
