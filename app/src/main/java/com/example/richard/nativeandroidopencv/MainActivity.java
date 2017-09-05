package com.example.richard.nativeandroidopencv;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
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

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
	private Mat frameIn;
	private Mat frameOut;
	private File mCascadeFile;
	private int[] irisCode;
	private static String TAG = "MainActivity";
	private CascadeClassifier eyes_cascade2;
	JavaCameraView jcv;
	BaseLoaderCallback mLoader = new BaseLoaderCallback(this)
	{
		@Override
		public void onManagerConnected(int status)
		{
			switch (status)
			{
				case BaseLoaderCallback.SUCCESS:
					jcv.enableView();
					eyes_cascade2 = new CascadeClassifier("haarcascade_eye.xml");
					try {
						// load cascade file from application resources
						InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
						File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
						mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
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
						}
						else
							Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

						cascadeDir.delete();

					}
					catch (IOException e)
					{
						e.printStackTrace();
						Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
					}
					break;
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

	// Used to load the 'native-lib' library on application startup.
	static
	{
		System.loadLibrary("native-lib");
		System.loadLibrary("opencv_java3");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_main);

		// Example of a call to a native method
		jcv = (JavaCameraView) findViewById(R.id.jcv);
		jcv.setVisibility(JavaCameraView.VISIBLE);
		jcv.setCvCameraViewListener(this);
		jcv.setCameraIndex(0);
	}

	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native String stringFromJNI();
	//public native void detectIris(long addrInput, long addrOutput);
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
		}
		else
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
		jcv.flashOn();
	}

	@Override
	public void onCameraViewStopped()
	{
		frameIn.release();
		frameOut.release();
		jcv.flashOff();
	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{
		frameIn = inputFrame.rgba();
		frameOut = findEye(frameIn);
		return frameOut;
	}

	public Mat findEye(Mat input)
	{
		Mat gray = new Mat();
		Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);
		//equalizeHist(gray, gray);

		float height = (float)gray.size().height;
		long minSize = Math.round(height * 0.15);

		MatOfRect rectOfEyes = new MatOfRect();

		eyes_cascade2.detectMultiScale(gray, rectOfEyes, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE , new Size(minSize, minSize), new Size(minSize, minSize)); //

		Rect eyeRegion;
		Rect[] eyes = rectOfEyes.toArray();
		if (eyes.length == 0)
			return input;
		else if (eyes.length == 1)
			eyeRegion = eyes[0];
		else
		{
			if (eyes[0].x > eyes[1].x)
				eyeRegion = eyes[0];//Rect(eyes[0].x, eyes[0].y, eyes[0].width, eyes[0].height);
			else
				eyeRegion = eyes[1];//Rect(eyes[1].x, eyes[1].y, eyes[1].width, eyes[1].height);
		}

		for (int x = 0; x < eyes.length; x++)
		{
			Imgproc.rectangle(input, new Point(eyes[x].x, eyes[x].y), new Point(eyes[x].x + eyes[x].width, eyes[x].y + eyes[x].height), new Scalar(255, 0, 0, 255), 3);
		}
		Mat eye = gray.submat(eyeRegion);
		irisCode = returnHist(eye.getNativeObjAddr());
		return input;
	}

	public double chiSquared(int[] hist1, int[] hist2)
	{
		double [] normalizedHist1 = new double[59];
		double [] normalizedHist2 = new double[59];;

		for (int i = 0; i < 58; i++)
		{
			normalizedHist1[i] = (double)hist1[i]/hist1[58];
			normalizedHist2[i] = (double)hist2[i]/hist2[58];
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
