package com.mozdeth.kanjikards;

import com.mozdeth.kanjikards.database.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
//import android.util.Log;

public class ActivityEditCard extends Activity
    implements DatabaseLoader.DatabaseLoadListener,
    DialogConfirmation.ConfirmationListener
{
    private static final String SAVE_ON_KEY = "SAVE_ON_KEY";
    private static final String SAVE_KUN_KEY = "SAVE_KUN_KEY";
    private static final String SAVE_ENGLISH_KEY = "SAVE_ENGLISH_KEY";
    private static final String SAVE_FOCUS_KEY = "SAVE_FOCUS_KEY";
    
    private String m_on = null;
    private String m_kun = null;
    private String m_english = null;
    private int m_focusedId = -1;
    
    private DatabaseHelper m_dbHelper;
    
    private boolean m_delayedSave = false;

    //*********************************************************************************************
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading); // show the layout with the progress bar
        
        Utilities.initJapaneseFont(getAssets());
        Utilities.initDeckNames(getResources().getStringArray(R.array.array_decks));
        
        //Log.v("MainActivity", "EditCardActivity.onCreate called");
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        m_dbHelper = DatabaseHelper.getInstance(getBaseContext(), getPreferences(Context.MODE_PRIVATE), getResources());
        
        if (savedInstanceState != null)
        {
            //Log.v("MainActivity", "retrieving saved instance state");
            // if we are being recreated, we already have the data in the bundle
            m_on = savedInstanceState.getString(SAVE_ON_KEY);
            m_kun = savedInstanceState.getString(SAVE_KUN_KEY);
            m_english = savedInstanceState.getString(SAVE_ENGLISH_KEY);
            m_focusedId = savedInstanceState.getInt(SAVE_FOCUS_KEY);
        }
        
        // load the database asynchronously
        new DatabaseLoader(m_dbHelper, this).execute();
    }

    //*********************************************************************************************
    
    public void onDatabaseLoaded()
    {
        //Log.v("MainActivity", "EditCardActivity.onDatabaseLoaded called");
        showEditBoxes();
    }
    
    //*********************************************************************************************
    
    @Override
    protected void onStop()
    {
        super.onStop();  // Always call the superclass method first
        
        //Log.v("MainActivity", "EditCardActivity.onStop called");

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
        
        //Log.v("MainActivity", "EditCardActivity.onSaveInstanceState called");
        
        // get it from the edit boxes or just save it as is
        getTextFromEditBoxes();
        
        outState.putString(SAVE_ON_KEY, m_on);
        outState.putString(SAVE_KUN_KEY, m_kun);
        outState.putString(SAVE_ENGLISH_KEY, m_english);
        outState.putInt(SAVE_FOCUS_KEY, m_focusedId);
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

    private void showEditBoxes()
    {
        // stop showing the progress bar
        setContentView(R.layout.activity_edit_card);
        
        // get the strings from the database if we don't have them already
        if (m_on == null)
        {
            m_on = m_dbHelper.getCurrentKanji().getOn();
        }
        
        if (m_kun == null)
        {
            m_kun = m_dbHelper.getCurrentKanji().getKun();
        }
        
        if (m_english == null)
        {
            m_english = m_dbHelper.getCurrentKanji().getEnglish();
        }

        Button saveButton = (Button)findViewById(R.id.save_button);
        //saveButton.setEnabled(isTextChanged()); // enable or disable the save button based on whether or not the text changed
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // show confirmation dialog, which calls onSaveConfirmed
                DialogConfirmation dlg = new DialogConfirmation();
                dlg.show(getFragmentManager(), "dialogConfirmation");
            }
        });
        
        Button cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onCancelButton();
            }
        });

        // set font for the japanese text boxes, leave the english text box alone
        TextView kanjiTitle = (TextView)findViewById(R.id.titleKanji);
        kanjiTitle.setTypeface(Utilities.getJapaneseFont());
        kanjiTitle.setText(m_dbHelper.getCurrentKanji().getKanji());
        
        EditText editOn = (EditText)findViewById(R.id.editOn);
        editOn.setTypeface(Utilities.getJapaneseFont());
        editOn.setText(m_on);

        EditText editKun = (EditText)findViewById(R.id.editKun);
        editKun.setTypeface(Utilities.getJapaneseFont());
        editKun.setText(m_kun);
        
        EditText editEnglish = (EditText)findViewById(R.id.editEnglish);
        editEnglish.setText(m_english);
        
        // restore the focus to the edit box that had it before
        if (m_focusedId > 0)
        {
            View viewToFocus = findViewById(m_focusedId);
            if (viewToFocus != null)
            {
                viewToFocus.requestFocus();
            }
        }
        
        if (m_delayedSave)
        {
            doSave();
        }
    }
    
    //*********************************************************************************************
    
    @Override
    public void onSaveConfirmed()
    {
        doSave();
    }
    
    //*********************************************************************************************
    
    private void onCancelButton()
    {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
    
    //*********************************************************************************************
    
    private void doSave()
    {
        if (m_dbHelper.isKanjiLoaded())
        {
            m_delayedSave = false;
            
            getTextFromEditBoxes();
            
            if (m_dbHelper.updateCurrentKanji(m_on, m_kun, m_english))
            {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        else
        {
            // set a flag that will save it during onDatabaseLoaded
            m_delayedSave = true;
        }
    }
    
    //*********************************************************************************************
    
    private void getTextFromEditBoxes()
    {
        EditText editOn = (EditText)findViewById(R.id.editOn);
        if (editOn != null)
        {
            m_on = editOn.getText().toString();
        }
        
        EditText editKun = (EditText)findViewById(R.id.editKun);
        if (editKun != null)
        {
            m_kun = editKun.getText().toString();
        }
        
        EditText editEnglish = (EditText)findViewById(R.id.editEnglish);
        if (editEnglish != null)
        {
            m_english = editEnglish.getText().toString();
        }
        
        View layout = findViewById(R.id.editCardLayout);
        if (layout != null)
        {
            View focusedView = layout.findFocus();
            if (focusedView != null)
            {
                m_focusedId = focusedView.getId();
            }
        }
    }
    
    //*********************************************************************************************
    
    /*private boolean isTextChanged()
    {
        KanjiEntry entry = m_dbHelper.getCurrentKanji();
        
        EditText edit = (EditText)findViewById(R.id.editOn);
        String userOnText = edit.getText().toString();
        if (entry.getOn() != userOnText)
        {
            return true; // detected a change
        }
        
        edit = (EditText)findViewById(R.id.editKun);
        String userKunText = edit.getText().toString();
        if (entry.getKun() != userKunText)
        {
            return true; // detected a change
        }
        
        edit = (EditText)findViewById(R.id.editEnglish);
        String userEnglishText = edit.getText().toString();
        if (entry.getEnglish() != userEnglishText)
        {
            return true; // detected a change
        }

        return false;
    }*/
}
