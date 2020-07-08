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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// applies a variable rotation to child elements
public class RotateLayout extends FrameLayout {
    private float cosrotation;
    private float degrees;
    private float halfheight;
    private float halfwidth;
    private float sinrotation;

    public RotateLayout (@NonNull Context context)
    {
        super (context);
        cosrotation = 1.0F;
    }

    public RotateLayout (@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super (context, attrs);
        cosrotation = 1.0F;
    }

    // rotate all children by the given number of degrees around the center
    public void setRotation (float degs)
    {
        degrees = degs;
        cosrotation = (float) Math.cos (Math.toRadians (degs));
        sinrotation = (float) Math.sin (Math.toRadians (degs));
        invalidate ();
    }

    // rotate the touch event so the menu and ident input boxes work
    @Override
    public boolean dispatchTouchEvent (MotionEvent event)
    {
        MotionEvent rotatedEvent = rotateMotionEvent (event);
        boolean rc = super.dispatchTouchEvent (rotatedEvent);
        rotatedEvent.recycle ();
        return rc;
    }

    // draw the children with the rotation applied
    @Override
    public void dispatchDraw (Canvas canvas)
    {
        halfwidth  = canvas.getWidth ()  / 2.0F;
        halfheight = canvas.getHeight () / 2.0F;
        canvas.save ();
        try {
            canvas.rotate (degrees, halfwidth, halfheight);
            super.dispatchDraw (canvas);
        } finally {
            canvas.restore ();
        }
    }

    // rotate the given motion event
    private MotionEvent rotateMotionEvent (MotionEvent event)
    {
        int pointerCount = event.getPointerCount ();

        MotionEvent.PointerProperties[] pointerPropertiesArray = new MotionEvent.PointerProperties[pointerCount];
        for (int i = 0; i < pointerCount; i ++) {
            MotionEvent.PointerProperties pps = new MotionEvent.PointerProperties ();
            event.getPointerProperties (i, pps);
            pointerPropertiesArray[i] = pps;
        }

        MotionEvent.PointerCoords[] pointerCoordsArray = new MotionEvent.PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; i ++) {
            MotionEvent.PointerCoords pcs = new MotionEvent.PointerCoords ();
            event.getPointerCoords (i, pcs);
            float oldx = pcs.x - halfwidth;
            float oldy = pcs.y - halfheight;
            pcs.x = oldx * cosrotation + oldy * sinrotation + halfwidth;
            pcs.y = oldy * cosrotation - oldx * sinrotation + halfheight;
            pointerCoordsArray[i] = pcs;
        }

        return MotionEvent.obtain (
                event.getDownTime (),
                event.getEventTime (),
                event.getAction (),
                pointerCount,
                pointerPropertiesArray,
                pointerCoordsArray,
                event.getMetaState (),
                event.getButtonState (),
                event.getXPrecision (),
                event.getYPrecision (),
                event.getDeviceId (),
                event.getEdgeFlags (),
                event.getSource (),
                event.getFlags ()
        );
    }
}
