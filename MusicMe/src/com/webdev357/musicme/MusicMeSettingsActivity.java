package com.webdev357.musicme;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.content.Intent;

public class MusicMeSettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
		//setResult(2,intentMessage);
       // getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferenceActivity()).commit();

	}
	public static class PrefsFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
		}
	}
	
	@Override
	protected void onPause(){
		  super.onPause();
	      String message="onPause called";
	      
	      //=.getText().toString();
	        Intent intentMessage=new Intent();
	 
	        // put the message in Intent
	        intentMessage.putExtra("MESSAGE",message);
	        // Set The Result in Intent
	        setResult(1,intentMessage);
	        // finish The activity
	        finish();
	}
}
