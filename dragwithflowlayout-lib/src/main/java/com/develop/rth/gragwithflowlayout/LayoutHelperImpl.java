package com.develop.rth.gragwithflowlayout;

import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rth on 16/11/19.
 */
public class LayoutHelperImpl implements ILayoutHelper {

    //当顶部的View被回收时，记录对应数据的布局信息，因为逆序布局时不能重新计算
    private SparseArray<LineItemPosRecord> preLayoutedViews = new SparseArray<>();
    //顶部View被回收时，需要创建Rect保存布局信息，改过程很频繁，需要对象池优化
    private Pools.SimplePool<LineItemPosRecord> rectSimplePool;
    //临时记录即将被回收的View
    private List<View> pendingRecycleView = new ArrayList<>();
    //一行中View的个数，且是所有行中最大的
    private int maxLineNumbser = Integer.MIN_VALUE;

    @Override
    public void layoutARow(List<View> views, RecyclerView.Recycler recycler, FlowDragLayoutManager layoutManager, boolean isLastRow) {
        final int alignMode = layoutManager.getLayoutInfo().alignMode;
        switch (alignMode) {
            case FlowDragLayoutConstant.LEFT:
                alignLeftLayout(views, layoutManager, recycler);
                break;
            case FlowDragLayoutConstant.TWO_SIDE:
                alignTwoSideLayout(views, layoutManager, isLastRow, recycler);
                break;
            case FlowDragLayoutConstant.RIGHT:
                alignRightLayout(views, layoutManager, recycler);
                break;
            case FlowDragLayoutConstant.CENTER:
                alignCenterLayout(views, layoutManager, recycler);
                break;
        }

        if (layoutManager.getLayoutInfo().layoutByScroll
                || (!layoutManager.getLayoutInfo().layoutByScroll && !layoutManager.getLayoutInfo().justCalculate)) {
            final View last = views.get(views.size() - 1);
            layoutManager.getLayoutInfo().layoutAnchor = layoutManager.getViewBottomWithMargin(last);
        }

        if (views.size() > maxLineNumbser) {
            maxLineNumbser = views.size();
            recycler.setViewCacheSize(maxLineNumbser);
        }
        views.clear();
    }

    /**
     * 布局时向中对齐
     */
    private void alignCenterLayout(List<View> views, FlowDragLayoutManager layoutManager, RecyclerView.Recycler recycler) {
        //计算向右的偏移量
        int totalWidth = 0;
        for (View view : views) {
            totalWidth += layoutManager.getWidthWithMargins(view);
        }
        int rest = layoutManager.getContentHorizontalSpace() - totalWidth;
        int rightOffset = rest / 2;

        //开始布局
        int xOffset = layoutManager.getPaddingLeft() + rightOffset;
        int heightSpace = 0;
        for (int i = 0; i < views.size(); i++) {
            final  View view = views.get(i);
            final int widthSpace = layoutManager.getWidthWithMargins(view);
            heightSpace = layoutManager.getHeightWithMargins(view);

            int l = xOffset;
            int t = layoutManager.getLayoutInfo().layoutAnchor;
            int r = l + widthSpace;
            int b = t + heightSpace;

            realLayoutItem(l, t, r, b, view, layoutManager, recycler, i == 0);

            xOffset = r;
        }
    }

    /**
     * 布局时向右对齐
     */
    private void alignRightLayout(List<View> views, FlowDragLayoutManager layoutManager, RecyclerView.Recycler recycler) {
        int xOffset = layoutManager.getWidth() - layoutManager.getPaddingRight();
        int heightSpace = 0;
        for (int i = views.size() - 1; i >= 0; i--) {
            final  View view = views.get(i);
            final int widthSpace = layoutManager.getWidthWithMargins(view);
            heightSpace = layoutManager.getHeightWithMargins(view);

            int l = xOffset - widthSpace;
            int t = layoutManager.getLayoutInfo().layoutAnchor;
            int r = xOffset;
            int b = t + heightSpace;

            realLayoutItem(l, t, r, b, view, layoutManager, recycler, i == 0);

            xOffset = l;
        }
    }

