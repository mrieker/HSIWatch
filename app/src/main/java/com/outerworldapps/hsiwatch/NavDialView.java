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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * The VOR-like nav dial, needles, etc.
 */
public class NavDialView extends View {

    public enum Mode {
        OFF, GCT, VOR, ADF, LOC, LOCBC, ILS
    }

    public interface OBSChangedListener {
        void obsChanged (double obs);
    }

    private final static double DIALRATIO  =  5;  // number of times drag finger to drag obs once
    public  final static double VORDEFLECT = 10;  // degrees each side for VOR mode deflection
    public  final static double LOCDEFLECT =  3;  // degrees each side for ILS/LOC mode deflection
    public  final static double GSDEFLECT  =  1;  // degrees each side for GS deflection

    private final static int airplaneHeight = 313 - 69;

    private boolean ambient;
    private boolean redRing;
    public  boolean revRotate;
    private boolean showarpln;
    private double deflect;
    private double heading;
    private double obsSetting;
    private double slope;
    private double touchDownOBS;
    private double touchDownX;
    private double touchDownY;
    private char[] strbuf;
    private int lastdmedist;
    private int lastdmetime;
    private int lastGpsHdg;
    private int lastGpsHms;
    private int lastGpsKts;
    private int lastObsInt;
    private int lastToWaypt;
    private Mode mode;
    public  OBSChangedListener obsChangedListener;
    private Paint adfNeedlePaint;
    private Paint airplanePaint;
    private Paint dialBackPaint;
    private Paint dialFatPaint;
    private Paint dialMidPaint;
    private Paint dialTextPaint;
    private Paint dialThinPaint;
    private Paint dirArrowPaint;
    private Paint dmeDistPaint;
    private Paint dmeTimePaint;
    private Paint fmtoWayptPaint;
    private Paint gpsHdgPaint;
    private Paint gpsMinPaint;
    private Paint innerRingPaint;
    private Paint obsArrowPaint;
    private Paint obsIntPaint;
    private Paint outerRingPaint;
    private Paint vorNeedlePaint;
    private Path adfNeedlePath;
    private Path airplanePath;
    private Path frArrowPath;
    private Path obsArrowPath;
    private Path toArrowPath;
    private String dmeDistStr;
    private String dmeTimeStr;
    private String fmWayptStr;
    private String gpsHdgStr;
    private String gpsHmsStr;
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
        strbuf = new char[8];

        adfNeedlePaint = new Paint ();
        adfNeedlePaint.setStyle (Paint.Style.FILL);

        dialBackPaint = new Paint ();
        dialBackPaint.setColor (Color.DKGRAY);
        dialBackPaint.setStrokeWidth (175);
        dialBackPaint.setStyle (Paint.Style.STROKE);

        dialFatPaint = new Paint ();
        dialFatPaint.setColor (Color.WHITE);
        dialFatPaint.setStrokeWidth (25);

        dialMidPaint = new Paint ();
        dialMidPaint.setColor (Color.WHITE);
        dialMidPaint.setStrokeWidth (15);

        dialTextPaint = new Paint ();
        dialTextPaint.setColor (Color.WHITE);
        dialTextPaint.setStrokeWidth (10);
        dialTextPaint.setTextAlign (Paint.Align.CENTER);
        dialTextPaint.setTextSize (140);

        dialThinPaint = new Paint ();
        dialThinPaint.setColor (Color.WHITE);
        dialThinPaint.setStrokeWidth (10);

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
        gpsHdgPaint.setTextSize (130);

        gpsMinPaint = new Paint ();
        gpsMinPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        gpsMinPaint.setTextAlign (Paint.Align.LEFT);
        gpsMinPaint.setTextSize (120);

        innerRingPaint = new Paint ();
        innerRingPaint.setColor (Color.WHITE);
        innerRingPaint.setStrokeWidth (10);
        innerRingPaint.setStyle (Paint.Style.STROKE);

        obsArrowPaint = new Paint ();
        obsArrowPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        obsIntPaint = new Paint ();
        obsIntPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        obsIntPaint.setTextAlign (Paint.Align.RIGHT);
        obsIntPaint.setTextSize (130);

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
        frArrowPath.moveTo (372,  92);
        frArrowPath.lineTo (445, 188);
        frArrowPath.lineTo (518,  92);

        obsArrowPath = new Path ();
        obsArrowPath.moveTo (-71, -518);
        obsArrowPath.lineTo (  0, -624);
        obsArrowPath.lineTo ( 71, -518);

        toArrowPath = new Path ();
        toArrowPath.moveTo (372,  -92);
        toArrowPath.lineTo (445, -188);
        toArrowPath.lineTo (518,  -92);

