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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.GeomagneticField;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import androidx.annotation.NonNull;

public class MainActivity extends WearableActivity {
    public final static String TAG = "HSIWatch";

    public final static double gpsMinSpeedMPS = 3.0;  // must be going this fast for heading valid
    private final static int agreeDays = 60;  // agreement good for this many days
    private final static long gpstimeout_amb = 60000;
    private final static long gpstimeout_nor = 5000;

    private boolean autoTunePending;
    private boolean gpsEnabled;
    public  double startlat;                // GPS received lat,lon when waypoint was selected
    public  double startlon;
    public  DownloadThread downloadThread;
    public  GpsLocation curLoc;
    private GpsReceiver gpsReceiver;
    public  GpsTransmitter gpsTransmitter;
    public  Handler myHandler;
    public  InternalGps internalGps;
    private long gpslastheardat;
    private long lastBackPressed;
    public  MenuMainPage menuMainPage;
    public  NavDialView navDialView;
    public  NavModeButton navModeButton;
    private RotateLayout rotateLayout;
    private Stack<View> mainPageStack;
    private View currentMainPage;
    public  Waypt navWaypt;
    private WayptEditText identEntry;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);

        AcraApplication.sendReports (this);

        // make sure they have agreed to little agreement
        final SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        long hasAgreed = prefs.getLong ("hasAgreed", 0);
        if ((System.currentTimeMillis () - hasAgreed) / 86400000L < agreeDays) {
            hasAgreed ();
        } else {
            setContentView (R.layout.agree_page);
            Button acceptButton = findViewById (R.id.acceptButton);
            acceptButton.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    SharedPreferences.Editor editr = prefs.edit ();
                    editr.putLong ("hasAgreed", System.currentTimeMillis ());
                    editr.apply ();
                    hasAgreed ();
                }
            });
            Button declineButton = findViewById (R.id.declineButton);
            declineButton.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    finish ();
                }
            });
        }
    }

    // user has agreed to agreement, either earlier or just now
    @SuppressLint("InflateParams")
    private void hasAgreed ()
    {
        mainPageStack = new Stack<> ();

        LayoutInflater layoutInflater = getLayoutInflater ();
        currentMainPage = layoutInflater.inflate (R.layout.main_page, null);
        setContentView (currentMainPage);

        // main_page.xml is based on 320x320 screen
        // scale correspondingly to fit the actual screen size
        DisplayMetrics metrics = new DisplayMetrics ();
        getWindowManager ().getDefaultDisplay ().getMetrics (metrics);
        int widthPixels  = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        // some watches have a little bit chopped off the bottom
        // .. and so heightPixels is a little smaller than widthPixels
        // so pretend height is the same as width
        // .. the gray ring and some of the bottom number
        //    gets chopped off the bottom
        if (heightPixels < widthPixels) {
            //noinspection SuspiciousNameCombination
            heightPixels = widthPixels;
        }
        float xscale = widthPixels  / 320.0F;
        float yscale = heightPixels / 320.0F;
        currentMainPage.setScaleX (xscale);
        currentMainPage.setScaleY (yscale);
        currentMainPage.setTranslationX ((widthPixels  - 320.0F) * xscale * 0.5F);
        currentMainPage.setTranslationY ((heightPixels - 320.0F) * yscale * 0.5F);

        menuMainPage = new MenuMainPage (this);

        myHandler = new Handler ();

        gpsReceiver = internalGps = new InternalGps (this);

        // enables Always-on
        setAmbientEnabled ();

        // start downloading database if we don't have one
        // also queues an update download timer
        // queues retry timer if fails to download
        downloadThread = new DownloadThread (this);
        downloadThread.getSqlDB ();

        // finish up initializing
        finishInitializing ();
    }

    @Override
    public void onDestroy ()
    {
        deactivateGPS ();
        super.onDestroy ();
    }

    /**
     * Show the given main page and set it up for back key.
     */
    public void showMainPage (View view)
    {
        // make sure there isn't more than one instance of each page in stack
        if (view != currentMainPage) {
            mainPageStack.remove (currentMainPage);
            mainPageStack.push (currentMainPage);
            currentMainPage = view;
            setContentView (view);
        }
    }

    /**
     * Switch between real and simulated.
     */
    public void setSimMode (boolean sim)
    {
        if (gpsEnabled) gpsReceiver.stopSensor ();
        if (sim) {
            gpsReceiver = menuMainPage.simMainPage;
        } else {
            gpsReceiver = internalGps;
        }
        if (gpsEnabled && ! gpsReceiver.startSensor ()) {
            showToast ("failed to start new gps source");
            gpsEnabled = false;
        }
    }

    /**
     * Moved to foreground.  If waypoint tuned in and not turned off, start the GPS.
    @Override
    public void onResume ()
    {
        super.onResume ();
        Log.d (TAG, "onResume*:" + (++ logseq));
        if ((navWaypt != null) && (navDialView.getMode () != NavDialView.Mode.OFF)) {
            activateGPS ();
        }
    }
     */

    /**
     * Moved to background.  Stop the GPS.
    @Override
    public void onPause ()
    {
        deactivategpsat = 0;
        deactivateGPS ();
        Log.d (TAG, "onPause*:" + (++ logseq));
        super.onPause ();
    }
     */

    /**
     * Going into ambient mode.
     */
    @Override
    public void onEnterAmbient (Bundle ambientDetails)
    {
        super.onEnterAmbient (ambientDetails);

        if ((menuMainPage != null) && (menuMainPage.ambEnabCkBox != null) &&
                menuMainPage.ambEnabCkBox.isChecked ()) {
            gpsReceiver.enterAmbient ();
            identEntry.setEnabled (false);
            navDialView.setAmbient (true);
            navModeButton.setAmbient (true);
        }
    }

    /**
     * Update during ambient mode once a minute.
    @Override
    public void onUpdateAmbient ()
    {
        super.onUpdateAmbient ();
        updateNavDial ();
    }
     */

    /**
     * Leaving ambient mode.
     */
    @Override
    public void onExitAmbient ()
    {
        if ((menuMainPage != null) && (menuMainPage.ambEnabCkBox != null) &&
                menuMainPage.ambEnabCkBox.isChecked ()) {
            identEntry.setEnabled (true);
            navDialView.setAmbient (false);
            navModeButton.setAmbient (false);
            gpsReceiver.exitAmbient ();
        }

        super.onExitAmbient ();
    }

    /**
     * Pop to previous page when back button pressed.
     */
    @Override
    public void onBackPressed ()
    {
        if (mainPageStack.isEmpty ()) {
            long now = System.currentTimeMillis ();
            if (now - lastBackPressed < 3000) {
                super.onBackPressed ();
            } else {
                lastBackPressed = now;
                showToast ("back once again in 3 seconds to exit app");
            }
        } else {
            currentMainPage = mainPageStack.pop ();
            setContentView (currentMainPage);
        }
    }

    /**
     * Finish initializing everything.
     */
    @SuppressLint("ClickableViewAccessibility")
    public void finishInitializing ()
    {
        // the OBS dial and needles
        navDialView = findViewById (R.id.navDialView);
        navDialView.revRotate = menuMainPage.hsiModeCkBox.isChecked ();
        navDialView.obsChangedListener = new NavDialView.OBSChangedListener () {
            private LatLon newll = new LatLon ();

            @SuppressLint("SetTextI18n")
            @Override
            public void obsChanged (double obs)
            {
                if ((navWaypt != null) && (curLoc != null) &&
                        (navDialView.getMode () == NavDialView.Mode.GCT)) {
                    // rotate whole course line by moving the start{lat,lon}
                    // if failed to converge, leave start{lat,lon} as is
                    // ...and updateNavDial() will put OBS dial back when it
                    //    updates on-course OBS for GCT mode
                    double newobstru = navDialView.getObs () - curLoc.magvar;
                    if (Lib.GCXTKCourse (curLoc.latitude, curLoc.longitude, navWaypt.lat, navWaypt.lon,
                            startlat, startlon, newobstru, newll)) {
                        startlat = newll.lat;
                        startlon = newll.lon;
                    }
                }

                // update numeric string and needle deflection
                updateNavDial ();
            }
        };

        // background and foreground item rotation
        rotateLayout = findViewById (R.id.rotateLayout);

        // OFF->GCT->VOR->ADF->LOC->LOCBC->ILS mode button
        navModeButton = findViewById (R.id.navModeButton);
        Button navModeButtox = findViewById (R.id.navModeButtox);
        navModeButtox.setOnClickListener (navModeButton);

        // open menu page button
        View.OnClickListener mbcl = new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                menuMainPage.show ();
            }
        };
        MyMenuButton mbv = findViewById (R.id.menuButtonView);
        mbv.setOnClickListener (mbcl);
        Button mbx = findViewById (R.id.menuButtonViex);
        mbx.setOnClickListener (mbcl);

        // waypoint entry text box
        SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        identEntry = findViewById (R.id.identEntry);
        identEntry.wcl = new WayptEditText.WayptChangeListener () {
            @Override
            public void wayptChanged (Waypt waypt)
            {
                // set it as current waypoint
                SharedPreferences prefs = getPreferences (MODE_PRIVATE);
                SharedPreferences.Editor editr = prefs.edit ();
                String ident = (waypt == null) ? "" : waypt.ident;
                editr.putString ("navWayptId", ident);
                editr.apply ();
                setNavWaypt (waypt);
            }

            @Override
            public void showToast (String msg)
            {
                MainActivity.this.showToast (msg);
            }

            @Override
            public SQLiteDatabase getSqlDB ()
            {
                return downloadThread.getSqlDB ();
            }
        };
        identEntry.setOnTouchListener (new View.OnTouchListener () {
            @Override
            public boolean onTouch (View v, MotionEvent event)
            {
                // if database not downloaded, don't bother opening keyboard
                if (identEntry.onTouch (v, event)) return true;

                // if voice not enabled, open keyboard
                if (! menuMainPage.voiceEnCkBox.isChecked ()) return false;

                // if voice input activity successfully started, don't open keyboard
                // if voice input activity failed to start, open keyboard
                return displaySpeechRecognizer ();
            }
        });

        // maybe load up waypoint from preferences
        String navWayptId = prefs.getString ("navWayptId", "");
        if ((navWayptId != null) && ! navWayptId.equals ("")) {
            identEntry.setText (navWayptId);
            SQLiteDatabase sqldb = downloadThread.getSqlDB ();
            if (sqldb != null) {
                setNavWaypt (Waypt.find (sqldb, navWayptId));
            }
        }
    }

    /**
     * Set the waypoint we are headed to
     * Turn GPS on, set nav dial
     */
    private void setNavWaypt (Waypt waypt)
    {
        navWaypt = waypt;
        if (navWaypt == null) {
            autoTunePending = false;
            setNavMode (NavDialView.Mode.OFF);
            navDialView.drawRedRing (false);
            navDialView.setObs (0.0);
        } else {

            // set initial OBS when we get GPS location
            autoTunePending = true;

            // make sure GPS is on and stays on until changed to OFF mode
            setNavMode (navWaypt.getInitialMode ());
        }
    }

    /**
     * Got an incoming GPS location.
     */
    public void gpsLocationReceived (GpsLocation location)
    {
        GeomagneticField gmf = new GeomagneticField (
                (float) location.latitude, (float) location.longitude,
                (float) location.altitude, location.time);
        location.magvar = - gmf.getDeclination();

        // ignore GPS for first 15 sec of every minute
        //if (((now / 15000) & 3) == 0) return;

        if (gpsTransmitter != null) {
            gpsTransmitter.sendLocation (location);
        }

        curLoc = location;
        gpslastheardat = System.currentTimeMillis ();

        // new waypoint was just entered and we know where we are
        // set the nav dial initial settings for that waypoint
        if (autoTunePending) {
            autoTunePending = false;
            startlat = curLoc.latitude;
            startlon = curLoc.longitude;
            navWaypt.autoTune (this);
        }

        updateNavDial ();
    }

    /**
     * Set new nav mode.
     * Maybe turn GPS on or off.
     */
    public void setNavMode (NavDialView.Mode newmode)
    {
        navDialView.setMode (newmode);
        navModeButton.setMode (newmode);
        if (newmode == NavDialView.Mode.OFF) {
            deactivateGPS ();
        } else {
            activateGPS ();
        }
    }

    /**
     * Turn the GPS on if not already.
     * First time requires user to give permission.
     */
    private void activateGPS ()
    {
        if (! gpsEnabled) {
            if (! gpsReceiver.startSensor ()) return;
            showToast ("turned GPS on");
            gpsEnabled = true;
            flashRedRing.run ();
        }
        updateNavDial ();
    }

    /**
     * Turn the GPS off if not already.
     */
    private void deactivateGPS ()
    {
        if (gpsEnabled) {
            showToast ("turning GPS off");
            gpsReceiver.stopSensor ();
            gpsEnabled = false;
        }
        updateNavDial ();
    }

    // Permission granting
    public final static int RC_INTGPS = 9876;
    @Override
    public void onRequestPermissionsResult (int requestCode,
                                            @NonNull String[] permissions,
                                            @NonNull int[] grantResults)
    {
        if (requestCode == RC_INTGPS) {
            activateGPS ();
        }
    }

    /**
     * Runs every 1.024 sec to flash the outer ring red if GPS not heard from in a while.
     * Runs only when the GPS is enabled.
     */
    private final Runnable flashRedRing = new Runnable () {
        @Override
        public void run ()
        {
            if (gpsEnabled) {
                updateNavDial ();
                int t = isAmbient () ? 8192: 1024;
                myHandler.postDelayed (this, t - (System.currentTimeMillis () % t));
            }
        }
    };

    /**
     * Something changed, update nav dial components.
     */
    @SuppressLint("SetTextI18n")
    public void updateNavDial ()
    {
        // flash outer ring red if haven't heard from GPS in a while
        boolean flashon = false;
        if (gpsEnabled) {
            long now = System.currentTimeMillis ();
            long gto = isAmbient () ? gpstimeout_amb : gpstimeout_nor;
            if (now - gpslastheardat > gto) {
                SharedPreferences prefs = getPreferences (MODE_PRIVATE);
                if (! prefs.getBoolean ("shownRedMsg", false)) {
                    showToast ("flashing outer red ring means no GPS signal");
                    SharedPreferences.Editor editr = prefs.edit ();
                    editr.putBoolean ("shownRedMsg", true);
                    editr.apply ();
                }
                int t = isAmbient () ? 8192 : 1024;
                flashon = (now & t) != 0;
            }
        }
        navDialView.drawRedRing (flashon);

        // update nav dial contents
        if (gpsEnabled && (navWaypt != null) && (curLoc != null)) {

            // update nav dial needles for new waypoint and/or new GPS location
            navWaypt.updateNav (this);

            // gps info
            navDialView.setGpsInfo (curLoc);

            // bearing to and from the station
            double radto = navWaypt.getMagRadTo (navDialView.getMode(), curLoc);
            navDialView.setToWaypt (radto);

            // rotate whole dial, text and all, if HSI mode, to put airplane at top
            // otherwise, yellow triangle (OBS setting) stays at top
            boolean hsiEnable = menuMainPage.hsiModeCkBox.isChecked ();
            float dialRotation = hsiEnable ? (float) - navDialView.getHeading () : 0;
            rotateLayout.setRotation (dialRotation);

            // update DME distance and time
            double dmenm = Lib.LatLonDist (curLoc.latitude, curLoc.longitude, navWaypt.dme_lat, navWaypt.dme_lon);
            int dmeTimeSec = (int) Math.round (dmenm * Lib.MPerNM / curLoc.speed);
            boolean slant = ! Double.isNaN (navWaypt.elev);
            if (slant) dmenm = Math.hypot (dmenm, curLoc.altitude / Lib.MPerNM - navWaypt.elev / Lib.FtPerNM);
            navDialView.setDme (dmenm, dmeTimeSec, slant);
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
    private boolean displaySpeechRecognizer ()
    {
        Intent intent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra (RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra (RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        try {
            startActivityForResult (intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException anfe) {
            Log.w (TAG, "error starting ACTION_RECOGNIZE_SPEECH", anfe);
            showToast ("device does not support voice input");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        Log.d (TAG, "speech requestCode=" + requestCode + " resultCode=" + resultCode);

        if ((requestCode == SPEECH_REQUEST_CODE) && (resultCode == RESULT_OK)) {

            // translate prowords into string of letters and numbers for waypoint ident
            StringBuilder sb = new StringBuilder ();
            List<String> results = data.getStringArrayListExtra (RecognizerIntent.EXTRA_RESULTS);
            boolean turnedOff = false;
            for (String result : results) {
                Log.d (TAG, "speech result='" + result + "'");
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
                    identEntry.setText ((navWaypt == null) ? "" : navWaypt.ident);
                    displaySpeechRecognizer ();
                }
            }
        }

        super.onActivityResult (requestCode, resultCode, data);
    }

    /**
     * Display toast message.
     */
    public void showToast (String msg)
    {
        Log.i (TAG, "showToast: " + msg);
        MyToast myToast = new MyToast ();
        myToast.length = Toast.LENGTH_SHORT;
        myToast.msg = msg;
        queueMyToast (myToast);
    }

    public void showToastLong (String msg)
    {
        Log.i (TAG, "showToastLong: " + msg);
        MyToast myToast = new MyToast ();
        myToast.length = Toast.LENGTH_LONG;
        myToast.msg = msg;
        queueMyToast (myToast);
    }

    private void queueMyToast (MyToast myToast)
    {
        if (myToastShowing != null) {
            if (myToast.msg.equals (myToastShowing.msg)) return;
            for (MyToast qd : myToasts) if (myToast.msg.equals (qd.msg)) return;
            myToasts.add (myToast);
        } else {
            myToast.show ();
        }
    }

    private LinkedList<MyToast> myToasts = new LinkedList<> ();
    private MyToast myToastShowing;

    private class MyToast implements Runnable {
        public int length;
        public String msg;

        public void show ()
        {
            myToastShowing = this;
            Toast.makeText (MainActivity.this, msg, length).show ();
            int delay = 0;
            switch (length) {
                case Toast.LENGTH_SHORT: delay = 3000; break;
                case Toast.LENGTH_LONG:  delay = 5000; break;
            }
            myHandler.postDelayed (this, delay);
        }

        @Override
        public void run ()
        {
            MyToast next = myToasts.poll ();
            if (next == null) {
                myToastShowing = null;
            } else {
                next.show ();
            }
        }
    }
}
