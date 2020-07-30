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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Single instance contains all the street map tiles for the whole world.
 */
public class OpenStreetMap {
    private final static String TAG = "WairToNow";

    public  final static int BitmapSize = 256;  // all OSM tiles are this size
    public  final static int MAXZOOM = 17;
    private final static int LOG2PIXPERIN = 8;
    private final static long TILE_FILE_AGE_MS = 1000L*60*60*24*365;
    private final static String copyrtMessage = "\u00A9 OpenStreetMap contributors";

    private MainActivity mainActivity;
    private MainTileDrawer mainTileDrawer;

    public OpenStreetMap (MainActivity ma)
    {
        mainActivity = ma;
        mainTileDrawer = new MainTileDrawer ();
    }

    /**
     * Compute zoom level to be used for drawing.
     */
    public void ComputeZoom (PixelMapper pmap)
    {
        mainTileDrawer.ComputeZoom (pmap);
    }

    /**
     * Draw tiles to canvas corresponding to given lat/lons.
     * @param canvas = canvas that draws to the view
     * @param pmap = maps canvas/view pixels to lat/lon
     * @param inval = what to call in an arbitrary thread when a tile gets loaded
     */
    public void Draw (Canvas canvas, PixelMapper pmap, Invalidatable inval)
    {
        mainTileDrawer.Draw (canvas, pmap, inval);
    }

    /**
     * Screen is being closed, close all open bitmaps.
     */
    public void CloseBitmaps ()
    {
        mainTileDrawer.CloseBitmaps ();
    }

    /**
     * This class actually draws tiles on a canvas.
     */
    private class MainTileDrawer extends TileDrawer {
        private Canvas canvas;
        private float[] bitmappts = new float[8];
        private Invalidatable redrawView;
        private Matrix matrix = new Matrix ();
        private Paint copyrtBGPaint = new Paint ();
        private Paint copyrtTxPaint = new Paint ();
        private Path canvasclip = new Path ();

        private final HashMap<Long,TileBitmap> openedBitmaps = new HashMap<> ();
        private final HashMap<Long,TileBitmap> neededBitmaps = new HashMap<> ();
        private TileOpenerThread tileOpenerThread;

        private final HashMap<Long,Invalidatable> downloadBitmaps = new HashMap<> ();
        private TileDownloaderThread tileDownloaderThread;

        public MainTileDrawer ()
        {
            copyrtBGPaint.setColor (Color.WHITE);
            copyrtBGPaint.setStyle (Paint.Style.FILL_AND_STROKE);
            copyrtBGPaint.setTextAlign (Paint.Align.CENTER);
            copyrtTxPaint.setColor (Color.BLACK);
            copyrtTxPaint.setTextAlign (Paint.Align.CENTER);
        }

        public void Draw (Canvas canvas, PixelMapper pmap, Invalidatable inval)
        {
            redrawView = inval;
            this.canvas = canvas;

            stopReadingTiles (false);

            synchronized (openedBitmaps) {
                for (TileBitmap tbm : openedBitmaps.values ()) {
                    tbm.used = false;
                }
            }

            if (DrawTiles (pmap) && (pmap.copyrtPath != null)) {
                copyrtBGPaint.setStrokeWidth (pmap.copyrtSize);
                copyrtBGPaint.setTextSize (pmap.copyrtSize);
                copyrtTxPaint.setTextSize (pmap.copyrtSize);
                canvas.drawTextOnPath (copyrtMessage, pmap.copyrtPath, 0, 0, copyrtBGPaint);
                canvas.drawTextOnPath (copyrtMessage, pmap.copyrtPath, 0, 0, copyrtTxPaint);
            }

            synchronized (openedBitmaps) {
                for (Iterator<TileBitmap> it = openedBitmaps.values ().iterator (); it.hasNext ();) {
                    TileBitmap tbm = it.next ();
                    if (! tbm.used) {
                        it.remove ();
                        if (tbm.bm != null) tbm.bm.recycle ();
                    }
                }
            }
        }

        @Override  // TileDrawer
        public boolean DrawTile ()
        {
            /*
             * Try to draw the bitmap or start downloading it if we don't have it.
             * Meanwhile, try to draw zoomed out tile if we have one (but don't download them).
             */
            if (TryToDrawTile (canvas, zoom, tileX, tileY, true)) return true;
            int tileXOut = tileX;
            int tileYOut = tileY;
            for (int zoomOut = zoom; -- zoomOut >= 0;) {
                tileXOut /= 2;
                tileYOut /= 2;
                if (TryToDrawTile (canvas, zoomOut, tileXOut, tileYOut, false)) return true;
            }
            return false;
        }

