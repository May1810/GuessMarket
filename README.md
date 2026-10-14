# 📈 GuessMarket - Real-Time Prediction Market Engine

A robust Client-Server prediction market platform simulating real-world event trading (similar to Polymarket). Users can trade shares on binary outcomes, with prices dynamically determined by advanced mathematical pricing mechanisms and matching engines.

## 🚀 Core Architecture & Features

- **Centralized Trading Engine (`GuessMarketEngine`):** A shared, thread-safe server state managing real-time order matching, user balances, and event lifecycle.
- **Dual Market Mechanisms:**
  - **LMSR (Logarithmic Market Scoring Rule):** An Automated Market Maker (AMM) where prices increase algorithmically as share demand rises. Supported by Market Maker initial subsidies.
  - **Order Book:** A traditional Bid/Ask matching engine for peer-to-peer trading. Includes an **auto-minting** feature that issues new share pairs when opposite orders cross the base value.
- **Client-Server Communication:** The JavaFX client communicates seamlessly with the Tomcat server via Java's native `java.net.http.HttpClient` through REST-like endpoints (`/login`, `/upload`, `/events`, `/users`, `/open`, `/buy`, `/order`, `/close`, `/topup`).
- **Dynamic State Initialization:** Market states, liquidity parameters, and user profiles are strictly parsed and loaded into the engine via XML.

## 🛠️ Tech Stack

- **Backend / Server:** Java, Apache Tomcat (Servlets API)
- **Client UI:** JavaFX 25 (OpenJFX) + FXML
- **Data Serialization:** Gson (JSON payload handling), JAXB (XML parsing)

## 🔄 Trading Workflow

1. **Authentication:** Users connect to the server via the client login.
2. **Initialization:** Market Makers load an XML configuration containing new events (default status: *Not Started*).
3. **Market Opening:** The Market Maker activates the event, funding the initial algorithmic subsidy or base liquidity from their cash balance.
4. **Live Trading:** Users place buy/sell orders (Order Book) or directly purchase shares from the AMM (LMSR).
5. **Settlement:** The Market Maker closes the event and declares the winning outcome. The engine automatically distributes payouts to winning shareholders and allocates trading commissions to the Market Maker.

## 💻 Local Development & Setup

This project is configured for execution via IntelliJ IDEA. The server must be initialized before the client to avoid connection timeouts.

### 1. Server Setup (Tomcat)
- Configure a local Tomcat Server run configuration in IntelliJ.
- **Deployment:** Add the `Server:war exploded` artifact.
- **Application context:** `/GuessMarket`
- **Port:** `8080`
- Run the server.

### 2. Client Setup (JavaFX)
- Create an Application run configuration for the `UI-FX` module.
- **Main Class:** `FxMain`
- **VM Options:** Link your local JavaFX SDK (v25) by adding:
  ```bash
  --module-path "<path_to_javafx_lib_directory>" --add-modules javafx.controls,javafx.fxml
