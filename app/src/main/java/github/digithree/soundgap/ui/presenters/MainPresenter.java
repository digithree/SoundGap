/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.ui.presenters;

import android.text.TextUtils;

import github.digithree.soundgap.handlers.ReceiverMessageHandler;
import github.digithree.soundgap.handlers.SendMessageHandler;
import github.digithree.soundgap.ui.interfaces.IActivityLifecycle;
import github.digithree.soundgap.ui.interfaces.IMainView;
import github.digithree.soundgap.ui.interfaces.IMainViewCallbacks;

public class MainPresenter extends BasePresenter<IMainView> implements IActivityLifecycle,
        IMainViewCallbacks, ReceiverMessageHandler.Callback, SendMessageHandler.Callback {

    private SendMessageHandler mSendMessageHandler;
    private ReceiverMessageHandler mReceiverMessageHandler;

    private boolean hasRecordPermission;

    public MainPresenter(IMainView view) {
        super(view);

        mSendMessageHandler = new SendMessageHandler(this);
        mReceiverMessageHandler = new ReceiverMessageHandler(this);

        hasRecordPermission = false;
    }


    // IMainViewCallbacks implementation

    @Override
    public void setRecordPermission(boolean hasPermission) {
        hasRecordPermission = hasPermission;
    }

    @Override
    public void clickListen() {
        if (hasRecordPermission) {
            if (!mReceiverMessageHandler.isActive()) {
                mReceiverMessageHandler.start();
            } else {
                mReceiverMessageHandler.stop();
            }
        } else {
            getView().showListeningForMessagesError();
        }
    }

    @Override
    public void clickPeakListClear() {
        getView().clearMessages();
    }

    @Override
    public void clickSendMessage() {
        String messageText = getView().getMessageToSend();
        if (!TextUtils.isEmpty(messageText)) {
            mSendMessageHandler.sendMessage(messageText);
        } else {
            getView().showSendMessageError();
        }
    }


    // IActivityLifecycle implementation

    @Override
    public void onPause() {
        mReceiverMessageHandler.stop();
    }

    @Override
    public void onResume() {
        //not used
    }


    // ReceiverMessageHandler.Callback implementation

    @Override
    public void startedListening() {
        getView().showListeningForMessages();
    }

    @Override
    public void stoppedListening() {
        getView().showStopListeningForMessages();
    }

    @Override
    public void heardMessage(String message) {
        getView().addNewMessage(message);
    }


    // SendMessageHandler.Callback implementation

    @Override
    public void startedSending() {
        getView().showSendMessageInProgress();
    }

    @Override
    public void sendError() {
        getView().showSendMessageError();
    }

    @Override
    public void messageSent() {
        getView().showSendMessageSuccess();
    }
}

