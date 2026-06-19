package Database;

/** Holds one instance of each repository over a shared {@link Database}. */
public class Repositories {

    private final ConfigRepository config;
    private final SessionRepository sessions;
    private final FoodRepository food;
    private final CharacterRepository characters;
    private final NpcMessageRepository npcMessages;

    public Repositories(Database db) {
        this.config = new ConfigRepository(db);
        this.sessions = new SessionRepository(db);
        this.food = new FoodRepository(db);
        this.characters = new CharacterRepository(db);
        this.npcMessages = new NpcMessageRepository(db);
    }

    public ConfigRepository config() {
        return config;
    }

    public SessionRepository sessions() {
        return sessions;
    }

    public FoodRepository food() {
        return food;
    }

    public CharacterRepository characters() {
        return characters;
    }

    public NpcMessageRepository npcMessages() {
        return npcMessages;
    }
}
