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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.GeomagneticField;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import static com.outerworldapps.hsiwatch.MainActivity.TAG;

/**
 * Waypoint database and nav dial updating.
 */
public abstract class Waypt {
    protected final static NavDialView.Mode[] valgen = {
                    NavDialView.Mode.OFF,
                    NavDialView.Mode.GCT,
                    NavDialView.Mode.VOR,
                    NavDialView.Mode.ADF };

    public double dme_lat;
    public double dme_lon;
    public double elev;     // feet (or NaN)
    public double lat;      // degrees
    public double lon;      // degrees
    public NavDialView.Mode[] validModes;
    public String ident;    // KBVY, ENE, BOSOX, I-SFM
    public String name;     // displayed in Toast

    protected double magvar;  // at waypoint (NaN until computed)

    public static Waypt find (Context ctx, SQLiteDatabase sqldb, String ident, LatLon refll)
    {
        Waypt waypt = UserWaypt.find (ctx, ident);
        if (waypt == null) waypt = AptWaypt.find (sqldb, ident, true);
        if (waypt == null) waypt = FixWaypt.find (sqldb, ident);
        if (waypt == null) waypt = LocWaypt.find (sqldb, ident);
        if (waypt == null) waypt = NavWaypt.find (sqldb, ident, refll);
        if (waypt == null) waypt = RwyWaypt.find (sqldb, ident);
        if (waypt == null) waypt = AptWaypt.find (sqldb, ident, false);
        return waypt;
    }

    // get mode associated with the waypoint type
    // should match what autoTune() does
    public NavDialView.Mode getInitialMode ()
    {
        return NavDialView.Mode.GCT;
    }

    // waypoint was just selected as a DirectTo - set navdial initial mode and OBS
    // by default, we treat waypoint as a GCT
    // but is overridden by LocWaypt,NavWaypt
    public void autoTune (MainActivity mainActivity)
    {
        GpsLocation curLoc = mainActivity.curLoc;

        // course line starts at current airplane location
        mainActivity.setStartLatLon (curLoc.lat, curLoc.lon);

        // set crosstrack mode to give crosstrack difference with great circle course
        mainActivity.setNavMode (NavDialView.Mode.GCT);

        // set obs to great circle initial heading
        double tc = Lib.LatLonTC (curLoc.lat, curLoc.lon, lat, lon);
        mainActivity.obsMagVar  = curLoc.magvar;
        mainActivity.obsSetting = tc + curLoc.magvar;
    }

    // get magnetic radial from curLoc to this waypoint
    // overidden by LocWaypt to handle LOC,LOCBC,ILS modes
    public double getMagRadTo (NavDialView.Mode mode, GpsLocation curLoc)
    {
        switch (mode) {
            default: return Double.NaN;

            // GCT and ADF use the great circle course from curLoc to the waypt
            case GCT:
            case ADF: {
                double tc = Lib.LatLonTC (curLoc.lat, curLoc.lon, lat, lon);
                return tc + curLoc.magvar;
            }

            // VOR uses the radial that a VOR receiver would show being at curLoc
            case VOR: {
                return Lib.LatLonTC (lat, lon, curLoc.lat, curLoc.lon) +
                        getMagVar (curLoc.altitude) + 180.0;
            }
        }
    }

