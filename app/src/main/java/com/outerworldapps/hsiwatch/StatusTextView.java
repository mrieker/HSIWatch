//    Copyright (C) 2016, Mike Rieker, Beverly, MA USA
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
import android.widget.TextView;

/**
 * Just like TextView except can do setText() from any thread.
 */
@SuppressLint({ "AppCompatCustomView", "ViewConstructor" })
public class StatusTextView extends TextView implements Runnable {
    private BufferType statusType;
    private CharSequence statusText;
    private MainActivity mainActivity;
    private final Object statusLock;

    public StatusTextView (MainActivity ma)
    {
        super (ma);
        setEms (10);  // don't take up too much width on round watch
        mainActivity = ma;
        statusLock = new Object ();
    }

    @Override
    public void setText (CharSequence text, BufferType type)
    {
        if (statusLock == null) {
            // called during super(ma) call in constructor
            super.setText (text, type);
            return;
        }

        synchronized (statusLock) {
            if (statusText == null) {
                statusText = text;
                statusType = type;
                mainActivity.myHandler.postDelayed (this, 100);
            } else {
                statusText = text;
                statusType = type;
            }
        }
    }

    @Override
    public void run ()
    {
        CharSequence text;
        BufferType type;
        synchronized (statusLock) {
            text = statusText;
            type = statusType;
            statusText = null;
        }
        if (text != null) StatusTextView.super.setText (text, type);
    }
}
