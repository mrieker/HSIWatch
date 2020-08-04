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

import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Decode NMEA messages and update displays.
 */
public class DecodeNMEA {
    private boolean dequeueRunning;
    private boolean gotgga;
    private boolean gotrmc;
    public  boolean locationEnabled;
    public  boolean statusEnabled;
    private GpsLocation gpsloc;
    private int nusedprns;
    private int[] usedprns;
    private final LinkedList<String> nmeaMessages;
    private MainActivity mainActivity;
    private String[][] gsvs;

    public DecodeNMEA (MainActivity ma)
    {
        mainActivity = ma;
        usedprns = new int[12];
        nmeaMessages = new LinkedList<> ();
    }

    /**
     * Decode incoming NMEA message string.
     * Can have more than one message.
     * Can be called by any thread.
     */
    public void gotLine (String line)
            throws Exception
    {
        int i = line.indexOf ('$');
        if (i >= 0) {
            int len = line.length ();
            int xor = 0;
            int j = -1;
            for (; i < len; i ++) {
                char b = line.charAt (i);
                switch (b) {
                    case '$': {
                        j = i;
                        xor = 0;
                        break;
                    }
                    case '*': {
                        if (j >= 0) {
                            int ck = Integer.parseInt (line.substring (i + 1, i + 3), 16);
                            if (ck != xor) throw new Exception ("bad NMEA checksum");
                            String nmea = line.substring (j, i);
                            queueIncomingNMEA (nmea);
                            j = -1;
                        }
                        break;
                    }
                    default: {
                        xor ^= b & 0xFF;
                        break;
                    }
                }
            }
        }
    }

    // queue message to UI thread so processIncomingNMEA() can process it
    private void queueIncomingNMEA (String nmea)
    {
        synchronized (nmeaMessages) {
            nmeaMessages.add (nmea);
            if (! dequeueRunning) {
                dequeueRunning = true;
                mainActivity.runOnUiThread (new Runnable () {
                    @Override
                    public void run ()
                    {
                        while (true) {
                            String nmea;
                            synchronized (nmeaMessages) {
                                nmea = nmeaMessages.poll ();
                                if (nmea == null) {
                                    dequeueRunning = false;
                                    break;
                                }
                            }
                            processIncomingNMEA (nmea);
                        }
                    }
                });
            }
        }
    }

