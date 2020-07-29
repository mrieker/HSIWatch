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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Moving Map
 */
public class MapDialView extends OBSDialView {

    private final static int MAXNEARAPTS = 20;
    private final static int MAXWAYPTS = 20;
    private final static int RUNWAYFT = 1000;

    private final static int defradidx = 3;
    private final static int[] radiinm = new int[] { 6, 10, 16, 20, 24, 30, 40, 50, 70, 100, 120, 150, 200 };

    private final static MapWpt[] nullMapWptArray = new MapWpt[0];

    private static class MapWpt implements Comparable<MapWpt> {
        public double lat;
        public double lon;
        public double dist;
        public double mhdg;
        public String id;
        public String name; // airport name string (or null)
        public int idh;     // id string height pixels
        public int idw;     // id string width pixels
        public int xpx;     // last drawn x pixel
        public int ypx;     // last drawn y pixel
        public int rft;     // runway length feet
        public boolean nav;

        // string to put on nearest airport radio button
        public String rbString ()
        {
            StringBuilder sb = new StringBuilder ();
            sb.append (id);
            sb.append (' ');
            int hdg = ((int) Math.round (mhdg) + 359) % 360 + 1;
            sb.append (hdg / 100);
            sb.append (hdg / 10 % 10);
            sb.append (hdg % 10);
            sb.append ("\u00B0 ");
            sb.append (Math.round (dist));
            sb.append (" nm");
            if (name != null) {
                sb.append (": ");
                sb.append (name);
            }
            return sb.toString ();
        }

        // sort by descending runway length
        public int compareTo (MapWpt o)
        {
            int cmp = o.rft - rft;
            if (cmp == 0) cmp = id.compareTo (o.id);
            return cmp;
        }
    }

    private boolean ambient;
    private float xpix, ypix;
    private double trueuprad;
    private double wayptEastLon;
    private double wayptNorthLat;
    private double wayptSouthLat;
    private double wayptWestLon;
    private int radiusIndex;
    private int radiusNM;
    private MainActivity mainActivity;
    private MapWpt[] drawnwpts;
    private MapWpt[] waypoints;
    private Paint coursePaint;
    private Paint dirArrowPaint;
    private Paint outerRingPaint;
    private Paint rangeRingPaint;
    private Paint wayptPaint;
    private RadioGroup nearRadioGroup;
    public  RwyDiagView rwyDiagView;
    private UpdateThread updateThread;
    private View nearPageView;

    public MapDialView (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        constructor ();
    }

    public MapDialView (Context ctx)
    {
        super (ctx);
        constructor ();
    }

    private void constructor ()
    {
        Context ctx = getContext ();
        if (ctx instanceof MainActivity) {
            mainActivity = (MainActivity) ctx;
        }

        goDownString  = "menu";
        goLeftString  = "back";
        goRightString = "rwys";
        goUpString    = "near";

        drawnwpts = new MapWpt[MAXWAYPTS];
        drawnwpts[0] = new MapWpt ();

        waypoints = nullMapWptArray;

        coursePaint = new Paint ();
        coursePaint.setStyle (Paint.Style.FILL_AND_STROKE);
        coursePaint.setTextSize (120);

        dirArrowPaint = new Paint ();
        dirArrowPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        outerRingPaint = new Paint ();
        outerRingPaint.setColor (Color.GRAY);
        outerRingPaint.setStrokeWidth (50);
        outerRingPaint.setStyle (Paint.Style.STROKE);

        rangeRingPaint = new Paint ();
        rangeRingPaint.setStrokeWidth (10);
        rangeRingPaint.setStyle (Paint.Style.STROKE);
        rangeRingPaint.setTextAlign (Paint.Align.CENTER);
        rangeRingPaint.setTextSize (100);

        wayptPaint = new Paint ();
        wayptPaint.setColor (Color.GREEN);
        wayptPaint.setStrokeWidth (10);
        wayptPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        wayptPaint.setTextSize (110);

        radiusIndex = defradidx;
        radiusNM = radiinm[radiusIndex];

        setAmbient ();
    }

