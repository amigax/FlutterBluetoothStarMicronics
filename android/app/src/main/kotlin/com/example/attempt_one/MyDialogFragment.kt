package com.example.attempt_one

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class MyDialogFragment( title: String) : DialogFragment() {

    val t=title;


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(t)
                .setPositiveButton("ok",
                    DialogInterface.OnClickListener { dialog, id ->
                        // START THE GAME!
                    })
               // .setNegativeButton("cancel",
               //     DialogInterface.OnClickListener { dialog, id ->
               //         // User cancelled the dialog
               //     })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}