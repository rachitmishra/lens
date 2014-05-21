package com.practo.lens;

import java.util.ArrayList;
import java.util.List;

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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.practo.lens.view.CropView;

public class MainActivity extends Activity {

	private ImageView captureView;

	private CropView cropView;

	private Mat imageMatrix, sourceMatrix, resultMatrix;

	private Bitmap sourceBitmap, resultBitmap;

	private Button crop, cancel, capture;

	private static final int REQUEST_IMAGE_CAPTURE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		captureView = (ImageView) findViewById(R.id.captureView);
		cropView = (CropView) findViewById(R.id.cropView);
		crop = (Button) findViewById(R.id.crop);
		cancel = (Button) findViewById(R.id.cancel);
		capture = (Button) findViewById(R.id.capture);

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				MainActivity.this.finish();
			}

		});

		crop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				crop();
			}

		});

		capture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Intent takePictureIntent = new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE);
				if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
					startActivityForResult(takePictureIntent,
							REQUEST_IMAGE_CAPTURE);
				}

			}

		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
	        Bundle extras = data.getExtras();
	        Bitmap imageBitmap = (Bitmap) extras.get("data");
	        captureView.setImageBitmap(imageBitmap);
	    }
	}
	
	private void crop() {
		sourceBitmap = ((BitmapDrawable) captureView.getDrawable()).getBitmap();

		Utils.bitmapToMat(sourceBitmap, imageMatrix);

		resultMatrix = extractPage();
		Utils.matToBitmap(resultMatrix, resultBitmap);
		captureView.setBackground(new BitmapDrawable(getResources(),
				resultBitmap));
	}

	public Mat extractPage() {
		Imgproc.Canny(imageMatrix, imageMatrix, 50, 50);

		// apply gaussian blur to smoothen lines of dots
		Imgproc.GaussianBlur(imageMatrix, imageMatrix,
				new org.opencv.core.Size(5, 5), 5);

		// find the contours
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(imageMatrix, contours, new Mat(),
				Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		double maxArea = -1;
		int maxAreaIdx = -1;
		Log.d("size", Integer.toString(contours.size()));
		MatOfPoint temp_contour = contours.get(0); // the largest is at the
													// index 0 for starting
													// point
		MatOfPoint2f approxCurve = new MatOfPoint2f();
		MatOfPoint largest_contour = contours.get(0);
		// largest_contour.ge
		List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
		// Imgproc.drawContours(imgSource,contours, -1, new Scalar(0, 255, 0),
		// 1);

		for (int idx = 0; idx < contours.size(); idx++) {
			temp_contour = contours.get(idx);
			double contourarea = Imgproc.contourArea(temp_contour);
			// compare this contour to the previous largest contour found
			if (contourarea > maxArea) {
				// check if this contour is a square
				MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
				int contourSize = (int) temp_contour.total();
				MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
				Imgproc.approxPolyDP(new_mat, approxCurve_temp,
						contourSize * 0.05, true);
				if (approxCurve_temp.total() == 4) {
					maxArea = contourarea;
					maxAreaIdx = idx;
					approxCurve = approxCurve_temp;
					largest_contour = temp_contour;
				}
			}
		}

		Imgproc.cvtColor(imageMatrix, imageMatrix, Imgproc.COLOR_BayerBG2RGB);

		//sourceMatrix = Highgui.imread(Environment.getExternalStorageDirectory()
			//	.getAbsolutePath() + "/scan/p/1.jpg");

		double[] tempDouble;
		tempDouble = approxCurve.get(0, 0);
		Point p1 = new Point(tempDouble[0], tempDouble[1]);
		// Core.circle(imgSource,p1,55,new Scalar(0,0,255));
		// Imgproc.warpAffine(sourceImage, dummy, rotImage,sourceImage.size());
		tempDouble = approxCurve.get(1, 0);
		Point p2 = new Point(tempDouble[0], tempDouble[1]);
		// Core.circle(imgSource,p2,150,new Scalar(255,255,255));
		tempDouble = approxCurve.get(2, 0);
		Point p3 = new Point(tempDouble[0], tempDouble[1]);
		// Core.circle(imgSource,p3,200,new Scalar(255,0,0));
		tempDouble = approxCurve.get(3, 0);
		Point p4 = new Point(tempDouble[0], tempDouble[1]);
		// Core.circle(imgSource,p4,100,new Scalar(0,0,255));
		List<Point> source = new ArrayList<Point>();
		source.add(p1);
		source.add(p2);
		source.add(p3);
		source.add(p4);
		Mat startMatrix = Converters.vector_Point2f_to_Mat(source);
		Mat resultMatrix = warp(imageMatrix, startMatrix);
		return resultMatrix;
	}

	private Mat warp(Mat inputMatrix, Mat startMatrix) {
		int resultWidth = 1000;
		int resultHeight = 1000;

		Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

		Point ocvPOut1 = new Point(0, 0);
		Point ocvPOut2 = new Point(0, resultHeight);
		Point ocvPOut3 = new Point(resultWidth, resultHeight);
		Point ocvPOut4 = new Point(resultWidth, 0);
		List<Point> dest = new ArrayList<Point>();
		dest.add(ocvPOut1);
		dest.add(ocvPOut2);
		dest.add(ocvPOut3);
		dest.add(ocvPOut4);
		
		Mat endMatrix = Converters.vector_Point2f_to_Mat(dest);

		Mat perspectiveTransform = Imgproc
				.getPerspectiveTransform(startMatrix, endMatrix);

		Imgproc.warpPerspective(inputMatrix, outputMat, perspectiveTransform,
				new Size(resultWidth, resultHeight), Imgproc.INTER_CUBIC);

		return outputMat;
	}

}
