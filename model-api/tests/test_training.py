from pathlib import Path

import pytest

from app.train import load_training_data

DATA = Path(__file__).resolve().parents[2] / "training_data.csv"


def test_training_data_schema_and_label_count():
    frame = load_training_data(DATA)
    assert len(frame) == 21
    assert "price" in frame.columns


def test_test_file_is_not_accepted_as_training_data():
    with pytest.raises(ValueError, match="schema mismatch"):
        load_training_data(DATA.with_name("test_data.csv"))
