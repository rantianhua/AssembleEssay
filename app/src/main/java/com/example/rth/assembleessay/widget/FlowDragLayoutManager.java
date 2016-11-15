package com.example.rth.assembleessay.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.example.rth.assembleessay.util.DebugUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rth on 16/11/15.
 */
public class FlowDragLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = "FlowDragLayoutManager";

    //记录一行的View
    private List<View> rowViews = new ArrayList<>();
    //一行对应的向上的偏移量
    private int verticalOffset;
    //这是在拖动过程中的偏移量
    private int scrollOffset;
    private SparseArray<Rect> preLayoutedViews = new SparseArray<>();

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }
        detachAndScrapAttachedViews(recycler);
        scrollOffset = 0;
        startLayout(recycler,state,0);
    }

    private int startLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        int startPos = 0;
        int endPos = getItemCount() - 1;
        //一行中向左的偏移量
        int leftOffset = getPaddingLeft();
        verticalOffset = getPaddingTop();

        //滚动的时候,先回收越界的子View
        recycleOutOfIndexView(recycler,state,dy);

        //开始新的布局阶段
        if (dy >= 0) {
            //向上或者没有拖动,若向上拖动,需要布局从底部新加入的View
            if (getChildCount() > 0) {
                View lastView = getChildAt(getChildCount()-1);
                //开始绘制的索引
                startPos = getPosition(lastView) + 1;
                verticalOffset = getDecoratedBottom(lastView);
            }
            for (int i = startPos;i <= endPos; i++) {
                View child = recycler.getViewForPosition(i);
                addView(child);
                measureChildWithMargins(child,0,0);
                int childWidthSpace = getWidthWithMargin(child);
                if (leftOffset + childWidthSpace <= getContentHorizontalSpace()) {
                    //当前行可以继续排列
                    rowViews.add(child);
                    leftOffset += childWidthSpace;
                    if (i == endPos) {
                        layoutARow(true);
                    }
                }else {
                    //先布局上一行的Views
                    layoutARow(false);
                    //换行显示
//                    DebugUtil.debugFormat("%s 换行显示, position is %s", TAG, i);
                    leftOffset = getPaddingLeft();
                    if (verticalOffset - dy > getHeight() - getPaddingBottom()) {
                        //向下越界,不用显示,回收View
//                        DebugUtil.debugFormat("%s 向下越界, position is %s", TAG, i);
                        removeAndRecycleView(child,recycler);
                        break;
                    }else {
                        rowViews.add(child);
                        leftOffset += childWidthSpace;
                        if (i == endPos) {
                            layoutARow(true);
                        }
                    }
                }
            }

            View lastChild = getChildAt(getChildCount() - 1);
            if (getPosition(lastChild) == getItemCount() - 1) {
                int interval = getHeight() - getPaddingBottom() - getDecoratedBottom(lastChild);
                if (interval > 0) {
                    dy -= interval;
                }
            }
        } else {
            if (getChildCount() > 0) {
                View firstView = getChildAt(0);
                startPos = getPosition(firstView) - 1;
            }else {
                return dy;
            }
            for (int i = startPos; i >= 0; i--) {
                Rect rect = preLayoutedViews.get(i);

                if (rect.bottom - scrollOffset - dy < getPaddingTop()) {
                    break;
                } else {
                    View child = recycler.getViewForPosition(i);
                    addView(child, 0);
                    measureChildWithMargins(child, 0, 0);

                    layoutDecoratedWithMargins(child, rect.left, rect.top - scrollOffset, rect.right, rect.bottom - scrollOffset);
                }
            }
        }

        return dy;
    }

    /**
     * 拖动过程中回收越界的View
     */
    private void recycleOutOfIndexView(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        if (getChildCount() > 0) {
            if(dy > 0) {
                //向上滑动,回收上越界的View
                for (int i = 0;i < getChildCount();i++) {
                    View child = getChildAt(i);
                    if (getDecoratedBottom(child) - dy < verticalOffset) {
                        removeAndRecycleView(child, recycler);
                        DebugUtil.debugFormat("%s 回收上越界View pos is %s",TAG, getPosition(child));
                    }
                }
            }else if (dy < 0){
                //向下滑动,回收下越界的View
                for (int i = getChildCount()-1;i >= 0;i--) {
                    View child = getChildAt(i);
                    if (getDecoratedTop(child) - dy > getHeight() - getPaddingBottom()) {
                        removeAndRecycleView(child,recycler);
                        DebugUtil.debugFormat("%s 回收下越界View pos is %s",TAG, getPosition(child));
                    }
                }
            }
        }
    }

    /**
     * 布局一行的View
     * @param isLastRow 是否最后一行
     */
    private void layoutARow(boolean isLastRow) {
        int rowItems = rowViews.size();
        if (rowItems == 0) return;
//        DebugUtil.debugFormat("%s layoutARow isLastRow is %s, rowItems is %s", TAG, isLastRow, rowItems);
        int viewWidthSpace = 0;
        for (View rowView : rowViews) {
            viewWidthSpace += getWidthWithMargin(rowView);
        }
        int restSpace = getContentHorizontalSpace() - viewWidthSpace;
        //计算每一个Item之间的间隔
        int horizontalInterval = isLastRow ? 0 : restSpace / (rowItems - 1);
//        DebugUtil.debugFormat("%s layoutARow horizontalInterval is %s", TAG, horizontalInterval);
        int horizontalOffset = getPaddingLeft();
        int itemHeightSpace = 0;
        for (int j = 0; j < rowItems; j++) {
            itemHeightSpace = getHeightWithMargin(rowViews.get(j));
            Rect rect = new Rect();
            if (j == rowItems - 1) {
                layoutDecoratedWithMargins(rowViews.get(j),
                        horizontalOffset,
                        verticalOffset,
                        horizontalOffset + getWidthWithMargin(rowViews.get(j)),
                        verticalOffset + itemHeightSpace);
                rect.set(horizontalOffset, verticalOffset + scrollOffset, horizontalOffset + getWidthWithMargin(rowViews.get(j)),verticalOffset + itemHeightSpace + scrollOffset);
            }else {
                layoutDecoratedWithMargins(rowViews.get(j),
                        horizontalOffset,
                        verticalOffset,
                        horizontalOffset + getWidthWithMargin(rowViews.get(j)) + horizontalInterval,
                        verticalOffset + itemHeightSpace);
                rect.set(horizontalOffset, verticalOffset + scrollOffset, horizontalOffset + getWidthWithMargin(rowViews.get(j)) + horizontalInterval,verticalOffset + itemHeightSpace + scrollOffset);
                horizontalOffset += getWidthWithMargin(rowViews.get(j)) + horizontalInterval;
            }
            preLayoutedViews.put(getPosition(rowViews.get(j)),rect);
        }
        verticalOffset += itemHeightSpace;
        DebugUtil.debugFormat("%s verticalOffset is %s", TAG, verticalOffset);
        rowViews.clear();
    }

    /**
     * 获取View占用的高度,包括margin
     */
    private int getHeightWithMargin(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin;
    }

    /**
     * 获取View占用的宽度,包括margin
     */
    private int getWidthWithMargin(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin;
    }

    /**
     * 获取内容View的最大宽度
     */
    private int getContentHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (dy == 0) return 0;
        if (getChildCount() == 0) return 0;
        int realOffset = dy;
        //进行边界修复
        if (dy > 0) {
            //向上拖动,根据当前显示的最后一个View判断是不是最后一个Item
            View lastVisibleView = getChildAt(getChildCount() - 1);
            if (getPosition(lastVisibleView) == getItemCount() - 1) {
                //滑动到下边界
//                DebugUtil.debugFormat("%s 滑动到下边界",TAG);
                //判断最后一个View是显示情况(完全显示,完全显示且底部有空白,不完全显示)
                int bottomInterval = getHeight() - getPaddingBottom() - getDecoratedBottom(lastVisibleView);
//                DebugUtil.debugFormat("%s scrollVerticallyBy bottomInterval is %s",TAG, bottomInterval);
                if (bottomInterval == 0) {
                    //正好完全显示
                    realOffset = 0;
                }else if (bottomInterval < 0) {
                    //不完全显示
                    realOffset = Math.min(-bottomInterval, realOffset);
                }else {
                    //底部还有空白
                    realOffset = -bottomInterval;
                }
            }
        }else if (dy < 0) {
            //向下拖动
            if (scrollOffset + realOffset < 0) {
//                DebugUtil.debugFormat("%s 滑动到上边界",TAG);
                realOffset = -scrollOffset;
            }
        }
        realOffset = startLayout(recycler,state,realOffset);
        scrollOffset += realOffset;
        offsetChildrenVertical(-realOffset);
        return realOffset;
    }
}