    // go DOWN: open menu
    @Override
    public View getDownView ()
    {
        return mainActivity.menuMainPage.getView ();
    }

    // go LEFT: go back (to nav dial)
    @Override
    public View getLeftView ()
    {
        return null;
    }

    @Override
    public View getRightView ()
    {
        if (rwyDiagView == null) rwyDiagView = new RwyDiagView (mainActivity);
        return rwyDiagView;
    }

    // go UP: open nearby airport selection page
    @SuppressLint("InflateParams")
    @Override
    public View getUpView ()
    {
        // inflate layout if not already
        if (nearPageView == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            nearPageView = layoutInflater.inflate (R.layout.near_page, null);
            Button nearBackButton = nearPageView.findViewById (R.id.nearBackButton);
            nearBackButton.setOnClickListener (mainActivity.backButtonListener);
            nearRadioGroup = nearPageView.findViewById (R.id.nearGroup);
        }

        // set up what to do when one of the buttons is clicked
        View.OnClickListener radioButtonListener = new OnClickListener () {
            @Override
            public void onClick (View v)
            {
                // doesn't seem to automatically uncheck the old one
                // so go through them all and set
                for (int i = nearRadioGroup.getChildCount (); -- i >= 0;) {
                    View rb = nearRadioGroup.getChildAt (i);
                    if (rb instanceof RadioButton) {
                        ((RadioButton) rb).setChecked (rb == v);
                    }
                }

                // select the given waypoint as current
                MapWpt mapwpt = (MapWpt) v.getTag ();
                Waypt waypt = Waypt.find (mainActivity.downloadThread.getSqlDB (), mapwpt.id);

                // fill in long name (eg, BEVERLY RGNL) into radio button
                if ((waypt != null) && (mapwpt.name == null)) {
                    mapwpt.name = waypt.name;
                    ((RadioButton) v).setText (mapwpt.rbString ());
                }

                // set it up for navigation
                mainActivity.setNavWaypt (waypt);
            }
        };

        nearRadioGroup.removeAllViews ();
        if (waypoints != null) {

            // get list of nearby airports sorted by distance
            double curlat = mainActivity.curLoc.latitude;
            double curlon = mainActivity.curLoc.longitude;
            MapWpt[] bydist = new MapWpt[waypoints.length];
            int j = 0;
            for (int i = 0; i < bydist.length; i ++) {
                MapWpt mapwpt = waypoints[i];
                if (mapwpt.nav) continue;
                mapwpt.dist = Lib.LatLonDist (curlat, curlon, mapwpt.lat, mapwpt.lon);
                bydist[j++] = mapwpt;
            }
            Arrays.sort (bydist, 0, j, new Comparator<MapWpt> () {
                @Override
                public int compare (MapWpt o1, MapWpt o2)
                {
                    if (o1.dist < o2.dist) return -1;
                    if (o1.dist > o2.dist) return 1;
                    return o1.id.compareTo (o2.id);
                }
            });

            // create radio buttons for nearby airports, closest at top
            // mark current destination as checked if in list
            double magvar = mainActivity.curLoc.magvar;
            if (j > MAXNEARAPTS) j = MAXNEARAPTS;
            for (int i = 0; i < j; i ++) {
                MapWpt mapwpt = bydist[i];
                mapwpt.mhdg = Lib.LatLonTC (curlat, curlon, mapwpt.lat, mapwpt.lon) + magvar;
                RadioButton rb = new RadioButton (mainActivity);
                rb.setChecked ((mainActivity.navWaypt != null) && mapwpt.id.equals (mainActivity.navWaypt.ident));
                rb.setOnClickListener (radioButtonListener);
                rb.setTag (mapwpt);
                rb.setText (mapwpt.rbString ());
                nearRadioGroup.addView (rb);
            }
        }

        // display back button and radio buttons
        return nearPageView;
    }