        /**
         * Try to draw a single tile to the canvas, rotated, scaled and translated in place.
         * @param canvas = canvas to draw it on
         * @param zoomOut = zoom level to try to draw
         * @param tileXOut = which tile at that zoom level to draw
         * @param tileYOut = which tile at that zoom level to draw
         * @param startDownload = true: start downloading if not on flash; false: don't bother
         * @return true: tile drawn; false: not drawn
         */
        private boolean TryToDrawTile (Canvas canvas, int zoomOut, int tileXOut, int tileYOut, boolean startDownload)
        {
            TileBitmap tbm;
            long key = ((long) tileXOut << 36) | ((long) tileYOut << 8) | zoomOut;
            synchronized (openedBitmaps) {

                // see if we have the exact tile requested already opened and ready to display
                tbm = openedBitmaps.get (key);
                if (tbm == null) {

                    // if not, request only if it is the most zoomed-in level
                    // the TileOpenerThread will open an outer-zoom level if the zoomed-in one is not downloaded
                    if (startDownload) {
                        tbm = new TileBitmap ();
                        tbm.inval = redrawView;
                        neededBitmaps.put (key, tbm);
                        if (tileOpenerThread == null) {
                            tileOpenerThread = new TileOpenerThread ();
                            tileOpenerThread.start ();
                        }
                    }
                    return false;
                }
            }

            // it is opened, remember it is being used so it doesn't get recycled
            // also it might be null meaning the bitmap file is corrupt
            tbm.used = true;
            Bitmap tile = tbm.bm;
            if (tile == null) return false;

            /*
             * We might be using a zoomed-out tile while correct one downloads.
             * So adjust which pixels in the zoomed-out tile have what we want.
             */
            int outed    = zoom - zoomOut;
            int ww       = BitmapSize >> outed;
            int hh       = BitmapSize >> outed;
            int leftBmp  = (tileX * ww) % BitmapSize;
            int topBmp   = (tileY * hh) % BitmapSize;
            int riteBmp  = leftBmp + ww;
            int botBmp   = topBmp  + hh;
            bitmappts[0] = leftBmp;
            bitmappts[1] = topBmp;
            bitmappts[2] = riteBmp;
            bitmappts[3] = topBmp;
            bitmappts[4] = riteBmp;
            bitmappts[5] = botBmp;
            bitmappts[6] = leftBmp;
            bitmappts[7] = botBmp;

            /*
             * If zoomed out, make a clip so we only draw what is needed on the canvas so we don't overdraw zoomed-in tiles..
             * If not zoomed out, we can draw the whole bitmap as it just fits the canvas spot.
             */
            canvas.save ();
            try {
                if (outed > 0) {
                    canvasclip.rewind ();
                    canvasclip.moveTo ((float) northwestcanpix.x, (float) northwestcanpix.y);
                    canvasclip.lineTo ((float) northeastcanpix.x, (float) northeastcanpix.y);
                    canvasclip.lineTo ((float) southeastcanpix.x, (float) southeastcanpix.y);
                    canvasclip.lineTo ((float) southwestcanpix.x, (float) southwestcanpix.y);
                    canvasclip.lineTo ((float) northwestcanpix.x, (float) northwestcanpix.y);
                    canvas.clipPath (canvasclip);
                }

                /*
                 * Draw the bitmap to the canvas using that transformation.
                 */
                if (!matrix.setPolyToPoly (bitmappts, 0, canvaspts, 0, 4)) {
                    return false;  // maybe zoomed in too far so ww or hh is zero
                }
                canvas.drawBitmap (tile, matrix, null);
            } finally {
                canvas.restore ();
            }

            return true;
        }

        public void CloseBitmaps ()
        {
            redrawView = null;
            stopReadingTiles (true);
            synchronized (openedBitmaps) {
                for (TileBitmap tbm : openedBitmaps.values ()) {
                    if (tbm.bm != null) tbm.bm.recycle ();
                }
                openedBitmaps.clear ();
            }
        }

        private void stopReadingTiles (boolean wait)
        {
            Thread t;
            synchronized (openedBitmaps) {
                neededBitmaps.clear ();
                t = tileOpenerThread;
            }
            if (wait && (t != null)) {
                try { t.join (); } catch (InterruptedException ignored) { }
            }
            synchronized (downloadBitmaps) {
                downloadBitmaps.clear ();
                t = tileDownloaderThread;
            }
            if (wait && (t != null)) {
                try { t.join (); } catch (InterruptedException ignored) { }
            }
        }

