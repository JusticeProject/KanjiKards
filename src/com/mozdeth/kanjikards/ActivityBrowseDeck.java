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

public class ActivityBrowseDeck extends ListActivity
    implements DatabaseLoader.DatabaseLoadListener
{
    private DatabaseHelper m_dbHelper;

    //*********************************************************************************************
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_loading); // show the layout with the progress bar
        
        Utilities.initJapaneseFont(getAssets());
        Utilities.initDeckNames(getResources().getStringArray(R.array.array_decks));
        
        //Log.v("MainActivity", "BrowseDeckActivity.onCreate called");
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        m_dbHelper = DatabaseHelper.getInstance(getBaseContext(), getPreferences(Context.MODE_PRIVATE), getResources());
        
        // this should be available even though the db is not fully loaded, the preferences are loaded by now
        String[] deckNames = getResources().getStringArray(R.array.array_decks);
        String currentDeckName = deckNames[m_dbHelper.getCurrentDeck()];
        setTitle(currentDeckName + " Deck");
        
        // load the database asynchronously
        new DatabaseLoader(m_dbHelper, this).execute();
    }

    //*********************************************************************************************
    
    public void onDatabaseLoaded()
    {
        //Log.v("MainActivity", "BrowseDeckActivity.onDatabaseLoaded called");
        showCardsOnScreen();
    }
    
    //*********************************************************************************************
    
    @Override
    protected void onStop()
    {
        super.onStop();  // Always call the superclass method first
        
        //Log.v("MainActivity", "BrowseDeckActivity.onStop called");

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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //*********************************************************************************************

    private void showCardsOnScreen()
    {
        // stop showing the progress bar
        setContentView(R.layout.activity_list);
        
        // get the text for the entire deck
        String[] displayValues = new String[m_dbHelper.getDeckSize(m_dbHelper.getCurrentDeck())];
        for (int i = 0; i < displayValues.length; i++)
        {
            KanjiEntry entry = m_dbHelper.getKanjiFromCurrentDeckByIndex(i);
            displayValues[i] = entry.getId() + " " + entry.getKanji() + " " + entry.getEnglish(); 
        }
        
        if (displayValues.length == 0)
        {
            String[] deckNames = getResources().getStringArray(R.array.array_decks);
            String currentDeckName = deckNames[m_dbHelper.getCurrentDeck()];
            
            TextView mtText = (TextView)findViewById(android.R.id.empty);
            mtText.setText("No cards in " + currentDeckName + " deck");
        }
        
        // tell the listView what String data to display
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayValues);
        setListAdapter(adapter);
        
        ListView list = getListView();
        
        // set all the listeners for the list view
        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id )
            {
                onCardSelected(position);
            }
        });
        
        list.setOnScrollListener(new AbsListView.OnScrollListener()
        {
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

        // scroll to the current card when the list is first shown
        int position = m_dbHelper.getIndexOfCurrentDeck();
        //list.smoothScrollToPositionFromTop(position, 0); // should work but doesn't seem to
        list.setSelectionFromTop(position, 0);
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
    
    private void onCardSelected(int position)
    {
        KanjiEntry entry = m_dbHelper.getKanjiFromCurrentDeckByIndex(position);
        m_dbHelper.moveToKanji(entry.getId());
        
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }
}
