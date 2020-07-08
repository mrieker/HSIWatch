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

public class Lib {

    // fine-tuned constants
    public static final double FtPerM    = 3.28084;
    public static final double FtPerNM   = 6076.12;
    public static final double KtPerMPS  = 1.94384;
    public static final double NMPerDeg  = 60.0;
    public static final int    MPerNM    = 1852;    // metres per naut mile

    /**
     * Create a quoted string suitable for QuotedCSVSplit().
     */
    @SuppressWarnings("unused")
    public static String QuotedString (String unquoted)
    {
        int len = unquoted.length ();
        StringBuilder sb = new StringBuilder (len + 2);
        sb.append ('"');
        for (int i = 0; i < len; i ++) {
            char c = unquoted.charAt (i);
            switch (c) {
                case '\\': {
                    sb.append ("\\\\");
                    break;
                }
                case '\n': {
                    sb.append ("\\n");
                    break;
                }
                case 0: {
                    sb.append ("\\z");
                    break;
                }
                case '"': {
                    sb.append ("\\\"");
                    break;
                }
                default: {
                    sb.append (c);
                    break;
                }
            }
        }
        sb.append ('"');
        return sb.toString ();
    }

    /**
     * Normalize a longitude in range -180.0..+179.999999999
     */
    public static double NormalLon (double lon)
    {
        while (lon < -180.0) lon += 360.0;
        while (lon >= 180.0) lon -= 360.0;
        return lon;
    }

    /**
     * Compute great-circle distance between two lat/lon co-ordinates
     * @return distance between two points (in nm)
     */
    public static double LatLonDist (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        return Math.toDegrees (LatLonDist_rad (srcLat, srcLon, dstLat, dstLon)) * NMPerDeg;
    }
    public static double LatLonDist_rad (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        // http://en.wikipedia.org/wiki/Great-circle_distance
        double sLat = srcLat / 180 * Math.PI;
        double sLon = srcLon / 180 * Math.PI;
        double fLat = dstLat / 180 * Math.PI;
        double fLon = dstLon / 180 * Math.PI;
        double dLon = fLon - sLon;
        double t1   = Sq (Math.cos (fLat) * Math.sin (dLon));
        double t2   = Sq (Math.cos (sLat) * Math.sin (fLat) - Math.sin (sLat) * Math.cos (fLat) * Math.cos (dLon));
        double t3   = Math.sin (sLat) * Math.sin (fLat);
        double t4   = Math.cos (sLat) * Math.cos (fLat) * Math.cos (dLon);
        return Math.atan2 (Math.sqrt (t1 + t2), t3 + t4);
    }

    private static double Sq (double x) { return x*x; }

    /**
     * Compute great-circle true course from one lat/lon to another lat/lon
     * @return true course (in degrees) at source point (-180..+179.999999)
     */
    public static double LatLonTC (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        return LatLonTC_rad (srcLat, srcLon, dstLat, dstLon) * 180.0 / Math.PI;
    }
    public static double LatLonTC_rad (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        // http://en.wikipedia.org/wiki/Great-circle_navigation
        double sLat = srcLat / 180 * Math.PI;
        double sLon = srcLon / 180 * Math.PI;
        double fLat = dstLat / 180 * Math.PI;
        double fLon = dstLon / 180 * Math.PI;
        double dLon = fLon - sLon;
        double t1 = Math.cos (sLat) * Math.tan (fLat);
        double t2 = Math.sin(sLat) * Math.cos(dLon);
        return Math.atan2 (Math.sin (dLon), t1 - t2);
    }

    /**
     * Compute new lat/lon given old lat/lon, heading (degrees), distance (nautical miles)
     * http://stackoverflow.com/questions/7222382/get-lat-long-given-current-point-distance-and-bearing
     */
    public static double LatHdgDist2Lat (double latdeg, double hdgdeg, double distnm)
    {
        double distrad = Math.toRadians (distnm / NMPerDeg);
        double latrad  = Math.toRadians (latdeg);
        double hdgrad  = Math.toRadians (hdgdeg);

        latrad = Math.asin (Math.sin (latrad) * Math.cos (distrad) + Math.cos (latrad) * Math.sin (distrad) * Math.cos (hdgrad));
        return Math.toDegrees (latrad);
    }
    public static double LatLonHdgDist2Lon (double latdeg, double londeg, double hdgdeg, double distnm)
    {
        double distrad = Math.toRadians (distnm / NMPerDeg);
        double latrad  = Math.toRadians (latdeg);
        double hdgrad  = Math.toRadians (hdgdeg);

        double newlatrad = Math.asin (Math.sin (latrad) * Math.cos (distrad) + Math.cos (latrad) * Math.sin (distrad) * Math.cos (hdgrad));
        double lonrad = Math.atan2 (Math.sin (hdgrad) * Math.sin (distrad) * Math.cos (latrad), Math.cos (distrad) - Math.sin (latrad) * Math.sin (newlatrad));
        return NormalLon (Math.toDegrees (lonrad) + londeg);
    }

