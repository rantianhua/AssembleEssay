package com.example.rth.assembleessay.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.example.rth.assembleessay.util.DebugUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rth on 16/11/19.
 */
public class LayoutHelperImpl implements ILayoutHelper {

    private SparseArray<LineChunkRecord> preLayoutedViews = new SparseArray<>();
    List<View> pendingRecycleView = new ArrayList<>();

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
            interval = rest / (views.size() - 1);
        }

        int xOffset = layoutManager.getPaddingLeft();
        int heightSpace = 0;
        for (int i = 0; i < views.size(); i++) {
            final  View view = views.get(i);
            final int widthSpace = layoutManager.getWidthWithMargins(view);
            heightSpace = layoutManager.getHeightWithMargins(view);

            int l = xOffset;
            int t = layoutManager.getLayoutInfo().layoutAnchor;
            int r = l + widthSpace;
            int b = t + heightSpace;

            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            xOffset = r + interval;

            LineChunkRecord chunkRecord = new LineChunkRecord();
            chunkRecord.getRect().set(l,t,r,b);
            chunkRecord.setInterval(interval);
            preLayoutedViews.put(layoutManager.getPosition(view),chunkRecord);
        }

        layoutManager.getLayoutInfo().layoutAnchor += heightSpace;
        views.clear();
    }

    @Override
    public void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragableLayoutManager layoutManager) {
        final FlowDragableLayoutManager.LayoutInfo layoutInfo = layoutManager.getLayoutInfo();
        DebugUtil.debugFormat("FlowDragableLayoutManager layout reverse start:%s, anchor:%s",layoutInfo.startLayoutPos,layoutInfo.layoutAnchor);
        for (int i = layoutInfo.startLayoutPos; i >= 0;i--) {
            LineChunkRecord lineChunkRecord = preLayoutedViews.get(i);
            Rect rect = lineChunkRecord.getRect();
            int heightSpace = rect.bottom - rect.top;

            DebugUtil.debugFormat("FlowDragableLayoutManager layout anchor:%s distance:%s",layoutInfo.layoutAnchor, layoutInfo.pendingScrollDistance);
            if (layoutInfo.layoutAnchor + layoutInfo.pendingScrollDistance <= layoutManager.getPaddingTop()) {
                DebugUtil.debugFormat("FlowDragableLayoutManager layout reverse over:%s",i);
                break;
            }

            final View view = recycler.getViewForPosition(i);
            layoutManager.addView(view,0);
            layoutManager.measureChildWithMargins(view,0,0);

            int l = rect.left, t = layoutInfo.layoutAnchor - heightSpace, r = rect.right, b = layoutInfo.layoutAnchor;
            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            if (rect.left == layoutManager.getPaddingLeft()) {
                //该行的第一个，也意味着这一行完成了
                DebugUtil.debugFormat("FlowDragableLayoutManager finish a row:%s",((TextView)view).getText());
                layoutInfo.layoutAnchor -= heightSpace;
            }
        }
    }

    @Override
    public void recycleUnvisibleViews(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragableLayoutManager flowDragableLayoutManager) {
        if (flowDragableLayoutManager.getChildCount() == 0) return;
        final FlowDragableLayoutManager.LayoutInfo layoutInfo = flowDragableLayoutManager.getLayoutInfo();

        if (layoutInfo.layoutFrom == FlowDragableLayoutManager.LayoutFrom.DOWN_TO_UP) {
            //回收底部不可见的View
            for (int i = flowDragableLayoutManager.getChildCount() - 1; i >= 0; i--) {
                final View view = flowDragableLayoutManager.getChildAt(i);
                int afterScrollTop = flowDragableLayoutManager.getViewTopWithMargin(view) + layoutInfo.pendingScrollDistance;
                if (afterScrollTop >= flowDragableLayoutManager.getHeight() - flowDragableLayoutManager.getPaddingBottom()) {
                    //回收
                    pendingRecycleView.add(view);
                }else {
                    break;
                }
            }
        }else {
            //回收顶部不可见的View
            for (int i = 0; i < flowDragableLayoutManager.getChildCount(); i++) {
                final View view = flowDragableLayoutManager.getChildAt(i);
                int afterScrollBottom = flowDragableLayoutManager.getViewBottomWithMargin(view) - layoutInfo.pendingScrollDistance;
                if (afterScrollBottom <= flowDragableLayoutManager.getPaddingTop()) {
                    //回收
                    pendingRecycleView.add(view);
                }else {
                    break;
                }
            }
        }

        if (pendingRecycleView.size() > 0) {
            DebugUtil.debugFormat("FlowDragableLayoutManager recycle %s views", pendingRecycleView.size());
        }

        for (View view : pendingRecycleView) {
            DebugUtil.debugFormat("FlowDragableLayoutManager recycle %s", flowDragableLayoutManager.getPosition(view));
            flowDragableLayoutManager.removeAndRecycleView(view, recycler);
        }

        pendingRecycleView.clear();
    }

}

