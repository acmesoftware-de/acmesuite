# Handoff: ACMEsuite — Business-Suite Oberfläche (CRM · Supply · Build · HR · Admin)

## Overview
ACMEsuite ist eine schlanke Business-Suite für kleine Unternehmen / Digital Twins / Test-Inputs, bestehend aus fünf Modulen: **CRM** (Vertrieb), **Supply** (SCM), **Build** (Produktion), **HR** (Personal) und **Admin** (Verwaltung). Dieser Entwurf zeigt eine navigierbare Single-Page-App-Shell: Top-Bar mit Modul-Reitern, pro Modul eine oder mehrere Unteransichten, ein Hell/Dunkel-Umschalter und durchgehend interaktive Elemente (Drag-&-Drop-Kanban, Inline-Bearbeitung, ein Kapazitäts-/Machbarkeitsrechner, ein Import-Restriktionsformular).

Jedes Modul hat eine **eigene Signalfarbe**, die beim Modulwechsel den Akzent der gesamten Oberfläche einfärbt (Reiter-Unterstrich, Buttons, KPI-Deltas, Avatare).

## About the Design Files
Die Datei in diesem Bundle (`CRM Dashboard.dc.html`) ist eine **Design-Referenz, erstellt in HTML** — ein Prototyp, der das beabsichtigte Aussehen und Verhalten zeigt, **kein Produktionscode zum direkten Übernehmen**. Die Aufgabe ist, dieses Design in der bestehenden Umgebung des Ziel-Codebase nachzubauen (React, Vue, Angular, Svelte o.ä.) mit dessen etablierten Mustern, Komponenten und Bibliotheken. Falls noch keine Umgebung existiert, wähle das am besten geeignete Framework (Empfehlung unten) und implementiere das Design dort.

> **Technischer Hinweis zum Prototyp:** Die `.dc.html`-Datei ist in einem hausinternen „Design Component"-Format geschrieben (ein `<x-dc>`-Template mit `{{ }}`-Platzhaltern plus eine `class Component`-Logikklasse, gerendert von `support.js`). Du musst dieses Format **nicht** übernehmen — es dient nur als exakte Referenz für Layout, Werte und Verhalten. Öffne die Datei im Browser, um sie live zu erkunden, und lies den Quelltext für die exakten Stilwerte.

## Fidelity
**High-fidelity (hifi).** Finale Farben, Typografie, Abstände und Interaktionen. Die UI soll pixelgenau mit den Bibliotheken und Mustern des Codebase nachgebaut werden. Alle Maße, Hex-Werte und Schriftgrößen unten sind verbindlich.

---

## Empfohlener Tech-Stack (falls kein Codebase existiert)
- **React + TypeScript** (Vite).
- State: lokaler Komponenten-State / Zustand oder Redux Toolkit — der Prototyp nutzt eine einzige Zustandsklasse; die Datenstrukturen lassen sich 1:1 übernehmen.
- Drag & Drop: `@dnd-kit/core` (Kanban-Spalten).
- Styling: CSS-Variablen für das Theming (siehe Design Tokens), da der Prototyp genau so themt. Tailwind ist möglich, aber die Modul-Akzentfarbe muss als CSS-Variable (`--accent`) laufzeitumschaltbar bleiben.

---

## Globales Layout (App-Shell)

Feste Design-Fläche im Prototyp: **1320 × 840 px** (Desktop-App-Fenster). Im echten Build responsiv/fluid umsetzen; die Proportionen unten beibehalten.

**Vertikale Struktur (flex column):**
1. **Top-Bar** — Höhe **57px**, `background: var(--panel)`, `border-bottom: 1px solid var(--line)`.
2. **Modul-Kopf** — Eyebrow + Titel (`Anton`, 40px) links; rechts die In-App-Reiter der Unteransichten, ganz rechts der „+ NEU"-Button.
3. **KPI-Leiste** — 4 Kacheln im Grid (`grid-template-columns: repeat(4,1fr); gap: 12px`). Wird auf reinen Formular-/Board-Ansichten ausgeblendet (z.B. Supply→Import-Regeln).
4. **Modul-Inhalt** — füllt den Rest (`flex:1; min-height:0`), scrollt intern.

