package com.funzio.pure2D.sounds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

public class SoundManager implements SoundPool.OnLoadCompleteListener, OnPreparedListener {
    protected static final String TAG = SoundManager.class.getSimpleName();

    protected static final float DEFAULT_MEDIA_VOLUME = 0.8f;

    protected volatile SparseArray<Soundable> mSoundMap;

    protected final SoundPool mSoundPool;
    protected volatile boolean mSoundEnabled = true;

    protected final Context mContext;
    protected final AudioManager mAudioManager;

    protected MediaPlayer mMediaPlayer;
    protected float mMediaVolume = DEFAULT_MEDIA_VOLUME;

    protected SoundManager(final Context context, final int maxStream) {
        mContext = context;
        mSoundMap = new SparseArray<Soundable>();

        mSoundPool = new SoundPool(maxStream, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(this);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean isSoundEnabled() {
        return mSoundEnabled;
    }

    public void setSoundEnabled(final boolean enabled) {
        mSoundEnabled = enabled;
    }

    @SuppressLint("NewApi")
    public void load(final Soundable... sounds) {
        final AsyncLoader loader = new AsyncLoader();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sounds);
        } else {
            loader.execute(sounds);
        }
    }

    private class AsyncLoader extends AsyncTask<Soundable, Void, Void> {
        @Override
        protected Void doInBackground(final Soundable... params) {
            for (final Soundable sound : params) {
                final int soundID = sound.load(mSoundPool);

                // check and add to the map
                if (soundID > 0) {
                    synchronized (mSoundMap) {
                        mSoundMap.put(sound.getKey(), sound);
                    }
                }
            }

            return null;
        }
    }

    /**
     * @param key
     * @return a non-zero as the Stream ID if success
     */
    public int play(final int key) {
        // Log.v(TAG, "play(" + key + ")");

        try {
            return play(mSoundMap.get(key));
        } catch (Exception e) {
            Log.e(TAG, "Unable to play sound:", e);
            return -1;
        }
    }

    /**
     * @param sound
     * @return a non-zero as the Stream ID if success
     */
    public int play(final Soundable sound) {
        // Log.v(TAG, "play(" + sound + ")");

        if (mSoundEnabled && sound != null && sound.getSoundID() > 0) {
            final float volume = (float) mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            return mSoundPool.play(sound.getSoundID(), volume, volume, 1, sound.getLoop(), 1f);
        }

        return 0;
    }

    public void play(final Media media) throws IllegalStateException {
        // if (!mSoundEnabled && !forcePlay) {
        // return;
        // }

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        mMediaPlayer.reset(); // reset the mediaplayer state - IDLE

        // load - Transitions to the INITIALIZED State
        if (media.load(mMediaPlayer, mContext) == 0) { // NOTE: Must be called before setting audio related stuff!
            return;
        }

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); // set type
        mMediaPlayer.setLooping(media.isLooping());
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setVolume(mMediaVolume, mMediaVolume);

        mMediaPlayer.prepareAsync();
    }

    public void setMediaVolume(final float volume) {
        mMediaVolume = volume;
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(mMediaVolume, mMediaVolume);
        }
    }

    public void stopMedia() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    public boolean isMediaPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    public void releaseMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    protected int playByID(final int soundID) {
        // Log.v(TAG, "playByID(" + soundID + ")");

        if (mSoundEnabled && soundID > 0) {
            final float volume = (float) mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            return mSoundPool.play(soundID, volume, volume, 1, 0, 1f);
        }

        return 0;
    }

    public void stop(final int streamID) {
        mSoundPool.stop(streamID);
    }

    public boolean unload(final int soundID) {
        return mSoundPool.unload(soundID);
    }

    public boolean unload(final Soundable sound) {
        return sound == null ? false : mSoundPool.unload(sound.getSoundID());
    }

    public Soundable getSound(final int key) {
        return mSoundMap.get(key);
    }

    public Context getContext() {
        return mContext;
    }

    public void dispose() {
        synchronized (mSoundMap) {
            mSoundMap.clear();
        }

        mSoundPool.release();

        releaseMedia();
    }

    public void onLoadComplete(final SoundPool soundPool, final int sampleId, final int status) {
        Log.v(TAG, "onLoadComplete(" + sampleId + ", " + status + ")");
    }

    /*
     * (non-Javadoc)
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
     */
    @Override
    public void onPrepared(final MediaPlayer mp) {
        mp.start();
    }

}
