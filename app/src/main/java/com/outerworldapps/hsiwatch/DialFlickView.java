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
 * Detect flick from center outward.
 * Change page as given by onFlick...() callbacks.
 */
public abstract class DialFlickView extends View {
    private final static float irad = 320.0F / 512.0F;

    protected String goDownString;
    protected String goLeftString;
    protected String goRightString;
    protected String goUpString;

    protected abstract boolean onTouchOutside (MotionEvent event);
    protected abstract View getDownView ();
    protected abstract View getLeftView ();
    protected abstract View getRightView ();
    protected abstract View getUpView ();

    private enum Dir { N, L, R, U, D }

    private boolean downOutside;
    private Dir dragFromDir;
    private float displacedX;
    private float displacedY;
    private float downInsideX;
    private float downInsideY;
    private float height;
    private float width;
    private long downInsideT;
    private MainActivity mainActivity;
    private Paint arrowPaint;
    private Paint circlePaint;
    private Paint textPaint;
    private Path arrows;
    private Path draggedClipPath;
    private View displacedView;

    protected DialFlickView (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        constructor ();
    }

    protected DialFlickView (Context ctx)
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

        dragFromDir = Dir.N;

        arrowPaint = new Paint ();
        arrowPaint.setColor (Color.BLUE);
        arrowPaint.setStrokeWidth (3.0F);
        arrowPaint.setStyle (Paint.Style.STROKE);

        circlePaint = new Paint ();
        circlePaint.setColor (Color.LTGRAY);

        textPaint = new Paint ();
        textPaint.setColor (Color.BLUE);
        textPaint.setTextAlign (Paint.Align.CENTER);
        textPaint.setTextSize (80);