### Top-Bar Inhalt (links → rechts)
- **Logo-Glyph**: vier vertikale Balken (4px breit), Höhen 8/13/19/11px, Farben `#E5322A #E9AE06 #1358D8 #159E5B`, `align-items:flex-end`, `gap:2px`.
- **Wortmarke**: „ACME" (700) + „SUITE" (500, `var(--dim)`), `Archivo` 13px, `letter-spacing:.06em`.
- **Trennlinie** 1px, `var(--line)`.
- **Modul-Reiter**: CRM · Supply · Build · HR · Admin. Aktiver Reiter: `Archivo` 13px 700, `var(--ink)`, mit 3px-Unterstrich in Modulfarbe; inaktiv 500, `var(--dim)`. Klick wechselt Modul.
- **Spacer** (`flex:1`).
- **Suchfeld**: „Suchen…" + `⌘K`-Badge, `border:1px solid var(--line)`, `background:var(--bg)`, Font 12.5px.
- **Theme-Toggle**: 34×34px, `border:1px solid var(--line)`, Glyph `◐` (dark) / `◑` (light). Schaltet `data-mode` zwischen `dark`/`light`.
- **Avatar**: 32×32px, `background: var(--accent)`, Initialen „JS" `Space Mono` 700 11px, weiß.

### In-App-Reiter (Unteransichten) — WICHTIGES MUSTER
Die Reiter für Unteransichten sitzen **rechts** im Modul-Kopf, unmittelbar **links vom „+ NEU"-Button** (ganz rechts). Muster:
- Reiter-Gruppe: `display:flex; border:1px solid var(--line2); margin-bottom:7px`. Buttons `padding:8px 13px` (Build: `8px 12px`), `Space Mono` 700 10px, `letter-spacing:.04em`. Aktiv: `background:var(--accent); color:#fff`. Inaktiv: `background:transparent; color:var(--dim)`. Trennung per `border-left:1px solid var(--line2)`.
- „+ NEU"-Button: `margin-bottom:7px; padding:8px 15px; border:1px solid var(--accent); background:var(--accent); color:#fff; Space Mono 700 10px`. Höhe und Grundlinie sind bewusst an die Reiter-Gruppe angeglichen.

---

## Module & Ansichten

### 1. CRM (Akzent `#E5322A` rot) — Titel „Pipeline / Alle Deals"
Unteransichten (Reiter rechts): **Tabelle · Kanban · Funnel** (ein `+ DEAL`-Button).
KPIs: Pipeline-Wert `€945k`, Offene Deals `11`, Win-Rate `38%`, Ø Zyklus `21 T` (Delta grün `#159E5B` / rot `#E5322A`).

- **Tabelle**: Spalten `26px 2fr 158px 128px 132px 1.3fr 44px` = Checkbox · Firma+Kontakt · Phase (Dropdown) · Wert · Wahrscheinlichkeit (Balken+%) · Letzte Aktivität · Team-Avatar. Zeilen 54px, Zebra (`var(--panel2)`). **Inline-Bearbeitung**: Firma und Wert sind `contentEditable` (Fokus: `outline:2px solid var(--accent)`); Phase per `<select>`. Wahrscheinlichkeit ergibt sich aus der Phase (neu 15 / qual 35 / angebot 60 / verhandlung 80 / gewonnen 100 %).
- **Kanban**: 5 Spalten (NEU · QUALIFIZIERT · ANGEBOT · VERHANDLUNG · GEWONNEN), Spaltenbreite 236px. Karten per **Drag & Drop** verschiebbar (`draggable`, `onDragStart`/`onDragOver`/`onDrop`); Zielspalte setzt `stage` des Deals neu. Karte: Firma (600 13.5px), Kontakt (`var(--dim)`), Owner-Avatar 20px, Wert `Space Mono` 700, Alter in Tagen. Spaltenkopf: farbiger Punkt (Phasenfarbe), Label, Count-Badge, Summe rechts.
- **Funnel**: pro Phase ein zentrierter Balken, Breite ∝ Deal-Anzahl (min 96px), Höhe 74px, Phasenfarbe; große `Anton`-Zahl + „DEALS"; rechts Anteil % und Konversionsrate zur Vorstufe. Rechte Spalte: Gesamt-Pipeline `€945k`, Lead→Abschluss `38%`.

