package com.v2retail.dotvik.store;

import android.content.Context;
import android.util.Log;

import com.v2retail.commons.UIFuncs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Device-local cache for {@code ZWM_ST_GRT_GET_PICKLIST_DATA} (ET_DATA + ET_EAN_DATA).
 * Article / EAN scans look up this cache instead of calling SAP again.
 */
final class StoreGrtPicklistLocalCache {

    private static final String TAG = "StoreGrtPicklistCache";
    private static final String DIR = "store_grt_picklist";
    private static final int MAGIC = 0x53475254;
    private static final int VERSION = 1;
    private static final long TTL_MS = 12L * 60L * 60L * 1000L;

    private StoreGrtPicklistLocalCache() {
    }

    static FragmentStoreGrtProcess.PicklistDataParseResult load(Context context,
                                                                String plant,
                                                                String picklist) {
        File file = cacheFile(context, plant, picklist);
        if (file == null || !file.isFile() || file.length() == 0) {
            return null;
        }
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 64 * 1024));
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != MAGIC || version != VERSION) {
                Log.w(TAG, "ignore cache bad header plant=" + plant + " picklist=" + picklist);
                closeQuietly(in);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                return null;
            }
            long savedAt = in.readLong();
            if (System.currentTimeMillis() - savedAt > TTL_MS) {
                Log.d(TAG, "cache expired plant=" + plant + " picklist=" + picklist);
                closeQuietly(in);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                return null;
            }
            FragmentStoreGrtProcess.PicklistDataParseResult result =
                    new FragmentStoreGrtProcess.PicklistDataParseResult();
            result.rdcPlant = readUtf(in);
            int articleCount = in.readInt();
            for (int i = 0; i < articleCount; i++) {
                FragmentStoreGrtProcess.PicklistArticleLine line =
                        new FragmentStoreGrtProcess.PicklistArticleLine();
                line.picklistNo = readUtf(in);
                line.source = readUtf(in);
                line.majCat = readUtf(in);
                line.size1 = readUtf(in);
                line.floor = readUtf(in);
                line.bgt = readUtf(in);
                line.matnr = readUtf(in);
                line.matkl = readUtf(in);
                indexArticle(result, line);
            }
            int eanCount = in.readInt();
            for (int i = 0; i < eanCount; i++) {
                FragmentStoreGrtProcess.EanRecord record = new FragmentStoreGrtProcess.EanRecord();
                record.matnr = readUtf(in);
                record.ean11 = readUtf(in);
                record.umrez = in.readDouble();
                if (record.umrez <= 0) {
                    record.umrez = 1;
                }
                indexEan(result, record);
            }
            Log.d(TAG, "cache load ok picklist=" + picklist
                    + " articles=" + result.articlesByMatnr.size()
                    + " eans=" + result.eanByScanCode.size()
                    + " bytes=" + file.length());
            return result;
        } catch (Exception e) {
            Log.e(TAG, "cache load failed picklist=" + picklist, e);
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    static void save(Context context, String plant, String picklist,
                     FragmentStoreGrtProcess.PicklistDataParseResult result) {
        if (context == null || result == null || picklist == null || picklist.isEmpty()) {
            return;
        }
        if (result.articlesByMatnr.isEmpty()) {
            return;
        }
        File file = cacheFile(context, plant, picklist);
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.w(TAG, "cache dir create failed " + parent.getAbsolutePath());
            return;
        }
        Set<FragmentStoreGrtProcess.PicklistArticleLine> articles =
                new LinkedHashSet<>(result.articlesByMatnr.values());
        Set<FragmentStoreGrtProcess.EanRecord> eans =
                new LinkedHashSet<>(result.eanByScanCode.values());
        File tmp = new File(file.getAbsolutePath() + ".tmp");
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024));
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeLong(System.currentTimeMillis());
            writeUtf(out, result.rdcPlant);
            out.writeInt(articles.size());
            for (FragmentStoreGrtProcess.PicklistArticleLine line : articles) {
                writeUtf(out, line.picklistNo);
                writeUtf(out, line.source);
                writeUtf(out, line.majCat);
                writeUtf(out, line.size1);
                writeUtf(out, line.floor);
                writeUtf(out, line.bgt);
                writeUtf(out, line.matnr);
                writeUtf(out, line.matkl);
            }
            out.writeInt(eans.size());
            for (FragmentStoreGrtProcess.EanRecord record : eans) {
                writeUtf(out, record.matnr);
                writeUtf(out, record.ean11);
                out.writeDouble(record.umrez > 0 ? record.umrez : 1);
            }
            out.flush();
            closeQuietly(out);
            out = null;
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "cache replace delete failed " + file.getName());
            }
            if (!tmp.renameTo(file)) {
                Log.w(TAG, "cache rename failed " + tmp.getName());
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
                return;
            }
            Log.d(TAG, "cache save ok picklist=" + picklist
                    + " articles=" + articles.size()
                    + " eans=" + eans.size()
                    + " bytes=" + file.length());
        } catch (Exception e) {
            Log.e(TAG, "cache save failed picklist=" + picklist, e);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        } finally {
            closeQuietly(out);
        }
    }

    static void indexArticle(FragmentStoreGrtProcess.PicklistDataParseResult result,
                             FragmentStoreGrtProcess.PicklistArticleLine line) {
        if (result == null || line == null || line.matnr == null || line.matnr.isEmpty()) {
            return;
        }
        result.articlesByMatnr.put(line.matnr.toUpperCase(Locale.ROOT), line);
        String noZeros = UIFuncs.removeLeadingZeros(line.matnr);
        if (noZeros != null && !noZeros.isEmpty()) {
            result.articlesByMatnr.putIfAbsent(noZeros.toUpperCase(Locale.ROOT), line);
        }
    }

    static void indexEan(FragmentStoreGrtProcess.PicklistDataParseResult result,
                         FragmentStoreGrtProcess.EanRecord record) {
        if (result == null || record == null || record.matnr == null || record.matnr.isEmpty()) {
            return;
        }
        putScanKey(result, record.matnr, record);
        String matNz = UIFuncs.removeLeadingZeros(record.matnr);
        if (matNz != null && !matNz.isEmpty()) {
            putScanKey(result, matNz, record);
        }
        if (record.ean11 != null && !record.ean11.isEmpty()) {
            putScanKey(result, record.ean11, record);
            String eanNz = UIFuncs.removeLeadingZeros(record.ean11);
            if (eanNz != null && !eanNz.isEmpty()) {
                putScanKey(result, eanNz, record);
            }
        }
    }

    private static void putScanKey(FragmentStoreGrtProcess.PicklistDataParseResult result,
                                   String key,
                                   FragmentStoreGrtProcess.EanRecord record) {
        if (key == null || key.isEmpty()) {
            return;
        }
        result.eanByScanCode.put(key.trim().toUpperCase(Locale.ROOT), record);
    }

    private static File cacheFile(Context context, String plant, String picklist) {
        if (context == null) {
            return null;
        }
        File dir = new File(context.getCacheDir(), DIR);
        return new File(dir, safeName(plant) + "_" + safeName(picklist) + ".bin");
    }

    private static String safeName(String value) {
        if (value == null || value.isEmpty()) {
            return "na";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void writeUtf(DataOutputStream out, String value) throws IOException {
        out.writeUTF(value != null ? value : "");
    }

    private static String readUtf(DataInputStream in) throws IOException {
        String value = in.readUTF();
        return value != null ? value : "";
    }

    private static void closeQuietly(DataInputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(DataOutputStream out) {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException ignored) {
        }
    }
}
