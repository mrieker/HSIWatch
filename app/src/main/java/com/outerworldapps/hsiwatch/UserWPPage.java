//    Copyright (C) 2023, Mike Rieker, Beverly, MA USA
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.TreeMap;

/**
 * Contains a BACK button, and database update selection buttons.
 */
public class UserWPPage {
    private MainActivity mainActivity;
    private View userwpPageView;
    private View uwpedPage;
    private Waypt.UserWaypt editWaypt;

    public UserWPPage (MainActivity ma)
    {
        mainActivity = ma;
    }

    @SuppressLint("InflateParams")
    public void show ()
    {
        // if first time here, set up initial state
        if (userwpPageView == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            userwpPageView = layoutInflater.inflate (R.layout.userwp_page, null);

            // back button to return to menu page
            Button userwpBack = userwpPageView.findViewById (R.id.userwpBack);
            userwpBack.setOnClickListener (mainActivity.backButtonListener);

            // NEW button to create a new user waypoint
            Button newuwpButton = userwpPageView.findViewById (R.id.userwpNew);
            newuwpButton.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View view) { newButtonClicked (); }
            });

            // read existing csv file and provide button for each entry
            buildButtons ();
        }

        // display the page
        mainActivity.showMainPage (userwpPageView);
    }

    // build buttons, one per user waypoint in database (CSV file)
    private void buildButtons ()
    {
        // remove any existing user waypoint buttons
        // leave the NEW button at the top
        LinearLayout userwpList = userwpPageView.findViewById (R.id.userwpList);
        while (userwpList.getChildCount () > 1) {
            userwpList.removeViewAt (1);
        }

        // fill list of buttons with existing user waypoints
        TreeMap<String,Waypt.UserWaypt> uwps = Waypt.UserWaypt.getUserWaypoints (mainActivity);
        for (final Waypt.UserWaypt waypt : uwps.values ()) {

            // create a button for the waypoint
            Button wpbut = new Button (mainActivity);
            wpbut.setTag (waypt);
            wpbut.setText (waypt.ident + "\n" + waypt.latstr + "\n" + waypt.lonstr);

            // short click opens edit menu
            wpbut.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View view) { editButtonClicked ((Button) view); }
            });

            // long click navigates to waypoint
            wpbut.setOnLongClickListener (new View.OnLongClickListener () {
                @Override
                public boolean onLongClick (View view) { goButtonClicked (waypt); return true; }
            });

            // add to button list
            userwpList.addView (wpbut);
        }
    }

    // short click on existing user waypoint, display form to edit waypoint
    private void editButtonClicked (Button wpbut)
    {
        // display form to enter new waypoint
        newButtonClicked ();

        // fill it in with existing waypoint contents
        editWaypt = (Waypt.UserWaypt) wpbut.getTag ();
        EditText uwpedIdentBox = uwpedPage.findViewById (R.id.uwpedIdent);
        EditText uwpedLatBox = uwpedPage.findViewById (R.id.uwpedLat);
        EditText uwpedLonBox = uwpedPage.findViewById (R.id.uwpedLon);
        uwpedIdentBox.setText (editWaypt.ident);
        uwpedLatBox.setText (editWaypt.latstr);
        uwpedLonBox.setText (editWaypt.lonstr);
    }

    // NEW button clicked, display form to enter new waypoint
    private void newButtonClicked ()
    {
        LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        uwpedPage = layoutInflater.inflate (R.layout.uwped_page, null);

        // back button to return to menu page
        Button uwpedBack = uwpedPage.findViewById (R.id.uwpedBack);
        uwpedBack.setOnClickListener (mainActivity.backButtonListener);

        // SAVE button to write to database
        Button uwpedSave = uwpedPage.findViewById (R.id.uwpedSave);
        uwpedSave.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view) { saveButtonClicked (); }
        });

        // GPS button to get current GPS location
        Button uwpedGPS = uwpedPage.findViewById (R.id.uwpedGPS);
        uwpedGPS.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view) { gpsButtonClicked (); }
        });

        // GO button to navigate to waypoint
        Button uwpedGo = uwpedPage.findViewById (R.id.uwpedGo);
        uwpedGo.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view) { goButtonClicked (editWaypt); }
        });

        // DEL button to delete waypoint
        Button uwpedDel = uwpedPage.findViewById (R.id.uwpedDel);
        uwpedDel.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view) { delButtonClicked (); }
        });

        mainActivity.showMainPage (uwpedPage);
    }

    // delete button clicked
    private void delButtonClicked ()
    {
        // set up page to ask for confirmation
        LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        View uwpdelynPage = layoutInflater.inflate (R.layout.uwpdelyn_page, null);
        TextView uwpdelynIdent = uwpdelynPage.findViewById (R.id.uwpdelynIdent);
        uwpdelynIdent.setText (editWaypt.ident);

        // if they click YES-NUKE, then delete from database then go back to waypoint list page
        Button uwpdelynNuke = uwpdelynPage.findViewById (R.id.uwpdelynNuke);
        uwpdelynNuke.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view) {
                TreeMap<String,Waypt.UserWaypt> uwps = Waypt.UserWaypt.getUserWaypoints (mainActivity);
                uwps.remove (editWaypt.ident);
                Waypt.UserWaypt.saveUserWaypoints (mainActivity);
                buildButtons ();
                mainActivity.onBackPressed ();
                mainActivity.onBackPressed ();
                mainActivity.showToast ("waypoint deleted");
            }
        });

        // if they click NO-KEEP, then just go back to edit page
        Button uwpdelynKeep = uwpdelynPage.findViewById (R.id.uwpdelynKeep);
        uwpdelynKeep.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view) {
                mainActivity.onBackPressed ();
            }
        });

        // display confirmation page
        mainActivity.showMainPage (uwpdelynPage);
    }

    // SAVE button clicked, write point to database
    private void saveButtonClicked ()
    {
        EditText uwpedIdentBox = uwpedPage.findViewById (R.id.uwpedIdent);
        EditText uwpedLatBox = uwpedPage.findViewById (R.id.uwpedLat);
        EditText uwpedLonBox = uwpedPage.findViewById (R.id.uwpedLon);
        String ident = uwpedIdentBox.getText ().toString ().toUpperCase ().replace (",", " ").trim ();
        String latstr = uwpedLatBox.getText ().toString ().toUpperCase ().trim ();
        String lonstr = uwpedLonBox.getText ().toString ().toUpperCase ().trim ();
        double lat = Waypt.UserWaypt.parseLatLon (latstr, 'N', 'S');
        double lon = Waypt.UserWaypt.parseLatLon (lonstr, 'E', 'W');
        if (ident.equals ("")) {
            mainActivity.showToastLong ("fill in ident");
        } else if (Double.isNaN (lat)) {
            mainActivity.showToastLong ("fill latitude\nNS d m s.s...\nNS d m.m...\nNS d.d...");
        } else if (Double.isNaN (lon)) {
            mainActivity.showToastLong ("fill longitude\nEW d m s.s...\nEW d m.m...\nEW d.d...");
        } else {
            TreeMap<String,Waypt.UserWaypt> uwps = Waypt.UserWaypt.getUserWaypoints (mainActivity);
            editWaypt = new Waypt.UserWaypt (ident, latstr, lonstr);
            uwps.put (ident, editWaypt);
            if (Waypt.UserWaypt.saveUserWaypoints (mainActivity)) {
                buildButtons ();
                mainActivity.onBackPressed ();
                mainActivity.showToastLong ("waypoint saved\n" +
                        editWaypt.ident + "\n" +
                        formatLatLon (editWaypt.lat, 'N', 'S') + "\n" +
                        formatLatLon (editWaypt.lon, 'E', 'W'));
            }
        }
    }

    // GPS button clicked, fill in lat/lon boxes from current GPS co-ordinates
    private void gpsButtonClicked ()
    {
        EditText uwpedLatBox = uwpedPage.findViewById (R.id.uwpedLat);
        EditText uwpedLonBox = uwpedPage.findViewById (R.id.uwpedLon);
        uwpedLatBox.setText (formatLatLon (mainActivity.curLoc.lat, 'N', 'S'));
        uwpedLonBox.setText (formatLatLon (mainActivity.curLoc.lon, 'E', 'W'));
    }

    private static String formatLatLon (double ll, char pos, char neg)
    {
        StringBuilder sb = new StringBuilder ();
        sb.append ((ll < 0) ? neg : pos);
        ll = Math.abs (ll);
        int llsec10ths = (int) Math.round (ll * 36000);
        int degs = llsec10ths / 36000;
        int mins = llsec10ths / 600 % 60;
        int secs = llsec10ths / 10 % 60;
        int tnts = llsec10ths % 10;
        sb.append (degs);
        sb.append (' ');
        sb.append (mins);
        sb.append (' ');
        sb.append (secs);
        sb.append ('.');
        sb.append (tnts);
        return sb.toString ();
    }

    // short click or GO button, make waypoint current
    private void goButtonClicked (Waypt.UserWaypt waypt)
    {
        if (waypt != null) {
            mainActivity.setNavWaypt (waypt);
            do mainActivity.onBackPressed ();
            while (mainActivity.currentMainPage != mainActivity.navMainPage);
        }
    }
}
