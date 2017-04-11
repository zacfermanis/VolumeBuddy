package com.fermanis.volumebuddy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by zacfe on 4/4/2017.
 */

public class NewLocationDialog extends DialogFragment {

    public interface NewLocationDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, LatLng point, Editable text);
    }

    NewLocationDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (NewLocationDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement NewLocationDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        input.setHint("name");
        Bundle bundle  = getArguments();
        Double lat = bundle.getDouble("lat");
        Double longitude = bundle.getDouble("long");
        final LatLng point = new LatLng(lat, longitude);
        builder.setView(input);
        builder.setMessage(R.string.dialog_new_location);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Takes the lat/long that was passed to fragment via bundle, and sends it to the callback
                mListener.onDialogPositiveClick(NewLocationDialog.this, point, input.getText());
            }
        })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled - do nothing.
                   }
               });

        builder.setTitle(R.string.title_new_location);
        return builder.create();
    }

}
