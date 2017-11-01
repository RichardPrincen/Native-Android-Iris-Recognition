package com.example.richard.nativeandroidopencv;


import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity
{
	//For debugging
	private static String TAG = "MainActivity";

	//Loads opencv
	static
	{
		if (OpenCVLoader.initDebug())
			Log.i(TAG, "OpenCV loaded successfully.");
		else
			Log.i(TAG, "OpenCV load failed.");
	}

	//Loads opencv
	static
	{
		System.loadLibrary("native-lib");
		System.loadLibrary("opencv_java3");
	}

	//Initialize the activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);
	}

	//Authenticate button handler
	public void authenticateButtonClicked(View view)
	{
		Intent getCameraAuthenticateScreen = new Intent(MainActivity.this, CameraAuthenticateActivity.class);
		final int result = 1;
		startActivityForResult(getCameraAuthenticateScreen, result);
	}

	//Register button handler
	public void registerButtonClicked(View view)
	{
		Intent getInputRegisterScreen = new Intent(this, InputNameRegisterActivity.class);
		final int result = 1;
		startActivityForResult(getInputRegisterScreen, result);
	}

	//Delete button handler
	public void deleteButtonClicked(View view)
	{
		Intent getDeleteUserScreen = new Intent(this, ListDeleteUsersActivity.class);
		final int result = 1;
		startActivityForResult(getDeleteUserScreen, result);
	}
}