    // zoom-{in,out} was clicked, change radius
    public void incRadius (int inc)
    {
        inc += radiusIndex;
        if (inc < 0) inc = 0;
        if (inc >= radiinm.length) inc = radiinm.length - 1;
        radiusIndex = inc;
        radiusNM = radiinm[inc];
        invalidate ();
    }

    /**
     * Use grayscale for ambient mode.
     */
    public void setAmbient ()
    {
        ambient = (mainActivity != null) && mainActivity.ambient;
        boolean redRing = (mainActivity != null) && mainActivity.redRingOn;
        if (ambient) {
            coursePaint.setColor (Color.WHITE);
            dirArrowPaint.setColor (Color.GRAY);
            outerRingPaint.setColor (redRing ? Color.LTGRAY : Color.GRAY);
            rangeRingPaint.setColor (Color.GRAY);
        } else {
            coursePaint.setColor (Color.MAGENTA);
            dirArrowPaint.setColor (Color.GREEN);
            outerRingPaint.setColor (redRing ? Color.RED : Color.GRAY);
            rangeRingPaint.setColor (Color.YELLOW);
        }
        if (rwyDiagView != null) rwyDiagView.setAmbient ();
        invalidate ();
    }

    // make sure we have the waypoints within the screen area
    // start reading from database if not
    private void updateWaypoints ()
    {
        double lat = mainActivity.curLoc.latitude;
        double lon = mainActivity.curLoc.longitude;
        double radiusLat = radiusNM / Lib.NMPerDeg;
        double radiusLon = radiusLat * Math.cos (Math.toRadians (lat));
        double northLat  = lat + radiusLat;
        double southLat  = lat - radiusLat;
        double eastLon   = Lib.NormalLon (lon + radiusLon);
        double westLon   = Lib.NormalLon (lon - radiusLon);
        if ((northLat > wayptNorthLat) || (southLat < wayptSouthLat) ||
                (eastLon > wayptEastLon) || (westLon < wayptWestLon)) {
            if ((mainActivity != null) && (updateThread == null)) {
                SQLiteDatabase sqldb = mainActivity.downloadThread.getSqlDB ();
                if (sqldb != null) {
                    updateThread = new UpdateThread ();
                    updateThread.lat = lat;
                    updateThread.lon = lon;
                    updateThread.radiusLat = radiusLat;
                    updateThread.radiusLon = radiusLon;
                    updateThread.sqldb = sqldb;
                    updateThread.start ();
                }
            }
        }
    }

    /**
     * Run a thread to read waypoints from database then update screen.
     */
    private class UpdateThread extends Thread {
        public double lat, lon;
        public double radiusLat, radiusLon;
        public SQLiteDatabase sqldb;