    /**
     * 布局时两边对齐
     */
    private void alignTwoSideLayout(List<View> views, FlowDragLayoutManager layoutManager, boolean isLastRow, RecyclerView.Recycler recycler) {
        final FlowDragLayoutManager.LayoutInfo layoutInfo = layoutManager.getLayoutInfo();
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

        //开始布局
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

            realLayoutItem(l, t, r, b, view, layoutManager, recycler, i == 0);

            xOffset = r + interval;
        }
    }

    /**
     * 布局时向左对齐
     */
    private void alignLeftLayout(List<View> views, FlowDragLayoutManager layoutManager, RecyclerView.Recycler recycler) {
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

            realLayoutItem(l, t, r, b, view, layoutManager, recycler, i == 0);

            xOffset = r;
        }
    }

    @Override
    public void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragLayoutManager layoutManager) {
        final FlowDragLayoutManager.LayoutInfo layoutInfo = layoutManager.getLayoutInfo();
//        DebugUtil.debugFormat("FlowDragLayoutManager layout reverse start:%s, anchor:%s",layoutInfo.startLayoutPos,layoutInfo.layoutAnchor);
        for (int i = layoutInfo.startLayoutPos; i >= 0;i--) {
            LineItemPosRecord lineItemPosRecord = preLayoutedViews.get(i);
            Rect rect = lineItemPosRecord.rect;
            int heightSpace = rect.bottom - rect.top;

//            DebugUtil.debugFormat("FlowDragLayoutManager layout anchor:%s distance:%s",layoutInfo.layoutAnchor, layoutInfo.pendingScrollDistance);
            if (layoutInfo.layoutAnchor + layoutInfo.pendingScrollDistance <= layoutManager.getPaddingTop()) {
//                DebugUtil.debugFormat("FlowDragLayoutManager layout reverse over:%s",i);
                break;
            }

            final View view = recycler.getViewForPosition(i);
            layoutManager.addView(view,0);
            layoutManager.measureChildWithMargins(view,0,0);
            int l = rect.left, t = layoutInfo.layoutAnchor - heightSpace, r = rect.right, b = layoutInfo.layoutAnchor;
            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            if (lineItemPosRecord.isFirstItemInLine) {
                layoutInfo.layoutAnchor -= heightSpace;
            }

            releaseItemLayoutInfo(lineItemPosRecord);
            preLayoutedViews.remove(i);
        }
//        DebugUtil.debugFormat("FlowDragLayoutManager finish reverse preLayoutedViews:%s",preLayoutedViews.size());
    }

    private void realLayoutItem(int l, int t, int r, int b, View view, FlowDragLayoutManager layoutManager, RecyclerView.Recycler recycler, boolean isFirstItemInARow) {
        final FlowDragLayoutManager.LayoutInfo layoutInfo = layoutManager.getLayoutInfo();
        if (layoutInfo.layoutByScroll) {
            DebugUtil.debugFormat("FlowDragLayoutManager realLayoutItem layoutOutByScroll");
            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);
        }else {
            if (layoutInfo.justCalculate) {
                LineItemPosRecord lineItemPosRecord = generateALineItem(layoutManager);
                lineItemPosRecord.setFirstItemInLine(isFirstItemInARow);
                lineItemPosRecord.rect.set(l, t, r, b);
                preLayoutedViews.put(layoutManager.getPosition(view), lineItemPosRecord);
                layoutManager.removeAndRecycleView(view, recycler);
                DebugUtil.debugFormat("FlowDragLayoutManager realLayoutItem put %s into preLayoutedViews, then preLayoutedViews size is %s", layoutManager.getPosition(view), preLayoutedViews.size());
            }else {
                layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);
                DebugUtil.debugFormat("FlowDragLayoutManager realLayoutItem layout %s without scroll", layoutManager.getPosition(view));
            }
        }
    }

    /**
     * 回收Rect对象再利用
     */
    private void releaseItemLayoutInfo(LineItemPosRecord rect) {
        try {
            rectSimplePool.release(rect);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void recycleUnvisibleViews(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragLayoutManager flowDragLayoutManager) {
        if (flowDragLayoutManager.getChildCount() == 0) return;
        final FlowDragLayoutManager.LayoutInfo layoutInfo = flowDragLayoutManager.getLayoutInfo();
        if (layoutInfo.pendingScrollDistance < 0) {
            return;
        }
        int top = Integer.MAX_VALUE;
        if (layoutInfo.layoutFrom == FlowDragLayoutManager.LayoutFrom.DOWN_TO_UP) {
            //回收底部不可见的View
            for (int i = flowDragLayoutManager.getChildCount() - 1; i >= 0; i--) {
                final View view = flowDragLayoutManager.getChildAt(i);
                int afterScrollTop = flowDragLayoutManager.getViewTopWithMargin(view) + layoutInfo.pendingScrollDistance;
                if (afterScrollTop >= flowDragLayoutManager.getHeight() - flowDragLayoutManager.getPaddingBottom()) {
                    pendingRecycleView.add(view);
                }else {
                    break;
                }
            }
        }else if (layoutInfo.layoutFrom == FlowDragLayoutManager.LayoutFrom.UP_TO_DOWN){
            //回收顶部不可见的View
            for (int i = 0; i < flowDragLayoutManager.getChildCount(); i++) {
                final View view = flowDragLayoutManager.getChildAt(i);
                int afterScrollBottom = flowDragLayoutManager.getViewBottomWithMargin(view) - layoutInfo.pendingScrollDistance;
                if (afterScrollBottom <= flowDragLayoutManager.getPaddingTop()) {
                    final int viewTop = flowDragLayoutManager.getViewTopWithMargin(view);
                    if (viewTop != top) {
                        saveLayoutInfo(view, flowDragLayoutManager, true);
                        top = viewTop;
                    }else {
                        saveLayoutInfo(view, flowDragLayoutManager, false);
                    }

                    pendingRecycleView.add(view);
                }else {
                    break;
                }
            }
        }

        if (pendingRecycleView.size() > 0) {
            DebugUtil.debugFormat("FlowDragLayoutManager recycle %s views", pendingRecycleView.size());
        }

        for (View view : pendingRecycleView) {
            flowDragLayoutManager.removeAndRecycleView(view, recycler);
        }

        pendingRecycleView.clear();
    }

    @Override
    public void willCalculateUnVisibleViews() {
        //需要重新计算之前没有显示的View的布局信息
//        DebugUtil.debugFormat("FlowDragLayoutManager layoutARow, not scroll preLayoutedViews.size:%s", preLayoutedViews.size());
        for (int i = 0;i < preLayoutedViews.size(); i++) {
            LineItemPosRecord record = preLayoutedViews.get(i, null);
            if (record != null) {
                releaseItemLayoutInfo(record);
            }
        }
        preLayoutedViews.clear();
    }

    /**
     * 当向上滑动时，记录从顶部回收的View的布局信息，因为往回滑的时候不能重新计算
     */
    private void saveLayoutInfo(View view, FlowDragLayoutManager layoutManager, boolean isFirstItemInLine) {
        LineItemPosRecord out = generateALineItem(layoutManager);
        out.setFirstItemInLine(isFirstItemInLine);
        layoutManager.getDecoratedBoundsWithMargins(view, out.rect);
        preLayoutedViews.put(layoutManager.getPosition(view), out);
    }

    private LineItemPosRecord generateALineItem(FlowDragLayoutManager layoutManager) {
        if (rectSimplePool == null) {
            rectSimplePool = new Pools.SimplePool<>(layoutManager.getChildCount());
        }
        LineItemPosRecord out = rectSimplePool.acquire();
        if (out == null) {
//            DebugUtil.debugFormat("FlowDragLayoutManager out come from new");
            out = new LineItemPosRecord();
        }else {
//            DebugUtil.debugFormat("FlowDragLayoutManager out come from pool");
        }
        return out;
    }

    private static final class LineItemPosRecord {

        Rect rect = new Rect();
        boolean isFirstItemInLine;

        LineItemPosRecord() {

        }

        void setFirstItemInLine(boolean firstItemInLine) {
            isFirstItemInLine = firstItemInLine;
        }
    }
}

