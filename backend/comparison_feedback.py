"""
comparison_feedback.py

Transforms numerical performance scores into useful, natural coaching
messages — the kind that a real vocal teacher would say.

For MVP this is fully deterministic (rule-based).
The rules are expressed as threshold checks on pitch_accuracy,
timing_accuracy, and stability scores.

Future versions can replace this module with an LLM-powered coach
without changing any other part of the pipeline.
"""

from typing import List, Dict, Any


# ── Thresholds ─────────────────────────────────────────────────────────────

EXCELLENT = 88
GOOD      = 72
FAIR      = 52


# ── Per-metric messages ────────────────────────────────────────────────────

def _pitch_message(
    pitch_score: int,
    pitch_details: Dict[str, Any],
) -> str:
    direction = pitch_details.get("pitch_direction", "centered")
    mae       = pitch_details.get("mae_cents")
    mae_str   = f" ({mae:.0f} cents off)" if mae is not None else ""

    if pitch_score >= EXCELLENT:
        return "Excellent pitch accuracy. You hit the right notes consistently."

    if pitch_score >= GOOD:
        if direction == "flat":
            return f"Good pitch, but you were slightly flat on several phrases{mae_str}. Support with more breath."
        if direction == "sharp":
            return f"Good pitch, but you were slightly sharp at times{mae_str}. Relax your throat and approach notes from below."
        return f"Good pitch accuracy overall{mae_str}."

    if pitch_score >= FAIR:
        if direction == "flat":
            return f"Your pitch was noticeably flat{mae_str}. Focus on singing from the diaphragm and mentally pre-tuning each note."
        if direction == "sharp":
            return f"Your pitch was noticeably sharp{mae_str}. Reduce tension in your jaw and larynx."
        return f"Pitch accuracy needs improvement{mae_str}. Practice with a drone note and match it carefully."

    return "Significant pitch inaccuracies were detected. Start with slow, single-note exercises against a piano or tuner."


def _timing_message(
    timing_score: int,
    timing_details: Dict[str, Any],
) -> str:
    mean_err = timing_details.get("mean_onset_error_ms")
    err_str  = f" (avg {mean_err:.0f} ms off)" if mean_err is not None else ""

    # Look at timing direction from details.
    late_count  = sum(1 for d in timing_details.get("details", []) if d.get("direction") == "late")
    early_count = sum(1 for d in timing_details.get("details", []) if d.get("direction") == "early")
    total       = late_count + early_count

    if timing_score >= EXCELLENT:
        return "Your timing was precise and well-matched to the reference."

    if timing_score >= GOOD:
        if total > 0 and late_count > early_count * 1.5:
            return f"Your timing was mostly good, but several phrases came in slightly late{err_str}."
        if total > 0 and early_count > late_count * 1.5:
            return f"Your timing was mostly good, but a few phrases started slightly early{err_str}."
        return f"Timing was generally consistent with minor offsets{err_str}."

    if timing_score >= FAIR:
        return f"Timing accuracy needs work{err_str}. Practicing with a metronome or click track will help."

    return "Significant timing issues were detected. Practice the piece slowly with a metronome before increasing speed."


def _stability_message(
    stability_score: int,
    stability_details: Dict[str, Any],
) -> str:
    mean_stab = stability_details.get("mean_stability_cents")
    stab_str  = f" (avg variation: {mean_stab:.0f} cents)" if mean_stab is not None else ""

    # Find worst note.
    worst = None
    worst_cents = 0.0
    for d in stability_details.get("details", []):
        if d.get("stability_cents", 0) > worst_cents:
            worst_cents = d["stability_cents"]
            worst = d.get("note")

    worst_str = f" '{worst}' was most unstable." if worst else ""

    if stability_score >= EXCELLENT:
        return "Your pitch was rock-steady throughout. Excellent breath control."

    if stability_score >= GOOD:
        return f"Good stability overall{stab_str}.{worst_str} A little wavering on sustained notes — work on even breath flow."

    if stability_score >= FAIR:
        return f"Pitch stability needs attention{stab_str}.{worst_str} Hold each note for 5 seconds daily with a drone tone to build control."

    return f"Sustained notes were noticeably unstable{stab_str}.{worst_str} Focus on diaphragmatic breathing and single-note holds before attempting phrases."


