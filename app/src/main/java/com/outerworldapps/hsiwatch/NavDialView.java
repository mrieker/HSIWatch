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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * The VOR-like nav dial, needles, etc.
 */
public class NavDialView extends OBSDialView {

    public enum Mode {
        OFF, GCT, VOR, ADF, LOC, LOCBC, ILS
    }

    private final static double VORDEFLECT = 10;  // degrees each side for VOR mode deflection
    public  final static double LOCDEFLECT =  3;  // degrees each side for ILS/LOC mode deflection
    public  final static double GSDEFLECT  =  1;  // degrees each side for GS deflection

    private char[] strbuf;
    private double deflect;
    private double slope;
    private int lastdmedist;
    private int lastdmetime;
    private int lastGpsAlt;
    private int lastGpsHdg;
    private int lastGpsKts;
    private int lastObsInt;
    private int lastToWaypt;
    private MainActivity mainActivity;
    private Paint adfNeedlePaint;
    private Paint dialBackPaint;
    private Paint dialTextPaint;
    private Paint dirArrowPaint;
    private Paint dmeDistPaint;
    private Paint dmeTimePaint;
    private Paint fmtoWayptPaint;
    private Paint gpsHdgPaint;
    private Paint gpsMinPaint;
    private Paint identPaint;
    private Paint innerRingPaint;
    private Paint modePaint;
    private Paint obsArrowPaint;
    private Paint obsIntPaint;
    private Paint outerRingPaint;
    private Paint vorNeedlePaint;
    private Path adfNeedlePath;
    private Path frArrowPath;
    private Path toArrowPath;
    private String dmeDistStr;
    private String dmeTimeStr;
    private String fmWayptStr;
    private String gpsAltStr;
    private String gpsHdgStr;
    private String gpsKtsStr;
    private String obsIntStr;
    private String toWayptStr;

    public NavDialView (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        constructor ();
    }

    public NavDialView (Context ctx)
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
        goLeftString = "exit";
        goRightString = "map";
        goUpString = "waypt";

        strbuf = new char[8];

        adfNeedlePaint = new Paint ();
        adfNeedlePaint.setStyle (Paint.Style.FILL);

        dialBackPaint = new Paint ();
        dialBackPaint.setColor (Color.DKGRAY);
        dialBackPaint.setStrokeWidth (175);
        dialBackPaint.setStyle (Paint.Style.STROKE);

        dialTextPaint = new Paint ();
        dialTextPaint.setColor (Color.WHITE);
        dialTextPaint.setStrokeWidth (10);
        dialTextPaint.setTextAlign (Paint.Align.CENTER);
        dialTextPaint.setTextSize (140);

        dirArrowPaint = new Paint ();
        dirArrowPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        dmeDistPaint = new Paint ();
        dmeDistPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        dmeDistPaint.setTextAlign (Paint.Align.RIGHT);
        dmeDistPaint.setTextSize (140);

        dmeTimePaint = new Paint ();
        dmeTimePaint.setStyle (Paint.Style.FILL_AND_STROKE);
        dmeTimePaint.setTextAlign (Paint.Align.RIGHT);
        dmeTimePaint.setTextSize (120);

        fmtoWayptPaint = new Paint ();
        fmtoWayptPaint.setColor (Color.WHITE);
        fmtoWayptPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        fmtoWayptPaint.setTextAlign (Paint.Align.RIGHT);
        fmtoWayptPaint.setTextSize (130);

        gpsHdgPaint = new Paint ();
        gpsHdgPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        gpsHdgPaint.setTextAlign (Paint.Align.LEFT);
        gpsHdgPaint.setTextSize (140);

        gpsMinPaint = new Paint ();
        gpsMinPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        gpsMinPaint.setTextAlign (Paint.Align.LEFT);
        gpsMinPaint.setTextSize (130);

        identPaint = new Paint ();
        identPaint.setColor (Color.WHITE);
        identPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        identPaint.setTextAlign (Paint.Align.LEFT);
        identPaint.setTextSize (130);

