# Housing Price Assessment

This repository contains a FastAPI model service, a Spring Boot market data service, and a Next.js portal. The portal calls same-origin Next.js routes; service URLs are configured with environment variables. The training CSV is the sole labeled source. `test_data.csv` is unlabeled and is never read by the trainer.

## Data and model

See [`data_validation_report.md`](data_validation_report.md) before interpreting results. The source CSV was transcribed from screenshots that are not included here, so individual source values remain unverified. There are 21 labeled records. The service trains a standardized Ridge regression pipeline, selects regularization within the training portion only, reports MAE/RMSE/R² on a deterministic 20% holdout, and then refits the selected pipeline on all labeled rows for prediction. Metrics are unstable at this sample size and should be treated as a demonstration, not validation of real-world pricing accuracy.

## Run all services with Docker Compose

Prerequisites: Docker with Compose. From the repository root:

```sh
docker compose up --build
```

Open the portal at <http://localhost:3000>, the model Swagger UI at <http://localhost:8000/docs>, and the market API at <http://localhost:8081/api/market/summary>. Compose builds the Python model artifact during image build, starts the Java service after model health is ready, and then starts the portal. Ports can be changed in `docker-compose.yml`; configure `PORTAL_ORIGIN` if the portal is served from another origin.

For local development, set `MODEL_API_URL` and `MARKET_API_URL` in `portal/.env.local`, run the model service from `model-api` after installing `requirements.txt` and executing `python -m app.train --data ../training_data.csv --output artifacts/model.joblib`, then start Java 21/Maven from `market-api`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot'
$env:HOUSING_DATA_FILE = '..\training_data.csv'
$env:ML_API_URL = 'http://127.0.0.1:8000'
mvn "-Dmaven.repo.local=..\.maven-repository" spring-boot:run
```

Finally run `npm install && npm run dev` in `portal`. On Windows without a Java 21 JDK, install Microsoft OpenJDK with `winget install Microsoft.OpenJDK.21`.

## Model API

From `model-api`, install requirements, train once, then start:

```sh
pip install -r requirements.txt
python -m app.train --data ../training_data.csv --output artifacts/model.joblib
uvicorn app.main:app --reload
```

Swagger: <http://localhost:8000/docs>. `POST /predict` accepts either `{"property": {...}}` or `{"properties": [{...}, ...]}` with the seven numeric feature names shown below. Additional fields are rejected. Scalar bounds are enforced by the API. Batch size is limited to 500.

```sh
curl -X POST http://localhost:8000/predict -H 'Content-Type: application/json' -d '{"property":{"square_footage":1650,"bedrooms":3,"bathrooms":2,"year_built":1999,"lot_size":7100,"distance_to_city_center":4.2,"school_rating":7.8}}'
curl http://localhost:8000/model-info
curl http://localhost:8000/health
```

The response is `{"predictions":[{"predicted_price": ...}]}`. `/model-info` reports the holdout metrics, metadata, feature names, and coefficients in standardized-feature units. `MODEL_PATH` and comma-separated `CORS_ORIGINS` configure artifact loading and browser origins.

## Market API

- `GET /api/market/summary` supports `bedrooms`, `minBathrooms`, `minYear`, `maxYear`, `minSize`, and `maxSize` query parameters; response includes count, mean, median, min/max, and matching source rows.
- `POST /api/market/what-if` forwards a property object to the model API. It does not duplicate model calculations.
- `GET /api/market/health` reports readiness.

Aggregation/filter results are cached in-process. The source CSV is loaded once at startup. `HOUSING_DATA_FILE`, `ML_API_URL`, `PORTAL_ORIGIN`, and `PORT` configure the service.

## Portal

- `/estimator`: seven-field form, input constraints, result card, browser-local history, up to three-property comparison.
- `/market`: initial server-side market data load, segment/year/size filters, sortable data, segment average chart, what-if form, CSV download, and print-to-PDF.
- `portal/.env.example` documents the server-side service URL variables. Production service URLs are not exposed to browser JavaScript.

## Verification

Run Python tests with `cd model-api && pip install -r requirements-dev.txt && pytest -q`, portal checks with `cd portal && npm ci && npm run lint && npm run build`, and Java tests with `cd market-api && mvn test` using Python 3.12+ and Java 21. In the local verification run, Python tests passed (7), Java tests passed (3), and the portal TypeScript check and production build passed. Swagger, estimator, and market pages returned HTTP 200; model prediction worked through the API and through both Java and portal what-if routes. The current held-out prediction comparison and its limits are in [`data_validation_report.md`](data_validation_report.md). Docker is unavailable in this environment, so Compose and the container build were not run. The source screenshots are also unavailable, so the CSV remains provisional.

## Five-minute interview walkthrough

1. Open the home page and explain the split between estimator and market analysis.
2. On `/estimator`, enter a home, request its value, then save two scenarios and compare their inputs and estimates.
3. On `/market`, filter the 21-row sample by bedroom count or construction year; explain how the averages and rows respond.
4. Sort a column and export the filtered sample as CSV; use Print / Save PDF for a shareable snapshot.
5. Change a what-if feature and show that the Java service forwards it to the shared Python model API. Open `/docs` to demonstrate the API contracts and `/model-info` to explain the holdout metrics and small-data limitation.
