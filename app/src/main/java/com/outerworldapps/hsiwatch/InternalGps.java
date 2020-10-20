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

import android.Manifest;
import android.annotation.SuppressLint;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.core.app.ActivityCompat;

/**
 * Use internal GPS receiver to determine location.
 */
@SuppressLint("SetTextI18n")
public class InternalGps extends GnssStatus.Callback implements GpsReceiver, LocationListener {
    private final static int rate_amb = 20000;
    private final static int rate_nor =  1000;

    private boolean haveAskedPerm;
    private boolean haveShownNoGps;
    private boolean locationRunning;
    private boolean statusRunning;
    private LocationManager locationManager;
    private MainActivity mainActivity;
    private TextView statusTextView;

    public InternalGps (MainActivity ma)
    {
        mainActivity = ma;
        locationManager = mainActivity.getSystemService (LocationManager.class);
        statusTextView = new TextView (mainActivity);
        statusTextView.setText ("off");
    }

    @SuppressLint("MissingPermission")
    @Override  // GpsReceiver
    public void enterAmbient ()
    {
        if (locationRunning) {
            locationManager.removeUpdates (this);
            locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, rate_amb, 0.0F, this);
        }
    }

    @Override  // GpsReceiver
    public View[] getParamViews ()
    {
        TextView tv = new TextView (mainActivity);
        tv.setText (statusTextView.getText ());
        statusTextView = tv;
        return new View[] { statusTextView };
    }

    @SuppressLint("MissingPermission")
    @Override  // GpsReceiver
    public void exitAmbient ()
    {
        if (locationRunning) {
            locationManager.removeUpdates (this);
            locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, rate_nor, 0.0F, this);
        }
    }

    @Override  // GpsReceiver
    public boolean startLocationSensor ()
    {
        boolean ok = startGpsGoing (new Runnable () {
            @Override
            public void run ()
                    throws IllegalArgumentException, NullPointerException, SecurityException
            {
                int rate = mainActivity.isAmbient () ? rate_amb : rate_nor;
                locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, rate, 0.0F, InternalGps.this);
            }
        });
        locationRunning = ok;
        return ok;
    }

    @Override  // GpsReceiver
    public boolean startStatusSensor ()
    {
        boolean ok = startGpsGoing (new Runnable () {
            @Override
            public void run ()
                    throws IllegalArgumentException, NullPointerException, SecurityException
            {
                locationManager.registerGnssStatusCallback (InternalGps.this);
            }
        });
        statusRunning = ok;
        return ok;
    }

    @Override  // GpsReceiver
    public boolean stopLocationSensor ()
    {
        if (locationManager == null) return false;
        boolean loc = locationRunning;
        locationRunning = false;
        locationManager.removeUpdates (this);
        return loc;
    }

    @Override  // GpsReceiver
    public boolean stopStatusSensor ()
    {
        if (locationManager == null) return false;
        boolean sts = statusRunning;
        statusRunning = false;
        locationManager.unregisterGnssStatusCallback (this);
        return sts;
    }

    @Override  // LocationListener
    public void onLocationChanged (Location location)
    {
        GpsLocation gpsloc = new GpsLocation ();
        gpsloc.altitude   = location.getAltitude ();
        gpsloc.lat        = location.getLatitude ();
        gpsloc.lon        = location.getLongitude ();
        gpsloc.speed      = location.getSpeed ();
        gpsloc.time       = location.getTime ();
        gpsloc.truecourse = location.getBearing ();
        mainActivity.gpsLocationReceived (gpsloc);
    }

    @Override  // LocationListener
    public void onStatusChanged (String provider, int status, Bundle extras)
    { }

    @Override  // LocationListener
    public void onProviderEnabled (String provider)
    { }

    @Override  // LocationListener
    public void onProviderDisabled (String provider)
    { }

    @Override  // GnssStatus.Callback
    public void onSatelliteStatusChanged (GnssStatus status)
    {
        int n = status.getSatelliteCount ();
        ArrayList<GpsStatus> statuses = new ArrayList<> (n);
        for (int i = 0; i < n; i ++) {
            GpsStatus gs = new GpsStatus ();
            gs.prn  = status.getSvid (i);
            gs.elev = status.getElevationDegrees (i);
            gs.azim = status.getAzimuthDegrees (i);
            gs.snr  = status.getCn0DbHz (i);
            gs.used = status.usedInFix (i);
            statuses.add (gs);
        }
        mainActivity.gpsStatusReceived (statuses);
    }

    // start GPS location or status updates
    // checks for no internal GPS receiver present
    // checks for no GPS permission granted, requesting if needed
    private boolean startGpsGoing (Runnable r)
    {
        try {
            r.run ();
            statusTextView.setText ("enabled");
            return true;
        } catch (IllegalArgumentException | NullPointerException iaenpe) {
            // java.lang.IllegalArgumentException: provider doesn't exist: gps
            Log.w (MainActivity.TAG, "exception enabling internal gps", iaenpe);
            locationManager = null;
            statusTextView.setText ("no internal GPS");
            if (! haveShownNoGps) {
                haveShownNoGps = true;
                mainActivity.showToast ("no internal GPS");
                mainActivity.showToast ("select alternative");
                mainActivity.showMainPage (mainActivity.gpsPageView);
            }
            return false;
        } catch (SecurityException se) {
            // java.lang.SecurityException: "gps" location provider requires ACCESS_FINE_LOCATION permission.
            Log.w (MainActivity.TAG, "exception enabling internal gps", se);
            if (! haveAskedPerm) {
                haveAskedPerm = true;
                statusTextView.setText ("permission denied");
                ActivityCompat.requestPermissions (mainActivity,
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                        MainActivity.RC_INTGPS);
            }
            return false;
        }
    }
}
