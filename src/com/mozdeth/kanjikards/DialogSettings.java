package com.mozdeth.kanjikards;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogSettings extends DialogFragment
{
	public static final String RANDOM_KEY = "DIALOG_SAVE_RANDOM";
	public static final String ANIMATION_KEY = "DIALOG_SAVE_ANIMATION";
    boolean m_random = false;
    boolean m_animationOn = true;
    
    //*********************************************************************************************
    
	public interface SettingsListener
	{
		public void onSettingsChanged(boolean random, boolean animationOn);
	}
	
	SettingsListener m_listener = null;
	
	//*********************************************************************************************
	
	public static DialogSettings newInstance(boolean random, boolean animationOn)
	{
		DialogSettings dlg = new DialogSettings();
		
		Bundle args = new Bundle();
		args.putBoolean(RANDOM_KEY, random);
		args.putBoolean(ANIMATION_KEY, animationOn);
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
    			m_random = args.getBoolean(RANDOM_KEY);
    			m_animationOn = args.getBoolean(ANIMATION_KEY);
    		}
    	}
    	else
    	{
    		m_random = savedInstanceState.getBoolean(RANDOM_KEY);
    		m_animationOn = savedInstanceState.getBoolean(ANIMATION_KEY);
    	}
	}
	
	//*********************************************************************************************
	
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // save the listener so we can send events to the host
    	m_listener = (SettingsListener)activity;
    }
    
    //*********************************************************************************************
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        String[] choices = {"Randomize", "Card Flip Animation"};
        boolean[] initialChecked = {m_random, m_animationOn};
        
        builder.setTitle(R.string.dialog_settings_title)
               .setMultiChoiceItems(choices, initialChecked, new DialogInterface.OnMultiChoiceClickListener()
               {
            	   @Override
            	   public void onClick(DialogInterface dialog, int which, boolean isChecked)
            	   {
            		   if (0 == which)
            		   {
            			   m_random = isChecked;
            		   }
            		   else if (1 == which)
            		   {
            			   m_animationOn = isChecked;
            		   }
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

    	outState.putBoolean(RANDOM_KEY, m_random);
    	outState.putBoolean(ANIMATION_KEY, m_animationOn);
    }
    
    //*********************************************************************************************
    
    public void onOK()
    {
    	if (m_listener != null)
    	{
    		m_listener.onSettingsChanged(m_random, m_animationOn);
    	}
    }
}
