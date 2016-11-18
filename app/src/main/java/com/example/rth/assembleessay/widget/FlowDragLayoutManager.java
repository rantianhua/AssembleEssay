package com.example.rth.assembleessay.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.rth.assembleessay.util.DebugUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rth on 16/11/15.
 */
public class FlowDragLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = "FlowDragLayoutManager";
    //记录要回收的View
    private List<View> recycleHeap = new ArrayList<>();
    //一行中View的个数，并且是所有行中最大的
    private int maxLineNumbers;
    private LayoutInfo layoutInfo;
    private ChildViewHelper childViewHelper;

    public FlowDragLayoutManager() {
        layoutInfo = new LayoutInfo();
        childViewHelper = new ChildViewHelper();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        detachAndScrapAttachedViews(recycler);
        startLayout(recycler, state, 0);
    }

    /**
     * 进行布局的方法
     * @param dy 拖动的偏移量
     * @return 修正后的偏移量
     */
    private int startLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        resetLayoutInfo();
        if (dy == 0) {
            layoutWithOutScroll(recycler);
            return 0;
        }
        View firstView = null;
        if (dy > 0) {
            //向上拖动,从底部添加视图
            final View lastVisibleView = childViewHelper.getLastVisibleView();
            if (childViewHelper.getViewBottomWithMargin(lastVisibleView) - dy < childViewHelper.getContainerBottom()) {
                layoutInfo.verticalOffset = childViewHelper.getViewBottomWithMargin(lastVisibleView);
                //需要从底部添加新的View
                for (int i = getPosition(lastVisibleView) + 1; i <= getItemCount() - 1; i++) {
                    final View child = recycler.getViewForPosition(i);
                    addView(child);
                    measureChildWithMargins(child, 0, 0);
                    final int childWidthSpace = childViewHelper.getWidthWithMargins(child);
                    if (layoutInfo.horizontalOffset + childWidthSpace <= childViewHelper.getContainerHorizontalSpace()) {
                        //一行未满
                        layoutInfo.pendingLayoutViews.add(child);
                        layoutInfo.horizontalOffset += childWidthSpace;
                        if (i == getItemCount() - 1) {
                            layoutARow(true,recycler);
                        }
                    } else {
                        //换行
                        layoutARow(false,recycler);
                        if (layoutInfo.verticalOffset - dy >= childViewHelper.getContainerBottom()) {
                            //越界,不用再布局
                            removeAndRecycleView(child, recycler);
                            break;
                        }
                        removeAndRecycleView(child,recycler);
                        layoutInfo.horizontalOffset = getPaddingLeft();
                        i--;
                    }
                }
            }
        } else {
            //向下拖动,从顶部添加视图
            firstView = childViewHelper.getFirstVisibleView();
            if (childViewHelper.getViewTopWithMargin(firstView) - dy > getPaddingTop()) {
                //需要从顶部添加View
                for (int i = getPosition(firstView) - 1; i >= 0; i--) {
                    Rect rect = layoutInfo.preLayoutedViewPositions.get(i);
                    if (rect.bottom - layoutInfo.scrollOffset - dy < getPaddingTop()) {
                        //越界,不画
                        break;
                    } else {
                        layoutInfo.startPos = i;
                        View child = recycler.getViewForPosition(i);
                        addView(child, 0);
                        measureChildWithMargins(child, 0, 0);
                        layoutDecoratedWithMargins(child,rect.left, rect.top - layoutInfo.scrollOffset, rect.right, rect.bottom - layoutInfo.scrollOffset);
                    }
                }
            }
        }
        firstView = childViewHelper.getFirstVisibleView();
        layoutInfo.startPos = getPosition(firstView);
        layoutInfo.firstRowVerticalOffset = childViewHelper.getViewTopWithMargin(firstView);
        DebugUtil.debugFormat("%s onLayoutChildren startLayout startPos:%s totalCount:%s firstRowVerticalOffset:%s",TAG, layoutInfo.startPos, getChildCount(), layoutInfo.firstRowVerticalOffset);
        return dy;
    }

    private void resetLayoutInfo() {
        layoutInfo.verticalOffset = layoutInfo.firstRowVerticalOffset;
        layoutInfo.horizontalOffset = getPaddingLeft();
    }

    /**
     * 非拖动情况下的布局
     */
    private void layoutWithOutScroll(RecyclerView.Recycler recycler) {
        for (int i = layoutInfo.startPos;i < getItemCount();i++) {
            final View child = recycler.getViewForPosition(i);
            addView(child);
            measureChildWithMargins(child, 0, 0);
            final int childWidthSpace = childViewHelper.getWidthWithMargins(child);
            if (layoutInfo.horizontalOffset + childWidthSpace <= childViewHelper.getContainerHorizontalSpace()) {
                //同一行布局
                layoutInfo.pendingLayoutViews.add(child);
                layoutInfo.horizontalOffset += childWidthSpace;
                if (i == getItemCount() - 1) {
                    layoutARow(true, recycler);
                }
            }else {
                //换行布局
                layoutARow(false,recycler);
                if (layoutInfo.verticalOffset > childViewHelper.getContainerBottom()) {
                    //越界
                    removeAndRecycleView(child,recycler);
                    break;
                }else {
                    layoutInfo.horizontalOffset = getPaddingLeft();
                    removeAndRecycleView(child,recycler);
                    i--;
                }
            }
        }
        layoutInfo.firstRowVerticalOffset = childViewHelper.getViewTopWithMargin(childViewHelper.getFirstVisibleView());
        DebugUtil.debugFormat("%s onLayoutChildren startLayout startPos:%s totalCount:%s firstRowVerticalOffset:%s",TAG, layoutInfo.startPos, getChildCount(), layoutInfo.firstRowVerticalOffset);
    }


    /**
     * 拖动过程中回收越界的View
     */
    private void recycleOutOfIndexView(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        if (dy > 0) {
            //向上滑动,回收上越界的View
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                final int afterScrollBottom = childViewHelper.getViewBottomWithMargin(child) - dy;
                if (afterScrollBottom <= getPaddingTop() ) {
                    //滑动之后不可见,回收掉
                    recycleHeap.add(child);
                }else {
                    break;
                }
            }
        } else if (dy < 0) {
            //向下滑动,回收下越界的View
            for (int i = getChildCount() - 1;i >= 0; i--) {
                final View child = getChildAt(i);
                final int afterScrollTop = childViewHelper.getViewTopWithMargin(child) - dy;
                if (afterScrollTop >= getHeight() - getPaddingBottom()) {
                    //不可见,回收
                    recycleHeap.add(child);
                }else {
                    break;
                }
            }
        }

        for (final View view : recycleHeap) {
            removeAndRecycleView(view,recycler);
        }
        if(recycleHeap.size() > 0) {
            DebugUtil.debugFormat("%s about view recycle %s views",TAG, recycleHeap.size());
        }
        recycleHeap.clear();
    }

    /**
     * 布局一行的View
     *
     * @param isLastRow 是否最后一行
     */
    private void layoutARow(boolean isLastRow, RecyclerView.Recycler recycler) {
        int rowItems = layoutInfo.pendingLayoutViews.size();
        if (rowItems == 0) return;
        int viewWidthSpace = 0;
        for (View rowView : layoutInfo.pendingLayoutViews) {
            viewWidthSpace += childViewHelper.getWidthWithMargins(rowView);
        }
        int restSpace = childViewHelper.getContainerHorizontalSpace() - viewWidthSpace;
        //计算每一个Item之间的间隔
        int horizontalInterval = 0;
        if (rowItems > 1) {
            horizontalInterval = isLastRow ? 0 : restSpace / (rowItems - 1);
        }
        int itemHeightSpace = 0;
        int offsetX = getPaddingLeft();
        for (int j = 0; j < rowItems; j++) {
            final View view = layoutInfo.pendingLayoutViews.get(j);
            itemHeightSpace = childViewHelper.getHeightWithMargins(view);
            final int itemWidthSpace = childViewHelper.getWidthWithMargins(view);
            Rect rect = new Rect();
            if (j == rowItems - 1) {
                layoutDecoratedWithMargins(view,
                        offsetX,
                        layoutInfo.verticalOffset,
                        offsetX + itemWidthSpace,
                        layoutInfo.verticalOffset + itemHeightSpace);
                rect.set(offsetX,
                        layoutInfo.verticalOffset + layoutInfo.scrollOffset,
                        offsetX + itemWidthSpace,
                        layoutInfo.verticalOffset + itemHeightSpace + layoutInfo.scrollOffset);
            } else {
                layoutDecoratedWithMargins(view,
                        offsetX,
                        layoutInfo.verticalOffset,
                        offsetX + itemWidthSpace,
                        layoutInfo.verticalOffset + itemHeightSpace);
                rect.set(offsetX,
                        layoutInfo.verticalOffset + layoutInfo.scrollOffset,
                        offsetX + itemWidthSpace,
                        layoutInfo.verticalOffset + itemHeightSpace + layoutInfo.scrollOffset);
                offsetX += itemWidthSpace + horizontalInterval;
            }
            layoutInfo.preLayoutedViewPositions.put(getPosition(view), rect);
        }
        layoutInfo.verticalOffset += itemHeightSpace;
        if (layoutInfo.pendingLayoutViews.size() > maxLineNumbers) {
            maxLineNumbers = layoutInfo.pendingLayoutViews.size();
            recycler.setViewCacheSize(maxLineNumbers);
        }
        layoutInfo.pendingLayoutViews.clear();
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (dy == 0) return 0;
        if (getChildCount() == 0) return 0;
        if (dy > 0) {
            //向上拖动
            View lastVisibleView = childViewHelper.getLastVisibleView();
            if (childViewHelper.isLastDataView(lastVisibleView)) {
                //判断最后一个View是显示情况(完全显示,完全显示且底部有空白,不完全显示)
                int bottomInterval = childViewHelper.getContainerBottom() - childViewHelper.getViewBottomWithMargin(lastVisibleView);
                if (bottomInterval == 0) {
                    //正好完全显示
                    dy = 0;
                }else if (bottomInterval < 0) {
                    //不完全显示
                    dy = Math.min(-bottomInterval, dy);
                }else {
                    //底部还有空白
                    dy = -bottomInterval;
                }
            }
        }else {
            //向下拖动
            if (layoutInfo.scrollOffset + dy < 0) {
                dy = -layoutInfo.scrollOffset;
            }
        }
        if (dy != 0) {
            //先处理View的回收
            recycleOutOfIndexView(recycler, state, dy);
            dy = startLayout(recycler, state, dy);
            layoutInfo.scrollOffset += dy;
            offsetChildrenVertical(-dy);
        }
        return dy;
    }

    /**
     * 保存和布局相关的信息
     */
    private class LayoutInfo {
        //由于滚动，需要记录响应的偏移量
        int scrollOffset;
        //布局过程中垂直方向的偏移量
        int verticalOffset;
        //第一行的verticalOffset
        int firstRowVerticalOffset = getPaddingTop();
        //布局过程中水平方向的偏移量
        int horizontalOffset;
        //记录将要布局的一行View
        List<View> pendingLayoutViews = new ArrayList<>();
        //记录已经布局过的View的布局参数，在向上滚动的时候需要用到
        SparseArray<Rect> preLayoutedViewPositions = new SparseArray<>();
        //第一个显示的数据的索引
        int startPos = 0;
    }

    private class ChildViewHelper {

        /**
         * 获取View占用的高度,包括margin
         */
        public int getHeightWithMargins(View view) {
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin;
        }

        /**
         * 获取View占用的宽度,包括margin
         */
        public int getWidthWithMargins(View view) {
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin;
        }

        /**
         * 获取内容View的最大宽度
         */
        public int getContainerHorizontalSpace() {
            return getWidth() - getPaddingLeft() - getPaddingRight();
        }

        /**
         * 获取View的最顶部位置，即考虑top margin
         */
        public int getViewTopWithMargin(View view) {
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return getDecoratedTop(view) - lp.topMargin;
        }

        /**
         * 获取View的最底部位置，考虑bottom margin
         */
        public int getViewBottomWithMargin(View view) {
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return getDecoratedBottom(view) + lp.bottomMargin;
        }

        /**
         * 获取内容视图的底部
         */
        public int getContainerBottom() {
            return getHeight() - getPaddingBottom();
        }

        /**
         * 获取内容视图的顶部
         */
        public int getContainerTop() {
            return getPaddingTop();
        }

        public View getLastVisibleView() {
            return getChildAt(getChildCount()-1);
        }

        public View getFirstVisibleView() {

            return getChildAt(0);
        }

        public boolean isLastDataView(View view) {
            return getPosition(view) == getItemCount() - 1;
        }

    }

}
