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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Collection;
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

    private BluetoothGps bluetoothGps;
    public  boolean ambient;
    private boolean autoTunePending;
    private boolean gpsEnabled;
    public  boolean hadPreviouslyAgreed;
    public  boolean isScreenRound;
    public  boolean redRingOn;
    public  Collection<GpsStatus> gpsStatuses;
    public  double latesttc;                // latest GPS truecourse received when above minimum speed
    public  double obsMagVar;               // magnetic variation used with obsSetting
    public  double obsSetting;              // where yellow triangle is on dials (always magnetic)
    public  double startlat;                // GPS received lat,lon when waypoint was selected
    public  double startlon;
    public  DownloadThread downloadThread;
    public  float dotsPerSqIn;
    public  GpsLocation curLoc;
    public  GpsReceiver gpsReceiver;
    public  GpsTransmitter gpsTransmitter;
    public  Handler myHandler;
    private int gpsSourceParamCount;
    private int gpsSourceParamIndex;
    public  int widthPixels;
    public  int heightPixels;
    public  InternalGps internalGps;
    private LatLon newll;
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
    private RadioGroup gpsSource;
    public  RwyDiagView rwyDiagView;
    private SimulatorGps simulatorGps;
    private Stack<View> mainPageStack;
    public  View currentMainPage;
    public  View gpsPageView;
    public  View mapPageView;
    public  View navMainPage;
    public  View rwyPageView;
    public  Waypt navWaypt;
    public  WiFiUDPGps wiFiUDPGps;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics ();
        getWindowManager ().getDefaultDisplay ().getMetrics (metrics);
        dotsPerSqIn = metrics.xdpi * metrics.ydpi;

        curLoc = new GpsLocation ();
        isScreenRound = getResources ().getConfiguration ().isScreenRound ();
        myHandler = new Handler ();
        newll = new LatLon ();
        mainPageStack = new Stack<> ();

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

        LayoutInflater layoutInflater = getLayoutInflater ();
        currentMainPage = navMainPage = layoutInflater.inflate (R.layout.main_page, null);
        setContentView (currentMainPage);

        mapPageView = layoutInflater.inflate (R.layout.map_page, null);
        mapDialView = mapPageView.findViewById (R.id.mapDialView);

        rwyPageView = layoutInflater.inflate (R.layout.rwy_page, null);
        rwyDiagView = rwyPageView.findViewById (R.id.rwyDiagView);

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

        setupGpsReceiver ();

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

        rwyPageView.setScaleX (scale);
        rwyPageView.setScaleY (scale);
        rwyPageView.setTranslationX (xt);
        rwyPageView.setTranslationY (yt);
    }

    @Override
    public void onDestroy ()
    {
        if (navModeButton != null) {
            navModeButton.setMode (NavDialView.Mode.OFF);
            currentMainPage = null;
            activateGPS ();
        }

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
            activateGPS ();
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
            if (currentMainPage instanceof BoxInsetLayoutAmb) {
                ((BoxInsetLayoutAmb) currentMainPage).setAmbient (true);
            }
            airplanePaint.setColor (Color.GRAY);
            gpsReceiver.enterAmbient ();
            navDialView.setAmbient ();
            mapDialView.setAmbient ();
            rwyDiagView.setAmbient ();
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
            if (currentMainPage instanceof BoxInsetLayoutAmb) {
                ((BoxInsetLayoutAmb) currentMainPage).setAmbient (false);
            }
            airplanePaint.setColor (Color.RED);
            navDialView.setAmbient ();
            mapDialView.setAmbient ();
            rwyDiagView.setAmbient ();
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
            activateGPS ();
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
        if (! navWayptId.equals ("")) {
            navModeButton.identEntry.setText (navWayptId);
            SQLiteDatabase sqldb = downloadThread.getSqlDB ();
            if (sqldb != null) {
                LatLon refll = new LatLon ();
                refll.lat = Double.parseDouble (prefs.getString ("navWayptLat", "0.0"));
                refll.lon = Double.parseDouble (prefs.getString ("navWayptLon", "0.0"));
                setNavWaypt (Waypt.find (this, sqldb, navWayptId, refll));
            }
        }
        startlat = Lib.parseDouble (prefs.getString ("startlat", "NaN"));
        startlon = Lib.parseDouble (prefs.getString ("startlon", "NaN"));
    }

    // set up GPS receiver based on what is selected by radio buttons on GPS menu
    @SuppressLint("InflateParams")
    private void setupGpsReceiver ()
    {
        gpsPageView = getLayoutInflater ().inflate (R.layout.gps_page, null);

        Button gpsBack = gpsPageView.findViewById (R.id.gpsBack);
        gpsBack.setOnClickListener (backButtonListener);

        gpsSource = gpsPageView.findViewById (R.id.gpsSource);

        RadioButton gpsSourceInternal = gpsPageView.findViewById (R.id.gpsSourceInternal);
        gpsSourceInternal.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                setNewGpsReceiver (v, internalGps, "internal");
            }
        });

        RadioButton gpsSourceBluetooth = gpsPageView.findViewById (R.id.gpsSourceBluetooth);
        gpsSourceBluetooth.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                setNewGpsReceiver (v, bluetoothGps, "bluetooth");
            }
        });

        RadioButton gpsSourceWiFiUDP = gpsPageView.findViewById (R.id.gpsSourceWiFiUDP);
        gpsSourceWiFiUDP.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                setNewGpsReceiver (v, wiFiUDPGps, "wifiudp");
            }
        });

        RadioButton gpsSourceSimulator = gpsPageView.findViewById (R.id.gpsSourceSimulator);
        gpsSourceSimulator.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                setNewGpsReceiver (v, simulatorGps, "simulator");
            }
        });

        SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        String gpsrcvr = prefs.getString ("gpsrcvr", "internal");
        gpsSourceInternal.setChecked ("internal".equals (gpsrcvr));
        gpsSourceBluetooth.setChecked ("bluetooth".equals (gpsrcvr));
        gpsSourceWiFiUDP.setChecked ("wifiudp".equals (gpsrcvr));
        gpsSourceSimulator.setChecked ("simulator".equals (gpsrcvr));

        internalGps  = new InternalGps  (this);
        bluetoothGps = new BluetoothGps (this);
        wiFiUDPGps   = new WiFiUDPGps   (this);
        simulatorGps = new SimulatorGps (this);

        if (gpsSourceInternal.isChecked  ()) setNewGpsReceiver (gpsSourceInternal,  internalGps,  null);
        if (gpsSourceBluetooth.isChecked ()) setNewGpsReceiver (gpsSourceBluetooth, bluetoothGps, null);
        if (gpsSourceWiFiUDP.isChecked   ()) setNewGpsReceiver (gpsSourceWiFiUDP,   wiFiUDPGps,   null);
        if (gpsSourceSimulator.isChecked ()) setNewGpsReceiver (gpsSourceSimulator, simulatorGps, null);
    }

    // a GPS source radio button was clicked
    // turn off the old GPS receiver
    // select and turn on the new GPS receiver
    private void setNewGpsReceiver (View rb, GpsReceiver rcvr, String name)
    {
        // shut old receiver off
        boolean loc = (gpsReceiver != null) && gpsReceiver.stopLocationSensor ();
        boolean sts = (gpsReceiver != null) && gpsReceiver.stopStatusSensor ();

        // remove old parameters from page
        while (-- gpsSourceParamCount >= 0) {
            gpsSource.removeViewAt (gpsSourceParamIndex);
        }

        // write new receiver to prefs so it will be chosen on next startup
        if (name != null) {
            SharedPreferences prefs = getPreferences (MODE_PRIVATE);
            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("gpsrcvr", name);
            editr.apply ();
        }

        // remember which receiver we are using now
        gpsReceiver = rcvr;

        // add its parameters to screen just below its radio button
        View[] params = rcvr.getParamViews ();
        int n = gpsSource.getChildCount ();
        for (int i = 0; i < n; i ++) {
            if (gpsSource.getChildAt (i) == rb) {
                gpsSourceParamCount = params.length;
                gpsSourceParamIndex = ++ i;
                RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams (
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                for (View pv : params) {
                    gpsSource.addView (pv, i ++, lp);
                }
                break;
            }
        }

        // start it up
        if (loc) gpsReceiver.startLocationSensor ();
        if (sts) gpsReceiver.startStatusSensor ();
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
        SharedPreferences prefs = getPreferences (MODE_PRIVATE);
        SharedPreferences.Editor editr = prefs.edit ();
        editr.putString ("navWayptId",  (waypt == null) ? ""    : waypt.ident);
        editr.putString ("navWayptLat", (waypt == null) ? "0.0" : Double.toString (waypt.lat));
        editr.putString ("navWayptLon", (waypt == null) ? "0.0" : Double.toString (waypt.lon));
        editr.apply ();

        navWaypt = waypt;
        if (navWaypt == null) {
            autoTunePending = false;
            setNavMode (NavDialView.Mode.OFF);
            navModeButton.identEntry.setText ("");
            navModeButton.identDescr.setText ("");
        } else {
            navModeButton.identEntry.setText (navWaypt.ident);
            navModeButton.identDescr.setText (navWaypt.name);

            // set initial OBS when we get GPS location
            autoTunePending = true;

            // make sure GPS is on and stays on until changed to OFF mode
            setNavMode (navWaypt.getInitialMode ());
        }
    }

    /**
     * OBS dial was manually rotated.
     */
    public void obsChanged ()
    {
        if (navWaypt != null) {
            switch (navModeButton.getMode ()) {

                // rotate whole course line by moving the start{lat,lon}
                // if failed to converge, leave start{lat,lon} as is
                // ...and updateNavDial() will put OBS dial back when it
                //    updates on-course OBS for GCT mode
                case GCT: {
                    double newobstru = obsSetting - obsMagVar;
                    if (! Double.isNaN (Lib.GCXTKCourse (
                            curLoc.lat, curLoc.lon,
                            navWaypt.lat, navWaypt.lon,
                            startlat, startlon, newobstru, newll))) {
                        setStartLatLon (newll.lat, newll.lon);
                    }
                    break;
                }

                // rotate whole course line by moving the start{lat,lon}
                case VOR: {
                    double distnm = Lib.LatLonDist (navWaypt.lat, navWaypt.lon, startlat, startlon);
                    double newobstru = obsSetting - obsMagVar;
                    double newlat = Lib.LatHdgDist2Lat (navWaypt.lat, newobstru, distnm);
                    double newlon = Lib.LatLonHdgDist2Lon (navWaypt.lat, navWaypt.lon, newobstru, distnm);
                    double wenlat = Lib.LatHdgDist2Lat (navWaypt.lat, newobstru + 180, distnm);
                    double wenlon = Lib.LatLonHdgDist2Lon (navWaypt.lat, navWaypt.lon, newobstru + 180, distnm);
                    double newdiff = Lib.LatLonDist (startlat, startlon, newlat, newlon);
                    double wendiff = Lib.LatLonDist (startlat, startlon, wenlat, wenlon);
                    if (newdiff < wendiff) {
                        wenlat = newlat;
                        wenlon = newlon;
                    }
                    setStartLatLon (wenlat, wenlon);
                    break;
                }

                // localizer is set to the inbound course line only
                case LOC:
                case ILS: {
                    Waypt.LocWaypt locwp = (Waypt.LocWaypt) navWaypt;
                    double distnm = Lib.LatLonDist (locwp.lat, locwp.lon, startlat, startlon);
                    setStartLatLon (
                            Lib.LatHdgDist2Lat (locwp.lat, locwp.thdg + 180.0, distnm),
                            Lib.LatLonHdgDist2Lon (locwp.lat, locwp.lon, locwp.thdg + 180.0, distnm)
                    );
                    break;
                }

                // localizer back-course is set to the outbound course line only
                case LOCBC: {
                    Waypt.LocWaypt locwp = (Waypt.LocWaypt) navWaypt;
                    double distnm = Lib.LatLonDist (locwp.lat, locwp.lon, startlat, startlon);
                    setStartLatLon (
                            Lib.LatHdgDist2Lat (locwp.lat, locwp.thdg, distnm),
                            Lib.LatLonHdgDist2Lon (locwp.lat, locwp.lon, locwp.thdg, distnm)
                    );
                    break;
                }
            }
        }

        // update numeric string and needle deflection
        updateNavDial ();
    }

    /**
     * Got an incoming GPS location.
     */
    public void gpsLocationReceived (GpsLocation location)
    {
        GeomagneticField gmf = new GeomagneticField (
                (float) location.lat, (float) location.lon,
                (float) location.altitude, location.time);
        location.magvar = - gmf.getDeclination();

        // ignore GPS for first 15 sec of every minute
        //if (((System.currentTimeMillis () / 15000) & 3) == 0) return;

        if (gpsTransmitter != null) {
            gpsTransmitter.sendLocation (location);
        }

        curLoc = location;
        if (curLoc.speed > gpsMinSpeedMPS) {
            latesttc = curLoc.truecourse;
        }
        gpslastheardat = System.currentTimeMillis ();

        // new waypoint was just entered and we know where we are
        // set the nav dial initial settings for that waypoint
        if (autoTunePending) {
            autoTunePending = false;
            navWaypt.autoTune (this);
        }

        if ((currentMainPage == menuMainPage.satsMainPage.satsPageView) &&
                (menuMainPage.satsMainPage.gpsStatusView != null)) {
            menuMainPage.satsMainPage.gpsStatusView.invalidate ();
        }

        updateNavDial ();
    }

    // got an incoming GPS status
    public void gpsStatusReceived (Collection<GpsStatus> statuses)
    {
        gpsStatuses = statuses;
        if ((currentMainPage == menuMainPage.satsMainPage.satsPageView) &&
                (menuMainPage.satsMainPage.gpsStatusView != null)) {
            menuMainPage.satsMainPage.gpsStatusView.invalidate ();
        }
    }

    /**
     * Set new nav mode.
     * Maybe turn GPS on or off.
     */
    public void setNavMode (NavDialView.Mode newmode)
    {
        if (newmode == NavDialView.Mode.OFF) {
            setStartLatLon (Double.NaN, Double.NaN);
        }
        navModeButton.setMode (newmode);
        activateGPS ();
    }

    /**
     * Set current course line starting point
     */
    public void setStartLatLon (double lat, double lon)
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
     * Turn the GPS on or off as needed.
     * First time on requires user to give permission.
     * GPS is on whenever (navDialMode != OFF) || (currentMainPage implements KeepGpsOn)
     */
    private void activateGPS ()
    {
        if ((navModeButton.getMode () != NavDialView.Mode.OFF) || (currentMainPage instanceof KeepGpsOn)) {
            if (!gpsEnabled) {
                if (!gpsReceiver.startLocationSensor ()) return;
                gpsEnabled = true;
                flashRedRing.run ();
            }
        } else {
            if (gpsEnabled) {
                gpsReceiver.stopLocationSensor ();
                gpsEnabled = false;
            }
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
                int t = isAmbient () ? 8192 : 1024;
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
        redRingOn = false;
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
                redRingOn = (now & t) != 0;
            }
        }

        // update nav dial
        if (navWaypt != null) navWaypt.updateNeedles (this);
        navDialView.invalidate ();

        // update moving map
        mapDialView.invalidate ();

        // update runway diagram
        rwyDiagView.invalidate ();
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
