# F06 — Geofence-Konfiguration via Karte

## Übersicht

Interaktive Kartenansicht, über die der Nutzer GPS-Zonen (Heimat-Bahnhof, Büro, Büro-Bahnhof) auf einer Karte setzen, anpassen und verwalten kann.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Navigation, Compose
- **F02** (Local Database) — GeofenceZone Entity + DAO für Persistierung
- **F05** (Permissions) — Fine Location Permission für "Mein Standort" auf der Karte

## Requirements-Referenz

FR-G1 bis FR-G6

## Umsetzung

### Kartenanbieter

**Primär:** Google Maps SDK for Android mit Compose-Wrapper (`maps-compose`)

```kotlin
implementation("com.google.maps.android:maps-compose:4.x.x")
implementation("com.google.android.gms:play-services-maps:18.x.x")
```

**Alternativ:** osmdroid + OpenStreetMap (falls kein Google Maps API Key gewünscht)

### UI-Komponenten

#### Karten-Screen

```
┌──────────────────────────────┐
│  🔍 Adresse suchen...        │   ← Geocoding-Suchfeld
├──────────────────────────────┤
│                              │
│        [Google Map]          │   ← Interaktive Karte
│                              │
│    ◯ Hauptbahnhof (150m)    │   ← Halbtransparente Kreise
│              ◯ Büro (150m)   │     für bestehende Zonen
│                              │
├──────────────────────────────┤
│  + Neue Zone hinzufügen      │   ← FAB oder Button
│                              │
│  📍 Hauptbahnhof    [✏️][🗑️] │   ← Liste bestehender Zonen
│  📍 Büro München     [✏️][🗑️] │
└──────────────────────────────┘
```

#### Zone erstellen / bearbeiten (Bottom Sheet)

```
┌──────────────────────────────┐
│  Neue Zone                   │
│                              │
│  Name: [Hauptbahnhof       ]│
│  Typ:  ● Heimat-Bahnhof     │
│         ○ Büro               │
│         ○ Büro-Bahnhof       │
│  Farbe: 🔴 🔵 🟢 🟡         │
│                              │
│  Radius: ──●────── 150m      │   ← Slider (50m–500m)
│                              │
│  Tap auf die Karte, um den   │
│  Mittelpunkt zu setzen.      │
│                              │
│  [Abbrechen]    [Speichern]  │
└──────────────────────────────┘
```

### Interaktionsmodell

1. **Zone hinzufügen:** Nutzer tippt "Neue Zone" → Bottom Sheet öffnet sich → Nutzer tippt auf Karte → Marker + Radius-Kreis erscheinen → Slider für Radius → Name + Typ eingeben → Speichern
2. **Zone verschieben:** Long-Press auf Marker → Marker wird draggable → Loslassen speichert neue Position
3. **Radius ändern:** Zone auswählen → Bottom Sheet mit Slider → Kreis auf Karte aktualisiert sich live
4. **Zone löschen:** Swipe in der Zonenliste oder Delete-Button im Bottom Sheet → Bestätigungsdialog
5. **Adresssuche:** Geocoding-Eingabefeld → Kamera fährt zu Ergebnis → Nutzer setzt von dort aus die Zone

### Geocoding

```kotlin
// Google Geocoding via Places SDK oder Geocoder-Klasse
val geocoder = Geocoder(context, Locale.getDefault())
val results = geocoder.getFromLocationName(query, 5)
```

Alternativ: Google Places Autocomplete Widget für bessere UX.

### Datenfluss

```
Karten-UI → GeofenceViewModel → GeofenceRepository → GeofenceDao (Room)
                                        ↓
                                GeofenceRegistrar (→ F07)
                                (registriert/deregistriert bei Google Play)
```

Bei jedem Speichern/Löschen/Ändern einer Zone wird automatisch die Geofence-Registrierung bei Google Play Services aktualisiert (Verbindung zu F07).

### Akzeptanzkriterien

- [ ] Karte zeigt aktuelle Position des Nutzers ("Mein Standort")
- [ ] Zonen können per Tap auf die Karte platziert werden
- [ ] Radius ist per Slider anpassbar (50m–500m), Kreis aktualisiert sich live
- [ ] Adresssuche (Geocoding) funktioniert und bewegt die Kamera
- [ ] Bestehende Zonen werden als farbige Kreise mit Marker angezeigt
- [ ] Zonen können verschoben, bearbeitet und gelöscht werden
- [ ] Zonendaten werden in Room persistiert
- [ ] Mindestens eine HOME_STATION- und eine OFFICE-Zone müssen gesetzt sein, bevor Pendel-Tracking möglich ist (Validierung)
