package io.github.vipcxj.jasync.ng.utils.hack.dummy;

import java.io.OutputStream;

@SuppressWarnings("all")
public class Parent {
    boolean first;
    static final Object staticObj = OutputStream.class;
    volatile Object second;
    private static volatile boolean staticSecond;
    private static volatile boolean staticThird;
}