        @Override
        public void run ()
        {
            // read waypoints within double radius so we don't need to read again for a while
            radiusLat *= 2;
            radiusLon *= 2;
            final double northLat = lat + radiusLat;
            final double southLat = lat - radiusLat;
            final double eastLon  = lon + radiusLon;
            final double westLon  = lon - radiusLon;
            HashMap<String,MapWpt> byident = new HashMap<> ();

            // read navaids with type VOR% (VOR, VOR/DME, VORTAC)
            String where = "nav_lat>" + southLat + " AND nav_lat<" + northLat;
            if (eastLon > westLon) {
                where += " AND nav_lon>" + westLon + " AND nav_lon<" + eastLon;
            } else {
                where += " AND (nav_lon>" + westLon + " OR nav_lon<" + eastLon + ")";
            }
            try (Cursor cursor = sqldb.query ("navaids", new String[] { "nav_faaid", "nav_lat", "nav_lon" },
                    where + " AND nav_type LIKE 'VOR%'", null, null, null, null, null)) {
                if (cursor.moveToFirst ()) do {
                    MapWpt mapwpt = new MapWpt ();
                    mapwpt.id  = cursor.getString (0);
                    mapwpt.lat = cursor.getDouble (1);
                    mapwpt.lon = cursor.getDouble (2);
                    mapwpt.nav = true;
                    byident.put (mapwpt.id, mapwpt);
                } while (cursor.moveToNext ());
            }

            // read airports with runway at least RUNWAYFT length
            where = where.replace ("nav_", "rwy_beg") + " AND apt_faaid=rwy_faaid";
            try (Cursor cursor = sqldb.query ("runways,airports",
                    new String[] { "apt_icaoid", "rwy_beglat", "rwy_beglon", "rwy_endlat", "rwy_endlon", "rwy_number" },
                    where, null, null, null, null, null)) {
                if (cursor.moveToFirst ()) do {
                    String number = cursor.getString (5);
                    if (! number.endsWith ("W")) {
                        double beglat = cursor.getDouble (1);
                        double beglon = cursor.getDouble (2);
                        double endlat = cursor.getDouble (3);
                        double endlon = cursor.getDouble (4);
                        int rft = (int) Math.round (Lib.LatLonDist (beglat, beglon, endlat, endlon) * Lib.FtPerNM);
                        if (rft >= RUNWAYFT) {
                            String id = cursor.getString (0);
                            MapWpt mapwpt = byident.get (id);
                            if (mapwpt == null) {
                                mapwpt = new MapWpt ();
                                mapwpt.id  = id;
                                mapwpt.lat = beglat;
                                mapwpt.lon = beglon;
                                byident.put (mapwpt.id, mapwpt);
                            }
                            if (mapwpt.rft < rft) mapwpt.rft = rft;
                        }
                    }
                } while (cursor.moveToNext ());
            }

            final MapWpt[] array = byident.values ().toArray (nullMapWptArray);
            Arrays.sort (array);

            // post results to main thread then check proper range again
            mainActivity.runOnUiThread (new Runnable () {
                @Override
                public void run ()
                {
                    wayptNorthLat = northLat;
                    wayptSouthLat = southLat;
                    wayptEastLon  = eastLon;
                    wayptWestLon  = westLon;
                    waypoints     = array;
                    updateThread  = null;
                    updateWaypoints ();
                }
            });
        }
    }

    // get magnetic variation in vicinity of drawn map
    @Override
    protected double getDispMagVar ()
    {
        return mainActivity.curLoc.magvar;
    }

    // always use simplified display
    @Override
    protected boolean getSimplify ()
    {
        return true;
    }

