package de.otaku.birthdayevent.reward;

public record CongratulationClaim(boolean accepted, CongratulationRecord record) {
    public static CongratulationClaim accepted(CongratulationRecord record) {
        return new CongratulationClaim(true, record);
    }

    public static CongratulationClaim rejected(CongratulationRecord record) {
        return new CongratulationClaim(false, record);
    }
}
