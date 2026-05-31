package name.monwf.customiuizer.mods;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

import name.monwf.customiuizer.mods.utils.ModuleHelper;

public class StepCounterController {

    private static int cachedSteps = 0;
    private static long lastUpdateTime = 0;

    /**
     * Formats the step count with the carrier text template.
     * The template is stored in the view info under key "stepsTpl".
     * If the sensor is not available, falls back to the template with "0".
     */
    public static String getStepsShowValue(TextView textView) {
        String tpl = (String) ModuleHelper.getViewInfo(textView, "stepsTpl");
        if (tpl == null) {
            tpl = "%s";
        }
        int steps = getCurrentSteps(textView.getContext());
        return String.format(tpl, String.valueOf(steps));
    }

    private static int getCurrentSteps(Context context) {
        long now = System.currentTimeMillis();
        // Cache step count for 5 seconds to avoid excessive sensor queries
        if (now - lastUpdateTime < 5000 && cachedSteps > 0) {
            return cachedSteps;
        }

        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor != null) {
                // Read the last known value from the sensor
                // Android doesn't allow reading sensor value directly, so we use a short registration
                final int[] result = {cachedSteps};
                final Object lock = new Object();

                SensorEventListener listener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        result[0] = (int) event.values[0];
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                };

                sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
                synchronized (lock) {
                    try {
                        lock.wait(500);
                    } catch (InterruptedException ignored) {}
                }
                sensorManager.unregisterListener(listener);

                cachedSteps = result[0];
                lastUpdateTime = now;
            }
        } catch (Throwable t) {
            // Sensor not available or permission denied
        }

        return cachedSteps;
    }
}
