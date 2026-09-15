# Guess Market – Exercise 2 (JavaFX)

Copy this text into a Word/PDF readme before Mama submit (not Notepad).

## Submitters

- **Name:** ______________________________
- **ID:** ______________________________
- **Email:** ______________________________
- **Partner (if any):** name / ID / email
- **GitHub:** https://github.com/__________/GuessMarket

## How to run (for the checker)

Requirements: **Java 25** on Windows. JavaFX and JAXB are included in this folder (third-party).

1. Unzip the submission so these items are in **one directory**.
2. Open **cmd**, `cd` into that directory.
3. Run: `run-ex2.bat`
4. In the window: **Load File** → choose an Exercise 2 XML (file chooser only). Paths with spaces are supported.

The batch file uses `cd /d "%~dp0"` so it also works if started by double-click.

Do **not** use the Exercise 1 console UI for this exercise.

## What to test (sample `ex2-sample.xml`)

Users: Avrum (1000, MM of 2), Tikva (10000, MM of 1,3,4), Menash (100).

1. After load, all events are **Not started**. Load shows a progress bar (~1.5 seconds).
2. Users tab → **Tikva** → event 1 → **Open event**. Cash drops by LMSR subsidy `b*ln(2)` (~69.31 for b=100).
3. **Menash** → event 1 → Buy 10 of `Hell Yea !`.
4. **Avrum** → event 2 → Open. Pays 100, receives 100 Argentina + 100 Spain.
5. Avrum sells Argentina, Menash buys at the same price → trade (LAST / book update).
6. Event 2 has `allow-mint=true`. A buy on Yes plus a buy on No whose prices sum to at least `d` mints new shares into the event account.
7. Only the MM can **Close event** and pick the winner. Winners receive `1` (LMSR) or `d` (Order Book) per winning share. On-close commission goes to the MM. Leftover LMSR pot returns to the MM.
8. Load `ex2-bad-commission.xml` → error, previous state stays.
9. Resize the window; use filters (method / status / commission). Trading is done from the **Users** tab.

## Design choices

- Same **Engine** module as Exercise 1. JavaFX is a new UI module (`Exercise2/UI/UI-FX`).
- Exercise 2 XML has users: events start **Not started**. MM open pays subsidy (LMSR) or `initial` (Order Book, `initial/d` share pairs).
- Exercise 1 XML (no users) still loads in the console engine: LMSR starts Active with subsidy (old behavior).
- User cash cannot start a trade without funds. If cash goes negative (for example MM covering a short event pot), the user is **blocked**.
- On-purchase commission is paid by the buyer to the **MM user**, not into the event pot. The event pot holds subsidy / initial / mint / LMSR prices and pays winners.
- Order prices are at most `d - 0.01`. Matching uses the resting order price. One incoming order can fill several book orders. Same-user orders do not match each other.
- Mint: best opposing **buy** prices summing to at least `d`; quantity is the minimum; resting order pays its full price; incoming order pays `d - resting price`.
- Open buy orders reserve cash at the limit price; unfilled remainder stays reserved; leftover vs trade price is refunded.
- Filters use toggle groups with an **All** option.
- XML load runs on a JavaFX `Task` with a 1.5s delay and a progress bar. A failed load does not replace a previous good file.
- Window stays resizable. Details sit in a `ScrollPane`.
- Bonuses were not implemented.

## Main classes (new / changed)

Engine: `User`, `EventStatus`, `BookOrder`, `OptionBook`, `UserPosition`; DTOs `UserDTO`, `OrderDTO`, `ParticipantDTO`, `UserEventDTO`; `GuessMarketEngine` open/close/buy/placeOrder.

UI: `FxMain`, `MainController`, `MainLayout.fxml`.
