#include <Wire.h>
#include <LiquidCrystal_I2C.h>

/**
 * Version I2C de l'afficheur LCD 16x2.
 * Adresse par défaut : 0x27 (ou 0x3F selon le module).
 */
LiquidCrystal_I2C lcd(0x27, 16, 2);

void setup() {
  Serial.begin(9600);

  // Initialisation du LCD I2C
  lcd.init();
  lcd.backlight();

  // Message de bienvenue
  lcd.setCursor(0, 0);
  lcd.print("NEXUS ESPORTS");
  lcd.setCursor(0, 1);
  lcd.print("WAITING DATA...");
}

void loop() {
  if (Serial.available() > 0) {
    String data = Serial.readStringUntil('\n');

    if (data.startsWith("M:")) { // Match Score
      String content = data.substring(2);
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("MATCH SCORE:");
      lcd.setCursor(0, 1);
      lcd.print(content);
    }
    else if (data.startsWith("R:")) { // Ranking
      String content = data.substring(2);
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("TOP RANKING:");
      lcd.setCursor(0, 1);
      lcd.print(content);
    }
  }
}
