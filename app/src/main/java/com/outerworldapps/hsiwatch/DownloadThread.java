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
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import androidx.annotation.NonNull;

/**
 * Thread what downloads latest database.
 */
public class DownloadThread implements DatabaseErrorHandler, Runnable {
    public final static String baseurl = "https://www.outerworldapps.com/WairToNow";

    private final static int NWARNDAYS = 3;
    private final static int throtkbps = 1024;  // throttle download to 1MByte per second

    @SuppressWarnings("deprecation")
    private final static long oldversiontime = Date.UTC (2020-1900, 7-1, 30, 15, 0, 0);

    private boolean threadrunning;      // gui thread only
    private long lastdownloadmsgat;     // gui thread only
    private long latestdb;              // gui thread only
    private final MainActivity mainActivity;
    private SQLiteDatabase sqldb;       // gui thread only
    private final String dbdir;
    public  String dbexp;               // gui thread only; yyyy-mm-dd
    private String dbpath;              // gui thread only

    public DownloadThread (MainActivity ma)
    {
        mainActivity = ma;
        dbdir = mainActivity.getNoBackupFilesDir ().getAbsolutePath ();
        dbexp = "(none)";
    }

    // delete all files from the database directory
    public void deleteAll ()
    {
        File[] files = new File (dbdir).listFiles ();
        if (files != null) for (File file : files) {
            Lib.Ignored (file.delete ());
        }
    }

    /**
     * Open database
     * If not downloaded,
     *   display message
     *   start downloading in background
     *   return null
     * Called in GUI thread only.
     */
    public SQLiteDatabase getSqlDB ()
    {
        if (sqldb == null) {

            // find latest database we have, if any
            dbpath = null;
            latestdb = 0;
            SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMdd", Locale.US);
            sdf.setTimeZone (TimeZone.getTimeZone ("UTC"));
            //noinspection ConstantConditions
            for (File oldfile : new File (dbdir).listFiles ()) {
                String oldname = oldfile.getName ();
                if (oldname.startsWith ("wayptabbs_") && oldname.endsWith (".db")) {
                    if (oldfile.lastModified () < oldversiontime) {
                        // delete database that doesn't have rwy_length,rwy_width
                        //noinspection ResultOfMethodCallIgnored
                        oldfile.delete ();
                        continue;
                    }
                    try {
                        String expstr = oldname.substring (10, oldname.length () - 3);
                        @SuppressWarnings("ConstantConditions")
                        long dbexp = sdf.parse (expstr).getTime ();
                        if (latestdb < dbexp) {
                            latestdb = dbexp;
                            dbpath   = oldfile.getPath ();
                        }
                    } catch (Exception e) {
                        Log.w (MainActivity.TAG, "error decoding expdate from " + oldname, e);
                    }
                }
            }

            // if first time, start downloading
            if (dbpath == null) upddb ();

            // otherwise, go with what we got
            else openDatabase ();
        }
        return sqldb;
    }

    /**
     * Get color for UPDDB button based on database expiration date.
     * Called in GUI thread only.
     */
    public int buttonColor ()
    {
        long now = System.currentTimeMillis ();
        if (now > latestdb) return Color.RED;
        if (latestdb - now < NWARNDAYS * 24L * 60L * 60L * 1000L) return Color.YELLOW;
        return Color.GREEN;
    }

    /**
     * Update database in background thread.
     * Called in GUI thread only.
     */
    public void upddb ()
    {
        if (! threadrunning) {
            threadrunning = true;
            if (sqldb == null) {
                mainActivity.showToast (
                        "downloading database\n" +
                        "takes a few minutes");
                mainActivity.showToastLong (
                        "progress shown on MENU\u25B7UPDDB button\n" +
                        "or just wait...");
            } else {
                mainActivity.showToast ("updating database");
            }
            lastdownloadmsgat = System.currentTimeMillis ();
            new Thread (this).start ();
        }
    }

