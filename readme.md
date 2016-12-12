# Sound Gap

Transfer short text messages, such as URLs, etc., via sound between devices.

Features:

1. Any text, including emoji, is supported as text is encoded in Base64 for transmission.
2. Multiple clients within quality earshot can receive message.
3. Can use audio cable for steadier signal, security, etc.
4. Links, email address, etc., automatically converted to hyperlinks in message log.

# How it works

Messages are encoded by the following process:

1. A UTF-16 string is grabbed from user
2. Convert to UTF-8 Base64 encoded, which is alphanumeric except for equals (=) symbol
3. Each UTF-8 character in Base64 encoded string is interpreted as it's ASCII integer form, and transposed into a smaller range of values, fitting into a range of 00 to 99 with leftover room. The equals character is given a special number.
4. Each Base64 encoded character thus consists of a two digit code. Concatenating these digits results in a series of decimal system digits, i.e. between 0 - 9. This is the resulting code to be transmitted.

To transmit, 12 playback frequencies used, taken from the chromatic scale of musical notes in the 7th octave. E.g. the first is C7, then C#7, etc. Frequency index 0, i.e. C7, is the start code, and index 11, i.e. B7, is the stop code. Between these lie 10 frequencies, which are mapped to the digits 0 - 9.

Thus, to transmit a message, the start frequency is played, then each frequency corresponding to the digits of the coded message, then the stop frequency.

Because the receiving process listens for difference in frequency so as not to have to synchronise timings, two frequencies played together just sound like one long frequency playback. To allow for this, the start frequency is played between two of the same frequencies, which is ignored by the listener.

The listening process thus waits for the start code, then processes input until the stop code is heard. The exact reverse of the encoding process is applied to the message received, and if it was not corrupted in transmission, it is displayed on the receivers screen.

# Roadmap

Planned features:

**Smaller features**

1. Edit parameters, such as note length, background noise threshold
2. Add checksum / parity bit type feature to verify no corruption
3. Upgrade UI to minimum standard user might expect

**Bigger features**

1. Improve integrity of signal, perhaps using different frequencies, or other systems.
2. One-to-one, two-way communication mode, such as splitting information into blocks which require acknowledgement of receipt to proceed. Would allow for longer messages to be sent with safety.
3. Major upgrade of UI to more quirky, minimal design, maybe of robots talking or something.

# Acknowledgements and licence

Sound recognition based on [audio-analyzer-for-android](https://github.com/bewantbe/audio-analyzer-for-android) and using a lot of code from that. Most of it is licensed under Apache 2.0, and all modifications are marked, and all original licenses still in place and in effect. These licenses are located in the relevant source directories which are covered, in FTTLibrary [here](FFTLibrary/LICENSE) and from audio-analyzer-for-android [here](app/src/main/java/github/digithree/soundgap/fft/LICENSE).

Sound generation and playback based on [SinVoice](https://github.com/dingjikerbo/SinVoice), which was an attemp at exactly this program but was unworking at the time I cloned it at the end of Novemeber 2016. The license on that code is GNU General Public License, version 2, but this document was not originally included in the repo. I have included it [here](app/src/main/java/github/digithree/soundgap/player/LICENSE.txt) and it applies to most of the source files in the github.digithree.soundgap.player.package

Any new code is **under no licence whatsoever** and is clearly marked as such.