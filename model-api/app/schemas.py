from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, model_validator


class PropertyFeatures(BaseModel):
    model_config = ConfigDict(extra="forbid")

    square_footage: Annotated[float, Field(gt=0, le=50000)]
    bedrooms: Annotated[int, Field(ge=0, le=20)]
    bathrooms: Annotated[float, Field(ge=0, le=20)]
    year_built: Annotated[int, Field(ge=1700, le=2100)]
    lot_size: Annotated[float, Field(gt=0, le=10_000_000)]
    distance_to_city_center: Annotated[float, Field(ge=0, le=1000)]
    school_rating: Annotated[float, Field(ge=0, le=10)]


class PredictRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    # Exactly one of property or properties must be supplied.
    property: PropertyFeatures | None = None
    properties: list[PropertyFeatures] | None = Field(default=None, min_length=1, max_length=500)

    @model_validator(mode="after")
    def require_one_form(self):
        if (self.property is None) == (self.properties is None):
            raise ValueError("Provide exactly one of 'property' or 'properties'.")
        return self


class Prediction(BaseModel):
    predicted_price: float


class PredictResponse(BaseModel):
    predictions: list[Prediction]
