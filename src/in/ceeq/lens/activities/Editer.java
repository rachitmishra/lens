package in.ceeq.lens.activities;

import in.ceeq.lens.R;
import in.ceeq.lens.commons.Helper;
import in.ceeq.lens.commons.Poynt;
import in.ceeq.lens.views.CropFrame;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Editer extends Activity {

	private CropFrame sourceView, resultView;

	private Mat sourceMatrix;

	private Bitmap sourceBitmap, resultBitmap;

	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private static final int REQUEST_IMAGE_SELECT = 11;

	private List<Poynt> detectedCorners = new ArrayList<Poynt>();

	private static final String TAG = "Tagged";

	private double mDefaultAspectRatio;

	private int mHeight;

	private int mWidth;

	private File photoFile;

	private String mCurrentPhotoPath;

	private String mAction;

	private Bundle data;
	
	private SectionsPagerAdapter mSectionsPagerAdapter;

	private ViewPager mViewPager;

	static {
		if (!OpenCVLoader.initDebug()) {
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editer);

		setupUI();
		setupActionbar();
		
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		//pickPictureFromGallery();
		setDoneBar();
		
		//mAction = getIntent().getAction();
		//data = getIntent().getExtras();

		//if (mAction.equals(Constants.ACTION_IMAGE_CAPTURE)) {
		//	takePictureFromCamera();
		//} else if (mAction.equals(Constants.ACTION_IMAGE_SELECT)) {
		//	String imagePath = data.getString(Constants.IMAGE_PATH);
		//	setPictureFromFile(Uri.parse(imagePath));
		//}
	}
	
	private View mCancelView;
	private View mDoneView;
	private static TextView doneTextView;
	
	public void setDoneBar() {
		final LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		View actionBarButtons = inflater.inflate(R.layout.actionbar_done, new LinearLayout(this), false);
		mCancelView = actionBarButtons.findViewById(R.id.action_cancel);
		//mCancelView.setOnClickListener(new CancelClickListener());
		mDoneView = actionBarButtons.findViewById(R.id.action_done);
		//mDoneView.setOnClickListener(this);
		doneTextView = (TextView) actionBarButtons.findViewById(R.id.tv_done);
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(actionBarButtons,
				new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}
	
	
	
	public void pickPictureFromGallery(){
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == 1 && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			setPictureFromFile(uri);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		//if (hasFocus) {
		//	setPictureFromCamera();
		//}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionbar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setDisplayShowHomeEnabled(false);
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_edge:
			getEdges();
			return true;
		case R.id.action_snap:
			snap();
			return true;
		}

		return false;
	}

	private void takePictureFromCamera() {
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

	private void setupUI() {
		sourceView = (CropFrame) findViewById(R.id.sourceView);
	}

	private void crop() {
		try {

			getPicture();
			sourceMatrix = new Mat(mHeight, mWidth, CvType.CV_8UC4);
			Utils.bitmapToMat(sourceBitmap, sourceMatrix);
			doPerspectiveCrop(sourceMatrix);

		} catch (Exception e) {
			Toast.makeText(Editer.this, "Problems in cropping.", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

	}

	private void snap() {
		sourceView.setDefault(true);
	}

	private void getEdges() {
		try {

			getPicture();
			sourceMatrix = new Mat(mHeight, mWidth, CvType.CV_8UC4);
			Utils.bitmapToMat(sourceBitmap, sourceMatrix);
			doEdgeDetection(sourceMatrix);
			sourceView.setImageBitmap(sourceBitmap);
			sourceView.setCorners(detectedCorners);

		} catch (Exception e) {
			Toast.makeText(Editer.this, "Problem in getting edges.", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

	}

	private File createImageFile() throws IOException {

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String imageFileName = "LENS_" + timeStamp + "_";

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
	}

	private void setPictureFromCamera() {
		sourceView.setDefault(true);
		int layoutWidth = sourceView.getWidth();
		int layoutHeight = sourceView.getHeight();

		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		int photoWidth = 0;
		int photoHeight = 0;

		Bitmap bitmap = null;

		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		photoWidth = bmOptions.outWidth;
		photoHeight = bmOptions.outHeight;

		mDefaultAspectRatio = Math.min(photoWidth / layoutWidth, photoHeight / layoutHeight);

		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = (int) mDefaultAspectRatio;
		bmOptions.inPurgeable = true;

		bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
		sourceView.setImageBitmap(bitmap);
	}

	private void setPictureFromFile(Uri selectedImage) {
		sourceView.setDefault(true);
		int layoutWidth = sourceView.getWidth();
		int layoutHeight = sourceView.getHeight();

		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		int photoWidth = 0;
		int photoHeight = 0;

		Bitmap bitmap = null;
		Helper.log(selectedImage+"Photopath");
		String[] filePathColumn = { android.provider.MediaStore.Images.ImageColumns.DATA };
		Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
		cursor.moveToFirst();
		mCurrentPhotoPath = cursor.getString(0);

		Helper.log(mCurrentPhotoPath+"Photopath");
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

	}

	public void doEdgeDetection(Mat tempMatrix) {

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
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(tempMatrix.cols() / 2F, tempMatrix.rows() / 2F),
				angle, 1);
		Imgproc.warpAffine(tempMatrix, tempMatrix, rotationMatrix, tempMatrix.size(), Imgproc.INTER_CUBIC);
		return tempMatrix;
	}

	public Mat doPerspectiveCrop(Mat tempMatrix) {

		List<Poynt> cropCorners = new ArrayList<Poynt>();
		double skewAngle = 0;

		cropCorners = Helper.getPointsOnImage(Poynt.sortPoynts(sourceView.getCropCorners()), sourceView.getImageView());
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
	
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				
				return CropFragment.newInstance();
			case 1:
				return EnhanceFragment.newInstance();
			default:
				return null;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_crop).toUpperCase(l);
			case 1:
				return getString(R.string.title_enhance).toUpperCase(l);
			}
			return null;
		}
	}

	public static class CropFragment extends Fragment {

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static CropFragment newInstance() {
			CropFragment fragment = new CropFragment();
			return fragment;
		}

		public CropFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_crop, container, false);
			return rootView;
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.fragment_crop, menu);
			super.onCreateOptionsMenu(menu, inflater);
		}
		
		@Override
		public void setUserVisibleHint(boolean isVisibleToUser) {
			if(isVisibleToUser){
				doneTextView.setText("Crop");
			}
		}
	}

	public static class EnhanceFragment extends Fragment {

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static EnhanceFragment newInstance() {
			EnhanceFragment fragment = new EnhanceFragment();
			return fragment;
		}

		public EnhanceFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_enhance, container, false);
			return rootView;
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.fragment_enhance, menu);
			super.onCreateOptionsMenu(menu, inflater);
		}
		
		@Override
		public void setUserVisibleHint(boolean isVisibleToUser) {
			if(isVisibleToUser){
				doneTextView.setText("Save");
			}
		}
	}

}
