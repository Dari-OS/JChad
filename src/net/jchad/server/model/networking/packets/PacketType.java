package net.jchad.server.model.networking.packets;

public enum PacketType {
    BANNED ,
    NOT_WHITELISTED;



    /**
     * Gets a more readable form of the enum:
     * NOT_WHITELISTED => Not whitelisted
     * BANNED          => Banned
     * @return
     */
    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase().replace("_", "");
    }
}
