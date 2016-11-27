package com.develop.rth.gragwithflowlayout;

import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

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
                alignLeftLayout(views, layoutManager);
                break;
            case FlowDragLayoutConstant.TWO_SIDE:
                alignTwoSideLayout(views, layoutManager, isLastRow);
                break;
            case FlowDragLayoutConstant.RIGHT:
                alignRightLayout(views, layoutManager);
                break;
            case FlowDragLayoutConstant.CENTER:
                alignCenterLayout(views, layoutManager);
                break;
        }

        final View last = views.get(views.size() - 1);
        layoutManager.getLayoutInfo().layoutAnchor = layoutManager.getViewBottomWithMargin(last);

        if (views.size() > maxLineNumbser) {
            maxLineNumbser = views.size();
            recycler.setViewCacheSize(maxLineNumbser);
        }
        views.clear();
    }

    /**
     * 布局时向中对齐
     */
    private void alignCenterLayout(List<View> views, FlowDragLayoutManager layoutManager) {
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

            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            xOffset = r;
        }
    }

    /**
     * 布局时向右对齐
     */
    private void alignRightLayout(List<View> views, FlowDragLayoutManager layoutManager) {
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

            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            xOffset = l;
        }
    }

    /**
     * 布局时两边对齐
     */
    private void alignTwoSideLayout(List<View> views, FlowDragLayoutManager layoutManager, boolean isLastRow) {
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

            layoutManager.layoutDecoratedWithMargins(view, l, t, r, b);

            xOffset = r + interval;
        }
    }

    /**
     * 布局时向左对齐
     */
    private void alignLeftLayout(List<View> views, FlowDragLayoutManager layoutManager) {
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

            xOffset = r;
        }
    }

    @Override
    public void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state, FlowDragLayoutManager layoutManager) {
        final FlowDragLayoutManager.LayoutInfo layoutInfo = layoutManager.getLayoutInfo();
        DebugUtil.debugFormat("FlowDragLayoutManager layout reverse start:%s, anchor:%s",layoutInfo.startLayoutPos,layoutInfo.layoutAnchor);
        for (int i = layoutInfo.startLayoutPos; i >= 0;i--) {
            LineItemPosRecord lineItemPosRecord = preLayoutedViews.get(i);
            Rect rect = lineItemPosRecord.rect;
            int heightSpace = rect.bottom - rect.top;

            DebugUtil.debugFormat("FlowDragLayoutManager layout anchor:%s distance:%s",layoutInfo.layoutAnchor, layoutInfo.pendingScrollDistance);
            if (layoutInfo.layoutAnchor + layoutInfo.pendingScrollDistance <= layoutManager.getPaddingTop()) {
                DebugUtil.debugFormat("FlowDragLayoutManager layout reverse over:%s",i);
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

            releaseItemLayoutInfo(i, lineItemPosRecord);
        }
        DebugUtil.debugFormat("FlowDragLayoutManager finish reverse preLayoutedViews:%s",preLayoutedViews.size());
    }

    /**
     * 回收Rect对象再利用
     */
    private void releaseItemLayoutInfo(int pos, LineItemPosRecord rect) {
        preLayoutedViews.remove(pos);
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
            DebugUtil.debugFormat("FlowDragLayoutManager no need to recycle dy:%s", layoutInfo.pendingScrollDistance);
            return;
        }
        DebugUtil.debugFormat("FlowDragLayoutManager start to recycle:%s", layoutInfo.pendingScrollDistance);
        int top = Integer.MAX_VALUE;
        if (layoutInfo.layoutFrom == FlowDragLayoutManager.LayoutFrom.DOWN_TO_UP) {
            //回收底部不可见的View
            for (int i = flowDragLayoutManager.getChildCount() - 1; i >= 0; i--) {
                final View view = flowDragLayoutManager.getChildAt(i);
                int afterScrollTop = flowDragLayoutManager.getViewTopWithMargin(view) + layoutInfo.pendingScrollDistance;
                if (afterScrollTop >= flowDragLayoutManager.getHeight() - flowDragLayoutManager.getPaddingBottom()) {
                    //回收
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
                    //回收
                    final int viewTop = flowDragLayoutManager.getViewTopWithMargin(view);
                    if (viewTop != top) {
                        saveLayoutInfo(view, flowDragLayoutManager, true);
                        top = viewTop;
                    }else {
                        saveLayoutInfo(view, flowDragLayoutManager, false);
                    }

                    pendingRecycleView.add(view);
                }else {
                    DebugUtil.debugFormat("FlowDragLayoutManager finish recycle preLayoutedViews:%s",preLayoutedViews.size());
                    break;
                }
            }
        }

        if (pendingRecycleView.size() > 0) {
            DebugUtil.debugFormat("FlowDragLayoutManager recycle %s views", pendingRecycleView.size());
        }

        for (View view : pendingRecycleView) {
            DebugUtil.debugFormat("FlowDragLayoutManager recycle %s", flowDragLayoutManager.getPosition(view));
            flowDragLayoutManager.removeAndRecycleView(view, recycler);
        }

        pendingRecycleView.clear();
    }

    /**
     * 当向上滑动时，记录从顶部回收的View的布局信息，因为往回滑的时候不能重新计算
     */
    private void saveLayoutInfo(View view, FlowDragLayoutManager layoutManager, boolean isFirstItemInLine) {
        if (rectSimplePool == null) {
            rectSimplePool = new Pools.SimplePool<>(layoutManager.getChildCount());
        }
        LineItemPosRecord out = rectSimplePool.acquire();
        if (out == null) {
            DebugUtil.debugFormat("FlowDragLayoutManager out come from new");
            out = new LineItemPosRecord();
        }else {
            DebugUtil.debugFormat("FlowDragLayoutManager out come from pool");
        }
        out.setFirstItemInLine(isFirstItemInLine);
        layoutManager.getDecoratedBoundsWithMargins(view, out.rect);
        preLayoutedViews.put(layoutManager.getPosition(view), out);
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

