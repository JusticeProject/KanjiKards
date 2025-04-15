package com.mozdeth.kanjikards.database;

import android.os.AsyncTask;
//import android.util.Log;

public class DatabaseDeckUpdater
        extends AsyncTask<Void, Void, Void>
{
	private DatabaseHelper m_dbHelper;
	private int m_id;
	private int m_requestedDeck;

    public DatabaseDeckUpdater(DatabaseHelper dbHelper, int id, int requestedDeck)
    {
    	this.m_dbHelper = dbHelper;
    	this.m_id = id;
    	this.m_requestedDeck = requestedDeck;
    }
    
    //*********************************************************************

    // runs on its own thread
    @Override
    protected Void doInBackground(Void... args)
    {
	   	m_dbHelper.updateDeckInDatabase(m_id, m_requestedDeck);
        return null;
    }
}
