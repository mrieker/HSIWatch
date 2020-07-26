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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Contains a BACK button and the GPS satellite status circle.
 */
public class SatsMainPage {
    public  GpsStatusView gpsStatusView;
    private MainActivity mainActivity;
    public  View satsPageView;

    public SatsMainPage (MainActivity ma)
    {
        mainActivity = ma;
    }

    @SuppressLint("InflateParams")
    public void show ()
    {
        if (satsPageView == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            satsPageView = layoutInflater.inflate (R.layout.sats_page, null);
            Button satsBack = satsPageView.findViewById (R.id.satsBack);
            satsBack.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    gpsStatusView.Shutdown ();
                    mainActivity.internalGps.setStatusListener (null);
                    mainActivity.onBackPressed ();
                }
            });
            gpsStatusView = satsPageView.findViewById (R.id.gpsStatusView);
        }
        gpsStatusView.Startup ();
        mainActivity.internalGps.setStatusListener (gpsStatusView);
        mainActivity.showMainPage (satsPageView);
    }
}