    /**
     * Given a course from beg to end and a current position cur, find what the on-course heading is at the
     * point on the course adjacent to the current position
     * @param beglatdeg = course beginning latitude
     * @param beglondeg = course beginning longitude
     * @param endlatdeg = course ending latitude
     * @param endlondeg = course ending longitude
     * @param curlatdeg = current position latitude
     * @param curlondeg = current position longitude
     * @return on-course true heading (degrees)
     */
    public static double GCOnCourseHdg (double beglatdeg, double beglondeg, double endlatdeg, double endlondeg, double curlatdeg, double curlondeg)
    {
        // convert arguments to radians
        double beglatrad = Math.toRadians (beglatdeg);
        double beglonrad = Math.toRadians (beglondeg);
        double endlatrad = Math.toRadians (endlatdeg);
        double endlonrad = Math.toRadians (endlondeg);
        double curlatrad = Math.toRadians (curlatdeg);
        double curlonrad = Math.toRadians (curlondeg);

        // find points on normalized sphere
        // +X axis goes through lat=0,lon=0
        // +Y axis goes through lat=0,lon=90
        // +Z axis goes through north pole
        double begX = Math.cos (beglonrad) * Math.cos (beglatrad);
        double begY = Math.sin (beglonrad) * Math.cos (beglatrad);
        double begZ = Math.sin (beglatrad);
        double endX = Math.cos (endlonrad) * Math.cos (endlatrad);
        double endY = Math.sin (endlonrad) * Math.cos (endlatrad);
        double endZ = Math.sin (endlatrad);
        double curX = Math.cos (curlonrad) * Math.cos (curlatrad);
        double curY = Math.sin (curlonrad) * Math.cos (curlatrad);
        double curZ = Math.sin (curlatrad);

        // compute normal to plane containing course, ie, containing beg, end, origin = beg cross end
        // note that the plane crosses through the center of earth, ie, (0,0,0)
        // equation of plane containing course: norX * x + norY * Y + norZ * z = 0
        double courseNormalX = begY * endZ - begZ * endY;
        double courseNormalY = begZ * endX - begX * endZ;
        double courseNormalZ = begX * endY - begY * endX;

        // compute normal to plane containing that normal, cur and origin = cur cross courseNormal
        double currentNormalX = curY * courseNormalZ - curZ * courseNormalY;
        double currentNormalY = curZ * courseNormalX - curX * courseNormalZ;
        double currentNormalZ = curX * courseNormalY - curY * courseNormalX;

        // we now have two planes that are perpendicular, both passing through the origin
        // one plane contains the course arc from beg to end
        // the other plane contains the current position cur
        // find one of the two points where the planes intersect along the course line, ie, on the normalized sphere
        double intersectX = courseNormalY * currentNormalZ - courseNormalZ * currentNormalY;
        double intersectY = courseNormalZ * currentNormalX - courseNormalX * currentNormalZ;
        double intersectZ = courseNormalX * currentNormalY - courseNormalY * currentNormalX;

        // find lat/lon of the intersection point
        double intersectLat = Math.toDegrees (Math.atan2 (intersectZ, Math.sqrt (intersectX * intersectX + intersectY * intersectY)));
        double intersectLon = Math.toDegrees (Math.atan2 (intersectY, intersectX));

        // find distance from intersection point to start and end points
        double distInt2End = LatLonDist_rad (intersectLat, intersectLon, endlatdeg, endlondeg);
        double distInt2Beg = LatLonDist_rad (intersectLat, intersectLon, beglatdeg, beglondeg);

        // if closer to start point, return true course from intersection point to end point
        if (distInt2End > distInt2Beg) {
            return LatLonTC (intersectLat, intersectLon, endlatdeg, endlondeg);
        }

        // but/and if closer to endpoint, return reciprocal of tc from intersection to start point
        double tc = LatLonTC (intersectLat, intersectLon, beglatdeg, beglondeg) + 180.0;
        if (tc >= 180.0) tc -= 360.0;
        return tc;
    }