    /**
     * Runs in its own thread to download database in background.
     * Notifies user when download complete or if error.
     */
    @Override
    public void run ()
    {
        try {

            // ask server what the latest database is
            BufferedReader br = new BufferedReader (new InputStreamReader (httpGetURL ("filelist.php?undername=Wayptabbs")));
            String wpgzname = br.readLine ();
            if (wpgzname == null) throw new EOFException ("error reading wayptabbs name");
            br.close ();
            if (! wpgzname.startsWith ("datums/wayptabbs_") || ! wpgzname.endsWith (".db.gz")) {
                throw new IOException ("bad wayptabbs database name " + wpgzname);
            }
            String wpdbname = wpgzname.substring (7, wpgzname.length () - 3);
            final File permfile = new File (dbdir + "/" + wpdbname);
            if (permfile.exists ()) {
                mainActivity.runOnUiThread (new Runnable () {
                    @Override
                    public void run ()
                    {
                        threadrunning = false;
                        mainActivity.showToast ("database up to date");
                        dbpath = permfile.getPath ();
                        openDatabase ();
                    }
                });
            } else {

                // we don't already have it, download into temp file
                long lastShownPct = -1;
                File tempfile = new File (dbdir + "/" + wpdbname + ".tmp");
                BulkDownload bulkDownload = new BulkDownload (wpgzname, dbdir + "/" + wpdbname + ".gz.tmp");
                try (InputStream gzis = new GZIPInputStream (bulkDownload)) {
                    try (OutputStream fos = new FileOutputStream (tempfile)) {
                        byte[] buf = new byte[8192];
                        for (int rc; (rc = gzis.read (buf)) > 0; ) {
                            long percent = bulkDownload.bytesread * 100 / bulkDownload.filesize;
                            if (lastShownPct != percent) {
                                lastShownPct = percent;
                                final String pctstr = percent + "%";
                                mainActivity.runOnUiThread (new Runnable () {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run ()
                                    {
                                        String text = mainActivity.menuMainPage.upddbButton.getText ().toString ();
                                        int i = text.indexOf ('\n');
                                        if (i >= 0) text = text.substring (0, i);
                                        text += '\n' + pctstr;
                                        mainActivity.menuMainPage.upddbButton.setText (text);
                                        if (sqldb == null) {
                                            long now = System.currentTimeMillis ();
                                            if (now - lastdownloadmsgat > 10000) {
                                                lastdownloadmsgat = now;
                                                mainActivity.showToast ("download " + pctstr);
                                            }
                                        }
                                    }
                                });
                            }
                            fos.write (buf, 0, rc);
                        }
                    }

                    // make sure we got everything correct
                    bulkDownload.verifyHash ();
                }

                // download complete, rename temp file to perm file
                if (! tempfile.renameTo (permfile)) {
                    throw new IOException ("error renaming " + tempfile.getPath () + " to " + permfile.getParent ());
                }

                // delete any old files (including temps)
                //noinspection ConstantConditions
                for (File oldfile : new File (dbdir).listFiles ()) {
                    if (oldfile.getName ().startsWith ("wayptabbs_") && ! oldfile.equals (permfile)) {
                        //noinspection ResultOfMethodCallIgnored
                        oldfile.delete ();
                    }
                }

                // tell user database upload is complete
                mainActivity.runOnUiThread (new Runnable () {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run ()
                    {
                        threadFinished ();
                        if (sqldb == null) {
                            mainActivity.showToast ("database downloaded");
                            mainActivity.showToast (new Runnable () {
                                @Override
                                public void run ()
                                {
                                    // if first time user, switch the 'swipe up/swipe down' message
                                    mainActivity.navDialView.invalidate ();
                                }
                            });
                        } else {
                            mainActivity.showToast ("database updated");
                        }
                        dbpath = permfile.getPath ();
                        openDatabase ();
                    }
                });
            }
        } catch (final Exception e) {
            Log.w (MainActivity.TAG, "error downloading database", e);
            mainActivity.runOnUiThread (new Runnable () {
                @SuppressLint("SetTextI18n")
                @Override
                public void run ()
                {
                    threadFinished ();
                    mainActivity.showToastLong ("error downloading database\n" +
                            e.getMessage ());
                    mainActivity.showToastLong ("do MENU\u25B7UPDDB to try again");
                }
            });
        }
    }