def _overall_opening(overall_score: int, tier: str) -> str:
    """A short opening sentence summarising the session."""
    openings = {
        "excellent": "Outstanding performance — this was a strong vocal session.",
        "good":      "Solid session. There are a few areas to refine, but the fundamentals are there.",
        "fair":      "A reasonable attempt. With focused practice on the areas below you'll improve quickly.",
        "needs_work": "This session highlighted some areas that need attention. Don't be discouraged — focused practice makes a real difference.",
        "poor":      "There is significant room for improvement. Let's break down the key areas to work on.",
    }
    return openings.get(tier, "Session complete. Here is your feedback.")


def _encouragement(tier: str) -> str:
    messages = {
        "excellent": "Keep it up — consistent sessions at this level will make you an outstanding vocalist.",
        "good":      "You are on the right track. Small adjustments will push you into the excellent range.",
        "fair":      "Every session counts. Focus on one metric at a time for the fastest improvement.",
        "needs_work": "Progress takes time. Come back tomorrow and work on just the pitch exercises first.",
        "poor":      "Every expert was once a beginner. Slow, deliberate practice is the fastest path forward.",
    }
    return messages.get(tier, "Keep practising — progress comes with consistency.")


# ── Master feedback builder ────────────────────────────────────────────────

def generate_comparison_feedback(
    scores: Dict[str, Any],
    performance: Dict[str, Any],
) -> Dict[str, Any]:
    """
    Build the complete feedback package for the Android app.

    Parameters
    ----------
    scores      : dict   Output from score_engine.compute_score()
    performance : dict   Output from performance_analyzer.analyze_performance()

    Returns
    -------
    dict:
        feedback_lines  list[str]   ordered coaching messages
        opening         str         overall summary sentence
        encouragement   str         motivational closing line
        pitch_advice    str         specific pitch message
        timing_advice   str         specific timing message
        stability_advice str        specific stability message
    """

    overall = scores["overall_score"]
    tier    = scores["tier"]

    pitch_details   = performance.get("pitch_accuracy",  {})
    timing_details  = performance.get("timing_accuracy", {})
    stab_details    = performance.get("stability",       {})

    pitch_msg  = _pitch_message(scores["pitch_accuracy"],  pitch_details)
    timing_msg = _timing_message(scores["timing_accuracy"], timing_details)
    stab_msg   = _stability_message(scores["stability"],   stab_details)

    opening      = _overall_opening(overall, tier)
    encourage    = _encouragement(tier)

    feedback_lines = [pitch_msg, timing_msg, stab_msg]

    return {
        "opening":          opening,
        "feedback_lines":   feedback_lines,
        "pitch_advice":     pitch_msg,
        "timing_advice":    timing_msg,
        "stability_advice": stab_msg,
        "encouragement":    encourage,
    }


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import json

    mock_scores = {
        "overall_score":   87,
        "pitch_accuracy":  91,
        "timing_accuracy": 84,
        "stability":       73,
        "tier":            "good",
    }

    mock_performance = {
        "pitch_accuracy":  {"pitch_direction": "flat",  "mae_cents": 8.2,  "reliable": True},
        "timing_accuracy": {"mean_onset_error_ms": 62,  "reliable": True,  "details": [
            {"direction": "late"},
            {"direction": "late"},
            {"direction": "early"},
        ]},
        "stability": {"mean_stability_cents": 22.4, "reliable": True, "details": [
            {"note": "A4", "stability_cents": 22.4},
        ]},
    }

    result = generate_comparison_feedback(mock_scores, mock_performance)
    print(json.dumps(result, indent=2))
