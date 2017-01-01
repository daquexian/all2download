package com.daquexian.all2download;

import android.app.DownloadManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

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
            FileInputStream fileInputStream;
            String path = session.getUri();
            /**
             * {@link path} is exactly the URI of the file in storage
             */
            LogUtil.d(TAG, "serve: " + path);

            try {
                fileInputStream = new FileInputStream(path);
            } catch (IOException ioe) {
                return null;
            }

            String type = getMimeType(Uri.fromFile(new File(path)));

            return newChunkedResponse(Response.Status.OK, type, fileInputStream);
        }

        private String getMimeType(Uri uri) {
            String mimeType;
            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver cr = MyApplication.getContext().getContentResolver();
                mimeType = cr.getType(uri);
            } else {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                        .toString());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        fileExtension.toLowerCase());
            }
            if (mimeType == null) {
                mimeType = NanoHTTPD.MIME_PLAINTEXT;
            }
            return mimeType;
        }
    }
}
