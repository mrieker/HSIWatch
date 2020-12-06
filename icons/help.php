<HTML>
    <HEAD>
        <TITLE> HSIWatch App for Android Watches </TITLE>
        <SCRIPT LANGUAGE=JAVASCRIPT>
            function showinstall ()
            {
                var ol = document.getElementById ('installinstrs');
                ol.hidden = ! ol.hidden;
                var cs = document.getElementById ('inscolon');
                cs.innerHTML = ol.hidden ? '.' : ':';
            }
        </SCRIPT>
    </HEAD>
    <BODY>
        <H3> HSIWatch App for Android Watches </H3>
        <UL>
            <LI><A HREF="https://play.google.com/store/apps/details?id=com.outerworldapps.hsiwatch">Download it from Google Play</A>
            <LI><A HREF="https://github.com/mrieker/HSIWatch">Source Code Download</A>
        </UL>
        <P><B><FONT COLOR=RED>Always be skeptical regarding any information provided
            by HSIWatch.  Accept it only if and when it is confirmed by
            navigation methods such as FAA approved chart visual references and
            FAA approved electronic navigation systems.
            EXPECT it to FAIL when someone's HeALTh or PROpeRTy is at RISk.</FONT></B></P>
        <P><B><FONT COLOR=RED>Do not use this app in IMC!</FONT></B></P>
        <P><B>Privacy policy:</B>  The only personal information used by HSIWatch is location, which
            is used only for updating the display and optionally forwarding it to other
            devices, all at the user's explicit direction.</P>
        <UL>
            <LI>Contact us at <SCRIPT LANGUAGE=JAVASCRIPT>
              document.write('i')
              document.write('n')
              document.write('f')
              document.write('o')
              document.write('@')
              document.write('o')
              document.write('u')
              document.write('t')
              document.write('e')
              document.write('r')
              document.write('w')
              document.write('o')
              document.write('r')
              document.write('l')
              document.write('d')
              document.write('a')
              document.write('p')
              document.write('p')
              document.write('s')
              document.write('.')
              document.write('c')
              document.write('o')
              document.write('m')
            </SCRIPT>
            <LI><A HREF="https://www.github.com/mrieker/HSIWatch">Source Code</A>
            <LI>Latest version: <?php
                $remhost = trim (file_get_contents ("remhost.dat"));
                @unlink ("x.tmp");
                if ((system ("scp -pq $remhost/output.json x.tmp", $rc) !== FALSE) && ($rc == 0)) {
                    rename ("x.tmp", "output.json");
                }
                $outjson = file_get_contents ("output.json");
                $outobj  = json_decode ($outjson, FALSE);
                $version = $outobj[0]->apkData->versionName;
                if (! file_exists ("hsiwatch-$version.apk")) {
                    @unlink ("x.tmp");
                    if ((system ("scp -pq $remhost/app-release.apk x.tmp", $rc) !== FALSE) && ($rc == 0)) {
                        rename ("x.tmp", "hsiwatch-$version.apk");
                    }
                }
                echo $version;
            ?>
        </UL>
        <HR>
        <H3> How To Install </H3>
        <P>The easiest way to install is by opening the
            <A HREF="https://play.google.com/store/apps/details?id=com.outerworldapps.hsiwatch">play store web page</A>
            on your phone then click the <B>INSTALL</B> button, then select your watch when prompted.
            <B>Do not use the Play Store App</B> as it will not allow selection of your watch for installation. 
        </P>
        <P>If that does not work, <A HREF="javascript:showinstall()">this procedure</A> can be used to install it on the watch via a
            PC connected to same WiFi network as the watch<SPAN ID=inscolon>.</SPAN></P>
        <OL ID=installinstrs HIDDEN>
            <LI>On watch:
                <OL>
                    <LI>Open <B>Settings</B><BR><IMG WIDTH=80 HEIGHT=80 SRC="install-1.png">
                    <LI>Open <B>System</B><BR><IMG WIDTH=80 HEIGHT=80 SRC="install-2.png">
                    <LI>Open <B>About</B><BR><IMG WIDTH=80 HEIGHT=80 SRC="install-3.png">
                    <LI>Click on <B>Build number</B> 7 times to enable developer options<BR><IMG WIDTH=80 HEIGHT=80 SRC="install-4.png">
                    <LI>Back/Back/Back to get to <B>Settings</B> page
                    <LI>Open <B>Developer options</B><BR><IMG WIDTH=80 HEIGHT=80 SRC="install-5.png">
                    <LI>Turn on <B>ADB debugging</B><BR><IMG WIDTH=80 HEIGHT=80 SRC="install-6.png">
                    <LI>Turn on <B>Debug over Wi-Fi</B><BR><IMG WIDTH=80 HEIGHT=80 SRC="install-7.png">
                    <LI>Note the ipaddress:port, eg, 192.168.1.102:5555
                </OL>
            <LI>On computer:
                <OL>
                    <LI>Install <A HREF="minimal_adb_fastboot_v1.4.3_setup.exe">Minimal ADB and Fastboot</A>
                    <LI>Open command prompt screen then <B><TT>cd</TT></B> to the folder it was installed into
                    <LI>Download <A HREF="hsiwatch-<?php echo $version;?>.apk">HSIWatch APK</A> into same folder
                    <LI>Type command: <B><TT>adb connect <I>ipaddress:port</I></TT></B><BR>
                        eg <B><TT>adb connect 192.168.1.102:5555</TT></B><BR>
                        (click <B>OK</B> on watch to <B>Allow Debugging?</B>)
                    <LI>Type command: <B><TT>adb install hsiwatch-<?php echo $version;?>.apk</TT></B><BR>
                </OL>
            <LI>On watch:
                <OL>
                    <LI>Turn off <B>ADB debugging</B>
                    <LI>Go back to main menu and launch HSIWatch app<BR><IMG WIDTH=80 HEIGHT=80 SRC="install-8.png">
                </OL>
        </OL>
        <HR>
        <H3> Page Map </H3>
        <TABLE>
            <TR>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> Waypoint Entry <BR> Nav Mode Select <BR> <IMG SRC="thumb-wpent.png"> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> Nearest Airport <BR> <IMG SRC="thumb-near.png"> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> Airport Information <BR> <IMG SRC="thumb-info.png"> </TD>
            </TR>
            <TR>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> &#8679; </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> &#8679; </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> &#8679; </TD>
            </TR>
            <TR>
                <TD ALIGN=CENTER> start </TD>
                <TD ALIGN=CENTER> &#8680; </TD>
                <TD ALIGN=CENTER> Nav Dial <BR> <IMG SRC="thumb-nav.png"> </TD>
                <TD ALIGN=CENTER> &#8680; </TD>
                <TD ALIGN=CENTER> Moving Map <BR> <IMG SRC="thumb-map.png"> </TD>
                <TD ALIGN=CENTER> &#8680; </TD>
                <TD ALIGN=CENTER> Runways <BR> <IMG SRC="thumb-rwys.png"> </TD>
            </TR>
            <TR>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> &#8681; </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> &#8681; </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> &#8681; </TD>
            </TR>
            <TR>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER COLSPAN=5> ... menu pages ... <BR> <IMG SRC="thumb-menu.png"> <IMG SRC="thumb-menu2.png"> </TD>
                <TD ALIGN=CENTER> </TD>
                <TD ALIGN=CENTER> </TD>
            </TR>
        </TABLE>
        <HR>
        <H3> Nav Dial Page </H3>
        <TABLE ALIGN=CENTER>
            <TR>
                <TD>
                    <TABLE>
                        <TR><TD>&nbsp;</TD></TR>
                        <TR><TD ALIGN=RIGHT>OBS setting &#9654;</TD></TR>
                        <TR><TD ALIGN=RIGHT>course to waypoint &#9654;</TD></TR>
                        <TR><TD ALIGN=RIGHT>course from waypoint &#9654;</TD></TR>
                        <TR><TD>&nbsp;</TD></TR>
                        <TR><TD ALIGN=RIGHT>distance to waypoint &#9654;</TD></TR>
                        <TR><TD ALIGN=RIGHT>time to waypoint &#9654;</TD></TR>
                        <TR><TD>&nbsp;</TD></TR>
                        <TR><TD>&nbsp;</TD></TR>
                    </TABLE>
                </TD>
                <TD><IMG SRC="main-page.png"></TD>
                <TD>
                    <TABLE>
                        <TR><TD>&nbsp;</TD></TR>
                        <TR><TD ALIGN=LEFT>&#9664; ground track</TD></TR>
                        <TR><TD ALIGN=LEFT>&#9664; altitude</TD></TR>
                        <TR><TD ALIGN=LEFT>&#9664; ground speed</TD></TR>
                        <TR><TD>&nbsp;</TD></TR>
                        <TR><TD>&nbsp;</TD></TR>
                        <TR><TD ALIGN=LEFT>&#9664; waypoint ident</TD></TR>
                        <TR><TD ALIGN=LEFT>&#9664; navigation mode</TD></TR>
                        <TR><TD>&nbsp;</TD></TR>
                    </TABLE>
                </TD>
            </TR>
        </TABLE>
        <TABLE>
            <TR>
                <TD VALIGN=TOP>
                    <UL>
                        <LI><B>OBS setting</B> - where the yellow triangle is,
                            indicating selected course or radial.
                            <UL>
                                <LI>Drag finger around number dial to change OBS
                                    when in GCT,VOR,ADF modes
                                <LI>LOC,LOCBC,ILS modes lock the OBS in place
                            </UL>
                        <LI><B>course to/from waypoint</B> - indicates the
                            OBS setting needed to center the needle
                        <LI><B>distance to waypoint</B> - indicates nautical
                            miles to the selected waypoint.  <I>Italics</I>
                            indicate slant-range distance (when elevation
                            of waypoint is known), normal text indicates
                            over-the-ground distance.
                        <LI><B>time to waypoint</B> - distance / ground speed,
                            ie, time to waypoint if headed directly to waypoint.
                            <SPAN STYLE="white-space: nowrap;">--:--:--</SPAN>
                            indicates time in excess of 100 hours.
                    </UL>
                </TD>
                <TD VALIGN=TOP>
                    <UL>
                        <LI><B>ground track</B> - course line currently following
                            over the ground as measured from GPS, same as red airplane icon.
                            <SPAN STYLE="white-space: nowrap;">---&#176;</SPAN>
                            indicates ground speed too low to determine track.
                        <LI><B>altitude</B> - feet MSL as measured from GPS
                        <LI><B>ground speed</B> - indicates ground speed in knots
                            as measured from GPS
                        <LI><B>waypoint ident</B> - waypoint being navigated to
                            <P>To select a waypoint, swipe downward on the nav dial page to
                                open the waypoint entry page.</P>
                        <LI><B>navigation mode</B> - indicates current navigation mode<BR>
                            <P>To change navigation mode, swipe downward on the nav dial page to
                                open the waypoint entry page.</P>
                    </UL>
                </TD>
            </TR>
        </TABLE>
        <P>To access the other pages, touch near the center of the screen, and a diagram will
            appear:</P>
        <TABLE>
            <TR>
                <TD><IMG SRC="dialflickmenu.png" WIDTH=320 HEIGHT=320></TD>
                <TD>
                    Swipe in the direction of the double arrows to select the function wanted:
                    <UL>
                        <LI><B>exit</B> - close the app (must be swiped twice)
                        <LI><B>map</B> - open the moving map page
                        <LI><B>menu</B> - access the menu pages
                        <LI><B>waypt</B> - enter waypoint being navigated to
                    </UL>
                    You must touch in the area of the menu to get the menu to appear.
                    Touching near the edges will rotate the nav dial (if current nav
                    mode allows it).
                </TD>
            </TR>
        </TABLE>
        <HR>
        <H3> Waypoint Input </H3>
        <P><IMG SRC="waypt-input.png"></P>
        <UL>
            <LI>Case-insensitive, and spaces are ignored.
            <LI>Accepts airport ICAO id (eg, KBOS or 2B2), VOR or ADF id (eg, BOS), fix id (eg, BOSOX).
                airport FAA ID accepted (eg, BVY) if not same as VOR.
            <LI>Accepts localizer IDs (eg, IBVY or I-BVY).
            <LI>Accepts aptid.rwyno (BOS.4R, BOS.04R, KBVY.27, 2B2.10)
                for runway provided the runway lat/lon data is in the FAA database.
                The . is optional and the leading 0 for runway numbers is optional, ie,
                7B3.02, 7B3.2, 7B302, 7B32 are all the same.  When selected, you are
                presented with a dial of a generic ILS lined up on the runway.
                <UL>
                    <LI><FONT COLOR=RED><B>WARNING:</B>  This generic ILS may very well indicate a course that
                        is obstructed, therefore it is usable only when the pilot
                        is able to verify that the path is clear.</FONT>
                </UL>
            <LI>See <A HREF="#voice"><B>Voice Recognition</B></A> section below for use of the <B>VOICE</B> button.
        </UL>

        <P>Navigation mode is selected with the radio buttons below the waypoint description text.</P>
        Valid for all waypoints:
        <UL>
            <LI><B>OFF</B> - stops GPS reception
            <LI><B>GCT</B> - track great-circle course to waypoint
                <UL>
                    <LI>start of course is what current position was when waypoint
                        was entered or app was started (whichever was later)
                    <LI>end of course is the entered waypoint
                    <LI>OBS initially set to great-circle course from starting position to entered waypoint
                    <LI>OBS automatically updates as flight progresses along course to track great-circle course
                    <LI>needle indicates off-course (crosstrack) distance in degrees similar to VOR
                    <LI>to re-center needle, either:
                        <UL>
                            <LI>manually rotate OBS dial
                            <LI>swipe down to access waypoint entry page, then tap on waypoint id which opens keyboard,
                                then tap enter to reset course from current position to the same waypoint
                        </UL>
                </UL>
            <LI><B>VOR</B> - standard HSI/OBS dial nav to/from waypoint.
                Treats all waypoints as if they were a VOR,
                even localizers.
            <LI><B>ADF</B> - ADF-style needle points to waypoint.
                Treats all waypoints as if they were an NDB.
        </UL>
        Valid only for localizer waypoints:
        <UL>
            <LI><B>LOC</B> - standard HSI/OBS dial nav on localizer course line
            <LI><B>LOCBC</B> - standard HSI/OBS dial nav on localizer back-course line
        </UL>
        Valid only for localizer waypoints with glideslope:
        <UL>
            <LI><B>ILS</B> - standard HSI/OBS dial nav on localizer course line
                            including glide slope
                <UL>
                    <LI><FONT COLOR=RED><B>WARNING:</B>  Altitude is notoriously inaccurate in these
                        devices so be doubly cautious when using the glide slope indication
                        provided by this app.</FONT>
                </UL>
        </UL>
        <HR>
        <H3> Menu Pages </H3>
        <TABLE>
            <TR>
                <TD><IMG SRC="menu-page.png" WIDTH=320 HEIGHT=320></TD>
                <TD>
                    <UL>
                        <LI><B>&#9664;BACK</B> - goes back to nav dial or moving map page
                        <LI><B>SATS</B> - show GPS satellite status (also has a magnetic compass)
                        <LI><B>GPS</B> - selects GPS source (<A HREF="#gps">see below</A>)
                        <LI><B>UPDDB</B> - select database and download updates (requires Internet access)
                            <UL>
                                <LI><FONT COLOR=GREEN>GREEN</FONT> - database is up to date and will remain so for a few days
                                <LI><FONT COLOR=YELLOW>YELLOW</FONT> - database is up to date but will expire soon
                                <LI><FONT COLOR=RED>RED</FONT> - database is expired
                            </UL>
                            <UL>
                                <LI>FAA (US only) - selects the FAA database, updated to current 28-day cycle
                                <LI>ourairports.com - selects the <A HREF="https://ourairports.com">ourairports.com</A> database.
                                    <FONT COLOR=RED>Be especially cautious when using this database.  As a crowd sourced database,
                                        it may contain badly outdated information!</FONT>  If you find inaccuracies, please inform
                                        <A HREF="https://ourairports.com">ourairports.com</A> so the database can be updated.
                                    <B>In any case, please support <A HREF="https://ourairports.com">ourairports.com</A> if you use this database.</B>
                            </UL>
                        <LI><B>HSI</B> - select HSI mode for nav dial (airplane always at top); else OBS with yellow triangle always at top
                        <LI><B>Ambient</B> - slow to 20 seconds per GPS sample in ambient mode; else maintain 1 second per sample<BR>
                                Note:  Only applies when using internal GPS source.  Retains same rate when using external GPS
                                received over Bluetooth or WiFi.
                        <LI><B>Time Dots</B> - display time received from GPS as dots around the perimeter of displays
                            <UL>
                                <LI><FONT COLOR=RED>RED</FONT> dot indicates HOURS
                                <LI><FONT COLOR=GREEN>GREEN</FONT> dot indicates MINUTES
                                <LI><FONT COLOR=BLUE>BLUE</FONT> dot indicates SECONDS
                            </UL>
                        <LI><B>SEND</B> - send location to another device using Bluetooth and/or WiFi (<A HREF="#send">see below</A>)
                        <LI><B>MORE&#9654;</B> - goes to next menu page
                    </UL>
                </TD>
            </TR>
            <TR>
                <TD><IMG SRC="menu2-page.png" WIDTH=320 HEIGHT=320></TD>
                <TD>
                    <UL>
                        <LI><B>&#9664;BACK</B> - goes back a page
                        <LI><B>Fill Chin</B> - has nav dial fill chin on devices with a chin
                            (only present on devices with a chin)
                            <TABLE>
                                <TR><TD>Filled Chin</TD><TD>Empty Chin</TD></TR>
                                <TR><TD><IMG SRC="filled-chin.png" WIDTH=107 HEIGHT=107></TD><TD><IMG SRC="empty-chin.png" WIDTH=107 HEIGHT=107></TD></TR>
                            </TABLE>
                        <LI><B>ABOUT</B> - display version and database expiration information
                        <LI><B>EXIT</B> - close app
                        <LI><B>RESET</B> - reset settings to factory defaults and purge database
                    </UL>
                </TD>
            </TR>
        </TABLE>
        <HR>
        <H3> Moving Map Page </H3>
        <TABLE>
            <TR><TD ALIGN=CENTER>zoom out</TD></TR>
            <TR><TD><IMG SRC="map-page.png"></TD>
                <TD><UL>
                    <LI><FONT COLOR=GREEN>GREEN</FONT> - airports (up to 20 with longest runways)
                    <LI><FONT COLOR=CYAN>CYAN</FONT> - VORs (if not 20 airports)
                    <LI><FONT COLOR=YELLOW>YELLOW</FONT> - range ring at half radius
                    <LI><FONT COLOR=MAGENTA>MAGENTA</FONT> - course line and destination waypoint<BR>
                        Course line shown is always great circle regardless of mode selected for nav dial page.
                </UL></TD>
            </TR>
            <TR><TD ALIGN=CENTER>zoom in</TD></TR>
        </TABLE>
        <P>The moving map page can be accessed by swiping right-to-left on the
            nav dial page.</P>
        <P>To access the other pages from the moving map page, touch near the center of the screen and a diagram will appear:</P>
        <TABLE>
            <TR>
                <TD><IMG SRC="map-menu.png" WIDTH=320 HEIGHT=320></TD>
                <TD>
                    Swipe in the direction of the double arrows to select the function wanted:
                    <UL>
                        <LI><B>back</B> - go back to nav dial page
                        <LI><B>menu</B> - access the menu pages
                        <LI><B>near</B> - access nearest airport selection page
                        <LI><B>rwys</B> - display runway diagram for current airport
                    </UL>
                </TD>
            </TR>
        </TABLE>
        <HR>
        <A NAME="voice"></A>
        <H3> Voice Recognition </H3>
        <P>Voice can be used to input the waypoint identifier being navigated to on the 
            waypoint input page.
            It can be selected by clicking the <B>VOICE</B>
            button on the waypoint input page.  It recognizes the usual aviation prowords
            (alpha, bravo, etc) and the
            decimal digits (see table below).</P>
        <P>NOTE:  Some watches may not have voice input capability.</P>
        <P>NOTE:  This feature may require internet access to function.  Usually displays a message indicating unable
            to connect to Google or something similar.  Swipe the voice input panel away then click on the ident entry
            box to use text input instead.</P>
        <OL>
            <LI>open the waypoint input page by swiping downward on the nav dial page
            <LI>tap the <B>VOICE</B> button, speech entry page should show
            <LI>tap microphone icon on speech page
            <LI>say prowords for the desired waypoint
                <UL>
                    <LI>do not pause between words, it will think you have finished
                    <LI>say "bravo victor yankee", not "bee vee wye" or "beverly"
                    <LI>say "one bravo two", not "one bee two" or "katama"
                    <LI>say "kilo mike hotel tango" (for the airport), not "k m h t" or "manchester nowhere near the sea"
                    <LI>say "mike hotel tango" for the VOR
                    <LI>say "bravo one niner", not "bee nineteen" or "biddefid"
                </UL>
            <LI>click checkmark icon
        </OL>
        <P>List of recognized prowords (other words are ignored):</P>
        <UL><LI>
            <TABLE>
                <TR><TH>0</TH><TD>zero</TD><TH>A</TH><TD>alpha</TD><TH>K</TH><TD>kilo</TD><TH>U</TH><TD>uniform</TD></TR>
                <TR><TH>1</TH><TD>one won</TD><TH>B</TH><TD>bravo</TD><TH>L</TH><TD>lima</TD><TH>V</TH><TD>victor</TD></TR>
                <TR><TH>2</TH><TD>to too two</TD><TH>C</TH><TD>charlie</TD><TH>M</TH><TD>mike</TD><TH>W</TH><TD>whiskey</TD></TR>
                <TR><TH>3</TH><TD>three tree</TD><TH>D</TH><TD>delta</TD><TH>N</TH><TD>november</TD><TH>X</TH><TD>x-ray xray</TD></TR>
                <TR><TH>4</TH><TD>for four</TD><TH>E</TH><TD>echo</TD><TH>O</TH><TD>oscar</TD><TH>Y</TH><TD>yankee</TD></TR>
                <TR><TH>5</TH><TD>fife five</TD><TH>F</TH><TD>fox foxtrot</TD><TH>P</TH><TD>papa</TD><TH>Z</TH><TD>zulu</TD></TR>
                <TR><TH>6</TH><TD>six</TD><TH>G</TH><TD>golf</TD><TH>Q</TH><TD>quebec</TD></TR>
                <TR><TH>7</TH><TD>seven</TD><TH>H</TH><TD>hotel</TD><TH>R</TH><TD>romeo</TD></TR>
                <TR><TH>8</TH><TD>ate eight</TD><TH>I</TH><TD>india</TD><TH>S</TH><TD>ciara sierra</TD></TR>
                <TR><TH>9</TH><TD>nine niner</TD><TH>J</TH><TD>juliet</TD><TH>T</TH><TD>tango</TD></TR>
            </TABLE>
            <LI><B>off</B> clear waypoint and turn GPS off
        </UL>
        <HR>
        <A NAME="gps"></A>
        <H3> GPS </H3>
        <P>Selects which GPS device is used.</P>
        <UL>
            <LI><B>Internal</B> - uses watch's internal GPS receiver
            <LI><B>Bluetooth</B> - uses external device's GPS connected via Bluetooth
                <OL>
                    <LI>Select paired device.  If not listed, use watch's
                        settings to pair the device, then re-select
                        <B>Bluetooth</B> from this menu.
                    <LI>Select UUID.  Usually the one beginning with 00001101
                        works, but the app on the external device may be using
                        a different one.  If the UUID being used by the device
                        does not appear in the selection list:
                        <OL>
                            <LI>close the HSIWatch app (BACK BACK BACK ... as needed)
                            <LI>make sure the device is on
                            <LI>start the GPS app on the device (if any)
                            <LI>un-pair and re-pair the watch with the device
                            <LI>restart the HSIWatch app
                        </OL>
                </OL>
                For example, <A HREF="https://play.google.com/store/apps/details?id=com.outerworldapps.gpsblue">GPSBlue</A>
                can be used on a phone to relay GPS from the phone to the watch using Bluetooth.  It has also been tested
                with a DUAL XGPS150A.
            <LI><B>WiFi UDP</B> - uses external device's GPS connected via WiFi using UDP
                <UL>
                    <LI><B>port</B> - UDP port number the external device sends packets on (Stratux uses port 4000)
                </UL>
                Can be used with Stratux-like devices that relay GPS over WiFi in UDP packets.
            <LI><B>Simulator</B> - generates GPS locations for testing
        </UL>
        <A NAME="send"></A>
        <H3> SEND </H3>
        <P>Transmits GPS positions (either real or simluated) on Bluetooth and/or WiFi UDP
            to such as a tablet or phone based EFB app.  Uses NMEA GPGGA,GPRMC messages to send
            GPS position reports.</P>
        <UL>
            <LI><B>UDP</B> - fill in ip address of EFB device and the port number the EFB app is listening on
            <LI><B>Bluetooth</B> - normally the 00001101 UUID prefix works, but if it fails try something
                    else like 00001102.  Make sure the EFB app is connecting to the same UUID and that the
                    EFB device is paired to the watch.
        </UL>
        <HR>
    </BODY>
</HTML>
