"""
score_engine.py

Converts the raw performance metrics from performance_analyzer.py
into a single transparent, explainable overall score.

Weighting model (MVP):
    Pitch Accuracy  : 60 %   (most critical — was the right note hit?)
    Timing Accuracy : 25 %   (was it sung at the right moment?)
    Pitch Stability : 15 %   (was the note held steadily?)

These weights can be tuned later based on user-testing data
or adjusted per-song based on the musical style.
"""

from typing import Dict, Any


# ── Default weight configuration ───────────────────────────────────────────

DEFAULT_WEIGHTS = {
    "pitch_accuracy":  0.60,
    "timing_accuracy": 0.25,
    "stability":       0.15,
}


# ── Score tiers ────────────────────────────────────────────────────────────

def _score_tier(score: int) -> str:
    """Human-readable tier label for a 0-100 score."""
    if score >= 90:
        return "excellent"
    elif score >= 75:
        return "good"
    elif score >= 55:
        return "fair"
    elif score >= 35:
        return "needs_work"
    else:
        return "poor"


# ── Core computation ───────────────────────────────────────────────────────

def compute_score(
    performance: Dict[str, Any],
    weights: Dict[str, float] | None = None,
) -> Dict[str, Any]:
    """
    Compute the overall Cadenza score from performance metrics.

    Parameters
    ----------
    performance : dict   Output from performance_analyzer.analyze_performance()
    weights     : dict   Optional custom weight overrides.

    Returns
    -------
    dict with:
        overall_score      int    0-100
        pitch_accuracy     int    0-100 (sub-score)
        timing_accuracy    int    0-100 (sub-score)
        stability          int    0-100 (sub-score)
        tier               str    "excellent" / "good" / "fair" / etc.
        weights_used       dict   the weights applied
        component_scores   dict   each sub-score with its weight and contribution
        reliable           bool   True if all three metrics had enough data
    """

    w = weights or DEFAULT_WEIGHTS

    # Extract sub-scores (default to 50 if unreliable / missing).
    pitch_score   = int(performance.get("pitch_accuracy",  {}).get("accuracy_score",   50))
    timing_score  = int(performance.get("timing_accuracy", {}).get("timing_score",     50))
    stab_score    = int(performance.get("stability",       {}).get("stability_score",  50))

    pitch_reliable  = bool(performance.get("pitch_accuracy",  {}).get("reliable", False))
    timing_reliable = bool(performance.get("timing_accuracy", {}).get("reliable", False))
    stab_reliable   = bool(performance.get("stability",       {}).get("reliable", False))

    # Weighted sum.
    overall_raw = (
        pitch_score  * w["pitch_accuracy"]
        + timing_score * w["timing_accuracy"]
        + stab_score   * w["stability"]
    )
    overall = int(round(overall_raw))

    return {
        "overall_score":    overall,
        "pitch_accuracy":   pitch_score,
        "timing_accuracy":  timing_score,
        "stability":        stab_score,
        "tier":             _score_tier(overall),
        "weights_used":     w,
        "component_scores": {
            "pitch_accuracy": {
                "score":        pitch_score,
                "weight":       w["pitch_accuracy"],
                "contribution": round(pitch_score * w["pitch_accuracy"], 1),
                "reliable":     pitch_reliable,
            },
            "timing_accuracy": {
                "score":        timing_score,
                "weight":       w["timing_accuracy"],
                "contribution": round(timing_score * w["timing_accuracy"], 1),
                "reliable":     timing_reliable,
            },
            "stability": {
                "score":        stab_score,
                "weight":       w["stability"],
                "contribution": round(stab_score * w["stability"], 1),
                "reliable":     stab_reliable,
            },
        },
        "reliable": pitch_reliable and timing_reliable and stab_reliable,
    }


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import json

    mock_performance = {
        "pitch_accuracy": {
            "accuracy_score": 91,
            "reliable":       True,
        },
        "timing_accuracy": {
            "timing_score": 84,
            "reliable":     True,
        },
        "stability": {
            "stability_score": 73,
            "reliable":        True,
        },
    }

    result = compute_score(mock_performance)
    print(json.dumps(result, indent=2))
    # Expected overall ≈ 0.60×91 + 0.25×84 + 0.15×73 = 54.6 + 21 + 10.95 = 86.55 → 87