    // new GPS co-ordinate received, update navdial needles
    public void updateNeedles (MainActivity mainActivity)
    {
        GpsLocation curLoc = mainActivity.curLoc;
        NavDialView ndv = mainActivity.navDialView;

        switch (mainActivity.navModeButton.getMode ()) {
            case OFF: {
                break;
            }

            // for cross-track mode, adjust needle to show crosstrack distance in degrees
            // also update obs for current on-course heading
            case GCT: {
                double och = Lib.GCOnCourseHdg (mainActivity.startlat, mainActivity.startlon, lat, lon, curLoc.lat, curLoc.lon);
                mainActivity.obsMagVar  = curLoc.magvar;
                mainActivity.obsSetting = och + curLoc.magvar;
                double cth = Lib.LatLonTC (curLoc.lat, curLoc.lon, lat, lon);
                ndv.setDeflect (och - cth + 180.0);
                break;
            }

            // for VOR mode, adjust needle to show what VOR radial we are on
            // for real VORs, it uses the radial that a VOR receiver would see
            // for other waypoints, it shows the direction to fly away from waypoint
            case VOR: {
                double radial = computeVorRadial (curLoc);
                ndv.setDeflect (mainActivity.obsSetting - radial);
                break;
            }

            // for ADF mode, point needle to direction to fly to head toward waypoint
            case ADF: {
                double tctowp = Lib.LatLonTC (curLoc.lat, curLoc.lon, lat, lon);
                double mctowp = tctowp + curLoc.magvar;
                ndv.setDeflect (mctowp - mainActivity.obsSetting);
                break;
            }

            // these modes only work for localizer waypoints...
            // for LOC, LOCBC, ILS, needle shows offset from localizer published true course
            case LOC: {
                ndv.setDeflect (computeLocDeflect (curLoc, 1));
                break;
            }

            case LOCBC: {
                ndv.setDeflect (computeLocDeflect (curLoc, -1));
                break;
            }

            case ILS: {
                ndv.setDeflect (computeLocDeflect (curLoc, 1));
                ndv.setSlope (computeGSDeflect (curLoc));
                break;
            }
        }
    }

    // nav dial in VOR mode: compute the radial from the waypoint we are on
    // - for VOR waypoints: use the VOR's built-in variation
    // - everything else uses modelled variation at waypoint site
    public double computeVorRadial (GpsLocation curLoc)
    {
        double radial = Lib.LatLonTC (lat, lon, curLoc.lat, curLoc.lon);
        return radial + getMagVar (curLoc.altitude);
    }

    // compute localizer needle deflection
    // - only valid for localizer waypoints
    public double computeLocDeflect (GpsLocation curLoc, int bc)
    {
        return Double.NaN;
    }
    public double computeGSDeflect (GpsLocation curLoc)
    {
        return Double.NaN;
    }

    // get magnetic variation at site of waypoint
    //  input:
    //   refaltm = in case waypoint doesn't have built-in elevation (such as fixes)
    public double getMagVar (double refaltm)
    {
        if (Double.isNaN (magvar)) {
            if (! Double.isNaN (elev)) refaltm = elev / Lib.FtPerM;
            GeomagneticField gmf = new GeomagneticField (
                    (float) lat, (float) lon, (float) refaltm,
                    System.currentTimeMillis ());
            magvar = - gmf.getDeclination ();
        }
        return magvar;
    }

    /********************\
     *  User Waypoints  *
    \********************/

    public static class UserWaypt extends Waypt {
        private static String csvpathname;
        private static TreeMap<String,UserWaypt> userwaypoints;

        // read user waypoints into TreeMap if not already
        public static TreeMap<String,UserWaypt> getUserWaypoints (Context ctx)
        {
            if (userwaypoints == null) {
                userwaypoints = new TreeMap<> ();
                csvpathname = ctx.getFilesDir () + "/hsiwatch_userwp.csv";
                if (new File (csvpathname).exists ()) {
                    try {
                        BufferedReader br = new BufferedReader (new FileReader (csvpathname));
                        for (String line; (line = br.readLine ()) != null; ) {
                            String[] parts = line.split (",");
                            UserWaypt waypt = new UserWaypt (parts[0], parts[1], parts[2]);
                            userwaypoints.put (waypt.ident, waypt);
                        }
                        br.close ();
                    } catch (IOException ioe) {
                        Log.e (TAG, "exception reading " + csvpathname, ioe);
                    }
                }
            }
            return userwaypoints;
        }

        // write user waypoint TreeMap to file
        public static boolean saveUserWaypoints (MainActivity mainActivity)
        {
            try {
                BufferedWriter bw = new BufferedWriter (new FileWriter (csvpathname + ".tmp"));
                for (UserWaypt uwp : userwaypoints.values ()) {
                    bw.write (uwp.ident + "," + uwp.latstr + "," + uwp.lonstr + "\n");
                }
                bw.close ();
                if (!new File (csvpathname + ".tmp").renameTo (new File (csvpathname))) {
                    throw new IOException ("rename failed");
                }
                return true;
            } catch (IOException ioe) {
                Log.e (TAG, "exception writing " + csvpathname, ioe);
                mainActivity.showToastLong ("error writing " + csvpathname + ": " + ioe.getMessage ());
                return false;
            }
        }

