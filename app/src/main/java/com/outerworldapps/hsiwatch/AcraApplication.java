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
        try {
            ACRA.init (this);
        } catch (Exception e) {
            /*
12-05 17:05:20.303 E/ACRA    (18903): ACRA caught a RuntimeException for com.outerworldapps.hsiwatch
12-05 17:05:20.303 E/ACRA    (18903): java.lang.RuntimeException: Unable to instantiate application com.outerworldapps.hsiwatch.AcraApplication: java.lang.IllegalStateException: Not allowed to start service Intent { cmp=com.outerworldapps.hsiwatch/org.acra.sender.SenderService (has extras) }: app is in background uid UidRecord{a8c00b6 u0a70 TPSL idle procs:1 seq(0,0,0)}
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.LoadedApk.makeApplication(LoadedApk.java:1069)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.ActivityThread.handleBindApplication(ActivityThread.java:5853)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.ActivityThread.access$1100(ActivityThread.java:200)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1651)
12-05 17:05:20.303 E/ACRA    (18903):   at android.os.Handler.dispatchMessage(Handler.java:106)
12-05 17:05:20.303 E/ACRA    (18903):   at android.os.Looper.loop(Looper.java:193)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.ActivityThread.main(ActivityThread.java:6680)
12-05 17:05:20.303 E/ACRA    (18903):   at java.lang.reflect.Method.invoke(Native Method)
12-05 17:05:20.303 E/ACRA    (18903):   at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:493)
12-05 17:05:20.303 E/ACRA    (18903):   at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)
12-05 17:05:20.303 E/ACRA    (18903): Caused by: java.lang.IllegalStateException: Not allowed to start service Intent { cmp=com.outerworldapps.hsiwatch/org.acra.sender.SenderService (has extras) }: app is in background uid UidRecord{a8c00b6 u0a70 TPSL idle procs:1 seq(0,0,0)}
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.ContextImpl.startServiceCommon(ContextImpl.java:1577)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.ContextImpl.startService(ContextImpl.java:1532)
12-05 17:05:20.303 E/ACRA    (18903):   at android.content.ContextWrapper.startService(ContextWrapper.java:664)
12-05 17:05:20.303 E/ACRA    (18903):   at org.acra.sender.SenderServiceStarter.startService(SenderServiceStarter.java:43)
12-05 17:05:20.303 E/ACRA    (18903):   at org.acra.util.ApplicationStartupProcessor.sendApprovedReports(ApplicationStartupProcessor.java:75)
12-05 17:05:20.303 E/ACRA    (18903):   at org.acra.ACRA.init(ACRA.java:230)
12-05 17:05:20.303 E/ACRA    (18903):   at org.acra.ACRA.init(ACRA.java:156)
12-05 17:05:20.303 E/ACRA    (18903):   at org.acra.ACRA.init(ACRA.java:139)
12-05 17:05:20.303 E/ACRA    (18903):   at com.outerworldapps.hsiwatch.AcraApplication.attachBaseContext(AcraApplication.java:69)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.Application.attach(Application.java:212)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.Instrumentation.newApplication(Instrumentation.java:1121)
12-05 17:05:20.303 E/ACRA    (18903):   at android.app.LoadedApk.makeApplication(LoadedApk.java:1061)
12-05 17:05:20.303 E/ACRA    (18903):   ... 9 more
             */
            Log.e (MainActivity.TAG, "error initializing acra", e);
        }
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
                if (filelist != null) for (File file : filelist) {

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
            URL url = new URL (DownloadThread.baseurl + "/acraupload.php");
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