Phasenfarben: neu `#8A8F98` · qualifiziert `#E9AE06` · angebot `#1358D8` · verhandlung `#E5322A` · gewonnen `#159E5B`.
Owner-Farben: JS `#E5322A` · AL `#1358D8` · MW `#159E5B`.

### 2. Supply (Akzent `#159E5B` grün) — Titel „Bestände"
Unteransichten: **Bestände · Lieferanten · Import-Regeln**.
KPIs: Lagerwert `€2,4 Mio`, Kritische SKUs `4`, Offene Bestellungen `12`, Liefertreue `96%`.

- **Bestände**: Tabelle (`2fr 108px 120px 1.3fr 118px`) Artikel+SKU · Bestand · Meldebestand · Füllstand-Balken · Status-Pille. Status-Logik: `< 0.5×Meldebestand` → KRITISCH (rot), `< Meldebestand` → NIEDRIG (gelb), sonst OK (grün). Rechte Rail „Nachbestellung": Artikel unter Meldebestand mit Vorschlagsmenge (`2×Meldebestand − Bestand`) und „BESTELLEN"-Button.
- **Lieferanten**: Tabelle (`1.9fr 1fr 96px 1.3fr 1fr 120px`) Lieferant+Land · Kategorie · Risiko-Pille (low grün/mid gelb/high rot) · Liefertreue-Balken · Letzte Prüfung · Status (freigegeben/Prüfung/gesperrt). Rechte Rail „Compliance": Zähler freigegeben/Prüfung/gesperrt (große `Anton`-Zahlen) + Sanktions-Hinweis.
- **Import-Regeln** (Formular, KPIs ausgeblendet): 
  - Regelname (`<input>`), 
  - Wirkung als Segmented-Control: ⛔ BLOCKIEREN (rot) / ◷ GENEHMIGUNG (gelb) / ⚠ WARNEN (blau), 
  - drei Chip-Gruppen **Länder** / **Firmen** (Denied Parties) / **Güter** (Warengruppen/HS) — Toggle-Chips (aktiv: `background:var(--accent); color:#fff`; inaktiv: `border:1px solid var(--line2)`), 
  - „Gültig ab" Datumsfeld + „REGEL SPEICHERN". Speichern legt die Regel an und listet sie live rechts unter „Aktive Regeln" (farbiger Links-Rand nach Wirkung, Scope-Zeile „N Länder · N Firmen · N Güter"); danach Formular-Reset.
  - Vorschlagslisten: Länder = Russland, Belarus, Iran, Nordkorea, Syrien, Myanmar, China, Kuba. Güter = Dual-Use-Güter, Halbleiter, Verschlüsselung, Chemikalien, Waffenteile, Drohnentechnik, Nachtsichtgeräte.

### 3. Build (Akzent `#1358D8` blau) — Fokus Planung
Unteransichten: **Machbarkeit · Schichten · Produkte · Aufträge · Maschinen** (`+ AUFTRAG`).
KPIs: Auslastung `87%`, Aufträge heute `10`, Ausschuss `1,8%`, Durchlaufzeit `3,2 T`.

