package com.webdev357.musicme;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MusicDBAdapter extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String KEY_ROWID = "_id";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ALBUM = "album";
    private static final String KEY_YEAR = "year";
    private static final String DATABASE_NAME = "MusicDB.db";
    private static final String DATABASE_TABLE = "artist";
    private static final String DATABASE_ARTIST_DISCOGS_TABLE = "artistdiscogs";
    String sql1 = "drop table if exists artist;";
    String sql2 = "drop index if exists artist_idx;";
    String sql3 = "create table artist (_id INTEGER primary key autoincrement, " +
        	" artist TEXT not null, album TEXT not null, year INTEGER not null, " +
        	" _albumid INTEGER, UNIQUE(artist, album) ON CONFLICT IGNORE);";
    String sql4 =  "CREATE INDEX artist_idx " +
        	" on artist (artist, year, album);";
    String sql5 = "drop table if exists artistdiscogs;";
    String sql6 = "drop index if exists artistdiscogs_idx;";
    String sql7 = "create table artistdiscogs (_id INTEGER primary key autoincrement, " +
        	" artist TEXT not null, album TEXT not null, year INTEGER not null, " +
        	" _albumid INTEGER, UNIQUE(artist, album) ON CONFLICT IGNORE);";
    String sql8 =  "CREATE INDEX artistdiscogs_idx " +
        	" on artist (artist, year, album);";
    String sqlGetAlbumsIDontHave =  "select artistdiscogs.artist, artistdiscogs.album, artistdiscogs.year " +
    			   " from artistdiscogs " +
    			   " EXCEPT " +
    			   " select artist.artist, artist.album, artist.year " +
    			   " from artist " +
    			   " order by artist, year, album; " ;
    			   


    String[] statements = new String[]{sql1, sql2, sql3, sql4, sql5, sql6, sql7, sql8};
    
    MusicDBAdapter(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    	for(String sql : statements){
    	    db.execSQL(sql);   //execSQL is retarded and cannot execute multiple 
    	    				   //sql stmnt so I had to split up to single ones
    	}

        ////Log.d("musicdbadaperter","database created!!!");
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
    //---insert a Google Play artist/albuminto the database---
    public long insertArtist(SQLiteDatabase db, String artist, String album, Integer year) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ARTIST, artist);
        initialValues.put(KEY_ALBUM, album);
        initialValues.put(KEY_YEAR, year);
        return db.insert(DATABASE_TABLE, null, initialValues);
    }
    //---insert a Discogs artist/album into the database---
    public long insertArtistDiscogs(SQLiteDatabase db, String artist, String album, Integer year,
    		String tempThumb, String tempRole, String tempResourceurl) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ARTIST, artist);
        initialValues.put(KEY_ALBUM, album);
        initialValues.put(KEY_YEAR, year);
        return db.insert(DATABASE_ARTIST_DISCOGS_TABLE, null, initialValues);
    }
  //closes the database
    public void close(SQLiteDatabase db) 
    {
        db.close();
    }

    //---retrieves all the artists---
	public ArrayList<String> getAllArtists(SQLiteDatabase db) {
		ArrayList<String> artists;
		Cursor c;
		artists = new ArrayList<String>();

		c = db.rawQuery("SELECT artist FROM artist ORDER BY artist;", null);
		if (c.moveToFirst()) {
			do {
				artists.add(c.getString(0)); // store in ArrayList, gets put
												// in list in oncreate
			} while (c.moveToNext());
		} else {
			//Log.d("doinbackground", "getAllArtists returned nothing!!!");
		}
		return artists;
	}
	//public ArrayList<String> getAllArtistsIDontHave(SQLiteDatabase db) {
	//	ArrayList<String> artistsalbumsyear;
	//sqlGetAlbumsIDontHave
	public ArrayList<String> getAllArtistsIDontHave(SQLiteDatabase db) {
		ArrayList<String> artistsalbumsyear;
		//String[] artistsalbumsyear;
		Cursor c;
		artistsalbumsyear = new ArrayList<String>();

		c = db.rawQuery(sqlGetAlbumsIDontHave, null);
		if (c.moveToFirst()) {
			do {
				//artistsalbumsyear[1]= c.getString(0) + ", Album:" + c.getString(1) + "(" + c.getString(2) + ")";
				artistsalbumsyear.add(c.getString(0) + ", Album:" + c.getString(1) + "(" + c.getString(2) + ")");												
			} while (c.moveToNext());
		} else {
			//Log.d("doinbackground", " getAllArtistsIDontHaves returned nothing!!!");
		}
		return artistsalbumsyear;
	}
}
