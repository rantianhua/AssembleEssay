package com.example.rth.assembleessay.widget;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import java.util.List;

/**
 * Created by rth on 16/11/19.
 */
public class LayoutHelperImpl implements ILayoutHelper {

    private SparseArray<LineChunkRecord> preLayoutedViews = new SparseArray<>();

    @Override
    public void layoutARow(List<View> views, RecyclerView.Recycler recycler, FlowDragableLayoutManager layoutManager, boolean isLastRow) {
        //计算行内间距
        int interval = 0;
        if (views.size() > 1 && !isLastRow) {
            int totalWidth = 0;
            for (View view : views) {
                totalWidth += layoutManager.getWidthWithMargins(view);
            }
            int rest = layoutManager.getContentHorizontalSpace() - totalWidth;
            interval = rest / views.size();
        }

        int xOffset = layoutManager.getPaddingLeft();
        boolean haveIncrementLayoutAnchor = false;
        for (int i = 0; i < views.size(); i++) {
            final  View view = views.get(i);
            final int widthSpace = layoutManager.getWidthWithMargins(view);
            final int heightSpace = layoutManager.getHeightWithMargins(view);

            int l = xOffset;
            int t = layoutManager.getLayoutInfo().layoutAnchor;
            int r = l + widthSpace;
            int b = t + heightSpace;

            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            xOffset += widthSpace;
            if (i != views.size() - 1) {
                xOffset += interval;
            }

            if (!haveIncrementLayoutAnchor) {
                haveIncrementLayoutAnchor = true;
                layoutManager.getLayoutInfo().layoutAnchor += heightSpace;
            }

            LineChunkRecord chunkRecord = new LineChunkRecord();
            chunkRecord.getRect().set(l,t,r,b);
            chunkRecord.setInterval(interval);
            preLayoutedViews.put(layoutManager.getPosition(view),chunkRecord);
        }

        views.clear();
    }

    @Override
    public void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragableLayoutManager layoutManager) {

    }

}
