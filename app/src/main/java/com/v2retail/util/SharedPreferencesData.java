package com.v2retail.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesData {
    Context context;
    SharedPreferences preferences = null;
    SharedPreferences.Editor editor = null;
    AlertBox box;
    public SharedPreferencesData(Context con) {
        context = con;

        box = new AlertBox(context);
        preferences = context.getSharedPreferences("login", 0);
    }

    public String read(String key) {

        String data = null;
        if (key != null || key.length() > 0 || !key.equals("")) {

            data = preferences.getString(key, null);
        } else {
            box.getBox("Alert!", "Empty Key ");
        }
        return data;
    }

    /**
     * SAP RFCs use {@code IM_USER}. Login stores {@code USER}; some sessions only have {@code USERNAME}.
     * Falls back to USERNAME and back-fills USER when missing.
     */
    public String getSapUserId() {
        String user = read("USER");
        if (user == null || user.trim().isEmpty()) {
            user = read("USERNAME");
        }
        if (user == null || user.trim().isEmpty()) {
            return "";
        }
        user = user.trim().toUpperCase();
        String storedUser = read("USER");
        if (storedUser == null || storedUser.trim().isEmpty()) {
            write("USER", user);
        }
        return user;
    }

    public boolean isKeyExists(String key){
        if (key != null && !key.isEmpty()) {
            return preferences.contains(key);
        }
        return false;
    }

    public void write(String key, String value) {
        if (key != null || key.length() > 0 || !key.equals("")) {
            if (value != null || value.length() > 0 || !value.equals("")) {
                editor = preferences.edit();
                editor.putString(key, value);
                editor.commit();
            }
        } else {
            box.getBox("Alert!", "Empty Login Key-Value");
        }
    }

    public void delete(String key) {
        if (key != null || key.length() > 0 || !key.equals("")) {
            editor = preferences.edit();

            editor.remove(key);

            editor.commit();
        } else {
            box.getBox("Alert!", "Empty Key, Can't delete null value ");
        }
    }

    public void clearAll() {
        editor = preferences.edit();

        editor.clear();

        editor.commit();
    }
}
