package Domain;

public record NpcMessage(long id, NpcMessageType type, String npc, String message) {
}
