int pins[3] = {9, 10, 11};
int incomingByte = 0;
int iter = 0;
int acc = 0;
int rgb[3] = {0, 0, 0};
void setup() {
  Serial.begin(115200);
  pinMode(pins[0], OUTPUT);
  pinMode(pins[1], OUTPUT);
  pinMode(pins[2], OUTPUT);
}

void loop() {
  //Serial.println("hi");
  //delay(1);
  // send data only when you receive data:
  if (Serial.available() > 0) {
  // read the incoming byte:
    incomingByte = Serial.read();
  // say what you got:
  if(incomingByte - 48 < 10 && incomingByte - 48 >= 0){
    Serial.print("I received: ");
    Serial.println(incomingByte - 48, DEC);
    iter++;
    switch(iter % 3){
      case 0:
      acc += (incomingByte -48);
      rgb[(iter / 3) - 1] = acc;
      acc = 0;
      if(iter == 9){
        iter = 0;
      }
      break;
      case 1:
      acc += (incomingByte - 48) * 100;
      break;
      case 2:
      acc += (incomingByte - 48) * 10;
      break;
    }
  }
  }
  analogWrite(pins[0], 255 - rgb[0]);
  analogWrite(pins[1], 255 - rgb[1]);
  analogWrite(pins[2], 255 - rgb[2]);
}


