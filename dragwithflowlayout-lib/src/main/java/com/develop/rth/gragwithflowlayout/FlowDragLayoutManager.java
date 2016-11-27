package com.develop.rth.gragwithflowlayout;

import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rth on 16/11/19.
 */
public class FlowDragLayoutManager extends RecyclerView.LayoutManager {

    private LayoutInfo layoutInfo;
    private ILayoutHelper layoutHelper;
    private List<View> rowViews;

    public FlowDragLayoutManager() {
        this(FlowDragLayoutConstant.TWO_SIDE);
    }

    public FlowDragLayoutManager(@FlowDragLayoutConstant.AlignMode int layoutAlignMode) {
        layoutInfo = new LayoutInfo();
        layoutHelper = new LayoutHelperImpl();
        rowViews = new ArrayList<>();
        layoutInfo.alignMode = layoutAlignMode;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        DebugUtil.debugFormat("FlowDragLayoutManager onLayoutChildren");
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        if (layoutInfo.haveMoveAction) {
            layoutInfo.haveMoveAction = false;
        } else {
            resetLayoutInfo();
        }
        detachAndScrapAttachedViews(recycler);
        startLayout(recycler,state);
    }

    private void startLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        switch (layoutInfo.layoutFrom) {
            case FlowDragLayoutManager.LayoutFrom.DOWN_TO_UP:
                layoutFromDownToUp(recycler, state);
                break;
            case FlowDragLayoutManager.LayoutFrom.UP_TO_DOWN:
                layoutFromUpToDown(recycler, state);
                break;
        }
    }

    /**
     * 从下向上布局,即逆序布局
     */
    private void layoutFromDownToUp(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (layoutInfo.layoutAnchor + layoutInfo.pendingScrollDistance <= getPaddingTop()) {
            //没必要添加视图
//            DebugUtil.debugFormat("FlowDragLayoutManager no need to layout from down to up");
            return;
        }
        layoutHelper.layoutReverse(recycler, state, this);
    }

    /**
     * 从上向下布局,即顺序布局
     */
    private void layoutFromUpToDown(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() > 0) {
            if (layoutInfo.layoutAnchor - layoutInfo.pendingScrollDistance >= getHeight() - getPaddingBottom()) {
                //不需要布局了
//                DebugUtil.debugFormat("FlowDragLayoutManager no need to layout from up to down");
                checkoutBottomOutofRange(state);
                return;
            }
        }
        int xOffset = getPaddingLeft();
        DebugUtil.debugFormat("FlowDragLayoutManager start pos:%s",layoutInfo.startLayoutPos);
        for (int i = layoutInfo.startLayoutPos; i < state.getItemCount(); i++) {
            final View view = recycler.getViewForPosition(i);
            addView(view);
            measureChildWithMargins(view,0,0);

            final int widthSpace = getWidthWithMargins(view);

            if (xOffset + widthSpace <= getContentHorizontalSpace()) {
                rowViews.add(view);
                xOffset += widthSpace;
                if (i == state.getItemCount() - 1) {
                    layoutHelper.layoutARow(rowViews, recycler, this, true);
                }
            }else {
                //已经是下一行了,先布局上一行
                layoutHelper.layoutARow(rowViews, recycler, this, false);
                //越界检查
                if (layoutInfo.layoutAnchor - layoutInfo.pendingScrollDistance >= getHeight() - getPaddingBottom()) {
                    removeAndRecycleView(view,recycler);
                    break;
                }
                xOffset = getPaddingLeft();
                rowViews.add(view);
                xOffset += widthSpace;

                if (i == state.getItemCount() - 1) {
                    layoutHelper.layoutARow(rowViews, recycler, this, true);
                }
            }
        }
        if (layoutInfo.pendingScrollDistance != 0) {
            //最后检查一下底部是否超出了滑动范围
            checkoutBottomOutofRange(state);
        }
        DebugUtil.debugFormat("FlowDragLayoutManager end pos:%s",getPosition(findCloestVisibleView(false)));
    }

    private void checkoutBottomOutofRange(RecyclerView.State state) {
        final View view = findCloestVisibleView(false);
        if (getPosition(view) == state.getItemCount() - 1) {
            int interval = getHeight() - getPaddingBottom() - (getViewBottomWithMargin(view) - layoutInfo.pendingScrollDistance);
            DebugUtil.debugFormat("底部 interval:%s",interval);
            if (interval > 0) {
                DebugUtil.debugFormat("底部超出了可滑动范围");
            }
        }
    }

    private void resetLayoutInfo() {
        if (getChildCount() != 0) {
            final View view = findCloestVisibleView(true);
            layoutInfo.firstVisibleViewTop = getViewTopWithMargin(view);
            layoutInfo.startLayoutPos = getPosition(view);
        }else {
            layoutInfo.firstVisibleViewTop = getPaddingTop();
            layoutInfo.startLayoutPos = 0;
        }
        layoutInfo.layoutAnchor = layoutInfo.firstVisibleViewTop;
        layoutInfo.pendingScrollDistance = 0;
        layoutInfo.layoutFrom = LayoutFrom.UP_TO_DOWN;
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        layoutInfo.haveMoveAction = true;
        resetLayoutInfo();
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        DebugUtil.debugFormat("FlowDragLayoutManager onLayoutComplete");
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        //先修正dy
        if (dy == 0) return 0;
        if (getChildCount() == 0) return 0;
        if (dy > 0) {
            //向上拖动
            View lastVisibleView = findCloestVisibleView(false);
            if (getPosition(lastVisibleView) == state.getItemCount() - 1) {
                //判断最后一个View是显示情况(完全显示,完全显示且底部有空白,不完全显示)
                int bottomInterval = getHeight() - getPaddingBottom() - getViewBottomWithMargin(lastVisibleView);
                if (bottomInterval == 0) {
                    //正好完全显示
                    return 0;
                }else if (bottomInterval < 0) {
                    //不完全显示
                    dy = Math.min(-bottomInterval, dy);
                }else {
                    //底部还有空白
//                    DebugUtil.debugFormat("FlowDragLayoutManager scroll to end and have white space");
//                    dy = -bottomInterval;
                    return 0;
                }
            }
        }else {
            //向下拖动
            if (layoutInfo.scrollOffset == 0) return 0;
            if (layoutInfo.scrollOffset + dy < 0) {
                dy = -layoutInfo.scrollOffset;
            }
        }

        layoutInfo.pendingScrollDistance = Math.abs(dy);
        layoutInfo.layoutFrom = dy > 0 ? LayoutFrom.UP_TO_DOWN : LayoutFrom.DOWN_TO_UP;
        layoutHelper.recycleUnvisibleViews(recycler, state, this);

        if (dy > 0) {
            final View last = findCloestVisibleView(false);
            layoutInfo.layoutAnchor = getViewBottomWithMargin(last);
            layoutInfo.startLayoutPos = getPosition(last) + 1;
        }else {
            final View first = findCloestVisibleView(true);
            layoutInfo.layoutAnchor = getViewTopWithMargin(first);
            layoutInfo.startLayoutPos = getPosition(first) - 1;
        }
        startLayout(recycler,state);
        offsetChildrenVertical(-dy);
        layoutInfo.scrollOffset += dy;
//        DebugUtil.debugFormat("FlowDragLayoutManager dy:%s scrollOffset:%s",dy,layoutInfo.scrollOffset);
        return dy;
    }

    public int getViewTopWithMargin(final View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedTop(view) - lp.topMargin;
    }

    public View findCloestVisibleView(boolean isFirst) {
        return getChildAt(isFirst ? 0 : getChildCount() - 1);
    }

    public int getViewBottomWithMargin(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedBottom(view) + lp.bottomMargin;
    }

    public int getContentHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    public int getWidthWithMargins(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin;
    }

    public int getHeightWithMargins(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin;
    }

    public LayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    /**
     * 记录和布局相关的全局信息
     */
    public final static class LayoutInfo {
        //滚动的偏移量
        int scrollOffset;
        //开始布局的锚点
        int layoutAnchor;
        //某一时刻将要滚动的距离
        int pendingScrollDistance;
        //布局的开始位置,在数据源中的位置
        int startLayoutPos;
        //第一个可见View的top
        int firstVisibleViewTop;
        //表示布局顺序
        int layoutFrom;
        boolean haveMoveAction = false;
        //对齐方式
        int alignMode;
    }

    public interface LayoutFrom {
        //从上往下布局
        int UP_TO_DOWN = 1;
        //相反
        int DOWN_TO_UP = -1;
    }
}
