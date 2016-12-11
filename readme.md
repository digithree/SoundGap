# SoundGap

Still in initial stage of research.

Transfers short messages, such as URLs, etc., via sound between devices.

Current implementation uses one octave of chromatic notes in the 7th Octave as recognised by the MIDI standard.

Very basic currently.

# Acknowledgements and licence

Sound recognition based on [audio-analyzer-for-android](https://github.com/bewantbe/audio-analyzer-for-android) and using a lot of code from that. Most of it is licensed under Apache 2.0, and all modifications are marked, and all original licenses still in place and in effect. These licenses are located in the relevant source directories which are covered, in FTTLibrary [here](FFTLibrary/LICENSE) and from audio-analyzer-for-android [here](app/src/main/java/github/digithree/soundgap/fft/LICENSE).

Sound generation and playback based on [SinVoice](https://github.com/dingjikerbo/SinVoice), which was an attemp at exactly this program but was unworking at the time I cloned it at the end of Novemeber 2016. The license on that code is GNU General Public License, version 2, but this document was not originally included in the repo. I have included it [here](app/src/main/java/github/digithree/soundgap/player/LICENSE.txt) and it applies to most of the source files in the github.digithree.soundgap.player.package

Any new code is **under no licence whatsoever** and is clearly marked as such.