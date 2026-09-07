def get_severity(deviation_cents: float) -> str:
    """
    Determines how serious the pitch deviation is.

    Small deviation means the singer is close to the target pitch.
    Large deviation means the singer needs correction.
    """

    absolute_deviation = abs(deviation_cents)

    if absolute_deviation <= 10:
        return "excellent"

    elif absolute_deviation <= 30:
        return "mild"

    elif absolute_deviation <= 60:
        return "moderate"

    else:
        return "high"


def get_accuracy_score(deviation_cents: float) -> int:
    """
    Converts pitch deviation into a simple pitch accuracy score out of 100.

    This score only measures how close the detected central pitch is
    to the nearest musical note.
    """

    absolute_deviation = abs(deviation_cents)

    score = 100 - (absolute_deviation * 1.5)

    if score < 0:
        score = 0

    if score > 100:
        score = 100

    return round(score)


def get_stability_score(stability_cents: float) -> int:
    """
    Converts pitch stability into a score out of 100.

    Lower stability_cents means the pitch was held more steadily.
    Higher stability_cents means the pitch moved around more.
    """

    score = 100 - (stability_cents * 1.2)

    if score < 0:
        score = 0

    if score > 100:
        score = 100

    return round(score)


def get_overall_score(
    pitch_accuracy_score: int,
    stability_score: int
) -> int:
    """
    Combines pitch accuracy and pitch stability into one final score.

    Pitch accuracy has slightly more importance than stability,
    but both affect the final result.
    """

    overall_score = (pitch_accuracy_score * 0.65) + (stability_score * 0.35)

    return round(overall_score)


def generate_summary(note: str, status: str) -> str:
    """
    Creates a short summary of the user's pitch accuracy.
    """

    if status == "in tune":
        return f"Your pitch center was close to {note}."

    elif status == "slightly flat":
        return f"Your pitch center was slightly flat on {note}."

    elif status == "flat":
        return f"Your pitch center was noticeably flat on {note}."

    elif status == "slightly sharp":
        return f"Your pitch center was slightly sharp on {note}."

    elif status == "sharp":
        return f"Your pitch center was noticeably sharp on {note}."

    else:
        return f"Your pitch on {note} needs review."


def generate_pitch_advice(status: str) -> str:
    """
    Creates practical singing advice based on pitch accuracy.
    """

    if status == "in tune":
        return "Your central pitch is accurate. Focus on maintaining the same placement throughout the note."

    elif status == "slightly flat":
        return "Try lifting the pitch gently. Avoid pushing too hard; instead, keep the tone supported and relaxed."

    elif status == "flat":
        return "Your pitch is below the target. Focus on better breath support and mentally aim slightly higher before singing the note."

    elif status == "slightly sharp":
        return "Try relaxing the note slightly. Keep your throat relaxed and avoid over-brightening the tone."

    elif status == "sharp":
        return "Your pitch is above the target. Reduce tension and approach the note with a calmer, more controlled tone."

    else:
        return "Listen carefully to the reference pitch and try matching it slowly before increasing speed."


def generate_stability_message(
    stability_status: str,
    stability_cents: float
) -> str:
    """
    Creates feedback based on how stable the pitch was.
    """

    if stability_status == "stable":
        return "Your pitch was steady and controlled."

    elif stability_status == "moderately stable":
        return "Your pitch was fairly controlled, but there were small movements. Try holding the note with smoother breath support."

    elif stability_status == "unstable":
        if stability_cents >= 120:
            return "Your voice moved across multiple notes, so this sounds more like a melodic phrase than one sustained note."
        else:
            return "Your pitch was unstable. Try holding one note steadily for a few seconds without sliding."

    else:
        return "Pitch stability could not be clearly evaluated."


def generate_stability_advice(stability_status: str) -> str:
    """
    Gives practical advice for improving pitch stability.
    """

    if stability_status == "stable":
        return "Keep practicing sustained notes to maintain this control."

    elif stability_status == "moderately stable":
        return "Practice singing a single note on 'Aaah' for 5 seconds while keeping the volume even."

    elif stability_status == "unstable":
        return "Start with short sustained-note exercises. Use a piano or reference tone, match the note, and hold it without sliding up or down."

    else:
        return "Record again in a quiet place and sing one clear sustained note."


