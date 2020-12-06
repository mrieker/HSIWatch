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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;

/**
 * Use Bluetooth to receive GPS location and status information.
 */
public class BluetoothGps extends ExternalGps {
    private final static String CLICKTOSEL = "click to select...";
    private final static String SPPUUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BufferedReader btReader;
    private SharedPreferences prefs;
    private TextView btdevidView;
    private TextView btuuidView;
    private View[] paramViews;

    public BluetoothGps (MainActivity ma)
    {
        super (ma);
        prefs = ma.getPreferences (Context.MODE_PRIVATE);
    }

    @SuppressLint("SetTextI18n")
    @Override  // GpsReceiver
    public View[] getParamViews ()
    {
        if (paramViews == null) {
            if (BluetoothAdapter.getDefaultAdapter () == null) {
                TextView nobt = new TextView (mainActivity);
                nobt.setText ("no bluetooth on device");
                return new View[] { nobt };
            }

            // set up device, uuid, secure, status lines
            btdevidView = new TextView (mainActivity);
            btuuidView = new TextView (mainActivity);
            statusView = new StatusTextView (mainActivity);
            paramViews = new View[] { btdevidView, btuuidView, statusView };

            // set up click listeners for inputs
            btdevidView.setOnClickListener (btdevidClickListener);
            btuuidView.setOnClickListener (btdevidClickListener);
        }

        // try to select previously selected device, uuid, secure
        String btdevidstr = prefs.getString ("bluetoothgps.devident", "");
        if (btdevidstr.equals ("")) {
            btdevidView.setText (CLICKTOSEL);
            btuuidView.setText ("");
        } else {
            try {
                getBtDev (btdevidstr);
                String btuuidstr = prefs.getString ("bluetoothgps." + btdevidstr + ".uuid", "");
                btdevidView.setText (spliceUpDevid (btdevidstr));
                btuuidView.setText (spliceUpUuid (btuuidstr));
                capable = true;
                restartThread ();
            } catch (Exception e) {
                statusView.setText (e.getMessage ());
            }
        }

        return paramViews;
    }

    private final View.OnClickListener btdevidClickListener = new View.OnClickListener () {
        @Override
        public void onClick (View v)
        {
            btdevidClicked ();
        }
    };

    // device name below the '(*) Bluetooth' radio button was clicked on
    // open a page to select which bluetooth device to connect to
    private void btdevidClicked ()
    {
        // get list of paired bluetooth devices
        String[] devids;
        try {
            devids = getBtDevIdents ();
        } catch (Exception e) {
            statusView.setText (e.getMessage ());
            return;
        }

        // build list of radio buttons one per possible device
        final LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        @SuppressLint("InflateParams")
        View gpsbtdevpage = layoutInflater.inflate (R.layout.gpsbtdev_page, null);
        Button gpsbtdevback = gpsbtdevpage.findViewById (R.id.gpsbtdevBack);
        gpsbtdevback.setOnClickListener (mainActivity.backButtonListener);
        RadioGroup gpsBtDevs = gpsbtdevpage.findViewById (R.id.gpsBtDevs);
        String selecteddevid = unspliceDevid (btdevidView.getText ().toString ());
        int i = 0;
        for (String devid : devids) {
            RadioButton gpsBtDevBut = new RadioButton (mainActivity);
            gpsBtDevBut.setText (spliceUpDevid (devid));
            gpsBtDevBut.setChecked (devid.equals (selecteddevid));
            gpsBtDevBut.setOnClickListener (btdevidSelectListener);
            gpsBtDevs.addView (gpsBtDevBut, i ++);
        }

        // display device selection radio button page
        mainActivity.showMainPage (gpsbtdevpage);
    }

    // a device was selected from the device radio button page
    private final View.OnClickListener btdevidSelectListener = new View.OnClickListener () {
        @Override
        public void onClick (View v)
        {
            mainActivity.onBackPressed ();
            btdevidSelected (unspliceDevid (((RadioButton) v).getText ().toString ()));
        }
    };

