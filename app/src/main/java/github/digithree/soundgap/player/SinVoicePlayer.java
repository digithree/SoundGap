/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 *
 * Modified on 10th Dec 2016 by digithree
 */

//https://github.com/dingjikerbo/SinVoice/blob/master/app/src/main/java/com/libra/sinvoice/SinVoicePlayer.java
//package com.libra.sinvoice;

package github.digithree.soundgap.player;

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.text.TextUtils;
import android.util.Log;


public class SinVoicePlayer implements Encoder.Listener, Encoder.Callback, PcmPlayer.Listener, PcmPlayer.Callback {
    private final static String TAG = "SinVoicePlayer";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;

    private final static int DEFAULT_GEN_DURATION = 200;

    private List<Integer> mCodes = new ArrayList<Integer>();

    private Encoder mEncoder;
    private PcmPlayer mPlayer;
    private Buffer mBuffer;

    private int mState;
    private Listener mListener;
    private Thread mPlayThread;
    private Thread mEncodeThread;

    public static interface Listener {
        void onPlayStart();

        void onPlayEnd();
    }

    public SinVoicePlayer() {
        this(Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE, Common.DEFAULT_BUFFER_COUNT);
    }

    public SinVoicePlayer(int sampleRate, int bufferSize, int buffCount) {
        mState = STATE_STOP;
        mBuffer = new Buffer(buffCount, bufferSize);

        mEncoder = new Encoder(this, sampleRate, SinGenerator.BITS_16, bufferSize);
        mEncoder.setListener(this);
        mPlayer = new PcmPlayer(this, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mPlayer.setListener(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private boolean convertTextToCodes(String text) {
        if (!TextUtils.isEmpty(text)) {
            mCodes.clear();
            mCodes.add(CodeBook.getInstance().getStartTokenIdx());
            int lastCode = -1;
            for (int i = 0; i < text.length() ; i++) {
                int code = text.charAt(i) - '0';
                if (code >= 0 && code <= 9) {
                    if (code == lastCode) {
                        mCodes.add(CodeBook.getInstance().getStartTokenIdx());
                    }
                    mCodes.add(code + 1); //add offset of 1 as start token is idx 0
                    lastCode = code;
                } else {
                    // invalid code
                    return false;
                }
            }
        }
        mCodes.add(CodeBook.getInstance().getEndTokenIdx());
        return true;
    }

    public void play(final String text) {
        play(text, false, 0);
    }

    public void play(final String text, final boolean repeat, final int muteInterval) {
        if (STATE_STOP == mState && convertTextToCodes(text)) {
            mState = STATE_PENDING;

            mPlayThread = new Thread() {
                @Override
                public void run() {
                    mPlayer.start();
                }
            };
            if (null != mPlayThread) {
                mPlayThread.start();
            }

            mEncodeThread = new Thread() {
                @Override
                public void run() {
                    do {
                        Log.d(TAG, "encode start");
                        mEncoder.encode(mCodes, DEFAULT_GEN_DURATION, muteInterval);
                        Log.d(TAG, "encode end");

                        mEncoder.stop();
                    } while (repeat && STATE_PENDING != mState);
                    stopPlayer();
                }
            };
            if (null != mEncodeThread) {
                mEncodeThread.start();
            }

            Log.d(TAG, "play");
            mState = STATE_START;
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_PENDING;

            Log.d(TAG, "force stop start");
            mEncoder.stop();
            if (null != mEncodeThread) {
                try {
                    mEncodeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mEncodeThread = null;
                }
            }

            Log.d(TAG, "force stop end");
        }
    }

    private void stopPlayer() {
        if (mEncoder.isStoped()) {
            mPlayer.stop();
        }

        // put end buffer
        mBuffer.putFull(Buffer.BufferData.getEmptyBuffer());

        if (null != mPlayThread) {
            try {
                mPlayThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mPlayThread = null;
            }
        }

        mBuffer.reset();
        mState = STATE_STOP;
    }

    @Override
    public void onStartEncode() {
        Log.d(TAG, "onStartGen");
    }

    @Override
    public void freeEncodeBuffer(Buffer.BufferData buffer) {
        if (null != buffer) {
            mBuffer.putFull(buffer);
        }
    }

    @Override
    public Buffer.BufferData getEncodeBuffer() {
        return mBuffer.getEmpty();
    }

    @Override
    public void onEndEncode() {
    }

    @Override
    public Buffer.BufferData getPlayBuffer() {
        return mBuffer.getFull();
    }

    @Override
    public void freePlayData(Buffer.BufferData data) {
        mBuffer.putEmpty(data);
    }

    @Override
    public void onPlayStart() {
        if (null != mListener) {
            mListener.onPlayStart();
        }
    }

    @Override
    public void onPlayStop() {
        if (null != mListener) {
            mListener.onPlayEnd();
        }
    }

}
