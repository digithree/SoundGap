/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import android.support.annotation.NonNull;

import java.util.Locale;

import github.digithree.soundgap.App;
import github.digithree.soundgap.fft.AudioProcessingThread;
import github.digithree.soundgap.fft.FrequencyProcessingHelper;

public class ReceiverMessageHandler implements AudioProcessingThread.Callback {

    public interface Callback {
        void setParamText(String text);
        void setCurrentPeakText(String text);
        void addTriggeredNote(String text);
    }

    private AudioProcessingThread mAudioProcessingThread;
    private Callback mCallback;

    private static final double MIN_TRIGGER_DB = -40;

    private FrequencyProcessingHelper.MidiNote mMidiNote;
    private int mDataUpdateCounter = 0;

    private boolean mActive = false;


    public ReceiverMessageHandler(@NonNull Callback callback) {
        mCallback = callback;
    }

    public void start() {
        if (mAudioProcessingThread != null) {
            mAudioProcessingThread.finish();
            mAudioProcessingThread = null;
        }
        mAudioProcessingThread = new AudioProcessingThread(this);
        mAudioProcessingThread.start();
        mActive = true;
    }

    public void stop() {
        if (mActive && mAudioProcessingThread != null) {
            mAudioProcessingThread.finish();
            mAudioProcessingThread = null;
            mActive = false;
        }
    }


    // AudioProcessingThread.Callback implementation

    @Override
    public void onInitParams(final float sampleRate, final int fftLen,
                             final double timeDurationPref, final int nFFTAverage) {
        if (mCallback != null) {
            App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCallback.setParamText("Params:" +
                            "\n\tSample rate: "+sampleRate+
                            "\n\tfftLen: "+fftLen+
                            "\n\ttimeDurationPref: "+timeDurationPref+
                            "\n\tnFFTAverage: "+nFFTAverage);
                }
            });
        }
    }

    @Override
    public void updateRawData(double[] data) {
        //Log.d(TAG, "data [frame "+mDataUpdateCounter+"]: ("+data.length+") "+data);
        //mDataUpdateCounter++;
    }

    @Override
    public void updatePeakData(final double maxAmpFreq, final double maxAmpDB) {
        if (maxAmpFreq == 0) {
            // discard if freq zero, send on init
            return;
        }
        if (mCallback != null) {
            App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    FrequencyProcessingHelper.MidiNote midiNote = FrequencyProcessingHelper.getMidinote(maxAmpFreq);
                    String peakInfoStr = buildPeakInfoString(maxAmpFreq, maxAmpDB, midiNote);
                    mCallback.setCurrentPeakText(peakInfoStr);
                    if (maxAmpDB >= MIN_TRIGGER_DB) {
                        if (midiNote == null || mMidiNote == null
                                || !mMidiNote.getStr().equals(midiNote.getStr())) {
                            mCallback.addTriggeredNote(peakInfoStr);
                        }
                    }
                    mMidiNote = midiNote;
                }
            });
        }
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
