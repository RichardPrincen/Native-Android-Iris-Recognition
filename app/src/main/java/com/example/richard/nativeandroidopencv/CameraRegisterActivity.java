package com.example.richard.nativeandroidopencv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Vector;

import static org.opencv.core.CvType.CV_8U;

public class CameraRegisterActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2
{
	private Mat frameIn;
	private Mat frameOut;
	private Mat eyeCircleSelection;
	public static Mat JNIReturn;
	private Mat JNIReturnNormalized;
	private int framesPassed;
	private Vector<Integer> irisCode = new Vector<>();
	private boolean IRISRECOGNITION = false;
	private static String TAG = "RegisterActivity";
	private static JavaCameraView jcv;
	int [] histogramValues = {0, 1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 24, 28, 30, 31, 32, 48, 56, 60, 62, 63, 64, 96, 112, 120, 124, 126, 127, 128, 129, 131, 135, 143, 159, 191, 192, 193, 195, 199, 207, 223, 224, 225, 227, 231, 239, 240, 241, 243, 247, 248, 249, 251, 252, 253, 254, 255};

	private UserDatabase userdb;
	private String passedName;

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

		Intent activityThatCalled = getIntent();
		passedName = activityThatCalled.getStringExtra("sendingName");

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

		loadUserDatabase();
	}

	public native void detectIris(long addrInput, long addrOutput, long addrOutputNormalized, long addrOriginal);

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
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{

		if (requestCode == 1)
		{
			if(resultCode == Activity.RESULT_OK)
			{
				int result = data.getIntExtra("result", -1);
				if (result  == 1)
				{
					irisCode = NBP(JNIReturnNormalized);//LBP(JNIReturnNormalized);
					userdb.addUser(irisCode, passedName);
					saveUserDatabase();
					Intent returnToMain = new Intent();
					setResult(RESULT_OK, returnToMain);
					finish();
				}
			}
		}
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

	Vector<Integer> NBP(Mat input)
	{
		Mat NBPimage = new Mat(input.rows(), input.cols(), CV_8U);
		Vector<Integer> NBPcode = new Vector<>();

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
					vectorValue += 128;

				//Top middle
				otherIntensity = input.get(i, j - 1);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 64;

				//Top right
				otherIntensity = input.get(i + 1, j - 1);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 32;


				//Right
				otherIntensity = input.get(i + 1, j);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 16;

				//Bottom right
				otherIntensity = input.get(i + 1, j + 1);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 8;

				//Botttom middle
				otherIntensity = input.get(i, j + 1);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 4;

				//Bottom left
				otherIntensity = input.get(i - 1, j + 1);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 2;

				//Left
				otherIntensity = input.get(i - 1, j);
				if (otherIntensity[0] < pixelIntensity)
					vectorValue += 1;

				NBPimage.put(i, j, vectorValue);
			}
		}

		Vector<Vector<Integer>> means = new Vector<>();
		Vector<Integer> rowmeans;
		for (int i = 0; i < 6; i++)
		{
			rowmeans = new Vector<>();
			for (int j = 0; j < 12; j++)
			{
				int blockmean = 0;
				for (int x = i * 10; x < i * 10 + 10; x++)
				{
					for (int y = j * 30; y < j * 30 + 30; y++)
					{
						blockmean += NBPimage.get(x, y)[0];
					}
				}
				rowmeans.add(blockmean/(30*10));
			}
			means.add(rowmeans);
		}

		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 10; j++)
			{
				if (means.elementAt(i).elementAt(j) > means.elementAt(i).elementAt(j + 1))
					NBPcode.add(1);
				else
					NBPcode.add(0);
			}
		}
		return NBPcode;
	}

	public void saveUserDatabase()
	{
		try
		{
			FileOutputStream irisCodesFileOutputStream = openFileOutput("irisCodes", Context.MODE_PRIVATE);
			ObjectOutputStream irisCodesObjectOutputStream = new ObjectOutputStream(irisCodesFileOutputStream);
			irisCodesObjectOutputStream.writeObject(userdb.irisCodes);
			irisCodesObjectOutputStream.close();
			irisCodesObjectOutputStream.flush();

			FileOutputStream namesFileOutputStream = openFileOutput("names", Context.MODE_PRIVATE);
			ObjectOutputStream namesObjectOutputStream = new ObjectOutputStream(namesFileOutputStream);
			namesObjectOutputStream.writeObject(userdb.names);
			namesObjectOutputStream.close();
			namesObjectOutputStream.flush();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void loadUserDatabase()
	{
//		File checkFile = new File("irisCodes");
//		if (!checkFile.exists())
//		{
//			try
//			{
//				FileOutputStream irisCodesFileOutputStream = openFileOutput("irisCodes", Context.MODE_PRIVATE);
//				ObjectOutputStream irisCodesObjectOutputStream = new ObjectOutputStream(irisCodesFileOutputStream);
//				irisCodesObjectOutputStream.writeObject(new Vector<Vector<Integer>>());
//				irisCodesObjectOutputStream.close();
//				irisCodesObjectOutputStream.flush();
//
//				FileOutputStream namesFileOutputStream = openFileOutput("names", Context.MODE_PRIVATE);
//				ObjectOutputStream namesObjectOutputStream = new ObjectOutputStream(namesFileOutputStream);
//				namesObjectOutputStream.writeObject(new Vector<String>());
//				namesObjectOutputStream.close();
//				namesObjectOutputStream.flush();
//
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//			}
//		}
		try
		{
			FileInputStream irisCodesFileInputStream = openFileInput("irisCodes");
			ObjectInputStream irisCodesObjectInputStream = new ObjectInputStream(irisCodesFileInputStream);
			Vector<Vector<Integer>> irisCodes = (Vector<Vector<Integer>>)irisCodesObjectInputStream.readObject();
			irisCodesObjectInputStream.close();

			FileInputStream namesFileInputStream = openFileInput("names");
			ObjectInputStream namesObjectInputStream = new ObjectInputStream(namesFileInputStream);
			Vector<String> names = (Vector<String>)namesObjectInputStream.readObject();
			namesObjectInputStream.close();

			userdb = new UserDatabase(irisCodes, names);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
