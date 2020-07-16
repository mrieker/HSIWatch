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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.GeomagneticField;

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

    public static Waypt find (SQLiteDatabase sqldb, String ident)
    {
        Waypt waypt = AptWaypt.find (sqldb, ident, true);
        if (waypt == null) waypt = FixWaypt.find (sqldb, ident);
        if (waypt == null) waypt = LocWaypt.find (sqldb, ident);
        if (waypt == null) waypt = NavWaypt.find (sqldb, ident);
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
        NavDialView ndv = mainActivity.navDialView;

        // set crosstrack mode to give crosstrack difference with great circle course
        mainActivity.setNavMode (NavDialView.Mode.GCT);

        // set obs to great circle initial heading
        double tc = Lib.LatLonTC (mainActivity.startlat, mainActivity.startlon, lat, lon);
        double mc = tc + curLoc.magvar;
        ndv.setObs (mc);
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
                double tc = Lib.LatLonTC (curLoc.latitude, curLoc.longitude, lat, lon);
                return tc + curLoc.magvar;
            }

            // VOR uses the radial that a VOR receiver would show being at curLoc
            case VOR: {
                return Lib.LatLonTC (lat, lon, curLoc.latitude, curLoc.longitude) +
                        getMagVar (curLoc.altitude) + 180.0;
            }
        }
    }

    // new GPS co-ordinate received, update navdial
    public void updateNav (MainActivity mainActivity)
    {
        GpsLocation curLoc = mainActivity.curLoc;
        NavDialView ndv = mainActivity.navDialView;

        switch (ndv.getMode ()) {
            case OFF: {
                break;
            }

            // for cross-track mode, adjust needle to show crosstrack distance in degrees
            case GCT: {
                double och = Lib.GCOnCourseHdg (mainActivity.startlat, mainActivity.startlon, lat, lon, curLoc.latitude, curLoc.longitude);
                double obs = och + curLoc.magvar;
                ndv.setObs (obs);
                double cth = Lib.LatLonTC (curLoc.latitude, curLoc.longitude, lat, lon);
                ndv.setDeflect (och - cth + 180.0);
                currentHeading (ndv, curLoc);
                break;
            }

            // for VOR mode, adjust needle to show what VOR radial we are on
            // for real VORs, it uses the radial that a VOR receiver would see
            // for other waypoints, it shows the direction to fly away from waypoint
            case VOR: {
                double radial = computeVorRadial (curLoc);
                ndv.setDeflect (ndv.getObs () - radial);
                currentHeading (ndv, curLoc);
                break;
            }

            // for ADF mode, point needle to direction to fly to head toward waypoint
            case ADF: {
                double tctowp = Lib.LatLonTC (curLoc.latitude, curLoc.longitude, lat, lon);
                double mctowp = tctowp + curLoc.magvar;
                ndv.setDeflect (mctowp - ndv.getObs ());
                currentHeading (ndv, curLoc);
                break;
            }

            // these modes only work for localizer waypoints...
            // for LOC, LOCBC, ILS, needle shows offset from localizer published true course
            case LOC: {
                ndv.setDeflect (computeLocDeflect (curLoc, 1));
                currentHeading (ndv, curLoc);
                break;
            }

            case LOCBC: {
                ndv.setDeflect (computeLocDeflect (curLoc, -1));
                currentHeading (ndv, curLoc);
                break;
            }

            case ILS: {
                ndv.setDeflect (computeLocDeflect (curLoc, 1));
                ndv.setSlope (computeGSDeflect (curLoc));
                currentHeading (ndv, curLoc);
                break;
            }
        }
    }

    // draw airplane depicting current heading
    // also twists the dial for HSI mode
    private void currentHeading (NavDialView ndv, GpsLocation curLoc)
    {
        boolean speedGood = curLoc.speed > MainActivity.gpsMinSpeedMPS;
        if (speedGood) {
            oldMagHeading = curLoc.truecourse + curLoc.magvar;
        }
        ndv.setHeading (oldMagHeading - ndv.getObs ());
        ndv.showAirplane (speedGood);
    }

    private double oldMagHeading;

    // nav dial in VOR mode: compute the radial from the waypoint we are on
    // - for VOR waypoints: use the VOR's built-in variation
    // - everything else uses modelled variation at waypoint site
    public double computeVorRadial (GpsLocation curLoc)
    {
        double radial = Lib.LatLonTC (lat, lon, curLoc.latitude, curLoc.longitude);
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

    /**************\
     *  Airports  *
    \**************/

    public static class AptWaypt extends Waypt {
        private final static String[] aptcols = new String[] {
                "apt_icaoid", "apt_lat", "apt_lon", "apt_name", "apt_desc1", "apt_elev" };

        public static Waypt find (SQLiteDatabase sqldb, String ident, boolean icao)
        {
            String where = icao ? "apt_icaoid=?" : "apt_faaid=?";
            try (Cursor cursor = sqldb.query ("airports", aptcols,
                    where, new String[] { ident }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    AptWaypt waypt = new AptWaypt ();
                    waypt.ident   = cursor.getString (0);
                    waypt.dme_lon = waypt.lat = cursor.getDouble (1);
                    waypt.dme_lon = waypt.lon = cursor.getDouble (2);
                    waypt.name    = cursor.getString (3) + "\n" + cursor.getString (4);
                    waypt.elev    = cursor.getDouble (5);
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

        public static Waypt find (SQLiteDatabase sqldb, String ident)
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
                "apt_elev", "loc_type", "loc_rwyid" };

        public static Waypt find (SQLiteDatabase sqldb, String ident)
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
                    waypt.gs_tilt = cursor.isNull (3) ? 0.0 : cursor.getDouble (3);
                    waypt.gs_lat  = cursor.getDouble (4);
                    waypt.gs_lon  = cursor.getDouble (5);
                    waypt.thdg    = cursor.getDouble (6);
                    waypt.elev    = cursor.isNull (7) ? cursor.getDouble (11) : cursor.getDouble (7);
                    waypt.name    = cursor.getString (12) + " RW " + cursor.getString (13) + '\n' + cursor.getString (8);
                    waypt.dme_lat = cursor.isNull  (9) ? 0.0 : cursor.getDouble  (9);
                    waypt.dme_lon = cursor.isNull (10) ? 0.0 : cursor.getDouble (10);
                    if ((waypt.dme_lat == 0.0) && (waypt.dme_lon == 0)) {
                        waypt.dme_lat = waypt.lat;
                        waypt.dme_lon = waypt.lon;
                    }
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

        // get mode associated with the waypoint type
        // should match what autoTune() does
        @Override  // Waypt
        public NavDialView.Mode getInitialMode ()
        {
            return Double.isNaN (gs_elev) ? NavDialView.Mode.LOC : NavDialView.Mode.ILS;
        }

        // for localizer, we always tune OBS to localizer heading
        // and we set to LOC or ILS mode
        @Override  // Waypt
        public void autoTune (MainActivity mainActivity)
        {
            NavDialView ndv = mainActivity.navDialView;

            mainActivity.setNavMode (Double.isNaN (gs_elev) ? NavDialView.Mode.LOC : NavDialView.Mode.ILS);
            ndv.setObs (thdg + getMagVar (Double.NaN));
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
            double tcfromloc = Lib.LatLonTC (lat, lon, curLoc.latitude, curLoc.longitude);
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
            double tctogsant = Lib.LatLonTC (curLoc.latitude, curLoc.longitude, gs_lat, gs_lon);
            double factor = Math.cos (Math.toRadians (tctogsant - thdg));
            double horizfromant_nm = Lib.LatLonDist (curLoc.latitude, curLoc.longitude, gs_lat, gs_lon) * factor;
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

        public static Waypt find (SQLiteDatabase sqldb, String ident)
        {
            try (Cursor cursor = sqldb.query ("navaids", navcols,
                    "nav_faaid=?", new String[] { ident }, null, null, null, null)) {
                if (cursor.moveToFirst ()) {
                    NavWaypt waypt = new NavWaypt ();
                    waypt.ident   = ident;
                    waypt.dme_lat = waypt.lat = cursor.getDouble (0);
                    waypt.dme_lon = waypt.lon = cursor.getDouble (1);
                    waypt.name    = cursor.getString (2);
                    waypt.magvar  = cursor.getInt (3);
                    waypt.type    = cursor.getString (4);
                    waypt.elev    = cursor.getDouble (5);
                    waypt.validModes = valgen;
                    return waypt;
                }
            }
            return null;
        }

        public String type;

        // get mode associated with the waypoint type
        // should match what autoTune() does
        @Override  // Waypt
        public NavDialView.Mode getInitialMode ()
        {
            return type.contains ("NDB") ? NavDialView.Mode.ADF : NavDialView.Mode.VOR;
        }

        @Override  // Waypt
        public void autoTune (MainActivity mainActivity)
        {
            GpsLocation curLoc = mainActivity.curLoc;
            NavDialView ndv = mainActivity.navDialView;

            if (type.contains ("NDB")) {
                mainActivity.setNavMode (NavDialView.Mode.ADF);
                ndv.setObs (curLoc.truecourse + curLoc.magvar);
            } else {
                double tc = Lib.LatLonTC (lat, lon, curLoc.latitude, curLoc.longitude);
                double mc = tc + magvar + 180.0;
                mainActivity.setNavMode (NavDialView.Mode.VOR);
                ndv.setObs (mc);
            }
        }
    }

    /*************\
     *  Runways  *
    \*************/

    public static class RwyWaypt extends LocWaypt {
        private final static String[] rwycols = new String[] {
                "apt_faaid", "apt_name", "rwy_tdze", "rwy_beglat", "rwy_beglon",
                "rwy_endlat", "rwy_endlon", "rwy_number", "apt_elev" };

        public static Waypt find (SQLiteDatabase sqldb, String ident)
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
            Waypt w = find (sqldb, ident.substring (0, 3), ident.substring (3));
            if (w != null) return w;
            if (i < 5) return null;
            return find (sqldb, ident.substring (0, 4), ident.substring (4));
        }

        private static Waypt find (SQLiteDatabase sqldb, String aptid, String rwyno)
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
                    // loc antenna 1000ft past far end numbers
                    waypt.lat      = Lib.LatHdgDist2Lat (endlat, rwytc, 1000.0 / Lib.FtPerNM);
                    waypt.lon      = Lib.LatLonHdgDist2Lon (endlat, endlon, rwytc, 1000.0 / Lib.FtPerNM);
                    // gs antenna 1000ft past near end numbers
                    waypt.gs_elev  = tdze;
                    waypt.gs_tilt  = 3.25;
                    waypt.gs_lat   = Lib.LatHdgDist2Lat (beglat, rwytc, 1000.0 / Lib.FtPerNM);
                    waypt.gs_lon   = Lib.LatLonHdgDist2Lon (beglat, endlon, rwytc, 1000.0 / Lib.FtPerNM);
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
