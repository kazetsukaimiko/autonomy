#include <Arduino.h>
#include <EEPROM.h>
#include <ArduinoJson.h>

/*
 * JSONLink for Arduino Mega2560
 * This sketch speaks a protocol I call "jsonlink" over serial. It allows you
 * to set pin modes, read and write from digital pins, and read analog pin values
 * via JSON commands.
 *
 * The idea here is that because the microcontroller requires flashing, it is
 * impossible to reconfigure it through user input. By boiling its usefulness down
 * to its I/O and leaving more complex decision logic to a general purpose computer,
 * you can reconfigure behavior based on user settings rather than hard-coded values,
 * or relying on state within the microcontroller itself.
 *
 */

/*
 * CONSTANTS - general
 */
// A convenience newline character.
static char NEWLINE = '\n';
// Where to find the UUID of the board in eeprom.
static char UUID_ADDRESS = 0;
// How long the UUID is (String)
static int UUID_LENGTH = 64;
// Version of JSONLink
int JSONLINK_VERSION[] = {1,0,0};


/*
 * CONSTANTS - jsonlink properties
 */

// Json property to read the version from.
static String VERSION = "version";

// Json property to request debug information with. Set to true if you want debugging output.
// Will return an array of the same name containing strings of debugging information.
static String DEBUG = "debug";

// Json property containing errors.
static String ERROR = "error";

// Json property containing the Request ID. This allows you to pool responses- critically important in
// multithreaded environments and asynchronous operations. Whatever you send as the request id is returned
// in the corresponding response.
static String REQUEST_ID = "requestId";

// Json property containing the Board ID. This allows you to differentiate between multiple boards
// connected to the same machine, regardless of connection order or other factors.
static String UUID = "uuid";

// Json property requesting the state of digital/analog pins.
static String READ = "read";

// Json property to write digital pin states to.
static String WRITE = "write";

// Json property to set a pins mode (read/write).
static String MODE = "mode";

// A Json property under READ: Array of pinNum; WRITE: Array of { pinNum : boolean }
static String DIGITAL = "digital";

// A Json property under READ: Array of { pinNum : resistance }; WRITE: todo
static String ANALOG = "analog";

// Shortcut Json properties to set pin states for digital pins set to write mode.
static String TURN_ON = "turn_on";
static String TURN_OFF = "turn_off";

// A response property containing the raw value of analog read.
static String RAW = "raw";

// A response property specifying a pin number.
static String PIN = "pin";

// A response property specifying the voltage for an analog read.
static String VOLTAGE = "voltage";

// A response property specifying the resistance.
static String RESISTANCE = "resistance";

/*
 * Critical Constants for Serial
 */

// The speed of the connection.
const long BAUD = 500000;
// The size of the StaticJsonDocuments to allocate.
const int JSONSIZE = 2048;
// The timeout value for serial- at maximum size, how long to wait for serial data.
// This is so that the controller is as responsive as possible.
static long TIMEOUT = (JSONSIZE*1.5)/(BAUD/1000);

// The document received by serial.
StaticJsonDocument<JSONSIZE> inputDocument;
// The document this sketch populates to write to serial.
StaticJsonDocument<JSONSIZE> outputDocument;



// Known "good" pins for the Arduino Mega2560.
// Some of these are probably incorrect.
int ANALOG_PINS[] = {A0,A1,A2,A3,A4,A5,A6,A7,A8,A9};
int DIGITAL_PINS[] = {1,2,3,5,6,7,12,13,15,16,17,18,19,20,21,22,23,24,25,26,35,36,37,38,39,40,41,42,43,44,45,46,50,51,52,53,54,55,56,57,58,59,60,63,64,70,71,72,73,74,75,76,77,78};

// Buffer for reading from Serial.
String buffer = "";


// Reads an analog pin using resistance given :
// The (int) pin number,
// The (float) voltage input,
// The (float) ohms value of the known resistor connected.
void readAnalogPin(int analogPinRead, float voltageIn, float knownResistance) {
  int raw = analogRead(analogPinRead);
  if (raw) {
    float buffer = raw * voltageIn;
    float voltageOut = (buffer)/1024.0;
    buffer = (voltageIn/voltageOut) - 1;
    float unknownResistance = knownResistance * buffer;

    if (!outputDocument.containsKey(ANALOG)) {
      outputDocument.createNestedArray(ANALOG);
    }
    JsonObject response = outputDocument[ANALOG].createNestedObject();
    response[PIN] = analogPinRead;
    response[RAW] = raw;
    response[VOLTAGE] = voltageOut;
    response[RESISTANCE] = unknownResistance;
  }
}

