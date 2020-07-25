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
import android.view.View;

/**
 * Moving Map page Back button.
 * Just draws a shape.
 */
@SuppressLint("AppCompatCustomView")
public class MapZoomButton extends View {
    private char ch;
    private MainActivity mainActivity;
    private Paint paint;
    private Path path;

    public MapZoomButton (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        construct ();
    }

    public MapZoomButton (Context ctx)
    {
        super (ctx);
        construct ();
    }

    private void construct ()
    {
        Context ctx = getContext ();
        if (ctx instanceof MainActivity) {
            mainActivity = (MainActivity) ctx;
        }

        paint = new Paint ();
        paint.setColor (Color.YELLOW);
        paint.setStrokeWidth (2.0F);
        paint.setStyle (Paint.Style.STROKE);
    }

    public void setAmbient ()
    {
        boolean ambient = (mainActivity != null) && mainActivity.ambient;
        setVisibility (ambient ? View.INVISIBLE : View.VISIBLE);
    }

    public void setChar (char c)
    {
        ch = c;
        path = null;
    }

    @Override
    public void onDraw (Canvas canvas)
    {
        int w = getWidth ();
        int h = getHeight ();

        if (path == null) {
            path = new Path ();
            switch (ch) {
                case '-': {
                    path.moveTo (w * 0.25F, h * 0.425F);
                    path.lineTo (w * 0.75F, h * 0.425F);
                    path.lineTo (w * 0.75F, h * 0.575F);
                    path.lineTo (w * 0.25F, h * 0.575F);
                    path.lineTo (w * 0.25F, h * 0.425F);
                    break;
                }
                case '+': {
                    path.moveTo (w * 0.250F, h * 0.425F);
                    path.lineTo (w * 0.425F, h * 0.425F);
                    path.lineTo (w * 0.425F, h * 0.250F);
                    path.lineTo (w * 0.575F, h * 0.250F);
                    path.lineTo (w * 0.575F, h * 0.425F);
                    path.lineTo (w * 0.750F, h * 0.425F);
                    path.lineTo (w * 0.750F, h * 0.575F);
                    path.lineTo (w * 0.575F, h * 0.575F);
                    path.lineTo (w * 0.575F, h * 0.750F);
                    path.lineTo (w * 0.425F, h * 0.750F);
                    path.lineTo (w * 0.425F, h * 0.575F);
                    path.lineTo (w * 0.250F, h * 0.575F);
                    path.lineTo (w * 0.250F, h * 0.425F);
                    break;
                }
            }
        }

        canvas.drawCircle (w / 2.0F, h / 2.0F, Math.min (h, w) / 2.5F, paint);
        canvas.drawPath (path, paint);
    }
}
