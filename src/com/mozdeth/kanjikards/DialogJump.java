package com.mozdeth.kanjikards;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogJump extends DialogFragment
{
    public interface JumpListener
    {
        public void onJumpDeck();
    }
    
    JumpListener m_listener = null;
    
    //*********************************************************************************************
    
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // save the listener so we can send events to the host
        m_listener = (JumpListener)activity;
    }
    
    //*********************************************************************************************
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        builder.setMessage(R.string.dialog_jump_message)
               .setPositiveButton(R.string.dialog_jump_button, new DialogInterface.OnClickListener()
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
    
    private void onOK()
    {
        if (m_listener != null)
        {
            m_listener.onJumpDeck();
        }
    }
}
