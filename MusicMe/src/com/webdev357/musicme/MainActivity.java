package com.webdev357.musicme;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.webdev357.musicme.R;
import com.webdev357.musicme.MusicDBAdapter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.view.Menu;
import gmusic.api.impl.GoogleMusicAPI;
import gmusic.api.impl.GoogleSkyJamAPI;
import gmusic.api.impl.InvalidCredentialsException;
import gmusic.api.interfaces.IGoogleMusicAPI;
//import gmusic.api.model.Playlist;
//import gmusic.api.model.Playlists;
import gmusic.api.model.Song;
import android.preference.PreferenceFragment;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MainActivity extends ListActivity {
	// get these from preferences screen later
	//public String username = "bakerchromatic@comcast.net";
	//public String password = "Friend12";
	public String username = "";
	public String password = "";
	public String typeOfMusicToGet = "Get Newer Music";     //other vals are "Get Older Music",  "Get All Music" from arrays.xml and 
															//used in preferences.xml .  Get New Music means music newer than what I have for 
	                                                        //that artist.  Older should be pretty obvious as is All.
    //Get Older Music,  Get All Music
	static final int PICK_PERMISSION_RESPONSE = 1;
	static final int PICK_LOGIN_RESPONSE = 2;
	MusicDBAdapter musicDb;
	// Gets the data repository in write mode
	SQLiteDatabase db;
	static String foo;
	String[] urls = { // replace ???? with query item
	"http://api.discogs.com/database/search?type=artist&q=????", // eg q=Lorde
																	// etc.
																	// called
																	// first
			"http://api.discogs.com/artists/????/releases" // eg
															// artist/13608/releases
															// called 2nd
	};
	String tempArtistQuery;  //this has the added id or name to urls[1] or 0
	private AQuery aq;
	ArrayList<String> artistList;  //artist names from google play
	ArrayList<String> artistId;    //ID's from Discogs
	Integer	artistIdIndex;		   //current position being processed in artistlist and artistId
	//public ListAdapter adapter;
	ArrayAdapter<String> adapter;
	ListView lstView;
	SharedPreferences prefs;


	// aQuery json tests
	public void asyncJson() {

		// perform a Google search in just a few lines of code
		String url = "http://www.google.com/uds/GnewsSearch?q=Obama&v=1.0";
		aq.ajax(url, JSONObject.class, this, "jsonCallback");

	}

	public void jsonCallback(String url, JSONObject json, AjaxStatus status) {
		String newsContent;
		if (json != null) {
			try {
				// This shows how to navigate down the multiple levels that
				// google news
				// responseData -> results
				JSONObject responseData = json.getJSONObject("responseData");
				JSONArray newsArray = new JSONArray(
						responseData.getString("results"));
				JSONObject newsItem = newsArray.getJSONObject(0); // first
																	// results
																	// object
				newsContent = newsItem.getString("content"); // news story
				//Log.d("jsonCallback newsContent =", newsContent);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// successful ajax call
		} else {
			// ajax error
		}

	}

	public String readJSONFeed(String URL) {
		String line;
		StringBuilder stringBuilder = new StringBuilder();
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(URL);
		try {
			// httpClient.addHeader();
			httpGet.addHeader(new BasicHeader("User-Agent",
					"MusicMe/0.1 +http://musicmediscogs.org"));
			HttpResponse response = httpClient.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream inputStream = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream));
				
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}
				inputStream.close();
			} else {
				//Log.d("JSON", "Failed to download file");
			}
		} catch (Exception e) {
			//Log.d("readJSONFeed", e.getLocalizedMessage());
		}
		com.webdev357.musicme.MainActivity.foo = stringBuilder.toString();
		//Log.d("readJSONFeed result=", stringBuilder.toString());

		return stringBuilder.toString();
	}

	private class ReadDiscogsFeedTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			return readJSONFeed(tempArtistQuery);
		}
		@Override
		protected void onPostExecute(String result) {
			try {
				String tempId;
				JSONObject jsonObject = new JSONObject(result);
				// Query
				// http://api.discogs.com/database/search?type=artist&q=Lorde
				// gives us:
				JSONArray discogsArtist = new JSONArray(
						jsonObject.getString("results"));
				// discogsArtist.length()
				// ---print out the content of the json feed---
				// for (int i = 0; i < 1 ; i++) {
				//was 0 in parens
				JSONObject discogsInfoItem = discogsArtist.getJSONObject(0);
				tempId = discogsInfoItem.getString("id");
				artistId.add(tempId);
				//Log.d("ReadDiscogsFeedTask tempId=", tempId);

				Toast.makeText(getBaseContext(),
						"New Music id=" + discogsInfoItem.getString("id"),
						Toast.LENGTH_LONG).show();
				// }
			} catch (Exception e) {
				//Log.d("ReadDiscogsFeedTask error=", e.getLocalizedMessage());
			}

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// aq = new AQuery(this); //set up AQuery API object for simplifying
		// code
		lstView = getListView();
		lstView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		lstView.setTextFilterEnabled(true);
		artistList = new ArrayList<String>(); // stores artist names from Google
												// Play Music
		musicDb = new MusicDBAdapter(MainActivity.this.getBaseContext());
		//artistList.add("No Artists Yet..");
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//		          android.R.layout.simple_list_item_checked, android.R.id.text1, artistList);
		adapter = new ArrayAdapter<String>(this,
		          android.R.layout.simple_list_item_checked, android.R.id.text1, artistList);
		lstView.setAdapter(adapter);
		
		//get user prefs from default preferences file
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		username = prefs.getString("edittext_preference", new String());
		password = prefs.getString("editpassword_preference", new String());
		typeOfMusicToGet = prefs.getString("list_preference", new String());
		
		if (username.isEmpty() || password.isEmpty() || password =="NA" || username=="NA") 
		{
			//Integer foo=2;
			//Intent googlePlayLoginintent = new Intent(this, GooglePlayLoginActivity.class);
			//startActivityForResult(googlePlayLoginintent, foo);
			artistList.add("No Google Play User and Password, enter in preferences");
		}else
		{
			// Get Users List of Music from Google Play using the gmusic API for
			// Java at https://github.com/jkiddo/gmusic.api
			// and store in SQLite database.  Place artists in artistList and onscreen in list.
			// wrapper was written by me, uses async stuff etc.
			new GmusicWrapper().execute(username, password, artistList, adapter);	
		}
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		CheckedTextView item = (CheckedTextView) v;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add("New Music");
		menu.add("Check All");
		menu.add("Uncheck");
		//menu.add("Test Something for Dev");
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void setAllCheckAllBoxes(boolean checkBoxVal) {
		// set all check boxes to checkBoxVal
		ListView lstView = getListView();
		int size = lstView.getCount();

		for (int i = 0; i < size; i++)
			lstView.setItemChecked(i, checkBoxVal);
	}

	public int getArtistsNewMusic() {

		return 0;
	}

	public int getNewMusic() {
		// getArtists();
		getArtistsNewMusic();
		return 0;
	}

	
	public class GmusicWrapper extends AsyncTask<Object, Void, Void> {
		// 1st param is what we pass in. It is possible to send in multiple
		// primitive objects of the same
		// primitive type, they are accessed in doInBackground as params[0] and
		// params[1]
		// 2nd param is for progress indicator, 3rd param is
		// what we pass out
		public IGoogleMusicAPI api; // from desktop.gmusic.api linked project
		private String result = null;
		private String myUserName;
		private String myPassword;
		private ArrayList<String> myArtists;
		private ArrayAdapter<String>  adaptercopy;
 
		@Override
		protected Void doInBackground(Object... params) {

			myUserName = (String)params[0];
			myPassword = (String)params[1];
			myArtists = new ArrayList<String>();
			myArtists = (ArrayList<String>) params[2];
			adaptercopy = (ArrayAdapter<String>) params[3];
			try {
				new GoogleSkyJamAPI().login(myUserName, myPassword);
				new GoogleMusicAPI().login(myUserName, myPassword);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try { // connect to google music, get artist info etc.,
					// store in artists table in SQLite database
				IGoogleMusicAPI api = new GoogleMusicAPI();
				api.login(myUserName, myPassword);
				db = musicDb.getWritableDatabase(); // open a writable database,
													// create if 1st time
				// use db to pass to music db class for its use
				musicDb.onCreate(db);

				for (Song list : api.getAllSongs()) {
					//next line is due to some bad data coming back from google music 
					//unofficial api that I will have to try and debug in the future.  Or maybe it is 
					//Google Play itself providing the bad data to the API.  There are 2 bad results coming back.
					//One is a url
					//the other is a song name.  One has a year of 0 , the other has a year 
					//of 1.
					if (((Integer)list.getYear()) >1000)
					musicDb.insertArtist(db, list.getArtist(), list.getAlbum(),
							list.getYear());
					//Log.d("in gwrapper getArtist=",
					//		list.getArtist() + " Album=" + list.getAlbum()
					//				+ " Title=" + list.getTitle() + " Year="
					//				+ list.getYear());
					//
					// "AlbumArtUrl=" + list.getAlbumArtUrl()
					// +
					// "AlbumArtURI=" + list.getAlbumArtUrlAsURI()
				}
				myArtists = musicDb.getAllArtists(db);
				// musicDb.close();

				/*
				 * if I decide to implement playlist code use starting point is
				 * below. Playlists playlists = api.getAllPlaylists();
				 * if(playlists.getMagicPlaylists() != null) { //are
				 * magicplaylists google generated recommendation playlists?
				 * for(Playlist list : playlists.getMagicPlaylists()) {
				 * System.out.println("--- " + list.getTitle() + " " +
				 * list.getPlaylistId() + " ---"); for(Song song :
				 * list.getPlaylist()) { System.out.println(song.getName() + " "
				 * + song.getArtist()); } } }
				 */

			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (InvalidCredentialsException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		}
        @Override		
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
	        adaptercopy.clear();
	        adaptercopy.addAll(myArtists);
	        adaptercopy.notifyDataSetChanged();	
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().toString().equals("New Music")) {
			// gets artist id info from discogs.
/*			
			String artistname;
			String tempId;
			artistId = new ArrayList<String>();
			for (int artistIdIndex = 0; artistIdIndex < artistList.size(); artistIdIndex++) {
				artistname = (artistList.get(artistIdIndex));
				tempArtistQuery = urls[0].replace("????",  //run query to get id back from discogs
						artistname.replace(" ", "%20")); // replace spaces with
															// %20
				//Log.d("onOptionsItemSelected tempArtistQuery=", tempArtistQuery);
				new ReadDiscogsFeedTask().execute(tempArtistQuery);
				try {
					Thread.sleep(1200);
//					tempId = artistId.get(artistIdIndex);					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
*/			
		} else if (item.getTitle().toString().equals("Check All")) {
			setAllCheckAllBoxes(true);
		} else if (item.getTitle().toString().equals("Uncheck")) {
			setAllCheckAllBoxes(false);
		} else if (item.getTitle().toString().equals("Settings")) {
			// Launch Settings activity
			Intent intent = new Intent(this, MusicMeSettingsActivity.class);
			startActivityForResult(intent, PICK_PERMISSION_RESPONSE);
			// getFragmentManager().beginTransaction().replace(android.R.id.content,
			// new PrefsFragment()).commit();
			// asyncJson(); //this test the aQuery lib web call to ny times for
			// obama stories

		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
		super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == PICK_PERMISSION_RESPONSE) { //this is Permissions Actvity that was up and coming back now..
	        // Make sure the request was successful
	        if (resultCode == 0) {
	            // The user user entered user/pw or it is already set
	        	SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

	        	username = SP.getString("edittext_preference", "NA");
	        	password = SP.getString("editpassword_preference", "NA");
	        	typeOfMusicToGet = SP.getString("list_preference","1");  // "Newer Music" "Older Music" "All Music"
				new GmusicWrapper().execute(username, password, artistList, adapter);	

	        }
	    }else if (requestCode == PICK_LOGIN_RESPONSE){
	    	//adapter is null bug  
	    	//username =data.getStringExtra("user");
	    	//password = data.getStringExtra("password");
			//new GmusicWrapper().execute(username, password, artistList, adapter);	
	    		    
	    }
	}

	/*
	 * private void ReadDiscogsJSONFeedTaskRun(String string) { new
	 * ReadDiscogsJSONFeedTask().execute(urls[0]); }
	 */
}
