package Domain;

/** A character sheet. {@code userId == null} means it is an NPC. */
public record CharacterSheet(long id, String userId, String name, String picture) {

    public boolean isNpc() {
        return userId == null;
    }
}
