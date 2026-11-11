# Data validation and provenance

## Files and provenance

The repository contains `training_data.csv` (21 labeled rows) and `test_data.csv` (10 unlabeled rows). The prompt says both files were transcribed from screenshots, but no screenshot originals were present in the workspace. Therefore **none of the individual values can be verified against their source images**. These files are treated as provisional; no values were changed. Keep the source CSVs immutable and verify them against the original screenshots before treating model results as final.

## Structural checks

| File | Rows | Headers | Missing cells | Non-numeric cells | Exact duplicate rows |
|---|---:|---|---:|---:|---:|
| training_data.csv | 21 | 8 expected columns, including `price` | 0 | 0 | 0 |
| test_data.csv | 10 | 7 expected feature columns, no `price` | 0 | 0 | 0 |

Column order matches the schema in the assessment. All observed fields parse as numeric. The unlabeled file does not contain a `price` field and is never loaded by training code.

## Observed training ranges

| Column | Minimum | Maximum |
|---|---:|---:|
| square_footage | 980 | 2400 |
| bedrooms | 2 | 4 |
| bathrooms | 1.0 | 3.0 |
| year_built | 1978 | 2012 |
| lot_size | 4500 | 11500 |
| distance_to_city_center | 2.1 | 8.2 |
| school_rating | 6.5 | 9.1 |
| price | 160000 | 410000 |

## Observed test feature ranges

| Column | Minimum | Maximum |
|---|---:|---:|
| square_footage | 1020 | 2300 |
| bedrooms | 2 | 4 |
| bathrooms | 1.0 | 3.0 |
| year_built | 1979 | 2013 |
| lot_size | 4400 | 10300 |
| distance_to_city_center | 2.0 | 8.0 |
| school_rating | 6.5 | 9.2 |

Three test feature maxima/minima are just outside training support: lot size 4400 is below the training minimum 4500, year built 2013 is above the training maximum 2012, and school rating 9.2 is above the training maximum 9.1. The model may extrapolate slightly for these rows. API limits are intentionally broader, plausible guardrails, not learned ranges.

## Limitations

There are only 21 training observations and 10 unlabeled examples. A deterministic 20% held-out validation split leaves very few validation records and makes MAE, RMSE, and R² unstable. The model report returns metrics calculated on that split, but they should be interpreted as a pipeline smoke check, not evidence of generalization. No metric is available for `test_data.csv` because it has no labels. A Ridge regression with scaling is used to limit coefficient instability; regularization is selected from a small fixed set using only training folds, then validation is kept separate.

The current local training run selected Ridge alpha **0.1** and produced holdout MAE **$16,657.99**, RMSE **$18,816.52**, and R² **0.9429** on 5 validation rows. The row-level holdout comparison follows; CSV row numbers count the header as row 1.

| Training CSV row | Actual price | Holdout prediction | Absolute error | Absolute error % |
|---:|---:|---:|---:|---:|
| 2 | $165,000 | $187,468 | $22,468 | 13.62% |
| 19 | $175,000 | $157,173 | $17,827 | 10.19% |
| 17 | $190,000 | $188,800 | $1,200 | 0.63% |
| 3 | $265,000 | $291,933 | $26,933 | 10.16% |
| 10 | $375,000 | $360,138 | $14,862 | 3.96% |

The predictions are directionally plausible on these five held-out examples, but this is too small a sample to establish reliable accuracy. The errors range from about $1,200 to $26,933, and all estimates are compared with provisional transcribed labels. The 10 `test_data.csv` rows have no labels, so their accuracy cannot be checked.

## End-to-end and model behavior checks

The saved scikit-learn pipeline was loaded and compared with a fresh deterministic training run using `training_data.csv`; predictions matched exactly. The unlabeled file produced 10 finite predictions, but it still cannot be scored. The live FastAPI single and batch routes, the Java what-if route, and both Next.js prediction routes returned the same **$260,002.92** for the same seven-feature example. This verifies that the applications call and display the fitted pipeline consistently; it does not establish that the estimate matches a real home's value.

An additional nested leave-one-out check (Ridge alpha chosen inside each outer training fold) gave MAE **$16,223**, RMSE **$23,546**, and R² **0.918** across the 21 provisional rows. A leave-one-out mean baseline had MAE **$73,500**. This suggests the features contain useful signal in this small dataset, but the evaluation remains internal to the same 21 transcribed observations and is not an external accuracy test.

The fitted model has a **negative standardized coefficient for `square_footage` (-$27,484 per standard deviation)**. Holding the other six inputs fixed, the example estimate falls from **$260,003 at 1,650 sq ft** to **$246,829 at 1,850 sq ft**. This counterintuitive response is consistent with unstable coefficients from correlated features and the tiny sample; it must be reviewed before describing the model as reliably accurate. One row is especially unusual relative to the rest of the sample: training CSV row 10 records **1,200 sq ft at $375,000**. In the nested leave-one-out check, that row's prediction was about **$289,839** (an **$85,161** error). The source screenshots are unavailable, so this value has not been changed. Verify it against the original image before drawing conclusions or retraining.

**Submission caveat:** the API and UI prediction paths are verified, but the dataset has no trustworthy labeled test set and the model's area response is non-monotonic. Present these as provisional demonstration estimates, not validated market valuations, unless the source data can first be checked.
