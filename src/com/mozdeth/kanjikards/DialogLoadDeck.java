package com.mozdeth.kanjikards;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogLoadDeck extends DialogFragment
{
	public static final String CURRENT_DECK_KEY = "DIALOG_SAVE_CURRENT_DECK";
	public static final String REQUESTED_DECK_KEY = "DIALOG_SAVE_REQUESTED_DECK";
	public static final String CHOICES_KEY = "DIALOG_SAVE_CHOICES";
    int m_currentDeck = 0;
	int m_requestedDeck = 0;
	String[] m_choices = null;
    
    //*********************************************************************************************
    
	public interface LoadDeckListener
	{
		public void onLoadDeck(int deck);
	}
	
	LoadDeckListener m_listener = null;
	
	//*********************************************************************************************
	
	public static DialogLoadDeck newInstance(int currentDeck, String[] choices)
	{
		DialogLoadDeck dlg = new DialogLoadDeck();
		
		Bundle args = new Bundle();
		args.putInt(CURRENT_DECK_KEY, currentDeck);
		args.putInt(REQUESTED_DECK_KEY, currentDeck);
		args.putStringArray(CHOICES_KEY, choices);
		dlg.setArguments(args);
		
		return dlg;
	}
	
	//*********************************************************************************************

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
    	if (savedInstanceState == null)
    	{
    		Bundle args = getArguments();
    		if (args != null)
    		{
    			m_currentDeck = args.getInt(CURRENT_DECK_KEY);
        		m_requestedDeck = args.getInt(REQUESTED_DECK_KEY);
        		m_choices = args.getStringArray(CHOICES_KEY);
    		}
    	}
    	else
    	{
    		m_currentDeck = savedInstanceState.getInt(CURRENT_DECK_KEY);
    		m_requestedDeck = savedInstanceState.getInt(REQUESTED_DECK_KEY);
    		m_choices = savedInstanceState.getStringArray(CHOICES_KEY);
    	}
	}
	
	//*********************************************************************************************
	
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // save the listener so we can send events to the host
    	m_listener = (LoadDeckListener)activity;
    }
    
    //*********************************************************************************************
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        builder.setTitle(R.string.dialog_load_deck_title)
               .setSingleChoiceItems(m_choices, m_requestedDeck, new DialogInterface.OnClickListener()
               {
			       @Override
				   public void onClick(DialogInterface dialog, int which)
			       {
			    	   // keep track of which choice the user has selected
			    	   m_requestedDeck = which;
			       }
			   })
               .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener()
               {
                   public void onClick(DialogInterface dialog, int which)
                   {
                	   onOK();
                   }
               })
               .setNegativeButton(R.string.dialog_cancel, null);
        
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    //*********************************************************************************************
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);

    	outState.putInt(CURRENT_DECK_KEY, m_currentDeck);
    	outState.putInt(REQUESTED_DECK_KEY, m_requestedDeck);
    	outState.putStringArray(CHOICES_KEY, m_choices);
    }
    
    //*********************************************************************************************
    
    public void onOK()
    {
    	/*AlertDialog dialog = (AlertDialog)getDialog();
    	ListView lView = dialog.getListView();
    	int requestedDeck = lView.getCheckedItemPosition();*/

    	// only call listener if the deck has changed
    	if (m_requestedDeck != m_currentDeck && m_listener != null)
    	{
    		m_listener.onLoadDeck(m_requestedDeck);
    	}
    }
}
