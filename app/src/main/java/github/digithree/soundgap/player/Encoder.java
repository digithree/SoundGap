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

//https://github.com/dingjikerbo/SinVoice/blob/master/app/src/main/java/com/libra/sinvoice/Encoder.java
//package com.libra.sinvoice;

package github.digithree.soundgap.player;

import android.util.Log;

import java.util.List;



public class Encoder implements SinGenerator.Listener, SinGenerator.Callback {
    private final static String TAG = "Encoder";
    private final static int STATE_ENCODING = 1;
    private final static int STATE_STOPED = 2;

    private int mState;

    private SinGenerator mSinGenerator;
    private Listener mListener;
    private Callback mCallback;

    public static interface Listener {
        void onStartEncode();

        void onEndEncode();
    }

    public static interface Callback {
        void freeEncodeBuffer(Buffer.BufferData buffer);

        Buffer.BufferData getEncodeBuffer();
    }

    public Encoder(Callback callback, int sampleRate, int bits, int bufferSize) {
        mCallback = callback;
        mState = STATE_STOPED;
        mSinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
        mSinGenerator.setListener(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public final static int getMaxCodeCount() {
        return CodeBook.getInstance().getCodeFrequencyTable().length;
    }

    public final boolean isStoped() {
        return (STATE_STOPED == mState);
    }

    // content of input from 0 to (CODE_FREQUENCY.length-1)
    public void encode(List<Integer> codes, int duration) {
        encode(codes, duration, 0);
    }

    public void encode(List<Integer> codes, int duration, int muteInterval) {
        if (STATE_STOPED == mState) {
            mState = STATE_ENCODING;

            if (null != mListener) {
                mListener.onStartEncode();
            }

            mSinGenerator.start();
            for (int index : codes) {
                if (STATE_ENCODING == mState) {
                    Log.d(TAG, "encode:" + index);
                    if (index >= 0 && index < CodeBook.getInstance().getCodeFrequencyTable().length) {
                        mSinGenerator.gen(CodeBook.getInstance().getCodeFrequencyTable()[index], duration);
                    } else {
                        Log.e(TAG, "code index error");
                    }
                } else {
                    Log.d(TAG, "encode force stop");
                    break;
                }
            }
            // for mute
            if (STATE_ENCODING == mState) {
                mSinGenerator.gen(0, muteInterval);
            } else {
                Log.d(TAG, "encode force stop");
            }
            stop();

            if (null != mListener) {
                mListener.onEndEncode();
            }
        }
    }

    public void stop() {
        if (STATE_ENCODING == mState) {
            mState = STATE_STOPED;

            mSinGenerator.stop();
        }
    }

    @Override
    public void onStartGen() {
        Log.d(TAG, "start gen codes");
    }

    @Override
    public void onStopGen() {
        Log.d(TAG, "end gen codes");
    }

    @Override
    public Buffer.BufferData getGenBuffer() {
        if (null != mCallback) {
            return mCallback.getEncodeBuffer();
        }
        return null;
    }

    @Override
    public void freeGenBuffer(Buffer.BufferData buffer) {
        if (null != mCallback) {
            mCallback.freeEncodeBuffer(buffer);
        }
    }
}
