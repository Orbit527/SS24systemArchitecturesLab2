# Lab 02 Actors

# UI Commands

## Air Condition

### Air Condition ein- und ausschalten: 
    a [true | false]

## Environment

### Temperatur manuell beeinflussen:
    e t value (z.B. e t 20)

### Wetter manuell beeinflussen: 
    e w [RAINY | SUNNY | CLOUDY |STORMY]

## Media Station

### MediaStation ein- und ausschalten: 
    m [true | false]

### Film abspielen und stoppen: 
    m [play | stop]

## Fridge
### Fridge gelagerte Produkte anzeigen:
    fridge query products

### Fridge alle Bestellungen anzeigen:
    fridge query orders

### Fridge alle subscribeden Produkte anzeigen:
    fridge query subscription

### Fridge Produkt konsumieren:
    fridge consume productName
    z.B. fridge consume Bacon

### Fridge Produkt bestellen:
    fridge order productName productPrice productWeight
    z.B. fridge order Bacon 15.99 0.8

### Fridge Subscription für Produkt einrichten:
    fridge subscribe productName productPrice productWeight
    z.B. fridge subscribe Bacon 15.99 0.8

### Fridge Subscription für Produkt abbrechen:
    fridge unsubscribe productName
    z.B. fridge unsubscribe Bacon

