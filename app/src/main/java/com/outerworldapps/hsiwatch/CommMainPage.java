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
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Transmit our GPS location over UDP and/or Bluetooth
 */
public class CommMainPage implements GpsTransmitter {
    private final static int numsats = 12;
    private final static String btServerUUIDPrefixDef = "00001101";
    private final static String btServerUUIDSuffix = "-0000-1000-8000-00805f9b34fb";

    private final static byte[] hexbytes = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };

    private BluetoothServer bluetoothServer;
    private CheckBox btEnabCkBox;
    private CheckBox udpEnabCkBox;
    private DatagramSocket datagramSocket;
    private InetAddress servipaddr;
    private int servportno;
    public  MainActivity mainActivity;
    private SharedPreferences prefs;
    private SimpleDateFormat sdfhms;
    private SimpleDateFormat sdfdmy;
    private UUID btServerUUID;
    private TextView conCountView;
    public  View commPageView;

    public CommMainPage (MainActivity ma)
    {
        mainActivity = ma;
        prefs = mainActivity.getPreferences (Context.MODE_PRIVATE);
    }

    @SuppressLint("InflateParams")
    public void show ()
    {
        if (commPageView == null) {
            LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
            commPageView = layoutInflater.inflate (R.layout.comm_page, null);
            Button udpBack = commPageView.findViewById (R.id.commBack);
            udpBack.setOnClickListener (mainActivity.backButtonListener);
            ScrollView commScroll = commPageView.findViewById (R.id.commScroll);
            commScroll.addView (Initialize ());
        }
        mainActivity.showMainPage (commPageView);
    }

    @SuppressLint("SetTextI18n")
    private View Initialize ()
    {
        // UDP parameter inputs

        udpEnabCkBox = new CheckBox (mainActivity);
        udpEnabCkBox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view)
            {
                try {
                    openUDPSocket ();
                } catch (Exception e) {
                    mainActivity.showToast (e.getMessage ());
                }
            }
        });

        LinearLayout lp1 = new LinearLayout (mainActivity);
        lp1.setOrientation (LinearLayout.HORIZONTAL);
        lp1.addView (udpEnabCkBox);
        lp1.addView (TextString ("UDP Transmit to"));

        MyEditText ipAddrBox = new MyEditText (mainActivity);
        ipAddrBox.setImeOptions (EditorInfo.IME_ACTION_DONE);
        ipAddrBox.setInputType (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        ipAddrBox.setEms (6);
        ipAddrBox.listener = new MyEditText.Listener () {
            @Override
            public boolean onEnterKey (TextView v) {
                return setIpAddr (v.getText ().toString ().trim ());
            }
            @Override
            public void onBackKey (TextView v) {
                v.setText (prefs.getString ("udpSendIpAddr", ""));
            }
        };

        LinearLayout llat = new LinearLayout (mainActivity);
        llat.setOrientation (LinearLayout.HORIZONTAL);
        llat.addView (TextString ("ip addr"));
        llat.addView (ipAddrBox);

        MyEditText portNoBox = new MyEditText (mainActivity);
        portNoBox.setImeOptions (EditorInfo.IME_ACTION_DONE);
        portNoBox.setInputType (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        portNoBox.setEms (6);
        portNoBox.listener = new MyEditText.Listener () {
            @Override
            public boolean onEnterKey (TextView v)
            {
                return setPortNo (v.getText ().toString ().trim ());
            }
            @Override
            public void onBackKey (TextView v)
            {
                v.setText (prefs.getString ("udpSendPortNo", ""));
            }
        };

        LinearLayout llon = new LinearLayout (mainActivity);
        llon.setOrientation (LinearLayout.HORIZONTAL);
        llon.addView (TextString ("port no"));
        llon.addView (portNoBox);

        // Bluetooth parameter inputs

        bluetoothServer = new BluetoothServer (this);

        btEnabCkBox = new CheckBox (mainActivity);
        btEnabCkBox.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view)
            {
                if (btEnabCkBox.isChecked ()) {
                    bluetoothServer.startup (btServerUUID);
                } else {
                    bluetoothServer.shutdown ();
                }
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("btLisEnable", btEnabCkBox.isChecked ());
                editr.apply ();
            }
        });

        LinearLayout lp2 = new LinearLayout (mainActivity);
        lp2.setOrientation (LinearLayout.HORIZONTAL);
        lp2.addView (btEnabCkBox);
        lp2.addView (TextString ("BT Listening on"));

        MyEditText uuidPfxBox = new MyEditText (mainActivity);
        uuidPfxBox.setImeOptions (EditorInfo.IME_ACTION_DONE);
        uuidPfxBox.setInputType (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        uuidPfxBox.setEms (4);
        uuidPfxBox.listener = new MyEditText.Listener () {
            @Override
            public boolean onEnterKey (TextView v)
            {
                btEnabCkBox.setChecked (false);
                bluetoothServer.shutdown ();
                SharedPreferences.Editor editr = prefs.edit ();
                editr.putBoolean ("btLisEnable", false);
                editr.apply ();
                return setBtUUIDPfx (v.getText ().toString ().trim ());
            }
            @Override
            public void onBackKey (TextView v)
            {
                v.setText (prefs.getString ("btLisUUIDPfx", btServerUUIDPrefixDef));
            }
        };

        LinearLayout llbt = new LinearLayout (mainActivity);
        llbt.setOrientation (LinearLayout.HORIZONTAL);
        llbt.addView (TextString ("UUID"));
        llbt.addView (uuidPfxBox);

        conCountView = new TextView (mainActivity);

        /*
         * Layout the screen and display it.
         */
        LinearLayout linearLayout = new LinearLayout (mainActivity);
        linearLayout.setOrientation (LinearLayout.VERTICAL);

        LinearLayout.LayoutParams llpwc = new LinearLayout.LayoutParams (
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llpwc.gravity = Gravity.CENTER;
        linearLayout.addView (lp1, llpwc);
        linearLayout.addView (llat, llpwc);
        linearLayout.addView (llon, llpwc);
        linearLayout.addView (lp2, llpwc);
        linearLayout.addView (llbt, llpwc);
        linearLayout.addView (TextString (btServerUUIDSuffix));
        linearLayout.addView (conCountView, llpwc);

        // allow network IO from GUI thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        sdfhms = new SimpleDateFormat ("HHmmss.SSS", Locale.US);
        sdfhms.setTimeZone (TimeZone.getTimeZone ("UTC"));

        sdfdmy = new SimpleDateFormat ("ddMMyy", Locale.US);
        sdfdmy.setTimeZone (TimeZone.getTimeZone ("UTC"));

        // get main program to send us gps points
        mainActivity.gpsTransmitter = this;

        // load previous values from preferences
        // UDP must be manually enabled each time starting app cuz it transmits
        // Bluetooth remembers being enabled or not cuz it's a server

        String prefipaddr = prefs.getString ("udpSendIpAddr", "");
        String prefportno = prefs.getString ("udpSendPortNo", "");
        if (! "".equals (prefipaddr)) {
            setIpAddr (prefipaddr);
            ipAddrBox.setText (prefipaddr);
        }
        if (! "".equals (prefportno)) {
            setPortNo (prefportno);
            portNoBox.setText (prefportno);
        }

        String prefbtuuid = prefs.getString ("btLisUUIDPfx", btServerUUIDPrefixDef);
        setBtUUIDPfx (prefbtuuid);
        uuidPfxBox.setText (prefbtuuid);
        boolean prefbtenab = prefs.getBoolean ("btLisEnable", false);
        if (prefbtenab && (btServerUUID != null)) bluetoothServer.startup (btServerUUID);

        return linearLayout;
    }

    private TextView TextString (String str)
    {
        TextView tv = new TextView (mainActivity);
        tv.setText (str);
        return tv;
    }

    /******************************************************\
     *  Just got a new GPS co-ordinate                    *
     *  Transmit it over UDP in the blind if enabled      *
     *  Transmit it over bluetooth if anything connected  *
    \******************************************************/

    @Override
    public void sendLocation (GpsLocation loc)
    {
        String strhms = sdfhms.format (loc.time);   // hhmmss.sss
        String strdmy = sdfdmy.format (loc.time);   // ddmmyy

        double lat = loc.latitude;
        double lon = loc.longitude;
        double alt = loc.altitude;

        byte[] buf = new byte[200];
        int i = buf.length;

        // http://www.gpsinformation.org/dale/nmea.htm#GGA
        //  $GPGGA,hhmmss,lat,NS,lon,EW,1,numsats,0.9,alt,M,,,,*xx\r\n
        buf[--i] = '\n';
        buf[--i] = '\r';
        i -= 2;
        buf[--i] = '*';
        buf[--i] = ',';
        buf[--i] = ',';
        buf[--i] = ',';
        buf[--i] = ',';
        buf[--i] = 'M';
        buf[--i] = ',';
        i = pushDouble (buf, i, alt);
        buf[--i] = ',';
        buf[--i] = '9';
        buf[--i] = '.';
        buf[--i] = '0';
        buf[--i] = ',';
        i = pushInteger (buf, i, numsats, 1);
        buf[--i] = ',';
        buf[--i] = '1';
        buf[--i] = ',';
        i = pushLatLon (buf, i, lon, 'E', 'W');
        buf[--i] = ',';
        i = pushLatLon (buf, i, lat, 'N', 'S');
        buf[--i] = ',';
        i = pushString (buf, i, strhms);
        buf[--i] = ',';
        buf[--i] = 'A';
        buf[--i] = 'G';
        buf[--i] = 'G';
        buf[--i] = 'P';
        buf[--i] = 'G';
        buf[--i] = '$';
        NMEAChecksum (buf, i);

        // http://www.gpsinformation.org/dale/nmea.htm#RMC
        //  $GPRMC,hhmmss,A,lat,NS,lon,EW,kts,tc,ddmmyy,,*xx\r\n
        buf[--i] = '\n';
        buf[--i] = '\r';
        i -= 2;
        buf[--i] = '*';
        buf[--i] = ',';
        buf[--i] = ',';
        i = pushString (buf, i, strdmy);
        buf[--i] = ',';
        i = pushDouble (buf, i, loc.truecourse);
        buf[--i] = ',';
        i = pushDouble (buf, i, loc.speed * Lib.KtPerMPS);
        buf[--i] = ',';
        i = pushLatLon (buf, i, lon, 'E', 'W');
        buf[--i] = ',';
        i = pushLatLon (buf, i, lat, 'N', 'S');
        buf[--i] = ',';
        buf[--i] = 'A';
        buf[--i] = ',';
        i = pushString (buf, i, strhms);
        buf[--i] = ',';
        buf[--i] = 'C';
        buf[--i] = 'M';
        buf[--i] = 'R';
        buf[--i] = 'P';
        buf[--i] = 'G';
        buf[--i] = '$';
        NMEAChecksum (buf, i);

        //Log.d (MainActivity.TAG, "CommMainPage.sendLocation*: " + new String (buf, i, buf.length - i));
        TransmitBytes (buf, i, buf.length - i);
    }

    // convert a number of degrees to ddmm.1000s,pn string
    private static int pushLatLon (byte[] buf, int i, double ll, char pos, char neg)
    {
        int min1000 = (int) Math.round (ll * 60000.0);
        if (min1000 < 0) {
            min1000 = - min1000;
            pos = neg;
        }
        buf[--i] = (byte) pos;
        buf[--i] = ',';
        int deg  = min1000 / 60000;
        min1000 %= 60000;
        int min  = min1000 / 1000;
        min1000 %= 1000;
        i = pushInteger (buf, i, min1000, 3);
        buf[--i] = '.';
        i = pushInteger (buf, i, min, 2);
        return pushInteger (buf, i, deg, 1);
    }

    private static int pushDouble (byte[] buf, int i, double dval)
    {
        int ival = (int) Math.round (dval * 10.0);
        if (ival % 10 != 0) {
            buf[--i] = (byte) (ival % 10 + '0');
            buf[--i] = '.';
        }
        return pushInteger (buf, i, ival / 10, 1);
    }

    private static int pushInteger (byte[] buf, int i, int ival, int mindigs)
    {
        do {
            buf[--i] = (byte) (ival % 10 + '0');
            ival /= 10;
        } while ((-- mindigs > 0) || (ival > 0));
        return i;
    }

    private static int pushString (byte[] buf, int i, String sval)
    {
        for (int j = sval.length (); -- j >= 0;) {
            buf[--i] = (byte) sval.charAt (j);
        }
        return i;
    }

    // append NMEA checksum and CRLF to a string
    private static void NMEAChecksum (byte[] buf, int i)
    {
        byte xor = 0;
        while (true) {
            byte b = buf[++i];
            if (b == '*') break;
            xor ^= b;
        }
        buf[++i] = hexbytes[(xor>>4)&15];
        buf[++i] = hexbytes[xor&15];
    }

    // transmit string as an UDP packet
    // also transmit to any connected blueteeth
    private void TransmitBytes (byte[] buf, int ofs, int len)
    {
        if (datagramSocket != null) {
            DatagramPacket packet = new DatagramPacket (buf, ofs, len, servipaddr, servportno);
            try {
                datagramSocket.send (packet);
            } catch (IOException ioe) {
                Log.e (MainActivity.TAG, "error sending UDP packet", ioe);
                mainActivity.showToast ("error sending UDP packet");
                datagramSocket = null;
            }
        }

        if (bluetoothServer != null) {
            bluetoothServer.write (buf, ofs, len);
        }
    }

    /***********************************************************\
     *  UDP processing                                         *
     *  Transmit GPS data in the blind to given ipaddr/portno  *
    \***********************************************************/

    private boolean setIpAddr (String text)
    {
        try {
            servipaddr = InetAddress.getByName (text);
            openUDPSocket ();
            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("udpSendIpAddr", text);
            editr.apply ();
        } catch (Exception e) {
            mainActivity.showToast (e.getMessage ());
            return true;
        }
        return false;
    }

    private boolean setPortNo (String text)
    {
        try {
            int pn = Integer.parseInt (text);
            if ((pn < 1024) || (pn > 65535)) {
                throw new Exception ("out of range 1024..65535");
            }
            servportno = pn;
            openUDPSocket ();
            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("udpSendPortNo", text);
            editr.apply ();
        } catch (Exception e) {
            mainActivity.showToast (e.getMessage ());
            return true;
        }
        return false;
    }

    // set up local UDP socket if everything is set up
    private void openUDPSocket ()
            throws SocketException
    {
        datagramSocket = null;
        if (udpEnabCkBox.isChecked () && (servipaddr != null) && (servportno != 0)) {
            datagramSocket = new DatagramSocket ();
            datagramSocket.setBroadcast (true);
        }
    }

    /**************************\
     *  Bluetooth processing  *
    \**************************/

    private boolean setBtUUIDPfx (String text)
    {
        try {
            if (text.length () != 8) throw new Exception ("uuid prefix must be 8 hexadecimal digits");
            btServerUUID = UUID.fromString (text + btServerUUIDSuffix);
            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("btLisUUIDPfx", text);
            editr.apply ();
        } catch (Exception e) {
            mainActivity.showToast (e.getMessage ());
            return true;
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    public void updateConCount (final int cc)
    {
        mainActivity.runOnUiThread (new Runnable () {
            @Override
            public void run ()
            {
                conCountView.setText ("bt connections: " + cc);
            }
        });
    }
}
