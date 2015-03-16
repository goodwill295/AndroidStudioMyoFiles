package com.android.game;

import com.unity3d.player.*;
import android.app.NativeActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.unity3d.player.UnityPlayer;

public class UnityPlayerNativeActivity extends NativeActivity
{
    protected UnityPlayer mUnityPlayer;		// don't change the name of this variable; referenced from native code

    private static final String TAG = UnityPlayerNativeActivity.class.getSimpleName();

    // Setup activity layout
    @Override protected void onCreate (Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().takeSurface(null);
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy

        mUnityPlayer = new UnityPlayer(this);
        if (mUnityPlayer.getSettings ().getBoolean ("hide_status_bar", true))
            getWindow ().setFlags (WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();

        Hub hub = Hub.getInstance();

        DeviceListener mListener = new AbstractDeviceListener() {
            // onOrientationData() is called whenever a Myo provides its current orientation,
// represented as a quaternion.
            @Override
            public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
// Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
                float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
                float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
                float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
// Adjust roll and pitch for the orientation of the Myo on the arm.

                //UnityPlayer.UnitySendMessage("MessageManager", "MyoRotation",  String.format("%f %f %f %f", rotation.x(), rotation.y(), rotation.z(), rotation.w()));
// Next, we send a rotation to the unity using the roll, pitch, and yaw.

                UnityPlayer.UnitySendMessage("MessageManager", "MyoRotation",  String.format("%f %f %f", roll, -pitch, yaw));
            }
            // onPose() is called whenever a Myo provides a new pose.
            @Override
            public void onPose(Myo myo, long timestamp, Pose pose) {
                String poseText = null;
// Handle the cases of the Pose enumeration, and change the text of the text view
// based on the pose we receive.
                switch (pose) {
                    case UNKNOWN:
                        break;
                    case REST:
                        poseText = "Rest";
                        break;
                    case DOUBLE_TAP:
                        poseText = "Tap";
                        break;
                    case FIST:
                        poseText = "Fist";
                        break;
                    case WAVE_IN:
                        poseText = "Wave_In";
                        break;
                    case WAVE_OUT:
                        poseText = "Wave_Out";
                        break;
                    case FINGERS_SPREAD:
                        poseText = "Spread";
                        break;
                }
                if (pose != Pose.UNKNOWN && pose != Pose.REST) {
// Tell the Myo to stay unlocked until told otherwise. We do that here so you can
// hold the poses without the Myo becoming locked.
                    myo.unlock(Myo.UnlockType.HOLD);
// Notify the Myo that the pose has resulted in an action, in this case changing
// the text on the screen. The Myo will vibrate.
                    myo.notifyUserAction();
                } else {
// Tell the Myo to stay unlocked only for a short period. This allows the Myo to
// stay unlocked while poses are being performed, but lock after inactivity.
                    myo.unlock(Myo.UnlockType.TIMED);
                }
                UnityPlayer.UnitySendMessage("MessageManager", "MyoPose",  poseText);
            }
            @Override
            public void onConnect(Myo myo, long timestamp) {
                Toast.makeText(UnityPlayerNativeActivity.this, "Myo Connected!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnect(Myo myo, long timestamp) {
                Toast.makeText(UnityPlayerNativeActivity.this, "Myo Disconnected!", Toast.LENGTH_SHORT).show();
            }
        };

        hub.addListener(mListener);

        hub.setLockingPolicy(Hub.LockingPolicy.NONE);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        super.onDestroy();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
    }

    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}
