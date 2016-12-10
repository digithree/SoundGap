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

import java.util.Locale;

/**
 * Created by simonkenny on 10/12/2016.
 */

public class FrequencyProcessingHelper {

    public static class MidiNote {
        public MidiNote(String str, int raw, int oct, int note) {
            this.str = str;
            this.raw = raw;
            this.oct = oct;
            this.note = note;
        }

        String str;
        int raw;
        int oct;
        int note;

        public int getNote() {
            return note;
        }

        public int getOct() {
            return oct;
        }

        public int getRaw() {
            return raw;
        }

        public String getStr() {
            return str;
        }
    }

    private static final String[] LP = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    // Convert frequency to pitch
    // Fill with sFill until length is 6. If sFill=="", do not fill
    public static MidiNote getMidinote(double freq) {
        if (freq<=0 || Double.isNaN(freq) || Double.isInfinite(freq)) {
            return null;
        }
        // A4 = 440Hz
        double p = 69 + 12 * Math.log(freq/440.0)/Math.log(2);  // MIDI pitch
        int pi = (int) Math.round(p);
        int po = (int) Math.floor(pi/12.0);
        int pm = pi-po*12;
        return new MidiNote(
                String.format(Locale.getDefault(), "%s%d", LP[pm], po),
                pi,
                po,
                pm
        );
    }
}
