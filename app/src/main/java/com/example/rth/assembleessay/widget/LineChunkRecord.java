package com.example.rth.assembleessay.widget;

import android.graphics.Rect;

/**
 * Created by rth on 16/11/19.
 * 记录一行中一个单词或短语的布局信息
 */
public class LineChunkRecord {


    private Rect rect;

    public LineChunkRecord() {
        rect = new Rect();
    }

    //行内行间距
    private int interval;

    public Rect getRect() {
        return rect;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
