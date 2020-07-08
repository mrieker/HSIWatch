//    Copyright (C) 2015, Mike Rieker, Beverly, MA USA
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

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.sender.ReportSenderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import androidx.annotation.NonNull;

/**
 * Captures any crashes and forwards the stack dump info to the web server
 * when the web server is available.
 */

// https://github.com/ACRA/acra/wiki/BasicSetup
// https://github.com/ACRA/acra/wiki/AdvancedUsage#reports-content
@ReportsCrashes (
        reportSenderFactoryClasses = { AcraApplication.YourOwnSenderFactory.class },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
)
public class AcraApplication extends Application {
    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init (this);
    }

    /**
     * Try to send any pending ACRA report files.
     */
    public static void sendReports (Context context)
    {
        SendReportThread srt = new SendReportThread ();
        srt.dir = context.getFilesDir ();
        srt.start ();
    }

    private static class SendReportThread extends Thread {
        public File dir;

        @Override
        public void run ()
        {
            setName ("SendReportThread");
            Log.d (MainActivity.TAG, "SendReportThread starting");
            boolean foundsomething;
            do {
                foundsomething = false;

                /*
                 * Loop through all files in the data directory.
                 */
                File[] filelist = dir.listFiles ();
                for (File file : filelist) {

                    /*
                     * Delete any files ending in .acra.gz.tmp cuz they weren't finished.
                     */
                    String path = file.getPath ();
                    if (path.endsWith (".acra.gz.tmp")) {
                        Lib.Ignored (file.delete ());
                        continue;
                    }

                    /*
                     * Send and delete any files ending in .acra.gz to acraupload.php script.
                     */
                    if (! path.endsWith (".acra.gz")) continue;
                    foundsomething = true;
                    tryToSendReport (file);

                    /*
                     * Make sure file is deleted if more than a week old.
                     */
                    String name = file.getName ();
                    boolean stale;
                    try {
                        int i = name.indexOf ('.');
                        long thenms = Long.parseLong (name.substring (0, i));
                        stale = System.currentTimeMillis () - thenms > 7L * 86400L * 1000L;
                    } catch (Exception e) {
                        stale = true;
                    }
                    if (stale) {
                        Log.i (MainActivity.TAG, "deleting stale acra " + path);
                    }
                }

                /*
                 * Try again later in case we weren't able to send this time.
                 */
                try { Thread.sleep (600000); } catch (InterruptedException ignored) { }
            } while (foundsomething);
            Log.d (MainActivity.TAG, "SendReportThread exiting");
        }
    }

    // https://github.com/ACRA/acra/wiki/Report-Destinations

    public static class YourOwnSender implements ReportSender {
        @Override
        public void send (@NonNull Context context, @NonNull CrashReportData report)
                throws ReportSenderException {
            try {
                long nowms = System.currentTimeMillis ();
                File dir   = context.getFilesDir ();
                File perm  = new File (dir, nowms + ".acra.gz");
                File temp  = new File (dir, nowms + ".acra.gz.tmp");
                try (PrintWriter pw = new PrintWriter (new GZIPOutputStream (new FileOutputStream (temp)))) {
                    pw.println (nowms);
                    for (ReportField reportField : report.keySet ()) {
                        String reportValue = report.get (reportField);
                        pw.println ("::" + reportField.toString () + "::" + reportValue);
                    }
                }
                if (!temp.renameTo (perm)) {
                    throw new IOException ("error renaming " + temp.getPath () + " to " + perm.getPath ());
                }
                Log.i (MainActivity.TAG, "created ACRA report " + perm.getPath ());
                tryToSendReport (perm);
            } catch (Exception e) {
                throw new ReportSenderException ("error creating ACRA file", e);
            }
        }
    }

    public static class YourOwnSenderFactory implements ReportSenderFactory {
        @NonNull
        public ReportSender create (@NonNull Context context, @NonNull ACRAConfiguration config)
        {
            return new YourOwnSender ();
        }
    }

    private static void tryToSendReport (File file)
    {
        Log.i (MainActivity.TAG, "sending ACRA report " + file.getPath ());
        long len = file.length ();
        try {
            URL url = new URL ("https://www.outerworldapps.com/WairToNow/acraupload.php");
            HttpURLConnection httpCon = (HttpURLConnection)url.openConnection ();
            try {
                httpCon.setRequestMethod ("POST");
                httpCon.setDoOutput (true);
                httpCon.setFixedLengthStreamingMode ((int) len);
                InputStream is = new FileInputStream (file);
                OutputStream os = httpCon.getOutputStream ();
                byte[] buf = new byte[4096];
                int rc;
                while ((rc = is.read (buf)) > 0) {
                    os.write (buf, 0, rc);
                }
                is.close ();
                rc = httpCon.getResponseCode ();
                if (rc != HttpURLConnection.HTTP_OK) {
                    throw new IOException ("http response code " + rc);
                }
                Log.i (MainActivity.TAG, "sent ACRA report " + file.getPath ());
                Lib.Ignored (file.delete ());
            } finally {
                httpCon.disconnect ();
            }
        } catch (Exception e) {
            Log.e (MainActivity.TAG, "exception sending ACRA report " + file.getPath (), e);
        }
    }
}