    // runs on UI thread to process incoming NMEA message
    // calls the mainActivity.gotGps{Location.Status}() methods
    private void processIncomingNMEA (String nmea)
    {
        try {
            String[] parts = nmea.split (",");
            switch (parts[0].substring (3)) {

                // location messages
                case "GGA": {
                    if (locationEnabled) {
                        if (gotgga) gotLocation ();
                        decodeNMEAhhmmss (parts[1]);
                        gpsloc.lat = decodeNMEALatLon (parts[2], parts[3], 'N', 'S');
                        gpsloc.lon = decodeNMEALatLon (parts[4], parts[5], 'E', 'W');
                        gpsloc.altitude = Double.parseDouble (parts[9]);
                        if (! parts[10].equals ("M")) throw new Exception ("altitude not in metres");
                        gotgga = true;
                        if (gotrmc) gotLocation ();
                    }
                    break;
                }
                case "RMC": {
                    if (locationEnabled) {
                        if (gotrmc) gotLocation ();
                        decodeNMEAhhmmss (parts[1]);
                        gpsloc.lat = decodeNMEALatLon (parts[3], parts[4], 'N', 'S');
                        gpsloc.lon = decodeNMEALatLon (parts[5], parts[6], 'E', 'W');
                        gpsloc.speed = Double.parseDouble (parts[7]) / Lib.KtPerMPS;
                        gpsloc.truecourse = Double.parseDouble (parts[8]);
                        decodeNMEAddmmyy (parts[9]);
                        gotrmc = true;
                        if (gotgga) gotLocation ();
                    }
                    break;
                }

                // status messages
                case "GSA": {
                    if (statusEnabled) {
                        nusedprns = 0;
                        for (int i = 0; (i < usedprns.length) && (i + 6 < parts.length); ) {
                            usedprns[i] = Integer.parseInt (parts[i + 3]);
                            nusedprns = ++i;
                        }
                        gotStatus ();
                    }
                    break;
                }

                case "GSV": {
                    if (statusEnabled) {
                        int totsen = Integer.parseInt (parts[1]);
                        int thissn = Integer.parseInt (parts[2]);
                        if (thissn == 1) gsvs = new String[totsen][];
                        gsvs[thissn - 1] = parts;
                        if (thissn == totsen) gotStatus ();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Log.w (MainActivity.TAG, "error processing NMEA " + nmea, e);
        }
    }

    // hhmmss.sss
    private void decodeNMEAhhmmss (String hhmmss)
    {
        int hhmmsssss = (int) Math.round (Double.parseDouble (hhmmss) * 1000.0);
        int sssss = hhmmsssss % 100000;
        int mm = hhmmsssss / 100000 % 100;
        int hh = hhmmsssss / 10000000 % 100;
        int msec = hh * 3600000 + mm * 60000 + sssss;
        if ((gpsloc == null) || (gpsloc.time % 86400000 != msec)) {
            if (gpsloc != null) gotLocation ();
            gpsloc = new GpsLocation ();
            long now = System.currentTimeMillis ();
            long nowday = now / 86400000;
            if (msec > 64800000 && now % 86400000 < 21600000) nowday --;
            gpsloc.time = nowday * 86400000 + msec;
        }
    }

    private void decodeNMEAddmmyy (String ddmmyy)
    {
        int dd = Integer.parseInt (ddmmyy.substring (0, 2));
        int mm = Integer.parseInt (ddmmyy.substring (2, 4));
        int yy = Integer.parseInt (ddmmyy.substring (4, 6));
        //noinspection deprecation
        gpsloc.time = Date.UTC (yy + 100, mm - 1, dd, 0, 0, 0) + gpsloc.time % 86400000;
    }

    // ddmm.mmm,D
    private static double decodeNMEALatLon (String ddmm, String dir, char pos, char neg)
    {
        int ddmmmmm = (int) Math.round (Double.parseDouble (ddmm) * 1000.0);
        int mmmmm = ddmmmmm % 100000;
        int dd = ddmmmmm / 100000 % 100;
        double ll = dd + mmmmm / 60000.0;
        if (dir.charAt (0) == neg) ll = - ll;
        else if (dir.charAt (0) != pos) throw new NumberFormatException ("bad lat/lon direction " + dir);
        return ll;
    }

    // got both a GGA and an RMC message with same timestamp, so the location message is complete
    // also called if get two GGAs or two RMCs in a row
    private void gotLocation ()
    {
        mainActivity.gpsLocationReceived (gpsloc);
        gotgga = false;
        gotrmc = false;
        gpsloc = null;
    }

    // got either a GSA or GSV message, update status display
    private void gotStatus ()
    {
        HashMap<Integer,GpsStatus> statuses = new HashMap<> ();

        // GSV gives list of all satellites and their positions
        for (String[] parts : gsvs) {
            for (int i = 4; i + 4 <= parts.length;) {
                GpsStatus status = new GpsStatus ();
                status.prn  = Integer.parseInt (parts[i++]);
                status.elev = Double.parseDouble (parts[i++]);
                status.azim = Double.parseDouble (parts[i++]);
                status.snr  = Double.parseDouble (parts[i++]);
                statuses.put (status.prn, status);
            }
        }

        // GSA says which ones are being used to compute fix
        for (int i = 0; i < nusedprns; i ++) {
            GpsStatus status = statuses.get (usedprns[i]);
            if (status != null) status.used = true;
        }

        // update display
        mainActivity.gpsStatusReceived (statuses.values ());
    }
}
