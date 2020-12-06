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
    public  CheckBox fillChinCkBox;
    public  CheckBox hsiModeCkBox;
    public  CheckBox timeDotsCkBox;
    private SendMainPage sendMainPage;
    private int numResetClicks;
    private long lastExitClick;
    private long lastResetClick;
    private MainActivity mainActivity;
    public  SatsMainPage satsMainPage;
    public  UpdDBMainPage updDBMainPage;
    private View menuPageView;
    private View menu2PageView;

    @SuppressLint("InflateParams")
    public MenuMainPage (MainActivity ma)
    {
        mainActivity = ma;

        final LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        menuPageView = layoutInflater.inflate (R.layout.menu_page, null);

        Button backButton = menuPageView.findViewById (R.id.backButton);
        backButton.setOnClickListener (mainActivity.backButtonListener);

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

        final Button gpsButton = menuPageView.findViewById (R.id.gpsButton);
        gpsButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                mainActivity.showMainPage (mainActivity.gpsPageView);
            }
        });

        updDBMainPage = new UpdDBMainPage (mainActivity);
        upddbButton = menuPageView.findViewById (R.id.upddbButton);
        upddbButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                updDBMainPage.show ();
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
                mainActivity.updateNavDial ();
                SharedPreferences prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("hsiMode", checked);
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

        timeDotsCkBox = menuPageView.findViewById (R.id.timeDotsCkBox);
        timeDotsCkBox.setChecked (prefs.getBoolean ("timeDots", false));
        timeDotsCkBox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("timeDots", timeDotsCkBox.isChecked ());
                editr.apply ();
            }
        });

        sendMainPage = new SendMainPage (mainActivity);
        Button sendButton = menuPageView.findViewById (R.id.sendButton);
        sendButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                sendMainPage.show ();
            }
        });

        // RIGHT COLUMN

        Button moreButton = menuPageView.findViewById (R.id.moreButton);
        moreButton.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View v)
            {
                mainActivity.showMainPage (menu2PageView);
            }
        });

        // PAGE 2

        menu2PageView = layoutInflater.inflate (R.layout.menu2_page, null);

        Button back2Button = menu2PageView.findViewById (R.id.back2Button);
        back2Button.setOnClickListener (mainActivity.backButtonListener);

        Button aboutButton = menu2PageView.findViewById (R.id.aboutButton);
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
                sb.append ("\n\nhelp available at https://www.outerworldapps.com/HSIWatch");
                textView.setText (sb);

                mainActivity.showMainPage (aboutPageView);
            }
        });

        fillChinCkBox = menu2PageView.findViewById (R.id.fillChinCkBox);
        boolean hasChin = (mainActivity.heightPixels < mainActivity.widthPixels);
        fillChinCkBox.setVisibility (hasChin ? View.VISIBLE : View.INVISIBLE);
        if (hasChin) {
            boolean fillChin;
            if (prefs.contains ("fillChin")) {
                fillChin = prefs.getBoolean ("fillChin", false);
            } else {
                fillChin = mainActivity.hadPreviouslyAgreed;
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("fillChin", fillChin);
                editr.apply ();
                if (! fillChin) {
                    mainActivity.showToastLong ("To fill chin, click MENU\u25B7MORE\u25B7Fill Chin");
                }
            }
            fillChinCkBox.setChecked (fillChin);
            fillChinCkBox.setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View v)
                {
                    SharedPreferences.Editor editr = prefs.edit ();
                    editr.putBoolean ("fillChin", fillChinCkBox.isChecked ());
                    editr.apply ();
                    mainActivity.setNavMainPageScale ();
                }
            });
        }

        Button exitButton = menu2PageView.findViewById (R.id.exitButton);
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

        Button resetButton = menu2PageView.findViewById (R.id.resetButton);
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

    public View getView ()
    {
        upddbButton.setTextColor (mainActivity.downloadThread.buttonColor ());
        return menuPageView;
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