    @SuppressLint("SetTextI18n")
    private void btdevidSelected (String devid)
    {
        // don't start a new ReceiverThread until user selects UUID
        capable = false;
        restartThread ();  // stops the thread if any cuz capable is false

        // display selected device id string just below '(*) Bluetooth' radio button
        btdevidView.setText (spliceUpDevid (devid));

        // get corresponding bluetooth device
        BluetoothDevice btdev;
        try {
            btdev = getBtDev (devid);
        } catch (Exception e) {
            statusView.setText (e.getMessage ());
            return;
        }

        // pretend UUID box was clicked on to select UUID for the device
        btuuidView.setText (CLICKTOSEL);
        btuuidClicked (btdev);
    }

    // UUID box was clicked on to select UUID of the previously selected device
    private void btuuidClicked (BluetoothDevice btdev)
    {
        // Get cached list of UUIDs known for the device.
        // This works if the device is currently paired,
        // it does not matter if it is in radio range or not.
        ParcelUuid[] uuidArray = btdev.getUuids ();

        // If no cache, use the default SPPUUID.
        if ((uuidArray == null) || (uuidArray.length == 0)) {
            String devid = btIdentString (btdev);
            Log.w (MainActivity.TAG, "bt device " + devid + " has no uuids");
            uuidArray = new ParcelUuid[] { ParcelUuid.fromString (SPPUUID) };
        }

        // display page to select UUID from
        LayoutInflater layoutInflater = mainActivity.getLayoutInflater ();
        @SuppressLint("InflateParams")
        View gpsbtuuidpage = layoutInflater.inflate (R.layout.gpsbtuuid_page, null);
        Button gpsbtuuidback = gpsbtuuidpage.findViewById (R.id.gpsbtuuidBack);
        gpsbtuuidback.setOnClickListener (mainActivity.backButtonListener);
        RadioGroup gpsBtUUIDs = gpsbtuuidpage.findViewById (R.id.gpsBtUUIDs);
        String selecteduuid = unspliceUuid (btuuidView.getText ().toString ());
        int i = 0;
        for (ParcelUuid uuid : uuidArray) {
            String uuidstr = uuid.toString ();
            RadioButton gpsBtuuidBut = new RadioButton (mainActivity);
            if (uuidstr.equalsIgnoreCase (SPPUUID)) {
                gpsBtuuidBut.setTextColor (0xFFFFFFAA);
            }
            gpsBtuuidBut.setText (spliceUpUuid (uuidstr));
            gpsBtuuidBut.setChecked (uuidstr.equals (selecteduuid));
            gpsBtuuidBut.setOnClickListener (btuuidListener);
            gpsBtUUIDs.addView (gpsBtuuidBut, i ++);
        }

        mainActivity.showMainPage (gpsbtuuidpage);
    }

    // a UUID radio button was clicked, save the selected UUID and connect to device/UUID
    private final View.OnClickListener btuuidListener = new View.OnClickListener () {
        @Override
        public void onClick (View v)
        {
            btuuidSelected (unspliceUuid (((RadioButton) v).getText ().toString ()));
        }
    };

    private void btuuidSelected (String uuid)
    {
        mainActivity.onBackPressed ();
        btuuidView.setText (spliceUpUuid (uuid));
        capable = true;
        restartThread ();
    }

    private static String spliceUpDevid (String devid)
    {
        if (devid == null) return "";
        int len = devid.length ();
        if (len < 20) return devid;
        if ((devid.charAt (len - 3) != ':') || (devid.charAt (len - 6) != ':') ||
                (devid.charAt (len - 9) != ':') || (devid.charAt (len - 12) != ':') ||
                (devid.charAt (len - 15) != ':') || (devid.charAt (len - 18) != ' ')) return devid;
        return devid.substring (0, len - 18) + "\n " + devid.substring (len - 17);
    }

    private static String unspliceDevid (String devid)
    {
        return devid.replace ("\n ", " ");
    }

    // splice up a uuid string into two lines
    // so it fits round watch face better
    private static String spliceUpUuid (String uuid)
    {
        if (uuid == null) return "";
        if (uuid.length () != 36) return uuid;
        if (uuid.charAt (18) != '-') return uuid;
        return uuid.substring (0, 18) + "\n " + uuid.substring (19);
    }

