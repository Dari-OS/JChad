package net.jchad.client.model.client.connection;

import net.jchad.shared.networking.packets.Packet;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * This class is responsible for sending packets to the server. It also handles the encryption and
 * serialization process.
 */
public class ConnectionWriter extends Thread implements AutoCloseable {
    /**
     * This PrintWriter wraps the {@link OutputStream} provided on construction of this object.
     */
    private final PrintWriter out;

    public ConnectionWriter(OutputStream out) {
        this.out = new PrintWriter(out, true);

        start();
    }

    /**
     * This method will serialize and encrypt the specified {@link Packet}. After that
     * it will be sent to the server.
     *
     * @param packet the {@link Packet} which will be serialized and encrypted and sent to the server.
     */
    public void sendPacket(Packet packet) {
        out.println(packet);
    }

    @Override
    public void close() throws Exception {
        out.close();
    }
}
