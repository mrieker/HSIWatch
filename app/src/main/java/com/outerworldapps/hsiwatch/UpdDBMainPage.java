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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * Contains a BACK button, and database update selection buttons.
 */
public class UpdDBMainPage {
    private MainActivity mainActivity;
    private RadioButton  useFAAdbButton;
    private RadioButton  useOAdbButton;
    public  String       dbselected;
    private View         upddbPageView;

    public UpdDBMainPage (MainActivity ma)
    {
        mainActivity = ma;
        SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
        dbselected = prefs.getString ("dbselect", "faa");
    }

    @SuppressLint("InflateParams")
    public void show ()
    {
        // if first time here, set up initial state
        if (upddbPageView == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            upddbPageView = layoutInflater.inflate (R.layout.upddb_page, null);

            // back button to return to menu page
            Button upddbBack = upddbPageView.findViewById (R.id.upddbBack);
            upddbBack.setOnClickListener (mainActivity.backButtonListener);

            // select FAA database
            useFAAdbButton = upddbPageView.findViewById (R.id.useFAAdb);
            useFAAdbButton.setTag ("faa");
            useFAAdbButton.setOnClickListener (dbselector);

            // select OA database
            useOAdbButton  = upddbPageView.findViewById (R.id.useOAdb);
            useOAdbButton.setTag ("oa");
            useOAdbButton.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View view)
                {
                    // if first time downloading, display warning screen and wait for OK or Cancel
                    if (mainActivity.downloadThread.getDbExp ("oa") == null) {
                        LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
                        View updoapage = layoutInflater.inflate (R.layout.updoa_page, null);
                        Button updoaok = updoapage.findViewById (R.id.updOAok);
                        updoaok.setOnClickListener (new View.OnClickListener () {
                            @Override
                            public void onClick (View view)
                            {
                                mainActivity.backButtonListener.onClick (view);
                                dbselector.onClick (useOAdbButton);
                            }
                        });
                        Button updoacan = updoapage.findViewById (R.id.updOAcan);
                        updoacan.setOnClickListener (new View.OnClickListener () {
                            @Override
                            public void onClick (View view)
                            {
                                mainActivity.backButtonListener.onClick (view);
                                setRadioButtons ();
                            }
                        });
                        mainActivity.showMainPage (updoapage);
                    } else {
                        dbselector.onClick (view);
                    }
                }
            });

            // download the latest version of selected database
            Button download = upddbPageView.findViewById (R.id.download);
            download.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View view)
                {
                    mainActivity.downloadThread.upddb ();
                }
            });

            // set up whichever database is selected by the preferences
            setRadioButtons ();
        }

        // display expiration dates of all databases
        updateExpirations ();

        // display the page
        mainActivity.showMainPage (upddbPageView);
    }

    // selects database given in button tag
    private View.OnClickListener dbselector = new View.OnClickListener () {
        @Override
        public void onClick (View view)
        {
            dbselected = (String) view.getTag ();
            SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("dbselect", dbselected);
            editr.apply ();
            mainActivity.downloadThread.dbSelected ();
        }
    };

    // set whichever radio button corresponds to currently selected database
    private void setRadioButtons ()
    {
        useFAAdbButton.setChecked (dbselected.equals ("faa"));
        useOAdbButton.setChecked (dbselected.equals ("oa"));
    }

    // a new database was just downloaded, update the expiration date strings being displayed
    public void updateExpirations ()
    {
        if (upddbPageView != null) {
            TextView expFAAdb = upddbPageView.findViewById (R.id.expFAAdb);
            TextView expOAdb  = upddbPageView.findViewById (R.id.expOAdb);

            String faaexp = mainActivity.downloadThread.getDbExp ("faa");
            String oaexp  = mainActivity.downloadThread.getDbExp ("oa");

            expFAAdb.setText ((faaexp == null) ? "(none)" : "exp: " + faaexp);
            expOAdb.setText  ((oaexp  == null) ? "(none)" : "exp: " + oaexp);
        }
    }
}
