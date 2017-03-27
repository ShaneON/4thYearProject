
#include <Arduino.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif
//#include <SPI.h>
#if not defined (_VARIANT_ARDUINO_DUE_X_) && not defined (_VARIANT_ARDUINO_ZERO_)
  #include <SoftwareSerial.h>
#endif

#define PIN            6

// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS      16

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
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

// Create the bluefruit object, either software serial...uncomment these lines

Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);


// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

void setup(void)
{
  //while (!Serial);  // required for Flora & Micro
  //delay(500);

  Serial.begin(115200);
  pixels.begin(); // This initializes the NeoPixel library.
  //Serial.println(F("Adafruit Bluefruit Command Mode Example"));
  //Serial.println(F("---------------------------------------"));

  /* Initialise the module */
  //Serial.print(F("Initialising the Bluefruit LE module: "));

  if ( !ble.begin(VERBOSE_MODE) )
  {
    error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
  }
  //Serial.println( F("OK!") );

  if( FACTORYRESET_ENABLE )
  {
    /* Perform a factory reset to make sure everything is in a known state */
    //Serial.println(F("Performing a factory reset: "));
    if(!ble.factoryReset()){
      //error(F("Couldn't factory reset"));
    }
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);

  //Serial.println("Requesting Bluefruit info:");
  /* Print Bluefruit information */
  ble.info();

  //Serial.println(F("Please use Adafruit Bluefruit LE app to connect in UART mode"));
  //Serial.println(F("Then Enter characters to send to Bluefruit"));
  //Serial.println();

  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while(! ble.isConnected()) {
      delay(500);
  }

  // LED Activity command is only supported from 0.6.6
  if( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
  {
    // Change Mode LED Activity
    //Serial.println(F("******************************"));
    //Serial.println(F("Change LED activity to " MODE_LED_BEHAVIOUR));
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
    //Serial.println(F("******************************"));
  }
}

void loop(void)
{
  // Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  ble.readline();
  ble.waitForOK();
  if(strcmp(ble.buffer, "OK") == 0) {
    // no data
    return;
  }
  if(ble.buffer[0] == 'L') {
    turnOnPixel(0);
    //Serial.println("You selected Left");
    // Send characters to Bluefruit
    ble.print("AT+BLEUARTTX=");
    ble.println("You selected Left");
    // check response status
    ble.waitForOK();
    delay(5000);
    turnOffPixel(0);
  }
  else if(ble.buffer[0] == 'R') {
    turnOnPixel(1);
    //Serial.println("You selected Right");
    // Send characters to Bluefruit
    ble.print("AT+BLEUARTTX=");
    ble.println("You selected Right");
    ble.waitForOK();
    delay(5000);
    turnOffPixel(1);
  }
  else{
    int i = 0;
    char latChar;
    String latString = "";
    while((latChar = ble.buffer[i++]) != NULL){
      latString = latString + latChar;
    }
    Serial.println("latString");
  }
}

void turnOnPixel(int pixel)
{
    pixels.setPixelColor(pixel, pixels.Color(200,200,200));
    pixels.show(); // This sends the updated pixel color to the hardware.
}

void turnOffPixel(int pixel)
{
    pixels.setPixelColor(pixel, pixels.Color(0,0,0)); 
    pixels.show(); // This sends the updated pixel color to the hardware.
 
}


