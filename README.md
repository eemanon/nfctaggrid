# nfctaggrid
An application to allow device tracking on a surface based on NFC tags and the device accelerometer.

## Code structure
All relevant code resides within the class "MainActivity.java" in app/src/main/java/com/example/nfctaggrid. "Accelerometer.java" is for debugging only.

## Algorithm
Initially, the user scans a special NFC tag containing the name of the map to display. The 
boolean "mapreadmode" tracks if the app is still in this initial phase and is set to false (line 277) when a tag containing this information has been scanned.  

Subsequently, every tag scanned is read as a position (line 278 and following) and the map is moved to the scanned position. The position within the tag is encoded as plain text as "x|y" where x and y are the coordinates as floating numbers.  

The user now has to "calibrate" its accelerometer by hitting the "Calibrate" button. During calibration, every detected acceleration increases a threshold (in the variable "thresholds").
Calibration usually takes only some seconds. When the user hits the button again, the recorded
threshold is used to ignore noise coming from the accelerometer and only use acceleration that
is beyond the recorded threshold.  

Now, whenever the threshold is exceeded, the highest recorded acceleration value within a timeout
of a fixed number of measurements ("bufferlength") is used to build a direction vector (line 174 and following). The higher and longer the acceleration, the longer the vector. This is based on the assumption that high initial acceleration leads to faster overall movement.  

Once this vector is obtained, the map is moved accordingly to the vector length and direction within a timeout (line 184 and following). The timeout is extended every time an acceleration is detected. This is based on the observation that moving a device over a flat surface is no regular process and goes along with a number of significant accelerations and decelerations.  

When the user stops moving the device, the timeout ends and the "mode" is set to "normal" - meaning that the device is not moved.  

All accelerometer values are smoothed by a moving average/low pass filter ("samplesX", "samplesY", "samplesZ").  

## Limitations
On the used device (Fairphone 3) NFC detection triggers a vibration alarm that extends the accelerometer leading to chaotic behaviour. There have also been very little testing as to the
parameters like buffer length and such.
Currently, only the Fairphone 3 can be used with this code, since screen dimensions vary from phone to phone and so does the position of the NFC chip within the phone ("offsetX", "offsetY", "scaleValue").

## Code quality and optimisations
This is a first prototype. Code should be separated in classes, threads and functions.
