package io.github.vipcxj.jasync.ng.agent.server.packet;

public interface ReplyProvider {

    ReplyPacket provide(RawPacket rawPacket);
}
