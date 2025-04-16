package com.mozdeth.kanjikards.database;

import android.os.AsyncTask;
//import android.util.Log;

public class DatabaseStringUpdater
        extends AsyncTask<Void, Void, Void>
{
    private DatabaseHelper m_dbHelper;
    private int m_id;
    private String m_newOn;
    private String m_newKun;
    private String m_newEnglish;

    public DatabaseStringUpdater(DatabaseHelper dbHelper, int id, String newOn, String newKun, String newEnglish)
    {
        this.m_dbHelper = dbHelper;
        this.m_id = id;
        this.m_newOn = newOn;
        this.m_newKun = newKun;
        this.m_newEnglish = newEnglish;
    }
    
    //*********************************************************************

    // runs on its own thread
    @Override
    protected Void doInBackground(Void... args)
    {
           m_dbHelper.updateStringsInDatabase(m_id, m_newOn, m_newKun, m_newEnglish);
        return null;
    }
}
