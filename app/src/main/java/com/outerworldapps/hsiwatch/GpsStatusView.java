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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.util.AttributeSet;
import android.view.View;

/**
 * Display a GPS status panel.
 * Also acts as a compass.
 */
public class GpsStatusView
        extends View
        implements SensorEventListener {

    private final static boolean USECOMPASS = true;
    private final static String[] compDirs = new String[] { "N", "E", "S", "W" };

    private float compRotDeg;  // compass rotation
    private float[] geomag;
    private float[] gravity;
    private float[] orient = new float[3];
    private float[] rotmat = new float[9];
    private GnssStatus satellites;
    private Paint ignoredSpotsPaint = new Paint ();
    private Paint ringsPaint        = new Paint ();
    private Paint textPaint         = new Paint ();
    private Paint usedSpotsPaint    = new Paint ();
    private SensorManager instrSM;
    private String gpstimstr;

    public GpsStatusView (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        construct ();
    }

    public GpsStatusView (Context ctx)
    {
        super (ctx);
        construct ();
    }

    public void construct ()
    {
        ringsPaint.setColor (Color.YELLOW);
        ringsPaint.setStyle (Paint.Style.STROKE);
        ringsPaint.setStrokeWidth (2);

        usedSpotsPaint.setColor (Color.GREEN);
        usedSpotsPaint.setStyle (Paint.Style.FILL);

        ignoredSpotsPaint.setColor (Color.CYAN);
        ignoredSpotsPaint.setStyle (Paint.Style.STROKE);

        textPaint.setColor (Color.WHITE);
        textPaint.setStrokeWidth (3);
        textPaint.setTextAlign (Paint.Align.CENTER);
    }

    public void Startup ()
    {
        geomag     = null;
        gravity    = null;
        compRotDeg = Float.NaN;
        textPaint.setTextSize (24.0F);
        instrSM    = getContext ().getSystemService (SensorManager.class);
        gpstimstr  = "?";
        if (USECOMPASS) {
            Sensor smf = instrSM.getDefaultSensor (Sensor.TYPE_MAGNETIC_FIELD);
            Sensor sac = instrSM.getDefaultSensor (Sensor.TYPE_ACCELEROMETER);
            instrSM.registerListener (this, smf, SensorManager.SENSOR_DELAY_UI);
            instrSM.registerListener (this, sac, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void Shutdown ()
    {
        instrSM.unregisterListener (this);
    }

    /**
     * Got a compass reading.
     */
    @Override  // SensorEventListener
    public void onSensorChanged (SensorEvent event)
    {
        switch (event.sensor.getType ()) {
            case Sensor.TYPE_MAGNETIC_FIELD: {
                geomag = event.values;
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                gravity = event.values;
                break;
            }
        }

        if ((geomag != null) && (gravity != null)) {
            SensorManager.getRotationMatrix (rotmat, null, gravity, geomag);
            SensorManager.getOrientation (rotmat, orient);
            compRotDeg = (float) - Math.toDegrees (orient[0]);
            geomag  = null;
            gravity = null;
            invalidate ();
        }
    }

    @Override  // SensorEventListener
    public void onAccuracyChanged (Sensor sensor, int accuracy)
    { }

    /**
     * Got a GPS satellite status reading.
     */
    public void onStatusReceived (GnssStatus gnssStatus)
    {
        satellites = gnssStatus;
        invalidate ();
    }

    /**
     * Got a GPS location reading.
     */
    public void gotGpsTime (long gpstime)
    {
        int gpssec = (int) (gpstime / 1000 % 86400);
        gpstimstr = new String (new char[] {
                (char) (gpssec / 36000 + '0'),
                (char) (gpssec / 3600 % 10 + '0'),
                ':',
                (char) (gpssec / 600 % 6 + '0'),
                (char) (gpssec / 60 % 10 + '0'),
                ':',
                (char) (gpssec / 10 % 6 + '0'),
                (char) (gpssec % 10 + '0')
        });
        invalidate ();
    }

    /**
     * Callback to draw the instruments on the screen.
     */
    @Override
    protected void onDraw (Canvas canvas)
    {
        float screenHeight  = getHeight ();
        float textHeight    = textPaint.getTextSize ();
        float circleCenterX = getWidth ()  / 2.0F;
        float circleCenterY = (screenHeight - textHeight) / 2.0F;
        float circleRadius  = Math.min (circleCenterX, circleCenterY) - textHeight * 2.25F;

        canvas.save ();
        try {
            canvas.drawText (gpstimstr, circleCenterX, circleCenterY + circleRadius + textHeight * 1.75F, textPaint);

            if (! Float.isNaN (compRotDeg)) {
                String cmphdgstr = Integer.toString (1360 - (int) Math.round (compRotDeg + 360.0) % 360).substring (1) + '\u00B0';
                canvas.drawText (cmphdgstr, circleCenterX, textHeight, textPaint);
                canvas.rotate (compRotDeg, circleCenterX, circleCenterY);
            }

            for (String compDir : compDirs) {
                canvas.drawText (compDir, circleCenterX, circleCenterY - circleRadius, textPaint);
                canvas.rotate (90.0F, circleCenterX, circleCenterY);
            }

            if (satellites != null) {
                canvas.drawCircle (circleCenterX, circleCenterY, circleRadius * 30 / 90, ringsPaint);
                canvas.drawCircle (circleCenterX, circleCenterY, circleRadius * 60 / 90, ringsPaint);
            }
            canvas.drawCircle (circleCenterX, circleCenterY, circleRadius * 90 / 90, ringsPaint);

            if (satellites != null) {
                int n = satellites.getSatelliteCount ();
                for (int i = 0; i < n; i ++) {
                    // hasAlmanac() and hasEphemeris() seem to always return false
                    // getSnr() in range 0..30 approx
                    double size = satellites.getCn0DbHz (i) / 3;
                    double radius = (90 - satellites.getElevationDegrees (i)) * circleRadius / 90;
                    double azirad = Math.toRadians (satellites.getAzimuthDegrees (i));
                    double deltax = radius * Math.sin (azirad);
                    double deltay = radius * Math.cos (azirad);
                    Paint paint = satellites.usedInFix (i) ? usedSpotsPaint : ignoredSpotsPaint;
                    canvas.drawCircle ((float) (circleCenterX + deltax), (float) (circleCenterY - deltay), (float) size, paint);
                }
            }
        } finally {
            canvas.restore ();
        }
    }
}
