package io.github.vipcxj.jasync.ng.agent.server.packet;

public interface CommandProvider {

    CommandPacket provide(RawPacket rawPacket);
}
