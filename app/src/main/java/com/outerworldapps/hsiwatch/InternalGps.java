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
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * Use internal GPS receiver to determine location.
 */
public class InternalGps implements GpsReceiver, LocationListener {
    private final static int rate_amb = 20000;
    private final static int rate_nor =  1000;

    private boolean running;
    private GpsStatusView statusListener;
    private LocationManager locationManager;
    private MainActivity mainActivity;

    public InternalGps (MainActivity ma)
    {
        mainActivity = ma;
        locationManager = mainActivity.getSystemService (LocationManager.class);
        if (locationManager == null) {
            mainActivity.showToastLong ("no location manager");
        }
    }

    @SuppressLint("MissingPermission")
    @Override  // GpsReceiver
    public void enterAmbient ()
    {
        if (running) {
            locationManager.removeUpdates (this);
            locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, rate_amb, 0.0F, this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override  // GpsReceiver
    public void exitAmbient ()
    {
        if (running) {
            locationManager.removeUpdates (this);
            locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, rate_nor, 0.0F, this);
        }
    }

    @Override  // GpsReceiver
    public boolean startSensor ()
    {
        if (ActivityCompat.checkSelfPermission (mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions (mainActivity,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    MainActivity.RC_INTGPS);
            return false;
        }

        int rate = mainActivity.isAmbient () ? rate_amb : rate_nor;
        locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, rate, 0.0F, this);
        if (statusListener != null) {
            Log.d (MainActivity.TAG, "InternalGps.startSensor*: turning on status listener");
            locationManager.registerGnssStatusCallback (gnssStatusCallback);
        }
        running = true;
        return true;
    }

    @Override  // GpsReceiver
    public void stopSensor ()
    {
        Log.d (MainActivity.TAG, "InternalGps.stopSensor*: turning off status listener");
        running = false;
        locationManager.removeUpdates (this);
        locationManager.unregisterGnssStatusCallback (gnssStatusCallback);
        if (statusListener != null) {
            statusListener.onStatusReceived (null);
        }
    }

    @SuppressLint("MissingPermission")
    public void setStatusListener (GpsStatusView gsl)
    {
        statusListener = gsl;
        if (gsl == null) {
            Log.d (MainActivity.TAG, "InternalGps.setStatusListener*: turning off status listener");
            locationManager.unregisterGnssStatusCallback (gnssStatusCallback);
        } else if (running) {
            Log.d (MainActivity.TAG, "InternalGps.setStatusListener*: turning on status listener");
            locationManager.registerGnssStatusCallback (gnssStatusCallback);
        }
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

    private final GnssStatus.Callback gnssStatusCallback = new GnssStatus.Callback () {
        @Override
        public void onSatelliteStatusChanged (GnssStatus status)
        {
            statusListener.onStatusReceived (status);
        }
    };
}