    // unsplice the uuid string
    private static String unspliceUuid (String uuid)
    {
        if (uuid.equals (CLICKTOSEL)) return "";
        return uuid.replace ("\n ", "-");
    }

    @Override  // ExternalGps
    protected @NonNull String typestr () { return "Bluetooth"; }

    /**
     * Connect to GPS receiver device using Bluetooth.
     */
    @Override  // ExternalGps
    protected @NonNull Closeable openSocket ()
            throws Exception
    {
        /*
         * Get and decode device and UUID from selector boxes.
         */
        String devidstr = unspliceDevid (btdevidView.getText ().toString ());
        String uuidstr = unspliceUuid (btuuidView.getText ().toString ());
        BluetoothDevice btdev = getBtDev (devidstr);
        UUID btuuid = UUID.fromString (uuidstr);

        /*
         * Connect and get input stream.  This will block for a second or two.
         * Try secure then insecure.
         */
        BluetoothSocket socket;
        try {
            socket = btdev.createRfcommSocketToServiceRecord (btuuid);
            socket.connect ();
        } catch (IOException ioe) {
            try {
                socket = btdev.createInsecureRfcommSocketToServiceRecord (btuuid);
                socket.connect ();
            } catch (IOException ioe2) {
                throw new ConnectException (ioe2);
            }
        }

        // connect successful, save bluetooth parameters for next time
        SharedPreferences.Editor editr = prefs.edit ();
        editr.putString ("bluetoothgps.devident", devidstr);
        editr.putString ("bluetoothgps." + devidstr + ".uuid", uuidstr);
        editr.apply ();

        // set up to read from stream
        btReader = new BufferedReader (new InputStreamReader (socket.getInputStream ()));
        return btReader;
    }

    private static class ConnectException extends Exception {
        public ConnectException (IOException ioe)
        {
            super (ioe);
        }
    }

    /**
     * Read an NMEA line from GPS receiver using Bluetooth.
     */
    @Override  // ExternalGps
    protected String readSocket ()
            throws Exception
    {
        return btReader.readLine ();
    }

    @Override  // ExternalGps
    protected String receiveException (Exception e)
    {
        if (e instanceof ConnectException) {
            return
                    "connect error\n" +
                    "make sure app (eg GPSBlue)\n" +
                    "running on selected device";
        }
        return e.getMessage ();
    }

    /**
     * Get bluetooth device for the given ident string.
     */
    private static @NonNull BluetoothDevice getBtDev (String btdevidstr)
            throws Exception
    {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter ();
        if (btAdapter == null) {
            throw new Exception ("no bluetooth adapter");
        }
        if (! btAdapter.isEnabled ()) {
            throw new Exception ("bluetooth not enabled");
        }
        Set<BluetoothDevice> devs = btAdapter.getBondedDevices ();
        if (devs == null) {
            throw new Exception ("unable to get list of paired devices");
        }
        for (BluetoothDevice dev : devs) {
            if (btIdentString (dev).equals (btdevidstr)) return dev;
        }
        throw new Exception ("device not found");
    }

    /**
     * Get list of bluetooth devices we are paired to.
     */
    private static String[] getBtDevIdents ()
            throws Exception
    {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter ();
        if (btAdapter == null) {
            throw new Exception ("no bluetooth adapter");
        }
        if (! btAdapter.isEnabled ()) {
            throw new Exception ("bluetooth not enabled");
        }
        Set<BluetoothDevice> devs = btAdapter.getBondedDevices ();
        if (devs == null) {
            throw new Exception ("unable to get list of paired devices");
        }
        String[] btdevidents = new String[devs.size()];
        int i = 0;
        for (BluetoothDevice dev : devs) {
            btdevidents[i++] = btIdentString (dev);
        }
        return btdevidents;
    }

    /**
     * Get user-friendly name string for device that is also unique.
     */
    private static String btIdentString (BluetoothDevice dev)
    {
        return dev.getName () + " " + dev.getAddress ();
    }
}
