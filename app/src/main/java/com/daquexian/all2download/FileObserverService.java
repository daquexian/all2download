package com.daquexian.all2download;

import android.app.DownloadManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by daquexian on 16-12-6.
 * Service which monitors directories
 */

public class FileObserverService extends Service {
    private final FileObserverBinder binder = new FileObserverBinder();
    private List<FileObserver> fileObservers = new ArrayList<>();
    private final static String TAG = "FileObserverS";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressWarnings("WeakerAccess")
    public class FileObserverBinder extends Binder {
        FileObserverService getService() {
            return FileObserverService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (fileObservers.size() == 0) {
            for (int i = 0; i < Constants.DIRECTORIES.length; i++) {
                String dir = Constants.DIRECTORIES[i];
                if (BuildConfig.DEBUG && !new File(dir).exists()) LogUtil.d(TAG, "onStartCommand: " + dir + "doesn't exist");
                else LogUtil.d(TAG, "onStartCommand: " + dir + " exists");
                fileObservers.add(new improvedFileObserver(dir, FileObserver.CREATE | FileObserver.MOVED_TO) {
                    @Override
                    public void onEvent(final int i, final String filename) {
                        if (filename == null || filename.equals("")) return;
                        Pattern tempFilePattern = Pattern.compile("(\\d{7}$)|(0{9}_9{6}\\.doc)");
                        Matcher tempFileMatcher = tempFilePattern.matcher(filename);
                        if (!tempFileMatcher.find()) {
                            File newFile = getFileUnderDir(filename);
                            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                            manager.addCompletedDownload(filename, "attached by All2Download", true, getMimeType(newFile), newFile.getAbsolutePath(), newFile.length(), true);
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
        for (FileObserver fileObserver : fileObservers) fileObserver.stopWatching();
    }

    @SuppressWarnings({ "WeakerAccess", "unused" })
    private abstract class improvedFileObserver extends FileObserver {
        private String mPath;
        private File mDirFile;
        improvedFileObserver(String path) {
            super(path);
            this.mPath = path;
            this.mDirFile = new File(path);
        }
        improvedFileObserver(String path, int mask) {
            super(path, mask);
            this.mPath = path;
            this.mDirFile = new File(path);
        }

        public String getDirPath() {
            return mPath;
        }

        public File getDirFile() {
            return mDirFile;
        }

        public File getFileUnderDir(String filename) {
            return new File(mDirFile, filename);
        }
    }

    @SuppressWarnings("unused")
    private String getMimeType(String path) {
        return getMimeType(Uri.fromFile(new File(path)));
    }

    private String getMimeType(File file) {
        return getMimeType(Uri.fromFile(file));
    }

    private String getMimeType(Uri uri) {
        String MIME_PLAINTEXT = "text/plain";
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
            mimeType = MIME_PLAINTEXT;
        }
        return mimeType;
    }
}
