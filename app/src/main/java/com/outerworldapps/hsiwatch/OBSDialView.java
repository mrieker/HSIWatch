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
import android.view.MotionEvent;
import android.view.View;

/**
 * The VOR-like nav dial, needles, etc.
 */
public abstract class OBSDialView extends DialFlickView {

    private final static double DIALRATIO = 5.0;
    private final static float SIMPLESCALE = 1.25F;

    protected final static int INNARDSRADIUS = 630;
    protected abstract double getDispMagVar ();
    protected abstract void onDrawInnards (Canvas canvas, double trueup, double scale);

    private boolean firstTime;
    private boolean lastAmbient;
    private boolean lastFillChin;
    private boolean lastRedRing;
    private double touchDownOBS;
    private double touchDownX;
    private double touchDownY;
    private float chin_x;
    private float chin_y;
    private MainActivity mainActivity;
    private Paint adfNeedlePaint;
    private Paint dialBackPaint;
    private Paint dialTextPaint;
    private Paint dirArrowPaint;
    private Paint dmeDistPaint;
    private Paint dmeTimePaint;
    private Paint gpsHdgPaint;
    private Paint gpsMinPaint;
    private Paint modePaint;
    private Paint obsArrowPaint;
    private Paint obsBackPaint;
    private Paint obsIntPaint;
    private Paint timeDotPaint;
    private Paint outerRingPaint;
    private Path circleClipPath;
    private Path obsArrowPath;

    public OBSDialView (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        constructor ();
    }

    public OBSDialView (Context ctx)
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

        gpsHdgPaint = new Paint ();
        gpsHdgPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        gpsHdgPaint.setTextAlign (Paint.Align.LEFT);
        gpsHdgPaint.setTextSize (140);

        gpsMinPaint = new Paint ();
        gpsMinPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        gpsMinPaint.setTextAlign (Paint.Align.LEFT);
        gpsMinPaint.setTextSize (130);

        modePaint = new Paint ();
        modePaint.setStyle (Paint.Style.FILL_AND_STROKE);
        modePaint.setTextAlign (Paint.Align.LEFT);
        modePaint.setTextSize (110);

        obsArrowPaint = new Paint ();
        obsArrowPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        obsBackPaint = new Paint ();
        obsBackPaint.setColor (Color.BLACK);
        obsBackPaint.setStyle (Paint.Style.STROKE);
        obsBackPaint.setStrokeWidth (175);

        obsIntPaint = new Paint ();
        obsIntPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        obsIntPaint.setTextAlign (Paint.Align.RIGHT);
        obsIntPaint.setTextSize (140);

        outerRingPaint = new Paint ();
        outerRingPaint.setColor (Color.DKGRAY);
        outerRingPaint.setStrokeWidth (50);
        outerRingPaint.setStyle (Paint.Style.STROKE);

        timeDotPaint = new Paint ();
        timeDotPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        obsArrowPath = new Path ();
        obsArrowPath.moveTo (-71, -518);
        obsArrowPath.lineTo (  0, -624);
        obsArrowPath.lineTo ( 71, -518);

        circleClipPath = new Path ();
        circleClipPath.addCircle (0, 0, 1000 - 50, Path.Direction.CCW);

