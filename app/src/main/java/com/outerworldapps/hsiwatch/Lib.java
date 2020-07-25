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
        double sLat = Math.toRadians (srcLat);
        double sLon = Math.toRadians (srcLon);
        double fLat = Math.toRadians (dstLat);
        double fLon = Math.toRadians (dstLon);
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
        double sLat = Math.toRadians (srcLat);
        double sLon = Math.toRadians (srcLon);
        double fLat = Math.toRadians (dstLat);
        double fLon = Math.toRadians (dstLon);
        double dLon = fLon - sLon;
        double t1 = Math.cos (sLat) * Math.tan (fLat);
        double t2 = Math.sin (sLat) * Math.cos (dLon);
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
        // rotate to make endlon = 0
        double beglatrad = Math.toRadians (beglatdeg);
        double beglonrad = Math.toRadians (beglondeg - endlondeg);
        double endlatrad = Math.toRadians (endlatdeg);
        //// double endlonrad = Math.toRadians (endlondeg - endlondeg);
        double curlatrad = Math.toRadians (curlatdeg);
        double curlonrad = Math.toRadians (curlondeg - endlondeg);

        double beglatcos = Math.cos (beglatrad);
        double endlatcos = Math.cos (endlatrad);
        double curlatcos = Math.cos (curlatrad);

        // find points on normalized sphere
        // +X axis goes through lat=0,lon=0
        // +Y axis goes through lat=0,lon=90
        // +Z axis goes through north pole
        double begX = Math.cos (beglonrad) * beglatcos;
        double begY = Math.sin (beglonrad) * beglatcos;
        double begZ = Math.sin (beglatrad);
        //noinspection UnnecessaryLocalVariable
        double endX = endlatcos;  //// Math.cos (endlonrad) * endlatcos;
        //// double endY = Math.sin (endlonrad) * endlatcos;
        double endZ = Math.sin (endlatrad);
        double curX = Math.cos (curlonrad) * curlatcos;
        double curY = Math.sin (curlonrad) * curlatcos;
        double curZ = Math.sin (curlatrad);

        // compute normal to plane containing course, ie, containing beg, end, origin = beg cross end
        // note that the plane crosses through the center of earth, ie, (0,0,0)
        // equation of plane containing course: norX * x + norY * Y + norZ * z = 0
        double courseNormalX = begY * endZ; //// begY * endZ - begZ * endY;
        double courseNormalY = begZ * endX - begX * endZ;
        double courseNormalZ = - begY * endX; //// begX * endY - begY * endX;

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
        double intersectM = Math.sqrt (intersectX * intersectX + intersectY * intersectY + intersectZ * intersectZ);

        // normal to plane from intersection to north pole (0,0,1)
        //noinspection UnnecessaryLocalVariable,SuspiciousNameCombination
        double norintnplX =   intersectY;
        double norintnplY = - intersectX;

        // dot product of course normal with normal to northpole gives cos of the heading at intersectXYZ to endXYZ
        double costheta = (courseNormalX * norintnplX + courseNormalY * norintnplY) * intersectM;

        // cross of course normal with normal to northpole gives vector pointing directly to (or away from) intersection point
        // its magnitude is the sin of the heading
        double towardintX = - courseNormalZ * norintnplY;
        double towardintY =   courseNormalZ * norintnplX;
        double towardintZ = courseNormalX * norintnplY - courseNormalY * norintnplX;

        // dot that with vector to intersection is the sin of the heading
        double sintheta = towardintX * intersectX + towardintY * intersectY + towardintZ * intersectZ;

        return Math.toDegrees (Math.atan2 (sintheta, costheta));
    }

    /**
     * Given a great circle course from old point to dst point,
     * find a new point to dst point that gives the given new
     * on-course heading at given cur point
     *  Input:
     *   cur{lat,lon} = point somewhere along the route, doesn't have to be exactly on the route
     *   dst{lat,lon} = endpoint of old and new routes (doesn't change)
     *   old{lat,lon} = old starting point
     *   target_curhdg = new route's on-course true heading at same cur point along new route
     *  Output:
     *   returns Double.NaN if divergent
     *     else actual heading very close to target_curhdg
     *          newll.{lat,lon} = new starting point
     */
    public static double GCXTKCourse (double curlat, double curlon, double dstlat, double dstlon,
                                      double oldlat, double oldlon, double target_curhdg, LatLon newll)
    {
        double distnm = LatLonDist (dstlat, dstlon, oldlat, oldlon);

        // step 360 deg around dst point and find which courses bracket target_curhdg
        // the one slightly higher than target_curhdg goes in first_...
        // the one slightly lower than target_curhdg goes in last_...
        double first_curhdg = Double.NaN;
        double first_dsthdg = Double.NaN;
        double last_curhdg  = Double.NaN;
        double last_dsthdg  = Double.NaN;

        boolean first = true;
        for (double dsthdg = -180.0; dsthdg < 180.0; dsthdg += 60.0) {
            double srclat = LatHdgDist2Lat (dstlat, dsthdg, distnm);
            double srclon = LatLonHdgDist2Lon (dstlat, dstlon, dsthdg, distnm);
            double curhdg = GCOnCourseHdg (srclat, srclon, dstlat, dstlon, curlat, curlon);

            double curofs = curhdg - target_curhdg;
            if (curofs < 0.0) curofs += 360.0;

            if (first) {
                first_curhdg = curofs;
                first_dsthdg = dsthdg;
                last_curhdg  = curofs;
                last_dsthdg  = dsthdg;
                first = false;
            } else {
                if (first_curhdg > curofs) {
                    first_curhdg = curofs;
                    first_dsthdg = dsthdg;
                }
                if (last_curhdg < curofs) {
                    last_curhdg = curofs;
                    last_dsthdg = dsthdg;
                }
            }
        }

        // unwrap the comparing values
        first_curhdg += target_curhdg;
        last_curhdg  += target_curhdg - 360.0;

        while (true) {

            // target should be slightly less than the first
            if (target_curhdg > first_curhdg) return Double.NaN;

            // target should be slightly more than the last
            if (target_curhdg < last_curhdg) return Double.NaN;

            // we now have:
            //  first_curhdg   =>  first_dsthdg
            //  target_curhdg  =>  what we want to know
            //  last_curhdg    =>  last_dsthdg
            // where:
            //  last_curhdg <= target_curhdg <= first_curhdg

            // no assumptions about first_dsthdg,last_dsthdg
            //  they could be wrapped and/or descending

            // wrap dsthdgs to be close
            if (last_dsthdg - first_dsthdg > 180.0) first_dsthdg += 360.0;
            if (first_dsthdg - last_dsthdg > 180.0) last_dsthdg  += 360.0;

            // find proportion of target_curhdg between last_curhdg and first_curhdg
            double proportion = (target_curhdg - last_curhdg) / (first_curhdg - last_curhdg);

            // estimate target_dsthdg using linear interpolation
            double trial_dsthdg = proportion * (first_dsthdg - last_dsthdg) + last_dsthdg;

            // calculate corresponding trial_curhdg
            double srclat = LatHdgDist2Lat (dstlat, trial_dsthdg, distnm);
            double srclon = LatLonHdgDist2Lon (dstlat, dstlon, trial_dsthdg, distnm);
            double trial_curhdg = GCOnCourseHdg (srclat, srclon, dstlat, dstlon, curlat, curlon);

            // all done if very close to target_curhdg
            if (trial_curhdg - target_curhdg >  180.0) trial_curhdg -= 360.0;
            if (trial_curhdg - target_curhdg < -180.0) trial_curhdg += 360.0;
            if (Math.abs (trial_curhdg - target_curhdg) < 0.000001) {
                newll.lat = srclat;
                newll.lon = srclon;
                return trial_curhdg;
            }

            // replace either first or last with the trial mapping to narrow search range
            // assume divergent if it doesn't narrow the search range
            if (trial_curhdg > target_curhdg) {
                if (trial_curhdg >= first_curhdg) return Double.NaN;
                first_curhdg = trial_curhdg;
                first_dsthdg = trial_dsthdg;
            } else {
                if (trial_curhdg <= last_curhdg) return Double.NaN;
                last_curhdg = trial_curhdg;
                last_dsthdg = trial_dsthdg;
            }
        }
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
