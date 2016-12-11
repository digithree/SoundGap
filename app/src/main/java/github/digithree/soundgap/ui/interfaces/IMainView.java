/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */


package github.digithree.soundgap.ui.interfaces;

public interface IMainView {

    void setParamsText(String text);
    void setPeakText(String text);

    // peak list
    void addPeakListItem(String text);
    void clearPeakListItems();

    // sending messages
    String getMessage();

    // error feedback
    void showSendMessageError();
}