        // look for user waypoint in TreeMap (return null if not found)
        public static UserWaypt find (Context ctx, String ident)
        {
            TreeMap<String,UserWaypt> uwps = getUserWaypoints (ctx);
            return uwps.get (ident);
        }

        // parse a user waypoint lat/lon string
        //  NSEW deg min sec.ss
        //  NSEW deg min.mm
        //  NSEW deg.ddd
        // returns NaN on parse failure
        public static double parseLatLon (String str, char pos, char neg)
        {
            try {
                // strip out apostrophes, quotes and make upper case
                str = str.toUpperCase ().replace ('\'', ' ').replace ('"', ' ');

                // check for embedded N, S, E, W, decode and strip it out
                boolean negate = false;
                int i = str.indexOf (pos);
                if (i >= 0) {
                    str = str.substring (0, i) + str.substring (i + 1);
                } else if ((i = str.indexOf (neg)) >= 0) {
                    negate = true;
                    str = str.substring (0, i) + str.substring (i + 1);
                }

                // parse degrees up to blank or end of string
                str = str.trim ();
                int len = str.length ();
                int j = 0;
                while (j < len) {
                    char c = str.charAt (j);
                    if (c <= ' ') break;
                    j++;
                }
                double degs = Double.parseDouble (str.substring (0, j));
                if (negate) degs = -degs;

                // parse minutes up to next blank or end of string
                str = str.substring (j).trim ();
                len = str.length ();
                if (len > 0) {
                    j = 0;
                    do {
                        char c = str.charAt (j);
                        if (c <= ' ') break;
                        j++;
                    } while (j < len);
                    double mins = Double.parseDouble (str.substring (0, j));

                    // parse seconds up to end of string
                    str = str.substring (j).trim ();
                    len = str.length ();
                    if (len > 0) {
                        double secs = Double.parseDouble (str);
                        mins += secs / 60;
                    }

                    // smash it all into degrees and return
                    if (degs < 0) {
                        degs -= mins / 60;
                    } else {
                        degs += mins / 60;
                    }
                }
                return degs;
            } catch (Exception e) {
                return Double.NaN;
            }
        }

        public String latstr;
        public String lonstr;

        public UserWaypt (String ident, String latstr, String lonstr)
        {
            this.ident   = ident;
            this.latstr  = latstr;
            this.lonstr  = lonstr;
            this.dme_lat = this.lat = parseLatLon (latstr, 'N', 'S');
            this.dme_lon = this.lon = parseLatLon (lonstr, 'E', 'W');
            this.name    = "user waypoint" + "\n" + ident;
            this.elev    = Double.NaN;
            this.magvar  = Double.NaN;
            this.validModes = valgen;
        }
    }

    /**************\
     *  Airports  *
    \**************/

    public static class AptWaypt extends Waypt {
        private final static String[] aptcols = new String[] {
                "apt_icaoid", "apt_lat", "apt_lon", "apt_name", "apt_desc1", "apt_elev", "apt_faaid", "apt_desc2" };

        public String faaid;
        public String desc2;

        public static AptWaypt find (SQLiteDatabase sqldb, String ident, boolean icao)
        {
            String where = icao ? "apt_icaoid=?" : "apt_faaid=?";
            try (Cursor cursor = sqldb.query ("airports", aptcols,
                    where, new String[] { ident }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    AptWaypt waypt = new AptWaypt ();
                    waypt.ident   = cursor.getString (0);
                    waypt.dme_lat = waypt.lat = cursor.getDouble (1);
                    waypt.dme_lon = waypt.lon = cursor.getDouble (2);
                    waypt.name    = cursor.getString (3) + "\n" + cursor.getString (4);
                    waypt.elev    = cursor.getDouble (5);
                    waypt.faaid   = cursor.getString (6);
                    waypt.desc2   = cursor.getString (7);
                    waypt.magvar  = Double.NaN;
                    waypt.validModes = valgen;
                    return waypt;
                }
            }
            return null;
        }
    }

    /***********\
     *  Fixes  *
    \***********/

    public static class FixWaypt extends Waypt {
        private final static String[] fixcols = new String[] { "fix_lat", "fix_lon", "fix_desc" };

