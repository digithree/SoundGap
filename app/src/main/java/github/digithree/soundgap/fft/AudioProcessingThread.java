/* Copyright 2011 Google Inc.
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 *
 * @author Stephen Uhler
 *
 * 2014 Eddy Xiao <bewantbe@gmail.com>
 * GUI extensively modified.
 * Add some naive auto refresh rate control logic.
 *
 * 2016 digithree <digithree@github.com>
 * Original code modified from AnalyzeActivity
 */

//https://github.com/bewantbe/audio-analyzer-for-android/blob/master/audioSpectrumAnalyzer/src/main/java/github/bewantbe/audio_analyzer_for_android/AnalyzeActivity.java
//package github.bewantbe.audio_analyzer_for_android;

package github.digithree.soundgap.fft;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

import github.digithree.soundgap.App;

/**
 * Created by digithree on 10/12/2016.
 */

public class AudioProcessingThread extends Thread {

    public interface Callback {
        void onInitParams(float sampleRate, int fftLen, double timeDurationPref, int nFFTAverage);
        void updateRawData(final double[] data);
        void updatePeakData(double maxAmpFreq, double maxAmpDB);
        void updateRec(double secs);
    }

    private static final String TAG = AudioProcessingThread.class.getSimpleName();

    private static final double TEST_SIGNAL_1_FREQ = 440.0;
    private static final double TEST_SIGNAL_1_DB_1 = -6.0;
    private static final double TEST_SIGNAL_2_FREQ_1 = 625.0;
    private static final double TEST_SIGNAL_2_FREQ_2 = 1875.0;
    private static final double TEST_SIGNAL_2_DB_1 = -6.0;
    private static final double TEST_SIGNAL_2_DB_2 = -12.0;

    private final static double SAMPLE_VALUE_MAX = 32767.0;   // Maximum signal value
    private final static int RECORDER_AGC_OFF = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private final static int BYTE_OF_SAMPLE = 2;

    private static int fftLen = 1024;
    private static int sampleRate = 16000;
    private static int nFFTAverage = 2;

    private boolean isMeasure = true;
    private boolean isAWeighting = false;
    private boolean bWarnOverrun = true;

    private double timeDurationPref = 4.0;
    private double wavSec, wavSecRemain;
    double dtRMS = 0;
    double dtRMSFromFT = 0;
    double maxAmpDB;
    double maxAmpFreq;

    AudioRecord record;
    volatile boolean isRunning = true;
    volatile boolean isPaused1 = false;
    double wavSecOld = 0;      // used to reduce frame rate
    public STFT stft;   // use with care

    DoubleSineGen sineGen1;
    DoubleSineGen sineGen2;
    double[] mdata;

    double[] spectrumDBcopy;   // XXX, transfers data from Looper to AnalyzeView

    // parameters
    int audioSourceId;
    private String windowFunctionName;
    private boolean saveToWavEnabled;

    private Callback mCallback;


    public AudioProcessingThread(Callback callback) {
        this(RECORDER_AGC_OFF, "Hanning", callback);
    }

    public AudioProcessingThread(int audioSourceId, String windowFunctionName, Callback callback) {
        this.audioSourceId = audioSourceId;
        this.windowFunctionName = windowFunctionName;
        this.mCallback = callback;
        saveToWavEnabled = false;

        //isPaused1 = ((SelectorText) findViewById(R.id.run)).getText().toString().equals("stop");
        // Signal sources for testing
        double fq0 = TEST_SIGNAL_1_FREQ;
        double amp0 = Math.pow(10, 1/20.0 * TEST_SIGNAL_1_DB_1);
        double fq1 = TEST_SIGNAL_2_FREQ_1;
        double fq2 = TEST_SIGNAL_2_FREQ_2;
        double amp1 = Math.pow(10, 1/20.0 * TEST_SIGNAL_2_DB_1);
        double amp2 = Math.pow(10, 1/20.0 * TEST_SIGNAL_2_DB_2);
        if (audioSourceId == 1000) {
            sineGen1 = new DoubleSineGen(fq0, sampleRate, SAMPLE_VALUE_MAX * amp0);
        } else {
            sineGen1 = new DoubleSineGen(fq1, sampleRate, SAMPLE_VALUE_MAX * amp1);
        }
        sineGen2 = new DoubleSineGen(fq2, sampleRate, SAMPLE_VALUE_MAX * amp2);
    }

    public void setSaveToWav(boolean enabled) {
        saveToWavEnabled = enabled;
    }

    private void SleepWithoutInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double baseTimeMs = SystemClock.uptimeMillis();