        innerRingPaint = new Paint ();
        innerRingPaint.setColor (Color.WHITE);
        innerRingPaint.setStrokeWidth (10);
        innerRingPaint.setStyle (Paint.Style.STROKE);

        modePaint = new Paint ();
        modePaint.setStyle (Paint.Style.FILL_AND_STROKE);
        modePaint.setTextAlign (Paint.Align.LEFT);
        modePaint.setTextSize (110);

        obsArrowPaint = new Paint ();
        obsArrowPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        obsIntPaint = new Paint ();
        obsIntPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        obsIntPaint.setTextAlign (Paint.Align.RIGHT);
        obsIntPaint.setTextSize (140);

        outerRingPaint = new Paint ();
        outerRingPaint.setColor (Color.GRAY);
        outerRingPaint.setStrokeWidth (50);
        outerRingPaint.setStyle (Paint.Style.STROKE);

        vorNeedlePaint = new Paint ();
        vorNeedlePaint.setColor (Color.WHITE);
        vorNeedlePaint.setStrokeWidth (25);

        adfNeedlePath = new Path ();
        adfNeedlePath.moveTo (  0, -604);
        adfNeedlePath.lineTo (-55, -473);
        adfNeedlePath.lineTo (-20, -473);
        adfNeedlePath.lineTo (-20,  604);
        adfNeedlePath.lineTo ( 20,  604);
        adfNeedlePath.lineTo ( 20, -473);
        adfNeedlePath.lineTo ( 55, -473);
        adfNeedlePath.lineTo (  0, -604);

        frArrowPath = new Path ();
        frArrowPath.moveTo (372+36,  80);
        frArrowPath.lineTo (445+36, 176);
        frArrowPath.lineTo (518+36,  80);

        toArrowPath = new Path ();
        toArrowPath.moveTo (372+36,  -80);
        toArrowPath.lineTo (445+36, -176);
        toArrowPath.lineTo (518+36,  -80);

        lastdmedist = -999;
        lastdmetime = -999;
        lastGpsAlt  = -999;
        lastGpsHdg  = -999;
        lastGpsKts  = -999;
        lastObsInt  = -999;
        lastToWaypt = -999;

        obsIntStr  = "";
        toWayptStr = "";
        fmWayptStr = "";
        dmeDistStr = "";
        dmeTimeStr = "";
        gpsAltStr  = "";
        gpsHdgStr  = "";
        gpsKtsStr  = "";

