/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */


package github.digithree.soundgap.ui.presenters;

import java.lang.ref.WeakReference;

public abstract class BasePresenter<T> {
    public final String TAG = this.getClass().getSimpleName();

    private WeakReference<T> mWeakReferenceView;

    public BasePresenter(T view) {
        attachView(view);
    }

    public void attachView(T view) {
        this.mWeakReferenceView = new WeakReference<T>(view);
    }

    public void detachView() {
        this.mWeakReferenceView = null;
    }

    protected T getView() {
        if (mWeakReferenceView != null) {
            return mWeakReferenceView.get();
        }
        return null;
    }

    protected boolean hasView() {
        if (mWeakReferenceView != null) {
            return mWeakReferenceView.get() != null;
        }
        return false;
    }
}