        // airplane icon pointing up with center at (0,0)
        airplanePath = new Path ();
        int acy = 181;
        airplanePath.moveTo (0, 313 - acy);
        airplanePath.lineTo ( -44, 326 - acy);
        airplanePath.lineTo ( -42, 301 - acy);
        airplanePath.lineTo ( -15, 281 - acy);
        airplanePath.lineTo ( -18, 216 - acy);
        airplanePath.lineTo (-138, 255 - acy);
        airplanePath.lineTo (-138, 219 - acy);
        airplanePath.lineTo (-17, 150 - acy);
        airplanePath.lineTo ( -17,  69 - acy);
        airplanePath.cubicTo (0, 39 - acy,
                0, 39 - acy,
                +17, 69 - acy);
        airplanePath.lineTo ( +17, 150 - acy);
        airplanePath.lineTo (+138, 219 - acy);
        airplanePath.lineTo (+138, 255 - acy);
        airplanePath.lineTo ( +18, 216 - acy);
        airplanePath.lineTo ( +15, 281 - acy);
        airplanePath.lineTo ( +42, 301 - acy);
        airplanePath.lineTo ( +44, 326 - acy);
        airplanePath.lineTo (0, 313 - acy);

        airplanePaint = new Paint ();
        airplanePaint.setColor (Color.RED);
        airplanePaint.setStyle (Paint.Style.FILL);

        mode = Mode.OFF;

        lastdmedist = -999;
        lastdmetime = -999;
        lastGpsHdg  = -999;
        lastGpsHms  = -999;
        lastGpsKts  = -999;
        lastObsInt  = -999;
        lastToWaypt = -999;

        obsIntStr  = "";
        toWayptStr = "";
        fmWayptStr = "";
        dmeDistStr = "";
        dmeTimeStr = "";
        gpsHdgStr  = "";
        gpsHmsStr  = "";
        gpsKtsStr  = "";