    // draw moving map assuming true north is up
    @Override
    protected void onDrawInnards (Canvas canvas, double trueup, double scale)
    {
        updateWaypoints ();

        // draw airplane in center of screen pointed in direction we are tracking
        canvas.save ();
        try {
            canvas.rotate ((float) mainActivity.curLoc.truecourse);
            canvas.scale (180.0F / MainActivity.airplaneHeight, 180.0F / MainActivity.airplaneHeight);
            canvas.drawPath (mainActivity.airplanePath, mainActivity.airplanePaint);
        } finally {
            canvas.restore ();
        }

        // draw range rings
        int outerNM = (int) Math.ceil (radiusNM * 0.8F);
        float outerRad = INNARDSRADIUS * outerNM / (float) radiusNM;
        canvas.drawCircle (0, 0, INNARDSRADIUS * 0.5F, rangeRingPaint);
        canvas.drawCircle (0, 0, outerRad, rangeRingPaint);

        // everything drawn below assumes trueup is up
        // this lets drawText() calls to draw text horizontally
        canvas.rotate ((float) trueup);

        // tell calcPixel() what true course is up for subsequent drawing
        trueuprad = Math.toRadians (trueup);

        // draw course line
        Waypt nwp = (mainActivity == null) ? null : mainActivity.navWaypt;
        float nwpxpix = Float.NaN;
        float nwpypix = Float.NaN;
        if ((nwp != null) && ! Double.isNaN (mainActivity.startlat) && ! Double.isNaN (mainActivity.startlon)) {
            calcPixel (nwp.lat, nwp.lon);
            nwpxpix = xpix;
            nwpypix = ypix;
            calcPixel (mainActivity.startlat, mainActivity.startlon);
            coursePaint.setStrokeWidth (25);
            canvas.drawLine (nwpxpix, nwpypix, xpix, ypix, coursePaint);
        }

        // make sure nav-to waypoint doesn't get overlapped by other waypoint
        float r = 25.0F;
        int numwpts = 0;
        if (nwp != null) {
            MapWpt mw = drawnwpts[0];
            if (! nwp.ident.equals (mw.id)) {
                mw.id  = nwp.ident;
                mw.idh = Math.round (r * 2 + coursePaint.getTextSize ());
                mw.idw = Math.round (r * 2 + coursePaint.measureText (mw.id));
            }
            mw.xpx = Math.round (nwpxpix);
            mw.ypx = Math.round (nwpypix);
            numwpts = 1;
        }

        // draw non-overlapping waypoints starting with longest runway
        for (MapWpt mapwpt : waypoints) {
            if ((nwp != null) && mapwpt.id.equals (nwp.ident)) continue;
            if (calcPixel (mapwpt.lat, mapwpt.lon)) {
                if (mapwpt.idh == 0) {
                    mapwpt.idh = Math.round (r * 2 + wayptPaint.getTextSize ());
                    mapwpt.idw = Math.round (r * 2 + wayptPaint.measureText (mapwpt.id));
                }
                mapwpt.xpx = Math.round (xpix);
                mapwpt.ypx = Math.round (ypix);
                int i = numwpts;
                while (-- i >= 0) {
                    MapWpt mw = drawnwpts[i];
                    if (mw.xpx > mapwpt.xpx + mapwpt.idw) continue;
                    if (mw.ypx > mapwpt.ypx + mapwpt.idh) continue;
                    if (mw.xpx + mw.idw < mapwpt.xpx) continue;
                    if (mw.ypx + mw.idh < mapwpt.ypx) continue;
                    break;
                }
                if (i < 0) {
                    wayptPaint.setColor (ambient ? Color.GRAY : mapwpt.nav ? Color.CYAN : Color.GREEN);
                    canvas.drawText (mapwpt.id, xpix + r, ypix - r, wayptPaint);
                    canvas.drawCircle (xpix, ypix, r, wayptPaint);
                    drawnwpts[numwpts++] = mapwpt;
                    if (numwpts >= drawnwpts.length) break;
                }
            }
        }

        // draw range numbers
        canvas.drawText (Integer.toString (radiusNM / 2), 0, INNARDSRADIUS * 0.5F, rangeRingPaint);
        canvas.drawText (Integer.toString (outerNM), 0, outerRad, rangeRingPaint);

        // always draw 'to' waypoint in magenta
        if (nwp != null) {
            coursePaint.setStrokeWidth (10);
            canvas.drawText (nwp.ident, nwpxpix + r, nwpypix - r, coursePaint);
            canvas.drawCircle (nwpxpix, nwpypix, r, coursePaint);
        }
    }

    // calculate pixel for the given lat,lon
    // return whether the point is within radius or not
    private boolean calcPixel (double lat, double lon)
    {
        double centerLat = mainActivity.curLoc.latitude;
        double centerLon = mainActivity.curLoc.longitude;
        double nm = Lib.LatLonDist (centerLat, centerLon, lat, lon);
        double tc = Lib.LatLonTC_rad (centerLat, centerLon, lat, lon) - trueuprad;
        double pix = nm / radiusNM * INNARDSRADIUS;
        xpix = (float) (pix * Math.sin (tc));
        ypix = (float) (-pix * Math.cos (tc));
        return nm < radiusNM;
    }
}
