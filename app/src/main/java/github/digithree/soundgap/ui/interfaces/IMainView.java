/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */


package github.digithree.soundgap.ui.interfaces;

public interface IMainView {

    // misc
    void setStatusText(String text);

    // receiving messages
    void showListeningForMessages();
    void showStopListeningForMessages();
    void showListeningForMessagesError();

    // message list
    void addNewMessage(String text);
    void clearMessages();

    // sending messages
    String getMessageToSend();
    void showSendMessageInProgress();
    void showSendMessageSuccess();
    void showSendMessageError();
}
