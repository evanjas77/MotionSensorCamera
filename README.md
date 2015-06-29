# MotionSensorCamera
An android camera in combination with an Arduino PIR sensor wired together using IBM Bluemix platform. The camera activates the shutter whenever a motion is detected.

# Why did I develop this app?
The Iot app was primarily developed to capture photographs of birds (could be squirells, rodents etc !!) visiting the gardern bird feeder. A Iot PIR sensor detects any motion when the bird lands on the feeder, sends a push notification to an android app to activate the phone's camera shutter and click the image of the birds. The photograph of the bird is stored in the same phone wich syncs and uploads the photo to the cloud.
However we can also extend the use of this app as a security camera.

# What are the app modules?
The Iot App is made up of 3 modules
1. A PIR Sensor connected to Arduino UNO interfaced with Ethernet/Wifi shield.
2.  The Bluemix cloud (InternetOfThings, Node-RED, Push, Mobile security services)
3.  An android app which activates the camera shutter and perform a click whenever the push notification is received.

# Description of the files in this project
1. PIRSensor_Arduino_MQTT_Bluemix.ino:
  This is the Arduino sketch which reads the PIR sensor output and publishes it to the IBM cloud IoT topic.
2. ArduinoPIRSensor_To_Android_Flow.json:
  This is the Bluemix Node-RED flow object which wires the Iot device and the Ardroid device running the app through push notification.
3. Rest of the files:
  These files form the Ardroid camera app which triggers the camera shutter upon receiving the push notification from Node-RED.
  
# Development tools used 
1. Eclipse IDE (ADT) / Android Studio
2. Arduino 1.6.0 sketch IDE
3. Node-RED flow editor

# Tested using
1. Arduino UNO R2 + Ethernet shield
2. HC-SR501 PIR motion sensor
3. Android phone running lollipop 5.0
4. IBM Bluemix cloud platform (InternetOfThings, Node-RED, Push, Mobile security services)

# A look at the complete setup
Detailed information on how the complete project was built can be found at the following link:
<br/>Link To be updated
