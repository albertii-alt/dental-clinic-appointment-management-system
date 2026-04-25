package com.dentalclinic.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class UpdateChecker {

    private static final String RELEASES_API =
        "https://api.github.com/repos/albertii-alt/dental-clinic-appointment-management-system/releases/latest";

    public static class UpdateInfo {
        public final String currentVersion;
        public final String latestVersion;
        public final String releaseNotes;
        public final String downloadUrl;
        public final boolean updateAvailable;

        public UpdateInfo(String currentVersion, String latestVersion, String releaseNotes, String downloadUrl) {
            this.currentVersion  = currentVersion;
            this.latestVersion   = latestVersion;
            this.releaseNotes    = releaseNotes;
            this.downloadUrl     = downloadUrl;
            this.updateAvailable = isNewer(latestVersion, currentVersion);
        }
    }

    public static String getCurrentVersion() {
        try (InputStream is = UpdateChecker.class.getResourceAsStream("/com/dentalclinic/resources/version.properties")) {
            if (is == null) return "0.0.0";
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("app.version", "0.0.0").trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    public static UpdateInfo checkForUpdate() {
        String current = getCurrentVersion();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(RELEASES_API).openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            String json = readResponse(conn);
            String tag          = extractJson(json, "tag_name");
            String body         = extractJson(json, "body");
            String downloadUrl  = extractDownloadUrl(json);

            String latest = tag != null ? tag.replaceAll("[^0-9.]", "") : null;
            if (latest == null || latest.isEmpty()) return null;

            return new UpdateInfo(current, latest, body != null ? body : "", downloadUrl != null ? downloadUrl : "");
        } catch (Exception e) {
            return null;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) return null;
        int end = start + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return json.substring(start + 1, end)
                   .replace("\\n", "\n")
                   .replace("\\r", "")
                   .replace("\\\"", "\"");
    }

    private static String extractDownloadUrl(String json) {
        // Find the .exe asset browser_download_url
        int idx = json.indexOf(".exe\"");
        if (idx < 0) return null;
        int urlEnd = idx + 4;
        int urlStart = json.lastIndexOf("\"", idx - 1);
        if (urlStart < 0) return null;
        return json.substring(urlStart + 1, urlEnd);
    }

    private static boolean isNewer(String latest, String current) {
        try {
            int[] l = parseParts(latest);
            int[] c = parseParts(current);
            for (int i = 0; i < 3; i++) {
                if (l[i] > c[i]) return true;
                if (l[i] < c[i]) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static int[] parseParts(String version) {
        String[] parts = version.split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            try { result[i] = Integer.parseInt(parts[i].trim()); } catch (Exception ignored) {}
        }
        return result;
    }
}
