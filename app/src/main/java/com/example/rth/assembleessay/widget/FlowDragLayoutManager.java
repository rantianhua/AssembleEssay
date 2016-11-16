package com.example.rth.assembleessay.widget;

import android.content.Context;
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
    private static final boolean DEBUG = true;

    //记录一行的View
    private List<View> rowViews = new ArrayList<>();
    //一行对应的向上的偏移量
    private int verticalOffset;
    //这是在拖动过程中的偏移量
    private int scrollOffset;
    //记录已经布局过的View的布局参数，在向上滚动的时候需要用到
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
        scrollOffset = 0;
        startLayout(recycler,state,0);
    }

    private int startLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        int moreLayoutRow = 0;
        DebugUtil.debugFormat("%s startLayout origin dy %s",TAG, dy);
        //先计算，矫正dy
        if (dy == 0) {
            //第一次布局，不需矫正
            detachAndScrapAttachedViews(recycler);
            doFirstLayout(recycler, state);
            return 0;
        }else if(dy > 0){
            //向上拖动
            final View lastVisibleView = getLastVisibleView();
            final int notShowHeight = getDecoratedBottom(lastVisibleView) + getBottomMargin(lastVisibleView) - (getHeight() - getPaddingBottom());
            final boolean isLastItem = getPosition(lastVisibleView) == getItemCount() - 1;
            final int childHeightSpace = getHeightWithMargin(lastVisibleView);
            if (notShowHeight > 0) {
                //没有完全显示
                if (isLastItem) {
                    //最后一个数据
                    dy = Math.min(dy,notShowHeight);
                }else {
                    int afterScroll = dy - notShowHeight;
                    if (afterScroll > 0) {
                        //此时需要在底部加载新行
                        moreLayoutRow = afterScroll / childHeightSpace + 1;
                    }
                }
            }else if(notShowHeight == 0) {
                //正好完全显示，
                if (isLastItem) {
                    dy = 0;
                }else {
                    moreLayoutRow = dy / childHeightSpace + 1;
                }
            }else {
                dy = dy + notShowHeight;
            }
        }else {
            //向下滑动
            final View firstVisibleView = getChildAt(0);
            final int childHeightSpace = getHeightWithMargin(firstVisibleView);
            final int notShowing = getDecoratedTop(firstVisibleView) - getPaddingTop();
            final boolean isFirstItem = getPosition(firstVisibleView) == 0;
            final int viewTopMargin = getTopMargin(firstVisibleView);
            DebugUtil.debugFormat("%s firstVisibleView getDecoratedTop is %s, notShowingHeight is %s, pos is %s, string is %s",TAG, getDecoratedTop(firstVisibleView),
                    notShowing, getPosition(firstVisibleView), ((TextView)firstVisibleView).getText());
            if (notShowing >= viewTopMargin) {
                if (isFirstItem) {
                    dy = 0;
                }else {
                    moreLayoutRow = Math.abs(dy) / childHeightSpace + 1;
                }
            } else {
                if (isFirstItem) {
                    dy = Math.min(dy, notShowing - viewTopMargin);
                }else {
                    int afterScroll = Math.abs(dy) - Math.abs(notShowing - viewTopMargin);
                    if (afterScroll > 0) {
                        moreLayoutRow = afterScroll / childHeightSpace + 1;
                    }
                }
            }
        }
        DebugUtil.debugFormat("%s startLayout handled dy %s, moreLayoutRow %s",TAG, dy, moreLayoutRow);

        //根据dy进行View的回收
        recycleOutOfIndexView(recycler,state,dy);

        if (moreLayoutRow == 0) return dy;
        //拖动中的布局
        int startPos = 0;
        int endPos = getItemCount() - 1;
        int leftOffset = getPaddingLeft();
        verticalOffset = getPaddingTop();
        int rowIndex = 0;
        if (dy > 0) {
            final View lastVisibleView = getLastVisibleView();
            startPos = getPosition(lastVisibleView) + 1;
            verticalOffset = getDecoratedBottom(lastVisibleView) + getBottomMargin(lastVisibleView);
            for (int i = startPos; i <= endPos; i++) {
                final View child = recycler.getViewForPosition(i);
                addView(child);
                measureChildWithMargins(child,0,0);
                final int childWidthSpace = getWidthWithMargin(child);
                if (leftOffset + childWidthSpace <= getContentHorizontalSpace()) {
                    //一行未满
                    rowViews.add(child);
                    leftOffset += childWidthSpace;
                    if (i == endPos) {
                        DebugUtil.debugFormat("%s startLayout up 一行未满，但数据没有了",TAG);
                        layoutARow(true);
                    }
                }else {
                    //换行
                    layoutARow(false);
                    rowIndex++;
                    if (rowIndex == moreLayoutRow) {
                        //布局完毕
                        removeAndRecycleView(child,recycler);
                        DebugUtil.debugFormat("%s startLayout up rowIndex 达标",TAG);
                        break;
                    }
                    leftOffset = getPaddingLeft();
                    rowViews.add(child);
                    leftOffset += childWidthSpace;
                    if (i == endPos) {
                        DebugUtil.debugFormat("%s startLayout up 刚慢一行满，新行只有一个数据",TAG);
                        layoutARow(true);
                    }
                }
            }
        }else {
            final View firstView = getChildAt(0);
            final int childWidthSpace = getWidthWithMargin(firstView);
            startPos = getPosition(firstView) - 1;
            DebugUtil.debugFormat("%s startLayout startPos %s",TAG, startPos);
            for (int i = startPos; i >= 0; i--) {
                Rect rect = preLayoutedViews.get(i);
                View child = recycler.getViewForPosition(i);
                addView(child, 0);
                measureChildWithMargins(child, 0, 0);
                DebugUtil.debugFormat("%s layout pos is %s, String is %s",TAG,i,((TextView)child).getText());
                layoutDecoratedWithMargins(child, rect.left, rect.top - scrollOffset, rect.right, rect.bottom - scrollOffset);
                if (leftOffset + childWidthSpace <= getContentHorizontalSpace()) {
                    leftOffset += childWidthSpace;
                }else {
                    rowIndex ++;
                    if (rowIndex == moreLayoutRow) {
                        break;
                    }
                    leftOffset = getPaddingLeft();
                }
            }
        }
        return dy;
    }

    /**
     * 进行首次绘制，与拖动无关
     */
    private void doFirstLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int startPos = 0;
        int endPos = getItemCount() - 1;
        int leftOffset = getPaddingLeft();
        verticalOffset = getPaddingTop();
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
                if (verticalOffset > getHeight() - getPaddingBottom()) {
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
    }

    private View getLastVisibleView() {
        return getChildAt(getChildCount()-1);
    }

    /**
     * 拖动过程中回收越界的View
     */
    private void recycleOutOfIndexView(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        for (int i = 0;i < getChildCount();i++) {
            View child = getChildAt(i);
            if (dy > 0) {
                //向上滑动,回收上越界的View
                if (getDecoratedBottom(child) + getBottomMargin(child) - dy <= getPaddingTop()) {
                    removeAndRecycleView(child, recycler);
                    DebugUtil.debugFormat("%s 回收上越界View string is %s",TAG, ((TextView)child).getText());
                }
            }else if (dy < 0){
                //向下滑动,回收下越界的View
                if (getDecoratedTop(child) - getTopMargin(child) - dy >= getHeight() - getPaddingBottom()) {
                    removeAndRecycleView(child,recycler);
                    DebugUtil.debugFormat("%s 回收下越界View string is %s",TAG,  ((TextView)child).getText());
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
            DebugUtil.debugFormat("%s layout pos is %s, String is %s",TAG,getPosition(rowViews.get(j)),((TextView)rowViews.get(j)).getText());
            preLayoutedViews.put(getPosition(rowViews.get(j)),rect);
        }
        verticalOffset += itemHeightSpace;
        DebugUtil.debugFormat("%s verticalOffset is %s, item height is %s", TAG, verticalOffset, itemHeightSpace);
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

    private int getTopMargin(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return lp.topMargin;
    }

    private int getBottomMargin(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return lp.bottomMargin;
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
        if (scrollOffset + realOffset < 0) {
            realOffset = -scrollOffset;
        }
        if (realOffset == 0) {
            return 0;
        }
        DebugUtil.debugFormat("%s scrollVerticallyBy realScroll is %s",TAG, realOffset);
        realOffset = startLayout(recycler,state,realOffset);
        scrollOffset += realOffset;
        offsetChildrenVertical(-realOffset);
        return realOffset;
    }
}
