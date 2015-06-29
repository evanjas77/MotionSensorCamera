//-------------------------------------------------------------------------------
//Copyright 2014 IBM Corp. All Rights Reserved
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0 
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License. 
//-------------------------------------------------------------------------------

package com.ibm.mbaas.push.android.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera.Parameters;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.mbaas.push.android.camera.R;
import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import bolts.Continuation;
import bolts.Task;


public class MotionSensorCamera extends Activity {

    private static final String CLASS_NAME = MotionSensorCamera.class.getSimpleName();
    private static final String APP_ID = "applicationID";
    private static final String APP_SECRET = "applicationSecret";
    private static final String APP_ROUTE = "applicationRoute";
    private static final String DEVICE_ALIAS = "deviceAlias";
    private static final String CONSUMER_ID = "consumerId";
    private static final String PROPS_FILE = "bluelist.properties";
    //Push attributes
    private TextView txtVResult = null;
    private IBMPush push = null;
    private IBMPushNotificationListener notificationListener = null;
    private List<String> allTags;
    private List<String> subscribedTags;
    //Camera attributes
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private Button capture, switchCamera;
    private Context myContext;
    private RelativeLayout cameraPreview;
    private boolean cameraFront = false;

    //make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "MotionDetect Camera");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;

        txtVResult = (TextView) findViewById(R.id.display);

        updateTextView("Initializing auto shutter...");
        // Read from properties file.
        Properties props = new Properties();
        Context context = getApplicationContext();
        try {
            AssetManager assetManager = context.getAssets();
            props.load(assetManager.open(PROPS_FILE));
            Log.i(CLASS_NAME, "Found configuration file: " + PROPS_FILE);
        } catch (FileNotFoundException e) {
            Log.e(CLASS_NAME, "The bluelist.properties file was not found.", e);
        } catch (IOException e) {
            Log.e(CLASS_NAME, "The bluelist.properties file could not be read properly.", e);
        }
        Log.i(CLASS_NAME, "Application ID is: " + props.getProperty(APP_ID));

        // Initialize the IBM core backend-as-a-service.
        IBMBluemix.initialize(this, props.getProperty(APP_ID), props.getProperty(APP_SECRET), props.getProperty(APP_ROUTE));

        push = IBMPush.initializeService();
        push.register(props.getProperty(DEVICE_ALIAS), props.getProperty(CONSUMER_ID)).continueWith(new Continuation<String, Void>() {

            @Override
            public Void then(Task<String> task) throws Exception {

                if (task.isFaulted()) {
                    updateTextView("Error registering with Push Service. " + task.getError().getMessage() + "\n"
                            + "Push notifications will not be received.");
                } else {
                    updateTextView("Device is registered with Push Service" + "\n" + "Device Id : " + task.getResult());
                    displayTagSubscriptions().continueWith(new Continuation<Void, Void>() {

                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            subscribeToTag();
                            return null;
                        }

                    });
                }
                return null;
            }
        });

        displayTags();
        initializeAndActivateCamera();

        notificationListener = new IBMPushNotificationListener() {
            @Override
            public void onReceive(final IBMSimplePushNotification message) {
                updateTextView("Motion sensor notification received. Activating camera...");
                //TODO: Important - Interpret the incoming JSON and then initiate the camera shutter.
                /**************************INCOMING JSON**********************************************
                 * { "d": { "myName": "Arduino PIRSensor", "motionDetectedAt": "51s", "time": 1435557391031, "deviceId": "deedbafefeef" }, "_msgid": "b3ae3fb2.4c51c" }
                 *************************************************************************************/
                cameraPreview.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCamera.takePicture(null, null, mPicture);
                    }
                }, 2000);

            }

        };
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (push != null) {
            push.listen(notificationListener);
        }

        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            //if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                switchCamera.setVisibility(View.GONE);
            }
            mCamera = Camera.open(findBackFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (push != null) {
            push.hold();
        }
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    void updateTextView(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtVResult.append("\n" + message + "\n");
            }
        });
    }

    void showSimplePushMessage(final IBMSimplePushNotification message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Builder builder = new Builder(MotionSensorCamera.this);
                builder.setMessage("Notification Received : "
                        + message.toString());
                builder.setCancelable(true);
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int s) {
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void displayTags() {
        push.getTags().continueWith(new Continuation<List<String>, Void>() {

            @Override
            public Void then(Task<List<String>> task) throws Exception {
                if (task.isFaulted()) {
                    updateTextView("Error getting tags. " + task.getError().getMessage());
                    return null;
                }
                List<String> tags = task.getResult();
                updateTextView("Retrieved Tags : " + tags);
                allTags = tags;
                return null;
            }
        });
    }

    private Task<Void> displayTagSubscriptions() {

        return push.getSubscriptions().continueWith(new Continuation<List<String>, Void>() {

            @Override
            public Void then(Task<List<String>> task) throws Exception {
                if (task.isFaulted()) {
                    updateTextView("Error getting subscriptions.. " + task.getError().getMessage());
                    return null;
                }
                List<String> tags = task.getResult();
                updateTextView("Retrieved subscriptions : " + tags);
                subscribedTags = tags;
                return null;
            }
        });
    }

    private void subscribeToTag() {

        if ((subscribedTags != null && subscribedTags.size() == 0) && (allTags != null && allTags.size() != 0)) {
            push.subscribe(allTags.get(0)).continueWith(new Continuation<String, Void>() {

                @Override
                public Void then(Task<String> task) throws Exception {
                    if (task.isFaulted()) {
                        updateTextView("Error subscribing to Tag.."
                                + task.getError().getMessage());
                        return null;
                    }
                    updateTextView("Successfully Subscribed to Tag " + task.getResult());

                    return null;
                }
            });

        } else {
            updateTextView("Not subscribing to any more tags.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void initializeAndActivateCamera() {
        cameraPreview = (RelativeLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);
    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //make a new picture file
                File pictureFile = getOutputMediaFile();

                if (pictureFile == null) {
                    return;
                }
                try {
                    //write the file
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
                    toast.show();

                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }

                //refresh camera to continue preview
                mPreview.refreshCamera(mCamera);
            }
        };
        return picture;
    }
}