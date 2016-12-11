/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.ui.interfaces;

public interface IMainViewCallbacks {

    void setRecordPermission(boolean hasPermission);

    void clickListen();

    void clickPeakListClear();
    void clickSendMessage();
}
