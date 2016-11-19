package com.example.rth.assembleessay.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

/**
 * Created by rth on 16/11/19.
 * 进行布局的帮助类
 */
public interface ILayoutHelper {

    /**
     * 布局一行
     */
    void layoutARow(List<View> views, RecyclerView.Recycler recycler, FlowDragableLayoutManager layoutManager, boolean isLastRow);

    /**
     * 逆序布局View
     */
    void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragableLayoutManager layoutManager);
}