- **Machbarkeit** (Kernfunktion): Prüft, ob ein Auftrag zum Wunschtermin mit der aktuellen Kapazität machbar ist.
  - **Banner** (Links-Rand grün/rot): „MACHBAR / NICHT MACHBAR ZUM WUNSCHTERMIN" + Icon ✓/✕; Auftrag FA-1050 · Rahmen XL · 800 Stk; rechts Benötigt / Verfügbar / Auslastung (große `Anton`-Zahlen).
  - **Engpass-Analyse**: pro Ressource (Zuschnitt/CNC-Fräsen/Schweißen/Montage) ein Auslastungsbalken; > 100% rot, > 85% gelb, sonst grün. CNC-Fräsen ist im Default der Engpass (159%).
  - **Stellhebel** (rechte Rail, Toggles): „Zweite Schicht" (+56 h/Tag), „Samstagsschicht" (+1 Tag), „Teil-Fremdvergabe" (−35% Eigenfertigung). Umschalten rechnet **live** neu.
  - Rechenmodell (aus Prototyp): `reqH = 384 × (Fremdvergabe? 0.65 : 1)`; `dailyCap = 56 × (2.Schicht? 2 : 1)`; `windowDays = 6 + (Samstag? 1 : 0)`; `availH = dailyCap × windowDays`; machbar wenn `availH ≥ reqH`. Ressourcen-Last = Anteil·reqH / Anteil·availH.
