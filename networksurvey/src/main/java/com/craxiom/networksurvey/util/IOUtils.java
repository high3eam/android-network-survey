/*
 * Copyright (C) 2019 Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.craxiom.networksurvey.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.craxiom.networksurvey.Application;
import com.craxiom.networksurvey.BuildConfig;
import com.craxiom.networksurvey.R;
//import com.google.zxing.integration.android.IntentIntegrator;

import java.io.File;

import static com.craxiom.networksurvey.util.LocationUtils.isValidLatitude;
import static com.craxiom.networksurvey.util.LocationUtils.isValidLongitude;

/**
 * Originally from the GPS Test open source Android app.  https://github.com/barbeau/gpstest
 */
public class IOUtils
{

    public static final String TAG = "IOUtils";

    /**
     * Returns the ground truth location encapsulated in the Intent if the provided Intent has a
     * SHOW_RADAR action (com.google.android.radar.SHOW_RADAR) with a valid latitude and longitude, or
     * null if the Intent doesn't have a SHOW_RADAR action or the intent has an invalid latitude or longitude
     *
     * @param intent Intent possibly containing the RADAR action
     * @return the ground truth location encapsulated in the Intent if the provided Intent has a
     * SHOW_RADAR action (com.google.android.radar.SHOW_RADAR) with a valid latitude and longitude, or
     * null if the Intent doesn't have a SHOW_RADAR action or the intent has an invalid latitude or longitude
     */
    public static Location getLocationFromIntent(Intent intent)
    {
        Location groundTruth = null;
        if (isShowRadarIntent(intent))
        {
            double lat = Double.NaN, lon = Double.NaN;
            float latFloat = intent.getFloatExtra(Application.get().getString(R.string.radar_lat_key), Float.NaN);
            float lonFloat = intent.getFloatExtra(Application.get().getString(R.string.radar_lon_key), Float.NaN);
            if (isValidLatitude(latFloat) && isValidLongitude(lonFloat))
            {
                // Use the float values
                lat = (double) latFloat;
                lon = (double) lonFloat;
            } else
            {
                // Try parsing doubles
                double latDouble = intent.getDoubleExtra(Application.get().getString(R.string.radar_lat_key), Double.NaN);
                double lonDouble = intent.getDoubleExtra(Application.get().getString(R.string.radar_lon_key), Double.NaN);
                if (isValidLatitude(latDouble) && isValidLongitude(lonDouble))
                {
                    lat = latDouble;
                    lon = lonDouble;
                }
            }

            if (isValidLatitude(lat) && isValidLongitude(lon))
            {
                groundTruth = new Location("ground_truth");
                groundTruth.setLatitude(lat);
                groundTruth.setLongitude(lon);
                if (intent.hasExtra(Application.get().getString(R.string.radar_alt_key)))
                {
                    float altitude = intent.getFloatExtra(Application.get().getString(R.string.radar_alt_key), Float.NaN);
                    if (!Float.isNaN(altitude))
                    {
                        groundTruth.setAltitude(altitude);
                    } else
                    {
                        // Try the double version
                        double altitudeDouble = intent.getDoubleExtra(Application.get().getString(R.string.radar_alt_key), Double.NaN);
                        if (!Double.isNaN(altitudeDouble))
                        {
                            groundTruth.setAltitude(altitudeDouble);
                        }
                    }
                }
            }
        }
        return groundTruth;
    }

    /**
     * Returns true if the provided intent has the SHOW_RADAR action (com.google.android.radar.SHOW_RADAR), or false if it does not
     *
     * @param intent
     * @return true if the provided intent has the SHOW_RADAR action (com.google.android.radar.SHOW_RADAR), or false if it does not
     */
    public static boolean isShowRadarIntent(Intent intent)
    {
        return intent != null &&
                intent.getAction() != null &&
                intent.getAction().equals(Application.get().getString(R.string.show_radar_intent));
    }

    /**
     * Creates a SHOW_RADAR intent from the provided Location
     *
     * @param location location information to be added to the intent
     * @return a SHOW_RADAR intent with the provided latitude, longitude, and, if provided, altitude, all in WGS-84
     */
    public static Intent createShowRadarIntent(Location location)
    {
        return createShowRadarIntent(location.getLatitude(), location.getLongitude(), location.hasAltitude() ? location.getAltitude() : null);
    }

    /**
     * Creates a SHOW_RADAR intent with the provided latitude, longitude, and, if provided, altitude, all in WGS-84.
     *
     * @param lat latitude in WGS84
     * @param lon longitude in WGS84
     * @param alt altitude in meters above WGS84 ellipsoid, or null if altitude shouldn't be included
     * @return a SHOW_RADAR intent with the provided latitude, longitude, and, if provided, altitude, all in WGS-84
     */
    public static Intent createShowRadarIntent(double lat, double lon, Double alt)
    {
        Intent intent = new Intent(Application.get().getString(R.string.show_radar_intent));
        intent.putExtra(Application.get().getString(R.string.radar_lat_key), lat);
        intent.putExtra(Application.get().getString(R.string.radar_lon_key), lon);
        if (alt != null && !Double.isNaN(alt))
        {
            intent.putExtra(Application.get().getString(R.string.radar_alt_key), alt);
        }
        return intent;
    }

