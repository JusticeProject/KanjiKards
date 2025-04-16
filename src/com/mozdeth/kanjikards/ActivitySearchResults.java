package com.mozdeth.kanjikards;

import com.mozdeth.kanjikards.database.*;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
//import android.util.Log;

public class ActivitySearchResults extends ListActivity
    implements DatabaseLoader.DatabaseLoadListener
{
    public static final String PASS_RESULTS_KEY = "PASS_SEARCH_KEY";
    public static final String PASS_QUERY_KEY = "PASS_QUERY_KEY";
    
    public static final String SAVE_RESULTS_KEY = "SAVE_RESULTS_KEY";
    public static final String SAVE_QUERY_KEY = "SAVE_QUERY_KEY";
    
    private int[] m_searchResults = null; // a list of kanji ids
    private String m_query = null;
    
    private DatabaseHelper m_dbHelper;

    //*********************************************************************************************
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_loading); // show the layout with the progress bar
        
        Utilities.initJapaneseFont(getAssets());
        Utilities.initDeckNames(getResources().getStringArray(R.array.array_decks));
        
        //Log.v("MainActivity", "SearchResultsActivity.onCreate called");
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        m_dbHelper = DatabaseHelper.getInstance(getBaseContext(), getPreferences(Context.MODE_PRIVATE), getResources());
        
        if (savedInstanceState != null)
        {
            //Log.v("MainActivity", "retrieving saved instance state");
            // if we are being recreated, we already have the results in the bundle
            m_searchResults = savedInstanceState.getIntArray(SAVE_RESULTS_KEY);
            m_query = savedInstanceState.getString(SAVE_QUERY_KEY);
        }
        else
        {
            //Log.v("MainActivity", "retrieving intent");
            // else we are receiving new results
            Intent intent = getIntent();
            m_searchResults = intent.getIntArrayExtra(PASS_RESULTS_KEY);
            m_query = intent.getStringExtra(PASS_QUERY_KEY);
        }
        
        // load the database asynchronously
        new DatabaseLoader(m_dbHelper, this).execute();
    }

    //*********************************************************************************************
    
    public void onDatabaseLoaded()
    {
        //Log.v("MainActivity", "SearchResultsActivity.onDatabaseLoaded called");
        showResultsOnScreen();
    }
    
    //*********************************************************************************************
    
    @Override
    protected void onStop()
    {
        super.onStop();  // Always call the superclass method first
        
        //Log.v("MainActivity", "SearchResultsActivity.onStop called");

        // save the state in case the activity gets destroyed
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (m_dbHelper != null)
        {
            m_dbHelper.savePreferences(editor, getResources());
        }
        
        editor.commit();
    }
    
    //*********************************************************************************************
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        //Log.v("MainActivity", "SearchResultsActivity.onSaveInstanceState called");
        
        outState.putIntArray(SAVE_RESULTS_KEY, m_searchResults);
        outState.putString(SAVE_QUERY_KEY, m_query);
    }
    
    //*********************************************************************************************
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                // why does this recreate the main activity?
                // This is called when the Home (Up) button is pressed in the Action Bar.
                /*Intent parentActivityIntent = new Intent(this, MainActivity.class);
                parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(parentActivityIntent);
                finish();*/
                
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //*********************************************************************************************

    private void showResultsOnScreen()
    {
        // stop showing the progress bar
        setContentView(R.layout.activity_list);
        
        String[] deckNames = Utilities.getDeckNames();
        
        // get the text for all of the search results
        String[] displayValues = new String[m_searchResults.length];
        for (int i = 0; i < displayValues.length; i++)
        {
            KanjiEntry entry = m_dbHelper.getKanjiById(m_searchResults[i]);
            String deck = "";
            if (entry.getDeck() >= 0)
            {
                deck = deckNames[entry.getDeck()];
            }
            displayValues[i] = entry.getKanji() + " (" + deck + " Deck) " + entry.getEnglish();
        }
        
        if (displayValues.length == 0)
        {
            TextView mtText = (TextView)findViewById(android.R.id.empty);
            mtText.setText("Search for '" + m_query + "' returned 0 results");
        }
        
        // tell the listView what String data to display
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayValues);
        setListAdapter(adapter);
        
        ListView list = getListView();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id )
            {
                onSearchResultSelected(position);
            }
        });
        
        list.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            // is there is ListActivity.onBlah function for catching it?
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
                //updateFont(view);
            }
            
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount)
            {
                updateFont(view, visibleItemCount);
            }
        });
    }

    //*********************************************************************************************
    
    private void updateFont(AbsListView absView, int visibleItemCount)
    {
        for (int i = 0; i < visibleItemCount; i++)
        {
            TextView txtView = (TextView)absView.getChildAt(i);
            if (txtView != null)
            {
                txtView.setTypeface(Utilities.getJapaneseFont());
            }
        }
    }
    
    //*********************************************************************************************
    
    private void onSearchResultSelected(int position)
    {
        if (m_searchResults != null && position < m_searchResults.length)
        {
            m_dbHelper.moveToKanji(m_searchResults[position]);
            
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