    /**
     * Given a great circle course from old point to dst point,
     * find a new point to dst point that gives the given new
     * on-course heading at given cur point
     *  Input:
     *   cur{lat,lon}deg = point somewhere along the route, doesn't have to be exactly on the route
     *   dst{lat,lon}deg = endpoint of old and new routes (doesn't change)
     *   old{lat,lon}deg = old starting point
     *   newhdgdeg = new route's on-course true heading at same cur point along new route
     *  Output:
     *   newll.{lat,lon} = new starting point
     */
    public static boolean GCXTKCourse (double curlatdeg, double curlondeg, double dstlatdeg, double dstlondeg,
                                       double oldlatdeg, double oldlondeg, double newhdgdeg, LatLon newll)
    {
        double distnm = LatLonDist (dstlatdeg, dstlondeg, oldlatdeg, oldlondeg);

        double lastdiff = 99999.0;

        for (int i = 100; -- i >= 0;) {

            // get on-course heading from old starting point to destination
            double oldhdgdeg = GCOnCourseHdg (oldlatdeg, oldlondeg, dstlatdeg, dstlondeg, curlatdeg, curlondeg);

            // all done if within 0.01 deg of requested on-course heading
            double difhdgdeg = newhdgdeg - oldhdgdeg;
            while (difhdgdeg < -180.0) difhdgdeg += 360.0;
            while (difhdgdeg >= 180.0) difhdgdeg -= 360.0;
            double difhdgabs = Math.abs (difhdgdeg);
            if (difhdgabs < 0.01) {
                newll.lat = oldlatdeg;
                newll.lon = oldlondeg;
                return true;
            }

            // stop if diverging
            if (lastdiff < difhdgabs) break;
            lastdiff = difhdgabs;

            // get heading from destination to old starting point
            double oldtc = LatLonTC (dstlatdeg, dstlondeg, oldlatdeg, oldlondeg);

            // get approximate new starting point
            // eg, old on-course heading is 070
            //     new on-course heading is 071
            //     that moves the course line a little north
            //     oldtc is something like 290
            //     so newtc should be 291 cuz we're moving course a little north
            double newtc = oldtc + difhdgdeg;
            oldlatdeg = LatHdgDist2Lat (dstlatdeg, newtc, distnm);
            oldlondeg = LatLonHdgDist2Lon (dstlatdeg, dstlondeg, oldtc, distnm);
        }

        return false;
    }

    /**
     * Double to string, strip trailing ".0" if any
     */
    public static String DoubleNTZ (double floatval, int decimals)
    {
        return DoubleNTZ (floatval, decimals, 0);
    }
    // maxdp = maximum number of decimal places to output
    // mindp = minimum number of decimal places to output
    public static String DoubleNTZ (double floatval, int maxdp, int mindp)
    {
        if (Double.isNaN (floatval)) return "NaN";
        double absvalue = Math.abs (floatval);
        if (absvalue >= 1.0E20) return Double.toString (floatval);
        int shiftedleft = 0;
        while ((-- maxdp >= 0) || (shiftedleft < mindp)) {
            double g = absvalue * 10.0;
            if (g >= 1.0E20) break;
            absvalue = g;
            shiftedleft ++;
        }
        long longval = Math.round (absvalue);
        while ((shiftedleft > mindp) && (longval % 10 == 0)) {
            longval /= 10;
            -- shiftedleft;
        }
        char[] str = new char[24];
        int i = str.length;
        do {
            str[--i] = (char) (longval % 10 + '0');
            longval /= 10;
            if (-- shiftedleft == 0) str[--i] = '.';
        } while ((longval > 0) || (shiftedleft >= 0));
        if (floatval < 0) str[--i] = '-';
        return new String (str, i, str.length - i);
    }

    public static int formatDigits (char[] digits, int i, int mindigs, long number)
    {
        while ((-- mindigs >= 0) || (number > 0)) {
            digits[--i] = (char) (number % 10 + '0');
            number /= 10;
        }
        return i;
    }

    /**
     * Convert a byte array to hexadecimal string.
     */
    private final static String hexbytes = "0123456789abcdef";
    public static String bytesToHex (byte[] bytes)
    {
        int len = bytes.length;
        char[] hex = new char[len*2];
        for (int i = 0; i < len; i ++) {
            byte b = bytes[i];
            hex[i*2] = hexbytes.charAt ((b >> 4) & 15);
            hex[i*2+1] = hexbytes.charAt (b & 15);
        }
        return new String (hex);
    }

    /**
     * Various compiler warnings.
     */
    public static void Ignored (@SuppressWarnings("UnusedParameters") boolean x) { }
}