        setAmbient (false);
    }

    /**
     * Use grayscale for ambient mode.
     */
    public void setAmbient (boolean ambient)
    {
        this.ambient = ambient;
        if (ambient) {
            adfNeedlePaint.setColor (Color.WHITE);
            airplanePaint.setColor (Color.GRAY);
            dirArrowPaint.setColor (Color.GRAY);
            dmeDistPaint.setColor (Color.LTGRAY);
            dmeTimePaint.setColor (Color.LTGRAY);
            gpsHdgPaint.setColor (Color.GRAY);
            gpsMinPaint.setColor (Color.GRAY);
            obsArrowPaint.setColor (Color.LTGRAY);
            obsIntPaint.setColor (Color.LTGRAY);
            outerRingPaint.setColor (redRing ? Color.LTGRAY : Color.GRAY);
            dialBackPaint.setColor (Color.BLACK);
        } else {
            adfNeedlePaint.setColor (Color.GREEN);
            airplanePaint.setColor (Color.RED);
            dirArrowPaint.setColor (Color.GREEN);
            dmeDistPaint.setColor (0xFFFFAA00);
            dmeTimePaint.setColor (0xFFFFAA00);
            gpsHdgPaint.setColor (Color.RED);
            gpsMinPaint.setColor (Color.RED);
            obsArrowPaint.setColor (Color.YELLOW);
            obsIntPaint.setColor (Color.YELLOW);
            outerRingPaint.setColor (redRing ? Color.RED : Color.GRAY);
            dialBackPaint.setColor (Color.DKGRAY);
        }
    }

    /**
     * Set operating mode.
     */
    public void setMode (Mode m)
    {
        mode = m;
        invalidate ();
    }

    public Mode getMode ()
    {
        return mode;
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
     * Set which heading we are actually going, relative to OBS.
     * Tells where to put the airplane icon.
     */
    public void setHeading (double h)
    {
        heading = h;
        invalidate ();
    }

    public double getHeading ()
    {
        return heading;
    }

    /**
     * Whether or not to show airplane indicating aircraft heading.
     */
    public void showAirplane (boolean sa)
    {
        showarpln = sa;
        invalidate ();
    }

    /**
     * Set OBS setting.
     * @param obs = degrees
     */
    public void setObs (double obs)
    {
        while (obs < -180.0) obs += 360.0;
        while (obs >= 180.0) obs -= 360.0;
        obsSetting = obs;
        int obsint = (int) Math.round (obs);
        if (lastObsInt != obsint) {
            lastObsInt = obsint;
            if (obsint <= 0) obsint += 360;
            strbuf[0] = (char) ((obsint / 100) + '0');
            strbuf[1] = (char) ((obsint / 10 % 10) + '0');
            strbuf[2] = (char) ((obsint % 10) + '0');
            strbuf[3] = '\u00B0';
            obsIntStr = new String (strbuf, 0, 4);
        }
        invalidate ();
    }

    /**
     * Get OBS setting.
     * @return value in range -180..179.999999
     */
    public double getObs ()
    {
        return obsSetting;
    }

    /**
     * Set 'to waypt' heading, ie, what would center the needle.
     */
    public void setToWaypt (double towp)
    {
        int towaypt = (int) Math.round (towp);
        while (towaypt <=  0) towaypt += 360;
        while (towaypt > 360) towaypt -= 360;
        if (lastToWaypt != towaypt) {
            lastToWaypt = towaypt;
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
            invalidate ();
        }
    }

    /**
     * Set the displayed DME distance and tine.
     */
    public void setDme (double distnm, int timesec, boolean slant)
    {
        int d10 = (int) Math.round (distnm * 10.0);
        if (d10 > 9999) d10 = 10 * (int) Math.round (distnm);
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
            invalidate ();
        }

        if (timesec >= 3600*100) timesec = -1;
        if (lastdmetime != timesec) {
            lastdmetime = timesec;
            if (timesec < 0) {
                dmeTimeStr = "--:--:--";
            } else {
                strbuf[0] = (char) (timesec / 36000 + '0');
                strbuf[1] = (char) (timesec / 3600 % 10 + '0');
                strbuf[2] = ':';
                strbuf[3] = (char) (timesec / 600 % 6 + '0');
                strbuf[4] = (char) (timesec / 60 % 10 + '0');
                strbuf[5] = ':';
                strbuf[6] = (char) (timesec / 10 % 6 + '0');
                strbuf[7] = (char) (timesec % 10 + '0');
                dmeTimeStr = new String (strbuf, 0, 8);
            }
            invalidate ();
        }

        dmeDistPaint.setTextSkewX (slant ? -0.25F : 0.0F);
    }

    /**
     * Set GPS info to be displayed.
     */
    public void setGpsInfo (GpsLocation gl)
    {
        int gpshdg = (int) Math.round (gl.truecourse + gl.magvar);
        while (gpshdg <=  0) gpshdg += 360;
        while (gpshdg > 360) gpshdg -= 360;
        if (lastGpsHdg != gpshdg) {
            lastGpsHdg = gpshdg;
            strbuf[0] = (char) ((gpshdg / 100) + '0');
            strbuf[1] = (char) ((gpshdg / 10 % 10) + '0');
            strbuf[2] = (char) ((gpshdg % 10) + '0');
            strbuf[3] = '\u00B0';
            gpsHdgStr = new String (strbuf, 0, 4);
            invalidate ();
        }

        int gpshms = (int) (gl.time / 1000 % 86400);
        if (lastGpsHms != gpshms) {
            lastGpsHms = gpshms;
            strbuf[0] = (char) (gpshms / 36000 + '0');
            strbuf[1] = (char) (gpshms / 3600 % 10 + '0');
            strbuf[2] = ':';
            strbuf[3] = (char) (gpshms / 600 % 6 + '0');
            strbuf[4] = (char) (gpshms / 60 % 10 + '0');
            strbuf[5] = ':';
            strbuf[6] = (char) (gpshms / 10 % 6 + '0');
            strbuf[7] = (char) (gpshms % 10 + '0');
            gpsHmsStr = new String (strbuf, 0, 8);
            invalidate ();
        }

        int gpskts = (int) Math.round (gl.speed * Lib.KtPerMPS);
        if (lastGpsKts != gpskts) {
            lastGpsKts = gpskts;
            strbuf[6] = 'k';
            strbuf[7] = 't';
            int i = Lib.formatDigits (strbuf, 6, 3, gpskts);
            gpsKtsStr = new String (strbuf, i, 8 - i);
            invalidate ();
        }
    }

    /**
     * Draw red ring or not indicating loss of GPS signal.
     */
    public void drawRedRing (boolean drr)
    {
        redRing = drr;
        outerRingPaint.setColor (drr ? (ambient ? Color.LTGRAY : Color.RED) : Color.GRAY);
        invalidate ();
    }

    /**
     * Touch for turning the dial.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent (MotionEvent event)
    {
        switch (event.getActionMasked ()) {
            case MotionEvent.ACTION_DOWN: {
                touchDownX = event.getX ();
                touchDownY = event.getY ();
                touchDownOBS = obsSetting;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if ((mode == Mode.OFF) || (mode == Mode.GCT) ||
                        (mode == Mode.VOR) || ((mode == Mode.ADF))) {
                    double moveX    = event.getX ();
                    double moveY    = event.getY ();
                    double centerX  = getWidth ()  / 2.0;
                    double centerY  = getHeight () / 2.0;
                    double startHdg = Math.atan2 (touchDownX - centerX, touchDownY - centerY);
                    double moveHdg  = Math.atan2 (moveX - centerX, moveY - centerY);

                    // compute how many degrees finger moved since ACTION_DOWN
                    double degsMoved = Math.toDegrees (moveHdg - startHdg);
                    while (degsMoved < -180) degsMoved += 360;
                    while (degsMoved >= 180) degsMoved -= 360;

                    // compute how many degrees to turn dial based on that
                    degsMoved /= DIALRATIO;

                    // backward when doing HSI mode
                    if (revRotate) degsMoved = - degsMoved;

                    // update the dial to the new obs
                    setObs (touchDownOBS + degsMoved);

                    // if turned a long way, pretend we just did a new finger down
                    // ...this lets us go round and round
                    if (Math.abs (degsMoved) > 90 / DIALRATIO) {
                        touchDownX = moveX;
                        touchDownY = moveY;
                        touchDownOBS = obsSetting;
                    }

                    // tell listener of new obs
                    if (obsChangedListener != null) {
                        obsChangedListener.obsChanged (obsSetting);
                    }
                }
                break;
            }
        }
        return true;
    }

    /**
     * Draw the nav widget.
     */
    @Override
    public void onDraw (Canvas canvas)
    {
        float lastWidth  = getWidth ();
        float lastHeight = getHeight ();
        float lastScale  = Math.min (lastWidth, lastHeight) / (1000 * 2 + outerRingPaint.getStrokeWidth ());

        canvas.save ();

        // set up translation/scaling so that outer ring is radius 1000 centered at 0,0
        canvas.translate (lastWidth / 2, lastHeight / 2);
        canvas.scale (lastScale, lastScale);

        // draw outer ring
        canvas.drawCircle (0, 0, 1000, outerRingPaint);

        // draw OBS arrow triangle
        if (mode != Mode.OFF) {
            canvas.drawPath (obsArrowPath, obsArrowPaint);
        }

        // GCT/VOR/LOC-style deflection dots and needle
        if ((mode == Mode.GCT) || (mode == Mode.VOR) || (mode == Mode.LOC) ||
                (mode == Mode.LOCBC) || (mode == Mode.ILS)) {

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
        }

        // draw glideslope needle
        if (mode == Mode.ILS) {
            double needleCentY = slope * -412;
            double needleLeftX = -412 * 1.2;
            double needleRiteX =  412 * 1.2;
            canvas.drawLine ((float) needleLeftX, (float) needleCentY, (float) needleRiteX, (float) needleCentY, vorNeedlePaint);
        }

        // ADF-style needle
        if (mode == Mode.ADF) {
            canvas.rotate ((float) deflect);
            canvas.drawPath (adfNeedlePath, adfNeedlePaint);
            canvas.rotate ((float) - deflect);
        }

        // cover up end of VOR-style needle in case it goes under dial
        canvas.drawCircle (0, 0, 718, dialBackPaint);

        if (mode != Mode.OFF) {

            // draw texts
            canvas.drawText (obsIntStr, -55, -390, obsIntPaint);
            canvas.drawText (toWayptStr, -55, -245, fmtoWayptPaint);
            canvas.drawText (fmWayptStr, -55, -100, fmtoWayptPaint);
            canvas.drawText (dmeDistStr, -55 + dmeDistPaint.getTextSize () * dmeDistPaint.getTextSkewX (), 190, dmeDistPaint);
            canvas.drawText (dmeTimeStr, -55, 330, dmeTimePaint);
            canvas.drawText (gpsHdgStr.equals ("") ? "" : showarpln ? gpsHdgStr : "---\u00B0", 55, -390, gpsHdgPaint);
            canvas.drawText (gpsHmsStr, 55, -250, gpsMinPaint);
            canvas.drawText (gpsKtsStr, 55, -110, gpsMinPaint);

            // draw OBS dial
            canvas.rotate ((float) - obsSetting);
            for (int deg = 0; deg < 360; deg += 5) {
                switch ((deg / 5) % 6) {
                    case 0: {
                        // number and a thick line
                        canvas.drawText (Integer.toString (deg), 0, -823, dialTextPaint);
                        canvas.drawLine (0, -647, 0, -788, dialFatPaint);
                        break;
                    }
                    case 2:
                    case 4: {
                        // a medium line
                        canvas.drawLine (0, -647, 0, -788, dialMidPaint);
                        break;
                    }
                    case 1:
                    case 3:
                    case 5: {
                        // a small thin line
                        canvas.drawLine (0, -647, 0, -718, dialThinPaint);
                        break;
                    }
                }
                canvas.rotate (5.0F);
            }

            // heading airplane
            if (showarpln) {
                canvas.rotate ((float) (heading + obsSetting));
                canvas.translate (0, -805);
                float scale = 180.0F / airplaneHeight;
                canvas.scale (scale, scale);
                canvas.drawPath (airplanePath, airplanePaint);
            }
        }

        canvas.restore ();
    }
}
