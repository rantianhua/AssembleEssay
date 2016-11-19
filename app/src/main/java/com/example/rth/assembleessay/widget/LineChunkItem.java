package com.example.rth.assembleessay.widget;

import android.view.View;

/**
 * Created by rth on 16/11/19.
 */
public class LineChunkItem {

    //待布局的View
    private View view;

    private int top;

    private int left;

    private int right;

    private int bottom;

    public LineChunkItem() {

    }

    public View getView() {
        return view;
    }

    public LineChunkItem setView(View view) {
        this.view = view;
        return this;
    }

    public int getTop() {
        return top;
    }

    public LineChunkItem setTop(int top) {
        this.top = top;
        return this;
    }

    public int getLeft() {
        return left;
    }

    public LineChunkItem setLeft(int left) {
        this.left = left;
        return this;
    }

    public int getRight() {
        return right;
    }

    public LineChunkItem setRight(int right) {
        this.right = right;
        return this;
    }

    public int getBottom() {
        return bottom;
    }

    public LineChunkItem setBottom(int bottom) {
        this.bottom = bottom;
        return this;
    }
}