def generate_encouragement(
    pitch_severity: str,
    stability_status: str
) -> str:
    """
    Generates a motivational line using both pitch accuracy and stability.
    """

    if pitch_severity == "excellent" and stability_status == "stable":
        return "Excellent control. Your pitch and stability are both strong."

    elif pitch_severity == "excellent" and stability_status != "stable":
        return "Good pitch awareness. Now focus on keeping the note steady."

    elif pitch_severity in ["mild", "moderate"] and stability_status == "stable":
        return "Your voice was steady. A small pitch adjustment will make it cleaner."

    elif pitch_severity in ["mild", "moderate"] and stability_status != "stable":
        return "Good attempt. Work on both pitch placement and steadiness step by step."

    else:
        return "Do not worry. Slow practice with reference notes will improve your control."


def generate_feedback(note_data: dict) -> dict:
    """
    Original simple feedback function.

    This is kept for compatibility with older code.
    It only considers pitch accuracy and does not consider stability.
    """

    note = note_data.get("note", "Unknown")
    status = note_data.get("status", "unknown")
    deviation_cents = note_data.get("deviation_cents", 0)

    pitch_severity = get_severity(deviation_cents)
    pitch_accuracy_score = get_accuracy_score(deviation_cents)

    summary = generate_summary(note, status)
    advice = generate_pitch_advice(status)
    encouragement = generate_encouragement(
        pitch_severity=pitch_severity,
        stability_status="unknown"
    )

    return {
        "note": note,
        "status": status,
        "deviation_cents": deviation_cents,
        "accuracy_score": pitch_accuracy_score,
        "pitch_accuracy_score": pitch_accuracy_score,
        "stability_score": None,
        "overall_score": pitch_accuracy_score,
        "severity": pitch_severity,
        "summary": summary,
        "advice": advice,
        "encouragement": encouragement,
        "stability_message": None,
        "stability_advice": None
    }


def generate_performance_feedback(
    note_data: dict,
    pitch_summary: dict
) -> dict:
    """
    Main feedback function for Cadenza.

    This considers:
    1. Pitch accuracy
    2. Pitch stability

    This gives more realistic vocal coaching feedback.
    """

    note = note_data.get("note", "Unknown")
    status = note_data.get("status", "unknown")
    deviation_cents = note_data.get("deviation_cents", 0)

    stability_status = pitch_summary.get("stability_status", "unknown")
    stability_cents = pitch_summary.get("stability_cents", 0)

    pitch_severity = get_severity(deviation_cents)

    pitch_accuracy_score = get_accuracy_score(deviation_cents)
    stability_score = get_stability_score(stability_cents)

    overall_score = get_overall_score(
        pitch_accuracy_score=pitch_accuracy_score,
        stability_score=stability_score
    )

    summary = generate_summary(note, status)
    pitch_advice = generate_pitch_advice(status)

    stability_message = generate_stability_message(
        stability_status=stability_status,
        stability_cents=stability_cents
    )

    stability_advice = generate_stability_advice(
        stability_status=stability_status
    )

    encouragement = generate_encouragement(
        pitch_severity=pitch_severity,
        stability_status=stability_status
    )

    combined_advice = f"{pitch_advice} {stability_advice}"

    return {
        "note": note,
        "status": status,
        "deviation_cents": deviation_cents,

        "accuracy_score": overall_score,
        "pitch_accuracy_score": pitch_accuracy_score,
        "stability_score": stability_score,
        "overall_score": overall_score,

        "severity": pitch_severity,
        "stability_status": stability_status,
        "stability_cents": stability_cents,

        "summary": summary,
        "stability_message": stability_message,
        "advice": combined_advice,
        "encouragement": encouragement
    }


if __name__ == "__main__":
    test_note_data = {
        "note": "D#3",
        "deviation_cents": 9.95,
        "status": "in tune"
    }

    test_pitch_summary = {
        "stability_status": "unstable",
        "stability_cents": 252.42
    }

    feedback = generate_performance_feedback(
        note_data=test_note_data,
        pitch_summary=test_pitch_summary
    )

    print(feedback)