    public static void openQrCodeReader(AppCompatActivity activity)
    {
        // Open ZXing to scan GEO URI from QR Code
        //IntentIntegrator integrator = new IntentIntegrator(activity);
        //integrator.initiateScan();
    }

    /**
     * Returns a location from the provided Geo URI (RFC 5870) or null if one can't be parsed
     *
     * @param geoUri a Geo URI following RFC 5870 (e.g., geo:37.786971,-122.399677)
     * @return a location from the provided Geo URI (RFC 5870) or null if one can't be parsed
     */
    public static Location getLocationFromGeoUri(String geoUri)
    {
        if (TextUtils.isEmpty(geoUri) || !geoUri.startsWith(Application.get().getString(R.string.geo_uri_prefix)))
        {
            return null;
        }
        Location l = null;

        String[] noPrefix = geoUri.split(":");
        String[] coords = noPrefix[1].split(",");
        if (isValidLatitude(Double.valueOf(coords[0])) && isValidLongitude(Double.valueOf(coords[1])))
        {
            l = new Location("Geo URI");
            l.setLatitude(Double.valueOf(coords[0]));
            l.setLongitude(Double.valueOf(coords[1]));
            if (coords.length == 3)
            {
                l.setAltitude(Double.valueOf(coords[2]));
            }
        }

        return l;
    }

    /**
     * Returns a Geo URI (RFC 5870) from the provided location, or null if one can't be created
     *
     * @param location
     * @param includeAltitude true if altitude should be included in the Geo URI, false if it should be omitted. If the location doesn't have an altitude value this parameter has no effect.
     * @return a Geo URI (RFC 5870) from the provided location, or null if one can't be created
     */
    public static String createGeoUri(Location location, boolean includeAltitude)
    {
        if (location == null)
        {
            return null;
        }
        String geoUri = Application.get().getString(R.string.geo_uri_prefix);
        geoUri += location.getLatitude() + ",";
        geoUri += location.getLongitude();
        if (location.hasAltitude() && includeAltitude)
        {
            geoUri += "," + location.getAltitude();
        }
        return geoUri;
    }

    /**
     * Copies the provided location string to the clipboard
     *
     * @param location the location string to copy to the clipboard
     */
    public static void copyToClipboard(String location)
    {
        ClipboardManager clipboard = (ClipboardManager) Application.get().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(Application.get().getString(R.string.pref_file_location_output_title), location);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Returns a string to be shared as plain text (e.g., via clipboard)
     *
     * @param location
     * @param includeAltitude true if altitude should be included in the output, false if it should
     *                        not.  If the location doesn't have an altitude this variable has no effect.
     * @return a string to be shared as plain text (e.g., via clipboard)
     */
    public static String createLocationShare(Location location, boolean includeAltitude)
    {
        if (location == null)
        {
            return null;
        }
        String locationString = location.getLatitude() + "," + location.getLongitude();
        if (location.hasAltitude() && includeAltitude)
        {
            locationString += "," + location.getAltitude();
        }
        return locationString;
    }

    /**
     * Returns a string to be shared as plain text (e.g., via clipboard) based on the provided
     * pre-formatted latitude, longitude, and (optionally) altitude
     *
     * @return a string to be shared as plain text (e.g., via clipboard) based on the provided
     * * pre-formatted latitude, longitude, and (optionally) altitude
     */
    public static String createLocationShare(String latitude, String longitude, String altitude)
    {
        String locationString = latitude + "," + longitude;
        if (!TextUtils.isEmpty(altitude))
        {
            locationString += "," + altitude;
        }
        return locationString;
    }

    /**
     * Sends the specified file via the ACTION_SEND Intent
     *
     * @param activity
     * @param fileUri  Android URI for the File to be attached
     */
    public static void sendLogFile(Activity activity, android.net.Uri fileUri)
    {
        Log.d(TAG, "Sending " + fileUri);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "GnssLog from GPSTest");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        activity.startActivity(Intent.createChooser(emailIntent, Application.get().getString(R.string.send_log)));
    }

    /**
     * Returns an Android URI for the provided File that can be used to attach the file to a message via ACTION_SEND Intent
     *
     * @param context
     * @param file
     * @return an Android URI for the provided File that can be used to attach the file to a message via ACTION_SEND Intent
     */
    public static android.net.Uri getUriFromFile(Context context, File file)
    {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
    }
}
