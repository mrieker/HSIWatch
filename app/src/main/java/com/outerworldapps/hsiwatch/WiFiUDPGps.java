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
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Use WiFi UDP to receive GPS location and status information.
 */
public class WiFiUDPGps extends ExternalGps {
    private byte[] buffer;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private int portno;
    private MyEditText portnoView;
    private SharedPreferences prefs;
    private View[] paramViews;

    public WiFiUDPGps (MainActivity ma)
    {
        super (ma);
        prefs = ma.getPreferences (Context.MODE_PRIVATE);
    }

    @SuppressLint("SetTextI18n")
    @Override  // GpsReceiver
    public View[] getParamViews ()
    {
        if (paramViews == null) {
            TextView portnoLabel = new TextView (mainActivity);
            portnoLabel.setText ("port");
            portnoView = new MyEditText (mainActivity);
            portno = prefs.getInt ("wifiudpgps.portno", 4000);
            portnoView.setInputType (InputType.TYPE_CLASS_NUMBER);
            portnoView.listener = portnoEntered;
            LinearLayout portnoLine = new LinearLayout (mainActivity);
            portnoLine.setOrientation (LinearLayout.HORIZONTAL);
            portnoLine.addView (portnoLabel);
            portnoLine.addView (portnoView);
            paramViews = new View[] { portnoLine, statusView };
            capable = true;
        }
        portnoView.setText (Integer.toString (portno));
        return paramViews;
    }

    // port number was entered
    // validate, write to preferences, restart receiver thread
    private final MyEditText.Listener portnoEntered = new MyEditText.Listener () {
        @Override
        public void onEnterKey (TextView v)
        {
            SharedPreferences.Editor editr = prefs.edit ();
            try {
                portno = Integer.parseInt (v.getText ().toString ());
                if ((portno < 1024) || (portno > 65535)) {
                    throw new NumberFormatException ("out of range");
                }
                editr.putInt ("wifiudpgps.portno", portno);
                editr.apply ();
                restartThread ();
            } catch (NumberFormatException nfe) {
                mainActivity.showToast ("must be integer 1024..65535");
            }
        }

        @Override
        public void onBackKey (TextView v)
        { }
    };

    /**
     * Enable receiving NMEA packets from GPS device via WiFi.
     */
    @Override  // ExternalGps
    protected Closeable openSocket ()
            throws Exception
    {
        socket = new DatagramSocket (portno);
        buffer = new byte[4096];
        packet = new DatagramPacket (buffer, buffer.length);
        return socket;
    }

    /**
     * Read an NMEA packet from GPS receiver via WiFi.
     * May contain more than one NMEA message.
     */
    @Override  // ExternalGps
    protected String readSocket ()
            throws Exception
    {
        socket.receive (packet);
        return new String (buffer, 0, packet.getLength ());
    }

    @Override  // ExternalGps
    protected String receiveException (Exception e)
    {
        return e.getMessage ();
    }
}
