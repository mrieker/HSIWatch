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
import android.util.Log;

import java.io.Closeable;

/**
 * Use Bluetooth or WiFi UDP to receive GPS location and status information.
 */
public abstract class ExternalGps implements GpsReceiver {
    protected boolean capable;
    private DecodeNMEA decodeNMEA;
    protected MainActivity mainActivity;
    private ReceiverThread receiverThread;
    protected StatusTextView statusView;

    protected abstract String getPeerName ();
    protected abstract Closeable openSocket () throws Exception;
    protected abstract String readSocket () throws Exception;

    public ExternalGps (MainActivity ma)
    {
        mainActivity = ma;
        decodeNMEA = new DecodeNMEA (ma);
        statusView = new StatusTextView (mainActivity);
    }

    @SuppressLint("MissingPermission")
    @Override  // GpsReceiver
    public void enterAmbient ()
    { }

    @SuppressLint("MissingPermission")
    @Override  // GpsReceiver
    public void exitAmbient ()
    { }

    @Override  // GpsReceiver
    public boolean startLocationSensor ()
    {
        decodeNMEA.locationEnabled = true;
        if (capable && (receiverThread == null)) {
            receiverThread = new ReceiverThread ();
        }
        return true;
    }

    @Override  // GpsReceiver
    public boolean startStatusSensor ()
    {
        decodeNMEA.statusEnabled = true;
        if (capable && (receiverThread == null)) {
            receiverThread = new ReceiverThread ();
        }
        return true;
    }

    @Override  // GpsReceiver
    public boolean stopLocationSensor ()
    {
        boolean loc = decodeNMEA.locationEnabled;
        decodeNMEA.locationEnabled = false;
        if (! decodeNMEA.statusEnabled && (receiverThread != null)) {
            receiverThread.kill ();
            receiverThread = null;
        }
        return loc;
    }

    @Override  // GpsReceiver
    public boolean stopStatusSensor ()
    {
        boolean sts = decodeNMEA.statusEnabled;
        decodeNMEA.statusEnabled = false;
        if (! decodeNMEA.locationEnabled && (receiverThread != null)) {
            receiverThread.kill ();
            receiverThread = null;
        }
        return sts;
    }

    // restart thread after a change of something like port number
    protected void restartThread ()
    {
        if (receiverThread != null) {
            receiverThread.kill ();
            receiverThread = new ReceiverThread ();
        }
    }

    // read NMEA messages from bluetooth device, call processIncomingNMEA() for each received
    // started with either start{Location,Status}Sensor()
    // stopped when socket closed by both stop{Location,Status}Sensor()
    private class ReceiverThread extends Thread {
        private boolean killed;
        private Closeable socket;
        private Exception exception;
        private String peerName;

        public ReceiverThread ()
        {
            peerName = getPeerName ();
            start ();
        }

        public void kill ()
        {
            killed = true;
            try { socket.close (); } catch (Exception ignored) { }
            try { join (); } catch (InterruptedException ignored) { }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void run ()
        {
            setName ("ExternalGps Receiver");
            try {
                statusView.setText ("[" + peerName + "] connecting");
                socket = openSocket ();

                /*
                 * Process incoming messages.
                 */
                statusView.setText ("[" + peerName + "] listening");
                int n = 0;
                for (String line; ! killed && (line = readSocket ()) != null;) {
                    statusView.setText ("[" + peerName + "] received " + ++ n);
                    decodeNMEA.gotLine (line);
                }
            } catch (Exception e) {
                if (! killed) {
                    Log.e (MainActivity.TAG, "error receiving packet", e);
                    exception = e;
                }
            } finally {
                try { socket.close (); } catch (Exception ignored) { }
                if (exception != null) {
                    mainActivity.runOnUiThread (new Runnable () {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run ()
                        {
                            statusView.setText ("[" + peerName + "] error " + exception.getMessage ());
                            mainActivity.showToastLong ("error receiving packet " +
                                    exception.getMessage ());
                        }
                    });
                } else {
                    statusView.setText ("[" + peerName + "] disconnected");
                }
            }
        }
    }
}
