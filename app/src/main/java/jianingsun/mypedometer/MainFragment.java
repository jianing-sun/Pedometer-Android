package jianingsun.mypedometer;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class extends from fragment and implements SensorEventListener
 * mainly used for creating a fragment in the main activity and getting
 * steps and other information from myPedometerDatabase
 * and then update the pie chart and bar chart with steps and distance transfer
 * create a menu in the top left of the view used to change step size and goal,
 * details about setting can be found in SettingFragment.java
 */

public class MainFragment extends Fragment implements SensorEventListener {


    // define piechart, piemodel, and a textview to show the number of steps
    private PieChart pc;
    private PieModel Current, Goal;
    private TextView stepsView;

    // total steps before today, user setting goal, today steps and total days
    private int todayOffset, total_start, goal, since_boot, total_days;

    // TODO: display "steps" as the unit of walking steps and change it to "km" when use taps it
    private boolean showSteps = true;
    // return a general-purpose number format for the specified locale.
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

    /**
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     *
     * initialize piechart with 2 slices: Goal and Current steps, create a view
     */

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // create a view from xml file
        final View view = inflater.inflate(R.layout.activity_main, null);
        stepsView = (TextView) view.findViewById(R.id.steps);
        pc = (PieChart) view.findViewById(R.id.piechart);

        Current = new PieModel("", 0 , Color.parseColor("#1dd1a1"));
        pc.addPieSlice(Current);

        Goal = new PieModel("", SettingFragment.DEFAULT_GOAL, Color.parseColor("#c8d6e5"));
        pc.addPieSlice(Goal);