- **Schichten**: editierbare Wochen-Matrix (3 Schichten × 6 Tage Mo–Sa). Zellen-Klick zykelt **Frei → Voll → Teil** (grün/gelb/leer). Kapazität = Σ (voll 1 / teil 0.5). Zeilen: Früh 06–14, Spät 14–22, Nacht 22–06.
- **Produkte**: Master-Detail. Links Produktliste (Rahmen XL, Gehäuse A2, Welle 12 mm, Halter C3, Klemme K2; Artikelnr., Revision, „Auslauf"-Badge; ausgewählt: `border-left:3px solid var(--accent)`, `background:var(--chip)`). Rechts Kopf mit Kennzahlen (Positionen, Schritte, Durchlauf min/Stk) und zwei Panels nebeneinander:
  - **Stückliste** (mehrstufig, Einrückung `12 + lvl×20 px`): POS · Komponente · Menge+Einheit · Typ (Eigen/Zukauf-Badge) · Verfügbarkeits-Punkt (grün/gelb/rot). Level-0-Positionen fett.
  - **Arbeitsschritte** (Routing): NR (10/20/30…) · Arbeitsgang · **Maschine** (Chip; „manuell" wenn keine, dann `var(--faint)`, kein Rahmen) · Zeit (min). Panel-Breite 452px.
- **Aufträge**: Fertigungs-Kanban (GEPLANT · RÜSTEN · IN ARBEIT · PRÜFUNG · FERTIG), Karten per Drag & Drop. Karte: Auftragsnr. (Akzent), Produkt, Menge, Maschine-Chip, Owner-Avatar.
- **Maschinen** (Digital-Twin-Monitor, Bonus): Grid `repeat(4,1fr)`. Kachel je Maschine mit `border-top:3px` in Statusfarbe: Name, Live-Status (Läuft grün+pulsierender Punkt / Rüsten gelb / Störung rot / Wartung blau / Leerlauf grau), große OEE-Zahl (`Anton`, Farbe ≥85 grün / ≥70 gelb / sonst rot), laufender Auftrag, drei Mini-Balken (Verfügbarkeit/Leistung/Qualität), Fortschrittsbalken. Leerlauf/Wartung: `opacity:.6`.

### 4. HR (Akzent `#E9AE06` gelb) — Titel „Team"
Unteransichten: **Team · Org-Chart · Bewerber** (`+ MITARBEITER` / `+ BEWERBER`).
KPIs: Mitarbeitende `148`, Offene Stellen `7`, Abwesend heute `9`, Fluktuation `4,2%`.
> Hinweis: Gelber Akzent — auf gelbem Grund schwarze Schrift (`#111`) verwenden (siehe aktive Reiter/Toggles).

- **Team**: Tabelle (`2fr 1.4fr 1fr 110px`) Mitarbeiter (Avatar+Name) · Rolle · Team · Status (aktiv grün / remote blau / urlaub gelb / krank rot). Rechte Rail „Abwesend heute" (Urlaub/Krank).
- **Org-Chart**: CEO-Karte oben (`background:#111`, weiß), 26px-Konnektor, darunter 4 Bereichsleitungs-Karten (`border-top:3px` in Bereichsfarbe: Vertrieb rot / Produktion blau / Supply grün / People gelb) mit Avatar, Rolle, Teamgröße; darunter je Direktberichte als eingerückte Liste (`border-left:2px`).
- **Bewerber**: Recruiting-Kanban (NEU · SCREENING · INTERVIEW · ANGEBOT · ABGELEHNT). Karte: Avatar, Name, Team, Rolle, „MATCH n%"-Badge (≥85 grün / ≥70 gelb / sonst rot), Alter in Tagen.

### 5. Admin (Akzent `#8A8F98` grau) — Titel „System"
KPIs: Nutzer `152`, Rollen `8`, Aktive Sessions `37`, Lizenzen `160`.
- Tabelle (`2.2fr 110px 100px 1fr`) Nutzer (Avatar+E-Mail) · Rolle (Badge; Owner/Admin gefüllt in Akzent, sonst `var(--chip)`) · Status (aktiv grün / inaktiv grau) · Letzter Zugriff. Rechte Rail „Sicherheit": Toggle-Liste (Zwei-Faktor erzwingen, Audit-Log, SSO (SAML), Wartungsmodus) mit AN/AUS-Schaltern (an: Track grün).

---

## Interactions & Behavior
- **Modulwechsel**: Klick auf Top-Reiter setzt `activeMod`; `--accent` und alle abgeleiteten Farben wechseln sofort. Jedes Modul merkt sich seine eigene Unteransicht.
- **Theme-Toggle**: `data-mode` auf dem App-Root zwischen `dark`/`light`; alle Farben sind CSS-Variablen (kein Hardcoding). Toggle ist pro Modulinstanz vorhanden.
- **Drag & Drop** (CRM-, Build-Aufträge-, HR-Bewerber-Kanban): Karte `draggable`, Spalte reagiert auf `onDragOver`(preventDefault)/`onDrop`; Drop setzt das `stage`-Feld neu. Gezogene Karte `opacity:.4`.
- **Inline-Bearbeitung** (CRM-Tabelle): `contentEditable` Firma/Wert, `<select>` Phase; Wert-Parsing entfernt Nicht-Ziffern.
- **Machbarkeitsrechner** (Build): Toggles rechnen synchron neu (Banner-Status, benötigte/verfügbare Stunden, Ressourcenlast).
- **Schicht-Matrix** (Build): Zellen-Klick zykelt Frei→Voll→Teil, Kapazitätszahl aktualisiert live.
- **Import-Regel speichern** (Supply): validiert Name + ≥1 Kriterium, prepended in Liste, Formular-Reset.
- **Chip-Toggles / Segmented-Controls**: Klick fügt hinzu/entfernt bzw. setzt Auswahl.
- **Pulsierender Live-Punkt**: `@keyframes acmepulse` (opacity 1→.3→1, 1.6–1.8s) für „laufende" Maschinen und Live-Feeds.
- **Fade-in** neuer Feed-Einträge: `@keyframes acmefade` (translateY(-5px)+opacity, .45s).

## State Management
Zentrale Zustandsfelder (aus dem Prototyp): `activeMod` ('CRM'|'SUP'|'BLD'|'HR'|'ADM'); pro Modul ein View-Feld (`viewD` CRM: tabelle/kanban/funnel · `supView` bestand/lieferanten/restriktion · `bldView` machbarkeit/schichten/produkte/auftraege/maschinen · `hrView` team/orgchart/bewerber); je Modul ein `mode` (dark/light). Daten-Arrays: Deals (mit `stage`), Bestände, Lieferanten, Import-Regeln (+ Formular-Objekt `impRule`), Fertigungsaufträge (`stage`), Produkte/BOM/Routing, Schicht-Grid (2D), Kapazitäts-Toggles (`capLevers`), Maschinen, HR-Mitarbeiter, Org-Baum, Bewerber (`stage`), Admin-Nutzer + Sicherheits-Toggles. State-Transitionen: Reiterklick → View; Drag-Drop → `stage`; Toggle/Chip → boolean/Array; Inline-Edit → Feldwert.
Kein Datenabruf im Prototyp — alle Daten sind statisch/mock. Im echten Build durch API ersetzen.

## Design Tokens

### Modul-Akzentfarben
- CRM rot `#E5322A` · HR gelb `#E9AE06` · Build blau `#1358D8` · Supply grün `#159E5B` · Admin grau `#8A8F98`

### Semantische Farben
- Erfolg/positiv `#159E5B` · Warnung `#E9AE06` · Fehler/negativ `#E5322A` · Info `#1358D8` · neutral `#8A8F98`

### Theme-Variablen (dark / light)
- `--bg`: `#0E0F11` / `#F1EFE9`
- `--panel`: `#17181C` / `#FFFFFF`
- `--panel2` (Zebra): `#1E2025` / `#F6F4EF`
- `--line`: `rgba(255,255,255,.09)` / `rgba(0,0,0,.10)`
- `--line2`: `rgba(255,255,255,.15)` / `rgba(0,0,0,.18)`
- `--ink` (Text): `#F4F3EF` / `#15161A`
- `--dim`: `rgba(255,255,255,.54)` / `rgba(0,0,0,.56)`
- `--faint`: `rgba(255,255,255,.34)` / `rgba(0,0,0,.40)`
- `--chip`: `rgba(255,255,255,.05)` / `rgba(0,0,0,.045)`
- `--shadow`: `rgba(0,0,0,.45)` / `rgba(0,0,0,.14)`

### Typografie
- **Display/Titel**: `Anton` (Google Fonts), normal weight, für große Zahlen und Modultitel (27–74px, `line-height` ~0.8–0.9).
- **UI/Fließtext**: `Archivo` (400/500/600/700).
- **Mono/Labels/Werte**: `Space Mono` (400/700) — Labels in Versalien mit `letter-spacing:.04–.14em`.
- Google-Fonts-Import: `Anton`, `Archivo:wght@400;500;600;700`, `Space+Mono:wght@400;700`.
- Referenzgrößen: Section-Labels 9.5–10px Space Mono 700; Tabellentext 11–13.5px Archivo; KPI-Zahl 27px Anton; Modultitel 40px Anton.

### Geometrie & Effekte
- **Ecken: durchgehend eckig (border-radius 0)** außer Avataren/Status-Punkten (`border-radius:50%` bei runden Punkten; Avatare sind quadratisch).
- Rahmen 1px `var(--line)` / `var(--line2)`.
- Akzent-Kanten: `border-left:3px` (Kanban-Karten, aktive Listeneinträge) / `border-top:3px` (Maschinen-/Org-Karten) in Signalfarbe.
- Abstände: Container-Padding meist 22–24px horizontal; Grid-`gap` 12–14px; Reiter-Padding 8px vertikal.
- Schatten sparsam (nur Popover/Copilot): `0 16px 48px var(--shadow)`.

## Assets
Keine externen Bild-/Icon-Dateien. Alle Grafiken sind CSS/Unicode:
- Logo = 4 CSS-Balken (Modulfarben).
- Icons als Unicode-Glyphen: `⌘K`, `◐`/`◑` (Theme), `✦` (Copilot/KI), `⚠`, `✓`/`✕`, `⚙`, `⛔`, `◷`.
- Avatare = Initialen auf farbigem Quadrat.
Im echten Build durch das Icon-Set des Codebase ersetzen (z.B. Lucide/Heroicons); Modulfarben als Token übernehmen.

## Files
- `CRM Dashboard.dc.html` — der vollständige Prototyp mit allen fünf Modulen und allen Unteransichten. Im Browser öffnen zum Erkunden; Quelltext lesen für exakte Stilwerte. (Die begleitende `support.js` ist nur die Laufzeit des Prototyp-Formats und wird im Zielprojekt **nicht** benötigt.)