        public static FixWaypt find (SQLiteDatabase sqldb, String ident)
        {
            try (Cursor cursor = sqldb.query ("fixes", fixcols,
                    "fix_name=?", new String[] { ident }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    FixWaypt waypt = new FixWaypt ();
                    waypt.ident   = ident;
                    waypt.dme_lat = waypt.lat = cursor.getDouble (0);
                    waypt.dme_lon = waypt.lon = cursor.getDouble (1);
                    waypt.name    = cursor.getString (2);
                    waypt.elev    = Double.NaN;
                    waypt.magvar  = Double.NaN;
                    waypt.validModes = valgen;
                    return waypt;
                }
            }
            return null;
        }
    }

    /****************\
     *  Localizers  *
    \****************/

    public static class LocWaypt extends Waypt {
        private final static NavDialView.Mode[] valngs = {
                    NavDialView.Mode.OFF,
                    NavDialView.Mode.GCT,
                    NavDialView.Mode.VOR,
                    NavDialView.Mode.ADF,
                    NavDialView.Mode.LOC,
                    NavDialView.Mode.LOCBC };

        protected final static NavDialView.Mode[] valwgs = {
                    NavDialView.Mode.OFF,
                    NavDialView.Mode.GCT,
                    NavDialView.Mode.VOR,
                    NavDialView.Mode.ADF,
                    NavDialView.Mode.LOC,
                    NavDialView.Mode.LOCBC,
                    NavDialView.Mode.ILS };

        private final static String[] loccols = new String[] {
                "loc_lat", "loc_lon", "gs_elev", "gs_tilt", "gs_lat", "gs_lon",
                "loc_thdg", "loc_elev", "apt_name", "dme_lat", "dme_lon",
                "apt_elev", "loc_type", "loc_rwyid", "apt_icaoid" };

        public static LocWaypt find (SQLiteDatabase sqldb, String ident)
        {
            if (! ident.startsWith ("I")) return null;
            if (! ident.startsWith ("I-")) ident = "I-" + ident.substring (1);
            try (Cursor cursor = sqldb.query ("localizers,airports", loccols,
                    "loc_faaid=? AND apt_faaid=loc_aptfid",
                    new String[] { ident }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    LocWaypt waypt = new LocWaypt ();
                    waypt.ident   = ident;
                    waypt.lat     = cursor.getDouble (0);
                    waypt.lon     = cursor.getDouble (1);
                    waypt.gs_elev = cursor.isNull (2) ? Double.NaN : cursor.getDouble (2);
                    waypt.gs_tilt = cursor.isNull (3) ? Double.NaN : cursor.getDouble (3);
                    waypt.gs_lat  = cursor.getDouble (4);
                    waypt.gs_lon  = cursor.getDouble (5);
                    waypt.thdg    = cursor.getDouble (6);
                    waypt.elev    = cursor.isNull (7) ? cursor.getDouble (11) : cursor.getDouble (7);
                    waypt.name    = cursor.getString (12) + " RW " + cursor.getString (13) + '\n' + cursor.getString (8);
                    waypt.apticao = cursor.getString (14);
                    waypt.dme_lat = cursor.isNull  (9) ? waypt.lat : cursor.getDouble  (9);
                    waypt.dme_lon = cursor.isNull (10) ? waypt.lon : cursor.getDouble (10);
                    waypt.magvar  = Double.NaN;
                    waypt.validModes = Double.isNaN (waypt.gs_elev) ? valngs : valwgs;
                    return waypt;
                }
            }
            return null;
        }

        public double gs_elev;
        public double gs_tilt;
        public double gs_lat;
        public double gs_lon;
        public double thdg;
        public String apticao;

        // get mode associated with the waypoint type
        // should match what autoTune() does
        @Override  // Waypt
        public NavDialView.Mode getInitialMode ()
        {
            return Double.isNaN (gs_elev) ? NavDialView.Mode.LOC : NavDialView.Mode.ILS;
        }

