package com.v2retail.util;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.v2retail.commons.SapJsonRows;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommonUtils {

    public static void showToast(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    public static void hideKeyboard(Activity activity) {
        try {
            if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
            }
        }catch (Exception e) {}
    }

    public static JSONArray sortJsonArray(JSONArray jsonArrToSort,String key,boolean ignoreTopElement) throws Exception{
        JSONArray sortedJsonArray = new JSONArray();

        List<JSONObject> jsonValues = new ArrayList<>();
        int startIndex;
        try {
            startIndex = ignoreTopElement ? SapJsonRows.startIndex(jsonArrToSort) : 0;
        } catch (JSONException e) {
            startIndex = ignoreTopElement ? 1 : 0;
        }
        for (int i = startIndex; i < jsonArrToSort.length(); i++) {
            jsonValues.add(jsonArrToSort.getJSONObject(i));
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b){
                String valA = new String();
                String valB = new String();
                try {
                    valA = (String) a.get(key);
                    valB = (String) b.get(key);
                }catch(JSONException jExce){
                }
                return valA.compareToIgnoreCase(valB);
            }
        });
        for (int i = 0; i < jsonValues.size(); i++) {
            sortedJsonArray.put(jsonValues.get(i));
        }
        return sortedJsonArray;
    }
}
