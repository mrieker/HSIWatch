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
import android.graphics.Path;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;

public class RwyDiagView extends OBSDialView implements Invalidatable {
    private final static float rwytextsize  = 120;

    private static class RwyPair {
        public String numa;
        public String numb;
        public double lata;
        public double lona;
        public double latb;
        public double lonb;
        public int length;
        public int width;

        public double dista;        // nm from airport center
        public double distb;        // nm from airport center

        public float rotate;        // degrees; numa=36: approx 0;
                                    // numa=34: approx -20; numa=2: approx 20

        public float centerxpix;    // center of runway
        public float centerypix;
        public float widthpix;      // width in pixels
        public float lengthpix;     // length in pixels
    }

    private BuildThread buildThread;
    private double pixpernm;
    private MainActivity mainActivity;
    private OpenStreetMap openStreetMap;
    private Paint backPaint;
    private Paint numberBGPaint;
    private Paint numberFGPaint;
    private Paint runwayPaint;
    private RwyPair[] rwyPairs;
    private View aptInfoPage;
    private Waypt waypoint;
    private Waypt.AptWaypt airport;

    private final static RwyPair[] nullRwyPairArray = new RwyPair[0];

    private final static String[] rwycols = {
            "rwy_number", "rwy_beglat", "rwy_beglon", "rwy_endlat", "rwy_endlon", "rwy_length", "rwy_width"
    };

    public RwyDiagView (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        constructor ();
    }

    public RwyDiagView (Context ctx)
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

        goDownString = "menu";
        goLeftString = "back";
        goUpString   = "info";

        rwyPairs = nullRwyPairArray;

        backPaint = new Paint ();
        backPaint.setColor (Color.BLACK);
        backPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        numberBGPaint = new Paint ();
        numberBGPaint.setStrokeWidth (rwytextsize / 4.0F);
        numberBGPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        numberBGPaint.setTextAlign (Paint.Align.CENTER);
        numberBGPaint.setTextSize (rwytextsize);

        numberFGPaint = new Paint ();
        numberFGPaint.setStrokeWidth (rwytextsize / 24.0F);
        numberFGPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        numberFGPaint.setTextAlign (Paint.Align.CENTER);
        numberFGPaint.setTextSize (rwytextsize);

        runwayPaint = new Paint ();
        runwayPaint.setColor (Color.GRAY);
        runwayPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        openStreetMap = new OpenStreetMap (mainActivity);

