package com.develop.rth.gragwithflowlayout;

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
    void layoutARow(List<View> views, RecyclerView.Recycler recycler, FlowDragLayoutManager layoutManager, boolean isLastRow);

    /**
     * 逆序布局View
     */
    void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragLayoutManager layoutManager);

    /**
     * 回收不可见的View
     */
    void recycleUnvisibleViews(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragLayoutManager flowDragLayoutManager);

    /**
     * 重新计算之前不可见布局的布局信息
     */
    void willCalculateUnVisibleViews();
}
