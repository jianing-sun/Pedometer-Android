package jianingsun.mypedometer;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by jianingsun on 2018-02-11.
 * used to call/connect MainFragment with the setting menu
 */

public class MainActivity extends FragmentActivity{

    /**
     * start sensor service
     * create MainFragment and replace the content in the view of main activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, SensorListener.class));
        if (savedInstanceState == null) {
            android.app.Fragment newFragment = new MainFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(android.R.id.content, newFragment);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();
        } else {
            finish();
        }
    }


    public boolean optionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                break;
            // add SettingFragment to back stack
            case R.id.action_settings:
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new SettingFragment()).addToBackStack(null)
                        .commit();
                break;
        }
        return true;
    }
}