// Convenience function to write a string to the debug property.
void debug(String debug) {
    if (inputDocument.containsKey(DEBUG)) {
        if (!outputDocument.containsKey(DEBUG)) {
            outputDocument.createNestedArray(DEBUG);
        }
        outputDocument[DEBUG].add(debug);
    }
}

// This writes non-json output straight to the Serial port.
// The java implementation of JSONLink writes non-json responses directly to the logger,
// so this is a great way to get good debug integrations.
void debugOutput(String debug) {
  for (int i=0; i< debug.length(); i++) {
    Serial.write(debug.charAt(i));
  }
  Serial.write(NEWLINE);
}

// Function to write a string to EEPROM.
// add - the address to write to.
// data - the String to write.
// Used primarily to write the board id on first setup.
void writeToEEPROM(char add,String data) {
  int _size = data.length();
  int i;
  for(i=0;i<_size;i++) {
    EEPROM.update(add+i,data[i]);
  }
  // Add termination null character for String Data
  EEPROM.update(add+_size,'\0');
}

// Function to read from EEPROM.
// add - the address to read from.
// Used primarily to read the board id.
String readFromEEPROM(char add) {
  int i;
  char data[UUID_LENGTH]; //Max 64 Bytes
  int len=0;
  unsigned char k;
  k=EEPROM.read(add);
  while(k != '\0' && len<UUID_LENGTH) {   //Read until null character
    k=EEPROM.read(add+len);
    data[len]=k;
    len++;
  }
  data[len]='\0';
  return String(data);
}


// Function to read the board id from EEPROM and write it to the outputDocument.
void setupVersion() {
    if (!outputDocument.containsKey(UUID)) {
        outputDocument.createNestedArray(VERSION);
        int size = sizeof(JSONLINK_VERSION) / sizeof(int);
        for (int i=0; i < size; i++) {
            outputDocument[VERSION].add(JSONLINK_VERSION[i]);
        }
    }
}

// Function to read the board id from EEPROM and write it to the outputDocument.
void readUUID() {
    String uuid = readFromEEPROM(UUID_ADDRESS);
    if (uuid.length() == UUID_LENGTH) {
        outputDocument[UUID] = uuid;
    }
}

// Function to setup the board id. Disallows multiple writes to preserve EEPROM.
void setupUUID() {
    if (inputDocument.containsKey(UUID)) {
        if (outputDocument.containsKey(UUID)) {
          appendError("UUID Already set.");
        } else {
          String newUUID = inputDocument[UUID];
          writeToEEPROM(UUID_ADDRESS, newUUID);
        }
    }
    readUUID();
}

// Function to pass the request Id through for multi-threaded and asynchronous environments.
void setupRequestId() {
    if (inputDocument.containsKey(REQUEST_ID)) {
        outputDocument[REQUEST_ID] = inputDocument[REQUEST_ID];
    }
}

// Is the pinNum a valid digital pin?
boolean validDigitalPin(int pinNum) {
  for(int pin : DIGITAL_PINS) {
    if (pin == pinNum) {
      return true;
    }
  }
  return false;
}

// Is the pinNum a valid analog pin?
boolean validAnalogPin(int pinNum) {
  for(int pin : ANALOG_PINS) {
    if (pin == pinNum) {
      return true;
    }
  }
  return false;
}

// Is the pinNum a valid pin?
boolean validPin(int pinNum) {
  return validDigitalPin(pinNum) || validAnalogPin(pinNum);
}

boolean getPinMode(int pinNum) {
  uint8_t bit = digitalPinToBitMask(pinNum);
  uint8_t port = digitalPinToPort(pinNum);
  volatile uint8_t *reg = portModeRegister(port);
  // TRUE: output. FALSE: input.
  return (*reg & bit) ? OUTPUT : INPUT;
}

