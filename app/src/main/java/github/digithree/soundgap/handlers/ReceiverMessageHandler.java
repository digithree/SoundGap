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

public class ReceiverMessageHandler implements AudioProcessingThread.Callback {

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

    private boolean mActive = false;


    public ReceiverMessageHandler(@NonNull Callback callback) {
        mCallback = callback;
        midiNoteArrayList = new ArrayList<>();
    }

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

    // internal process

    private void processMidiNotes() {
        // TODO : look for messages in midiNoteArrayList
        // TODO : trigger mCallback.heardMessage(message) if find one
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
        FrequencyProcessingHelper.MidiNote midiNote = FrequencyProcessingHelper.getMidinote(maxAmpFreq);
        String peakInfoStr = buildPeakInfoString(maxAmpFreq, maxAmpDB, midiNote);
        Log.v("ReceiverMessageHandler", peakInfoStr);
        if (maxAmpDB >= MIN_TRIGGER_DB) {
            if (midiNote == null || mMidiNote == null
                    || !mMidiNote.getStr().equals(midiNote.getStr())) {
                midiNoteArrayList.add(midiNote);
            }
        }
        mMidiNote = midiNote;
        processMidiNotes();
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
