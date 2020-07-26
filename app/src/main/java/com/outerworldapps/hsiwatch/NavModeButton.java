//    Copyright (C) 2020, Mike Rieker, Beverly, MA USA
//    www.outerworldapps.com
//
//    This program is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; version 2 of the License.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    EXPECT it to FAIL when someone's HeALTh or PROpeRTy is at RISk.
//
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//    http://www.gnu.org/licenses/gpl-2.0.html

package com.outerworldapps.hsiwatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Destination Waypoint Entry and Nav Mode Selection page
 */
public class NavModeButton {
    private HashMap<NavDialView.Mode,RadioButton> navModeButtons;
    private MainActivity mainActivity;
    private RadioGroup modeGroup;
    public TextView identDescr;
    private View modePageView;
    public  WayptEditText identEntry;

    @SuppressLint({ "InflateParams", "ClickableViewAccessibility" })
    public NavModeButton (MainActivity ma)
    {
        mainActivity = ma;

        LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        modePageView = layoutInflater.inflate (R.layout.mode_page, null);
        identEntry = modePageView.findViewById (R.id.identEntry);
        identDescr = modePageView.findViewById (R.id.identDescr);

        Button voiceButton = modePageView.findViewById (R.id.voiceButton);
        voiceButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                displaySpeechRecognizer ();
            }
        });

        // waypoint entry text box
        final SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
        identEntry.wcl = new WayptEditText.WayptChangeListener () {
            @Override
            public void wayptChanged (Waypt waypt)
            {
                // set it as current waypoint
                SharedPreferences.Editor editr = prefs.edit ();
                String ident = (waypt == null) ? "" : waypt.ident;
                editr.putString ("navWayptId", ident);
                editr.apply ();
                if ((mainActivity.navWaypt == null) && (waypt != null)) {
                    mainActivity.showToast ("click \u25C0BACK to show dial");
                }
                mainActivity.setNavWaypt (waypt);
            }

            @Override
            public void showToast (String msg)
            {
                mainActivity.showToast (msg);
            }

            @Override
            public SQLiteDatabase getSqlDB ()
            {
                return mainActivity.downloadThread.getSqlDB ();
            }
        };
    }

    public View getView ()
    {
        if (modeGroup == null) {
            Button modeBack = modePageView.findViewById (R.id.modeBack);
            modeBack.setOnClickListener (mainActivity.backButtonListener);
            modeGroup = modePageView.findViewById (R.id.modeGroup);

            // set up what to do when a radio button gets clicked
            View.OnClickListener radioButtonListener = new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    NavDialView.Mode newmode = (NavDialView.Mode) v.getTag ();
                    mainActivity.setNavMode (newmode);
                    if ((newmode != NavDialView.Mode.OFF) && (mainActivity.navWaypt != null) && (mainActivity.curLoc != null)) {
                        mainActivity.setStartLatLon (mainActivity.curLoc.latitude, mainActivity.curLoc.longitude);
                        double newobs = mainActivity.navWaypt.getMagRadTo (newmode, mainActivity.curLoc);
                        mainActivity.navDialView.setObs (newobs);
                    }
                }
            };

            // make map of radio buttons for all the modes
            HashMap<NavDialView.Mode,RadioButton> nmb = new HashMap<> ();
            for (NavDialView.Mode mode : NavDialView.Mode.values ()) {
                RadioButton rb = new RadioButton (mainActivity);
                rb.setTag (mode);
                String modestr = mode.toString ();
                if (mode == NavDialView.Mode.GCT) modestr = "GreatCircleTrack";
                rb.setText (modestr);
                rb.setOnClickListener (radioButtonListener);
                nmb.put (mode, rb);
            }

            navModeButtons = nmb;
        }

        // build group of buttons allowed for the waypoint type
        setMode ();

        // display radio button page
        return modePageView;
    }

    // waypoint changed, set radio buttons for the waypoint's default mode
    public void setMode ()
    {
        if (navModeButtons != null) {

            // build group of buttons allowed for the waypoint type
            // check the one for the current waypoint
            NavDialView.Mode oldmode = mainActivity.navDialView.getMode ();
            modeGroup.removeAllViews ();
            NavDialView.Mode[] valids = (mainActivity.navWaypt == null) ?
                new NavDialView.Mode[] { NavDialView.Mode.OFF } :
                    mainActivity.navWaypt.validModes;
            for (NavDialView.Mode newmode : valids) {
                RadioButton rb = navModeButtons.get (newmode);
                assert rb != null;
                rb.setChecked (newmode == oldmode);
                modeGroup.addView (rb);
            }
        }
    }

    /**
     * Voice recognition
     */
    private final static int SPEECH_REQUEST_CODE = 0;

    private final static HashMap<String,Character> letters = createLetters ();
    private static HashMap<String,Character> createLetters ()
    {
        HashMap<String,Character> hm = new HashMap<> ();
        hm.put ("0", '0');
        hm.put ("1", '1');
        hm.put ("2", '2');
        hm.put ("3", '3');
        hm.put ("4", '4');
        hm.put ("5", '5');
        hm.put ("6", '6');
        hm.put ("7", '7');
        hm.put ("8", '8');
        hm.put ("9", '9');
        hm.put ("alpha", 'A');
        hm.put ("ate", '8');
        hm.put ("bravo", 'B');
        hm.put ("charlie", 'C');
        hm.put ("ciara", 'S');
        hm.put ("delta", 'D');
        hm.put ("echo", 'E');
        hm.put ("eight", '8');
        hm.put ("fife", '5');
        hm.put ("five", '5');
        hm.put ("for", '4');
        hm.put ("four", '4');
        hm.put ("fox", 'F');
        hm.put ("foxtrot", 'F');
        hm.put ("golf", 'G');
        hm.put ("hotel", 'H');
        hm.put ("india", 'I');
        hm.put ("juliet", 'J');
        hm.put ("kilo", 'K');
        hm.put ("lima", 'L');
        hm.put ("mike", 'M');
        hm.put ("nine", '9');
        hm.put ("niner", '9');
        hm.put ("november", 'N');
        hm.put ("off", '@');
        hm.put ("one", '1');
        hm.put ("oscar", 'O');
        hm.put ("papa", 'P');
        hm.put ("quebec", 'Q');
        hm.put ("romeo", 'R');
        hm.put ("seven", '7');
        hm.put ("sierra", 'S');
        hm.put ("six", '6');
        hm.put ("tango", 'T');
        hm.put ("three", '3');
        hm.put ("tree", '3');
        hm.put ("to", '2');
        hm.put ("too", '2');
        hm.put ("two", '2');
        hm.put ("uniform", 'U');
        hm.put ("victor", 'V');
        hm.put ("whiskey", 'W');
        hm.put ("won", '1');
        hm.put ("x-ray", 'X');
        hm.put ("xray", 'X');
        hm.put ("yankee", 'Y');
        hm.put ("zero", '0');
        hm.put ("zulu", 'Z');
        return hm;
    }

    // https://developer.android.com/training/wearables/apps/voice#java
    private void displaySpeechRecognizer ()
    {
        Intent intent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra (RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra (RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        try {
            mainActivity.startActivityForResult (intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException anfe) {
            Log.w (MainActivity.TAG, "error starting ACTION_RECOGNIZE_SPEECH", anfe);
            mainActivity.showToast ("device does not support voice input");
        }
    }

    // got speech decoding result
    public void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        Log.d (MainActivity.TAG, "speech requestCode=" + requestCode + " resultCode=" + resultCode);

        if ((requestCode == SPEECH_REQUEST_CODE) && (resultCode == Activity.RESULT_OK)) {

            // translate prowords into string of letters and numbers for waypoint ident
            StringBuilder sb = new StringBuilder ();
            List<String> results = data.getStringArrayListExtra (RecognizerIntent.EXTRA_RESULTS);
            boolean turnedOff = false;
            for (String result : results) {
                Log.d (MainActivity.TAG, "speech result='" + result + "'");
                for (String word : result.replace ("/", " ").split (" ")) {
                    Character ch = letters.get (word.toLowerCase (Locale.US));
                    if (ch != null) {
                        //noinspection SwitchStatementWithTooFewBranches
                        switch (ch) {
                            case '@': {
                                turnedOff = true;
                                break;
                            }
                            default: {
                                sb.append (ch);
                                break;
                            }
                        }
                    }
                }
            }

            // if turned off, clear input box and set waypoint to null
            if (turnedOff) {
                identEntry.setText ("");
                identEntry.wcl.wayptChanged (null);
            } else if (sb.length () > 0) {

                // pretend like those letters and numbers were typed in waypoint ident box
                identEntry.setText (sb);
                if (identEntry.onEnterKey (identEntry)) {

                    // failed to look it up in database, restore text box and re-open speech input
                    identEntry.setText ((mainActivity.navWaypt == null) ? "" : mainActivity.navWaypt.ident);
                    displaySpeechRecognizer ();
                }
            }
        }
    }
}
