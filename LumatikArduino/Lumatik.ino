boolean debug = false;

boolean initialised = false; // set after first call of setArduinoFactors on the Android system

#include <SoftwareSerial.h>
#include <Adafruit_TCS34725.h>
#include <Adafruit_VEML6075.h>

SoftwareSerial BTserial(2, 3); // RX | TX

// max length of command and data
const byte numChars = 20;
char receivedChars[numChars];
boolean newData = false;

Adafruit_TCS34725 tcs = Adafruit_TCS34725(TCS34725_INTEGRATIONTIME_700MS, TCS34725_GAIN_1X);
Adafruit_VEML6075 uv = Adafruit_VEML6075();

//for timing
unsigned long start;
unsigned long current;
const unsigned long period = 1000;

//for sending data at periods
const unsigned short dataSendCounterMax = 59;
unsigned short dataSendCounter = 0;

//for keeping internal "clock" state
uint8_t hours = 0;
uint8_t minutes = 0;
uint8_t seconds = 0;

//for wake and bed times
uint8_t wakeHours = 0;
uint8_t wakeMinutes = 0;
uint8_t bedHours = 0;
uint8_t bedMinutes = 0;

//for uv calculation, real values sent from app
float exposureFactor = 1; // 0-1 (completely clothed-naked)
float pigmentFactor = 1; //(1 = typeII others are ratios set by android app)
float ageFactor = 1; //0-1 see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3256341/
int IU_per_SED = 4861; // see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3256341/
float cumIU = 0;
float IU_threshold = 400;
bool warn_IU_sent = false;

//for LED Warnings
uint8_t LED_UV = 4;
uint8_t LED_BLUE = 5;


void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(LED_UV, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);
  if (debug)
  {
    Serial.begin(9600);
    Serial.println("<Lumatik is ready>");
  }

  BTserial.begin(9600);
  bool check1 = false;
  bool check2 = false;
  while (!check1 || !check2)
  {
    if (tcs.begin())
    {
      if (debug)
      {
        Serial.println("RGB Sensor Detected");
      }
      check1 = true;
    } else {
      if (debug)
      {
        Serial.println("RGB Sensor Not Detected");
      }
    }
    if (uv.begin())
    {
      if (debug)
      {
        Serial.println("UV Sensor Detected");
      }
      check2 = true;
    } else
    {
      if (debug)
      {
        Serial.println("UV Sensor Not Detected");
      }
    }
    delay(5000);
  }
  start = millis();
  digitalWrite(LED_UV, LOW);
  digitalWrite(LED_BLUE, LOW);
}

void loop() {
  if (BTserial.available() > 0)
  {
    recvWithStartEndMarkers();
  }
  if (newData)
  {
    parseData();
  }
  current = millis();
  if (current - start >= period)
  {
    start = current;
    updateTime();
    if (initialised)
    {
      updateWarnings();
      if (dataSendCounter < dataSendCounterMax)
      {
        dataSendCounter += 1;
      } else {
        dataSend();
        dataSendCounter = 0;
      }
    }
  }
}

void updateWarnings()
{
  if (cumIU > IU_threshold)
  {
    digitalWrite(LED_UV, HIGH);
    if (!warn_IU_sent)
    {
      BTserial.print("<Wuv>");
      if (debug)
      {
        Serial.println("<Wuv>");
      }
      warn_IU_sent = true;
    }
  } 
  else 
  {
    digitalWrite(LED_UV, LOW);
  }

  // light stuff
  uint16_t r, g, b, c, lux;
  tcs.getRawData(&r, &g, &b, &c);
  lux = tcs.calculateLux(r, g, b);

  // check for blue light before bed
  if (isBeforeBedTime() && b > 100)
  {
    digitalWrite(LED_BLUE, HIGH);
  }
  else
  {
    digitalWrite(LED_BLUE, LOW);
  }

  // check for light during night time
  if (isNightTime() && lux > 30)
  {
    BTserial.print("<Wlu>");
    if (debug)
    {
      Serial.println("<Wlu>");
    }
  }
}

