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

import android.util.Log;

import github.digithree.soundgap.App;
import github.digithree.soundgap.R;

/**
 * Created by simonkenny on 10/12/2016.
 */

public class AudioSourceHelper {
    private static final String TAG = AudioSourceHelper.class.getSimpleName();

    static String[] audioSourceNames;
    static int[] audioSourceIDs;

    private static void getAudioSourceNameFromIdPrepare() {
        audioSourceNames = App.getStaticInstance().getResources().getStringArray(R.array.audio_source);
        String[] sasid = App.getStaticInstance().getResources().getStringArray(R.array.audio_source_id);
        audioSourceIDs = new int[audioSourceNames.length];
        for (int i = 0; i < audioSourceNames.length; i++) {
            audioSourceIDs[i] = Integer.parseInt(sasid[i]);
        }
    }

    // Get audio source name from its ID
    // Tell me if there is better way to do it.
    public static String getAudioSourceNameFromId(int id) {
        if (audioSourceNames == null || audioSourceIDs == null) {
            getAudioSourceNameFromIdPrepare();
        }
        for (int i = 0; i < audioSourceNames.length; i++) {
            if (audioSourceIDs[i] == id) {
                return audioSourceNames[i];
            }
        }
        Log.e(TAG, "getAudioSourceName(): no this entry.");
        return "";
    }
}
