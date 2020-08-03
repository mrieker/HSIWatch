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
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;

/**
 * Use Bluetooth to receive GPS location and status information.
 */
public class BluetoothGps extends ExternalGps {
    private final static String SPPUUID = "00001101-0000-1000-8000-00805f9b34fb";

    private BufferedReader btReader;
    private CheckBox btsecureCkBox;
    private SharedPreferences prefs;
    private TextArraySpinner btdevidView;
    private TextArraySpinner btuuidView;
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
            btdevidView = new TextArraySpinner (mainActivity);
            btuuidView = new TextArraySpinner (mainActivity);
            btsecureCkBox = new CheckBox (mainActivity);
            statusView = new StatusTextView (mainActivity);

            btdevidView.setOnItemSelectedListener (devidSelected);
            btuuidView.setOnItemSelectedListener (uuidSelected);
            btsecureCkBox.setOnClickListener (secureSelected);

            btsecureCkBox.setText ("secure");
            statusView.setText ("initialized");

            paramViews = new View[] { btdevidView, btuuidView, btsecureCkBox, statusView };
        }

        // scan bluetooth for list of paired devices
        // if failed, wipe out what we built and return error message
        //            and let the next call to getParamViews() scan again
        //            in case they come back after turning bluetooth on
        String poperr = populateBtDevSelector ();
        if (poperr != null) {
            paramViews = null;
            TextView nobtview = new TextView (mainActivity);
            nobtview.setText (poperr);
            return new View[] { nobtview };
        }

        // got list of paired devices so we must be bluetooth capable
        capable = true;

        // try to select previously selected device, uuid, secure
        String btdevidstr = prefs.getString ("bluetoothgps.devident", "");
        try {
            BluetoothDevice btdev = getBtDev (btdevidstr);
            if (btdevidView.setText (btdevidstr)) {
                populateBtUUIDSelector (btdev);
                String btuuidstr = prefs.getString ("bluetoothgps." + btdevidstr + ".uuid", SPPUUID);
                boolean btsecure = prefs.getBoolean ("bluetoothgps." + btdevidstr + ".secure", false);
                btuuidView.setText (btuuidstr);
                btsecureCkBox.setChecked (btsecure);
            }
        } catch (Exception ignored) { }

        return paramViews;
    }

    // populate the bluetooth device name spinner with paired device names
    private String populateBtDevSelector ()
    {
        btdevidView.setTitle ("bluetooth device");
        String[] labels;
        try {
            labels = getBtDevIdents ();
        } catch (Exception e) {
            return e.getMessage ();
        }
        btdevidView.setLabels (labels, null, "(select)", null);
        btdevidView.setIndex (TextArraySpinner.NOTHING);
        return null;
    }

    // called when device selected from spinner
    private final TextArraySpinner.OnItemSelectedListener devidSelected =
            new TextArraySpinner.OnItemSelectedListener () {
        @Override
        public boolean onItemSelected (View view, int index, String btdevidstr)
        {
            try {
                populateBtUUIDSelector (getBtDev (btdevidstr));
            } catch (Exception e) {
                Log.w (MainActivity.TAG, "error looking up " + btdevidstr, e);
                mainActivity.showToast (btdevidstr + ": " + e.getMessage());
                return false;
            }

            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("bluetoothgps.devident", btdevidstr);
            editr.apply ();

            String btuuidstr = prefs.getString ("bluetoothgps." + btdevidstr + ".uuid", SPPUUID);
            boolean btsecure = prefs.getBoolean ("bluetoothgps." + btdevidstr + ".secure", false);
            btuuidView.setText (btuuidstr);
            btsecureCkBox.setChecked (btsecure);

            restartThread ();

            return true;
        }

        @Override
        public boolean onNegativeClicked (View view)
        {
            return false;
        }

        @Override
        public boolean onPositiveClicked (View view)
        {
            return false;
        }
    };

    // populate uuid spinner with UUIDs for the given device
    private void populateBtUUIDSelector (BluetoothDevice btdev)
    {
        /*
         * Get cached list of UUIDs known for the device.
         * This works if the device is currently paired,
         * it does not matter if it is in radio range or not.
         */
        String btdevname = btIdentString (btdev);
        ParcelUuid[] uuidArray = btdev.getUuids ();

        /*
         * If no cache, use the default SPPUUID.
         */
        if ((uuidArray == null) || (uuidArray.length == 0)) {
            Log.w (MainActivity.TAG, "bt device " + btdevname + " has no uuids");
            uuidArray = new ParcelUuid[] { ParcelUuid.fromString (SPPUUID) };
        }

        /*
         * Set that list up as selections on the spinner button.
         */
        int nuuids = uuidArray.length;
        String[] uuidsWithDef = new String[nuuids];
        int i = 0;
        int defi = TextArraySpinner.NOTHING;
        for (ParcelUuid uuid : uuidArray) {
            String uuidstr = uuid.toString ();
            if (uuidstr.equals (SPPUUID)) {
                defi = i;
            }
            uuidsWithDef[i] = uuidstr;
            i ++;
        }

        /*
         * Write the uuids to the buttons.
         */
        btuuidView.setTitle ("Select UUID for " + btdevname);
        btuuidView.setLabels (uuidsWithDef, null, "(select)", null);
        btuuidView.setIndex (defi);
    }

    // called when uuid selected from spinner
    private final TextArraySpinner.OnItemSelectedListener uuidSelected =
            new TextArraySpinner.OnItemSelectedListener () {
        @Override
        public boolean onItemSelected (View view, int index, String btuuidstr)
        {
            String btdevidstr = btdevidView.getText ();

            SharedPreferences.Editor editr = prefs.edit ();
            editr.putString ("bluetoothgps." + btdevidstr + ".uuid", btuuidstr);
            editr.apply ();

            restartThread ();

            return true;
        }

        @Override
        public boolean onNegativeClicked (View view)
        {
            return false;
        }

        @Override
        public boolean onPositiveClicked (View view)
        {
            return false;
        }
    };

    // secure checked or unchecked
    private final View.OnClickListener secureSelected =
            new View.OnClickListener () {
        @Override
        public void onClick (View v)
        {
            String btdevidstr = btdevidView.getText ();

            boolean btsecure = btsecureCkBox.isChecked ();
            SharedPreferences.Editor editr = prefs.edit ();
            editr.putBoolean ("bluetoothgps." + btdevidstr + ".secure", btsecure);
            editr.apply ();

            restartThread ();
        }
    };

    /**
     * Get peer name for messages.
     */
    @Override  // ExternalGps
    protected String getPeerName ()
    {
        return btdevidView.getText () + " " + btuuidView.getText () +
                (btsecureCkBox.isChecked () ? " secure" : " insec");
    }

    /**
     * Connect to GPS receiver device using Bluetooth.
     */
    @Override  // ExternalGps
    protected Closeable openSocket ()
            throws Exception
    {
        BluetoothDevice btDevice = getBtDev (btdevidView.getText ());
        UUID btUUID = UUID.fromString (btuuidView.getText ());
        boolean btSecure = btsecureCkBox.isChecked ();

        /*
         * Get socket, secure or insecure according to checkbox.
         */
        BluetoothSocket socket;
        if (btSecure) {
            socket = btDevice.createRfcommSocketToServiceRecord (btUUID);
        } else {
            socket = btDevice.createInsecureRfcommSocketToServiceRecord (btUUID);
        }

        /*
         * Connect and get input stream.  This will block until it connects.
         */
        socket.connect ();

        btReader = new BufferedReader (new InputStreamReader (socket.getInputStream ()));

        return btReader;
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

    /**
     * Get bluetooth device for the given ident string.
     */
    private static BluetoothDevice getBtDev (String btdevidstr)
            throws Exception
    {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter ();
        if (btAdapter == null) {
            throw new Exception ("bluetooth not supported on this device");
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
            throw new Exception ("bluetooth not supported on this device");
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