    // GUI thread only
    private void threadFinished ()
    {
        threadrunning = false;
        String text = mainActivity.menuMainPage.upddbButton.getText ().toString ();
        int i = text.indexOf ('\n');
        if (i >= 0) {
            text = text.substring (0, i);
            mainActivity.menuMainPage.upddbButton.setText (text);
        }
    }

    // download file via bulkdownload method so we get a filesize up front
    private static class BulkDownload extends InputStream {
        private File tempFile;
        private InputStream inputStream;
        private MessageDigest digest;
        private RandomAccessFile tempRandFile;
        public  long bytesread;     // total bytes read so far, from old temp + network
        private long fileopened;    // time the network was connected up
        public  long filesize;      // total bytes of file, temp + network
        private long tempsize;      // size of old temp file

        // open stream to the given file on server
        //  input:
        //   sn = name of file on server
        //   tn = temp filename for intermediate download caching
        public BulkDownload (String sn, String tn)
                throws IOException, NoSuchAlgorithmException
        {
            // save raw data here in case aborted and restarted later
            tempFile = new File (tn);
            tempRandFile = new RandomAccessFile (tempFile, "rw");
            tempRandFile.seek (0);
            tempsize = tempRandFile.length ();

            // start reading from server
            // skip over what we have already downloaded if anything
            inputStream = new BufferedInputStream (httpGetURL ("bulkdownload.php?f0=" + sn + "&h0=md5&s0=" + tempsize));
            String nameLine = readLine ();
            if (! nameLine.startsWith ("@@name=")) throw new IOException ("missing @@name");
            if (! nameLine.substring (7).equals (sn)) throw new IOException ("missing servername");
            long skipped = 0;
            String sizeLine = readLine ();
            if (sizeLine.startsWith ("@@skip=")) {
                skipped = Long.parseLong (sizeLine.substring (7));
                sizeLine = readLine ();
            }
            if (skipped != tempsize) throw new IOException ("only skipped " + skipped + " of " + tempsize);
            if (! sizeLine.startsWith ("@@size=")) throw new IOException ("missing @@size");
            try {
                filesize = Long.parseLong (sizeLine.substring (7));
            } catch (NumberFormatException nfe) {
                throw new IOException ("bad filesize " + sizeLine.substring (7), nfe);
            }
            fileopened = System.currentTimeMillis ();
            digest = MessageDigest.getInstance ("MD5");
        }

        // throttle network reads (mostly for debugging)
        private void throttlereadwait ()
        {
            long mstotal = (bytesread - tempsize) / throtkbps;
            long now = System.currentTimeMillis ();
            long msactual = now - fileopened;
            if (mstotal > msactual) {
                try { Thread.sleep (mstotal - msactual); } catch (InterruptedException ignored) { }
            }
        }

        // read next byte, either from old temp file or from network
        @Override
        public int read ()
                throws IOException
        {
            if (bytesread >= filesize) return -1;
            int rc;
            if (bytesread < tempsize) {
                rc = tempRandFile.read ();
            } else {
                throttlereadwait ();
                rc = inputStream.read ();
                if (rc >= 0) tempRandFile.write (rc);
            }
            if (rc >= 0) {
                digest.update ((byte) rc);
                bytesread ++;
            }
            return rc;
        }

        // read next block, either from old temp file or from network
        @Override
        public int read (@NonNull byte[] buf, int ofs, int len)
                throws IOException
        {
            if (len > filesize - bytesread) len = (int) (filesize - bytesread);
            if (len <= 0) return -1;
            int rc;
            if (bytesread < tempsize) {
                if (len > tempsize - bytesread) len = (int) (tempsize - bytesread);
                rc = tempRandFile.read (buf, ofs, len);
            } else {
                throttlereadwait ();
                rc = inputStream.read (buf, ofs, len);
                if (rc > 0) tempRandFile.write (buf, ofs, rc);
            }
            if (rc > 0) {
                digest.update (buf, ofs, rc);
                bytesread += rc;
            }
            return rc;
        }

