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
class MapTriangle extends View {
    private MainActivity mainActivity;
    private Paint bgpaint;
    private Path bgrect;

    public MapTriangle (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        construct ();
    }

    public MapTriangle (Context ctx)
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

        bgpaint = new Paint ();
        bgpaint.setStrokeWidth (2.0F);
        bgpaint.setStyle (Paint.Style.STROKE);
    }

    @Override
    public void onDraw (Canvas canvas)
    {
        super.onDraw (canvas);

        int w = getWidth ();
        int h = getHeight ();

        if (bgrect == null) {
            bgrect = new Path ();
            bgrect.moveTo (1, h / 2.0F);
            bgrect.lineTo (w - 1, 0);
            bgrect.lineTo (w - 1, h - 1);
            bgrect.lineTo (1, h / 2.0F);
        }

        boolean ambient = (mainActivity != null) && mainActivity.ambient;
        bgpaint.setColor (ambient ? Color.LTGRAY : Color.YELLOW);
        canvas.drawPath (bgrect, bgpaint);
    }
}