// Reads modeset commands from the JSON inputDocument, and acts upon them.
// Writes any errors to outputDocument.
void modePins() {
  if (inputDocument.containsKey(MODE)) {
    JsonObject pinsToMode = inputDocument[MODE];
    for( JsonPair pinToMode : pinsToMode ) {
      // Which pin
      int pinToModeNumber = atoi(pinToMode.key().c_str());
      // True = OUTPUT, False = INPUT
      bool value = pinToMode.value();

      // If we're setting as output (digital) and not a valid digital pin
      if (value && !validDigitalPin(pinToModeNumber)) {
        appendError(String("Cannot set OUTPUT, Invalid Digital Pin: ") + pinToMode.key().c_str());
      }

      if (!value && !validAnalogPin(pinToModeNumber)) {
        appendError(String("Cannot set INPUT, Invalid Analog Pin: ") + pinToMode.key().c_str());
      }
      pinMode(pinToModeNumber, value ? OUTPUT : INPUT);
      if (value) {
        digitalWrite(pinToModeNumber, HIGH);
      }
    }
  }
}


// Reads digital write commands from the JSON inputDocument, and acts upon them.
// Writes the state of any pins set back to the outputDocument.
void writePins() {
  if (inputDocument.containsKey(WRITE)) {
    if (inputDocument[WRITE].containsKey(DIGITAL)) {
      JsonObject digitalPins = inputDocument[WRITE][DIGITAL];
      for( JsonPair digitalPin : digitalPins ) {
        int digitalPinNumber = atoi(digitalPin.key().c_str());
        bool value = digitalPin.value();
        digitalWrite(digitalPinNumber, value ? LOW : HIGH);
        outputDocument[DIGITAL][String(digitalPinNumber)] = value;
        debug("Set:" + String(digitalPinNumber) + ":" + value ? "LOW" : "HIGH");
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


// Reads digital and analog read commands from the JSON inputDocument, and acts upon them.
// Writes the state of any requested pins back to the outputDocument.
void readPins() {
  if (inputDocument.containsKey(READ)) {
    if (inputDocument[READ].containsKey(DIGITAL)) {
      JsonArray digitalPins = inputDocument[READ][DIGITAL];
      for( const int& digitalPin : digitalPins ) {
        outputDocument[DIGITAL][String(digitalPin)] = (digitalRead(digitalPin) == LOW);
      }
    }
    if (inputDocument[READ].containsKey(ANALOG)) {
      JsonArray analogPins = inputDocument[READ][ANALOG];
      for (JsonObject analogPin : analogPins) {
        if (analogPin.containsKey(PIN) && analogPin.containsKey(VOLTAGE) && analogPin.containsKey(RESISTANCE)) {
          int analogPinNumber = analogPin[PIN];
          float voltageIn = analogPin[VOLTAGE];
          float knownResistance = analogPin[RESISTANCE];
          readAnalogPin(analogPinNumber, voltageIn, knownResistance);
        } else {
          appendError(String("Requested Analog Read but missing keys."));
        }
      }
    }
  }
}

void appendError(String message) {
    if (!outputDocument.containsKey(ERROR)) {
        outputDocument.createNestedArray(ERROR);
    }
    outputDocument[ERROR].add(message);
}

// The general JSON processing loop.
void processJson() {
  // Set Version
  setupVersion();
  // Set Board Id
  setupUUID();
  // Set Request Id
  setupRequestId();
  // Enact any pin modesets.
  modePins();
  // Write any digital pins set
  writePins();
  // Read any pins requested
  readPins();
  
  
  // outputDocument["TIMEOUT"] = TIMEOUT;
}

// Arduino Loop.
void loop() {
  // Reset the output
  deserializeJson(outputDocument, "{}");

  // Read the buffer and process it.
  buffer = Serial.readStringUntil(NEWLINE);
  processBuffer();
}

// Processing the JSON buffer.
void processBuffer() {
    // We don't have a JSON request if the payload doesn't begin with { and end with }.
    if (buffer.startsWith("{") && buffer.endsWith("}")) {
      // Attempt to deserialize the json buffer. If it is malformed, keep the error.
      DeserializationError error = deserializeJson(inputDocument, buffer);
      // If no error, process the buffer.
      if (!error) {
        processJson();
      } else {
        // If an error occurred processing the JSON, set the board id and respond with the error.
        readUUID();
        appendError(error.c_str());
      }
      // Write the outputDocument JSON to serial, with a newline to hint we're done.
      serializeJson(outputDocument, Serial);
      Serial.write(NEWLINE);
    } else if (buffer.length() > 0) {
      // Log any bad input.
      debugOutput("Bad Input.");
      debugOutput(buffer);
    }
    
    buffer = "";
}

// Arduino setup.
void setup() {
    // Initialize Serial port
    Serial.begin(BAUD);
    Serial.setTimeout(TIMEOUT);

    // Clear the buffer.
    buffer = "";
}
