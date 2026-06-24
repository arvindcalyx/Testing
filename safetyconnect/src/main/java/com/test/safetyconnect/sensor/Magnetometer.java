package com.test.safetyconnect.sensor;

/**
 * Created on 01/01/23.
 */
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Magnetometer {

    // create an interface with one method
    public interface Listener {
        // create method with all 3
        // axis translation as argument
        void onDirectionChange(float mx, float my, float mts);
    }

    // create an instance
    private Listener listener;

    // method to set the instance
    public void setListener(Listener l) {
        listener = l;
    }

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;

    // create constructor with context as argument
    public Magnetometer(Context context) {

        // create instance of sensor manager
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // create instance of sensor with type magnetometer
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // create the sensor listener
        sensorEventListener = new SensorEventListener() {

            // If we have readings from both sensors then
            // use the readings to compute the device's orientation
            // and then update the display.
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // check if listener is different from null
                if (listener != null && sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    // pass the three floats in listener on rotation of axis
                    listener.onDirectionChange(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    // create register method
    // for sensor notifications
    public void register() {
        // call sensor manager's register listener and pass the required arguments
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    // create method to unregister
    // from sensor notifications
    public void unregister() {
        // call sensor manager's unregister listener
        // and pass the required arguments
        sensorManager.unregisterListener(sensorEventListener);
    }
}

