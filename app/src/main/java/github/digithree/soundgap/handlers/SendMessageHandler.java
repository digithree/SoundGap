/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import android.support.annotation.NonNull;

import github.digithree.soundgap.App;
import github.digithree.soundgap.player.SinVoicePlayer;

public class SendMessageHandler implements SinVoicePlayer.Listener {

    public interface Callback {
        void startedSending();
        void sendError();
        void messageSent();
    }

    private final static String CODEBOOK = "0123456789";
    private final static int TIME_BETWEEN_REPEATS = 1000;

    private SinVoicePlayer mSinVoicePlayer;

    private Callback mCallback;


    public SendMessageHandler(@NonNull Callback callback) {
        mSinVoicePlayer = new SinVoicePlayer(CODEBOOK);
        mSinVoicePlayer.setListener(this);
        mCallback = callback;
    }


    // public interface

    public void sendMessage(String text) {
        // TODO : use text String instead of bogus string
        mSinVoicePlayer.play("139410", true, TIME_BETWEEN_REPEATS);
    }


    // SinVoicePlayer.Listener implementation

    @Override
    public void onPlayEnd() {
        if (mCallback != null) {
            App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCallback.messageSent();
                }
            });
        }
    }

    @Override
    public void onPlayStart() {
        if (mCallback != null) {
            App.getStaticInstance().getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    mCallback.startedSending();
                }
            });
        }
    }
}
