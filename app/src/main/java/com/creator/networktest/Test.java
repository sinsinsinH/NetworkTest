package com.creator.networktest;

public class Test {
    public static void main(String[] args) {
        System.out.println(getDegree(8192));
    }
    private static int getDegree(double cur_speed) {
        int ret = 0;
        if (cur_speed >= 0 && cur_speed <= 512) {
            ret = (int) (cur_speed / 512.0 * 60);
        } else if (cur_speed >= 512 && cur_speed <= 1024) {
            ret = (int) (60 + cur_speed / 1024.0 * 30);
        } else if (cur_speed >= 1024 && cur_speed <= 5 * 1024) {
            ret = (int) (90 + cur_speed / (1024.0 * 5) * 60);
        } else if (cur_speed >= 5 * 1024 && cur_speed <= 10 * 1024) {
            ret = (int) (150 + cur_speed / (1024.0 * 10) * 30);
        } else {
            ret = 180;
        }
        return ret;
    }
}
