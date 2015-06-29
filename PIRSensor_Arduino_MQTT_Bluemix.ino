/*
This sample code interacts with a PIR Sensor, and switched ON/OFF an LED connected to arduino whenever motion is detected.
The code also publishes to a Internet Of Things topic on the ibm cloud.
*/

#include <SPI.h>
#include <Ethernet.h>
#include <PubSubClient.h>

// Update these with values suitable for your network.
byte mac[]    = {  0xDE, 0xED, 0xBA, 0xFE, 0xFE, 0xEF};
String macString = "deedbafefeef";
char servername[] = "<deviceId>.messaging.internetofthings.ibmcloud.com";
//char servername[] = "messaging.quickstart.internetofthings.ibmcloud.com";
byte ip[]     = { 192, 168, 1, 49 };
String clientName = String("d:<deviceId>:<deviceType>:") + macString;
//String clientName = String("d:quickstart:arduino:") + macstr;
String topicName = String("iot-2/evt/status/fmt/json");
String usernameStr = "use-token-auth";
String passwordStr = "<generated password when device is registered in bluemix Iot service>";
int motionDetectedAt = 0;
int motionEndedAt = 0;
EthernetClient ethClient;

//the time we give the sensor to calibrate (10-60 secs according to the datasheet)
int calibrationTime = 30;        

//the time when the sensor outputs a low impulse
long unsigned int lowIn;         

//the amount of milliseconds the sensor has to be low 
//before we assume all motion has stopped
long unsigned int pause = 5000;  

boolean lockLow = true;
boolean takeLowTime;  
int pirPin = 3;    //the digital pin connected to the PIR sensor's output
int ledPin = 13;

PubSubClient client(servername, 1883, 0, ethClient);

void setup()
{
  
  Ethernet.begin(mac, ip);
  Serial.begin(9600);
  pinMode(pirPin, INPUT);
  pinMode(ledPin, OUTPUT);
  digitalWrite(pirPin, LOW);
  
  //give the sensor some time to calibrate
  Serial.print("calibrating sensor ");
  for(int i = 0; i < calibrationTime; i++){
      Serial.print(".");
      delay(1000);
   }
  Serial.println(" done");
  Serial.println("SENSOR ACTIVE");
  delay(50);
}

void loop()
{
  char clientStr[50];
  clientName.toCharArray(clientStr,50);
  char topicStr[26];
  topicName.toCharArray(topicStr,26);
  char username[50];
  usernameStr.toCharArray(username,50);
  char password[100];
  passwordStr.toCharArray(password,100);
  char macstr[50];
  macString.toCharArray(macstr,50);
  
  if (!client.connected()) {
    Serial.println("Device (" + clientName + ") trying to attempt connection with bluemix");
    client.connect(clientStr,username,password);
    //client.connect(clientStr);
    Serial.println("Connection established with bluemix");
  }
  
  if(digitalRead(pirPin) == HIGH){
       digitalWrite(ledPin, HIGH);   //the led visualizes the sensors output pin state
       if(lockLow){  
         //makes sure we wait for a transition to LOW before any further output is made:
         lockLow = false;     
         motionDetectedAt = millis()/1000;
         String json = buildJson();
         char jsonStr[200];
         json.toCharArray(jsonStr,200);
         
         Serial.print("attempt to send ");
         Serial.println(jsonStr);
         Serial.print("to ");
         Serial.println(topicStr);   
         boolean pubresult =false;    
         if (client.connected() ) {
           pubresult = client.publish(topicStr,jsonStr);
         }
         if (pubresult)
          Serial.println("Message publish success");
         else
          Serial.println("Message publish failed");
         delay(50);    
       }
         takeLowTime = true;
     }

     if(digitalRead(pirPin) == LOW){       
       digitalWrite(ledPin, LOW);  //the led visualizes the sensors output pin state

       if(takeLowTime){
        lowIn = millis();          //save the time of the transition from high to LOW
        takeLowTime = false;       //make sure this is only done at the start of a LOW phase
        }
       //if the sensor is low for more than the given pause, 
       //we assume that no more motion is going to happen
       if(!lockLow && millis() - lowIn > pause){  
           //makes sure this block of code is only executed again after 
           //a new motion sequence has been detected
           lockLow = true;  
           motionEndedAt = (millis() - pause)/1000;           
           Serial.print("motion ended at ");      //output
           Serial.print(motionEndedAt);
           Serial.println(" sec");
           delay(50);
         }
     }
       
  delay(5000);
}

String buildJson() {
  String data = "{";
  data+="\n";
  data+= "\"d\": {";
  data+="\n";
  data+="\"myName\": \"Arduino PIRSensor\",";
  data+="\n";
  data+="\"motionDetectedAt\": \"";
  data+=motionDetectedAt;
  data+="s\"";
  data+="\n";
  data+="}";
  data+="\n";
  data+="}";
  return data;
}

