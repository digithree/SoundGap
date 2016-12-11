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
        IMainViewCallbacks, ReceiverMessageHandler.Callback {

    private SendMessageHandler mSendMessageHandler;
    private ReceiverMessageHandler mReceiverMessageHandler;

    private boolean hasRecordPermission;

    public MainPresenter(IMainView view) {
        super(view);

        mSendMessageHandler = new SendMessageHandler();
        mReceiverMessageHandler = new ReceiverMessageHandler(this);

        hasRecordPermission = false;
    }


    // IMainViewCallbacks implementation

    @Override
    public void setRecordPermission(boolean hasPermission) {
        hasRecordPermission = hasPermission;
        if (hasRecordPermission) {
            // TODO : remove this from automatic starting
            mReceiverMessageHandler.start();
        }
    }

    @Override
    public void clickPeakListClear() {
        getView().clearPeakListItems();
    }

    @Override
    public void clickSendMessage() {
        String messageText = getView().getMessage();
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
        // TODO : remove this from automatic starting
        if (hasRecordPermission) {
            mReceiverMessageHandler.start();
        }
    }


    // ReceiverMessageHandler.Callback implementation

    @Override
    public void setParamText(String text) {
        getView().setParamsText(text);
    }

    @Override
    public void setCurrentPeakText(String text) {
        getView().setPeakText(text);
    }

    @Override
    public void addTriggeredNote(String text) {
        getView().addPeakListItem(text);
    }
}

