# Face Tracking using OpenCV, a Kalman Filter, and Assignment Algorithms

## Objective

#### Detect and track a faces in a live feed.

## Code and Files 

#### 1. My project includes the following files
* [FaceTrackMain.java](FaceTrackMain.java) is the main code
* [ImageConverter.java](ImageConverter.java) is the image conversion class that converts the opencv Mat data to image data.
* [KalmanFilter.java](KalmanFilter.java) is the Kalman Filter class that makes predictions (outputs) based on the current & past inputs.
* [Tracks.java](Tracks.java) is the Track class which holds all the properties of a track as if goes from frame to frame
* [Webcam.java](Webcam.java) is the Webcam class that allows us to pull real time data from the webcam 
* [CVHungarianAssignment.java](CVHungarianAssignment.java) is the Hungarian Assignment class that matches the tracks to their respective detected faces