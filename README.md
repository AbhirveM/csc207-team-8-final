# MarketLens — Market Watchlist & Backtester

## Table of Contents

- [Project Overview](#project-overview)
- [Team Members](#team-members)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [External APIs](#external-apis)
- [Feedback](#feedback)
- [Contributing](#contributing)
- [License](#license)

---

# Project Overview
Users can create a watchlist of stock tickers, retrieve historical price information, apply trading strategies, and analyze how those strategies would have performed in the past.

The purpose of this application is to help users understand and compare trading strategies through historical simulation without risking real money.

The application supports:
- Managing a personalized stock watchlist
- Retrieving historical market data
- Running trading strategy simulations
- Comparing strategy performance
- Saving user preferences between sessions

The project is developed for **CSC207: Software Design** at the University of Toronto and follows **Clean Architecture principles**.

---

# Team Members

| Name              | GitHub    | Role | Responsibilities |
|-------------------|-----------|------|------------------|
| Abhirve Munipalle | AbhirveM         | Watchlist & Alpha Vantage Market Data | Manages stock ticker watchlists, validates ticker input, retrieves historical market data from Alpha Vantage, converts API responses into DailyPrice objects, displays historical price data, and handles API/network errors. |
| Ratnabh Khare     | RatnabhK  | Strategy Configuration & Moving Average Strategy | Creates and edits strategy configurations, defines shared strategy interfaces, implements the Moving Average Crossover strategy, validates parameters, generates buy/sell signals, and tests the strategy. |
| Dongyan Zhou      | ZhouDD213 | Momentum Strategy & General Backtesting Engine | Implements the RSI Momentum strategy, validates parameters, generates buy/sell signals, and builds the general backtesting engine to simulate trades and calculate performance metrics such as total return, number of trades, and win rate. |
| Ziyad Mouftah     | mouftz    | Persistence, Strategy Comparison, Main UI & Integration | Handles saving and loading watchlists and strategy configurations, comparing backtest results, building application navigation, connecting modules through the application builder, and performing final integration testing. |

---

# Features

## Watchlist Management

Users can:
- Add stock tickers to their personal watchlist
- Remove tickers from their watchlist
- View company information and ticker details
- View recent historical price data

Example:

```
AAPL - Apple Inc.
TSLA - Tesla Inc.
NVDA - NVIDIA Corporation
```

---

## Historical Market Data

The application retrieves historical daily market data including:

- Open price
- High price
- Low price
- Closing price
- Trading volume

Market data is retrieved automatically from an external financial data provider.

---

## Trading Strategy Backtesting

Users can test different trading strategies against stocks in their watchlist.

Supported strategies include:

### Moving Average Crossover Strategy

Users can configure:
- Short-term moving average window
- Long-term moving average window

Example:

```
Short window: 10 days
Long window: 50 days
```

---

### Momentum Strategy

Users can configure:
- Overbought threshold
- Oversold threshold

The strategy identifies possible buying and selling opportunities based on momentum indicators.

---

## Performance Analysis

After running a backtest, users receive:

- Simulated trade history
- Buy and sell points
- Individual trade gains/losses
- Total return
- Number of trades
- Win rate

Users can compare multiple strategies on the same stock to determine which strategy performed better historically.

---

## Data Persistence

The application saves:
- User watchlists
- Strategy configurations
- User preferences

Users can close and reopen the application without losing their previous setup.

---

# Technology Stack

| Category | Technology |
|----------|------------|
| Programming Language | Java 17+ |
| Build Tool | Maven |
| Testing Framework | JUnit 5 |
| Architecture | Clean Architecture |
| Market Data API | Alpha Vantage |

---

# Installation

## Requirements

Before running the project, install:

- **Java 17 or higher** — [Eclipse Temurin ( Adoptium) downloads](https://adoptium.net/) or `brew install openjdk@17` on macOS
- **Apache Maven** — [Maven install guide](https://maven.apache.org/install.html), or `brew install maven` on macOS (Homebrew must be installed first: [brew.sh](https://brew.sh/))
- **Git** — [git-scm.com/downloads](https://git-scm.com/downloads)

This project runs on any OS with a JDK (Windows, macOS, Linux) — there are no OS-specific dependencies. Installation *commands* differ by OS (e.g. Homebrew is macOS-only); Windows/Linux users should use their platform's package manager or the linked installers above.

Verify installations:

```bash
java -version
mvn -version
git --version
```

---

## Clone the Repository

```bash
git clone https://github.com/AbhirveM/csc207-team-8-final.git

cd csc207-team-8-final
```

---

## Configure API Access

This project uses the [Alpha Vantage API](https://www.alphavantage.co/documentation/) to retrieve market data.

1. Get a free API key: https://www.alphavantage.co/support/#api-key
2. Set it as an environment variable (do **not** hard-code it or commit it to GitHub):
   ```bash
   export ALPHA_VANTAGE_API_KEY=your_key_here
   ```
   Add this line to your shell profile (`~/.zshrc` or `~/.bash_profile`) to persist it across terminal sessions.
3. The application reads this key at runtime via `System.getenv("ALPHA_VANTAGE_API_KEY")`.

⚠️ Never commit your API key. If you accidentally do, rotate it immediately from your Alpha Vantage account.

---

## Build the Project

```bash
mvn clean install
```

## Run Tests

```bash
mvn test
```

## Run the Application

```bash
mvn exec:java -Dexec.mainClass="app.Main"
```

*(If `exec:java` isn't available, build first with `mvn clean package`, then run the packaged jar with `java -cp target/classes app.Main`.)*

---

## Troubleshooting

**`mvn: command not found`**
Maven isn't installed or isn't on your PATH. Install it (`brew install maven` on macOS) and restart your terminal.

**Compilation errors about a missing class in `entity`**
Someone's feature branch (e.g. a strategy configuration class) likely hasn't merged into `main` yet. Run `git pull` to get the latest `main`, and check open PRs before assuming your local copy is broken.

**`mvn clean install` downloads a lot the first time**
Normal — Maven is fetching dependencies into your local `~/.m2` cache. Subsequent builds are much faster.

**Application throws an error related to the API key**
Confirm the `ALPHA_VANTAGE_API_KEY` environment variable is set in the same terminal session you're running the app from (`echo $ALPHA_VANTAGE_API_KEY` to check).

---

# Usage

1. Set up your API key (see Installation above).
2. Launch the application (`mvn exec:java -Dexec.mainClass="app.Main"`).
3. Create or modify your stock watchlist.
4. Add desired stock tickers.
5. Retrieve historical market data.
6. Select a trading strategy.
7. Configure strategy parameters.
8. Run the backtest.
9. Review trade history and performance statistics.

Example workflow:

```
Add TSLA to watchlist
        ↓
Select Moving Average Crossover
        ↓
Set short window = 10 days
Set long window = 50 days
        ↓
Run backtest
        ↓
View return, trades, and win rate
```

Screenshots and demonstrations will be added here as each screen is completed.

---

# Project Structure

This project follows Clean Architecture.

Structure:

```
src/main/java/
├── entity/
├── use_case/
├── interface_adapter/
├── data_access/
├── view/
└── app/
```

## Entities

Responsible for core business objects, such as:

- Stock ticker
- Historical price data
- Trading strategy
- Backtest results

## Use Cases

Handles application operations, including:

- Adding/removing watchlist items
- Running backtests
- Comparing strategies
- Saving user data

## Interface Adapters

Connects application logic to external interfaces.

Examples:
- Controllers
- Presenters
- View models

## Data Access

Handles persistence — reading and writing watchlist/strategy data to local storage.

## View / App

Handles the Swing user interface and the application's entry point / dependency wiring.

---

# External APIs

## Alpha Vantage

Alpha Vantage provides historical stock market data used by the application.

- `TIME_SERIES_DAILY` — daily open, high, low, close, and volume data
- `OVERVIEW` — company name and identifying information

Documentation:

https://www.alphavantage.co/documentation/

---

# Feedback

Users can provide feedback by creating a GitHub Issue.

When submitting feedback, include:

- A description of the issue or suggestion
- Steps to reproduce bugs
- Expected behaviour
- Actual behaviour

Useful feedback should be:
- Specific
- Reproducible
- Related to improving the application

---

# Contributing

This is a closed team project for CSC207 coursework — external contributions/forks are not being accepted. Team members follow a branch → pull request → review workflow.

## Creating a Branch

```bash
git checkout main
git pull
git checkout -b feature/short-description
```

## Pull Requests

Before merging:

1. Ensure the project builds successfully (`mvn clean install`).
2. Ensure all tests pass (`mvn test`).
3. Describe the changes made.
4. Request review from another teammate.
5. Address review comments.
6. At least one other team member approves before merging.

Direct commits to `main` should be avoided.

---

# License

This project was created as coursework for:

**CSC207: Software Design**
University of Toronto

All rights reserved — this project is not licensed for external or commercial use.