    private void LimitFrameRate(double updateMs) {
        // Limit the frame rate by wait `delay' ms.
        baseTimeMs += updateMs;
        long delay = (int) (baseTimeMs - SystemClock.uptimeMillis());
//      Log.i(TAG, "delay = " + delay);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Log.i(TAG, "Sleep interrupted");  // seems never reached
            }
        } else {
            baseTimeMs -= delay;  // get current time
            // Log.i(TAG, "time: cmp t="+Long.toString(SystemClock.uptimeMillis())
            //            + " v.s. t'=" + Long.toString(baseTimeMs));
        }
    }

    // generate test data
    private int readTestData(short[] a, int offsetInShorts, int sizeInShorts, int id) {
        if (mdata == null || mdata.length != sizeInShorts) {
            mdata = new double[sizeInShorts];
        }
        Arrays.fill(mdata, 0.0);
        switch (id - 1000) {
            case 1:
                sineGen2.getSamples(mdata);
            case 0:
                sineGen1.addSamples(mdata);
                for (int i = 0; i < sizeInShorts; i++) {
                    a[offsetInShorts + i] = (short) Math.round(mdata[i]);
                }
                break;
            case 2:
                for (int i = 0; i < sizeInShorts; i++) {
                    a[i] = (short) (SAMPLE_VALUE_MAX * (2.0*Math.random() - 1));
                }
                break;
            default:
                Log.w(TAG, "readTestData(): No this source id = " + audioSourceId);
        }
        LimitFrameRate(1000.0*sizeInShorts / sampleRate);
        return sizeInShorts;
    }

    @Override
    public void run() {
        if (mCallback != null) {
            mCallback.onInitParams(sampleRate, fftLen, timeDurationPref, nFFTAverage);
        }

        // Wait until previous instance of AudioRecord fully released.
        SleepWithoutInterrupt(500);

        int minBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Looper::run(): Invalid AudioRecord parameter.\n");
            return;
        }

        /**
         * Develop -> Reference -> AudioRecord
         *    Data should be read from the audio hardware in chunks of sizes
         *    inferior to the total recording buffer size.
         */
        // Determine size of buffers for AudioRecord and AudioRecord::read()
        int readChunkSize    = fftLen/2;  // /2 due to overlapped analyze window
        readChunkSize        = Math.min(readChunkSize, 2048);  // read in a smaller chunk, hopefully smaller delay
        int bufferSampleSize = Math.max(minBytes / BYTE_OF_SAMPLE, fftLen/2) * 2;
        // tolerate up to about 1 sec.
        bufferSampleSize = (int)Math.ceil(1.0 * sampleRate / bufferSampleSize) * bufferSampleSize;

        // Use the mic with AGC turned off. e.g. VOICE_RECOGNITION
        // The buffer size here seems not relate to the delay.
        // So choose a larger size (~1sec) so that overrun is unlikely.
        if (audioSourceId < 1000) {
            record = new AudioRecord(audioSourceId, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BYTE_OF_SAMPLE * bufferSampleSize);
        } else {
            record = new AudioRecord(RECORDER_AGC_OFF, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BYTE_OF_SAMPLE * bufferSampleSize);
        }
        Log.i(TAG, "Looper::Run(): Starting recorder... \n" +
                "  source          : " + (audioSourceId<1000?AudioSourceHelper.getAudioSourceNameFromId(audioSourceId):audioSourceId) + "\n" +
                String.format("  sample rate     : %d Hz (request %d Hz)\n", record.getSampleRate(), sampleRate) +
                String.format("  min buffer size : %d samples, %d Bytes\n", minBytes / BYTE_OF_SAMPLE, minBytes) +
                String.format("  buffer size     : %d samples, %d Bytes\n", bufferSampleSize, BYTE_OF_SAMPLE*bufferSampleSize) +
                String.format("  read chunk size : %d samples, %d Bytes\n", readChunkSize, BYTE_OF_SAMPLE*readChunkSize) +
                String.format("  FFT length      : %d\n", fftLen) +
                String.format("  nFFTAverage     : %d\n", nFFTAverage));
        sampleRate = record.getSampleRate();

        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "Looper::run(): Fail to initialize AudioRecord()");
            // If failed somehow, leave user a chance to change preference.
            return;
        }

        short[] audioSamples = new short[readChunkSize];
        int numOfReadShort;

        stft = new STFT(fftLen, sampleRate, windowFunctionName);
        stft.setAWeighting(isAWeighting);
        if (spectrumDBcopy == null || spectrumDBcopy.length != fftLen/2+1) {
            spectrumDBcopy = new double[fftLen/2+1];
        }

        RecorderMonitor recorderMonitor = new RecorderMonitor(sampleRate, bufferSampleSize, "Looper::run()");
        recorderMonitor.start();

