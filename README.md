# Guess Market – Exercise 1 (Console)

**Fill in before submitting, then copy this text into Word or PDF (not Notepad).**

## Submitters

- **Name:** ______________________________
- **ID:** ______________________________
- **Email:** ______________________________
- **Partner (if any):** name / ID / email
- **GitHub:** https://github.com/__________/GuessMarket

---

## How to run

1. Extract the zip.
2. Double-click `run.bat` (or run it from the folder that contains the jars).
3. The console menu appears. Use command **1** to load an XML file (full path, including files with spaces).

The program needs Java 8+ and the JAXB libraries that are listed in the batch file / `lib` folder.

---

## What the system does

Guess Market is a prediction-market console app.

- The **Engine** module holds all market logic (XML load, LMSR pricing, accounts, trades, close).
- The **UI** module is the only place with `main`, `Scanner`, and `System.out`. It prints menus, reads input, and calls the engine.

There is one user (the checker). Each event has exactly two options, its own MM account, and starts **Active** until it is closed.

### Console commands

1. Load XML file  
2. Show events  
3. Show event trading state  
4. Participate (buy shares)  
5. Close event  
6. Exit  

Lists are numbered from **1**. The user never types option names. Invalid input does not crash the program.

---

## Design choices

- **Two IntelliJ modules:** Engine (passive) and UI (active), as required.
- **Engine API is an interface** (`IGuessMarketEngine`) so later exercises can reuse the same engine without changing the UI contract much.
- **DTOs only** are returned to the UI. The UI never touches `Event` / `Option` model objects.
- **XML** is read with JAXB. Tag names follow the course schema (`Guess-Market`, `comision`, `GM-LMSR`, etc.).
- **Safe load:** a new event list is built in a temporary list. Only if the whole file is valid is it assigned. A bad file does not wipe a previous good load.
- **LMSR cost:** `C(q) = b * ln(Σ exp(q_i / b))`. Buy price = `C(after) − C(before)`.
- **Subsidy on load:** each event account starts at `C(0,0) = b * ln(2)` for two options.
- **Option probability:** `P_i = exp(q_i / b) / Σ exp(q_j / b)`, shown with two decimals in the UI.
- **On-purchase commission:** percent of the LMSR base price, added to the event account.
- **On-close commission:** percent of the winners’ payout (`$1 × winning shares`), then winners are paid from the account. The remaining balance is **not** reset and may be negative.
- **Trade history** is stored newest-first. Price in history is the LMSR base price (without commission).
- **Buy result** is returned as `BuyResultDTO` (base, commission, total) so the UI can print the receipt.

---

## Main classes

### UI module

| Class | Role |
|--------|------|
| `Main` | Program entry. Menu loop, input validation, all printing. Calls only `IGuessMarketEngine`. |

### Engine – API and logic

| Class | Role |
|--------|------|
| `IGuessMarketEngine` | Public engine operations: load XML, list events, buy, close. |
| `GuessMarketEngine` | Implementation: JAXB load, validations, LMSR math, commission, DTO mapping. |

### Engine – models (internal)

| Class | Role |
|--------|------|
| `Event` | One market: options, LMSR `b`, commission, active/closed, winner, history, account. |
| `Option` | Option name and number of shares bought. |
| `EventAccount` | Event (MM) balance and total commission collected. |
| `Trade` | One buy: option, quantity, base price paid. |

### Engine – DTOs (sent to UI)

| Class | Role |
|--------|------|
| `EventDTO` | Display data for an event (including account, history, winner). |
| `OptionDTO` | Option name, shares, current probability. |
| `TradeDTO` | One history row. |
| `BuyResultDTO` | Receipt after a purchase. |

### Engine – XML (JAXB)

| Class | Role |
|--------|------|
| `GuessMarketDescriptor` | Root `Guess-Market`. |
| `GMEvent` | One `GM-event`. |
| `Commission` | `comision` value + `type`. |
| `GMMethod` / `GMLmsr` | Trading method and liquidity `b`. |

---

## XML checks (application-wise)

The file is assumed valid against the schema. The engine still checks:

- path exists and ends with `.xml`
- unique event IDs
- commission between 0 and 90
- each event has exactly two options

Errors are returned as clear messages. The UI prints them and continues.

---

## Project structure

```
GuessMarket/
  Exercise1/Engine/   engine module
  Exercise1/UI/       console UI module
  lib/                JAXB jars
  GM-EX1-schema.xsd
  single.xml          sample file
```
