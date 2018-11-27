package com.projects.synergy.sampleaccelerometerapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener{

    //setting variables
    private SensorManager mSensorManager;
    private Sensor accelerometerSensor;
    private List<AccelerometerClass> accelerometerDataList;
    private AccelerometerClass accelerometerData;
    private GraphView accelerometerGraph;
    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;
    int index=0;
    long curTime;
    long diffTime;
    long lastUpdate= System.currentTimeMillis();
    private Boolean isShowGraph = false;
    private Boolean isRecordData = false;
    private Button playBtn;
    private Button recordBtn;
    private Button stopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.playBtn = (Button) findViewById(R.id.playBtn);
        this.recordBtn = (Button)findViewById(R.id.recBtn);
        this.stopBtn = (Button)findViewById(R.id.stopBtn);

        this.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playBtnClicked();
            }
        });
        this.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopBtnClicked();
            }
        });
        this.recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordBtnClicked();
            }
        });

        this.recordBtn.setEnabled(false);
        this.recordBtn.setVisibility(View.INVISIBLE);
        this.stopBtn.setEnabled(false);
        this.stopBtn.setVisibility(View.INVISIBLE);
        //to identify the sensors available in an android device
        this.mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //to check if accelerometer is present in a device
        if(this.mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)!=null){
            System.out.println("inside");

        }else{
            //displaying message if no accelerometer sensor is found in the device.
            Toast.makeText(this, "There is no accelerometer in this device!", Toast.LENGTH_LONG);
        }
    }

    //play pressed
    public void playBtnClicked()
    {
        if(this.mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)!=null) {

            isShowGraph = true;
            this.playBtn.setEnabled(false);
            this.recordBtn.setEnabled(true);
            this.stopBtn.setEnabled(false);
            this.playBtn.setVisibility(View.INVISIBLE);
            this.recordBtn.setVisibility(View.VISIBLE);
            this.stopBtn.setVisibility(View.INVISIBLE);

            //setting reference of the accelerometer to the variable accelerometerSensor
            this.accelerometerSensor = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            //initializing arraylist
            this.accelerometerDataList = new ArrayList<AccelerometerClass>();

            //displaying message accelerometer found
            Toast.makeText(this, "Play pressed!", Toast.LENGTH_LONG);

            this.index = 0;
            //adding accelerometer data list values for the starting
            this.accelerometerDataList.add(new AccelerometerClass(0, 0, 0, 0, 0));

            //intializing accelerometer graph
            initializeAccelerometerGraph();
            lastUpdate= System.currentTimeMillis();
            //register to start taking sensor value
            mSensorManager.registerListener(this, this.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        }
    }

    public void recordBtnClicked(){
        isRecordData = !isRecordData;
        if(isRecordData){
            this.playBtn.setEnabled(false);
            this.recordBtn.setEnabled(false);
            this.stopBtn.setEnabled(true);
            this.playBtn.setVisibility(View.INVISIBLE);
            this.recordBtn.setVisibility(View.INVISIBLE);
            this.stopBtn.setVisibility(View.VISIBLE);

            mSensorManager.unregisterListener(this);

            //initializing arraylist
            this.accelerometerDataList = new ArrayList<AccelerometerClass>();

            //displaying message accelerometer found
            Toast.makeText(this, "Recording Started!", Toast.LENGTH_LONG);

            this.index = 0;
            //adding accelerometer data list values for the starting
            this.accelerometerDataList.add(new AccelerometerClass(0, 0, 0, 0, 0));
            this.accelerometerGraph.removeAllSeries();
            initializeAccelerometerGraph();
            lastUpdate= System.currentTimeMillis();
            //register to start taking sensor value
            mSensorManager.registerListener(this, this.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopBtnClicked()
    {

        //unregistering sensor when application is on pause
        //this is done to save battery
        mSensorManager.unregisterListener(this);
        this.accelerometerGraph.removeAllSeries();
        isShowGraph = false;
        isRecordData = false;
        this.playBtn.setEnabled(true);
        this.recordBtn.setEnabled(false);
        this.stopBtn.setEnabled(false);
        this.playBtn.setVisibility(View.VISIBLE);
        this.recordBtn.setVisibility(View.INVISIBLE);
        this.stopBtn.setVisibility(View.INVISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Recorded Data");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveDataToFileWithName(input.getText().toString());
            }
        });


        builder.show();

    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void saveDataToFileWithName(String fileName){

        if(isExternalStorageWritable()){
            //saving data onto a file
            File myFile = new File(Environment.getExternalStorageDirectory()+"/Documents/"+fileName+".txt");
            try {

                myFile.createNewFile();
                FileOutputStream fOut = new FileOutputStream(myFile);

                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                for(AccelerometerClass accel: this.accelerometerDataList) {
                    myOutWriter.append(String.valueOf(accel.getxAxisValue()));
                    myOutWriter.append('\t');
                    myOutWriter.append(String.valueOf(accel.getyAxisValue()));
                    myOutWriter.append('\t');
                    myOutWriter.append(String.valueOf(accel.getzAxisValue()));
                    myOutWriter.append('\t');
                    myOutWriter.append(String.valueOf(accel.getTimestamp()));
                    myOutWriter.append('\n');
                }
                myOutWriter.close();
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //this method is implemented as part of SensorEventListener
    //it is called automatically at specific time intervals by the phone to retrieve accelerometer values
    //event object contains sensor values at a timeinstance
    @Override
    public void onSensorChanged(SensorEvent event) {

        //creating a accelerometerclass and filling in all the data
        this.accelerometerData = new AccelerometerClass();
        this.accelerometerData.setxAxisValue(event.values[0]);
        this.accelerometerData.setyAxisValue(event.values[1]);
        this.accelerometerData.setzAxisValue(event.values[2]);
        this.accelerometerData.setAccuracy(event.accuracy);

        //calculating time lapse
        long curTime = System.currentTimeMillis();
        diffTime = (curTime - this.lastUpdate);


        //setting time lapse between consecutive datapoints
        this.accelerometerData.setTimestamp(diffTime);

        //adding the class to the list of accelerometer data points
        this.accelerometerDataList.add(accelerometerData);



        //displaying accelerometer values on the console
        String display = String.valueOf(this.accelerometerData.getxAxisValue())+ "; "
                +String.valueOf(this.accelerometerData.getyAxisValue())+"; "
                +String.valueOf(this.accelerometerData.getzAxisValue())+"; "
                +String.valueOf(this.accelerometerData.getTimestamp());
        //Toast.makeText(this, display, Toast.LENGTH_LONG);
        System.out.println(display);

        //updating graph display
        updateAccelerometerGraph();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //registering the sensor when application is resumed
    //continues retrieving data from sensor
    @Override
    protected void onResume() {
        super.onResume();
        if(isShowGraph){
            mSensorManager.registerListener(this, this.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    //method called when application is on pause
    @Override
    protected void onPause() {
        super.onPause();

    }


    public void initializeAccelerometerGraph(){

        this.accelerometerGraph = (GraphView) findViewById(R.id.accelerometerGraph);
        //creating series for x axis plot
        this.seriesX = new LineGraphSeries<DataPoint>();
        this.seriesX.setColor(Color.RED);
        //creating series for y axis plot
        this.seriesY = new LineGraphSeries<DataPoint>();
        this.seriesY.setColor(Color.BLUE);
        //creating series for z axis plot
        this.seriesZ = new LineGraphSeries<DataPoint>();
        this.seriesZ.setColor(Color.GREEN);

        // legend
        this.seriesX.setTitle("xAxis");
        this.seriesY.setTitle("yAxis");
        this.seriesZ.setTitle("zAxis");
        this.accelerometerGraph.getLegendRenderer().setVisible(true);
        this.accelerometerGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.accelerometerGraph.getViewport().setXAxisBoundsManual(true);
        this.accelerometerGraph.getViewport().setMinX(0);
        this.accelerometerGraph.getViewport().setMaxX(9);

        // set manual Y bounds
        this.accelerometerGraph.getViewport().setYAxisBoundsManual(true);
        this.accelerometerGraph.getViewport().setMinY(-10);
        this.accelerometerGraph.getViewport().setMaxY(10);
        this.accelerometerGraph.addSeries(this.seriesX);
        this.accelerometerGraph.addSeries(this.seriesY);
        this.accelerometerGraph.addSeries(this.seriesZ);
    }

    //update accelerometer data
    public void updateAccelerometerGraph(){

        this.index=this.index+1;

        System.out.println("inside update!");

        this.seriesX.appendData(new DataPoint(index, this.accelerometerDataList.get(this.index).getxAxisValue()),
                true, 10);

        this.seriesY.appendData(new DataPoint(index, this.accelerometerDataList.get(this.index).getyAxisValue()),
                true, 10);

        this.seriesZ.appendData(new DataPoint(index, this.accelerometerDataList.get(this.index).getzAxisValue()),
                true, 10);
    }
}