//      FramesPerSecondCounter fpsCounter = new FramesPerSecondCounter("Looper::run()");

        WavWriter wavWriter = new WavWriter(sampleRate);
        boolean bSaveWavLoop = saveToWavEnabled;  // change of saveToWavEnabled during loop will only affect next enter.
        if (bSaveWavLoop) {
            wavWriter.start();
            wavSecRemain = wavWriter.secondsLeft();
            wavSec = 0;
            wavSecOld = 0;
            Log.i(TAG, "PCM write to file " + wavWriter.getPath());
        }

        // Start recording
        record.startRecording();

        // Main loop
        // When running in this loop (including when paused), you can not change properties
        // related to recorder: e.g. audioSourceId, sampleRate, bufferSampleSize
        // TODO: allow change of FFT length on the fly.
        while (isRunning) {
            // Read data
            if (audioSourceId >= 1000) {
                numOfReadShort = readTestData(audioSamples, 0, readChunkSize, audioSourceId);
            } else {
                numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // pulling
            }
            if ( recorderMonitor.updateState(numOfReadShort) ) {  // performed a check
                if (recorderMonitor.getLastCheckOverrun())
                    notifyOverrun();
                if (bSaveWavLoop)
                    wavSecRemain = wavWriter.secondsLeft();
            }
            if (bSaveWavLoop) {
                wavWriter.pushAudioShort(audioSamples, numOfReadShort);  // Maybe move this to another thread?
                wavSec = wavWriter.secondsWritten();
                updateRec();
            }
            if (isPaused1) {
//          fpsCounter.inc();
                // keep reading data, for overrun checker and for write wav data
                continue;
            }

            stft.feedData(audioSamples, numOfReadShort);

            // If there is new spectrum data, do plot
            if (stft.nElemSpectrumAmp() >= nFFTAverage) {
                // Update spectrum or spectrogram
                final double[] spectrumDB = stft.getSpectrumAmpDB();
                System.arraycopy(spectrumDB, 0, spectrumDBcopy, 0, spectrumDB.length);
                update(spectrumDBcopy);
//          fpsCounter.inc();

                stft.calculatePeak();
                maxAmpFreq = stft.maxAmpFreq;
                maxAmpDB = stft.maxAmpDB;

                // get RMS
                dtRMS = stft.getRMS();
                dtRMSFromFT = stft.getRMSFromFT();
            }
        }
        Log.i(TAG, "Looper::Run(): Actual sample rate: " + recorderMonitor.getSampleRate());
        Log.i(TAG, "Looper::Run(): Stopping and releasing recorder.");
        record.stop();
        record.release();
        record = null;
        if (bSaveWavLoop) {
            Log.i(TAG, "Looper::Run(): Ending saved wav.");
            wavWriter.stop();
            notifyWAVSaved(wavWriter.relativeDir);
        }
    }

    long lastTimeNotifyOverrun = 0;
    private void notifyOverrun() {
        if (!bWarnOverrun) {
            return;
        }
        long t = SystemClock.uptimeMillis();
        if (t - lastTimeNotifyOverrun > 6000) {
            lastTimeNotifyOverrun = t;
            App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    String text = "Recorder buffer overrun!\nYour cell phone is too slow.\nTry lower sampling rate or higher average number.";
                    Toast toast = Toast.makeText(App.getStaticInstance(), text, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

    private void notifyWAVSaved(final String path) {
        App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                String text = "WAV saved to " + path;
                Toast toast = Toast.makeText(App.getStaticInstance(), text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void update(final double[] data) {
        if (mCallback != null) {
            mCallback.updateRawData(spectrumDBcopy);
            mCallback.updatePeakData(maxAmpFreq, maxAmpDB);
        }
        /*
        if (graphView.getShowMode() == 1) {
            // data is synchronized here
            graphView.saveRowSpectrumAsColor(spectrumDBcopy);
        }
        AnalyzeActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (graphView.getShowMode() == 0) {
                    graphView.saveSpectrum(spectrumDBcopy);
                }
                // data will get out of synchronize here
                AnalyzeActivity.this.invalidateGraphView();
            }
        });
        */
    }


    private void updateRec() {
        if (wavSec - wavSecOld < 0.1) {
            return;
        }
        wavSecOld = wavSec;
        if (mCallback != null) {
            mCallback.updateRec(wavSec - wavSecOld);
        }
        /*
        AnalyzeActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // data will get out of synchronize here
                AnalyzeActivity.this.invalidateGraphView(VIEW_MASK_RecTimeLable);
            }
        });
        */
    }

    /*
    private void setupView(float sampleRate, int fftLen, int timeDurationPref, int nFFTAverage) {
        // Maybe move these out of this class
        RectF bounds = graphView.getBounds();
        bounds.right = sampleRate / 2;
        graphView.setBounds(bounds);
        graphView.setupSpectrogram(sampleRate, fftLen, timeDurationPref);
        graphView.setTimeMultiplier(nFFTAverage);
    }
    */

    public void setPause(boolean pause) {
        this.isPaused1 = pause;
    }

    public boolean getPause() {
        return this.isPaused1;
    }

    public void finish() {
        isRunning = false;
        interrupt();
    }
}