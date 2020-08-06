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
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.GeomagneticField;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SimulatorGps implements GpsReceiver, Runnable {
    private final static long dtms = 1000;

    private boolean ptendTimerPend;
    private boolean locationRunning;
    private boolean statusRunning;
    private MainActivity mainActivity;
    private MyEditText ptendAltitude;
    private MyEditText ptendClimbRt;
    private MyEditText ptendHeading;
    private MyEditText ptendLat;
    private MyEditText ptendLon;
    private MyEditText ptendSpeed;
    private MyEditText ptendTurnRt;
    private long ptendTime;
    private SharedPreferences prefs;
    private View[] paramViews;

    public SimulatorGps (MainActivity ma)
    {
        mainActivity = ma;
        prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
    }

    @Override  // GpsReceiver
    public View[] getParamViews ()
    {
        if (paramViews == null) {
            paramViews = initialize ();
        }
        return paramViews;
    }

    @Override
    public boolean startLocationSensor ()
    {
        locationRunning = true;
        if (! ptendTimerPend) {
            ptendTimerPend = true;
            long now = System.currentTimeMillis ();
            ptendTime = now / dtms * dtms;
            mainActivity.myHandler.postDelayed (this, dtms - now % dtms);
        }
        return true;
    }

    @Override
    public boolean startStatusSensor ()
    {
        statusRunning = true;
        return true;
    }

    @Override
    public boolean stopLocationSensor ()
    {
        boolean loc = locationRunning;
        locationRunning = false;
        saveValues ();
        return loc;
    }

    @Override
    public boolean stopStatusSensor ()
    {
        boolean sts = statusRunning;
        statusRunning = false;
        return sts;
    }

    @Override
    public void enterAmbient () { }

    @Override
    public void exitAmbient () { }

    @SuppressLint("SetTextI18n")
    private View[] initialize ()
    {
        WayptEditText wet = new WayptEditText (mainActivity);
        wet.setEms (4);
        wet.setImeOptions (EditorInfo.IME_ACTION_DONE);
        wet.setInputType (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        wet.wcl = new WayptEditText.WayptChangeListener () {
            @Override
            public void wayptChanged (Waypt waypt)
            {
                SharedPreferences.Editor editr = prefs.edit ();
                if (waypt == null) {
                    editr.putString ("simWaypoint", "");
                    ptendLat.setText ("");
                    ptendLon.setText ("");
                } else {
                    editr.putString ("simWaypoint", waypt.ident);
                    ptendLat.setText (Lib.DoubleNTZ (waypt.lat, 6));
                    ptendLon.setText (Lib.DoubleNTZ (waypt.lon, 6));
                }
                editr.putString ("simLatitude", ptendLat.getText ().toString ());
                editr.putString ("simLongitude", ptendLon.getText ().toString ());
                editr.apply ();
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
        wet.setText (prefs.getString ("simWaypoint", ""));

        ptendLat = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED,
                5, "simLatitude");

        ptendLon = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED,
                5, "simLongitude");

        ptendSpeed = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                3, "simSpeed");

        ptendHeading = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED,
                3, "simHeading");

        ptendAltitude = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED,
                4, "simAltitude");

        ptendTurnRt = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED,
                3, "simTurnRate");

        ptendClimbRt = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED,
                3, "simClimbRate");

        /*
         * Layout the screen and display it.
         */
        LinearLayout linearLayout = new LinearLayout (mainActivity);
        linearLayout.setOrientation (LinearLayout.VERTICAL);

        LinearLayout lwpt = makeLine (new View[] { TextString ("wpt"),    wet });
        LinearLayout llat = makeLine (new View[] { TextString ("lat"),    ptendLat });
        LinearLayout llon = makeLine (new View[] { TextString ("lon"),    ptendLon });
        LinearLayout lspd = makeLine (new View[] { TextString ("speed"),  ptendSpeed,    TextString ("kts") });
        LinearLayout lhdg = makeLine (new View[] { TextString ("hdg"),    ptendHeading,  TextString ("deg") });
        LinearLayout lalt = makeLine (new View[] { TextString ("alt"),    ptendAltitude, TextString ("ft")  });
        LinearLayout ltrt = makeLine (new View[] { TextString ("turnrt"), ptendTurnRt,   TextString ("dps") });
        LinearLayout lcrt = makeLine (new View[] { TextString ("climb"),  ptendClimbRt,  TextString ("fpm") });

        return new View[] {
                lwpt, llat, llon, lspd, lhdg, lalt, ltrt, lcrt
        };
    }

    private TextView TextString (String str)
    {
        TextView tv = new TextView (mainActivity);
        tv.setText (str);
        return tv;
    }

    // make a horizontal line out of the given views
    private LinearLayout makeLine (View[] views)
    {
        LinearLayout lp = new LinearLayout (mainActivity);
        lp.setOrientation (LinearLayout.HORIZONTAL);
        for (View v : views) lp.addView (v);
        return lp;
    }

    // make edit text block that will set the given preference
    private MyEditText makeEditText (int inputType, int ems, final String prefName)
    {
        MyEditText met = new MyEditText (mainActivity);
        met.setImeOptions (EditorInfo.IME_ACTION_DONE);
        met.setInputType (inputType);
        met.setEms (ems);
        met.listener = new MyEditText.Listener () {
            @Override
            public void onEnterKey (TextView v)
            {
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putString (prefName, v.getText ().toString ());
                editr.apply ();
            }
            @Override
            public void onBackKey (TextView v) { }
        };
        met.setText (prefs.getString (prefName, ""));
        return met;
    }

    @SuppressLint("ApplySharedPref")
    private void saveValues ()
    {
        SharedPreferences.Editor editr = prefs.edit ();
        editr.putString ("simLatitude", ptendLat.getText ().toString ());
        editr.putString ("simLongitude", ptendLon.getText ().toString ());
        editr.putString ("simHeading", ptendHeading.getText ().toString ());
        editr.putString ("simAltitude", ptendAltitude.getText ().toString ());
        editr.commit ();
    }

    // Called every dtms milliseconds to step simulation
    // dtms timer expired
    // runs in GUI thread
    @Override  // Runnable
    public void run ()
    {
        ptendTimerPend = false;
        if (locationRunning) {
            if (! isDisplayOpen ()) {

                /*
                 * Get values input by the user.
                 */
                double oldlat = 0.0;
                double oldlon = 0.0;
                double spdkts = 0.0;
                double hdgdeg = 0.0;
                double altft  = 0.0;
                double turnrt = 0.0;
                double climrt = 0.0;
                try { oldlat = Double.parseDouble (ptendLat.getText ().toString ()); } catch (NumberFormatException ignored) { }
                try { oldlon = Double.parseDouble (ptendLon.getText ().toString ()); } catch (NumberFormatException ignored) { }
                try { spdkts = Double.parseDouble (ptendSpeed.getText ().toString ()); } catch (NumberFormatException ignored) { }
                try { hdgdeg = Double.parseDouble (ptendHeading.getText ().toString ()); } catch (NumberFormatException ignored) { }
                try { altft  = Double.parseDouble (ptendAltitude.getText ().toString ()); } catch (NumberFormatException ignored) { }
                try { turnrt = Double.parseDouble (ptendTurnRt.getText ().toString ()); } catch (NumberFormatException ignored) { }
                try { climrt = Double.parseDouble (ptendClimbRt.getText ().toString ()); } catch (NumberFormatException ignored) { }

                /*
                 * Get updated lat/lon, heading and time.
                 */
                long   newnow = ptendTime + dtms;
                double distnm = spdkts * dtms / 3600000.0;
                double newhdg = hdgdeg + dtms / 1000.0 * turnrt;
                GeomagneticField gmf = new GeomagneticField ((float) oldlat, (float) oldlon, (float) (altft / Lib.FtPerM), newnow);
                double hdgtru = newhdg + gmf.getDeclination ();
                double newlat = Lib.LatHdgDist2Lat (oldlat, hdgtru, distnm);
                double newlon = Lib.LatLonHdgDist2Lon (oldlat, oldlon, hdgtru, distnm);
                double newalt = altft + dtms / 60000.0 * climrt;

                while (newhdg <=  0.0) newhdg += 360.0;
                while (newhdg > 360.0) newhdg -= 360.0;

                /*
                 * Save the new values in the on-screen boxes.
                 */
                ptendTime = newnow;
                ptendLat.setText (Lib.DoubleNTZ (newlat, 6));
                ptendLon.setText (Lib.DoubleNTZ (newlon, 6));
                ptendHeading.setText (Lib.DoubleNTZ (newhdg, 2));
                ptendAltitude.setText (Lib.DoubleNTZ (newalt, 2));

                /*
                 * Send the values in the form of a GPS reading to the active screen.
                 */
                GpsLocation loc = new GpsLocation ();
                loc.altitude = altft / Lib.FtPerM;
                loc.lat = newlat;
                loc.lon = newlon;
                loc.speed = spdkts / Lib.KtPerMPS;
                loc.time = newnow;
                loc.truecourse = hdgtru;
                mainActivity.gpsLocationReceived (loc);
            }

            /*
             * Start the timer to do next interval.
             */
            ptendTimerPend = true;
            long now = System.currentTimeMillis ();
            mainActivity.myHandler.postDelayed (this, dtms - now % dtms);
        }
    }

    // suspend simulator time while parameter page is open
    private boolean isDisplayOpen ()
    {
        return mainActivity.currentMainPage == mainActivity.gpsPageView;
    }
}
