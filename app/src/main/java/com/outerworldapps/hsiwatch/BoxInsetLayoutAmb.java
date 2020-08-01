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

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

import androidx.wear.widget.BoxInsetLayout;

// wraps BoxInsetLayout changing all the contents to grayscale when in ambient mode
public class BoxInsetLayoutAmb extends BoxInsetLayout implements KeepGpsOn {
    private Bitmap backButtonImage;
    private ColorStateList backgroundColor;
    private ColorStateList foregroundColor;
    private HashMap<View,ColorStateList> backgroundTintLists;
    private HashMap<View,ColorStateList> foregroundTintLists;
    private HashSet<View> visibleInputViews;
    private View backButtonView;

    public BoxInsetLayoutAmb (Context ctx)
    {
        super (ctx);
    }

    public BoxInsetLayoutAmb (Context context, AttributeSet attrs)
    {
        super (context, attrs);
    }

    public BoxInsetLayoutAmb (Context context, AttributeSet attrs, int defStyle)
    {
        super (context, attrs, defStyle);
    }

    // called by the activity's onEnterAmbient() and onExitAmbient() methods
    //  input:
    //   ambient = true: entering ambient mode
    //            false: exiting ambient mode
    public void setAmbient (boolean ambient)
    {
        if (ambient) {
            if (backgroundColor == null) {
                backgroundColor = getContext ().getColorStateList (R.color.amb_background);
                foregroundColor = getContext ().getColorStateList (R.color.amb_foreground);
            }
            backButtonImage     = null;
            backButtonView      = null;
            backgroundTintLists = new HashMap<> ();
            foregroundTintLists = new HashMap<> ();
            visibleInputViews   = new HashSet<> ();
            setAmbientOn (this);
        } else if (backgroundTintLists != null) {
            setAmbientOff ();
            backButtonImage     = null;
            backButtonView      = null;
            backgroundTintLists = null;
            foregroundTintLists = null;
            visibleInputViews   = null;
        }
        invalidate ();
    }

    // make everything grayscale
    //  input:
    //   view = view to make grayscale
    //          also does all children if any
    private void setAmbientOn (View view)
    {
        if (view.getVisibility () == VISIBLE) {
            if ((view instanceof Button) || (view instanceof EditText)) {
                visibleInputViews.add (view);
                view.setVisibility (INVISIBLE);
            } else {
                backgroundTintLists.put (view, view.getBackgroundTintList ());
                foregroundTintLists.put (view, view.getForegroundTintList ());
                view.setBackgroundTintList (backgroundColor);
                view.setForegroundTintList (foregroundColor);

                // set all children to ambient mode
                if (view instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) view;
                    int nchilds = viewGroup.getChildCount ();
                    for (int i = 0; i < nchilds; i ++) {
                        View child = viewGroup.getChildAt (i);

                        // check for <BACK button
                        if ((child instanceof Button) && (((Button) child).getText ().toString ().equals ("\u25C0BACK"))) {
                            setUpBackButton (child);
                        }

                        // set child to ambient mode
                        setAmbientOn (child);
                    }
                }
            }
        }
    }

    // restore all views to normal
    private void setAmbientOff ()
    {
        for (View view : foregroundTintLists.keySet ()) {
            view.setBackgroundTintList (backgroundTintLists.get (view));
            view.setForegroundTintList (foregroundTintLists.get (view));
        }
        for (View view : visibleInputViews) {
            view.setVisibility (VISIBLE);
        }
    }

    // set up image where <BACK button is in case display goes completely black in ambient mode
    private void setUpBackButton (View child)
    {
        // remember where the back button is
        backButtonView   = child;

        // read main-round.png icon file
        int buttonWidth  = backButtonView.getWidth ();
        int buttonHeight = backButtonView.getHeight ();
        AssetManager assetManager = getContext ().getAssets ();
        try {
            try (InputStream imageStream = assetManager.open ("main-round.png")) {
                Bitmap imageBitmap = BitmapFactory.decodeStream (imageStream);
                if (imageBitmap == null) throw new IOException ("image file corrupt");

                // scale image to be same size as back button
                // but preserve image aspect ratio
                int imageWidth  = imageBitmap.getWidth ();
                int imageHeight = imageBitmap.getHeight ();
                int sampleSize  = 1;
                while ((imageWidth > buttonWidth * sampleSize) || (imageHeight > buttonHeight * sampleSize)) {
                    sampleSize ++;
                }
                imageWidth  /= sampleSize;
                imageHeight /= sampleSize;
                backButtonImage = Bitmap.createScaledBitmap (imageBitmap, imageWidth, imageHeight, true);

                // grayscale image for ambient mode
                int[] pixels = new int[imageWidth*imageHeight];
                backButtonImage.getPixels (pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight);
                for (int j = pixels.length; -- j >= 0;) {
                    int p = pixels[j];
                    int r = ((p >> 16) & 255);
                    int g = ((p >> 16) & 255);
                    int b = ((p >> 16) & 255);
                    int m = (int) (Math.sqrt ((r * r + g * g + b * b) / 3.0));
                    pixels[j] = (p & 0xFF000000) | (m * 0x010101);
                }
                backButtonImage.setPixels (pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight);
            }
        } catch (IOException ioe) {
            Log.w (MainActivity.TAG, "error reading asset main-round.png", ioe);
        }
    }

    // draw all image children then maybe draw <BACK button image overlay
    @Override
    protected void dispatchDraw (Canvas canvas)
    {
        super.dispatchDraw (canvas);
        if (backButtonImage != null) {
            int buttonWidth    = backButtonView.getWidth ();
            int buttonHeight   = backButtonView.getHeight ();
            float centerx      = backButtonView.getX () + buttonWidth  / 2.0F;
            float centery      = backButtonView.getY () + buttonHeight / 2.0F;
            Bitmap imageBitmap = backButtonImage;
            int imageWidth     = imageBitmap.getWidth ();
            int imageHeight    = imageBitmap.getHeight ();
            canvas.drawBitmap (imageBitmap, centerx - imageWidth / 2.0F, centery - imageHeight / 2.0F, null);
        }
    }
}
