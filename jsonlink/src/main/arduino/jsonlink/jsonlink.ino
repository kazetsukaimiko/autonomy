#include <Arduino.h>

#include <EEPROM.h>
#include <ArduinoJson.h>
StaticJsonDocument<2048> inputDocument;
StaticJsonDocument<2048> outputDocument;
char NEWLINE = '\n';

char UUID_ADDRESS = 0;
// Board ID
String UUID = "uuid";
// Stuff to return
String READ = "read";
// Stuff to change
String WRITE = "write";
// Stuff to manage
String MODE = "mode";

// READ: Array of pinNum; WRITE: Array of { pinNum : boolean }
String DIGITAL = "digital";
// READ: Array of { pinNum : resistance }; WRITE: todo
String ANALOG = "analog";

// Shortcuts
String TURN_ON = "turn_on";
String TURN_OFF = "turn_off";

int ANALOG_PINS[] = {82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97};
int DIGITAL_PINS[] = {1,2,3,5,6,7,12,13,15,16,17,18,19,20,21,22,23,24,25,26,35,36,37,38,39,40,41,42,43,44,45,46,50,51,52,53,54,55,56,57,58,59,60,63,64,70,71,72,73,74,75,76,77,78};



// TODO: analog read pins
/*
void analogReadMode() {
  int analogPin= 0; // Which pin
  int raw= 0; // Raw read value
  int Vin= 5; // Voltage In
  float Vout= 0; // Voltage Drop across unknown resistor.
  float R1= 1000; // Known Resistor
  float R2= 0; //Resistance of unknown in ohms.
  float buffer= 0;

  raw = analogRead(analogPin);
  if(raw) {
    buffer= raw * Vin;
    Vout= (buffer)/1024.0;
    buffer= (Vin/Vout) -1;
    R2= R1 * buffer;
    Serial.print("Vout: ");
    Serial.println(Vout);
    Serial.print("R2: ");
    Serial.println(R2);
    delay(1000);
  }
}*/


void writeToEEPROM(char add,String data) {
  int _size = data.length();
  int i;
  for(i=0;i<_size;i++) {
    EEPROM.update(add+i,data[i]);
  }
  EEPROM.update(add+_size,'\0');   //Add termination null character for String Data
  //EEPROM.
}

String readFromEEPROM(char add) {
  int i;
  char data[100]; //Max 100 Bytes
  int len=0;
  unsigned char k;
  k=EEPROM.read(add);
  while(k != '\0' && len<500) {   //Read until null character
    k=EEPROM.read(add+len);
    data[len]=k;
    len++;
  }
  data[len]='\0';
  return String(data);
}

void setupUUID() {
    if (inputDocument.containsKey(UUID)) {
        String newUUID = inputDocument[UUID];
        writeToEEPROM(UUID_ADDRESS, newUUID);
    }
    String uuid = readFromEEPROM(UUID_ADDRESS);
    if (uuid.length() == 36) {
        outputDocument[UUID] = uuid;
    }
}

boolean validPin(int pinNum) {
  for(int pin : ANALOG_PINS) {
    if (pin == pinNum) {
      return true;
    }
  }
  for(int pin : DIGITAL_PINS) {
    if (pin == pinNum) {
      return true;
    }
  }
  return false;
}

boolean getPinMode(int pinNum) {
  uint8_t bit = digitalPinToBitMask(pinNum);
  uint8_t port = digitalPinToPort(pinNum);
  volatile uint8_t *reg = portModeRegister(port);
  // TRUE: output. FALSE: input.
  return (*reg & bit) ? OUTPUT : INPUT;
}

void modePins() {
  if (inputDocument.containsKey(MODE)) {
    JsonObject pinsToMode = inputDocument[MODE];
    for( JsonPair pinToMode : pinsToMode ) {
      int pinToModeNumber = atoi(pinToMode.key().c_str());
      bool value = pinToMode.value();
      pinMode(pinToModeNumber, value ? OUTPUT : INPUT);
      if (value) {
        digitalWrite(pinToModeNumber, HIGH);
      }
    }
  }
}

void writePins() {
  if (inputDocument.containsKey(WRITE)) {
    if (inputDocument[WRITE].containsKey(DIGITAL)) {
      JsonObject digitalPins = inputDocument[WRITE][DIGITAL];
      for( JsonPair digitalPin : digitalPins ) {
        int digitalPinNumber = atoi(digitalPin.key().c_str());
        bool value = digitalPin.value();
        digitalWrite(digitalPinNumber, value ? HIGH : LOW);
        outputDocument[DIGITAL][String(digitalPinNumber)] = value;
      }
    }
  }
  if (inputDocument.containsKey(TURN_ON)) {
    JsonArray pinsToTurnOn = inputDocument[TURN_ON];
    for (int pinToTurnOn : pinsToTurnOn) {
      digitalWrite(pinToTurnOn, LOW);
      outputDocument[DIGITAL][String(pinToTurnOn)] = true;
    }
  }

  if (inputDocument.containsKey(TURN_OFF)) {
    JsonArray pinsToTurnOff = inputDocument[TURN_OFF];
    for (int pinToTurnOff : pinsToTurnOff) {
      digitalWrite(pinToTurnOff, HIGH);
      outputDocument[DIGITAL][String(pinToTurnOff)] = false;
    }
  }
}

void readPins() {
  if (inputDocument.containsKey(READ)) {
    if (inputDocument[READ].containsKey(DIGITAL)) {
      JsonArray digitalPins = inputDocument[READ][DIGITAL];
      for( const int& digitalPin : digitalPins ) {
        outputDocument[DIGITAL][String(digitalPin)] = (digitalRead(digitalPin) == HIGH);
      }
    }
  }
}


void processJson() {
  setupUUID();
  modePins();
  writePins();
  readPins();
}

void loop() {
  // Reset the output
  deserializeJson(outputDocument, "{}");
  // If no error, process
  if (!deserializeJson(inputDocument, Serial.readStringUntil(NEWLINE))) {
    processJson();
    serializeJson(outputDocument, Serial);
    Serial.write(NEWLINE);
  }
}


void setup() {
  // Initialize Serial port
  Serial.begin(115200);
  Serial.setTimeout(50);
}
