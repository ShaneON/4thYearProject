//This program tests that when the direction commands are sent to the flora, the appropriate
//neopixel turns on or off. 
//If "lft" is received, the left neopixel should light up. 
//If "rgt" is received, the right neopixel should light up.
//If "off" is received, the neopixels should be turned off. 

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
//Initialize Bluefruit object
Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);
//Initialize Neopixel object
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

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
  
  if ( !ble.begin(VERBOSE_MODE) )
    error(F("Couldn't find Bluefruit make sure it's in CoMmanD mode & check wiring?"));
  if( FACTORYRESET_ENABLE ){
    /* Perform a factory reset to make sure everything is in a known state */
    if(!ble.factoryReset())
      error(F("Couldn't factory reset"));
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);
  ble.info();
  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while(!ble.isConnected()) {
    Serial.println(F("bluetooth not connected"));
    delay(500);
  }
  
  if(ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION))
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
 
  //This is the first main loop. This loop will continue
  //as long as the app is tethered to the flora
  while(ble.isConnected()){
    tetheredFunc();
  }

}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void loop(void){
}

//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------

void tetheredFunc(){
  //Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  ble.readline();
  ble.waitForOK();

  char bufferData[21];
  readFromBuffer(bufferData);

  if(bufferData[0] != 'O'){
    Serial.print(F("BufferData: "));
    Serial.println(bufferData);
    processData(bufferData);
  }
}

void processData(char data[]){
  if(condition(data, 'l', 'f', 't')) leftTurnTethered();
  else if(condition(data, 'r', 'g', 't')) rightTurnTethered();
  else if(condition(data, 'o', 'f', 'f')) turnOffLights();
}

void turnOffLights(){
  changePixel(0, 0, 0, 0);
  changePixel(1, 0, 0, 0);
}

void leftTurnTethered(){
  changePixel(1, 255, 255, 255);
}

void rightTurnTethered(){
  changePixel(0, 255, 255, 255);
}

//Reads data from bluetooth buffer as cstring
void readFromBuffer(char bufferData[]){
  int i = 0;
  char bufferValue;
  while((bufferValue = ble.buffer[i]) != NULL)
    bufferData[i++] = bufferValue;
  bufferData[i] = '\0';
}

void sendConfirmation(){
  // Send characters to Bluefruit
  ble.print("AT+BLEUARTTX=");
  ble.println('0');
  // check response status
  ble.waitForOK();
}

boolean condition(char bufferData[], char zero, char one, char two){
  return bufferData[0] == zero && 
          bufferData[1] == one && 
          bufferData[2] == two; 
}


//Set chosen pixel color/brightness
void changePixel(int pixel, int r, int g, int b){
    pixels.setPixelColor(pixel, pixels.Color(r, g, b));
    pixels.show(); //This sends the updated pixel color to the hardware.
}




