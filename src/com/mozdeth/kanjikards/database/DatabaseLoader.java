package com.mozdeth.kanjikards.database;

import android.os.AsyncTask;
//import android.util.Log;

public class DatabaseLoader
        extends AsyncTask<Void, Void, Void>
{
	public interface DatabaseLoadListener
	{
		void onDatabaseLoaded();
	}
	
	//*********************************************************************

	private DatabaseHelper m_dbHelper;
	private DatabaseLoadListener m_listener;

    public DatabaseLoader(DatabaseHelper dbHelper, DatabaseLoadListener listener)
    {
    	this.m_dbHelper = dbHelper;
    	this.m_listener = listener;
    }
    
    //*********************************************************************

    // runs on its own thread
    @Override
    protected Void doInBackground(Void... args)
    {
	   	m_dbHelper.loadAllKanjiFromDatabase();
        return null;
    }
    
    //*********************************************************************
    
    // runs on the UI thread
    @Override
    protected void onPostExecute(Void args)
    {
    	if (m_listener != null)
    	{
    		m_listener.onDatabaseLoaded();
    	}
    }
}
