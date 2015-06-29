# MotionSensorCamera
An android camera in combination with an Arduino PIR sensor wired together using IBM Bluemix platform. The camera activates the shutter whenever a motion is detected.

# Why did I develop this app?
The Iot app was primarily developed to capture photographs of birds (could be squirells, rodents etc !!) visiting the garden bird feeder. The Iot PIR sensor detects any motion when birds land on the feeder, sends a push notification to an android app to activate the phone's camera shutter and click an image of the bird(s). The photograph of the bird is stored in the same phone which syncs and uploads the photo to the cloud. We could however extend the use of this app in other ways such as a security camera.

# What are the app modules?
The Iot App is made up of 3 modules
<br/>1. A PIR Sensor connected to Arduino UNO interfaced with Ethernet/Wifi shield.
<br/>2. The Bluemix cloud (InternetOfThings, Node-RED, Push, Mobile security services)
<br/>3. An android app which activates the camera shutter and perform a click whenever the push notification is received.

# Description of the files in this project
1. PIRSensor_Arduino_MQTT_Bluemix.ino:
  This is the Arduino sketch which reads the PIR sensor output and publishes it to the IBM cloud IoT topic. 
<u>Note</u>: You will need to provide appropriate values for mac address, ip address, device ID, device type, device-cloud connection password etc in the declaration section of this file for the sketch to work. 
2. ArduinoPIRSensor_To_Android_Flow.json:
  This is the Bluemix Node-RED flow object which wires the Iot device and the Ardroid device running the app through push notification. 
<u>Note</u>: After importing this file in node-RED flow editor, the application secret has to be manually updated in the IBM push node.
3. Rest of the files:
  These files form the Ardroid camera app which triggers the camera shutter upon receiving the push notification from Node-RED.
<u>Note</u>:In assets/bluelist.properties file, appropriate mobile cloud related values need to supplied.
  
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
