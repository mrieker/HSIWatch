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
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * EditText with ENTER,BACKSPACE key callbacks.
 */
@SuppressLint("AppCompatCustomView")
public class MyEditText extends EditText implements TextView.OnEditorActionListener {

    public interface Listener {
        boolean onEnterKey (TextView v);  // true: stay in keypad; false: return to page with text box
        void onBackKey (TextView v);      // always goes back to page with text box
    }

    public Listener listener;

    public MyEditText (Context ctx, AttributeSet attrs)
    {
        super (ctx, attrs);
        setOnEditorActionListener (this);
    }

    public MyEditText (Context ctx)
    {
        super (ctx);
        setOnEditorActionListener (this);
    }

    // detect the ENTER key
    @Override
    public boolean onEditorAction (TextView v, int actionId, KeyEvent event)
    {
        if ((actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
            if (listener != null) return listener.onEnterKey (this);
        }
        return false;
    }

    // detect the BACK key
    // restore the original field contents
    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event)
    {
        if ((event.getAction () == KeyEvent.ACTION_UP) && (event.getKeyCode () == KeyEvent.KEYCODE_BACK)) {
            if (listener != null) listener.onBackKey (this);
        }
        return super.dispatchKeyEventPreIme (event);
    }
}
