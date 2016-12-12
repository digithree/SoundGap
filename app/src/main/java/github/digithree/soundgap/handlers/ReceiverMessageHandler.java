/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import github.digithree.soundgap.App;
import github.digithree.soundgap.fft.AudioProcessingThread;
import github.digithree.soundgap.fft.FrequencyProcessingHelper;
import github.digithree.soundgap.player.CodeBook;

public class ReceiverMessageHandler implements AudioProcessingThread.Callback {
    private static final String TAG = ReceiverMessageHandler.class.getSimpleName();

    public interface Callback {
        void startedListening();
        void stoppedListening();
        void heardMessage(String message);
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


    public ReceiverMessageHandler(@NonNull Callback callback) {
        mCallback = callback;
        midiNoteArrayList = new ArrayList<>();
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
            }
        } else {
            if (midiNote.getStr().equals(CodeBook.getInstance().getCodeNoteNameTable()[CodeBook.getInstance().getEndTokenIdx()])) {
                Log.d(TAG, "processMidiNotes, stopping listening");
                mListening = false;
                stop();
                extractMessageFromHeardNotes();
            } else if (midiNote.getStr().equals(CodeBook.getInstance().getCodeNoteNameTable()[CodeBook.getInstance().getStartTokenIdx()])) {
                //ignore, used as spacer
                Log.d(TAG, "processMidiNotes, heard spacer, ignoring");
            } else {
                Log.d(TAG, "processMidiNotes, added note");
                midiNoteArrayList.add(midiNote);
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
