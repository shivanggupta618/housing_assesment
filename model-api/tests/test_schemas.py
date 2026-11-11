import pytest
from pydantic import ValidationError

from app.schemas import PredictRequest


VALID = {
    "square_footage": 1650, "bedrooms": 3, "bathrooms": 2,
    "year_built": 1999, "lot_size": 7100,
    "distance_to_city_center": 4.2, "school_rating": 7.8,
}


def test_single_and_batch_request_shapes():
    assert PredictRequest(property=VALID).property.square_footage == 1650
    assert len(PredictRequest(properties=[VALID, VALID]).properties) == 2


@pytest.mark.parametrize("body", [{}, {"property": VALID, "properties": [VALID]}, {"property": {**VALID, "unknown": 1}}])
def test_request_requires_exactly_one_valid_shape(body):
    with pytest.raises(ValidationError):
        PredictRequest.model_validate(body)


def test_feature_range_is_checked():
    with pytest.raises(ValidationError):
        PredictRequest(property={**VALID, "school_rating": 11})
