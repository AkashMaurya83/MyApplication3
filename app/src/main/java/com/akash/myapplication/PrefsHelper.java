package com.akash.myapplication;


    import android.content.Context;
import android.content.SharedPreferences;

    public class PrefsHelper {
        private static final String PREF_NAME = "SecureRingPrefs";
        private static final String KEY_PASSWORD = "user_password";
        private static final String KEY_IS_FIRST_TIME = "is_first_time";

        private SharedPreferences sharedPreferences;
        private SharedPreferences.Editor editor;

        public PrefsHelper(Context context) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }

        // Check karna ke pehli baar open ho raha hai ya nahi
        public boolean isFirstTime() {
            return sharedPreferences.getBoolean(KEY_IS_FIRST_TIME, true);
        }

        // Naya password set karna (Pehli baar ya Change karne ke liye)
        public void setPassword(String password) {
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_IS_FIRST_TIME, false); // Ab first time nahi rahega
            editor.apply();
        }

        // Login ke liye password check karna
        public boolean checkPassword(String enteredPassword) {
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            return savedPassword.equals(enteredPassword);
        }

        // Forgot Password ke liye (SMS se jab reset command aayega tab use hoga)
        public void resetPassword(String newPassword) {
            editor.putString(KEY_PASSWORD, newPassword);
            editor.apply();
        }
    }

