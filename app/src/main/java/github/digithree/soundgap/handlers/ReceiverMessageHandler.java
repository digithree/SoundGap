/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import github.digithree.soundgap.App;
import github.digithree.soundgap.fft.AudioProcessingThread;
import github.digithree.soundgap.fft.FrequencyProcessingHelper;
import github.digithree.soundgap.player.CodeBook;
import github.digithree.soundgap.utils.ManagementUtil;

public class ReceiverMessageHandler implements AudioProcessingThread.Callback {
    private static final String TAG = ReceiverMessageHandler.class.getSimpleName();

    private static final int TIMER_DELAY = 3000;

    public interface Callback {
        void startedListening();
        void stoppedListening();
        void heardMessage(String message);
        void errorListening();
    }

    private AudioProcessingThread mAudioProcessingThread;
    private Callback mCallback;

    private static final double MIN_TRIGGER_DB = -40;

    private FrequencyProcessingHelper.MidiNote mMidiNote;
    private int mDataUpdateCounter = 0;

    private ArrayList<FrequencyProcessingHelper.MidiNote> midiNoteArrayList;

    private String mLastMessage;

    private boolean mActive = false;
    private boolean mListening = false;

    private Handler mTimerHandler;
    private Runnable mTimerRunnable;
    private int mTimerCode;


    public ReceiverMessageHandler(@NonNull Callback callback) {
        mCallback = callback;
        midiNoteArrayList = new ArrayList<>();
        mTimerHandler = App.getStaticInstance().getHandler();
    }


    // public interface

    public boolean isActive() {
        return mActive;
    }

    public void start() {
        if (mAudioProcessingThread != null) {
            mAudioProcessingThread.finish();
            mAudioProcessingThread = null;
        }
        mAudioProcessingThread = new AudioProcessingThread(this);
        mAudioProcessingThread.start();
        mActive = true;
        if (mCallback != null) {
            mCallback.startedListening();
        }
    }

    public void stop() {
        if (mActive && mAudioProcessingThread != null) {
            mAudioProcessingThread.finish();
            mAudioProcessingThread = null;
            mActive = false;
            if (mCallback != null) {
                mCallback.stoppedListening();
            }
        }
    }

    public String getLastMessage() {
        return mLastMessage;
    }


    // internal process

    private void processMidiNotes(FrequencyProcessingHelper.MidiNote midiNote) {
        Log.d(TAG, "processMidiNotes, got note"+midiNote.toString());
        // TODO : look for messages in midiNoteArrayList
        // TODO : trigger mCallback.heardMessage(message) if find one
        if (!mListening) {
            Log.d(TAG, "processMidiNotes, not listening yet");
            if (midiNote.getStr().equals(CodeBook.getInstance().getCodeNoteNameTable()[CodeBook.getInstance().getStartTokenIdx()])) {
                mListening = true;
                // will capture notes the next round
                Log.d(TAG, "processMidiNotes, got start note");
                midiNoteArrayList.clear();
                restartTimer();
            }
        } else {
            if (midiNote.getStr().equals(CodeBook.getInstance().getCodeNoteNameTable()[CodeBook.getInstance().getEndTokenIdx()])) {
                Log.d(TAG, "processMidiNotes, stopping listening");
                mListening = false;
                stop();
                stopTimer();
                extractMessageFromHeardNotes();
            } else if (midiNote.getStr().equals(CodeBook.getInstance().getCodeNoteNameTable()[CodeBook.getInstance().getStartTokenIdx()])) {
                //ignore, used as spacer
                Log.d(TAG, "processMidiNotes, heard spacer, ignoring");
                restartTimer();
            } else {
                Log.d(TAG, "processMidiNotes, added note");
                midiNoteArrayList.add(midiNote);
                restartTimer();
            }
        }
    }

    private void extractMessageFromHeardNotes() {
        Log.d(TAG, "extractMessageFromHeardNotes");
        StringBuilder stringBuilder = new StringBuilder();
        for (FrequencyProcessingHelper.MidiNote midiNote : midiNoteArrayList) {
            stringBuilder.append(String.format(Locale.getDefault(), "%d", (midiNote.getNote() - 1)));
        }
        String decodedMessage = MessageCodex.decode(stringBuilder.toString());
        if (decodedMessage != null) {
            mLastMessage = decodedMessage;
            if (mCallback != null) {
                mCallback.heardMessage(decodedMessage);
            }
        }
        midiNoteArrayList.clear();
    }

    // timer

    private void stopTimer() {
        if (mTimerHandler != null) {
            if (mTimerRunnable != null) {
                mTimerHandler.removeCallbacks(mTimerRunnable);
                mTimerRunnable = null;
            }
        }
    }

    private void restartTimer() {
        stopTimer();
        final int timerCode = ManagementUtil.generateRandomCode();
        mTimerCode = timerCode;
        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timerCode == mTimerCode) {
                    // no change in minimum time, restart listening process
                    mListening = false;
                    midiNoteArrayList.clear();
                    Log.d(TAG, "TIMER EXPIRED, took too long to understand message, restarting listening");
                    App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null) {
                                mCallback.errorListening();
                            }
                        }
                    });
                } // else do nothing, is detached from mTimerHandler
            }
        };
        mTimerHandler.postDelayed(mTimerRunnable, TIMER_DELAY);
    }


    // AudioProcessingThread.Callback implementation

    @Override
    public void onInitParams(final float sampleRate, final int fftLen,
                             final double timeDurationPref, final int nFFTAverage) {
        App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                Log.v("ReceiverMessageHandler", "Params:" +
                        "\n\tSample rate: "+sampleRate+
                        "\n\tfftLen: "+fftLen+
                        "\n\ttimeDurationPref: "+timeDurationPref+
                        "\n\tnFFTAverage: "+nFFTAverage);
            }
        });
    }

    @Override
    public void updateRawData(double[] data, final double maxAmpFreq, final double maxAmpDB) {
        // TODO : can use data[] to check how much of peak the max peaks are and only use
        // TODO :       if significantly different in amplitude from rest of signal
        if (maxAmpFreq == 0) {
            // discard if freq zero, send on init
            return;
        }
        final FrequencyProcessingHelper.MidiNote midiNote = FrequencyProcessingHelper.getMidinote(maxAmpFreq);
        String peakInfoStr = buildPeakInfoString(maxAmpFreq, maxAmpDB, midiNote);
        //Log.v("ReceiverMessageHandler", peakInfoStr);
        if (maxAmpDB >= MIN_TRIGGER_DB) {
            if (midiNote == null || mMidiNote == null
                    || !mMidiNote.getStr().equals(midiNote.getStr())) {
                App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        processMidiNotes(midiNote);
                    }
                });
            }
        }
        mMidiNote = midiNote;
    }

    @Override
    public void updateRec(double secs) {
        //not used
    }

    private String buildPeakInfoString(double maxAmpFreq, double maxAmpDB,
                                       FrequencyProcessingHelper.MidiNote midiNote) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format(Locale.getDefault(), "Peak: %.2f", maxAmpFreq));
        stringBuilder.append(String.format("Hz(%s)", midiNote != null ? midiNote.getStr() : "NULL"));
        stringBuilder.append(String.format(Locale.getDefault(), " db %.2f", maxAmpDB));

        return stringBuilder.toString();
    }
}