void dataSend()
{
  //<rgbabidt>
  //send <MES-R-red-G-green-B-blue-A-uva-V-uvb-I-uvi-U-cumiu-T-time->
  //now we have unknown full length however parsing uses letter heads and reads ascii values of floats
  //easier to ignore endianess of architecture ie saves me time

  // UV bits
  float uvi = uv.readUVI();
  float uvb = uv.readUVB();
  float uva = uv.readUVA();
  float uv_sed = (60 * ((uvi * 25) / 1000)) / 100; // Based on UVI -> SED conversion at http://support.bentham.co.uk/support/solutions/articles/5000619299-erythemal-radiant-exposure-and-uv-index
  float IU = (uv_sed * IU_per_SED * pigmentFactor * exposureFactor * ageFactor);
  cumIU += IU;

  // light stuff
  uint16_t r, g, b, c, lux;
  tcs.getRawData(&r, &g, &b, &c);
  lux = tcs.calculateLux(r, g, b);


  //Default Message
  BTserial.print("<");
  BTserial.print("R"); BTserial.print(r, DEC);
  BTserial.print("G"); BTserial.print(g, DEC);
  BTserial.print("B"); BTserial.print(b, DEC);
  BTserial.print("A"); BTserial.print(uva, 6);
  BTserial.print("V"); BTserial.print(uvb, 6);
  BTserial.print("I"); BTserial.print(uvi, 6);
  BTserial.print("U"); BTserial.print(cumIU, 6);
  BTserial.print("T"); BTserial.write(seconds); BTserial.write(minutes); BTserial.write(hours);
  BTserial.print(">");
  if (debug)
  {
    Serial.print("R: "); Serial.print(r, DEC); Serial.print(" ");
    Serial.print("G: "); Serial.print(g, DEC); Serial.print(" ");
    Serial.print("B: "); Serial.print(b, DEC); Serial.print(" ");
    Serial.print("Lux: "); Serial.println(lux, DEC);
    Serial.print("A: "); Serial.print(uva, 6); Serial.print(" ");
    Serial.print("V: "); Serial.print(uvb, 6); Serial.print(" ");
    Serial.print("I: "); Serial.println(uvi, 6);
    Serial.print("IU: "); Serial.println(IU);
    Serial.print("CUMIU: "); Serial.println(cumIU);
    Serial.print("T"); Serial.print(seconds); Serial.print(minutes); Serial.println(hours);
  }
}

void updateTime()
{
  seconds += 1;
  if (seconds > 59)
  {
    seconds = 0;
    minutes += 1;
    if (minutes > 59)
    {
      minutes = 0;
      hours += 1;
      if (hours > 23)
      {
        hours = 0;
      }
    }
  }
  if (debug)
  {
    Serial.print((int)hours); Serial.print(":"); Serial.print((int)minutes); Serial.print(":"); Serial.print((int)seconds); Serial.println("");
  }
  if (hours == 0 && minutes == 0 && seconds == 0)
  {
    cumIU = 0;
    warn_IU_sent = false;
  }
}

void setDeviceTime()
{
  bool permitted = true;
  if (debug)
  {
    Serial.print((uint8_t) receivedChars[1]);
    Serial.print((uint8_t) receivedChars[2]);
    Serial.print((uint8_t) receivedChars[3]);
    Serial.println("");
  }
  if ((uint8_t)receivedChars[1] < 24 && (uint8_t)receivedChars[2] < 60 && (uint8_t)receivedChars[3] < 60)
  {
    seconds = receivedChars[3];
    minutes = receivedChars[2];
    hours = receivedChars[1];
  } else
  {
    permitted = false;
  }
  if (debug && !permitted)
  {
    Serial.println("Time incorrect format");
    Serial.print((uint8_t) receivedChars[1]);
    Serial.print((uint8_t) receivedChars[2]);
    Serial.print((uint8_t) receivedChars[3]);
  }
}


//This and the following function isNightTime are not effeciently called as they check every data update,
//however lets be honest, effecient implementations would be complicated

//SHOULD THIS WRAP AROUND? ie check for 2 hours after also for blue light
bool isBeforeBedTime()
{
  //if bedhours < 2
  // one system
  // else
  // normal system

  int res = bedHours - hours;
  if (debug)
  {
    Serial.print("Res: "); Serial.println(res);
  }
  if (bedHours == 1)
  {
    if (res == 0)
    {
      if (bedMinutes <= minutes)
      {
        return true;
      }
    }
    else if (res == 1)
    {
      return true;
    }
    else if (res == -22)
    {
      if (bedMinutes >= minutes)
      {
        return true;
      }
    }
  }
  else if (bedHours == 0)
  {
    if (res == 0)
    {
      if (bedMinutes <= minutes)
      {
        return true;
      }
    }
    else if (res == -23)
    {
      return true;
    }
    else if (res == -22)
    {
      if (bedMinutes >= minutes)
      {
        return true;
      }
    }
  }
  else
  {
    if (res == 2)
    {
      if (bedMinutes <= minutes)
      {
        return true;
      }
    }
    else if (res == 1)
    {
      return true;
    }
    else if (res == 0)
    {
      if (bedMinutes >= minutes)
      {
        return true;
      }
    }
  }
  return false;
}

