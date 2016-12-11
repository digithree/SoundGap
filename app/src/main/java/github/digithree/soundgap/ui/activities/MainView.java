/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import github.digithree.soundgap.App;
import github.digithree.soundgap.R;
import github.digithree.soundgap.ui.interfaces.IMainView;
import github.digithree.soundgap.ui.presenters.MainPresenter;

public class MainView extends AppCompatActivity implements IMainView {
    private static final String TAG = MainView.class.getSimpleName();

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private TextView mTvTitle;
    private TextView mTvDetail;
    private LinearLayout mLlPeakInfo;
    private Button mBtnClearList;
    private EditText mEtMessage;
    private Button mBtnSend;

    private MainPresenter mMainPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();

        mMainPresenter = new MainPresenter(this);

        mBtnClearList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMainPresenter.clickPeakListClear();
            }
        });

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMainPresenter.clickSendMessage();
            }
        });

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkForAccessPermissions(this)) {
            mMainPresenter.setRecordPermission(true);
        } else {
            mMainPresenter.setRecordPermission(false);
        }
    }

    private void bindViews() {
        mTvTitle = (TextView) findViewById(R.id.text_view_title);
        mTvDetail = (TextView) findViewById(R.id.text_view_detail);
        mLlPeakInfo = (LinearLayout) findViewById(R.id.ll_view_peaks);
        mBtnClearList = (Button) findViewById(R.id.btn_clear_list);
        mEtMessage = (EditText) findViewById(R.id.edit_text_message);
        mBtnSend = (Button) findViewById(R.id.btn_send);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainPresenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMainPresenter.onPause();
    }


    // IMainView

    @Override
    public void setParamsText(String text) {
        mTvTitle.setText(text);
    }

    @Override
    public void setPeakText(String text) {
        mTvDetail.setText(text);
    }

    @Override
    public void addPeakListItem(String text) {
        TextView textView = (TextView) LayoutInflater.from(mLlPeakInfo.getContext())
                .inflate(R.layout.view_freq_item, mLlPeakInfo, false);
        textView.setText(text);
        mLlPeakInfo.addView(textView);
    }

    @Override
    public void clearPeakListItems() {
        mLlPeakInfo.removeAllViews();
    }

    @Override
    public String getMessage() {
        return mEtMessage.getText().toString();
    }

    @Override
    public void showSendMessageError() {
        Toast.makeText(App.getStaticInstance(), "Can't send empty message", Toast.LENGTH_SHORT).show();
    }

    // Permissions handling for Marshmallow +

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkForAccessPermissions(Context context) {
        int permission = App.getStaticInstance().checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ((AppCompatActivity) context).requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_ASK_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // granted
                    Toast.makeText(App.getStaticInstance(), "Record permission granted", Toast.LENGTH_SHORT).show();
                    mMainPresenter.setRecordPermission(true);
                } else {
                    // denied
                    Toast.makeText(App.getStaticInstance(), "Record permission must be granted for app to work", Toast.LENGTH_SHORT).show();
                    mMainPresenter.setRecordPermission(false);
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