        // supposedly reached end of file, verify data
        public void verifyHash ()
                throws IOException
        {
            // delete temp file in case it all went wrong, it won't gum up a retry
            Lib.Ignored (tempFile.delete ());

            // make sure we got correct number of bytes
            if (bytesread != filesize) throw new IOException ("only read " + bytesread + " of " + filesize);

            // there should be an @@md5=<hashbytes>\n next from the server
            String md5Line = readLine ();
            if (! md5Line.startsWith ("@@md5=")) throw new IOException ("missing @@md5");

            // make sure it matches what we returned out all our read() calls
            String localmd5 = Lib.bytesToHex (digest.digest ());
            if (! md5Line.substring (6).equals (localmd5)) throw new IOException ("md5 mismatch");
        }

        // close network and temp files
        @Override
        public void close ()
                throws IOException
        {
            inputStream.close ();
            tempRandFile.close ();
        }

        // read string line from network up to newline
        private String readLine ()
                throws IOException
        {
            StringBuilder sb = new StringBuilder ();
            for (int rc; (rc = inputStream.read ()) != '\n';) {
                if (rc < 0) throw new EOFException ("eof downloading");
                sb.append ((char) rc);
            }
            return sb.toString ();
        }
    }

    /**
     * Open stream to read the given file from the server.
     */
    private static InputStream httpGetURL (String filename)
            throws IOException
    {
        URL url = new URL (baseurl + "/" + filename);
        Log.d (MainActivity.TAG, "downloading from " + url.toString ());
        int retry = 3;
        while (true) {
            try {
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection ();
                httpCon.setRequestMethod ("GET");
                httpCon.connect ();
                int rc = httpCon.getResponseCode ();
                if (rc != HttpURLConnection.HTTP_OK) {
                    throw new IOException ("http response code " + rc);
                }
                return httpCon.getInputStream ();
            } catch (IOException ioe) {
                if (-- retry <= 0) throw ioe;
                Log.w (MainActivity.TAG, "error downloading " + url.toString (), ioe);
            }
        }
    }

    /**
     * Open latest database.
     *  Input:
     *   dbpath = database file to open
     *  Output:
     *   dbexp = latestdb = database expiration
     *   sqldb = database handle
     * Called in GUI thread only
     */
    private void openDatabase ()
    {
        if (sqldb != null) {
            sqldb.close ();
            sqldb = null;
        }

        int i = dbpath.indexOf ("/wayptabbs_");
        if ((i < 0) || ! dbpath.substring (i + 19).equals (".db")) {
            throw new IllegalArgumentException ("bad dbpath " + dbpath);
        }

        SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMdd", Locale.US);
        sdf.setTimeZone (TimeZone.getTimeZone ("UTC"));
        try {
            //noinspection ConstantConditions
            latestdb = sdf.parse (dbpath.substring (i + 11, i + 19)).getTime ();
        } catch (Exception e) {
            throw new IllegalArgumentException ("bad dbpath " + dbpath, e);
        }

        dbexp = dbpath.substring (i + 11, i + 15) + "-" +
                dbpath.substring (i + 15, i + 17) + "-" +
                dbpath.substring (i + 17, i + 19);
        sqldb = SQLiteDatabase.openDatabase (dbpath, null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS,
                    this);

        int bc = buttonColor ();
        if ((mainActivity.menuMainPage != null) && (mainActivity.menuMainPage.upddbButton != null)) {
            mainActivity.menuMainPage.upddbButton.setTextColor (bc);
        }

        // we have a database, maybe warn about expiration
        switch (bc) {
            case Color.RED: {
                mainActivity.showToastLong ("database expired " + dbexp);
                mainActivity.showToastLong ("use MENU\u25B7UPDDB to update");
                break;
            }
            case Color.YELLOW: {
                mainActivity.showToastLong ("database will expire " + dbexp);
                mainActivity.showToastLong ("use MENU\u25B7UPDDB to update");
                break;
            }
        }
    }

    @Override  // DatabaseErrorHandler
    public void onCorruption (SQLiteDatabase dbobj)
    {
        mainActivity.runOnUiThread (new Runnable () {
            @Override
            public void run ()
            {
                mainActivity.showToastLong ("database corrupted, restart app to re-download");
                Lib.Ignored (new File (dbpath).delete ());
                sqldb = null;
            }
        });
    }
}