        // for localizer, we always tune OBS to localizer heading
        // and we set to LOC or ILS mode
        // and start of course is on the extended runway centerline
        @Override  // Waypt
        public void autoTune (MainActivity mainActivity)
        {
            GpsLocation curLoc = mainActivity.curLoc;

            double distnm = Lib.LatLonDist (lat, lon, curLoc.lat, curLoc.lon);
            mainActivity.setStartLatLon (
                    Lib.LatHdgDist2Lat (lat, thdg + 180.0, distnm),
                    Lib.LatLonHdgDist2Lon (lat, lon, thdg + 180.0, distnm)
            );

            mainActivity.setNavMode (Double.isNaN (gs_elev) ? NavDialView.Mode.LOC : NavDialView.Mode.ILS);

            mainActivity.obsMagVar  = getMagVar (Double.NaN);
            mainActivity.obsSetting = thdg + mainActivity.obsMagVar;
        }

        // get magnetic radial from curLoc to this waypoint
        @Override  // Waypt
        public double getMagRadTo (NavDialView.Mode mode, GpsLocation curLoc)
        {
            switch (mode) {
                default: return super.getMagRadTo (mode, curLoc);
                case LOC:
                case ILS: {
                    return thdg + getMagVar (Double.NaN);
                }
                case LOCBC: {
                    return thdg + getMagVar (Double.NaN) + 180.0;
                }
            }
        }

        // compute localizer needle deflection
        //  input:
        //   curLoc = current aircraft location
        //   bc = 1 : forward localizer
        //       -1 : back-course localizer
        @Override  // Waypt
        public double computeLocDeflect (GpsLocation curLoc, int bc)
        {
            double tcfromloc = Lib.LatLonTC (lat, lon, curLoc.lat, curLoc.lon);
            double diff = (tcfromloc + 180.0 - thdg) * bc;
            while (diff < -180.0) diff += 360.0;
            while (diff >= 180.0) diff -= 360.0;
            return diff;
        }

        // compute glideslope needle deflection
        //  input:
        //   curLoc = current aircraft location
        @Override  // Waypt
        public double computeGSDeflect (GpsLocation curLoc)
        {
            double tctogsant = Lib.LatLonTC (curLoc.lat, curLoc.lon, gs_lat, gs_lon);
            double factor = Math.cos (Math.toRadians (tctogsant - thdg));
            double horizfromant_nm = Lib.LatLonDist (curLoc.lat, curLoc.lon, gs_lat, gs_lon) * factor;
            double aboveantenna_ft = curLoc.altitude * Lib.FtPerM - gs_elev;
            double degaboveantenna = Math.toDegrees (Math.atan2 (aboveantenna_ft, horizfromant_nm * Lib.FtPerNM));
            return gs_tilt - degaboveantenna;
        }
    }

    /************************\
     *  Navaids (VOR, NDB)  *
    \************************/

    public static class NavWaypt extends Waypt {
        private final static String[] navcols = new String[] { "nav_lat", "nav_lon", "nav_name",
                "nav_magvar", "nav_type", "nav_elev" };

        public static NavWaypt find (SQLiteDatabase sqldb, String ident, LatLon refll)
        {
            try (Cursor cursor = sqldb.query ("navaids", navcols,
                    "nav_faaid=?", new String[] { ident }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    double bestnm = 999999;
                    NavWaypt bestwp = new NavWaypt ();
                    do {
                        double lat = cursor.getDouble (0);
                        double lon = cursor.getDouble (1);
                        double distnm = Lib.LatLonDist (lat, lon, refll.lat, refll.lon);
                        if (bestnm > distnm) {
                            bestnm = distnm;
                            bestwp.ident   = ident;
                            bestwp.dme_lat = bestwp.lat = lat;
                            bestwp.dme_lon = bestwp.lon = lon;
                            bestwp.name    = cursor.getString (4) + " " + cursor.getString (2);
                            bestwp.magvar  = cursor.getInt (3);
                            bestwp.isndb   = cursor.getString (4).contains ("NDB");
                            bestwp.elev    = cursor.getDouble (5);
                            bestwp.validModes = valgen;
                        }
                    } while (cursor.moveToNext ());
                    return bestwp;
                }
            }
            return null;
        }

        private boolean isndb;

        // get mode associated with the waypoint type
        // should match what autoTune() does
        @Override  // Waypt
        public NavDialView.Mode getInitialMode ()
        {
            return isndb ? NavDialView.Mode.ADF : NavDialView.Mode.VOR;
        }

