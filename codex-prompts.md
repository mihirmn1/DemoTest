
## 1) Pipeline comprehension
Explain the full checkout calculation pipeline in order:
inventory/fraud → subtotal → item discounts → promo → shipping → tax → total.
For each step list inputs/outputs and implicit assumptions.
Then identify the top 5 fragile points most likely to cause regressions.

## 2) Regression risk map → test plan
Create a regression risk map for this file: list the top 8 behavior risks with concrete examples.
Then propose the first 6 unit tests we should add (names + scenarios) to protect behavior.

## 3) Safe refactor plan (no behavior change)
Propose a safe refactor plan that preserves behavior:
extract pure methods for subtotal, item discounts, promo application, shipping, taxable base, totals.
Define method signatures and acceptance criteria (what must not change).
Keep it to the minimum first step we could land in one PR.

## 4) Implement the first refactor step (patch)
Implement the first refactor step as a patch-style diff:
extract computeSubtotal(...) and computeItemDiscountTotal(...) from checkout().
Do not change behavior. Do not change public APIs. No new dependencies.

## 5) Generate targeted JUnit tests
Generate JUnit 5 tests for:
1) FREESHIP eligibility (US + subtotal >= 25)
2) SAVE10 min threshold + employee exclusion
3) AK/DE taxable base excludes shipping vs CA includes shipping
4) BOGO50 requires 2+ eligible quantity
Include test names plus arrange/act/assert.
EOF