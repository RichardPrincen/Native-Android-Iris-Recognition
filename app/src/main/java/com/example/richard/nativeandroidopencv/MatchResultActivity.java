package com.example.richard.nativeandroidopencv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;


public class MatchResultActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_match_result);

		Intent activityThatCalled = getIntent();
		double passed = (double) activityThatCalled.getDoubleExtra("sendingDistance",10.0);
		TextView tvDistance = (TextView) findViewById(R.id.distanceText);
		tvDistance.setText(""+passed);

		TextView tvMatch = (TextView) findViewById(R.id.matchText);
		if (passed < 0.7)
			tvMatch.setText("Match.");
		else
			tvMatch.setText("No match.");
	}

	public void buttonOKClicked(View view)
	{
		Intent returnToCameraView = new Intent();
		setResult(RESULT_OK, returnToCameraView);
		finish();
	}
}
