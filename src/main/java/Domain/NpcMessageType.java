package Domain;

public enum NpcMessageType {
    BASIC,
    SPECIFIC;

    /** Maps the legacy command/storage strings ("Basic"/"Specific") to the enum. */
    public static NpcMessageType fromLegacy(String value) {
        return "Specific".equalsIgnoreCase(value) ? SPECIFIC : BASIC;
    }
}
