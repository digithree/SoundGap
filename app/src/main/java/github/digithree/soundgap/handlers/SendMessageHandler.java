/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import github.digithree.soundgap.App;
import github.digithree.soundgap.player.SinVoicePlayer;

public class SendMessageHandler implements SinVoicePlayer.Listener {

    public interface Callback {
        void startedSending();
        void sendError();
        void messageSent();
    }


    //private final static int TIME_BETWEEN_REPEATS = 1000;

    private SinVoicePlayer mSinVoicePlayer;
    private boolean mSending;

    private Callback mCallback;


    public SendMessageHandler(@NonNull Callback callback) {
        mSinVoicePlayer = new SinVoicePlayer();
        mSinVoicePlayer.setListener(this);
        mCallback = callback;
        mSending = false;
    }


    // public interface

    public void sendMessage(String text) {
        if (!mSending) {
            if (!TextUtils.isEmpty(text)) {
                String codedMessage = MessageCodex.encode(text);
                mSinVoicePlayer.play(codedMessage, false, 0);
                mSending = true;
            } else {
                if (mCallback != null) {
                    mCallback.sendError();
                }
            }
        }
    }

    public void cancelSendingMessage() {
        if (mSending) {
            mSinVoicePlayer.stop();
            mSending = false;
            if (mCallback != null) {
                mCallback.sendError(); // TODO : consider making good stop callback method
            }
        }
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
        mSending = false;
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
        mSending = true;
    }
}
