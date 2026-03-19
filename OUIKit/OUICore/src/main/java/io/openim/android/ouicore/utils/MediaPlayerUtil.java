package io.openim.android.ouicore.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

public final class MediaPlayerUtil {
    public static final MediaPlayerUtil INSTANCE = new MediaPlayerUtil();

    private MediaPlayer mPlayer;
    private boolean isPause = false;
    private boolean isPlaying = false;

    private MediaPlayerUtil() {
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void initMedia(Context context, int rawRes) {
        mPlayer = MediaPlayer.create(context, rawRes);
        config();
    }

    @RequiresApi(Build.VERSION_CODES.N)
    public void initMedia(Context context, AssetFileDescriptor fad) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fad);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config();
    }

    public void initMedia(Context context, String path) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config();
    }

    public void initUriMedia(Context context, String uriPath) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(context, Uri.parse(uriPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config();
    }

    private void config() {
    }

    public void loopPlay() {
        setMediaListener(new MediaPlayerListener() {
            @Override
            public void finish() {
                playMedia();
            }

            @Override
            public void onErr(int what) {
            }

            @Override
            public void prepare() {
                playMedia();
            }
        });
        playMedia();
    }

    public void setMediaListener(final MediaPlayerListener listener) {
        if (mPlayer == null) {
            return;
        }

        mPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            listener.finish();
        });

        mPlayer.setOnErrorListener((mp, what, extra) -> {
            isPlaying = false;
            listener.onErr(what);
            return false;
        });

        mPlayer.setOnPreparedListener(mp -> listener.prepare());
    }

    public void playMedia() {
        if (mPlayer != null && !mPlayer.isPlaying()) {
            mPlayer.start();
            isPlaying = true;
        }
    }

    public void prepare() {
        if (mPlayer != null && !mPlayer.isPlaying()) {
            try {
                mPlayer.prepare();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void prepareAsync() {
        if (mPlayer != null && !mPlayer.isPlaying()) {
            mPlayer.prepareAsync();
        }
    }

    public void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
            isPause = true;
        }
    }

    public void resume() {
        if (mPlayer != null && isPause) {
            mPlayer.start();
            isPlaying = true;
            isPause = false;
        }
    }

    public void release() {
        if (mPlayer != null) {
            try {
                mPlayer.release();
            } catch (Exception ignored) {
            }
            mPlayer = null;
        }
        isPlaying = false;
    }
}

interface MediaPlayerListener {
    void finish();

    void onErr(int what);

    void prepare();
}