        arrows = new Path ();
        arrows.moveTo (-100, -220);
        arrows.lineTo (100, -220);
        arrows.lineTo (0, -170);
        arrows.lineTo (-100, -220);
        arrows.moveTo (-100, -160);
        arrows.lineTo (100, -160);
        arrows.lineTo (0, -110);
        arrows.lineTo (-100, -160);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event)
    {
        if (downOutside) {
            downOutside = (event.getAction () != MotionEvent.ACTION_UP);
            return onTouchOutside (event);
        }
        long  t = System.currentTimeMillis ();
        float x = event.getX () * 2 / width  - 1;   // -1..1
        float y = event.getY () * 2 / height - 1;   // -1..1
        switch (event.getAction ()) {

            // if just started touch, remember where and when they touched
            case MotionEvent.ACTION_DOWN: {
                float rsq = x * x + y * y;
                if (rsq > irad * irad) {
                    downOutside = true;
                    return onTouchOutside (event);
                }
                downInsideT = t;
                downInsideX = x;
                downInsideY = y;
                invalidate ();
                break;
            }

            // if moving around, get which view they are accessing
            // ...and set it up for partial viewing
            case MotionEvent.ACTION_MOVE: {
                float dx = x - downInsideX;
                float dy = y - downInsideY;
                float dxsq = dx * dx;
                float dysq = dy * dy;
                Dir newdir = Dir.N;
                if (dxsq + dysq > 0.25F * 0.25F * irad * irad) {
                    if (dxsq > dysq) {
                        if (dx < 0) {
                            if (goRightString != null) {
                                newdir = Dir.R;
                                displacedX = dx * width / 2 + height;
                            }
                        } else {
                            if (goLeftString != null) {
                                newdir = Dir.L;
                                displacedX = dx * width / 2 - height;
                            }
                        }
                        displacedY = 0;
                    } else {
                        displacedX = 0;
                        if (dy < 0) {
                            if (goDownString != null) {
                                newdir = Dir.D;
                                displacedY = dy * height / 2 + height;
                            }
                        } else {
                            if (goUpString != null) {
                                newdir = Dir.U;
                                displacedY = dy * height / 2 - height;
                            }
                        }
                    }
                }

                // if different direction than last time, get which view is being dragged
                // make sure it has been laid out or we will be dragging blanks in onDraw()
                if (dragFromDir != newdir) {
                    dragFromDir = newdir;
                    switch (newdir) {
                        case N: displacedView = null; break;
                        case L: displacedView = getLeftView (); break;
                        case R: displacedView = getRightView (); break;
                        case U: displacedView = getUpView (); break;
                        case D: displacedView = getDownView (); break;
                    }
                    if ((displacedView != null) && ! displacedView.isLaidOut ()) {
                        //displacedView.requestLayout ();
                        displacedView.measure (0, 0);
                        displacedView.layout (0, 0, (int) width, (int) height);
                    }
                }

                // dragFromDir = N: nothing being dragged
                //            else: something being dragged
                //                  displacedView = null: dragging the back view
                //                                  else: view being dragged

                invalidate ();
                break;
            }

            // releasing, if moved at least half way across, display the page
            case MotionEvent.ACTION_UP: {
                float dx = x - downInsideX;
                float dy = y - downInsideY;
                float dxsq = dx * dx;
                float dysq = dy * dy;
                if ((dragFromDir != Dir.N) && (dxsq + dysq > 0.50F * 0.50F * irad * irad)) {
                    if (displacedView == null) {
                        mainActivity.onBackPressed ();
                    } else {
                        mainActivity.showMainPage (displacedView);
                    }
                }
                downInsideT = 0;
                dragFromDir = Dir.N;
                displacedView = null;
                invalidate ();
                break;
            }
        }
        return true;
    }

    @Override
    public void onDraw (Canvas canvas)
    {
        width  = getWidth  ();
        height = getHeight ();

        if (downInsideT > 0) {
            canvas.save ();
            try {
                // set up so drawing is centered at 0,0
                // and whole dial has radius 512
                canvas.translate (width / 2, height / 2);
                canvas.scale (width / 1024, height / 1024);

                // confine our stuff to radius 320
                canvas.drawCircle (0, 0, 320, circlePaint);
                canvas.drawCircle (0, 0, 319, arrowPaint);

                drawArrows (canvas, 180, goDownString);
                drawArrows (canvas, 270, goLeftString);
                drawArrows (canvas,  90, goRightString);
                drawArrows (canvas,   0, goUpString);
            } finally {
                canvas.restore ();
            }
        }

        // drag selected view partially on top of this one
        if (dragFromDir != Dir.N) {
            View dv = displacedView;
            if (dv == null) dv = mainActivity.peekBackView ();
            if (dv != null) {
                canvas.save ();
                try {
                    canvas.translate (displacedX, displacedY);
                    if (mainActivity.isScreenRound) {
                        if (draggedClipPath == null) {
                            draggedClipPath = new Path ();
                            draggedClipPath.addCircle (width / 2, height / 2,
                                    Math.min (width, height) / 2, Path.Direction.CCW);
                        }
                        canvas.clipPath (draggedClipPath);
                    }
                    dv.draw (canvas);
                } finally {
                    canvas.restore ();
                }
            }
        }
    }

    private void drawArrows (Canvas canvas, int newrot, String text)
    {
        if (text == null) return;

        canvas.rotate (newrot);
        canvas.drawPath (arrows, arrowPaint);
        switch (newrot) {
            // going up
            case   0: {
                canvas.drawText (text, 0, -240, textPaint);
                break;
            }
            // going right
            case  90: {
                canvas.rotate (270);
                drawVertText (canvas, text, 265, textPaint);
                break;
            }
            // going down
            case 180: {
                canvas.rotate (180);
                canvas.drawText (text, 0, 240+40, textPaint);
                break;
            }
            // going left
            case 270: {
                canvas.rotate (90);
                drawVertText (canvas, text, -265, textPaint);
                break;
            }
        }
    }

    private static void drawVertText (Canvas canvas, String text, float x, Paint paint)
    {
        int len = text.length ();
        for (int i = 0; i < len; i ++) {
            float y = (i - len / 2.0F) * 70 + 55;
            canvas.drawText (text, i, i + 1, x, y, paint);
        }
    }
}
