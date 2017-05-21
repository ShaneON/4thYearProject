//This program tests that when the GPS retreives a fix the neopixels flash orange to inform
//the user.
#include <Adafruit_GPS.h>
#include <Arduino.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

//Pin used for neopixel data
#define PIN            6
//Number of pixels being used
#define NUMPIXELS      2
#define GPSECHO false

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_UART.h"
#include "BluefruitConfig.h"

    #define FACTORYRESET_ENABLE         1
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
    #define BLUEFRUIT_HWSERIAL_NAME          Serial1
    #define BLUEFRUIT_UART_MODE_PIN         -1   // Not used with FLORA
    #define BLUEFRUIT_UART_CTS_PIN          -1   // Not used with FLORA
    #define BLUEFRUIT_UART_RTS_PIN          -1   // Not used with FLORA
/*=========================================================================*/
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);
#include <SoftwareSerial.h>
//Set the pins for RX/TX connections to GPS
SoftwareSerial mySerial(A9, A10);
//Initialize GPS object
Adafruit_GPS GPS(&mySerial);

unsigned long previousMillisLights = 0;
unsigned long flashInterval = 0;
boolean firstFix = false;
boolean lightOn = false;
uint8_t red = 0;
uint8_t green = 0;
uint8_t blue = 0;
uint8_t flashes = 0;
uint8_t flashNum = 0;
float oldTarget = 0.0;
float oldGpsLat = 0.0;
float oldGpsLng = 0.0;
float targetDistance;

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void setup(void){
  Serial.begin(115200);
  pixels.begin();

  //Make sure pixels are set to of from start
  changePixel(0, 0, 0, 0);
  changePixel(1, 0, 0, 0);
  
  //Sets the variables that the interrupt uses to flash the lights.
  //If the bluetooth becomes disconnected the lights will flash purple
  setFlashVariables(128, 0 ,128, 10, 0, 300, false, 0, 1);

  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
  delay(1000);
  Serial.println(PMTK_Q_RELEASE);

}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void loop(void){
  
  //Timer interrupt which flashes the lights
  flashingSignal();
  //Function to read from GPS
  readFromGPSModule();
  
}

//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------

void readFromGPSModule(){
  char c = GPS.read();
  if (GPSECHO)
      if(c) Serial.print(c);
  if (GPS.newNMEAreceived()) 
    if (!GPS.parse(GPS.lastNMEA())) 
      return; 
  if (GPS.fix) {
    //flash orange for gps ready
    if(!firstFix){
      firstFix = true;
      setFlashVariables(255, 69, 0, 10, 0, 100, false, 0, 1);
    }
    
    float gpsLat = latLngToDecimal(GPS.latitude, GPS.lat);
    float gpsLng = latLngToDecimal(GPS.longitude, GPS.lon);
    float targetLat = latLngArray[latLngArrayPointer];
    float targetLng = latLngArray[latLngArrayPointer + 1];
    // was cast as a double
    targetDistance = calculateLatLngDist(gpsLat, gpsLng, targetLat, targetLng);
    
    if(oldGpsLat != gpsLat || oldGpsLng != gpsLng){
      oldGpsLat = gpsLat;
      oldGpsLng = gpsLng;
      Serial.print(F("Got a GPS fix: "));
      Serial.print(gpsLat, 6);
      Serial.print(F(", "));
      Serial.println(gpsLng, 6);
    }
    
    if(targetDistance != oldTarget){
      oldTarget = targetDistance;
      Serial.print("Distance to target = ");
      Serial.println(targetDistance);
    }
    
    if(targetDistance < 30.0){
      printTurnDetails(targetLat, targetLng, targetDistance);
      onApproachingTarget();          
    }
  }
}

//Timer interrupt to flash neopixels
void flashingSignal(){
  //Get the amount of milliseconds since the program started
  unsigned long currentMillis = millis();
  
  //Every n milliseconds (dictated by flashinterval) 
  //the interrupt will fire
  if((currentMillis - previousMillisLights > flashInterval)){
    
    //While there are flashes left of still waiting for 
    //latlng array to fill up
    if(flashNum < flashes || waitingForJob){
      flashNum++;
      previousMillisLights = currentMillis;
      if(!lightOn){
        //If lights are off, turn them on
        changePixel(left, red, green, blue);
        changePixel(right, red, green, blue);
        lightOn = true;
      }
      else{
        //If lights are on, turn them off
        changePixel(0, 0, 0, 0);
        changePixel(1, 0, 0, 0);
        lightOn = false;
      }
    }
    //When flashNum reaches the specified flashes, set
    //the flashVariables and pixels back to zero
    else if(flashNum > 0 && flashNum == flashes){
      setFlashVariables(0, 0, 0, 0, 0, 0, false, 0, 0);
      changePixel(0, 0, 0, 0);
      changePixel(1, 0, 0, 0);
    }
  }
}

void setFlashVariables(uint8_t r, uint8_t g, uint8_t b, 
            uint8_t f, uint8_t fNum, unsigned long fI, boolean wFJ,
            uint8_t l, uint8_t ri){
   red = r;
   green = g;
   blue = b;
   flashes = f;
   flashNum = fNum;
   flashInterval = fI;
   waitingForJob = wFJ;
   left = l;
   right = ri;
}

boolean condition(char bufferData[], char zero, char one, char two){
  return bufferData[0] == zero && 
          bufferData[1] == one && 
          bufferData[2] == two; 
}

// Convert NMEA coordinate to decimal degrees
float latLngToDecimal(float nmeaCoord, char dir) {
  uint16_t wholeDegrees = 0.01 * nmeaCoord;
  int modifier = 1;
 
  if (dir == 'W' || dir == 'S') modifier = -1;
 
  return (wholeDegrees + (nmeaCoord - 100.0 * wholeDegrees) / 60.0) * modifier;
}

//Set chosen pixel color/brightness
void changePixel(int pixel, int r, int g, int b){
    pixels.setPixelColor(pixel, pixels.Color(r, g, b));
    pixels.show(); //This sends the updated pixel color to the hardware.
}




