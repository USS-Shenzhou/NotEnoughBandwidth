package cn.ussshenzhou.notenoughbandwidth.indextype;

import java.util.HashMap;

public class PathIndex extends HashMap<String, Integer> {

    public final int namespaceIndex;

    public PathIndex(int namespaceIndex) {
        this.namespaceIndex = namespaceIndex;
    }
}