        @Override  // Waypt
        public void autoTune (MainActivity mainActivity)
        {
            GpsLocation curLoc = mainActivity.curLoc;
            mainActivity.setStartLatLon (curLoc.lat, curLoc.lon);
            if (isndb) {
                mainActivity.setNavMode (NavDialView.Mode.ADF);
                mainActivity.obsMagVar  = curLoc.magvar;
                mainActivity.obsSetting = curLoc.truecourse + curLoc.magvar;
            } else {
                double tc = Lib.LatLonTC (lat, lon, curLoc.lat, curLoc.lon);
                double mc = tc + magvar + 180.0;
                mainActivity.setNavMode (NavDialView.Mode.VOR);
                mainActivity.obsMagVar  = magvar;
                mainActivity.obsSetting = mc;
            }
        }
    }

    /*************\
     *  Runways  *
    \*************/

    public static class RwyWaypt extends LocWaypt {
        private final static String[] rwycols = new String[] {
                "apt_faaid", "apt_name", "rwy_tdze", "rwy_beglat", "rwy_beglon",
                "rwy_endlat", "rwy_endlon", "rwy_number", "apt_elev", "apt_icaoid" };

        public static RwyWaypt find (SQLiteDatabase sqldb, String ident)
        {
            // accept <aptid>.<rwyno>
            int i = ident.indexOf ('.');
            if (i >= 0) {
                String aptid = ident.substring (0, i);
                String rwyno = ident.substring (++i);
                return find (sqldb, aptid, rwyno);
            }

            // also try without the . (assumes <aptid> is 3 or 4 chars)
            i = ident.length ();
            if (i < 4) return null;
            RwyWaypt w = find (sqldb, ident.substring (0, 3), ident.substring (3));
            if (w != null) return w;
            if (i < 5) return null;
            return find (sqldb, ident.substring (0, 4), ident.substring (4));
        }

        private static RwyWaypt find (SQLiteDatabase sqldb, String aptid, String rwyno)
        {
            try (Cursor cursor = sqldb.query ("runways,airports", rwycols,
                    "(apt_icaoid=? OR apt_faaid=?) AND rwy_faaid=apt_faaid AND (rwy_number=? OR rwy_number=?)",
                    new String[] { aptid, aptid, rwyno, '0' + rwyno }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    double tdze    = cursor.isNull (2) ? cursor.getDouble (8) : cursor.getDouble (2);
                    double beglat  = cursor.getDouble (3);
                    double beglon  = cursor.getDouble (4);
                    double endlat  = cursor.getDouble (5);
                    double endlon  = cursor.getDouble (6);
                    rwyno          = cursor.getString (7);
                    double rwytc   = Lib.LatLonTC (beglat, beglon, endlat, endlon);
                    RwyWaypt waypt = new RwyWaypt ();
                    // use faaid for airport cuz it's shorter than icaoid
                    waypt.ident    = cursor.getString (0) + "." + rwyno;
                    waypt.apticao  = cursor.getString (9);
                    // loc antenna 1000ft past far end numbers
                    waypt.lat      = Lib.LatHdgDist2Lat (endlat, rwytc, 1000.0 / Lib.FtPerNM);
                    waypt.lon      = Lib.LatLonHdgDist2Lon (endlat, endlon, rwytc, 1000.0 / Lib.FtPerNM);
                    // gs antenna 1000ft past near end numbers
                    waypt.gs_elev  = tdze;
                    waypt.gs_tilt  = 3.25;
                    waypt.gs_lat   = Lib.LatHdgDist2Lat (beglat, rwytc, 1000.0 / Lib.FtPerNM);
                    waypt.gs_lon   = Lib.LatLonHdgDist2Lon (beglat, beglon, rwytc, 1000.0 / Lib.FtPerNM);
                    waypt.thdg     = rwytc;
                    waypt.elev     = tdze;
                    waypt.name     = cursor.getString (1) + "\nRunway " + rwyno;
                    // dme antenna right at near end numbers
                    waypt.dme_lat  = beglat;
                    waypt.dme_lon  = beglon;
                    waypt.magvar   = Double.NaN;
                    waypt.validModes = valwgs;
                    return waypt;
                }
            }
            return null;
        }
    }
}
