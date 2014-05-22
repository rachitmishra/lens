package com.practo.lens;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

				if (sourceBitmap == null) {
					Log.e(TAG, "Source bitmap is null.");
				}

				Utils.bitmapToMat(sourceBitmap, sourceMatrix);

				detectCorners(sourceMatrix);

				cropView.init(cornerPoints);

				cropView.invalidate();

			}

		});
		
		crop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				resultMatrix = warp();
				
				Utils.matToBitmap(resultMatrix, resultBitmap);
				
				captureView.setImageBitmap(resultBitmap);
				
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

	public BaseLoaderCallback mOpenCVLoaderCallback = new BaseLoaderCallback(
			this) {

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV Manager connected.");
				sourceBitmap = ((BitmapDrawable) captureView.getDrawable())
						.getBitmap();
				sourceMatrix = new Mat(sourceBitmap.getHeight(),
						sourceBitmap.getWidth(), CvType.CV_8U, new Scalar(4));
				tempMatrix = sourceMatrix;
				sourceBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true);

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

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mOpenCVLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager.");
		}
	}

	public Mat detectCorners(Mat sourceMatrix) {

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

		Imgproc.cvtColor(sourceMatrix, tempMatrix, Imgproc.COLOR_BGR2GRAY);

		Imgproc.Canny(tempMatrix, tempMatrix, 50, 50);

		Imgproc.GaussianBlur(tempMatrix, tempMatrix, new org.opencv.core.Size(
				5, 5), 5);

		Imgproc.findContours(tempMatrix, contours, new Mat(),
				Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		double bigContour = -1;

		int bigContourId = -1;

		MatOfPoint tempContour = contours.get(0);
		MatOfPoint2f approxCurveMatrix = new MatOfPoint2f();
		MatOfPoint largestContour = contours.get(0);

		for (int idx = 0; idx < contours.size(); idx++) {
			tempContour = contours.get(idx);
			double contourArea = Imgproc.contourArea(tempContour);

			if (contourArea > bigContour) {

				MatOfPoint2f newCurveMatrix = new MatOfPoint2f(
						tempContour.toArray());
				int contourSize = (int) tempContour.total();
				MatOfPoint2f tempCurveMatrix = new MatOfPoint2f();

				Imgproc.approxPolyDP(newCurveMatrix, tempCurveMatrix,
						contourSize * 0.05, true);

				if (tempCurveMatrix.total() == 4) {
					bigContour = contourArea;
					bigContourId = idx;
					approxCurveMatrix = tempCurveMatrix;
					largestContour = tempContour;
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
	
	public Mat warp() {
		
		if(cropView!=null){
			touchCorners = cropView.getTouchCorners();
		}
		
		Point p1 = new Point(touchCorners.get(0).getX(), touchCorners.get(0).getY());

		Point p2 = new Point(touchCorners.get(1).getX(), touchCorners.get(1).getY());

		Point p3 = new Point(touchCorners.get(2).getX(), touchCorners.get(2).getY());

		Point p4 = new Point(touchCorners.get(3).getX(), touchCorners.get(3).getY());
		
		source.add(p1);
		source.add(p2);
		source.add(p3);
		source.add(p4);
		
		Mat startMatrix = Converters.vector_Point2f_to_Mat(source);
		

		
		int height = (int) (touchCorners.get(1).getX() - touchCorners.get(0).getX());
		
		int width =  (int) (touchCorners.get(3).getY() - touchCorners.get(0).getY());
		
		Mat outputMatrix = new Mat(width, height, CvType.CV_8UC4);

		Point ocvPOut1 = new Point(touchCorners.get(0).getX(), touchCorners.get(0).getY());
		Point ocvPOut2 = new Point(touchCorners.get(0).getX(), height);
		Point ocvPOut3 = new Point(width, height);
		Point ocvPOut4 = new Point(width, touchCorners.get(0).getX());
		
		
		resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		List<Point> dest = new ArrayList<Point>();

		dest.add(ocvPOut1);
		dest.add(ocvPOut2);
		dest.add(ocvPOut3);
		dest.add(ocvPOut4);

		Mat endMatrix = Converters.vector_Point2f_to_Mat(dest);

		Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startMatrix, endMatrix);

		Imgproc.warpPerspective(sourceMatrix, outputMatrix,
				perspectiveTransform, new Size(width, height),
				Imgproc.INTER_CUBIC);

		return outputMatrix;
	}

}