        setAmbient ();
    }

    protected View getDownView () { return mainActivity.menuMainPage.getView (); }
    protected View getLeftView () { openStreetMap.CloseBitmaps (); return null; }
    protected View getRightView () { return null; }

    @SuppressLint({ "InflateParams", "SetTextI18n" })
    protected View getUpView ()
    {
        // get overall display loaded if not already
        if (aptInfoPage == null) {
            LayoutInflater inflater = mainActivity.getLayoutInflater ();
            aptInfoPage = inflater.inflate (R.layout.info_page, null);
            Button infoBackButton = aptInfoPage.findViewById (R.id.infoBack);
            infoBackButton.setOnClickListener (mainActivity.backButtonListener);
        }

        // fill in waypoint information text
        buildInfoText ();

        // display the info page
        return aptInfoPage;
    }

    // fill in the info text box with the current waypoint info
    private void buildInfoText ()
    {
        int tagcolor = mainActivity.ambient ? Color.LTGRAY : Color.YELLOW;

        // build string with waypoint info
        SpannableStringBuilder ssb = new SpannableStringBuilder ();
        if (waypoint == null) {
            ssb.append ("no waypoint selected");
        } else {

            // waypoint ident and name
            ForegroundColorSpan fcs = new ForegroundColorSpan (tagcolor);
            ssb.append (waypoint.ident);
            ssb.append (": ");
            ssb.setSpan (fcs, 0, ssb.length (), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            ssb.append (waypoint.name);

            // airport frequencies and such
            if (airport != null) {

                // add yellow color to keyword of each desc2 line
                for (String des : airport.desc2.split ("\n")) {
                    ssb.append ('\n');
                    int i = des.indexOf (':');
                    if (i < 0) ssb.append (des);
                    else {
                        fcs = new ForegroundColorSpan (tagcolor);
                        int j = ssb.length ();
                        ssb.append (des, 0, ++ i);
                        int k = ssb.length ();
                        ssb.append (des, i, des.length ());
                        ssb.setSpan (fcs, j, k, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            // for small scrolling area at the bottom
            ssb.append ("\n\n\n\n\n\n\n");
        }

        // put text in box on screen
        TextView aptInfoText = aptInfoPage.findViewById (R.id.infoText);
        aptInfoText.setText (ssb);
    }

    // ambient level changed
    // update paints and redraw
    public void setAmbient ()
    {
        // fix up moving map in case it is showing
        if (mainActivity.ambient) {
            numberBGPaint.setColor (Color.DKGRAY);
            numberFGPaint.setColor (Color.LTGRAY);
        } else {
            numberBGPaint.setColor (Color.WHITE);
            numberFGPaint.setColor (Color.RED);
        }
        invalidate ();

        // fix up waypoint info in case it is showing
        if (aptInfoPage != null) buildInfoText ();
    }

    // magnetic variation in vicinity of what is drawn
    @Override
    protected double getDispMagVar ()
    {
        return (airport == null) ? 0.0 : airport.getMagVar (0.0);
    }

    // draw diagram assuming true north is up
    @Override
    protected void onDrawInnards (Canvas canvas, double trueup, double scale)
    {
        canvas.drawRect (- INNARDSRADIUS, - INNARDSRADIUS,
                INNARDSRADIUS * 2, INNARDSRADIUS * 2, backPaint);

        // if waypoint changed, set up to draw its runways
        if (waypoint != mainActivity.navWaypt) {
            waypoint = mainActivity.navWaypt;
            airport  = null;
            rwyPairs = nullRwyPairArray;
            if (waypoint instanceof Waypt.AptWaypt) {
                airport = (Waypt.AptWaypt) waypoint;
            }
            if (waypoint instanceof Waypt.LocWaypt) {
                Waypt.LocWaypt locwp = (Waypt.LocWaypt) waypoint;
                SQLiteDatabase sqldb = mainActivity.downloadThread.getSqlDB ();
                if (sqldb != null) {
                    airport = Waypt.AptWaypt.find (sqldb, locwp.apticao, true);
                }
            }
            if (airport != null) {
                SQLiteDatabase sqldb = mainActivity.downloadThread.getSqlDB ();
                if ((sqldb != null) && (buildThread == null)) {
                    buildThread = new BuildThread ();
                    buildThread.aptwp = airport;
                    buildThread.sqldb = sqldb;
                    buildThread.start ();
                }
            }
        }

        if (airport == null) {
            canvas.rotate ((float) trueup);
            canvas.drawText ("no airport or", 0, rwytextsize * -0.5F, numberFGPaint);
            canvas.drawText ("localizer or runway", 0, rwytextsize * 0.5F, numberFGPaint);
            canvas.drawText ("selected", 0, rwytextsize * 1.5F, numberFGPaint);
        } else {

            // display copyright message for first 3 seconds
            if ((pixmap.copyrtSize == 0.0F) && (pixmap.copyrtPath == null)) {
                float r = INNARDSRADIUS * 0.92F;
                pixmap.copyrtPath = new Path ();
                pixmap.copyrtPath.addArc (-r, -r, r, r, (float) trueup - 90, 180);
                pixmap.copyrtSize = INNARDSRADIUS * 0.10F;
                mainActivity.myHandler.postDelayed (pixmap, 3000);
            }

            // draw background tiles
            if (!mainActivity.ambient) {
                double cppsi = mainActivity.dotsPerSqIn / (scale * scale);
                if (pixmap.canPixPerSqIn != cppsi) {
                    pixmap.canPixPerSqIn = cppsi;
                    openStreetMap.ComputeZoom (pixmap);
                }
                openStreetMap.Draw (canvas, pixmap, this);
            }

            // draw runway outlines
            drawRunways (canvas, false, runwayPaint);

            // draw runway numbers
            drawRunways (canvas, true, numberBGPaint);
            drawRunways (canvas, true, numberFGPaint);
        }
    }

    // draw all the runways
    private void drawRunways (Canvas canvas, boolean numbers, Paint paint)
    {

        canvas.save ();
        try {
            float lasttx = 0;
            float lastty = 0;
            float lastrot = 0;
            for (RwyPair rp : rwyPairs) {
                float cxp = rp.centerxpix;
                float cyp = rp.centerypix;
                float hwp = rp.widthpix  / 2.0F;
                float hlp = rp.lengthpix / 2.0F;
                float typ = cyp + hlp + rwytextsize;

                // equivalent to:
                //  rotate (-lastrot)               // undo last rotate
                //  translate (-lasttx, -lastty)    // undo last translate
                //  translate (cxp, cyp)            // do this translate
                //  rotate (rp.rotate)              // do this rotate
                float cos_lastrot = (float) Math.cos (Math.toRadians (lastrot));
                float sin_lastrot = (float) Math.sin (Math.toRadians (lastrot));
                canvas.translate (
                        cos_lastrot * (cxp - lasttx) + sin_lastrot * (cyp - lastty),
                        cos_lastrot * (cyp - lastty) - sin_lastrot * (cxp - lasttx)
                );
                canvas.rotate (rp.rotate - lastrot);
                lastrot = rp.rotate;
                lasttx  = cxp;
                lastty  = cyp;

                // draw either the numbers or the runway rectangle
                if (numbers) {
                    canvas.drawText (rp.numa, 0, typ - cyp, paint);
                    canvas.scale (-1, -1);
                    lastrot += 180.0F;
                    canvas.drawText (rp.numb, 0, typ - cyp, paint);
                } else {
                    canvas.drawRect (- hwp, - hlp, hwp, hlp, paint);
                }
            }
        } finally {
            canvas.restore ();
        }
    }

    // get pixel x,y for a given lat,lon
    private void getPixXY (double lat, double lon, PointD pix)
    {
        double nm = Lib.LatLonDist (airport.lat, airport.lon, lat, lon);
        double px = nm * pixpernm;
        double tc = Lib.LatLonTC_rad (airport.lat, airport.lon, lat, lon);
        pix.x =   px * Math.sin (tc);
        pix.y = - px * Math.cos (tc);
    }

    // get lat,lon for a given pixel x,y
    private void getLatLon (double xpix, double ypix, LatLon ll)
    {
        double tc = Math.toDegrees (Math.atan2 (xpix, - ypix));
        double nm = Math.hypot (xpix, ypix) / pixpernm;
        ll.lat = Lib.LatHdgDist2Lat (airport.lat, tc, nm);
        ll.lon = Lib.LatLonHdgDist2Lon (airport.lat, airport.lon, tc, nm);
    }

    private final RwyPixelMapper pixmap = new RwyPixelMapper ();
    private class RwyPixelMapper extends PixelMapper implements Runnable {

        @Override
        public void LatLon2CanPixAprox (double lat, double lon, PointD pix)
        {
            getPixXY (lat, lon, pix);
        }

        @Override
        public void run ()
        {
            copyrtPath = null;
            invalidate ();
        }
    }

    // build drawable list of runways for an airport
    private class BuildThread extends Thread {
        public SQLiteDatabase sqldb;
        public Waypt.AptWaypt aptwp;

        @Override
        public void run ()
        {
            // get list of runway pairs associated with the given airport
            final LinkedList<RwyPair> rps = new LinkedList<> ();
            double radiusnm = 0.25;
            try (Cursor cursor = sqldb.query ("runways", rwycols,
                    "rwy_faaid='" + aptwp.faaid + "'",
                    null, null, null, null, null)) {
                if (cursor.moveToFirst ()) do {
                    String numa = cursor.getString (0);
                    double lata = cursor.getDouble (1);
                    double lona = cursor.getDouble (2);
                    double latb = cursor.getDouble (3);
                    double lonb = cursor.getDouble (4);
                    if (numa.startsWith ("0")) numa = numa.substring (1);
                    for (RwyPair rp : rps) {
                        if ((rp.lata == latb) && (rp.lona == lonb) &&
                                (rp.latb == lata) && (rp.lonb == lona)) {
                            rp.numb = numa;
                            numa = null;
                            break;
                        }
                    }
                    if (numa != null) {
                        RwyPair rp = new RwyPair ();
                        rp.numa    = numa;
                        rp.numb    = "";
                        rp.lata    = lata;
                        rp.lona    = lona;
                        rp.latb    = latb;
                        rp.lonb    = lonb;
                        rp.length  = cursor.getInt (5);
                        rp.width   = cursor.getInt (6);
                        rp.dista   = Lib.LatLonDist (aptwp.lat, aptwp.lon, rp.lata, rp.lona);
                        rp.distb   = Lib.LatLonDist (aptwp.lat, aptwp.lon, rp.latb, rp.lonb);
                        rps.add (rp);
                        radiusnm   = Math.max (radiusnm, Math.max (rp.dista, rp.distb));
                    }
                } while (cursor.moveToNext ());
            }

            // compute pixels per nautical mile
            // leave room for runway numbers plus a little extra
            pixpernm = (INNARDSRADIUS - rwytextsize * 2) / radiusnm;

            // compute pixel dimensions, locations, orientations, etc of runways
            double pixperft = pixpernm / Lib.FtPerNM;
            PointD apix = new PointD ();
            PointD bpix = new PointD ();
            for (RwyPair rp : rps) {
                rp.lengthpix = (float) (rp.length * pixperft);
                rp.widthpix  = Math.max (5.0F, (float) (rp.width  * pixperft));

                getPixXY (rp.lata, rp.lona, apix);
                getPixXY (rp.latb, rp.lonb, bpix);

                rp.centerxpix = (float) (apix.x + bpix.x) / 2.0F;
                rp.centerypix = (float) (apix.y + bpix.y) / 2.0F;

                rp.rotate = (float) Math.toDegrees (Math.atan2 (bpix.x - apix.x, apix.y - bpix.y));
            }

            mainActivity.runOnUiThread (new Runnable () {
                @Override
                public void run ()
                {
                    if (aptwp == airport) {
                        buildThread = null;

                        rwyPairs = rps.toArray (nullRwyPairArray);

                        pixmap.canvasWidth  = INNARDSRADIUS * 2;
                        pixmap.canvasHeight = INNARDSRADIUS * 2;

                        double radnm = INNARDSRADIUS / pixpernm;
                        pixmap.canvasNorthLat = Lib.LatHdgDist2Lat (aptwp.lat, 0.0, radnm);
                        pixmap.canvasSouthLat = Lib.LatHdgDist2Lat (aptwp.lat, 180.0, radnm);
                        pixmap.canvasEastLon  = Lib.LatLonHdgDist2Lon (aptwp.lat, aptwp.lon, 90.0, radnm);
                        pixmap.canvasWestLon  = Lib.LatLonHdgDist2Lon (aptwp.lat, aptwp.lon, 270.0, radnm);

                        pixmap.copyrtPath = null;
                        pixmap.copyrtSize = 0.0F;

                        LatLon ll = new LatLon ();
                        getLatLon (-INNARDSRADIUS, -INNARDSRADIUS, ll);
                        pixmap.lastTlLat = ll.lat;
                        pixmap.lastTlLon = ll.lon;
                        getLatLon (INNARDSRADIUS, -INNARDSRADIUS, ll);
                        pixmap.lastTrLat = ll.lat;
                        pixmap.lastTrLon = ll.lon;
                        getLatLon (-INNARDSRADIUS, INNARDSRADIUS, ll);
                        pixmap.lastBlLat = ll.lat;
                        pixmap.lastBlLon = ll.lon;
                        getLatLon (INNARDSRADIUS, INNARDSRADIUS, ll);
                        pixmap.lastBrLat = ll.lat;
                        pixmap.lastBrLon = ll.lon;

                        // re-compute zoom level
                        pixmap.canPixPerSqIn = 0.0;

                        invalidate ();
                    } else {
                        buildThread = new BuildThread ();
                        buildThread.aptwp = airport;
                        buildThread.sqldb = sqldb;
                        buildThread.start ();
                    }
                }
            });
        }
    }
}
