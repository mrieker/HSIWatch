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
import android.graphics.Rect;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.HashMap;

/**
 * Nav Mode button
 */
@SuppressLint("AppCompatCustomView")
public class NavModeButton extends View implements View.OnClickListener {

    private static Object modeMenuBold = new StyleSpan (android.graphics.Typeface.BOLD);
    private static Object modeMenuColor = new ForegroundColorSpan (Color.YELLOW);
    private static Object modeMenuSize = new RelativeSizeSpan (1.5F);

    private HashMap<NavDialView.Mode,RadioButton> navModeButtons;
    private MainActivity mainActivity;
    private Paint bgpaint;
    private RadioGroup modeGroup;
    private Rect bounds;
    private String letter;
    private View modePageView;

    public NavModeButton (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        construct (ctx);
    }

    public NavModeButton (Context ctx)
    {
        super (ctx);
        construct (ctx);
    }

    private void construct (Context ctx)
    {
        setOnClickListener (this);
        bgpaint = new Paint ();
        bgpaint.setColor (Color.YELLOW);
        bgpaint.setStrokeWidth (2.0F);
        bgpaint.setStyle (Paint.Style.STROKE);
        bgpaint.setTextAlign (Paint.Align.CENTER);
        bgpaint.setTextSize (18);
        bounds = new Rect ();
        letter = "";
        if (ctx instanceof MainActivity) {
            mainActivity = (MainActivity) ctx;
        }
    }

    public void setAmbient (boolean amb)
    {
        bgpaint.setColor (amb ? Color.LTGRAY : Color.YELLOW);
        invalidate ();
    }

    public void setMode (NavDialView.Mode mode)
    {
        switch (mode) {
            case OFF:   letter = " "; break;
            case GCT:   letter = "G"; break;
            case VOR:   letter = "V"; break;
            case ADF:   letter = "A"; break;
            case LOC:   letter = "L"; break;
            case LOCBC: letter = "B"; break;
            case ILS:   letter = "I"; break;
        }
        bgpaint.getTextBounds (letter, 0, 1, bounds);
        invalidate ();
    }

    // nav mode button clicked
    // open dialog to select new mode
    @Override
    public void onClick (View v)
    {
        if (mainActivity.navWaypt != null) {
            showButtons ();
        } else {
            mainActivity.showToast ("set waypoint on front page first");
        }
    }

    @SuppressLint("InflateParams")
    public void showButtons ()
    {
        // inflate the layout
        if (modeGroup == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            modePageView = layoutInflater.inflate (R.layout.mode_page, null);
            Button modeBack = modePageView.findViewById (R.id.modeBack);
            modeBack.setOnClickListener (mainActivity.backButtonListener);
            modeGroup = modePageView.findViewById (R.id.modeGroup);

            // set up what to do when a radio button gets clicked
            View.OnClickListener radioButtonListener = new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    NavDialView.Mode newmode = (NavDialView.Mode) v.getTag ();
                    mainActivity.setNavMode (newmode);
                    if ((mainActivity.navWaypt != null) && (mainActivity.curLoc != null)) {
                        mainActivity.startlat = mainActivity.curLoc.latitude;
                        mainActivity.startlon = mainActivity.curLoc.longitude;
                        double newobs = mainActivity.navWaypt.getMagRadTo (newmode, mainActivity.curLoc);
                        mainActivity.navDialView.setObs (newobs);
                    }
                    mainActivity.onBackPressed ();
                }
            };

            // make map of radio buttons for all the modes
            HashMap<NavDialView.Mode,CharSequence> nms = new HashMap<> ();
            nms.put (NavDialView.Mode.OFF,   "OFF");
            nms.put (NavDialView.Mode.GCT,   boldCharString ("GCT",   0));
            nms.put (NavDialView.Mode.VOR,   boldCharString ("VOR",   0));
            nms.put (NavDialView.Mode.ADF,   boldCharString ("ADF",   0));
            nms.put (NavDialView.Mode.LOC,   boldCharString ("LOC",   0));
            nms.put (NavDialView.Mode.LOCBC, boldCharString ("LOCBC", 3));
            nms.put (NavDialView.Mode.ILS,   boldCharString ("ILS",   0));

            HashMap<NavDialView.Mode,RadioButton> nmb = new HashMap<> ();
            for (NavDialView.Mode mode : nms.keySet ()) {
                RadioButton rb = new RadioButton (mainActivity);
                rb.setTag (mode);
                rb.setText (nms.get (mode));
                rb.setOnClickListener (radioButtonListener);
                nmb.put (mode, rb);
            }

            navModeButtons = nmb;
        }

        // build group of buttons allowed for the waypoint type
        NavDialView.Mode oldmode = mainActivity.navDialView.getMode ();
        modeGroup.removeAllViews ();
        NavDialView.Mode[] valids = mainActivity.navWaypt.validModes;
        for (NavDialView.Mode newmode : valids) {
            RadioButton rb = navModeButtons.get (newmode);
            assert rb != null;
            rb.setChecked (newmode == oldmode);
            modeGroup.addView (rb);
        }

        // display radio button page
        mainActivity.showMainPage (modePageView);
    }

    // make a spannable string with one character bolded
    private static SpannableString boldCharString (String str, int idx)
    {
        SpannableString ss = new SpannableString (str);
        ss.setSpan (modeMenuBold,  idx, idx + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan (modeMenuColor, idx, idx + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan (modeMenuSize,  idx, idx + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @Override
    public void onDraw (Canvas canvas)
    {
        int w = getWidth ();
        int h = getHeight ();
        canvas.drawCircle (w / 2.0F, h / 2.0F, w / 2.0F, bgpaint);
        canvas.drawText (letter, w / 2.0F, (h + bounds.height ()) / 2.0F, bgpaint);
    }
}
