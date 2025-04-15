package com.mozdeth.kanjikards;

import com.mozdeth.kanjikards.database.*;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;
import android.widget.SearchView;
//import android.util.DisplayMetrics;

public class ActivityMain extends Activity
	implements DialogLoadDeck.LoadDeckListener, 
	DialogMoveCard.MoveCardListener, 
	DatabaseLoader.DatabaseLoadListener,
	DialogSettings.SettingsListener,
	DialogJump.JumpListener,
	TouchDetector.TouchDetectionListener
{
	private DatabaseHelper m_dbHelper;
	private TouchDetector m_touchDetector;
	private MenuItem m_searchMenuItem = null;
	private String m_lastQuery = null;
	private boolean m_movingToSearchActivity = false;
	
    //*********************************************************************************************

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Log.v("MainActivity", "MainActivity.onCreate called");
		
		// print the density of the screen
		//DisplayMetrics metrics = new DisplayMetrics();
		//getWindowManager().getDefaultDisplay().getMetrics(metrics);
		//Log.v("MainActivity", "density=" + metrics.density);
		
		// load the proper Japanese font, don't want to use the stock Chinese font because it 
		// makes the characters look different
		Utilities.initJapaneseFont(getAssets());
		
		// load the deck names for future reference
		Utilities.initDeckNames(getResources().getStringArray(R.array.array_decks));
		
		// create the touch motion detector
		m_touchDetector = null;
		m_touchDetector = new TouchDetector(this, getBaseContext());
		
		// make sure the progress bar is shown and fragments are not
		ProgressBar theProgressBar = (ProgressBar)findViewById(R.id.progressBar1);
		theProgressBar.setVisibility(View.VISIBLE);
		try
		{
			Fragment frag = getFragmentManager().findFragmentById(R.id.container);
			if (frag != null)
			{
				getFragmentManager()
					.beginTransaction()
					.remove(frag)
					.commit();
				//Log.v("MainActivity", "fragment removed");
			}
		}
		catch (Exception e)
		{
		}
		
		// figure out if this is the first launch, if so then we need to show the help dialog
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		boolean firstLaunch = sharedPref.getBoolean(getString(R.string.first_launch_key), true);
		if (firstLaunch)
		{
			showHelpDialog();
		}
		//Log.v("MainActivity", "help dialog launched");
		
		m_dbHelper = DatabaseHelper.getInstance(getBaseContext(), sharedPref, getResources());
		
		//Log.v("MainActivity", "dbHelper retrieved");

		// Populate an ArrayList with all of the KanjiEntry's so that they
		// are readily available when navigating to the next/previous.
		// Building the database takes a few seconds, but loading from the database goes pretty quickly
		new DatabaseLoader(m_dbHelper, this).execute();
	}
	
    //*********************************************************************************************
	
	public void onDatabaseLoaded()
	{
		//Log.v("MainActivity", "MainActivity.onDatabaseLoaded called");
		
		// stop showing the progress bar
		ProgressBar theProgressBar = (ProgressBar)findViewById(R.id.progressBar1);
	    theProgressBar.setVisibility(View.INVISIBLE);
		
		// If there is no saved instance state, add a fragment representing the
        // front/back of the card to this activity. If there is a saved instance state,
        // this fragment will have already been added to the activity.
		//if (getFragmentManager().findFragmentById(R.id.container) == null)
		initializeKanjiOnScreen();
		//Log.v("MainActivity", "kanji initialized on screen");
		
		// set background color of main frame to black, 
		// do it asynchronously because the fragment commit won't be completed yet
		new Handler().post(new Runnable()
		{
            @Override
            public void run()
            {
            	invalidateOptionsMenu();
            	FrameLayout theLayout = (FrameLayout)findViewById(R.id.container);
        		theLayout.setBackgroundColor(0xff000000);
            }
        });
		
		// find the FrameLayout and tell it what to do when clicking it
		FrameLayout theLayout = (FrameLayout)findViewById(R.id.container);
		theLayout.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						// do nothing, if I don't include this I won't get the up action in onTouch
					}
				});

		theLayout.setOnTouchListener(new View.OnTouchListener()
		        {
					@Override
					public boolean onTouch(View v, MotionEvent event)
					{
						return m_touchDetector.handleNewMotionEvent(event);
					}
				});
	}
	
    //*********************************************************************************************
	
	@Override
	protected void onStop()
	{
		super.onStop();  // Always call the superclass method first
		
		//Log.v("MainActivity", "MainActivity.onStop called");

		// save the state in case the activity gets destroyed
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		
		// this is no longer the first launch, make a note of that
		editor.putBoolean(getString(R.string.first_launch_key), false);

		if (m_dbHelper != null)
		{
			m_dbHelper.savePreferences(editor, getResources());
		}
		
		editor.commit();
	}
	
    //*********************************************************************************************
	
	@Override
	public void onResume()
	{
		super.onResume();  // Always call the superclass method first
		m_movingToSearchActivity = false; // reset it
		
		//Log.v("MainActivity", "MainActivity.onResume called");

		// if the progress bar is no longer shown, we need a kanji, make sure there is one on the screen
		// this might be necessary if the app is stopped when the database is being created during it's first run
		//ProgressBar theProgressBar = (ProgressBar)findViewById(R.id.progressBar1);
	    //if (theProgressBar.getVisibility() == View.INVISIBLE && m_dbHelper.isKanjiLoaded())
		if (m_dbHelper.isKanjiLoaded())
	    {
	    	//Log.v("MainActivity", "onResume 2");
	    	//if (getFragmentManager().findFragmentById(R.id.container) == null)
    		//Log.v("MainActivity", "onResume 3");
    		initializeKanjiOnScreen();
	    }
	}
	
	//*********************************************************************************************
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		// save the search query in case the user comes back to it
		if (m_searchMenuItem != null)
		{
			SearchView searchView = (SearchView)m_searchMenuItem.getActionView();
			m_lastQuery = searchView.getQuery().toString();
		}
		
		if (m_movingToSearchActivity == false)
		{
			hideSearchMenu();
		}
	}
	
    //*********************************************************************************************

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		
		//Log.v("MainActivity", "MainActivity.onCreateOptionsMenu called");
		
		if (m_dbHelper.isKanjiLoaded())
		{
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.options_menu, menu);
			
			//Log.v("MainActivity", "menu inflated");
			
			// Associate searchable configuration with the SearchView
			m_searchMenuItem = menu.findItem(R.id.search);
		    SearchView searchView = (SearchView)m_searchMenuItem.getActionView();
		    searchView.setQueryHint(getString(R.string.search_hint));
		    
		    // use recursion to set the font of the searchview to Japanese
		    traverseView(searchView);
		    
		    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		    {
				@Override
				public boolean onQueryTextSubmit(String query)
				{
					//Log.v("MainActivity", "onQueryTextSubmit");
					onSubmitQuery(query);
					return true;
				}
				
				@Override
				public boolean onQueryTextChange(String newText)
				{
					//Log.v("MainActivity", "onQueryTextChange");
					return false;
				}
			});
		}
		
		//Log.v("MainActivity", "onCreateOptionsMenu done");

		return true;
	}
	
	//*********************************************************************************************
	
	private void traverseView(View view)
	{
		try
		{
		    if (view instanceof SearchView)
		    {
		        SearchView v = (SearchView)view;
		        for (int i = 0; i < v.getChildCount(); i++)
		        {
		            traverseView(v.getChildAt(i));
		        }
		    }
		    else if (view instanceof LinearLayout)
		    {
		        LinearLayout ll = (LinearLayout)view;
		        for (int i = 0; i < ll.getChildCount(); i++)
		        {
		            traverseView(ll.getChildAt(i));
		        }
		    }
		    else if (view instanceof EditText)
		    {
		        ((EditText)view).setTypeface(Utilities.getJapaneseFont());
		    }
		    else if (view instanceof TextView)
		    {
		        ((TextView)view).setTypeface(Utilities.getJapaneseFont());
		    }
		}
		catch (Exception e)
		{
		}
	}
	
	//*********************************************************************************************
	
	private void onSubmitQuery(String query)
	{
		// hide keyboard so user doesn't hit enter twice
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		SearchView searchView = (SearchView)m_searchMenuItem.getActionView();
		imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
		
		if (query.equals("scott wrote this"))
		{
			Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.copyright_message), Toast.LENGTH_LONG);
			toast.show();
		}
		else
		{
			// search, returns a list of kanji ids, example: new int[] {1, 3, 5, 7, 8, 9, 10};
			int[] searchResults = m_dbHelper.searchForQuery(query);
			
			// prepare the message that will be sent to the new activity
			Intent intent = new Intent(this, ActivitySearchResults.class);
			intent.putExtra(ActivitySearchResults.PASS_RESULTS_KEY, searchResults);
			intent.putExtra(ActivitySearchResults.PASS_QUERY_KEY, query);
			
			m_movingToSearchActivity = true;
			startActivityForResult(intent, getResources().getInteger(R.integer.SEARCH_REQUEST));
		}
	}
	
    //*********************************************************************************************
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		//Log.v("MainActivity", "MainActivity.onActivityResult called");
		
	    if (requestCode == getResources().getInteger(R.integer.SEARCH_REQUEST))
	    {
	        if (resultCode == RESULT_OK)
	        {
	        	// collapse search menu
	        	hideSearchMenu();

	        	// navigate to the chosen card
	        	initializeKanjiOnScreen();
	        }
	        else
	        {
	        	// else the user hit the back button, reload the search query
	        	if (m_lastQuery != null && m_searchMenuItem != null)
	        	{
	        		SearchView searchView = (SearchView)m_searchMenuItem.getActionView();
	        		m_searchMenuItem.expandActionView();
	        		searchView.setQuery(m_lastQuery, false);
	        	}
	        }
	    }
	    else if (requestCode == getResources().getInteger(R.integer.BROWSE_REQUEST))
	    {
	    	if (resultCode == RESULT_OK)
	    	{
	    		// navigate to the chosen card
	        	initializeKanjiOnScreen();
	    	}
	    }
	    else if (requestCode == getResources().getInteger(R.integer.EDIT_CARD_REQUEST))
	    {
	    	if (resultCode == RESULT_OK)
	    	{
	    		// refresh the card
	        	initializeKanjiOnScreen();
	    	}
	    }
	}

    //*********************************************************************************************
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_browse_deck:
			{
				// prepare the message that will be sent to the new activity
				Intent intent = new Intent(this, ActivityBrowseDeck.class);
				startActivityForResult(intent, getResources().getInteger(R.integer.BROWSE_REQUEST));
				return true;
			}
				
			case R.id.menu_edit_card:
			{
				// prepare the message that will be sent to the new activity
				if (m_dbHelper.getCurrentKanji().getId() > 0)
				{
					Intent intent = new Intent(this, ActivityEditCard.class);
					startActivityForResult(intent, getResources().getInteger(R.integer.EDIT_CARD_REQUEST));
				}
				return true;
			}
				
			case R.id.menu_settings:
				showSettingsDialog();
				return true;
				
			case R.id.menu_help:
				showHelpDialog();
				return true;
				
			case R.id.menu_about:
				showAboutDialog();
				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	//*********************************************************************************************
	
	private void hideSearchMenu()
	{
		// collapsing the search menu is preferred before opening a dialog, because otherwise 
		// when the dialog is dismissed the keyboard will show up right away which is annoying
		if (m_searchMenuItem != null)
		{
			m_searchMenuItem.collapseActionView();
		}
	}
	
	//*********************************************************************************************
	
	// builds the strings that will be displayed on the move card / load deck dialog
	// example: Easy (10 cards)
	public String[] getDialogChoiceStrings()
	{
		String[] baseChoices = Utilities.getDeckNames();
		
		String[] choices = new String[baseChoices.length];
		for (int i = 0; i < choices.length; i++)
		{
			final String SINGULAR = " card)";
			final String PLURAL = " cards)";
			int deckSize = m_dbHelper.getDeckSize(i);
			choices[i] = baseChoices[i] + "  (" + deckSize + (deckSize == 1 ? SINGULAR : PLURAL);
		}

		return choices;
	}

	//*********************************************************************************************
	
	private void showMoveCardDialog()
	{
		hideSearchMenu();
		
		KanjiEntry currentKanji = m_dbHelper.getCurrentKanji();
		if (currentKanji.getId() > 0)
		{
			// valid ID, so allow user to move it
			DialogMoveCard moveDialog = DialogMoveCard.newInstance(currentKanji.getDeck(), getDialogChoiceStrings());
			moveDialog.show(getFragmentManager(), "dialogMoveCard");
			// will call this.onMoveCard when a deck has been chosen
		}
	}
	
	//*********************************************************************************************
	
	private void showLoadDeckDialog()
	{
		hideSearchMenu();
		
		DialogLoadDeck loadDialog = DialogLoadDeck.newInstance(m_dbHelper.getCurrentDeck(), getDialogChoiceStrings());
		loadDialog.show(getFragmentManager(), "dialogLoadDeck");
		// will call this.onLoadDeck when a deck has been chosen
	}

	//*********************************************************************************************
	
	private void showJumpDialog()
	{
		hideSearchMenu();
		
		// only allow jumping if there are cards in the deck
		if (m_dbHelper.getDeckSize(m_dbHelper.getCurrentDeck()) > 0)
		{
			DialogJump dlg = new DialogJump();
			dlg.show(getFragmentManager(), "dialogJump");
			// will call this.onJumpDeck when OK has been pressed
		}
	}

	//*********************************************************************************************
	
	private void showSettingsDialog()
	{
		hideSearchMenu();
		
		DialogSettings dlg = DialogSettings.newInstance(m_dbHelper.isRandom(), m_dbHelper.isAnimationOn());
		dlg.show(getFragmentManager(), "dialogSettings");
		// will call this.onSettingsChanged when OK has been pressed
	}

	//*********************************************************************************************
	
	public void showHelpDialog()
	{
		hideSearchMenu();
		
		DialogHelp helpDialog = new DialogHelp();
		helpDialog.show(getFragmentManager(), "dialogHelp");
	}

	//*********************************************************************************************
	
	public void showAboutDialog()
	{
		hideSearchMenu();
		
		DialogAbout aboutDialog = new DialogAbout();
		aboutDialog.show(getFragmentManager(), "dialogAbout");
	}
	
	//*********************************************************************************************
	
	public void onMoveCard(int requestedDeck)
	{
		if (m_dbHelper.moveCurrentKanjiToDeck(requestedDeck))
		{
			KanjiEntry entry = m_dbHelper.getCurrentKanji();
	
			// show the next card
			Fragment newFragment = m_dbHelper.isBackOfCardVisible() ? CardBackFragment.newInstance(entry) : CardFrontFragment.newInstance(entry);
			
			try
			{
				getFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.animator.card_swipe_left_in, R.animator.card_move_to_deck_out)
					.replace(R.id.container, newFragment)
					.commit();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	//*********************************************************************************************
	
	public void onLoadDeck(int deck)
	{
		if (m_dbHelper.isKanjiLoaded())
		{
			m_dbHelper.loadDeck(deck);
			KanjiEntry entry = m_dbHelper.getCurrentKanji();
			
			// show the current card in the new deck
			Fragment newFragment = m_dbHelper.isBackOfCardVisible() ? CardBackFragment.newInstance(entry) : CardFrontFragment.newInstance(entry);
			
			try
			{
				getFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.animator.card_load_deck_in, R.animator.card_load_deck_out)
					.replace(R.id.container, newFragment)
					.commit();
			}
			catch (Exception e)
			{
			}
		}
	}

	//*********************************************************************************************
	
	public void onJumpDeck()
	{
		if (m_dbHelper.isKanjiLoaded())
		{
			m_dbHelper.jumpToBeginningOfDeck();
			KanjiEntry entry = m_dbHelper.getCurrentKanji();
			
			Fragment newFragment = m_dbHelper.isBackOfCardVisible() ? CardBackFragment.newInstance(entry) : CardFrontFragment.newInstance(entry);
			
			try
			{
				getFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.animator.card_swipe_right_in, R.animator.card_swipe_right_out)
					.replace(R.id.container, newFragment)
					.commit();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	//*********************************************************************************************
	
	public void onSettingsChanged(boolean random, boolean animationOn)
	{
		m_dbHelper.setRandom(random);
		m_dbHelper.setAnimationOn(animationOn);
	}
	
	//*********************************************************************************************
	
	private void initializeKanjiOnScreen()
	{
		KanjiEntry entry = m_dbHelper.getCurrentKanji();
		
		// create new fragment
		Fragment newFragment = m_dbHelper.isBackOfCardVisible() ? CardBackFragment.newInstance(entry) : CardFrontFragment.newInstance(entry);
		
		try
		{
			getFragmentManager()
	            .beginTransaction()
	            .replace(R.id.container, newFragment) // just in case there's already a fragment in there
	            //.add(R.id.container, newFragment)
	            .commit();
		}
		catch (Exception e)
		{
			//Log.v("MainActivity", "caught exception");
		}
	}
	
	//*********************************************************************************************

	@Override
	public void onSwipeRightToLeft()
	{
		KanjiEntry entry = m_dbHelper.getNextKanji();
		
		Fragment newFragment = m_dbHelper.isBackOfCardVisible() ? CardBackFragment.newInstance(entry) : CardFrontFragment.newInstance(entry);
		
		try
		{
			getFragmentManager()
				.beginTransaction()
				.setCustomAnimations(R.animator.card_swipe_left_in, R.animator.card_swipe_left_out)
				.replace(R.id.container, newFragment)
				.commit();
		}
		catch (Exception e)
		{
		}
	}
	
	//*********************************************************************************************

	@Override
	public void onSwipeLeftToRight()
	{
		KanjiEntry entry = m_dbHelper.getPrevKanji();
		
		Fragment newFragment = m_dbHelper.isBackOfCardVisible() ? CardBackFragment.newInstance(entry) : CardFrontFragment.newInstance(entry);
		
		try
		{
			getFragmentManager()
				.beginTransaction()
				.setCustomAnimations(R.animator.card_swipe_right_in, R.animator.card_swipe_right_out)
				.replace(R.id.container, newFragment)
				.commit();
		}
		catch (Exception e)
		{
		}
	}
	
	//*********************************************************************************************

	@Override
	public void onSwipeUp()
	{
		showMoveCardDialog();
	}
	
	//*********************************************************************************************

	@Override
	public void onSwipeDown()
	{
		showLoadDeckDialog();
	}
	
	//*********************************************************************************************

	@Override
	public void onSingleTap()
	{
		KanjiEntry entry = m_dbHelper.getCurrentKanji();
		
		try
		{
			if (m_dbHelper.isBackOfCardVisible())
			{
				if (m_dbHelper.isAnimationOn())
				{
					getFragmentManager()
			        	.beginTransaction()
			            .setCustomAnimations(R.animator.card_flip_left_in, R.animator.card_flip_left_out)
			            .replace(R.id.container, CardFrontFragment.newInstance(entry))
			            .commit();
				}
				else
				{
					getFragmentManager()
			        	.beginTransaction()
			            .replace(R.id.container, CardFrontFragment.newInstance(entry))
			            .commit();
				}
			}
			else
			{
				if (m_dbHelper.isAnimationOn())
				{
					getFragmentManager()
			            .beginTransaction()
			            .setCustomAnimations(R.animator.card_flip_right_in, R.animator.card_flip_right_out)
			            .replace(R.id.container, CardBackFragment.newInstance(entry))
			            .commit();
				}
				else
				{
					getFragmentManager()
		            	.beginTransaction()
		            	.replace(R.id.container, CardBackFragment.newInstance(entry))
		            	.commit();
				}
			}
		}
		catch (Exception e)
		{
		}

		boolean toggle = m_dbHelper.isBackOfCardVisible();
		m_dbHelper.setBackOfCardVisible(!toggle);
	}
	
	//*********************************************************************************************

	@Override
	public void onLongTap()
	{
		showJumpDialog();
	}
}
