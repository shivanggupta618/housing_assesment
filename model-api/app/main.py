import os
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .schemas import PredictRequest, PredictResponse, Prediction
from .train import FEATURES

MODEL_PATH = Path(os.getenv("MODEL_PATH", str(Path(__file__).resolve().parents[1] / "artifacts/model.joblib")))
_artifact = None
app = FastAPI(title="Housing Price Prediction API", version="1.0.0", description="Ridge regression estimates from the provisional housing dataset. Estimates are not appraisals.")
origins = [item.strip() for item in os.getenv("CORS_ORIGINS", "http://localhost:3000").split(",") if item.strip()]
app.add_middleware(CORSMiddleware, allow_origins=origins, allow_methods=["GET", "POST"], allow_headers=["Content-Type"])


def get_artifact():
    global _artifact
    if _artifact is not None:
        return _artifact
    if not MODEL_PATH.is_file():
        raise HTTPException(status_code=503, detail="Model artifact is unavailable. Train the model before starting the API.")
    try:
        _artifact = joblib.load(MODEL_PATH)
        return _artifact
    except Exception as exc:
        raise HTTPException(status_code=503, detail="Model artifact could not be loaded.") from exc


@app.get("/health")
def health():
    try:
        get_artifact()
        return {"status": "ready", "model_loaded": True}
    except HTTPException:
        return {"status": "not_ready", "model_loaded": False}


@app.get("/model-info")
def model_info():
    artifact = get_artifact()
    pipeline = artifact["pipeline"]
    ridge = pipeline.named_steps["ridge"]
    return {
        "model": artifact["metadata"], "features": artifact["features"],
        "metrics": artifact["metrics"],
        "coefficients_standardized": {name: float(value) for name, value in zip(FEATURES, ridge.coef_)},
        "intercept": float(ridge.intercept_),
        "data_note": "Validation metrics use only a held-out subset of the 21 labeled records; test_data.csv is unlabeled and is not used.",
    }


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    artifact = get_artifact()
    rows = [request.property] if request.property is not None else request.properties
    try:
        frame = pd.DataFrame([item.model_dump() for item in rows], columns=FEATURES)
        values = artifact["pipeline"].predict(frame)
        return PredictResponse(predictions=[Prediction(predicted_price=round(float(value), 2)) for value in values])
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Prediction failed due to an internal model error.") from exc