        /**
         * Get tiles to open from neededBitmaps and put in openedBitmaps.
         * If any need downloading from the server, put them in downloadBitmaps.
         */
        private class TileOpenerThread extends Thread {
            @Override
            public void run ()
            {
                setName ("OpenStreetMap tile opener");

                long key = 0;
                TileBitmap tbm = null;
                while (true) {

                    // queue previously opened bitmap into openedBitmaps
                    // ...and dequeue needed bitmap from neededBitmaps
                    // if nothing to dequeue, terminate thread
                    synchronized (openedBitmaps) {
                        if (tbm != null) {
                            openedBitmaps.put (key, tbm);
                            tbm.inval.postInvalidate ();
                        }
                        Iterator<Long> it = neededBitmaps.keySet ().iterator ();
                        do {
                            if (! it.hasNext ()) {
                                tileOpenerThread = null;
                                return;
                            }
                            key = it.next ();
                            tbm = neededBitmaps.get (key);
                            assert tbm != null;
                            it.remove ();
                        } while (openedBitmaps.containsKey (key));
                    }

                    // open the requested tile or one at an outer zoom level
                    // do not request any tile be downloaded from server yet
                    int tileIX = (int) (key >> 36) & 0x0FFFFFFF;
                    int tileIY = (int) (key >>  8) & 0x0FFFFFFF;
                    int zoomLevel = (int) key & 0xFF;
                    int zl;
                    for (zl = zoomLevel; zl >= 0; -- zl) {
                        tbm.bm = ReadTileBitmap (tileIX, tileIY, zl, false);
                        if (tbm.bm != null) break;
                        tileIX /= 2;
                        tileIY /= 2;
                    }

                    // if an outer tile was found, ie, the inner tile not found on flash,
                    // request that it be downloaded from server
                    if (zl < zoomLevel) {
                        synchronized (downloadBitmaps) {
                            downloadBitmaps.put (key, tbm.inval);
                            if (tileDownloaderThread == null) {
                                tileDownloaderThread = new TileDownloaderThread ();
                                tileDownloaderThread.start ();
                            }
                        }
                    }

                    // mark the possibly zoomed-out tile as now being opened
                    // and prevent it from being recycled right away
                    if (zl < 0) {
                        tbm = null;
                    } else {
                        key = (((long) tileIX) << 36) | (((long) tileIY) << 8) | zl;
                        tbm.used = true;
                    }
                }
            }
        }

        private class TileDownloaderThread extends Thread {
            @Override
            public void run ()
            {
                setName ("OpenStreetMap tile downloader");
                while (true) {
                    long key;
                    Invalidatable inval;
                    synchronized (downloadBitmaps) {
                        Iterator<Long> it = downloadBitmaps.keySet ().iterator ();
                        if (! it.hasNext ()) {
                            tileDownloaderThread = null;
                            return;
                        }
                        key = it.next ();
                        inval = downloadBitmaps.get (key);
                        assert inval != null;
                        it.remove ();
                    }

                    int tileIX = (int) (key >> 36) & 0x0FFFFFFF;
                    int tileIY = (int) (key >>  8) & 0x0FFFFFFF;
                    int zoomLevel = (int) key & 0xFF;
                    DownloadTileBitmap (tileIX, tileIY, zoomLevel, true);
                    inval.postInvalidate ();
                }
            }
        }
    }

    private static class TileBitmap {
        public Invalidatable inval;     // callback when tile gets loaded
        public Bitmap bm;               // bitmap (or null if not on flash or corrupt)
        public boolean used;            // it was used this cycle, don't recycle
    }

    /**
     * This class simply scans the tiles needed to draw to a canvas.
     * It does a callback to DrawTile() for each tile needed.
     */
    private abstract static class TileDrawer {
        protected float[] canvaspts = new float[8];
        protected int tileX, tileY, zoom;
        protected PointD northwestcanpix = new PointD ();
        protected PointD northeastcanpix = new PointD ();
        protected PointD southwestcanpix = new PointD ();
        protected PointD southeastcanpix = new PointD ();

        // draw tile zoom/tileX/tileY.png
        public abstract boolean DrawTile ();

