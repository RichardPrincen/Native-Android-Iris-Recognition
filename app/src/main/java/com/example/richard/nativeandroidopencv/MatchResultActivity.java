package com.example.richard.nativeandroidopencv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class MatchResultActivity extends Activity
{
	//Initialize the activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_match_result);

		TextView tvMatch = (TextView) findViewById(R.id.matchText);

		//Checks whether the previous activity sent a match or not and change the text values accordingly
		Intent activityThatCalled = getIntent();
		double passed = activityThatCalled.getDoubleExtra("sendingDistance",-1);
		if (passed == -1)
			tvMatch.setText("No match found.");
		else
		{
			String matchName = activityThatCalled.getStringExtra("sendingMatchedName");
			TextView tvDistance = (TextView) findViewById(R.id.distanceText);
			tvDistance.setText(""+passed);
			tvMatch.setText(matchName);
		}
	}

	//Goes back to the camera
	public void buttonOKClicked(View view)
	{
		Intent returnToCameraView = new Intent();
		setResult(RESULT_OK, returnToCameraView);
		finish();
	}
}
