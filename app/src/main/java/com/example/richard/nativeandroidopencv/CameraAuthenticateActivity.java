package com.example.richard.nativeandroidopencv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.nfc.Tag;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;

public class CameraAuthenticateActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2
{
	private Mat frameIn;
	private Mat frameOut;
	private Mat eyeCircleSelection;
	public static Mat JNIReturn;
	private Mat JNIReturnNormalized;
	private int framesPassed;
	private Vector<Integer> irisCode1 = new Vector<>();
	private Vector<Integer> irisCode2 = new Vector<>();
	private boolean IRISRECOGNITION = false;
	private boolean correctIris = false;
	private static String TAG = "AuthenticateActivity";
	private static JavaCameraView jcv;
	int [] histogramValues = {0, 1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 24, 28, 30, 31, 32, 48, 56, 60, 62, 63, 64, 96, 112, 120, 124, 126, 127, 128, 129, 131, 135, 143, 159, 191, 192, 193, 195, 199, 207, 223, 224, 225, 227, 231, 239, 240, 241, 243, 247, 248, 249, 251, 252, 253, 254, 255};

	private File mCascadeFile;
	private CascadeClassifier eyes_cascade2;

	BaseLoaderCallback mLoader = new BaseLoaderCallback(this)
	{
		@Override
		public void onManagerConnected(int status)
		{
			switch (status)
			{
				case BaseLoaderCallback.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
					jcv.enableView();
					eyes_cascade2 = new CascadeClassifier("haarcascade_eye.xml");
					try
					{
						// load cascade file from application resources
						InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
						File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
						mCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
						FileOutputStream os = new FileOutputStream(mCascadeFile);

						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = is.read(buffer)) != -1)
						{
							os.write(buffer, 0, bytesRead);
						}
						is.close();
						os.close();

						eyes_cascade2 = new CascadeClassifier(mCascadeFile.getAbsolutePath());
						if (eyes_cascade2.empty())
						{
							Log.e(TAG, "Failed to load cascade classifier");
							eyes_cascade2 = null;
						} else
							Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

						cascadeDir.delete();

					} catch (IOException e)
					{
						e.printStackTrace();
						Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
					}
					break;
				}
					default:
						super.onManagerConnected(status);
						break;
			}
		}
	};

	static
	{
		if (OpenCVLoader.initDebug())
			Log.i(TAG, "OpenCV loaded successfully.");
		else
			Log.i(TAG, "OpenCV load failed.");
	}

	static
	{
		System.loadLibrary("native-lib");
		System.loadLibrary("opencv_java3");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_camera_view_authenticate);

		jcv = (JavaCameraView) findViewById(R.id.jcv);
		jcv.setVisibility(JavaCameraView.VISIBLE);
		jcv.setCvCameraViewListener(this);
		jcv.setCameraIndex(0);

		jcv.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (IRISRECOGNITION == false)
				{
					IRISRECOGNITION = true;
					jcv.flashOn();
					framesPassed = 0;
				}
				return false;
			}
		});
	}

	public native void detectIris(long addrInput, long addrOutput, long addrOutputNormalized, long addrOriginal);
	public native int[] returnHist(long addrInput);

	@Override
	protected void onPause()
	{
		super.onPause();
		if (jcv != null)
			jcv.disableView();
	}
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (jcv != null)
			jcv.disableView();
		JNIReturn.release();
		JNIReturnNormalized.release();
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		if (OpenCVLoader.initDebug())
		{
			Log.i(TAG, "OpenCV loaded successfully.");
			mLoader.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		} else
		{
			Log.i(TAG, "OpenCV load failed.");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoader);
		}
	}
	@Override
	public void onCameraViewStarted(int width, int height)
	{
		frameIn = new Mat();
		frameOut = new Mat();
		eyeCircleSelection = new Mat();
		JNIReturn = new Mat();
		JNIReturnNormalized = new Mat();
		framesPassed = 0;
	}
	@Override
	public void onCameraViewStopped()
	{
		frameIn.release();
		frameOut.release();
		eyeCircleSelection.release();
		//JNIReturn.release();
	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{
		int radius = (int)Math.round(frameIn.width()*0.12);
		Point circleRCenter = new Point(frameIn.width()*0.25, frameIn.height()*0.3);
		Point circleLCenter = new Point(frameIn.width()*0.25, frameIn.height()*0.7);
		Rect eyeRegion = new Rect((int)Math.round(frameIn.width()*0.25-radius), (int)Math.round(frameIn.height()*0.3-radius), radius*2, radius*2);


		frameIn = inputFrame.rgba();
		frameIn.copyTo(frameOut);
		eyeCircleSelection = frameIn.submat(eyeRegion);
		Imgproc.circle(frameOut, circleRCenter, radius, new Scalar(255, 0, 0), 5);
		Imgproc.circle(frameOut, circleLCenter, radius, new Scalar(255, 0, 0), 5);

		if (IRISRECOGNITION == false)
			return frameOut;

		if (framesPassed == 35)
		{
			framesPassed = 0;
			jcv.flashOff();
			IRISRECOGNITION = false;
			//eyeCircleSelection = findEye(eyeCircleSelection);
			detectIris(eyeCircleSelection.getNativeObjAddr(),JNIReturn.getNativeObjAddr(), JNIReturnNormalized.getNativeObjAddr(), frameIn.getNativeObjAddr());

			Intent getImageViewScreen = new Intent(this, ImageViewActivity.class);
			final int result = 1;
			Bitmap pass = Bitmap.createBitmap(JNIReturn.cols(), JNIReturn.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(JNIReturn, pass);
			getImageViewScreen.putExtra( "sendingMat", pass );
			startActivityForResult(getImageViewScreen, result);
		}
		framesPassed++;
		return frameOut;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == 1) {
			if(resultCode == Activity.RESULT_OK){
				int result=data.getIntExtra("result", -1);
				if (result  == 1)
				{
					correctIris = true;
					if (irisCode1.isEmpty())
						irisCode1 = LBP(JNIReturnNormalized);
					else
					{
						irisCode2 = LBP(JNIReturnNormalized);
						double check = chiSquared(irisCode1, irisCode2);
						AlertDialog alertDialog = new AlertDialog.Builder(CameraAuthenticateActivity.this).create();
						alertDialog.setTitle("Alert");
						alertDialog.setMessage("Distance: "+check);
						alertDialog.show();
						irisCode1 = new Vector<>();
						irisCode2 = new Vector<>();
					}

				}
				else
					correctIris = false;
			}
		}
	}

	public Mat findEye(Mat input)
	{
		Mat gray = new Mat();
		Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);
		//equalizeHist(gray, gray);

		float height = (float) gray.size().height;
		long minSize = Math.round(height * 0.5);
		long maxSize = Math.round(height);

		MatOfRect rectOfEyes = new MatOfRect();

		eyes_cascade2.detectMultiScale(gray, rectOfEyes, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(minSize, minSize), new Size(maxSize, maxSize)); //

		Rect[] eyes = rectOfEyes.toArray();
		if (eyes.length == 0)
			return input;
		Rect eyeRegion = eyes[0];

		if (eyes.length == 1)
			eyeRegion = eyes[0];

		Mat eye = input.submat(eyeRegion);
		return eye;
	}

	public Mat findCircles(Mat input)
	{
		Mat gray = new Mat();
		Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);
		Mat thresholded = new Mat();
		Mat circles = new Mat();

		Imgproc.threshold(gray, thresholded, 70, 255, Imgproc.THRESH_BINARY_INV);
		Mat floodfilled = thresholded.clone();
		Imgproc.floodFill(thresholded, floodfilled, new Point(0, 0), new Scalar(255));

		Core.bitwise_not(floodfilled, floodfilled);

		Core.add(thresholded, floodfilled, thresholded);

		Imgproc.GaussianBlur(thresholded, thresholded, new Size(9, 9), 3, 3);
		Imgproc.HoughCircles(thresholded, circles, Imgproc.CV_HOUGH_GRADIENT,
				2.0, thresholded.rows() / 8, 255, 30, 0, 0);

		if (circles.cols() > 0)
			for (int x = 0; x < circles.cols(); x++)
			{
				double vCircle[] = circles.get(0,x);

				if (vCircle == null)
					break;

				Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
				int radius = (int)Math.round(vCircle[2]);

				Imgproc.circle(input, pt, radius, new Scalar(0,255,0), -1);
			}
		circles.release();
		thresholded.release();
		return input;
	}

	Vector<Integer> LBP(Mat input)
	{
		Vector<Integer> outputHist = new Vector<Integer>();
		outputHist.setSize(59);
		Collections.fill(outputHist, 0);

		for (int i = 1; i < input.rows() - 1; i++)
		{
			for (int j = 1; j < input.cols() - 1; j++)
			{
				//Currently centered pixel
				double [] otherIntensity = input.get(i, j);

				int vectorValue = 0;
				Vector<Integer> binaryCode = new Vector<>();
				double pixelIntensity = otherIntensity[0];

				//Top left
				otherIntensity = input.get(i, j);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 128;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Top middle
				otherIntensity = input.get(i, j - 1);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 64;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Top right
				otherIntensity = input.get(i + 1, j - 1);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 32;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Right
				otherIntensity = input.get(i + 1, j);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 16;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Bottom right
				otherIntensity = input.get(i + 1, j + 1);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 8;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Botttom middle
				otherIntensity = input.get(i, j + 1);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 4;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Bottom left
				otherIntensity = input.get(i - 1, j + 1);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 2;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				//Left
				otherIntensity = input.get(i - 1, j);
				if (otherIntensity[0] < pixelIntensity)
				{
					vectorValue += 1;
					binaryCode.add(1);
				}
				else
					binaryCode.add(0);

				if (checkUniform(binaryCode))
				{
					for (int x = 0; x < 59; x++)
						if (histogramValues[x] == vectorValue)
						{
							int hold = outputHist.elementAt(x);
							outputHist.remove(x);
							outputHist.add(x, ++hold);
							break;
						}
				}
				else
				{
					int hold = outputHist.elementAt(58);
					outputHist.remove(58);
					outputHist.add(58, ++hold);
				}
			}
		}
		return outputHist;
	}

	boolean checkUniform(Vector<Integer> binaryCode)
	{
		int transitionCount = 0;
		for (int i = 1; i < 8; i++)
		{
			if ((binaryCode.elementAt(i)^ binaryCode.elementAt(i-1)) == 1)
				transitionCount++;
			if (transitionCount > 2)
				return false;
		}
		return true;
	}

	public double chiSquared(Vector<Integer> hist1, Vector<Integer> hist2)
	{
		double[] normalizedHist1 = new double[59];
		double[] normalizedHist2 = new double[59];
		;

		for (int i = 0; i < 58; i++)
		{
			normalizedHist1[i] = (double) hist1.elementAt(i) / hist1.elementAt(58);
			normalizedHist2[i] = (double) hist2.elementAt(i) / hist2.elementAt(58);
		}

		normalizedHist1[58] = 1.0;
		normalizedHist2[58] = 1.0;

		double chiSquaredValue = 0.0;
		for (int i = 1; i < 59; i++)
		{
			if (hist1.elementAt(i) + hist2.elementAt(i) != 0)
			{
				chiSquaredValue += Math.pow(normalizedHist1[i] - normalizedHist2[i], 2) / (normalizedHist1[i] + normalizedHist2[i]);
			}
		}
		return chiSquaredValue * 10;
	}
}
