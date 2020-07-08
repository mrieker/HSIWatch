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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Menu button.  Like a regular button excepts draws dot-dot-dot.
 */
@SuppressLint("AppCompatCustomView")
class MyMenuButton extends View {
    private Paint bgpaint;
    private Paint fgpaint;
    private Path bgrect;

    public MyMenuButton (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        construct ();
    }

    public MyMenuButton (Context ctx)
    {
        super (ctx);
        construct ();
    }

    private void construct ()
    {
        bgpaint = new Paint ();
        bgpaint.setColor (Color.GRAY);
        bgpaint.setStyle (Paint.Style.STROKE);

        fgpaint = new Paint ();
        fgpaint.setColor (Color.WHITE);
        fgpaint.setStyle (Paint.Style.FILL);
        fgpaint.setTextAlign (Paint.Align.CENTER);
        fgpaint.setTypeface (Typeface.MONOSPACE);
    }

    @Override
    public void onDraw (Canvas canvas)
    {
        super.onDraw (canvas);

        int w = getWidth ();
        int h = getHeight ();

        if (bgrect == null) {
            float r = Math.min (w, h) / 8.0F;
            bgrect = new Path ();
            bgrect.addRoundRect (0, 0, w - 1, h - 1, r, r, Path.Direction.CCW);
        }
        canvas.drawPath (bgrect, bgpaint);

        fgpaint.setTextSize (h / 5.0F);
        canvas.drawText ("M", w / 2.0F, h * 1.5F / 5.0F, fgpaint);
        canvas.drawText ("E", w / 2.0F, h * 2.5F / 5.0F, fgpaint);
        canvas.drawText ("N", w / 2.0F, h * 3.5F / 5.0F, fgpaint);
        canvas.drawText ("U", w / 2.0F, h * 4.5F / 5.0F, fgpaint);
    }
}
