/***
  Copyright (c) 2013 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleCameraHost implements CameraHost {
  private static final String[] SCAN_TYPES= { "image/jpeg" };
  private Context ctxt=null;

  public SimpleCameraHost(Context _ctxt) {
    this.ctxt=_ctxt.getApplicationContext();
  }

  @Override
  public Camera.Parameters adjustPictureParameters(Camera.Parameters parameters) {
    return(parameters);
  }

  @Override
  public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
    return(parameters);
  }

  @Override
  public void configureRecorderAudio(int cameraId,
                                     MediaRecorder recorder) {
    recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
  }

  @Override
  public void configureRecorderOutput(int cameraId,
                                      MediaRecorder recorder) {
    recorder.setOutputFile(getVideoPath().getAbsolutePath());
  }

  @Override
  public void configureRecorderProfile(int cameraId,
                                       MediaRecorder recorder) {
    if (CamcorderProfile.hasProfile(cameraId,
                                    CamcorderProfile.QUALITY_HIGH)) {
      recorder.setProfile(CamcorderProfile.get(cameraId,
                                               CamcorderProfile.QUALITY_HIGH));
    }
    else if (CamcorderProfile.hasProfile(cameraId,
                                        CamcorderProfile.QUALITY_LOW)) {
      recorder.setProfile(CamcorderProfile.get(cameraId,
                                               CamcorderProfile.QUALITY_LOW));
    }
    else {
      throw new IllegalStateException("cannot find valid CamcorderProfile");
    }
  }

  @Override
  public int getCameraId() {
    int count=Camera.getNumberOfCameras();
    Camera.CameraInfo info=new Camera.CameraInfo();

    for (int i=0; i < count; i++) {
      Camera.getCameraInfo(i, info);

      if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK
          && !useFrontFacingCamera()) {
        return(i);
      }
      else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
          && useFrontFacingCamera()) {
        return(i);
      }
    }

    return(0);
  }

  @Override
  public DeviceProfile getDeviceProfile() {
    return(DeviceProfile.getInstance());
  }

  @Override
  public Camera.Size getPictureSize(Camera.Parameters parameters) {
    return(CameraUtils.getLargestPictureSize(parameters));
  }

  @Override
  public Camera.Size getPreviewSize(int displayOrientation, int width,
                                    int height,
                                    Camera.Parameters parameters) {
    Camera.Size result=null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
        && mayUseForVideo()) {
      result=parameters.getPreferredPreviewSizeForVideo();
    }

    if (result == null) {
      result=
          CameraUtils.getBestAspectPreviewSize(displayOrientation,
                                               width, height,
                                               parameters);
    }

    return(result);
  }

  @Override
  public Camera.ShutterCallback getShutterCallback() {
    return(null);
  }

  @Override
  public void handleException(Exception e) {
    Log.e(getClass().getSimpleName(),
          "Exception in setPreviewDisplay()", e);
  }

  @Override
  public boolean mirrorFFC() {
    return(false);
  }

  @Override
  public void saveImage(Bitmap bitmap) {
    // no-op
  }

  @Override
  public void saveImage(byte[] image) {
    File photo=getPhotoPath();

    if (photo.exists()) {
      photo.delete();
    }

    try {
      FileOutputStream fos=new FileOutputStream(photo.getPath());
      BufferedOutputStream bos=new BufferedOutputStream(fos);

      bos.write(image);
      bos.flush();
      fos.getFD().sync();
      bos.close();

      if (scanSavedImage()) {
        MediaScannerConnection.scanFile(ctxt,
                                        new String[] { photo.getPath() },
                                        SCAN_TYPES, null);
      }
    }
    catch (java.io.IOException e) {
      handleException(e);
    }
  }

  protected File getPhotoPath() {
    File dir=getPhotoDirectory();

    dir.mkdirs();

    return(new File(dir, getPhotoFilename()));
  }

  protected File getPhotoDirectory() {
    return(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
  }

  protected String getPhotoFilename() {
    String ts=
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

    return("Photo_" + ts + ".jpg");
  }

  protected File getVideoPath() {
    File dir=getVideoDirectory();

    dir.mkdirs();

    return(new File(dir, getVideoFilename()));
  }

  protected File getVideoDirectory() {
    return(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
  }

  protected String getVideoFilename() {
    String ts=
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

    return("Video_" + ts + ".mp4");
  }

  protected boolean mayUseForVideo() {
    return(true);
  }

  protected boolean useFrontFacingCamera() {
    return(false);
  }

  protected boolean scanSavedImage() {
    return(true);
  }
}
