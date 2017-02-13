#include <Adafruit_NeoPixel.h>

int pin  = 6;
int nLEDs = 20;
int incomingByte = 0;
int iter = 0;
int acc = 0;
int data = 7;

Adafruit_NeoPixel strip = Adafruit_NeoPixel(nLEDs, pin, NEO_GRB + NEO_KHZ800);

void setup() {
  Serial.begin(115200);
  strip.begin();
  strip.show();
}

void loop() {
  if (Serial.available() > 0) {
    incomingByte = Serial.read();
    Serial.println(incomingByte, DEC);
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
    case 1:
    staticColor(strip.Color(0, 0, 0));
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


