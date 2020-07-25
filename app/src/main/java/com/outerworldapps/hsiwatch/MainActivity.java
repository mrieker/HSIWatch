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
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.GeomagneticField;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Stack;

import androidx.annotation.NonNull;

public class MainActivity extends WearableActivity {
    public final static String TAG = "HSIWatch";

    public final static double gpsMinSpeedMPS = 3.0;  // must be going this fast for heading valid
    private final static int agreeDays = 60;  // agreement good for this many days
    private final static long gpstimeout_amb = 60000;
    private final static long gpstimeout_nor = 5000;

    public final static int airplaneHeight = 313 - 69;

    private boolean autoTunePending;
    private boolean gpsEnabled;
    public  boolean ambient;
    public  boolean hadPreviouslyAgreed;
    public  boolean isScreenRound;
    public  double startlat;                // GPS received lat,lon when waypoint was selected
    public  double startlon;
    public  DownloadThread downloadThread;
    public  GpsLocation curLoc;
    private GpsReceiver gpsReceiver;
    public  GpsTransmitter gpsTransmitter;
    public  Handler myHandler;
    public  int widthPixels;
    public  int heightPixels;
    public  InternalGps internalGps;
    private long gpslastheardat;
    private long lastBackPressed;
    public  MapDialView mapDialView;
    private MapZoomButton mapBotButton;
    private MapZoomButton mapTopButton;
    public  MenuMainPage menuMainPage;
    public  NavDialView navDialView;
    public  NavModeButton navModeButton;
    public  Paint airplanePaint;
    public  Path airplanePath;
    private Stack<View> mainPageStack;
    private View currentMainPage;
    public  View mapPageView;
    private View navMainPage;
    public  Waypt navWaypt;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);

        AcraApplication.sendReports (this);

        isScreenRound = getResources ().getConfiguration ().isScreenRound ();

        myHandler = new Handler ();

        // make sure they have agreed to little agreement
        final SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        long hasAgreed = prefs.getLong ("hasAgreed", 0);
        hadPreviouslyAgreed = hasAgreed > 0;
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
        DisplayMetrics metrics = new DisplayMetrics ();
        getWindowManager ().getDefaultDisplay ().getMetrics (metrics);
        widthPixels  = metrics.widthPixels;
        heightPixels = metrics.heightPixels;

        mainPageStack = new Stack<> ();

        LayoutInflater layoutInflater = getLayoutInflater ();
        currentMainPage = navMainPage = layoutInflater.inflate (R.layout.main_page, null);
        setContentView (currentMainPage);

        mapPageView = layoutInflater.inflate (R.layout.map_page, null);
        mapDialView = mapPageView.findViewById (R.id.mapDialView);

        // top button zooms out
        mapTopButton = mapPageView.findViewById (R.id.mapZoomOutButton);
        mapTopButton.setChar ('+');
        mapTopButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                mapDialView.incRadius (1);
            }
        });

        // bottom button zooms in
        mapBotButton = mapPageView.findViewById (R.id.mapZoomInButton);
        mapBotButton.setChar ('-');
        mapBotButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                mapDialView.incRadius (-1);
            }
        });

        menuMainPage = new MenuMainPage (this);
        setNavMainPageScale ();

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

    // scale nav main page so nav dial fits screen
    // main_page.xml is based on 320x320 screen
    // scale correspondingly to fit the actual screen size
    // maybe fill chin area, chopping off little bit of gray ring and numbers
    public void setNavMainPageScale ()
    {
        int wp = widthPixels;
        int hp = heightPixels;
        float scale = Math.min (wp, hp) / 320.0F;
        if (menuMainPage.fillChinCkBox.isChecked ()) {
            hp = wp;
            scale = wp / 320.0F;
        }
        float xt = (wp - 320.0F) * scale * 0.5F;
        float yt = (hp - 320.0F) * scale * 0.5F;

        navMainPage.setScaleX (scale);
        navMainPage.setScaleY (scale);
        navMainPage.setTranslationX (xt);
        navMainPage.setTranslationY (yt);

        mapPageView.setScaleX (scale);
        mapPageView.setScaleY (scale);
        mapPageView.setTranslationX (xt);
        mapPageView.setTranslationY (yt);
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
     * Going into ambient mode.
     */
    @Override
    public void onEnterAmbient (Bundle ambientDetails)
    {
        super.onEnterAmbient (ambientDetails);

        ambient = true;
        if ((menuMainPage != null) && (menuMainPage.ambEnabCkBox != null) &&
                menuMainPage.ambEnabCkBox.isChecked ()) {
            airplanePaint.setColor (Color.GRAY);
            gpsReceiver.enterAmbient ();
            navDialView.setAmbient ();
            mapDialView.setAmbient ();
            mapBotButton.setAmbient ();
            mapTopButton.setAmbient ();
        }
    }

    /**
     * Leaving ambient mode.
     */
    @Override
    public void onExitAmbient ()
    {
        ambient = false;
        if ((menuMainPage != null) && (menuMainPage.ambEnabCkBox != null) &&
                menuMainPage.ambEnabCkBox.isChecked ()) {
            airplanePaint.setColor (Color.RED);
            navDialView.setAmbient ();
            mapDialView.setAmbient ();
            mapBotButton.setAmbient ();
            mapTopButton.setAmbient ();
            gpsReceiver.exitAmbient ();
        }

        super.onExitAmbient ();
    }

    /**
     * Pop to previous page when back button pressed.
     */
    public final View.OnClickListener backButtonListener = new View.OnClickListener () {
        @Override
        public void onClick (View v)
        {
            onBackPressed ();
        }
    };

    public View peekBackView ()
    {
        if (mainPageStack.isEmpty ()) return null;
        return mainPageStack.peek ();
    }

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
                    if (! Double.isNaN (Lib.GCXTKCourse (
                            curLoc.latitude, curLoc.longitude,
                            navWaypt.lat, navWaypt.lon,
                            startlat, startlon, newobstru, newll))) {
                        setStartLatLon (newll.lat, newll.lon);
                    }
                }

                // update numeric string and needle deflection
                updateNavDial ();
            }
        };

        // waypoint entry and nav mode selection page
        navModeButton = new NavModeButton (this);

        // airplane icon pointing up with center at (0,0)
        airplanePath = new Path ();
        int acy = 181;
        airplanePath.moveTo (0, 313 - acy);
        airplanePath.lineTo ( -44, 326 - acy);
        airplanePath.lineTo ( -42, 301 - acy);
        airplanePath.lineTo ( -15, 281 - acy);
        airplanePath.lineTo ( -18, 216 - acy);
        airplanePath.lineTo (-138, 255 - acy);
        airplanePath.lineTo (-138, 219 - acy);
        airplanePath.lineTo (-17, 150 - acy);
        airplanePath.lineTo ( -17,  69 - acy);
        airplanePath.cubicTo (0, 39 - acy,
                0, 39 - acy,
                +17, 69 - acy);
        airplanePath.lineTo ( +17, 150 - acy);
        airplanePath.lineTo (+138, 219 - acy);
        airplanePath.lineTo (+138, 255 - acy);
        airplanePath.lineTo ( +18, 216 - acy);
        airplanePath.lineTo ( +15, 281 - acy);
        airplanePath.lineTo ( +42, 301 - acy);
        airplanePath.lineTo ( +44, 326 - acy);
        airplanePath.lineTo (0, 313 - acy);

        airplanePaint = new Paint ();
        airplanePaint.setColor (Color.RED);
        airplanePaint.setStyle (Paint.Style.FILL);

        // maybe load up waypoint from preferences
        SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        String navWayptId = prefs.getString ("navWayptId", "");
        if ((navWayptId != null) && ! navWayptId.equals ("")) {
            navModeButton.identEntry.setText (navWayptId);
            SQLiteDatabase sqldb = downloadThread.getSqlDB ();
            if (sqldb != null) {
                setNavWaypt (Waypt.find (sqldb, navWayptId));
            }
        }
        //noinspection ConstantConditions
        startlat = Double.parseDouble (prefs.getString ("startlat", "0"));
        //noinspection ConstantConditions
        startlon = Double.parseDouble (prefs.getString ("startlon", "0"));
    }

    /**
     * Voice recognition result, pass to waypoint entry screen.
     */
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        navModeButton.onActivityResult (requestCode, resultCode, data);
        super.onActivityResult (requestCode, resultCode, data);
    }

    /**
     * Set the waypoint we are headed to
     * Turn GPS on, set nav dial
     */
    public void setNavWaypt (Waypt waypt)
    {
        navWaypt = waypt;
        if (navWaypt == null) {
            autoTunePending = false;
            setNavMode (NavDialView.Mode.OFF);
            navDialView.drawRedRing (false);
            mapDialView.drawRedRing (false);
            navDialView.setObs (0.0);
            navDialView.setIdent ("");
            navModeButton.identEntry.setText ("");
            navModeButton.identDescr.setText ("");
        } else {
            navDialView.setIdent (waypt.ident);
            navModeButton.identEntry.setText (navWaypt.ident);
            navModeButton.identDescr.setText (navWaypt.name);

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
            setStartLatLon (curLoc.latitude, curLoc.longitude);
            navWaypt.autoTune (this);
        }

        updateNavDial ();
    }

    /**
     * Set current course line starting point
     */
    private void setStartLatLon (double lat, double lon)
    {
        startlat = lat;
        startlon = lon;
        SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        SharedPreferences.Editor editr = prefs.edit ();
        editr.putString ("startlat", Double.toString (startlat));
        editr.putString ("startlon", Double.toString (startlon));
        editr.apply ();
    }

    /**
     * Set new nav mode.
     * Maybe turn GPS on or off.
     */
    public void setNavMode (NavDialView.Mode newmode)
    {
        navDialView.setMode (newmode);
        navModeButton.setMode ();
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
        mapDialView.drawRedRing (flashon);

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
            navDialView.setHSIRotation (dialRotation);

            // update DME distance and time
            double dmenm = Lib.LatLonDist (curLoc.latitude, curLoc.longitude, navWaypt.dme_lat, navWaypt.dme_lon);
            int dmeTimeSec = (int) Math.round (dmenm * Lib.MPerNM / curLoc.speed);
            boolean slant = ! Double.isNaN (navWaypt.elev);
            if (slant) dmenm = Math.hypot (dmenm, curLoc.altitude / Lib.MPerNM - navWaypt.elev / Lib.FtPerNM);
            navDialView.setDme (dmenm, dmeTimeSec, slant);

            // update moving map location
            mapDialView.setLocation (curLoc);
        }
    }

    /**
     * Display toast message.
     *  Input:
     *   msg = String: display message
     *       Runnable: call msg.run()
     */
    public void showToast (Object msg)
    {
        Log.i (TAG, "showToast: " + msg);
        MyToast myToast = new MyToast ();
        myToast.length = Toast.LENGTH_SHORT;
        myToast.msg = msg;
        queueMyToast (myToast);
    }

    public void showToastLong (Object msg)
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
        public Object msg;

        public void show ()
        {
            myToastShowing = this;
            if (msg instanceof Runnable) {
                ((Runnable) msg).run ();
            } else {
                Toast.makeText (MainActivity.this, msg.toString (), length).show ();
            }
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