        /**
         * Compute zoom level to be used for drawing.
         */
        public void ComputeZoom (PixelMapper pmap)
        {
            // get canvas area in square inches
            int w = pmap.canvasWidth;
            int h = pmap.canvasHeight;
            double canvasAreaSqIn = (double) w * (double) h / pmap.canPixPerSqIn;

            // see how many zoom=MAXZOOM canvas pixels would be displayed on canvas
            zoom = MAXZOOM;
            double tlTileX = lon2TileX (pmap.lastTlLon);
            double tlTileY = lat2TileY (pmap.lastTlLat);
            double trTileX = lon2TileX (pmap.lastTrLon);
            double trTileY = lat2TileY (pmap.lastTrLat);
            double blTileX = lon2TileX (pmap.lastBlLon);
            double blTileY = lat2TileY (pmap.lastBlLat);
            double brTileX = lon2TileX (pmap.lastBrLon);
            double brTileY = lat2TileY (pmap.lastBrLat);
            double topEdge = Math.hypot (tlTileX - trTileX, tlTileY - trTileY);
            double botEdge = Math.hypot (blTileX - brTileX, blTileY - brTileY);
            double leftEdg = Math.hypot (tlTileX - blTileX, tlTileY - blTileY);
            double riteEdg = Math.hypot (trTileX - brTileX, trTileY - brTileY);
            double horEdge = (topEdge + botEdge) / 2.0;
            double verEdge = (leftEdg + riteEdg) / 2.0;
            double canvasAreaTilePixels = horEdge * verEdge * BitmapSize * BitmapSize;

            // see how many zoom=MAXZOOM tile pixels per canvas square inch
            double tilePixelsPerCanvasSqIn = canvasAreaTilePixels / canvasAreaSqIn;

            // each zoom level has 4 times the pixels as the lower level

            double log4TilePixelsPerCanvasSqIn = Math.log (tilePixelsPerCanvasSqIn) / Math.log (4);

            // tiles look good at 256 pixels per inch, so LOG2PIXPERIN = 8
            // assuming MAXZOOM = 17:
            // tilePixelsPerCanvasSqIn@MAXZOOM  log4TilePixelsPerCanvasSqIn@MAXZOOM  neededzoom
            //     16M                                  12                               13 = MAXZOOM-4
            //      4M                                  11                               14 = MAXZOOM-3
            //      1M                                  10                               15 = MAXZOOM-2
            //    256K                                   9                               16 = MAXZOOM-1
            //     64K                                   8                               17 = MAXZOOM-0

            zoom = MAXZOOM - (int) Math.round (log4TilePixelsPerCanvasSqIn - LOG2PIXPERIN);
            if (zoom < 0) zoom = 0;
            if (zoom > MAXZOOM) zoom = MAXZOOM;
        }

        /**
         * Draw OpenStreetMap tiles that fill the given pmap area.
         * Calls DrawTile() for each tile needed.
         * @param pmap = what pixels to draw to and the corresponding lat/lon mapping
         */
        public boolean DrawTiles (PixelMapper pmap)
        {
            boolean gotatile = false;

            /*
             * See what range of tile numbers are needed to cover the canvas.
             */
            int maxTileY = (int) lat2TileY (pmap.canvasSouthLat);
            int minTileY = (int) lat2TileY (pmap.canvasNorthLat);
            int minTileX = (int) lon2TileX (pmap.canvasWestLon);
            int maxTileX = (int) lon2TileX (pmap.canvasEastLon);

            /*
             * Loop through all the possible tiles to cover the canvas.
             */
            for (tileY = minTileY; tileY <= maxTileY; tileY ++) {
                double northlat = tileY2Lat (tileY);
                double southlat = tileY2Lat (tileY + 1);
                for (int rawTileX = minTileX; rawTileX <= maxTileX; rawTileX ++) {
                    double westlon = tileX2Lon (rawTileX);
                    double eastlon = tileX2Lon (rawTileX + 1);
                    tileX = rawTileX & ((1 << zoom) - 1);

                    /*
                     * Get rectangle outlining where the entire bitmap goes on canvas.
                     * It's quite possible that some of the bitmap is off the canvas.
                     * It's also quite possible that it is flipped around on the canvas.
                     */
                    pmap.LatLon2CanPixAprox (northlat, westlon, northwestcanpix);
                    pmap.LatLon2CanPixAprox (northlat, eastlon, northeastcanpix);
                    pmap.LatLon2CanPixAprox (southlat, westlon, southwestcanpix);
                    pmap.LatLon2CanPixAprox (southlat, eastlon, southeastcanpix);

                    /*
                     * At least some part of tile is on canvas, draw it.
                     */
                    canvaspts[0] = (float) northwestcanpix.x;
                    canvaspts[1] = (float) northwestcanpix.y;
                    canvaspts[2] = (float) northeastcanpix.x;
                    canvaspts[3] = (float) northeastcanpix.y;
                    canvaspts[4] = (float) southeastcanpix.x;
                    canvaspts[5] = (float) southeastcanpix.y;
                    canvaspts[6] = (float) southwestcanpix.x;
                    canvaspts[7] = (float) southwestcanpix.y;
                    gotatile |= DrawTile ();
                }
            }
            return gotatile;
        }

