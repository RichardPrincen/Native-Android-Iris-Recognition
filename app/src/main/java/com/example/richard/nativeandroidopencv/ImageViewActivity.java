package com.example.richard.nativeandroidopencv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class ImageViewActivity extends Activity
{
	private Mat imageViewContent;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_image_display);

		Intent activityThatCalled = getIntent();
		Bitmap passed = (Bitmap) activityThatCalled.getParcelableExtra("sendingMat");
		imageViewContent = new Mat();
		Utils.bitmapToMat(passed, imageViewContent);

		Point pt = new Point(imageViewContent.rows()/2, imageViewContent.cols()/2);
		Mat r = Imgproc.getRotationMatrix2D(pt, -90, 1.0);
		Imgproc.warpAffine(imageViewContent, imageViewContent, r, new Size(imageViewContent.rows(), imageViewContent.cols()));

		changeImageView(imageViewContent);
	}

	public void changeImageView(Mat input)
	{
		final Bitmap bm = Bitmap.createBitmap(input.cols(), input.rows(),Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(input, bm);
		final ImageView iv = (ImageView) findViewById(R.id.imageView);
		runOnUiThread(new Runnable()
		{
			@Override
			public void run() {
				iv.setImageBitmap(bm);
			}
		});
	}

	public void cameraViewButtonClicked(View view)
	{
		Intent returnToCameraView = new Intent();
		setResult(RESULT_OK, returnToCameraView);
		imageViewContent.release();
		finish();
	}
}


