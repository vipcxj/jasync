package io.github.vipcxj.jasync.ng.runtime.utils;

public class CommonUtils {

    public static int normalCapacity(int input) {
        if (input <= 0b10) {
            return 0b10;
        } else if (input <= 0b100) {
            return 0b100;
        } else if (input <= 0b1000) {
            return 0b1000;
        } else if (input <= 0b10000) {
            return 0b10000;
        } else if (input <= 0b100000) {
            return 0b100000;
        } else if (input <= 0b1000000) {
            return 0b1000000;
        } else if (input <= 0b10000000) {
            return 0b10000000;
        } else if (input <= 0b100000000) {
            return 0b100000000;
        } else if (input <= 0b1000000000) {
            return 0b1000000000;
        } else if (input <= 0b10000000000) {
            return 0b10000000000;
        } else if (input <= 0b100000000000) {
            return 0b100000000000;
        } else if (input <= 0b1000000000000) {
            return 0b1000000000000;
        } else if (input <= 0b10000000000000) {
            return 0b10000000000000;
        } else if (input <= 0b100000000000000) {
            return 0b100000000000000;
        } else if (input <= 0b1000000000000000) {
            return 0b1000000000000000;
        } else if (input <= 0b10000000000000000) {
            return 0b10000000000000000;
        } else if (input <= 0b100000000000000000) {
            return 0b100000000000000000;
        } else if (input <= 0b1000000000000000000) {
            return 0b1000000000000000000;
        } else if (input <= 0b10000000000000000000) {
            return 0b10000000000000000000;
        } else if (input <= 0b100000000000000000000) {
            return 0b100000000000000000000;
        } else if (input <= 0b1000000000000000000000) {
            return 0b1000000000000000000000;
        } else if (input <= 0b10000000000000000000000) {
            return 0b10000000000000000000000;
        } else if (input <= 0b100000000000000000000000) {
            return 0b100000000000000000000000;
        } else if (input <= 0b1000000000000000000000000) {
            return 0b1000000000000000000000000;
        } else if (input <= 0b10000000000000000000000000) {
            return 0b10000000000000000000000000;
        } else if (input <= 0b100000000000000000000000000) {
            return 0b100000000000000000000000000;
        } else if (input <= 0b1000000000000000000000000000) {
            return 0b1000000000000000000000000000;
        } else if (input <= 0b10000000000000000000000000000) {
            return 0b10000000000000000000000000000;
        } else if (input <= 0b100000000000000000000000000000) {
            return 0b100000000000000000000000000000;
        } else if (input <= 0b1000000000000000000000000000000) {
            return 0b1000000000000000000000000000000;
        } else {
            throw new IllegalArgumentException("The capacity is too big.");
        }
    }
}
