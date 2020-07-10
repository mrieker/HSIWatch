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
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * Input box for entering a waypoint.
 */
@SuppressLint("AppCompatCustomView")
public class WayptEditText extends MyEditText implements MyEditText.Listener, View.OnTouchListener {

    // mandatory callbacks
    public interface WayptChangeListener {
        void wayptChanged (Waypt waypt);
        void showToast (String msg);
        SQLiteDatabase getSqlDB ();
    }

    private final static String dbnotready = "must wait for database to download\ndo MENU\u25B7UPDDB to start if not already";

    private String savedIdent;
    public  WayptChangeListener wcl;

    public WayptEditText (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        constructor ();
    }
    public WayptEditText (Context ctx)
    {
        super (ctx);
        constructor ();
    }
    private void constructor ()
    {
        savedIdent = "";
        listener = this;  // enable onEnterKey() and onBackKey() callbacks
        setOnTouchListener (this);
    }

    // clicked on, make sure there is a database before they can enter anything
    @Override
    public boolean onTouch (View v, MotionEvent event)
    {
        if (wcl.getSqlDB () == null) {
            wcl.showToast (dbnotready);
            return true;  // inhibit entry
        }
        return false;  // allow entry
    }

    // enter key pressed
    @Override  // Listener
    public boolean onEnterKey (TextView v)
    {
        String idstr = v.getText ().toString ().replace (" ", "").toUpperCase (Locale.US);
        if (! idstr.equals ("")) {

            // access latest database
            SQLiteDatabase sqldb = wcl.getSqlDB ();
            if (sqldb == null) {
                wcl.showToast (dbnotready);
                // database download in progress
                // displays toast when download started
                // displays toast when download completes or errors out
                return true;  // remain in text box
            }

            // find waypoint in database
            Waypt waypt = Waypt.find (sqldb, idstr);
            if (waypt == null) {
                wcl.showToast ("unknown " + idstr);
                return true;  // remain in text box
            }

            // display message so user can verify it is correct waypoint
            wcl.showToast (waypt.name);

            // tell mainActivity it is changed
            setText (waypt.ident);
            wcl.wayptChanged (waypt);
        } else {

            // tell mainActivity it is cleared
            setText ("");
            wcl.wayptChanged (null);
        }

        // return false - returns to main screen
        return false;
    }

    // restore original ident when BACK key pressed
    @Override  // Listener
    public void onBackKey (TextView v)
    {
        setText (savedIdent);
    }

    // save text being displayed
    @Override  // TextView
    public void setText (CharSequence text, TextView.BufferType type)
    {
        savedIdent = text.toString ();
        super.setText (text, type);
    }
}
