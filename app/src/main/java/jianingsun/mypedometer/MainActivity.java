package jianingsun.mypedometer;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

/**
 * Created by jianingsun on 2018-02-11.
 */

public class MainActivity extends FragmentActivity{

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                break;

        }
        return true;
    }
}
