# Housing Price Assessment

## Run with Docker Compose

Install and start Docker Desktop, then run from the repository root:

```powershell
docker compose up --build
```

Open:

- Portal: <http://localhost:3000>
- Model API and Swagger: <http://localhost:8000/docs>
- Market API: <http://localhost:8081/api/market/summary>

Press `Ctrl+C` to stop the services. To stop detached services, run `docker compose down`.

## Run locally on Windows

Prerequisites: Python 3.12+, Java 21, Maven, and Node.js/npm.

### 1. Model API

In PowerShell from the repository root:

```powershell
Set-Location model-api
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m app.train --data ..\training_data.csv --output artifacts\model.joblib
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

### 2. Market API

In a second PowerShell window from the repository root:

```powershell
Set-Location market-api
$env:HOUSING_DATA_FILE = '..\training_data.csv'
$env:ML_API_URL = 'http://127.0.0.1:8000'
mvn spring-boot:run
```

### 3. Portal

In a third PowerShell window from the repository root:

```powershell
Set-Location portal
npm ci
$env:MODEL_API_URL = 'http://127.0.0.1:8000'
$env:MARKET_API_URL = 'http://127.0.0.1:8081'
npm run dev
```

Open the portal at <http://localhost:3000> and Swagger at <http://localhost:8000/docs>.
