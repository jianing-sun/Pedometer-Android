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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;
import org.w3c.dom.Text;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainFragment extends Fragment implements SensorEventListener {

    private PieChart pc;
//    private BarChart bc;
    private PieModel Current, Goal;

    private TextView stepsView;

    private int todayOffset, total_start, goal, since_boot, total_days;
    private boolean showSteps = true;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());




    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_main, null);
        stepsView = (TextView) view.findViewById(R.id.steps);
        pc = (PieChart) view.findViewById(R.id.piechart);

        Current = new PieModel("", 0 , Color.parseColor("#99CC00"));
        pc.addPieSlice(Current);

        Goal = new PieModel("", SettingFragment.DEFAULT_GOAL, Color.parseColor("#CC0000"));
        pc.addPieSlice(Goal);

        pc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSteps = !showSteps;
                stepUnitUpdate();
            }
        });

        pc.setDrawValueInPie(false);
        pc.setUsePieRotation(true);
        pc.startAnimation();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
//        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);

        myPedometerDatabase db = myPedometerDatabase.getInstance(getActivity());

        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        goal = prefs.getInt("goal", SettingFragment.DEFAULT_GOAL);
        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        if (!prefs.contains("pauseCount")) {
            SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
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
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
            }
        }

        since_boot -= pauseDifference;

        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();

        stepUnitUpdate();


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    // TODO: add the unit of distance
    private void stepUnitUpdate() {
        // show "steps" by default
//        if (showSteps) {
//            ((TextView) getView().findViewById(R.id.unit)).setText("steps");
//        }
        updatePie();
        updateBars();
    }

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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // won't happen
    }

    private void updatePie() {
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + since_boot, 0);
        Current.setValue(steps_today);
        if (goal - steps_today > 0) {
            // goal not reached yet
            if (pc.getData().size() == 1) {
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
            stepsView.setText(formatter.format(steps_today));
//            totalView.setText(formatter.format(total_start + steps_today));
//            averageView.setText(formatter.format((total_start + steps_today) / total_days));
        } else {
            // update only every 10 steps when displaying distance
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", SettingFragment.DEFAULT_STEP_SIZE);
            float distance_today = steps_today * stepsize;
            float distance_total = (total_start + steps_today) * stepsize;
            if (prefs.getString("stepsize_unit", SettingFragment.DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                distance_today /= 100000;
                distance_total /= 100000;
            } else {
                distance_today /= 5280;
                distance_total /= 5280;
            }
            stepsView.setText(formatter.format(distance_today));
//            totalView.setText(formatter.format(distance_total));
//            averageView.setText(formatter.format(distance_total / total_days));
        }
    }

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
        for (int i = last.size() - 1; i > 0; i--) {
            Pair<Long, Integer> current = last.get(i);
            steps = current.second;
            if (steps > 0) {
                bm = new BarModel(df.format(new Date(current.first)), 0,
                        steps > goal ? Color.parseColor("#99CC00") : Color.parseColor("#0099cc"));
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
        if (barChart.getData().size() > 0) {
            barChart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Statistics.getDialog(getActivity(), since_boot).show();
                }
            });
            barChart.startAnimation();
        } else {
            barChart.setVisibility(View.GONE);
        }
    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
//        MenuItem pause = menu.getItem(0);
        //pause.setTitle("Pause");


    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_pause:
//                Toast.makeText(getContext(), "???", Toast.LENGTH_SHORT).show();
//                return true;
            default:
                return ((MainActivity) getActivity()).optionsItemSelected(item);
        }
    }
}















