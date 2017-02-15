#include <Adafruit_NeoPixel.h>

int pin  = 6;
int nLEDs = 20;
int incomingByte = 0;
int iter = 0;
int acc = 0;
int data = 255;

Adafruit_NeoPixel strip = Adafruit_NeoPixel(nLEDs, pin, NEO_GRB + NEO_KHZ800);

void setup() {
  Serial.begin(9600);
  strip.begin();
  strip.show();
}

void loop() {
  if (Serial.available() > 0) {
    incomingByte = Serial.read();
    //Serial.println(incomingByte, DEC);
    data = incomingByte;
  }
  doEffect(data);
}

boolean undo = false;
void doEffect(int integer){
  switch(integer){
    default:
    staticColor(strip.Color(255, 255, 0));
    break;
    
    case 1:
    staticColor(strip.Color(0, 0, 0));
    break;
    
    case 2:
    staticColor(strip.Color(255, 0, 255));
    break;
    
    case 3:
    staticColor(strip.Color(0, 255, 255));
    break;
    
    case 4:
    staticColor(strip.Color(255, 0, 0));
    break;
    
    case 5:
    staticColor(strip.Color(0, 255, 0));
    break;
    
    case 6:
    staticColor(strip.Color(0, 0, 255));
    break;
    
    case 7:
    rainbowCycle(20);
    break;
    
    case 8:
    if(undo){
      colorWipe(strip.Color(0, 0, 0), 50);
    }else{
      colorWipe(strip.Color(0, 0, 255), 50);
    }
    break;
    
    case 9:
    if(undo){
      colorWipe(strip.Color(0, 0, 0), 50);
    }else{
      colorWipe(strip.Color(0, 255, 0), 50);
    }
    break;
    
    case 10:
    if(undo){
      colorWipe(strip.Color(0, 0, 0), 50);
    }else{
      colorWipe(strip.Color(255, 0, 0), 50);
    }
    break;
    
    case 11:
    flash(strip.Color(255, 255, 255));
    break;
    
    case 12:
    flash(strip.Color(255, 0, 0));
    break;
    
    case 13:
    flash(strip.Color(0, 255, 0));
    break;
    
    case 14:
    flash(strip.Color(0, 0, 255));
    break;
    
    case 15:
    rainbowFlash(30);
    break;

    case 16:
    theaterChaseRainbow(50);
    break;
    
    case 255:
    rainbowFlash(1000);
    break;
    }
  }

void staticColor(uint32_t c){
  for(int i = 0; i < strip.numPixels(); i++){
    strip.setPixelColor(i, c);
  }
  strip.show();
}

uint16_t colorWipeIter = 0;
void colorWipe(uint32_t c, uint8_t wait){
  if(colorWipeIter > strip.numPixels()){
    colorWipeIter = 0;
    undo = !undo;
  }else{
  strip.setPixelColor(colorWipeIter, c);
  strip.show();
  delay(wait);
  colorWipeIter++;
  }
}

uint16_t rainbowIter = 0;
void rainbowCycle(uint32_t wait) {
  uint16_t i;
  
  for(i=0; i< strip.numPixels(); i++) {
      strip.setPixelColor(i, Wheel(((i * 256 / strip.numPixels()) + rainbowIter) & 255));
  }
  rainbowIter++;
  delay(wait);
  strip.show();
}
boolean yes = false;
void flash(uint32_t c){
  if(yes){
    for(int i = 0; i < strip.numPixels(); i++){
      strip.setPixelColor(i, c);
    }
  }else{
    for(int i = 0; i < strip.numPixels(); i++){
      strip.setPixelColor(i, strip.Color(0, 0, 0));
    }
  }
  yes = !yes;
  strip.show();
  delay(20);
}

void rainbowFlash(uint32_t wait){
  uint16_t i;
  if(yes){
    for(i=0; i< strip.numPixels(); i++) {
        strip.setPixelColor(i, Wheel(((i * 256 / strip.numPixels()) + rainbowIter) & 255));
    }
    rainbowIter++;
  }else{
    for(int i = 0; i < strip.numPixels(); i++){
      strip.setPixelColor(i, strip.Color(0, 0, 0));
    }
  }
  yes = !yes;
  delay(wait);
  strip.show();
}
int chaseRainbowIter = 0;
void theaterChaseRainbow(uint8_t wait) {
    for (int q=0; q < 3; q++) {
      for (uint16_t i=0; i < strip.numPixels(); i=i+3) {
        strip.setPixelColor(i+q, Wheel( (i+chaseRainbowIter) % 255));    //turn every third pixel on
      }
      strip.show();
      delay(wait);

      for (uint16_t i=0; i < strip.numPixels(); i=i+3) {
        strip.setPixelColor(i+q, 0);        //turn every third pixel off
      }
    }
  chaseRainbowIter++;
}

uint32_t Wheel(byte WheelPos) {
  WheelPos = 255 - WheelPos;
  if(WheelPos < 85) {
    return strip.Color(255 - WheelPos * 3, 0, WheelPos * 3);
  }
  if(WheelPos < 170) {
    WheelPos -= 85;
    return strip.Color(0, WheelPos * 3, 255 - WheelPos * 3);
  }
  WheelPos -= 170;
  return strip.Color(WheelPos * 3, 255 - WheelPos * 3, 0);
}


