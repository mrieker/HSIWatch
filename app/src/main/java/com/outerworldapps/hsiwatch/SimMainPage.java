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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SimMainPage implements GpsReceiver, Runnable {
    private MainActivity mainActivity;
    private SharedPreferences prefs;
    public  View simPageView;

    public SimMainPage (MainActivity ma)
    {
        mainActivity = ma;
        prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
    }

    @SuppressLint("InflateParams")
    public void show ()
    {
        if (simPageView == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            simPageView = layoutInflater.inflate (R.layout.sim_page, null);
            Button simBack = simPageView.findViewById (R.id.simBack);
            simBack.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    displayOpen = false;
                    if (started && ! ptendTimerPend) {
                        ptendTime = System.currentTimeMillis ();
                        PretendStep ();
                    }
                    mainActivity.onBackPressed ();
                }
            });
            ScrollView simScroll = simPageView.findViewById (R.id.simScroll);
            simScroll.addView (Initialize ());
        }
        mainActivity.showMainPage (simPageView);
        displayOpen = true;
    }

    private boolean displayOpen;
    private boolean ptendTimerPend;
    private boolean started;
    private MyEditText ptendAltitude;
    private MyEditText ptendClimbRt;
    private MyEditText ptendHeading;
    private MyEditText ptendLat;
    private MyEditText ptendLon;
    private MyEditText ptendSpeed;
    private MyEditText ptendTurnRt;
    private long ptendTime;

    @Override
    public boolean startSensor ()
    {
        started = true;
        if (! displayOpen && ! ptendTimerPend) {
            ptendTime = System.currentTimeMillis ();
            PretendStep ();
        }
        return true;
    }

    @Override
    public void stopSensor ()
    {
        started = false;
    }

    @Override
    public void enterAmbient () { }

    @Override
    public void exitAmbient () { }

    // pretendInterval timer expired
    // runs in GUI thread
    @Override  // Runnable
    public void run ()
    {
        ptendTimerPend = false;
        PretendStep ();
    }

    @SuppressLint("SetTextI18n")
    private View Initialize ()
    {
        final CheckBox ptendCheckbox = new CheckBox (mainActivity);
        ptendCheckbox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view)
            {
                boolean enab = ptendCheckbox.isChecked ();
                mainActivity.setSimMode (enab);
            }
        });

        WayptEditText wet = new WayptEditText (mainActivity);
        wet.setEms (4);
        wet.setImeOptions (EditorInfo.IME_ACTION_DONE);
        wet.setInputType (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        wet.wcl = new WayptEditText.WayptChangeListener () {
            @Override
            public void wayptChanged (Waypt waypt)
            {
                ptendLat.setText (Lib.DoubleNTZ (waypt.lat, 6));
                ptendLon.setText (Lib.DoubleNTZ (waypt.lon, 6));

                putPref ("simWaypoint", waypt.ident);
                putPref ("simLatitude", ptendLat.getText ().toString ());
                putPref ("simLongitude", ptendLon.getText ().toString ());
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
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                5, "simLatitude");

        ptendLon = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                5, "simLongitude");

        ptendSpeed = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                3, "simSpeed");

        ptendHeading = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                3, "simHeading");

        ptendAltitude = makeEditText (
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
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

        LinearLayout lena = makeLine (new View[] { ptendCheckbox, TextString ("Pretend to be at ") });
        LinearLayout lwpt = makeLine (new View[] { TextString ("wpt"),    wet });
        LinearLayout llat = makeLine (new View[] { TextString ("lat"),    ptendLat });
        LinearLayout llon = makeLine (new View[] { TextString ("lon"),    ptendLon });
        LinearLayout lspd = makeLine (new View[] { TextString ("speed"),  ptendSpeed,    TextString ("kts") });
        LinearLayout lhdg = makeLine (new View[] { TextString ("hdg"),    ptendHeading,  TextString ("deg") });
        LinearLayout lalt = makeLine (new View[] { TextString ("alt"),    ptendAltitude, TextString ("ft")  });
        LinearLayout ltrt = makeLine (new View[] { TextString ("turnrt"), ptendTurnRt,   TextString ("dps") });
        LinearLayout lcrt = makeLine (new View[] { TextString ("climb"),  ptendClimbRt,  TextString ("fpm") });

        LinearLayout.LayoutParams llpwc = new LinearLayout.LayoutParams (
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llpwc.gravity = Gravity.CENTER;
        linearLayout.addView (lena, llpwc);
        linearLayout.addView (lwpt, llpwc);
        linearLayout.addView (llat, llpwc);
        linearLayout.addView (llon, llpwc);
        linearLayout.addView (lspd, llpwc);
        linearLayout.addView (lhdg, llpwc);
        linearLayout.addView (lalt, llpwc);
        linearLayout.addView (ltrt, llpwc);
        linearLayout.addView (lcrt, llpwc);

        return linearLayout;
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
            public boolean onEnterKey (TextView v)
            {
                putPref (prefName, v.getText ().toString ());
                return false;
            }
            @Override
            public void onBackKey (TextView v) { }
        };
        met.setText (prefs.getString (prefName, ""));
        return met;
    }

    private void putPref (String name, String value)
    {
        SharedPreferences.Editor editr = prefs.edit ();
        editr.putString (name, value);
        editr.apply ();
    }

    /**
     * Called every pretendInterval milliseconds to step simulation.
     */
    @SuppressLint("SetTextI18n")
    private void PretendStep ()
    {
        if (! displayOpen && started) {

            /*
             * Get values input by the user.
             */
            double oldlat = Double.parseDouble (ptendLat.getText ().toString ());
            double oldlon = Double.parseDouble (ptendLon.getText ().toString ());
            double spdkts = 0.0;
            double hdgdeg = 0.0;
            double altft  = 0.0;
            double turnrt = 0.0;
            double climrt = 0.0;
            try { spdkts = Double.parseDouble (ptendSpeed.getText ().toString ()); } catch (NumberFormatException ignored) { }
            try { hdgdeg = Double.parseDouble (ptendHeading.getText ().toString ()); } catch (NumberFormatException ignored) { }
            try { altft  = Double.parseDouble (ptendAltitude.getText ().toString ()); } catch (NumberFormatException ignored) { }
            try { turnrt = Double.parseDouble (ptendTurnRt.getText ().toString ()); } catch (NumberFormatException ignored) { }
            try { climrt = Double.parseDouble (ptendClimbRt.getText ().toString ()); } catch (NumberFormatException ignored) { }

            /*
             * Get updated lat/lon, heading and time.
             */
            long   dtms   = 1000;
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

            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("simLatitude", ptendLat.getText ().toString ());
            editr.putString ("simLongitude", ptendLon.getText ().toString ());
            editr.putString ("simHeading", ptendHeading.getText ().toString ());
            editr.putString ("simAltitude", ptendAltitude.getText ().toString ());
            editr.apply ();

            /*
             * Send the values in the form of a GPS reading to the active screen.
             */
            GpsLocation loc = new GpsLocation ();
            loc.altitude = altft / Lib.FtPerM;
            loc.latitude = newlat;
            loc.longitude = newlon;
            loc.speed = spdkts / Lib.KtPerMPS;
            loc.time = newnow;
            loc.truecourse = hdgtru;
            mainActivity.gpsLocationReceived (loc);

            /*
             * Start the timer to do next interval.
             */
            ptendTimerPend = true;
            mainActivity.myHandler.postDelayed (this, dtms - System.currentTimeMillis () % dtms);
        }
    }
}
