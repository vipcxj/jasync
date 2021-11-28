package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.LabelNode;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LabelMap extends AbstractMap<LabelNode, LabelNode> {

    private final Map<LabelNode, LabelNode> innerMap;

    public LabelMap() {
        this.innerMap = new HashMap<>();
    }

    @Override
    public LabelNode get(Object key) {
        if (key instanceof LabelNode) {
            LabelNode labelNode = new LabelNode();
            put((LabelNode) key, labelNode);
            return labelNode;
        } else {
            return null;
        }
    }

    @Override
    public LabelNode put(LabelNode key, LabelNode value) {
        return innerMap.put(key, value);
    }

    @Override
    public Set<Entry<LabelNode, LabelNode>> entrySet() {
        return innerMap.entrySet();
    }
}
