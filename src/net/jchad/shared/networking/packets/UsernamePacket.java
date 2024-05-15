package net.jchad.shared.networking.packets;

public class UsernamePacket implements Packet {

    private final PacketType packet_type = PacketType.USERNAME;

    private final String username;

    public UsernamePacket(String username) {
        this.username = username;
    }

    public PacketType getPacket_type() {
        return packet_type;
    }

    public String getUsername() {
        return username;
    }
}