        // once tapped the step number, change the unit from "steps" to distance
        pc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSteps = !showSteps;
                stepUnitUpdate();
            }
        });

        // show the value from Textview (not the build-in place) in the middle, set rotation animation
        pc.setDrawValueInPie(false);
        pc.setUsePieRotation(true);
        pc.startAnimation();

        return view;
    }


    /**
     * update the database after running in onResume state
     */
    @Override
    public void onResume() {
        super.onResume();

        myPedometerDatabase db = myPedometerDatabase.getInstance(getActivity());

        // TODO: a bug here
        todayOffset = db.getSteps(Util.getToday());
        System.out.println(todayOffset);

        SharedPreferences prefs = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        goal = prefs.getInt("goal", SettingFragment.DEFAULT_GOAL);
        // date = -1
        since_boot = db.getCurrentSteps();
        // get the difference between the step value of now and the step value of last pause
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        if (!prefs.contains("pauseCount")) {
            SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            // TYPE_STEP_COUNTER: A sensor of this type returns the number of steps taken by the user
            // since the last reboot while activated.
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (sensor == null) {
                new AlertDialog.Builder(getActivity()).setTitle("Sensor not found")
                        .setMessage("We need the step sensor hardware in your phone")
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(final DialogInterface dialogInterface) {
                                getActivity().finish();
                            }
                        }).setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create().show();
            } else {
                // registerListener(the class which connects with the SensorEventListener,
                //                  the sensor we need to register)
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
            }
        }
        // ignore the steps during pause
        since_boot -= pauseDifference;
        // get total steps before today
        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();
        // close the database
        db.close();
        // update unit
        stepUnitUpdate();


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * update the unit in bar chart and pie chart
     */
    private void stepUnitUpdate() {
        // change the unit between "steps" and "km" by tapping the step number
        if (showSteps) {
            ((TextView) getView().findViewById(R.id.units)).setText("steps");
        } else {
            String unit = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .getString("stepsize_unit", SettingFragment.DEFAULT_STEP_UNIT);
            if (unit.equals("cm")) {
                unit = "km";
            } else {
                // TODO
                unit = "mi";
            }
            ((TextView) getView().findViewById(R.id.units)).setText(unit);
        }

        // update the unit in both piechart and barchart
        updatePie();
        updateBars();
    }

    /**
     * once get into onPause state, save the current data into database
     * don't forget to close the database
     */
    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myPedometerDatabase db = myPedometerDatabase.getInstance(getActivity());
        db.saveCurrentSteps(since_boot);
        db.close();
    }

    /**
     * Called when there is a new sensor event.
     * sensorEvent[0] represents the x axis of gyroscope (I think so)
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.values[0] > Integer.MAX_VALUE || sensorEvent.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) sensorEvent.values[0];
            myPedometerDatabase db = myPedometerDatabase.getInstance(getActivity());
            db.insertNewDay(Util.getToday(), (int) sensorEvent.values[0]);
            db.close();
        }
        since_boot = (int) sensorEvent.values[0];
        updatePie();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // won't happen
    }

    /**
     * update pie chart with goal and current two slices as well as steps and distance
     */
    private void updatePie() {
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + since_boot, 0);
        Current.setValue(steps_today);
        if (goal - steps_today > 0) {
            // goal not reached yet
            System.out.println(pc.getData().size());
            if (pc.getData().size() == 1) {
                System.out.println(pc.getData());
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pc.addPieSlice(Goal);
            }
            Goal.setValue(goal - steps_today);
        } else {
            // goal reached
            pc.clearChart();
            pc.addPieSlice(Current);
        }
        pc.update();
        if (showSteps) {
            // format a number to String and set it to the stepsView
            stepsView.setText(formatter.format(steps_today));
        } else {
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", SettingFragment.DEFAULT_STEP_SIZE);
            float distance_today = steps_today * stepsize;
            float distance_total = (total_start + steps_today) * stepsize;

            // transfer steps into distance
            if (prefs.getString("stepsize_unit", SettingFragment.DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                distance_today /= 100000;
                distance_total /= 100000;
            } else {
                distance_today /= 5280;
                distance_total /= 5280;
            }
            stepsView.setText(formatter.format(distance_today));

        }
    }

    /**
     * update and display the past several days (up to 7) data to the bar chart
     */
    private void updateBars() {
        SimpleDateFormat df = new SimpleDateFormat("E", Locale.getDefault());
        BarChart barChart = (BarChart) getView().findViewById(R.id.barchart);
        if (barChart.getData().size() > 0) barChart.clearChart();
        int steps;
        float distance, stepsize = SettingFragment.DEFAULT_STEP_SIZE;
        boolean stepsize_cm = true;
        if (!showSteps) {
            // load some more settings if distance is needed
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            stepsize = prefs.getFloat("stepsize_value", SettingFragment.DEFAULT_STEP_SIZE);
            stepsize_cm = prefs.getString("stepsize_unit", SettingFragment.DEFAULT_STEP_UNIT)
                    .equals("cm");
        }
        barChart.setShowDecimal(!showSteps); // show decimal in distance view only
        BarModel bm;
        myPedometerDatabase db = myPedometerDatabase.getInstance(getActivity());
        List<Pair<Long, Integer>> last = db.getLastEntries(8);
        db.close();
        // display the past 7 days step data with either steps or distance
        for (int i = last.size() - 1; i > 0; i--) {
            Pair<Long, Integer> current = last.get(i);
            steps = current.second;
            if (steps > 0) {
                bm = new BarModel(df.format(new Date(current.first)), 0,
                        steps > goal ? Color.parseColor("#1dd1a1") : Color.parseColor("#c8d6e5"));
                if (showSteps) {
                    bm.setValue(steps);
                } else {
                    distance = steps * stepsize;
                    if (stepsize_cm) {
                        distance /= 100000;
                    } else {
                        distance /= 5280;
                    }
                    distance = Math.round(distance * 1000) / 1000f; // 3 decimals
                    bm.setValue(distance);
                }
                barChart.addBar(bm);
            }
        }
        // only show the bar chart when there's already existing one day data at least
        if (barChart.getData().size() > 0) {
            barChart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // call the statistics dialog to show record, average, etc.
                    Statistics.getDialog(getActivity(), since_boot).show();
                }
            });
            barChart.startAnimation();
        } else {
            barChart.setVisibility(View.GONE);
        }
    }

    /**
     * display the menu with settings
     * TODO: add more item in the menu (about, intro, etc.)
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        // TODO: pause and resume?? (i think it's meanlingless to add this function, maybe find sth different)

    }

    /**
     * connect menu with fragment and activity
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {

            default:
                return ((MainActivity) getActivity()).optionsItemSelected(item);
        }
    }
}