        firstTime = true;
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
     * Touch for turning the dial.
     */
    @Override
    public boolean onTouchOutside (MotionEvent event)
    {
        switch (event.getActionMasked ()) {
            case MotionEvent.ACTION_DOWN: {
                touchDownX = event.getX ();
                touchDownY = event.getY ();
                touchDownOBS = mainActivity.obsSetting;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                NavDialView.Mode mode = mainActivity.navModeButton.getMode ();
                if ((mode == NavDialView.Mode.OFF) || (mode == NavDialView.Mode.GCT) ||
                        (mode == NavDialView.Mode.VOR) || ((mode == NavDialView.Mode.ADF))) {
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
                    if (mainActivity.menuMainPage.hsiModeCkBox.isChecked ()) {
                        degsMoved = - degsMoved;
                    }

                    // update the dial to the new obs
                    double obs = touchDownOBS + degsMoved;
                    while (obs < -180.0) obs += 360.0;
                    while (obs >= 180.0) obs -= 360.0;
                    mainActivity.obsSetting = obs;

                    // if turned a long way, pretend we just did a new finger down
                    // ...this lets us go round and round
                    if (Math.abs (degsMoved) > 90 / DIALRATIO) {
                        touchDownX = moveX;
                        touchDownY = moveY;
                        touchDownOBS = mainActivity.obsSetting;
                    }

                    // tell main activity that obs has changed
                    mainActivity.obsChanged ();
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
        boolean ambient = (mainActivity != null) && mainActivity.ambient;
        boolean redRing = (mainActivity != null) && mainActivity.redRingOn;
        if (lastAmbient ^ ambient | lastRedRing ^ redRing | firstTime) {
            lastAmbient = ambient;
            lastRedRing = redRing;
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
                outerRingPaint.setColor (redRing ? Color.GRAY : Color.DKGRAY);
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
                outerRingPaint.setColor (redRing ? Color.RED : Color.DKGRAY);
            }
        }

        // if we are filling a chin, get the height of the chin
        boolean fillChin = (mainActivity != null) && mainActivity.menuMainPage.fillChinCkBox.isChecked ();
        if ((fillChin ^ lastFillChin) | firstTime) {
            lastFillChin = fillChin;
            chin_x = 0.0F;
            chin_y = 1000.0F;
            if (fillChin) {
                int displayWidth  = mainActivity.widthPixels;
                int displayHeight = mainActivity.heightPixels;
                float scaledWidth = 1000 * 2 + outerRingPaint.getStrokeWidth ();
                float scaledHeight = displayHeight * scaledWidth / displayWidth;
                chin_y = scaledHeight - 1000.0F - outerRingPaint.getStrokeWidth ();
                chin_x = (float) Math.sqrt (1000.0 * 1000.0 - chin_y * chin_y);
            }
        }

        firstTime = false;

        canvas.save ();
        try {

            // set up translation/scaling so that outer ring is radius 1000 centered at 0,0
            float width = getWidth ();
            float height = getHeight ();
            float scale = Math.min (width, height) / (1000 * 2 + outerRingPaint.getStrokeWidth ());
            canvas.translate (width / 2, height / 2);
            canvas.scale (scale, scale);

            // draw outer ring - maybe it is flashing red
            canvas.drawCircle (0, 0, 1000, outerRingPaint);

            // maybe draw time dots on outer ring
            if ((mainActivity != null) && mainActivity.menuMainPage.timeDotsCkBox.isChecked ()) {

                // if fill chin mode, see where outer ring is chopped off
                // it's at approximately 5o'clock to 7o'clock
                if (mainActivity.menuMainPage.fillChinCkBox.isChecked ()) {

                    // draw flattened outer ring
                    // chin_x,_y = flattened endpoints of center of outer ring line
                    canvas.drawLine (chin_x, chin_y, - chin_x, chin_y, outerRingPaint);
                }

                // draw minute dots
                // solid grayscale if normal, open grayscale if ambient
                timeDotPaint.setStyle (lastAmbient ? Paint.Style.STROKE : Paint.Style.FILL_AND_STROKE);
                for (int i = 0; i < 60; i ++) {
                    if (i == 0) {
                        drawTimeDot (canvas, 25, 0.0, Color.WHITE);
                        continue;
                    }
                    if (i % 15 == 0) {
                        drawTimeDot (canvas, 25, i / 60.0, Color.LTGRAY);
                        continue;
                    }
                    if (i % 5 == 0) {
                        drawTimeDot (canvas, 25, i / 60.0, Color.GRAY);
                        continue;
                    }
                    drawTimeDot (canvas, 15, i / 60.0, Color.BLACK);
                }

                // draw current time dots
                // colored if normal, grayscale if ambient
                timeDotPaint.setStyle (Paint.Style.FILL_AND_STROKE);
                long time = mainActivity.curLoc.time;
                double gps12hr = (time % 43200000L) / 43200000.0;
                double gpshour = (time %  3600000L) /  3600000.0;
                double gpsmin  = (time %    60000L) /    60000.0;
                drawTimeDot (canvas, 25, gps12hr, lastAmbient ? Color.WHITE  : Color.RED);
                drawTimeDot (canvas, 25, gpshour, lastAmbient ? Color.LTGRAY : Color.GREEN);
                drawTimeDot (canvas, 25, gpsmin,  lastAmbient ? Color.GRAY   : Color.CYAN);

                // don't overdraw the line and dots just above the chin
                canvas.clipRect (-1000.0F, -1000.0F, 1000.0F, chin_y - outerRingPaint.getStrokeWidth () / 2.0F);
            }

            // see how much to rotate from true north
            // hsi: airplane at top; else: yellow triangle at top
            double magvar = getDispMagVar ();
            double trueup;
            if (mainActivity.menuMainPage.hsiModeCkBox.isChecked ()) {
                trueup = mainActivity.latesttc;
            } else {
                trueup = mainActivity.obsSetting - magvar;
            }
            canvas.rotate ((float) -trueup);

            // draw innards assuming true north is up
            canvas.save ();
            try {
                canvas.clipPath (circleClipPath);
                canvas.scale (SIMPLESCALE, SIMPLESCALE);
                scale *= SIMPLESCALE;
                onDrawInnards (canvas, trueup, scale);
            } finally {
                canvas.restore ();
            }

            // everything below drawn assuming magnetic north is up
            canvas.rotate ((float) -magvar);

            // draw OBS dial with "0" at top
            canvas.drawCircle (0, 0, 890, obsBackPaint);
            for (int deg = 0; deg < 360; deg += 30) {
                canvas.drawText (Integer.toString (deg), 0, -823, dialTextPaint);
                canvas.rotate (30);
            }

            // draw obs triangle
            canvas.save ();
            try {
                canvas.scale (SIMPLESCALE, SIMPLESCALE);
                canvas.rotate ((float) mainActivity.obsSetting);
                canvas.drawPath (obsArrowPath, obsArrowPaint);
            } finally {
                canvas.restore ();
            }

            // draw ground track airplane
            if (mainActivity.curLoc.speed > MainActivity.gpsMinSpeedMPS) {
                canvas.rotate ((float) (mainActivity.latesttc + magvar));
                canvas.translate (0, -805);
                float sf = 180.0F / MainActivity.airplaneHeight;
                canvas.scale (sf, sf);
                canvas.drawPath (mainActivity.airplanePath, mainActivity.airplanePaint);
            }

        } finally {
            canvas.restore ();
        }

        // maybe draw swipe menu
        super.onDraw (canvas);
    }

    // draw time dot on top of the outer ring
    //  input:
    //   canvas = what to draw dot on
    //   where  = 0: top; 0.5: bottom; 0.25: right side; 0.75: left side
    //   color  = color for the dot
    private void drawTimeDot (Canvas canvas, float radius, double where, int color)
    {
        timeDotPaint.setColor (color);
        float cx =  1000.0F * (float) Math.sin (where * 2.0 * Math.PI);
        float cy = -1000.0F * (float) Math.cos (where * 2.0 * Math.PI);
        if (cy > chin_y) {
            cx *= chin_y / cy;
            cy  = chin_y;
        }
        canvas.drawCircle (cx, cy, radius, timeDotPaint);
    }
}