bool isNightTime()
{
  int ct = hours * 60 + minutes;
  int bt = bedHours * 60 + bedMinutes;
  int wt = wakeHours * 60 + wakeMinutes;
  if (bt < wt) // bedtime and waketime exist in same 24 hr period
  {
    return (ct >= bt && ct <= wt);
  }
  else //bedtime is before 0:00 and waketime is after
  {
    return (ct >= bt || ct <= wt);
  }
}

void setExposure()
{
  char in[6] = {receivedChars[1], receivedChars[2], receivedChars[3], receivedChars[4], receivedChars[5], '\0'};
  String inString = String(in);
  exposureFactor = inString.toFloat();
  if (debug)
  {
    Serial.print("Exposure: "); Serial.println(exposureFactor, 6);
  }
}

void setPigment()
{
  char in[7] = {receivedChars[1], receivedChars[2], receivedChars[3], receivedChars[4], receivedChars[5], receivedChars[6], '\0'};
  String inString = String(in);
  pigmentFactor = inString.toFloat();
  if (debug)
  {
    Serial.print("Pigment: "); Serial.println(pigmentFactor, 6);
  }
}

void setAge()
{
  char in[5] = {receivedChars[1], receivedChars[2], receivedChars[3], receivedChars[4], '\0'};
  String inString = String(in);
  if (debug)
  {
    Serial.print("Age: "); Serial.print(inString);
  }
  ageFactor = inString.toFloat();
  if (debug)
  {
    Serial.print("Age: "); Serial.println(ageFactor, 6);
  }
}

void setWakeTime()
{
  wakeHours = receivedChars[1];
  wakeMinutes = receivedChars[2];
  if (debug)
  {
    Serial.print("Waketime: "); Serial.print(wakeHours); Serial.print(":"); Serial.println(wakeMinutes);
  }
}

void setBedTime()
{

  bedHours = receivedChars[1];
  bedMinutes = receivedChars[2];
  if (debug)
  {
    Serial.print("Bedtime: "); Serial.print(bedHours); Serial.print(":"); Serial.println(bedMinutes);
  }
}

void parseData()
{
  newData = false;
  if (receivedChars[0] == 'O'  && receivedChars[1] == 'N' )
  {
    digitalWrite(LED_BUILTIN, HIGH);
  }
  if (receivedChars[0] == 'O'  && receivedChars[1] == 'F' )
  {
    digitalWrite(LED_BUILTIN, LOW);
  }
  if (receivedChars[0] == 'I')
  {
    initialised = true;
  }
  if (receivedChars[0] == 'T')
  {
    setDeviceTime();
  }
  if (receivedChars[0] == 'E')
  {
    setExposure();
  }
  if (receivedChars[0] == 'P')
  {
    setPigment();
  }
  if (receivedChars[0] == 'A')
  {
    setAge();
  }
  if (receivedChars[0] == 'W')
  {
    setWakeTime();
  }
  if (receivedChars[0] == 'B')
  {
    setBedTime();
  }
}

void recvWithStartEndMarkers()
{

  // function recvWithStartEndMarkers by Robin2 of the Arduino forums
  // See  http://forum.arduino.cc/index.php?topic=288234.0

  static boolean recvInProgress = false;
  static byte ndx = 0;
  char startMarker = '<';
  char endMarker = '>';
  char rc;

  if (BTserial.available() > 0)
  {
    rc = BTserial.read();
    if (recvInProgress == true)
    {
      if (rc != endMarker)
      {
        receivedChars[ndx] = rc;
        ndx++;
        if (ndx >= numChars) {
          ndx = numChars - 1;
        }
      }
      else
      {
        receivedChars[ndx] = '\0'; // terminate the string
        recvInProgress = false;
        ndx = 0;
        newData = true;
      }
    }

    else if (rc == startMarker) {
      recvInProgress = true;
    }
  }
}
