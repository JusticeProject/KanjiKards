package com.mozdeth.kanjikards;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogAbout extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        builder.setTitle(R.string.dialog_about_title)
               .setMessage(R.string.dialog_about_message)
               .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener()
               {
                   public void onClick(DialogInterface dialog, int which)
                   {
                       // Don't do anything special when user clicks OK
                   }
               });
        
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
