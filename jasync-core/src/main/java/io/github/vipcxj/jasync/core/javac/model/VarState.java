package io.github.vipcxj.jasync.core.javac.model;

import java.io.Serializable;
import java.util.Objects;

public class VarState implements Serializable {
    private static final long serialVersionUID = 1931903667095146691L;
    private boolean initialized;
    private boolean read;
    private boolean write;
    private boolean readBeforeInitialized;
    private boolean redefined;

    public VarState() {
        this.initialized = false;
        this.read = false;
        this.write = false;
        this.readBeforeInitialized = false;
        this.redefined = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public boolean isReadBeforeInitialized() {
        return readBeforeInitialized;
    }

    public void setReadBeforeInitialized(boolean readBeforeInitialized) {
        this.readBeforeInitialized = readBeforeInitialized;
    }

    public boolean isRedefined() {
        return redefined;
    }

    public void setRedefined(boolean redefined) {
        this.redefined = redefined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarState state = (VarState) o;
        return initialized == state.initialized &&
                read == state.read &&
                write == state.write &&
                readBeforeInitialized == state.readBeforeInitialized &&
                redefined == state.redefined;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialized, read, write, readBeforeInitialized, redefined);
    }

    @Override
    public String toString() {
        return "VarState{" +
                "initialized=" + initialized +
                ", read=" + read +
                ", write=" + write +
                ", readBeforeInitialized=" + readBeforeInitialized +
                ", redefined=" + redefined +
                '}';
    }
}
