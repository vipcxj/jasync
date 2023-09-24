package io.github.vipcxj.jasync.ng.agent.server.packet;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.utils.BytesUtils;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.HashMap;
import java.util.Map;

public class RawPacket {
    private final static int HEADER_SIZE = 11;
    private final static Map<Integer, CommandProvider> commandProviders = new HashMap<>();
    private byte[] header;
    private byte[] data;
    private int length;
    private int id;
    private byte flag;
    private byte commandSet;
    private byte command;
    private short errorCode;

    public RawPacket() { }

    public JPromise<Void> read(EasyNettyContext context) {
        this.header = new byte[HEADER_SIZE];
        context.readBytes(this.header).await();
        this.length = BytesUtils.getInt(this.header, 0);
        assert length >= HEADER_SIZE;
        this.id = BytesUtils.getInt(this.header, 4);
        this.data = new byte[length - HEADER_SIZE];
        context.readBytes(this.data).await();
        this.flag = BytesUtils.getByte(this.header, 8);
        if (isReply()) {
            this.errorCode = BytesUtils.getShort(this.header, 9);
        } else {
            this.commandSet = BytesUtils.getByte(this.header, 9);
            this.command = BytesUtils.getByte(this.header, 10);
        }
        return JPromise.empty();
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    public int getId() {
        return id;
    }

    public byte getFlag() {
        return flag;
    }

    public byte getCommandSet() {
        if (isReply()) {
            throw new UnsupportedOperationException("Not a command packet, unable to access the command set.");
        }
        return commandSet;
    }

    public byte getCommand() {
        if (isReply()) {
            throw new UnsupportedOperationException("Not a command packet, unable to access the command.");
        }
        return command;
    }

    public short getErrorCode() {
        if (!isReply()) {
            throw new UnsupportedOperationException("Not a reply packet, unable to access the error code.");
        }
        return errorCode;
    }

    public boolean isReply() {
        return (this.flag & 0x80) != 0;
    }
}
