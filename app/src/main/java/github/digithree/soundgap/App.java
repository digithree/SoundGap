/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

public class App extends Application {

    protected static App sInstance;

    public static App getStaticInstance() {
        return sInstance;
    }

    public static Handler sMainThreadHandler, sCVHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public Handler getMainThreadHandler() {
        if (sMainThreadHandler == null) {
            sMainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return sMainThreadHandler;
    }

    public Handler getHandler() {
        if (sCVHandler == null) {
            sCVHandler = new Handler();
        }
        return sCVHandler;
    }
}
