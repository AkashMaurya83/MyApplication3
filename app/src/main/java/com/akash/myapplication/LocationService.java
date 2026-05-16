package com.akash.myapplication;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

public class LocationService extends Service {

    private String senderNumber = "";
    private LocationManager locationManager;
    private Location bestLocation = null;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 60000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            senderNumber = intent.getStringExtra("sender");
            startLocationSearch();
        }
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void startLocationSearch() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            stopSelf();
            return;
        }

        Location gpsLast = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location netLast = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        
        bestLocation = getBetterLocation(gpsLast, netLast);

        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.getTime() < 30000) && bestLocation.getAccuracy() < 30) {
            sendLocationAndStop();
            return;
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
            }
        } catch (Exception e) {
            Log.e("LocationService", "Error requesting updates: " + e.getMessage());
        }

        timeoutHandler.postDelayed(this::sendLocationAndStop, TIMEOUT_MS);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            bestLocation = getBetterLocation(bestLocation, location);
            
            if (bestLocation != null && bestLocation.getAccuracy() < 20) {
                sendLocationAndStop();
            }
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(@NonNull String provider) {}
        @Override public void onProviderDisabled(@NonNull String provider) {}
    };

    private void sendLocationAndStop() {
        timeoutHandler.removeCallbacksAndMessages(null);
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }
        } catch (Exception ignored) {}

        if (bestLocation != null) {
            sendLocationSms(bestLocation);
        } else {
            sendSms(senderNumber, "Security Guard: Error - Could not find location. Please ensure GPS is ON.");
        }
        stopSelf();
    }

    private void sendLocationSms(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float accuracy = location.getAccuracy();
        String provider = location.getProvider();
        
        String providerName = (provider != null) ? provider.toUpperCase() : "UNKNOWN";
        
        String mapsUrl = "https://www.google.com/maps?q=" + lat + "," + lon;
        String message = "Security Guard: Real-time Location\n" +
                         "Accuracy: " + (int)accuracy + " meters\n" +
                         "Source: " + providerName + "\n" +
                         mapsUrl;

        sendSms(senderNumber, message);
    }

    private void sendSms(String number, String msg) {
        if (number == null || number.isEmpty()) return;
        try {
            android.telephony.SmsManager smsManager = getSystemService(android.telephony.SmsManager.class);
            smsManager.sendTextMessage(number, null, msg, null, null);
        } catch (Exception e) {
            Log.e("LocationService", "SMS failed: " + e.getMessage());
        }
    }

    private Location getBetterLocation(Location loc1, Location loc2) {
        if (loc1 == null) return loc2;
        if (loc2 == null) return loc1;

        long timeDelta = loc2.getTime() - loc1.getTime();
        boolean isSignificantlyNewer = timeDelta > 120000;
        boolean isSignificantlyOlder = timeDelta < -120000;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) return loc2;
        if (isSignificantlyOlder) return loc1;

        int accuracyDelta = (int) (loc2.getAccuracy() - loc1.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;

        if (isMoreAccurate) return loc2;
        if (isNewer && !isLessAccurate) return loc2;

        return loc1;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
