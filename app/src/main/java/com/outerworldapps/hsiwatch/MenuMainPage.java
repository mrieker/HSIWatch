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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Map;

/**
 * Page containing the menu buttons.
 */
public class MenuMainPage {
    public  Button upddbButton;
    public  CheckBox ambEnabCkBox;
    public  CheckBox hsiModeCkBox;
    public  CheckBox voiceEnCkBox;
    private CommMainPage commMainPage;
    private int numResetClicks;
    private long lastExitClick;
    private long lastResetClick;
    private MainActivity mainActivity;
    private SatsMainPage satsMainPage;
    public  SimMainPage simMainPage;
    private View menuPageView;

    @SuppressLint("InflateParams")
    public MenuMainPage (MainActivity ma)
    {
        mainActivity = ma;

        final LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        menuPageView = layoutInflater.inflate (R.layout.menu_page, null);

        Button backButton = menuPageView.findViewById (R.id.backButton);
        backButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                mainActivity.onBackPressed ();
            }
        });

        // LEFT COLUMN

        satsMainPage = new SatsMainPage (mainActivity);
        final Button satsButton = menuPageView.findViewById (R.id.satsButton);
        satsButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                satsMainPage.show ();
            }
        });

        Button aboutButton = menuPageView.findViewById (R.id.aboutButton);
        aboutButton.setOnClickListener (new View.OnClickListener () {
            private View aboutPageView;

            @Override
            public void onClick (View v)
            {
                if (aboutPageView == null) {
                    aboutPageView = layoutInflater.inflate (R.layout.about_page, null);
                    Button aboutBack = aboutPageView.findViewById (R.id.aboutBack);
                    aboutBack.setOnClickListener (new View.OnClickListener () {
                        @Override
                        public void onClick (View v)
                        {
                            mainActivity.onBackPressed ();
                        }
                    });
                }

                TextView textView = aboutPageView.findViewById (R.id.aboutText);
                StringBuilder sb = new StringBuilder ();
                sb.append ("\u00A9 outerworldapps.com\nversion: ");
                sb.append (getVersionName ());
                sb.append (" - ");
                sb.append (getGitHash ());
                if (getGitDirtyFlag ()) sb.append ('+');
                sb.append ("\ndb exp: ");
                sb.append (mainActivity.downloadThread.dbexp);
                sb.append ("\n\nhelp available at https://www.outerworldapps.com/HSIWatch");
                textView.setText (sb);

                mainActivity.showMainPage (aboutPageView);
            }
        });

        upddbButton = menuPageView.findViewById (R.id.upddbButton);
        upddbButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                mainActivity.downloadThread.upddb ();
            }
        });

        // MIDDLE COLUMN

        hsiModeCkBox = menuPageView.findViewById (R.id.hsiModeCkbox);
        final SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
        hsiModeCkBox.setChecked (prefs.getBoolean ("hsiMode", true));
        hsiModeCkBox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                boolean checked = hsiModeCkBox.isChecked ();
                mainActivity.navDialView.revRotate = checked;
                mainActivity.updateNavDial ();
                SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("hsiMode", checked);
                editr.apply ();
            }
        });

        voiceEnCkBox = menuPageView.findViewById (R.id.voiceEnCkbox);
        voiceEnCkBox.setChecked (prefs.getBoolean ("voiceEnable", false));
        voiceEnCkBox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                boolean checked = voiceEnCkBox.isChecked ();
                SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("voiceEnable", checked);
                editr.apply ();
            }
        });

        ambEnabCkBox = menuPageView.findViewById (R.id.ambEnabCkbox);
        ambEnabCkBox.setChecked (prefs.getBoolean ("ambModeEnab", true));
        ambEnabCkBox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                boolean checked = ambEnabCkBox.isChecked ();
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("ambModeEnab", checked);
                editr.apply ();
            }
        });

        simMainPage = new SimMainPage (mainActivity);
        Button simButton = menuPageView.findViewById (R.id.simButton);
        simButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                simMainPage.show ();
            }
        });

        commMainPage = new CommMainPage (mainActivity);
        Button udpButton = menuPageView.findViewById (R.id.commButton);
        udpButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                commMainPage.show ();
            }
        });

        // RIGHT COLUMN

        Button exitButton = menuPageView.findViewById (R.id.exitButton);
        exitButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                long now = System.currentTimeMillis ();
                long sinceLastClick = now - lastExitClick;
                if (sinceLastClick < 3000) System.exit (0);
                lastExitClick = now;
                mainActivity.showToast ("close app\nonce more in 3 seconds");
            }
        });

        Button resetButton = menuPageView.findViewById (R.id.resetButton);
        resetButton.setOnClickListener (new View.OnClickListener () {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick (View v)
            {
                long now = System.currentTimeMillis ();
                long sinceLastClick = now - lastResetClick;
                if (sinceLastClick < 3000) {
                    if (++ numResetClicks < 3) return;
                    SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
                    SharedPreferences.Editor editr = prefs.edit ();
                    Map<String,?> keys = prefs.getAll ();
                    for (Map.Entry<String,?> entry : keys.entrySet ()) {
                        editr.remove (entry.getKey ());
                    }
                    editr.commit ();
                    mainActivity.downloadThread.deleteAll ();
                    System.exit (0);
                }
                lastResetClick = now;
                numResetClicks = 1;
                mainActivity.showToast ("reset to fresh install\ntwice more in 3 seconds");
            }
        });
    }

    public void show ()
    {
        upddbButton.setTextColor (mainActivity.downloadThread.buttonColor ());
        mainActivity.showMainPage (menuPageView);
    }

    private String getVersionName ()
    {
        try {
            PackageInfo pInfo = mainActivity.getPackageManager ().getPackageInfo (mainActivity.getPackageName (), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            return "";
        }
    }

    private static String getGitHash ()
    {
        String fullhash = BuildConfig.GitHash;
        return fullhash.substring (0, 7);
    }

    private static boolean getGitDirtyFlag ()
    {
        String status = BuildConfig.GitStatus;
        String[] lines = status.split ("\n");
        for (String line : lines) {
            if (line.contains ("modified:") && !line.contains ("app.iml")) {
                return true;
            }
        }
        return false;
    }
}