        setAmbient ();
    }

    @Override
    protected View getDownView ()
    {
        return mainActivity.menuMainPage.getView ();
    }

    @Override
    protected View getLeftView ()
    {
        return null;
    }

    @Override
    protected View getRightView ()
    {
        return mainActivity.mapPageView;
    }

    @Override
    protected View getUpView ()
    {
        return mainActivity.navModeButton.getView ();
    }

    /**
     * Use grayscale for ambient mode.
     */
    public void setAmbient ()
    {
        boolean ambient = (mainActivity != null) && mainActivity.ambient;
        boolean redRing = (mainActivity != null) && mainActivity.redRingOn;
        if (ambient) {
            adfNeedlePaint.setColor (Color.WHITE);
            dialBackPaint.setColor (Color.BLACK);
            dirArrowPaint.setColor (Color.GRAY);
            dmeDistPaint.setColor (Color.LTGRAY);
            dmeTimePaint.setColor (Color.LTGRAY);
            gpsHdgPaint.setColor (Color.GRAY);
            gpsMinPaint.setColor (Color.GRAY);
            modePaint.setColor (Color.GRAY);
            obsArrowPaint.setColor (Color.LTGRAY);
            obsIntPaint.setColor (Color.LTGRAY);
            outerRingPaint.setColor (redRing ? Color.LTGRAY : Color.GRAY);
        } else {
            adfNeedlePaint.setColor (Color.GREEN);
            dialBackPaint.setColor (Color.DKGRAY);
            dirArrowPaint.setColor (Color.GREEN);
            dmeDistPaint.setColor (0xFFFFAA00);
            dmeTimePaint.setColor (0xFFFFAA00);
            gpsHdgPaint.setColor (Color.RED);
            gpsMinPaint.setColor (Color.RED);
            modePaint.setColor (Color.YELLOW);
            obsArrowPaint.setColor (Color.YELLOW);
            obsIntPaint.setColor (Color.YELLOW);
            outerRingPaint.setColor (redRing ? Color.RED : Color.GRAY);
        }
    }

    /**
     * Set needle deflection.
     * @param d = GCT: deflection degrees
     *            VOR: deflection degrees
     *            ADF: relative bearing degrees
     *            LOC: deflection degrees
     *    < 0: deflect needle to left of center
     *    > 0: deflect needle to right of center
     */
    public void setDeflect (double d)
    {
        while (d >= 180) d -= 360;
        while (d < -180) d += 360;
        deflect = d;
        invalidate ();
    }

    /**
     * Set glideslope deviation if in Mode.ILS.
     * @param s = degrees
     */
    public void setSlope (double s)
    {
        double maxdeflect = GSDEFLECT;
        double pegdeflect = maxdeflect * 1.2;
        if (s >= pegdeflect) s =  pegdeflect;
        if (s < -pegdeflect) s = -pegdeflect;
        slope = s / maxdeflect;
        invalidate ();
    }

    /**
     * Get magnetic variation is vicinity of drawing.
     * In our case, it is magvar that OBS was set with.
     */
    @Override
    protected double getDispMagVar ()
    {
        return mainActivity.obsMagVar;
    }

    /**
     * Draw the nav widget innards.
     */
    @Override
    protected void onDrawInnards (Canvas canvas, double trueup, double scale)
    {
        Mode mode = mainActivity.navModeButton.getMode ();

        // set rotation and draw texts
        if (mode == Mode.OFF) {
            // leave OFF mode's swipe messages upright
            canvas.rotate ((float) trueup);
            if (mainActivity.downloadThread.getSqlDB () != null) {
                canvas.drawText ("swipe down", 0, -390, dialTextPaint);
                canvas.drawText ("from here to", 0, -240, dialTextPaint);
                canvas.drawText ("enter waypoint", 0, -90, dialTextPaint);
            } else {
                canvas.drawText ("swipe up from", 0, -240, dialTextPaint);
                canvas.drawText ("OFF button to", 0, -90, dialTextPaint);
                canvas.drawText ("open menu page", 0, 60, dialTextPaint);
            }
        } else {
            // turn everything to align with OBS setting (yellow triangle)
            // obsSetting is always magnetic so convert to true
            canvas.rotate ((float) (mainActivity.obsSetting - getDispMagVar ()));

            // draw texts
            updateTexts ();
            canvas.drawText (obsIntStr, -55, -390, obsIntPaint);
            canvas.drawText (toWayptStr, -55, -245, fmtoWayptPaint);
            canvas.drawText (fmWayptStr, -55, -100, fmtoWayptPaint);
            canvas.drawText (dmeDistStr, -55 + dmeDistPaint.getTextSize () * dmeDistPaint.getTextSkewX (), 190, dmeDistPaint);
            canvas.drawText (dmeTimeStr, -55, 330, dmeTimePaint);
            canvas.drawText (gpsHdgStr, 55, -390, gpsHdgPaint);
            canvas.drawText (gpsAltStr, 55, -245, gpsMinPaint);
            canvas.drawText (gpsKtsStr, 55, -100, gpsMinPaint);
            canvas.drawText (mainActivity.navWaypt.ident, 55, 250, identPaint);
        }

        // always display mode string (OFF,GCT,...,ILS)
        canvas.drawText (mode.toString (), 85, 410, modePaint);

        // draw needles and dots assuming yellow triangle is at top
        switch (mode) {

            // draw glideslope needle
            case ILS: {
                double needleCentY = slope * -412;
                double needleLeftX = -412 * 1.2;
                double needleRiteX =  412 * 1.2;
                canvas.drawLine ((float) needleLeftX, (float) needleCentY, (float) needleRiteX, (float) needleCentY, vorNeedlePaint);
                // fall through
            }

            // GCT/VOR/LOC-style deflection dots and needle
            case GCT:
            case VOR:
            case LOC:
            case LOCBC: {

                // draw inner ring
                canvas.drawCircle (0, 0, 412.0F / 5, innerRingPaint);

                // draw dots
                for (int ir = 1; ++ ir <= 5;) {
                    float r = ir * 412.0F / 5;
                    canvas.drawCircle ( r, 0, 412.0F / 20, innerRingPaint);
                    canvas.drawCircle (-r, 0, 412.0F / 20, innerRingPaint);
                    canvas.drawCircle (0,  r, 412.0F / 20, innerRingPaint);
                    canvas.drawCircle (0, -r, 412.0F / 20, innerRingPaint);
                }

                // draw GCT/VOR/localizer needle
                double degdiff = deflect;
                if ((degdiff > -90) && (degdiff <= 90)) {
                    if ((mode == Mode.GCT) || (mode == Mode.VOR)) canvas.drawPath (frArrowPath, dirArrowPaint);
                } else {
                    if ((mode == Mode.GCT) || (mode == Mode.VOR)) canvas.drawPath (toArrowPath, dirArrowPaint);
                    degdiff += 180;
                    if (degdiff >= 180) degdiff -= 360;
                    degdiff = - degdiff;
                }
                double maxdeflect = ((mode == Mode.GCT) || (mode == Mode.VOR)) ? VORDEFLECT : LOCDEFLECT;
                double pegdeflect = maxdeflect * 1.2;
                if (degdiff >= pegdeflect) degdiff =  pegdeflect;
                if (degdiff < -pegdeflect) degdiff = -pegdeflect;
                double needleMidX = degdiff / maxdeflect * 412;
                double needleTopY = -412 * 1.2;
                double needleBotY =  412 * 1.2;
                canvas.drawLine ((float) needleMidX, (float) needleTopY, (float) needleMidX, (float) needleBotY, vorNeedlePaint);
                break;
            }

            // ADF-style needle
            case ADF: {
                canvas.rotate ((float) deflect);
                canvas.drawPath (adfNeedlePath, adfNeedlePaint);
                canvas.rotate ((float) - deflect);
                break;
            }
        }
    }

    // update any text strings that need it
    private void updateTexts ()
    {
        GpsLocation curLoc = mainActivity.curLoc;
        Waypt navWaypt = mainActivity.navWaypt;

        int obsint = (int) Math.round (mainActivity.obsSetting);
        if (lastObsInt != obsint) {
            lastObsInt = obsint;
            while (obsint <=  0) obsint += 360;
            while (obsint > 360) obsint -= 360;
            strbuf[0] = (char) ((obsint / 100) + '0');
            strbuf[1] = (char) ((obsint / 10 % 10) + '0');
            strbuf[2] = (char) ((obsint % 10) + '0');
            strbuf[3] = '\u00B0';
            obsIntStr = new String (strbuf, 0, 4);
        }

        double towp = navWaypt.getMagRadTo (mainActivity.navModeButton.getMode (), curLoc);
        int towaypt = (int) Math.round (towp);
        if (lastToWaypt != towaypt) {
            lastToWaypt = towaypt;
            while (towaypt <=  0) towaypt += 360;
            while (towaypt > 360) towaypt -= 360;
            strbuf[2] = '\u25B2';
            strbuf[3] = (char) ((towaypt / 100) + '0');
            strbuf[4] = (char) ((towaypt / 10 % 10) + '0');
            strbuf[5] = (char) ((towaypt % 10) + '0');
            strbuf[6] = '\u00B0';
            toWayptStr = new String (strbuf, 2, 5);
            towaypt -= 180;
            if (towaypt <= 0) towaypt += 360;
            strbuf[2] = '\u25BC';
            strbuf[3] = (char) ((towaypt / 100) + '0');
            strbuf[4] = (char) ((towaypt / 10 % 10) + '0');
            fmWayptStr = new String (strbuf, 2, 5);
        }

        double dmenm = Lib.LatLonDist (curLoc.lat, curLoc.lon, navWaypt.dme_lat, navWaypt.dme_lon);
        boolean slant = !Double.isNaN (navWaypt.elev);
        if (slant) dmenm = Math.hypot (dmenm, curLoc.altitude / Lib.MPerNM - navWaypt.elev / Lib.FtPerNM);
        int d10 = (int) Math.round (dmenm * 10.0);
        if (d10 > 9999) d10 = 10 * (int) Math.round (dmenm);
        if (lastdmedist != d10) {
            lastdmedist = d10;
            int i;
            if (d10 > 9999) {
                i = Lib.formatDigits (strbuf, 7, 1, d10 / 10);
            } else {
                strbuf[6] = (char) (d10 % 10 + '0');
                strbuf[5] = '.';
                i = Lib.formatDigits (strbuf, 5, 1, d10 / 10);
            }
            dmeDistStr = new String (strbuf, i, 7 - i);
        }

        int dmeTimeSec = (int) Math.round (dmenm * Lib.MPerNM / curLoc.speed);
        if (dmeTimeSec >= 3600*100) dmeTimeSec = -1;
        if (lastdmetime != dmeTimeSec) {
            lastdmetime = dmeTimeSec;
            if (dmeTimeSec < 0) {
                dmeTimeStr = "\u2012\u2012:\u2012\u2012:\u2012\u2012";
            } else {
                strbuf[0] = (char) (dmeTimeSec / 36000 + '0');
                strbuf[1] = (char) (dmeTimeSec / 3600 % 10 + '0');
                strbuf[2] = ':';
                strbuf[3] = (char) (dmeTimeSec / 600 % 6 + '0');
                strbuf[4] = (char) (dmeTimeSec / 60 % 10 + '0');
                strbuf[5] = ':';
                strbuf[6] = (char) (dmeTimeSec / 10 % 6 + '0');
                strbuf[7] = (char) (dmeTimeSec % 10 + '0');
                dmeTimeStr = new String (strbuf, 0, 8);
            }
        }

        if (curLoc.speed > MainActivity.gpsMinSpeedMPS) {
            int gpshdg = (int) Math.round (curLoc.truecourse + curLoc.magvar);
            if (lastGpsHdg != gpshdg) {
                lastGpsHdg = gpshdg;
                while (gpshdg <=  0) gpshdg += 360;
                while (gpshdg > 360) gpshdg -= 360;
                strbuf[0] = (char) ((gpshdg / 100) + '0');
                strbuf[1] = (char) ((gpshdg / 10 % 10) + '0');
                strbuf[2] = (char) ((gpshdg % 10) + '0');
                strbuf[3] = '\u00B0';
                gpsHdgStr = new String (strbuf, 0, 4);
            }
        } else {
            gpsHdgStr = "\u2012\u2012\u2012\u00B0";
        }

        int gpsalt = (int) Math.round (curLoc.altitude * Lib.FtPerM);
        if (lastGpsAlt != gpsalt) {
            lastGpsAlt = gpsalt;
            boolean neg = gpsalt < 0;
            if (neg) gpsalt = - gpsalt;
            strbuf[6] = 'f';
            strbuf[7] = 't';
            int i = Lib.formatDigits (strbuf, 6, 1, gpsalt);
            if (neg) strbuf[--i] = '\u2012';
            gpsAltStr = new String (strbuf, i, 8 - i);
        }

        int gpskts = (int) Math.round (curLoc.speed * Lib.KtPerMPS);
        if (lastGpsKts != gpskts) {
            lastGpsKts = gpskts;
            strbuf[6] = 'k';
            strbuf[7] = 't';
            int i = Lib.formatDigits (strbuf, 6, 1, gpskts);
            gpsKtsStr = new String (strbuf, i, 8 - i);
        }
    }
}
