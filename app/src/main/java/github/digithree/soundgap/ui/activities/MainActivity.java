/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import github.digithree.soundgap.App;
import github.digithree.soundgap.R;
import github.digithree.soundgap.fft.AudioProcessingThread;
import github.digithree.soundgap.fft.FrequencyProcessingHelper;

public class MainActivity extends AppCompatActivity implements AudioProcessingThread.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private static final double MIN_TRIGGER_DB = -40;

    private TextView mTvTitle;
    private TextView mTvDetail;
    private LinearLayout mLlPeakInfo;
    private Button mBtnClearList;

    private AudioProcessingThread mAudioProcessingThread;
    private boolean hasRecordPermission = false;

    private int mDataUpdateCounter = 0;

    FrequencyProcessingHelper.MidiNote mMidiNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();

        mBtnClearList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLlPeakInfo.post(new Runnable() {
                    @Override
                    public void run() {
                        mLlPeakInfo.removeAllViews();
                    }
                });
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasRecordPermission = checkForAccessPermissions(this);
        } else {
            hasRecordPermission = true;
        }
    }

    private void bindViews() {
        mTvTitle = (TextView) findViewById(R.id.text_view_title);
        mTvDetail = (TextView) findViewById(R.id.text_view_detail);
        mLlPeakInfo = (LinearLayout) findViewById(R.id.ll_view_peaks);
        mBtnClearList = (Button) findViewById(R.id.btn_clear_list);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (hasRecordPermission) {
            mAudioProcessingThread = new AudioProcessingThread(this);
            mAudioProcessingThread.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (hasRecordPermission) {
            if (mAudioProcessingThread != null) {
                mAudioProcessingThread.finish();
            }
        }
    }


    // AudioProcessingThread.Callback implementation

    @Override
    public void onInitParams(final float sampleRate, final int fftLen,
                             final double timeDurationPref, final int nFFTAverage) {
        mTvTitle.post(new Runnable() {
            @Override
            public void run() {
                mTvTitle.setText("Params:" +
                        "\n\tSample rate: "+sampleRate+
                        "\n\tfftLen: "+fftLen+
                        "\n\ttimeDurationPref: "+timeDurationPref+
                        "\n\tnFFTAverage: "+nFFTAverage);
            }
        });
    }

    @Override
    public void updateRawData(double[] data) {
        //Log.d(TAG, "data [frame "+mDataUpdateCounter+"]: ("+data.length+") "+data);
        //mDataUpdateCounter++;
    }

    @Override
    public void updatePeakData(final double maxAmpFreq, final double maxAmpDB) {
        mTvDetail.post(new Runnable() {
            @Override
            public void run() {
                FrequencyProcessingHelper.MidiNote midiNote = FrequencyProcessingHelper.getMidinote(maxAmpFreq);
                String peakInfoStr = buildPeakInfoString(maxAmpFreq, maxAmpDB, midiNote);
                mTvDetail.setText(peakInfoStr);
                if (maxAmpDB >= MIN_TRIGGER_DB) {
                    if (midiNote == null || mMidiNote == null
                            || !mMidiNote.getStr().equals(midiNote.getStr())) {
                        TextView textView = (TextView) LayoutInflater.from(mLlPeakInfo.getContext())
                                .inflate(R.layout.view_freq_item, mLlPeakInfo, false);
                        textView.setText(peakInfoStr);
                        mLlPeakInfo.addView(textView);
                    }
                }
                mMidiNote = midiNote;
            }
        });
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

    // Permissions handling for Marshmallow +

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkForAccessPermissions(Context context) {
        int permission = App.getStaticInstance().checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ((AppCompatActivity) context).requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_ASK_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // granted
                    Toast.makeText(App.getStaticInstance(), "Record permission granted", Toast.LENGTH_SHORT).show();
                    hasRecordPermission = true;
                    if (mAudioProcessingThread == null) {
                        mAudioProcessingThread = new AudioProcessingThread(this);
                        mAudioProcessingThread.start();
                    }
                } else {
                    // denied
                    Toast.makeText(App.getStaticInstance(), "Record permission must be granted for app to work", Toast.LENGTH_SHORT).show();
                    hasRecordPermission = false;
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
