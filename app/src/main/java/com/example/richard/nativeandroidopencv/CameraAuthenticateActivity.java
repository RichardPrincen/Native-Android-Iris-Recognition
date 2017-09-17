package com.example.richard.nativeandroidopencv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.nfc.Tag;
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
	private int framesPassed;
	private int[] irisCode;
	private boolean IRISRECOGNITION = false;
	private static String TAG = "AuthenticateActivity";
	private static JavaCameraView jcv;

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
				}	break;
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

	public native void detectIris(long addrInput, long addrOutput, long addrOriginal);
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
		framesPassed = 0;
	}
	@Override
	public void onCameraViewStopped()
	{
		frameIn.release();
		frameOut.release();
		eyeCircleSelection.release();
		JNIReturn.release();
	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{
		int radius = (int)Math.round(frameIn.width()*0.12);
		Point circleRCenter = new Point(frameIn.width()*0.25, frameIn.height()*0.3);
		Point circleLCenter = new Point(frameIn.width()*0.25, frameIn.height()*0.7);
		Rect eyeRegion = new Rect((int)Math.round(frameIn.width()*0.25-radius*0.5), (int)Math.round(frameIn.height()*0.3-radius*0.5), radius, radius);


		frameIn = inputFrame.rgba();
		frameIn.copyTo(frameOut);
		eyeCircleSelection = frameIn.submat(eyeRegion);
		Imgproc.circle(frameOut, circleRCenter, radius, new Scalar(255, 0, 0), 5);
		Imgproc.circle(frameOut, circleLCenter, radius, new Scalar(255, 0, 0), 5);

		if (IRISRECOGNITION == false)
			return frameOut;

		if (framesPassed == 20)
		{
			framesPassed = 0;
			jcv.flashOff();
			IRISRECOGNITION = false;
			detectIris(eyeCircleSelection.getNativeObjAddr(),JNIReturn.getNativeObjAddr(), frameIn.getNativeObjAddr());
			//changeImageView(JNIReturn);
			Intent getImageViewScreen = new Intent(this, ImageViewActivity.class);
			final int result = 1;
			long addr = JNIReturn.getNativeObjAddr();

			Bitmap pass = Bitmap.createBitmap(JNIReturn.cols(), JNIReturn.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(JNIReturn, pass);

			getImageViewScreen.putExtra( "sendingMat", pass );
			startActivityForResult(getImageViewScreen, result);
		}
		framesPassed++;
		return frameOut;
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

	public double chiSquared(int[] hist1, int[] hist2)
	{
		double[] normalizedHist1 = new double[59];
		double[] normalizedHist2 = new double[59];
		;

		for (int i = 0; i < 58; i++)
		{
			normalizedHist1[i] = (double) hist1[i] / hist1[58];
			normalizedHist2[i] = (double) hist2[i] / hist2[58];
		}

		normalizedHist1[58] = 1.0;
		normalizedHist2[58] = 1.0;

		double chiSquaredValue = 0.0;
		for (int i = 1; i < 59; i++)
		{
			if (hist1[i] + hist2[i] != 0)
			{
				chiSquaredValue += Math.pow(normalizedHist1[i] - normalizedHist2[i], 2) / (normalizedHist1[i] + normalizedHist2[i]);
			}
		}
		return chiSquaredValue * 10;
	}
}
