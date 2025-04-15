package com.mozdeth.kanjikards.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import java.util.*;
//import android.util.Log;

import com.mozdeth.kanjikards.R;

public class DatabaseHelper extends SQLiteOpenHelper
{	
	static DatabaseHelper m_dbHelper = null; // the one and only instance
	
	public static DatabaseHelper getInstance(Context context, SharedPreferences sharedPref, Resources rsrc)
	{
		if (m_dbHelper == null)
		{
			m_dbHelper = new DatabaseHelper(context);
			m_dbHelper.loadPreferences(context, sharedPref, rsrc);
			//Log.v("MainActivity", "dbHelper created and preferences loaded");
		}
		return m_dbHelper;
	}
	
	//*************************************************************************
	
	ArrayList<KanjiEntry> m_allKanji = null;
	
	// each deck is an array of Kanji ids
	public static final int NUMBER_OF_DECKS = 4;
	ArrayList<ArrayList<Integer>> m_decks = null;
	ArrayList<ArrayList<Integer>> m_decksRandom = null;
	
	int[] m_deckIndexes = {-1, -1, -1, -1}; // stores the current index for each deck
	int[] m_deckIndexesRandom = {-1, -1, -1, -1}; // stores the current index for each deck after randomization
	
	int m_currentDeckNumber = CARD_DECK_UNLEARNED;
	
	boolean m_isKanjiLoaded = false;
	boolean m_backOfCardVisible = false;
	
	boolean m_random = false;
	boolean m_animationOn = true;
	
	Object m_loadedLock = new Object();
	Object m_updateDbLock = new Object();
	
	//*********************************************************************************************
	
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "kanji.db";
    
    // labels for the table and each column
    public static abstract class TableLabels implements BaseColumns
    {
	    public static final String TABLE_NAME = "KanjiTable";
	    // ._ID is inherited, the id starts at 1 and increments from there
	    public static final String COLUMN_NAME_KANJI = "kanji";
	    public static final String COLUMN_NAME_ON = "onReading"; // SQL does not like "on"
	    public static final String COLUMN_NAME_KUN = "kunReading";
	    public static final String COLUMN_NAME_ENGLISH = "english";
	    public static final String COLUMN_DECK = "carddeck";
	    public static final String COLUMN_USER_MODIFIED = "usermodified";
    }
    
    // enums for the different decks of cards
    //public static final int CARD_DECK_EASY = 0;
    //public static final int CARD_DECK_DIFFICULT = 1;
    //public static final int CARD_DECK_LEARNING = 2;
    public static final int CARD_DECK_UNLEARNED = 3;
    
    //*********************************************************************************************
    
    // SQL commands
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + TableLabels.TABLE_NAME + " (" +
        				  TableLabels._ID + " INTEGER PRIMARY KEY," +
        				  TableLabels.COLUMN_NAME_KANJI + TEXT_TYPE + COMMA_SEP +
                          TableLabels.COLUMN_NAME_ON + TEXT_TYPE + COMMA_SEP +
                          TableLabels.COLUMN_NAME_KUN + TEXT_TYPE + COMMA_SEP +
                          TableLabels.COLUMN_NAME_ENGLISH + TEXT_TYPE + COMMA_SEP +
                          TableLabels.COLUMN_DECK + INTEGER_TYPE + COMMA_SEP +
                          TableLabels.COLUMN_USER_MODIFIED + INTEGER_TYPE +
        " )";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TableLabels.TABLE_NAME;

    //*********************************************************************************************
    
