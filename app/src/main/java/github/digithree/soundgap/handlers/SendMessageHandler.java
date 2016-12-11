/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import github.digithree.soundgap.player.SinVoicePlayer;

public class SendMessageHandler implements SinVoicePlayer.Listener {

    private final static String CODEBOOK = "0123456789";
    private final static int TIME_BETWEEN_REPEATS = 1000;

    private SinVoicePlayer mSinVoicePlayer;

    public SendMessageHandler() {
        mSinVoicePlayer = new SinVoicePlayer(CODEBOOK);
        mSinVoicePlayer.setListener(this);
    }


    // public interface

    public void sendMessage(String text) {
        // TODO : use text String instead of bogus string
        mSinVoicePlayer.play("139410", true, TIME_BETWEEN_REPEATS);
    }


    // SinVoicePlayer.Listener implementation

    @Override
    public void onPlayEnd() {
        //not used
    }

    @Override
    public void onPlayStart() {
        //not used
    }
}
