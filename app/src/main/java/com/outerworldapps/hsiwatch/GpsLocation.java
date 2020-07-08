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

/**
 * Our own GPS location that holds magnetic variation.
 */
public class GpsLocation {
    public double altitude;     // metres MSL
    public double latitude;     // degrees
    public double longitude;    // degrees
    public double magvar;       // degrees (magcourse = truecourse + magvar)
                                // - gets filled in by MainActivity.gpsLocationReceived()
    public double speed;        // metres per second
    public double truecourse;   // degrees
    public long time;           // ms (unix timestamp * 1000)
}
