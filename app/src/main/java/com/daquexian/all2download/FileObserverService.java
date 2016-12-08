package com.daquexian.all2download;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by daquexian on 16-12-6.
 * Service which monitors directories
 */

public class FileObserverService extends Service {
    private final FileObserverBinder binder = new FileObserverBinder();
    private WebServer server;
    private List<FileObserver> fileObservers = new ArrayList<>();
    private final static String TAG = "FileObserverS";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class FileObserverBinder extends Binder {
        FileObserverService getService() {
            return FileObserverService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (server == null) server = new WebServer();
        if (fileObservers.size() == 0) {
            for (int i = 0; i < Constants.DIRECTORIES.length; i++) {
                String dir = Constants.DIRECTORIES[i];
                if (BuildConfig.DEBUG && !new File(dir).exists()) LogUtil.d(TAG, "onStartCommand: " + dir + "doesn't exist");
                else LogUtil.d(TAG, "onStartCommand: " + dir + " exists");
                fileObservers.add(new FileObserverSavingPath(dir, FileObserver.CREATE | FileObserver.MOVED_TO) {
                    @Override
                    public void onEvent(final int i, final String s) {
                        if (s == null || s.equals("")) return;
                        Pattern tempFilePattern = Pattern.compile("(\\d{7}$)|(0{9}_9{6}\\.doc)");
                        Matcher tempFileMatcher = tempFilePattern.matcher(s);
                        if (!tempFileMatcher.find()) {
                            try {
                                if (!server.wasStarted()) {
                                    server.start();
                                }

                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://127.0.0.1:8080" + this.path + s));
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, s);
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
                                request.allowScanningByMediaScanner();// if you want to be available from media players
                                LogUtil.d(TAG, "onEvent: begin to download " + s);
                                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                manager.enqueue(request);
                            } catch (IOException e) {
                                Log.d(TAG, "onEvent: server failed to start!");
                            }
                        }
                    }
                });
                fileObservers.get(i).startWatching();
            }
        }
        return START_STICKY;
    }

    public void setEnabled(int i, boolean enabled) {
        if (enabled) fileObservers.get(i).startWatching();
        else fileObservers.get(i).stopWatching();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        server.stop();
        for (FileObserver fileObserver : fileObservers) fileObserver.stopWatching();
    }

    private abstract class FileObserverSavingPath extends FileObserver {
        String path;
        @SuppressWarnings("unused")
        FileObserverSavingPath(String path) {
            super(path);
            this.path = path;
        }
        FileObserverSavingPath(String path, int mask) {
            super(path, mask);
            this.path = path;
        }
    }

    private class WebServer extends NanoHTTPD {
        WebServer() {
            super(8080);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String[] prefix = {".doc", ".dot", ".docx", ".dotx", ".docm", ".dotm", ".xls", ".xlt", ".xla", ".xlsx",
                    ".xltx", ".xlsm", ".xltm", ".xlam", ".xlsb", ".ppt", ".pot", ".pps", ".ppa", ".pptx", ".potx", ".ppsx",
                    ".ppam", ".pptm", ".potm", ".ppsm", ".jpg", ".jpeg", ".png", ".gif", ".js", ".pdf"};
            String[] mimeType = {"application/msword", "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                    "application/vnd.ms-word.document.macroEnabled.12", "application/vnd.ms-word.document.macroEnabled.12",
                    "application/vnd.ms-excel", "application/vnd.ms-excel", "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                    "application/vnd.ms-excel.sheet.macroEnabled.12", "application/vnd.ms-excel.template.macroEnabled.12",
                    "application/vnd.ms-excel.addin.macroEnabled.12", "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
                    "application/vnd.ms-powerpoint", "application/vnd.ms-powerpoint", "application/vnd.ms-powerpoint",
                    "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.openxmlformats-officedocument.presentationml.template",
                    "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                    "application/vnd.ms-powerpoint.addin.macroEnabled.12",
                    "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
                    "application/vnd.ms-powerpoint.template.macroEnabled.12",
                    "application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
                    "image/jpeg", "image/jpeg", "image/png", "image/gif", "application/x-javascript", "application/pdf"};
            FileInputStream fileInputStream;
            String path = session.getUri();
            LogUtil.d(TAG, "serve: " + path);

            try {
                fileInputStream = new FileInputStream(path);
            } catch (IOException ioe) {
                Log.w("Httpd", ioe.toString());
                return null;
            }

            String type = NanoHTTPD.MIME_PLAINTEXT;
            for (int i = 0; i < prefix.length; i++) {
                String p = prefix[i];
                if (path.endsWith(p)) {
                    type = mimeType[i];
                    break;
                }
            }
            return newChunkedResponse(Response.Status.OK, type, fileInputStream);
        }
    }
}