        /**
         * Convert lat,lon to x,y tile numbers
         * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
         */
        private double lon2TileX (double lon)
        {
            double n = 1 << zoom;
            return n * (lon + 180.0) / 360.0;
        }
        private double lat2TileY (double lat)
        {
            double n = 1 << zoom;
            double latrad = Math.toRadians (lat);
            return n * (1.0 - (Math.log (Math.tan (latrad) + 1.0 / Math.cos (latrad)) / Math.PI)) / 2.0;
        }
        private double tileX2Lon (int xTile)
        {
            double n = 1 << zoom;
            return xTile * 360.0 / n - 180.0;
        }
        private double tileY2Lat (int yTile)
        {
            double n = 1 << zoom;
            return Math.toDegrees (Math.atan (Math.sinh (Math.PI * (1.0 - 2.0 * yTile / n))));
        }
    }

    /**
     * Synchronously read a tile's bitmap file.
     * Maybe download from server if we don't have it on flash.
     */
    public Bitmap ReadTileBitmap (int tileIX, int tileIY, int zoomLevel, boolean download)
    {
        File permfile = DownloadTileBitmap (tileIX, tileIY, zoomLevel, download);
        if (permfile == null) return null;
        try {

            /*
             * Read flash file into memorie.
             */
            Bitmap bm = BitmapFactory.decodeFile (permfile.getAbsolutePath ());
            if (bm == null) throw new IOException ("bitmap corrupt");
            if ((bm.getWidth () != BitmapSize) || (bm.getHeight () != BitmapSize)) {
                throw new IOException ("bitmap bad size " + bm.getWidth () + "," + bm.getHeight ());
            }
            return bm;
        } catch (Exception e) {
            Log.e (TAG, "error reading tile: " + permfile, e);
            Lib.Ignored (permfile.delete ());
            return null;
        }
    }

    /**
     * Synchronously download a tile's bitmap file from server onto flash.
     * @param tileIX = x coord left edge of tile 0..(1<<zoomLevel)-1
     * @param tileIY = y coord top edge of tile 0..(1<<zoomLevel)-1
     * @param zoomLevel = tile zoom level
     * @param download = false: return null if not on flash; true: download if not on flash
     * @return null if not on flash; else: name of flash file
     */
    public File DownloadTileBitmap (int tileIX, int tileIY, int zoomLevel, boolean download)
    {
        String tilename = zoomLevel + "/" + tileIX + "/" + tileIY + ".png";
        File permfile = new File (mainActivity.getNoBackupFilesDir (), "streets/" + tilename);
        File tempfile = new File (permfile.getPath () + ".tmp");

        /*
         * See if recent file exists, if not maybe download.
         */
        if (download && (System.currentTimeMillis () - permfile.lastModified () > TILE_FILE_AGE_MS)) {

            /*
             * Open connection to the server to fetch it.
             */
            try {
                URL url = new URL (DownloadThread.baseurl + "/streets.php?tile=" + tilename);
                HttpURLConnection httpCon = (HttpURLConnection)url.openConnection ();
                try {

                    /*
                     * Check HTTP status.
                     */
                    httpCon.setRequestMethod ("GET");
                    int rc = httpCon.getResponseCode ();
                    if (rc != HttpURLConnection.HTTP_OK) {
                        throw new IOException ("http response code " + rc);
                    }

                    /*
                     * Read stream into temp file.
                     */
                    try (InputStream is = httpCon.getInputStream ()) {
                        Lib.Ignored (permfile.getParentFile ().mkdirs ());
                        try (OutputStream os = new FileOutputStream (tempfile)) {
                            byte[] buff = new byte[4096];
                            while (true) {
                                rc = is.read (buff);
                                if (rc <= 0) break;
                                os.write (buff, 0, rc);
                            }
                        }
                    }
                } finally {
                    httpCon.disconnect ();
                }

                /*
                 * Successfully wrote complete temp file,
                 * rename temp file to permanant file.
                 */
                if (! tempfile.renameTo (permfile)) {
                    throw new IOException ("error renaming " + tempfile + " to " + permfile);
                }
            } catch (Exception e) {
                Log.e (TAG, "error downloading tile: " + tilename, e);
                Lib.Ignored (tempfile.delete ());
            }
        }

        return permfile.exists () ? permfile : null;
    }
}