    // Constructor
    private DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    //*********************************************************************************************
    
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_ENTRIES);
        
        // populate database, this only happens the first time you run the app
        //Log.v("MainActivity", "populating database");
        DatabasePopulator.populateDatabase(db);
    }

    //*********************************************************************************************
    
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    	db.execSQL(SQL_DELETE_ENTRIES);
    	onCreate(db);
    }
    
    //*********************************************************************************************
    
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // upgrade policy is to simply discard the data and start over
        //db.execSQL(SQL_DELETE_ENTRIES);
        //onCreate(db);
    	
    	// onUpgrade is called when the DATABASE_VERSION number changes
    	// update the database with the new on/kun/english but leave the deck numbers
    	DatabasePopulator.updateDatabase(db);
    }
    
    //*********************************************************************************************
    
    public void loadAllKanjiFromDatabase()
    {
    	//Log.v("MainActivity", "loadAllKanjiFromDatabase called");

    	synchronized (m_updateDbLock)
    	{
    		if (isKanjiLoaded())
        	{
    			//Log.v("MainActivity", "not loading database");
        		return;
        	}
    		
	    	//Log.v("MainActivity", "loading database");
	    	
	    	SQLiteDatabase db = getReadableDatabase();
	
			// Define a projection that specifies which columns from the database
			// you will actually use after this query.
			String[] projection = {TableLabels._ID, 
					TableLabels.COLUMN_NAME_KANJI, 
					TableLabels.COLUMN_NAME_ON, 
					TableLabels.COLUMN_NAME_KUN, 
					TableLabels.COLUMN_NAME_ENGLISH,
					TableLabels.COLUMN_DECK};
	
			// How you want the results sorted in the resulting Cursor
			String sortOrder = TableLabels._ID + " ASC"; // ASC for ascending, DESC for descending
	
			Cursor c = db.query(
				TableLabels.TABLE_NAME,  // The table to query
			    projection,                               // The columns to return
			    null,                                // The columns for the WHERE clause
			    null,                            // The values for the WHERE clause
			    null,                                     // don't group the rows
			    null,                                     // don't filter by row groups
			    sortOrder                                 // The sort order
			    );
			
			// set the capacity, just like reserve with vectors in C++
			m_allKanji = new ArrayList<KanjiEntry>(c.getCount());
			
			// allocate space for the decks
			m_decks = new ArrayList<ArrayList<Integer>>(NUMBER_OF_DECKS);
			m_decksRandom = new ArrayList<ArrayList<Integer>>(NUMBER_OF_DECKS);
			for (int i = 0; i < NUMBER_OF_DECKS; i++)
			{
				m_decks.add(new ArrayList<Integer>(c.getCount()));
				m_decksRandom.add(new ArrayList<Integer>());
			}
			
			c.moveToFirst();
			do
			{
				int id = (int)c.getLong(c.getColumnIndex(TableLabels._ID));
				String kanji = c.getString(c.getColumnIndex(TableLabels.COLUMN_NAME_KANJI));
				String on = c.getString(c.getColumnIndex(TableLabels.COLUMN_NAME_ON));
				String kun = c.getString(c.getColumnIndex(TableLabels.COLUMN_NAME_KUN));
				String english = c.getString(c.getColumnIndex(TableLabels.COLUMN_NAME_ENGLISH));
				int deck = (int)c.getLong(c.getColumnIndex(TableLabels.COLUMN_DECK));
				
				KanjiEntry entry = new KanjiEntry(id, kanji, on, kun, english, deck);
				m_allKanji.add(entry);
				
				// keep track of which deck this card is in
				m_decks.get(deck).add(id);
			} while (c.moveToNext());
			
			//Log.v("MainActivity", "done loading database");
			
			db.close();
			
			setKanjiLoaded();
    	}
    }

    //*********************************************************************************************
    
    private void setKanjiLoaded()
	{
    	synchronized (m_loadedLock)
    	{
    		m_isKanjiLoaded = true;
    	}
	}

    //*********************************************************************************************
    
    public boolean isKanjiLoaded()
    {
    	boolean retVal = false;
    	
    	synchronized (m_loadedLock)
    	{
    		retVal = m_isKanjiLoaded;
    	}
    	
    	return retVal;
    }
    
    //*********************************************************************************************
    
    public void setBackOfCardVisible(boolean isVisible)
    {
    	m_backOfCardVisible = isVisible;
    }
    
    //*********************************************************************************************
    
    public boolean isBackOfCardVisible()
    {
    	return m_backOfCardVisible;
    }
    
    //*********************************************************************************************
    
    public void setRandom(boolean random)
    {
    	m_random = random;
    }
    
    //*********************************************************************************************
    
    public boolean isRandom()
    {
    	return m_random;
    }
    
    //*********************************************************************************************
    
    public void setAnimationOn(boolean animationOn)
    {
    	m_animationOn = animationOn;
    }
    
    //*********************************************************************************************
    
    public boolean isAnimationOn()
    {
    	return m_animationOn;
    }
    
    //*********************************************************************************************
    
    public void updateDeckInDatabase(int id, int requestedDeck)
    {
		synchronized (m_updateDbLock)
		{
	    	SQLiteDatabase db = getWritableDatabase();
	    	
	    	// New value for one column
	    	ContentValues values = new ContentValues();
	    	values.put(TableLabels.COLUMN_DECK, requestedDeck);
	
	    	// Which row to update, based on the ID
	    	String selection = TableLabels._ID + " = ?"; // the ? correlates to the arg in the next line
	    	String[] selectionArgs = { String.valueOf(id) };
	
	    	db.update(
	    	    TableLabels.TABLE_NAME,
	    	    values,
	    	    selection,
	    	    selectionArgs);
	    	
	    	db.close();
		}
    }

    //*********************************************************************************************
    
    public void updateStringsInDatabase(int id, String onReading, String kunReading, String english)
    {
    	synchronized (m_updateDbLock)
		{
	    	SQLiteDatabase db = getWritableDatabase();
	    	
	    	// New values for the columns
	    	ContentValues values = new ContentValues();
	    	values.put(TableLabels.COLUMN_NAME_ON, onReading);
			values.put(TableLabels.COLUMN_NAME_KUN, kunReading);
			values.put(TableLabels.COLUMN_NAME_ENGLISH, english);
			values.put(TableLabels.COLUMN_USER_MODIFIED, 1); // mark that the entry was modified by the user
	
	    	// Which row to update, based on the ID
	    	String selection = TableLabels._ID + " = ?"; // the ? correlates to the arg in the next line
	    	String[] selectionArgs = { String.valueOf(id) };
	
	    	db.update(
	    	    TableLabels.TABLE_NAME,
	    	    values,
	    	    selection,
	    	    selectionArgs);
	    	
	    	db.close();
		}
    }
    
    //*********************************************************************************************
    
    public int getCurrentDeck()
    {
    	return m_currentDeckNumber;
    }

    //*********************************************************************************************
    
    public void loadDeck(int requestedDeck)
    {
    	m_currentDeckNumber = requestedDeck;
    }

    //*********************************************************************************************
    
    public void jumpToBeginningOfDeck()
    {
    	if (m_decks == null)
    	{
    		return;
    	}
    	
    	ArrayList<Integer> currentDeckArray = m_decks.get(m_currentDeckNumber);
    	
    	if (currentDeckArray.isEmpty())
    	{
    		return;
    	}
    	else
    	{
    		m_deckIndexes[m_currentDeckNumber] = 0;
    	}
    }
    
    //*********************************************************************************************
    
    public int getDeckSize(int deck)
    {
    	if (m_decks == null)
    	{
    		return 0;
    	}
    	else
    	{
    		return m_decks.get(deck).size();
    	}
    }
    
    //*********************************************************************************************
    
    public void initDeckIndex(int deck, int index)
    {
    	if (deck >= 0 && index >= 0)
    	{
    		m_deckIndexes[deck] = index;
    	}
    }

    //*********************************************************************************************
    
    public boolean moveCurrentKanjiToDeck(int requestedDeck)
    {
    	if (!isKanjiLoaded())
    	{
    		return false;
    	}
    	
    	// get current id
    	int id = getCurrentId();
    	
    	// if valid id
    	if (id > 0)
    	{
    		ArrayList<Integer> currentDeckArray = m_decks.get(m_currentDeckNumber);
    		ArrayList<Integer> requestedDeckArray = m_decks.get(requestedDeck);
    		ArrayList<Integer> currentDeckArrayRandom = m_decksRandom.get(m_currentDeckNumber);
    		ArrayList<Integer> requestedDeckArrayRandom = m_decksRandom.get(requestedDeck);
    		
    		// remove id from current array
    		currentDeckArray.remove(Integer.valueOf(id));
    		
    		// if we removed the card at the end of the current deck, need to update the current deck's index
    		int indexOfCurrentDeck = getIndexOfCurrentDeck();
    		if (indexOfCurrentDeck >= currentDeckArray.size())
    		{
    			m_deckIndexes[m_currentDeckNumber]--;
    		}
    		
    		// invalidate the random arrays in case they are being used
    		currentDeckArrayRandom.clear();
    		requestedDeckArrayRandom.clear();
    		
    		// if random is on and there are cards in the current deck, then jump to the next random card
    		if (m_random && currentDeckArray.size() > 0)
    		{
    			getNextId();
    		}

    		// make sure the requested deck still points to the same card after adding the new one
    		if (requestedDeckArray.size() > 0 && 
    			m_deckIndexes[requestedDeck] < requestedDeckArray.size() &&
    			m_deckIndexes[requestedDeck] >= 0)
    		{
    			int currentIdOfRequestedDeck = requestedDeckArray.get(m_deckIndexes[requestedDeck]);
    			
    			if (id < currentIdOfRequestedDeck)
    			{
    				// the card we are adding to this deck will displace the current card,
    				// so we need to update the index so that it still points to the same card
    				m_deckIndexes[requestedDeck] += 1;
    			}
    		}
    		else
    		{
    			// we added a card to an empty deck, we need to update the index
    			m_deckIndexes[requestedDeck] = 0;
    		}
    		
    		// add id to requested deck at the end, then sort by number
    		requestedDeckArray.add(id);
    		Collections.sort(requestedDeckArray);
    		
    		// update the kanji in the master kanji list
    		KanjiEntry currentKanji = getKanjiById(id);
    		currentKanji.setDeck(requestedDeck);
    		
    		// tell the database this kanji belongs to a new deck, offload it to a different thread
    		new DatabaseDeckUpdater(m_dbHelper, id, requestedDeck).execute();
    		return true;
    	}
    	
    	return false;
    }

    //*********************************************************************************************
    
    public boolean updateCurrentKanji(String onReading, String kunReading, String english)
    {
    	if (!isKanjiLoaded())
    	{
    		return false;
    	}
    	
    	// get current id
    	int id = getCurrentId();
    	
    	// if valid id
    	if (id > 0)
    	{
    		// update the master kanji list
    		KanjiEntry entry = getKanjiById(id);
    		entry.setOn(onReading);
    		entry.setKun(kunReading);
    		entry.setEnglish(english);
    		
    		// update the database asynchronously
    		new DatabaseStringUpdater(m_dbHelper, id, onReading, kunReading, english).execute();
    		
    		return true;
    	}
    	
    	return false;
    }
    
    //*********************************************************************************************
    
    public void moveToKanji(int id)
    {
    	// find which deck it is in
    	if (id >= 1 && m_decks != null)
    	{
    		int masterIndex = id - 1; // Kanji IDs start at 1 but indexing starts at 0
    		KanjiEntry entry = m_allKanji.get(masterIndex);
    		
    		m_currentDeckNumber = entry.getDeck();
    		
    		ArrayList<Integer> newDeckArray = m_decks.get(m_currentDeckNumber); // a deck is a list of ids
    		int indexOfDeck = newDeckArray.indexOf(Integer.valueOf(id)); // find where that id is, get its index
    		
    		m_deckIndexes[m_currentDeckNumber] = indexOfDeck;
    	}
    }

    //*********************************************************************************************
    
    public int[] searchForQuery(String query)
    {
    	String noParenthesesQuery = query.replace("(", "").replace(")", "");
    	String lowerCaseQuery = query.toLowerCase(Locale.US);
    	
    	// holds all the ids in a variable sized array
    	ArrayList<Integer> idList = new ArrayList<Integer>();
    	for (KanjiEntry entry : m_allKanji)
    	{
    		if (entry.containsQuery(noParenthesesQuery, lowerCaseQuery))
    		{
    			idList.add(entry.getId());
    		}
    	}
    	
    	// holds all the ids in a fixed length array
    	int[] retArray = new int[idList.size()];
    	for (int i = 0; i < idList.size(); i++)
    	{
    		retArray[i] = idList.get(i);
    	}
    	
    	return retArray;
    }

    //*********************************************************************************************
    
    // from current deck
    public KanjiEntry getKanjiFromCurrentDeckByIndex(int index)
    {
		if (m_decks == null)
    	{
    		return KanjiEntry.blankKanji;
    	}
    	
    	ArrayList<Integer> currentDeckArray = m_decks.get(m_currentDeckNumber);
    	
    	if (currentDeckArray.isEmpty())
    	{
    		return KanjiEntry.blankKanji;
    	}
    	else
    	{
    		if (index < 0 || index >= currentDeckArray.size())
    		{
    			return KanjiEntry.blankKanji;
    		}
    		else
    		{
    			int id = currentDeckArray.get(index);
    			return getKanjiById(id);
    		}
    	}
    }
    
    //*********************************************************************************************
    
    public KanjiEntry getKanjiById(int id)
    {
    	// the IDs of the Kanji start at 1
    	// but the indexing of the ArrayList starts at 0
    	
    	if (id >= 1)
    	{
    		int masterIndex = id - 1;
        	return m_allKanji.get(masterIndex);
    	}
    	else
    	{
    		return KanjiEntry.blankKanji;
    	}
    }

    //*********************************************************************************************
    
    public KanjiEntry getCurrentKanji()
    {
    	int id = getCurrentId();
    	return getKanjiById(id);
    }
    
    //*********************************************************************************************
    
    public int getCurrentId()
    {
    	if (m_decks == null)
    	{
    		return -1;
    	}
    	
    	ArrayList<Integer> currentDeckArray = m_decks.get(m_currentDeckNumber);
    	
    	if (currentDeckArray.isEmpty())
    	{
    		return -1;
    	}
    	else
    	{
    		// at this point we know there are cards in the deck, so if we get an invalid index we need to fix it
    		int indexOfCurrentDeck = m_deckIndexes[m_currentDeckNumber];
    		if (indexOfCurrentDeck < 0 || indexOfCurrentDeck >= currentDeckArray.size())
    		{
    			// oops, fix the index first before returning the id
    			m_deckIndexes[m_currentDeckNumber] = 0;
    			return currentDeckArray.get(0);
    		}
    		else
    		{
    			return currentDeckArray.get(indexOfCurrentDeck);
    		}
    	}
    }
    
    //*********************************************************************************************
    
    public int getIndexOfCurrentDeck()
    {
    	return m_deckIndexes[m_currentDeckNumber];
    }

    //*********************************************************************************************
    
    public int getIndexOfDeck(int deck)
    {
    	if (deck >= 0 && deck < 4)
    	{
    		return m_deckIndexes[deck];
    	}
    	else
    	{
    		return -1;
    	}
    }
    
    //*********************************************************************************************
    
    public KanjiEntry getNextKanji()
    {
    	// get the next id, get it from the current deck
    	int id = getNextId();
    	return getKanjiById(id);
    }
    
    //*********************************************************************************************
    
    @SuppressWarnings("unchecked")
	private int getNextId()
    {
    	ArrayList<Integer> currentDeckArray = m_decks.get(m_currentDeckNumber);
    	ArrayList<Integer> currentDeckArrayRandom = m_decksRandom.get(m_currentDeckNumber);
    	
    	if (m_random)
    	{
    		if (currentDeckArray.size() > 0) // if there are some cards to work with
    		{
    			m_deckIndexesRandom[m_currentDeckNumber]++;
    					
    			if (currentDeckArrayRandom.isEmpty() ||
    				m_deckIndexesRandom[m_currentDeckNumber] >= currentDeckArrayRandom.size() || 
    			    currentDeckArray.size() != currentDeckArrayRandom.size())
    			{
    				// clone, shuffle, start at beginning of randomized deck
    				currentDeckArrayRandom = (ArrayList<Integer>)currentDeckArray.clone();
    				Collections.shuffle(currentDeckArrayRandom);
    				Collections.shuffle(currentDeckArrayRandom);
    				m_decksRandom.set(m_currentDeckNumber, currentDeckArrayRandom);
    				m_deckIndexesRandom[m_currentDeckNumber] = 0;
    			}
    			
    			int kanjiId = currentDeckArrayRandom.get(m_deckIndexesRandom[m_currentDeckNumber]);
    			m_deckIndexes[m_currentDeckNumber] = currentDeckArray.indexOf(kanjiId);
    			
    			// this should never happen, but just in case
    			if (m_deckIndexes[m_currentDeckNumber] == -1)
    			{
    				m_deckIndexes[m_currentDeckNumber] = 0;
    			}
    		}
    		else
    		{
    			m_deckIndexes[m_currentDeckNumber] = -1;
    			m_deckIndexesRandom[m_currentDeckNumber] = -1;
    		}
    	}
    	else
    	{
	    	// update the index to point to a new id
	    	m_deckIndexes[m_currentDeckNumber]++;
	    	if (m_deckIndexes[m_currentDeckNumber] > (currentDeckArray.size() - 1))
	    	{
	    		m_deckIndexes[m_currentDeckNumber] = 0;
	    	}
    	}
    	
    	// return the id that we are now pointing to
    	return getCurrentId();
    }
    
    //*********************************************************************************************
    
    public KanjiEntry getPrevKanji()
    {
    	
    	int id = getPrevId();
    	return getKanjiById(id);
    }
    
    //*********************************************************************************************
    
    @SuppressWarnings("unchecked")
	private int getPrevId()
    {
    	ArrayList<Integer> currentDeckArray = m_decks.get(m_currentDeckNumber);
    	ArrayList<Integer> currentDeckArrayRandom = m_decksRandom.get(m_currentDeckNumber);
    	
    	if (m_random)
    	{
    		if (currentDeckArray.size() > 0) // if there are some cards to work with
    		{
    			m_deckIndexesRandom[m_currentDeckNumber]--;
    			
    			if (currentDeckArrayRandom.isEmpty() ||
        			m_deckIndexesRandom[m_currentDeckNumber] < 0 || 
        			currentDeckArray.size() != currentDeckArrayRandom.size())
    			{
    				// clone, shuffle, start at end of randomized deck
    				currentDeckArrayRandom = (ArrayList<Integer>)currentDeckArray.clone();
    				Collections.shuffle(currentDeckArrayRandom);
    				Collections.shuffle(currentDeckArrayRandom);
    				m_decksRandom.set(m_currentDeckNumber, currentDeckArrayRandom);
    				m_deckIndexesRandom[m_currentDeckNumber] = currentDeckArrayRandom.size() - 1;
    			}
    			
    			int kanjiId = currentDeckArrayRandom.get(m_deckIndexesRandom[m_currentDeckNumber]);
    			m_deckIndexes[m_currentDeckNumber] = currentDeckArray.indexOf(kanjiId);
    			
    			// this should never happen, but just in case
    			if (m_deckIndexes[m_currentDeckNumber] == -1)
    			{
    				m_deckIndexes[m_currentDeckNumber] = currentDeckArray.size() - 1;
    			}
    		}
    		else
    		{
    			m_deckIndexes[m_currentDeckNumber] = -1;
    			m_deckIndexesRandom[m_currentDeckNumber] = -1;
    		}
    	}
    	else
    	{
	    	// update the index to point to a new id
	    	m_deckIndexes[m_currentDeckNumber]--;
	    	if (m_deckIndexes[m_currentDeckNumber] < 0)
	    	{
	    		m_deckIndexes[m_currentDeckNumber] = currentDeckArray.size() - 1;
	    	}
    	}
    	
    	// return the id that we are now pointing to
    	return getCurrentId();
    }
    
    //*********************************************************************************************
    
    public void loadPreferences(Context context, SharedPreferences sharedPref, Resources rsrc)
    {
    	// update DatabaseHelper with the current index for each deck and the current deck, get it from preferences file
		String[] deckStrings = rsrc.getStringArray(R.array.array_decks);
		for (int i = 0; i < deckStrings.length; i++)
		{
			int indexOfDeck = sharedPref.getInt(deckStrings[i], 0);
			initDeckIndex(i, indexOfDeck);
		}
		
		int currentDeck = sharedPref.getInt(rsrc.getString(R.string.current_deck_key), DatabaseHelper.CARD_DECK_UNLEARNED);
		loadDeck(currentDeck);
		
		boolean random = sharedPref.getBoolean(rsrc.getString(R.string.randomize_key), false);
		setRandom(random);
		
		boolean animationOn = sharedPref.getBoolean(rsrc.getString(R.string.animation_key), true);
		setAnimationOn(animationOn);
		
		setBackOfCardVisible(sharedPref.getBoolean(rsrc.getString(R.string.back_of_card_key), false));
    }
    
    //*********************************************************************************************
    
    public void savePreferences(SharedPreferences.Editor editor, Resources rsrc)
    {
    	// save the current index of each deck
		String[] deckStrings = rsrc.getStringArray(R.array.array_decks);
		for (int i = 0; i < deckStrings.length; i++)
		{
			editor.putInt(deckStrings[i], getIndexOfDeck(i));
		}
		
		// save the current deck
		editor.putInt(rsrc.getString(R.string.current_deck_key), getCurrentDeck());
		
		// save the randomize settings
		editor.putBoolean(rsrc.getString(R.string.randomize_key), isRandom());
		
		// save the animation settings
		editor.putBoolean(rsrc.getString(R.string.animation_key), isAnimationOn());
		
		// save whether or not the back of the card is currently shown
		editor.putBoolean(rsrc.getString(R.string.back_of_card_key), isBackOfCardVisible());
    }
}
