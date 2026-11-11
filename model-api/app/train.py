"""Train and persist the reproducible Ridge pipeline and held-out metrics."""
import argparse
import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import GridSearchCV, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

FEATURES = [
    "square_footage", "bedrooms", "bathrooms", "year_built", "lot_size",
    "distance_to_city_center", "school_rating",
]
TARGET = "price"
SEED = 42


def load_training_data(path: str | Path) -> pd.DataFrame:
    frame = pd.read_csv(path)
    required = FEATURES + [TARGET]
    missing = [column for column in required if column not in frame.columns]
    extra = [column for column in frame.columns if column not in required]
    if missing or extra:
        raise ValueError(f"CSV schema mismatch; missing={missing}, unexpected={extra}")
    frame = frame[required].apply(pd.to_numeric, errors="coerce")
    if frame.isna().any().any():
        raise ValueError("Training CSV contains missing or non-numeric values.")
    if (frame[FEATURES] < 0).any().any() or (frame[TARGET] <= 0).any():
        raise ValueError("Training data contains values outside basic non-negative/positive ranges.")
    if len(frame) < 10:
        raise ValueError("At least 10 labeled records are required for a held-out split.")
    return frame


def train(data_path: str | Path, output_path: str | Path) -> dict:
    frame = load_training_data(data_path)
    x_train, x_valid, y_train, y_valid = train_test_split(
        frame[FEATURES], frame[TARGET], test_size=0.20, random_state=SEED
    )
    base = Pipeline([("scale", StandardScaler()), ("ridge", Ridge())])
    # Tune alpha within the training subset only. Keep CV small because N is tiny.
    search = GridSearchCV(
        base, {"ridge__alpha": [0.1, 1.0, 10.0, 100.0]},
        cv=min(5, len(x_train)), scoring="neg_mean_absolute_error", n_jobs=1,
    )
    search.fit(x_train, y_train)
    pipeline = search.best_estimator_
    pred = pipeline.predict(x_valid)
    metrics = {
        "mae": float(mean_absolute_error(y_valid, pred)),
        "rmse": float(mean_squared_error(y_valid, pred) ** 0.5),
        "r2": float(r2_score(y_valid, pred)),
        "validation_rows": int(len(y_valid)),
        "split": "80/20 random holdout, random_state=42",
        "selection": "Ridge alpha selected by 5-fold (or available-fold) CV on training subset",
    }
    # Refit the chosen fixed pipeline on all labeled rows for production prediction.
    pipeline.fit(frame[FEATURES], frame[TARGET])
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    artifact = {
        "pipeline": pipeline,
        "features": FEATURES,
        "metrics": metrics,
        "metadata": {
            "model": "Ridge regression with StandardScaler",
            "selected_alpha": float(search.best_params_["ridge__alpha"]),
            "training_rows": int(len(frame)),
            "training_source": Path(data_path).name,
            "random_state": SEED,
            "test_data_used_for_training": False,
        },
    }
    joblib.dump(artifact, output_path)
    return artifact


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../training_data.csv")
    parser.add_argument("--output", default="artifacts/model.joblib")
    args = parser.parse_args()
    result = train(args.data, args.output)
    print(json.dumps({"metrics": result["metrics"], "metadata": result["metadata"]}, indent=2))
