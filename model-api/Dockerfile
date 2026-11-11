FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 MODEL_PATH=/app/artifacts/model.joblib
WORKDIR /app
COPY model-api/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY model-api/app ./app
COPY training_data.csv /app/data/training_data.csv
RUN python -m app.train --data /app/data/training_data.csv --output /app/artifacts/model.joblib
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
