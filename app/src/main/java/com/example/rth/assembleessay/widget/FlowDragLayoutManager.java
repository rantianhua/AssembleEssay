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

    //记录一行的View
    private List<View> rowViews = new ArrayList<>();
    //一行对应的向上的偏移量
    private int verticalOffset;
    //这是在拖动过程中的偏移量
    private int scrollOffset;
    //记录已经布局过的View的布局参数，在向上滚动的时候需要用到
    private SparseArray<Rect> preLayoutedViews = new SparseArray<>();
    //记录要回收的View
    private List<View> recycleHeap = new ArrayList<>();
    private int maxLineNumbers;


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
        startLayout(recycler, state, 0);
    }

    private int startLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        //先计算，矫正dy
        if (dy == 0) {
            //第一次布局，不需矫正
            int startPos = 0;
            int endPos = getItemCount() - 1;
            if (getChildCount() > 0) {
                final View firstView = getChildAt(0);
                verticalOffset = getDecoratedTop(firstView) - getTopMargin(firstView);
                DebugUtil.debugFormat("%s first View : %s, verticalOffset:%s", TAG, ((TextView)firstView).getText(),verticalOffset);
                startPos = getPosition(firstView);
                final View lastView = getLastVisibleView();
                endPos = getPosition(lastView);
            } else {
                scrollOffset = 0;
                verticalOffset = getPaddingTop();
            }
            detachAndScrapAttachedViews(recycler);
            doFirstLayout(recycler, startPos, endPos);
            return 0;
        } else if (dy > 0) {
            //向上拖动
            final View lastVisibleView = getLastVisibleView();
            final int notShowHeight = getDecoratedBottom(lastVisibleView) + getBottomMargin(lastVisibleView) - (getHeight() - getPaddingBottom());
            final boolean isLastItem = getPosition(lastVisibleView) == getItemCount() - 1;
            if (isLastItem) {
                if (notShowHeight > 0) {
                    dy = Math.min(dy, notShowHeight);
                } else if (notShowHeight == 0) {
                    //正好完全显示，
                    dy = 0;
                } else {
                    dy = notShowHeight;
                }
            }
        } else {
            //向下拖动
            if (scrollOffset + dy < 0) {
                dy = -scrollOffset;
            }
        }

        //根据dy进行View的回收
        recycleOutOfIndexView(recycler, state, dy);

        if (dy == 0) return dy;
        //拖动中的布局
        int startPos = 0;
        int endPos = getItemCount() - 1;
        int leftOffset = getPaddingLeft();
        verticalOffset = getPaddingTop();
        int newView = 0;
        if (dy > 0) {
            final View lastVisibleView = getLastVisibleView();
            verticalOffset = getDecoratedBottom(lastVisibleView) + getBottomMargin(lastVisibleView);
            if (verticalOffset - dy < getHeight() - getPaddingBottom()) {
                startPos = getPosition(lastVisibleView) + 1;
                //需要从底部添加新的View
                for (int i = startPos; i <= endPos; i++) {
                    final View child = recycler.getViewForPosition(i);
                    addView(child);
                    measureChildWithMargins(child, 0, 0);
                    final int childWidthSpace = getWidthWithMargin(child);
                    if (leftOffset + childWidthSpace <= getContentHorizontalSpace()) {
                        //一行未满
                        rowViews.add(child);
                        leftOffset += childWidthSpace;
                        if (i == endPos) {
                            newView += rowViews.size();
                            layoutARow(true,recycler);
                        }
                    } else {
                        //换行
                        newView += rowViews.size();
                        layoutARow(false,recycler);
                        if (verticalOffset - dy >= getHeight() - getPaddingBottom()) {
                            //越界,不用再布局
                            removeAndRecycleView(child, recycler);
                            break;
                        }
                        removeAndRecycleView(child,recycler);
                        leftOffset = getPaddingLeft();
                        i--;
                    }
                }
            }
            //最后再纠正一次dy,有可能拖出空白
            final View lastView = getLastVisibleView();
            if (getPosition(lastView) == getItemCount() - 1) {
                int interval = getHeight() - getPaddingBottom() - getDecoratedBottom(lastView) - getBottomMargin(lastView);
                if (interval > 0) {
                    dy -= interval;
                }
            }
        } else {
            final View firstView = getChildAt(0);
            startPos = getPosition(firstView) - 1;
            if (getDecoratedTop(firstView) - getTopMargin(firstView) - dy > getPaddingTop()) {
                //需要从顶部添加View
                leftOffset = getContentHorizontalSpace();
                for (int i = startPos; i >= 0; i--) {
                    Rect rect = preLayoutedViews.get(i);
                    if (rect.bottom - scrollOffset - dy < getPaddingTop()) {
                        //越界,不画
                        break;
                    } else {
                        View child = recycler.getViewForPosition(i);
                        addView(child, 0);
                        measureChildWithMargins(child, 0, 0);
                        newView ++;
                        layoutDecoratedWithMargins(child,rect.left, rect.top - scrollOffset, rect.right, rect.bottom - scrollOffset);
                        DebugUtil.debugFormat("%s startLayout pos is %s, string %s",
                                TAG,
                                getPosition(child),
                                ((TextView) child).getText());
                    }
                }
            }
        }
        if (newView > 0) {
            DebugUtil.debugFormat("%s about view add %s views",TAG, newView);
        }
        return dy;
    }

    /**
     * 进行首次绘制，与拖动无关
     */
    private void doFirstLayout(RecyclerView.Recycler recycler, int startPos, int endPos) {
        int leftOffset = getPaddingLeft();
        for (int i = startPos; i <= endPos; i++) {
            View child = recycler.getViewForPosition(i);
            addView(child);
            measureChildWithMargins(child, 0, 0);
            int childWidthSpace = getWidthWithMargin(child);
            if (leftOffset + childWidthSpace <= getContentHorizontalSpace()) {
                //当前行可以继续排列
                rowViews.add(child);
                leftOffset += childWidthSpace;
                if (i == getItemCount() - 1) {
                    layoutARow(true,recycler);
                }else if(i == endPos) {
                    layoutARow(false,recycler);
                }
            } else {
                //换行显示
                //先布局上一行的Views
                layoutARow(false,recycler);
                if (verticalOffset > getHeight() - getPaddingBottom()) {
                    removeAndRecycleView(child, recycler);
                    break;
                } else {
                    leftOffset = getPaddingLeft();
                    removeAndRecycleView(child,recycler);
                    i--;
                }
            }
        }
        DebugUtil.debugFormat("%s onLayoutChildren startLayout startPos:%s endPos:%s totalCount:%s",TAG, startPos, endPos, getChildCount());
    }

    private View getLastVisibleView() {
        return getChildAt(getChildCount() - 1);
    }

    /**
     * 拖动过程中回收越界的View
     * 改方法还有问题
     */
    private void recycleOutOfIndexView(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        if (dy > 0) {
            //向上滑动,回收上越界的View
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                final int afterScrollBottom = getDecoratedBottom(child) + getBottomMargin(child) - dy;
                if (afterScrollBottom <= getPaddingTop() ) {
                    //滑动之后不可见,回收掉
                    recycleHeap.add(child);
//                    DebugUtil.debugFormat("%s recycleOutOfIndexView %s will be recycle",TAG,i);
                }else {
                    break;
                }
            }
        } else if (dy < 0) {
            //向下滑动,回收下越界的View
            for (int i = getChildCount() - 1;i >= 0; i--) {
                final View child = getChildAt(i);
                final int afterScrollTop = getDecoratedTop(child) - getTopMargin(child) - dy;
                if (afterScrollTop >= getHeight() - getPaddingBottom()) {
                    //不可见,回收
                    recycleHeap.add(child);
//                    DebugUtil.debugFormat("%s recycleOutOfIndexView %s will be recycle",TAG,i);
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
        int rowItems = rowViews.size();
        if (rowItems == 0) return;
        int viewWidthSpace = 0;
        for (View rowView : rowViews) {
            viewWidthSpace += getWidthWithMargin(rowView);
        }
        int restSpace = getContentHorizontalSpace() - viewWidthSpace;
        //计算每一个Item之间的间隔
        int horizontalInterval = 0;
        if (rowItems > 1) {
            horizontalInterval = isLastRow ? 0 : restSpace / (rowItems - 1);
        }
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
                rect.set(horizontalOffset, verticalOffset + scrollOffset, horizontalOffset + getWidthWithMargin(rowViews.get(j)), verticalOffset + itemHeightSpace + scrollOffset);
            } else {
                layoutDecoratedWithMargins(rowViews.get(j),
                        horizontalOffset,
                        verticalOffset,
                        horizontalOffset + getWidthWithMargin(rowViews.get(j)) + horizontalInterval,
                        verticalOffset + itemHeightSpace);
                rect.set(horizontalOffset, verticalOffset + scrollOffset, horizontalOffset + getWidthWithMargin(rowViews.get(j)) + horizontalInterval, verticalOffset + itemHeightSpace + scrollOffset);
                horizontalOffset += getWidthWithMargin(rowViews.get(j)) + horizontalInterval;
            }
            DebugUtil.debugFormat("%s startLayout pos is %s, string %s",
                    TAG,
                    getPosition(rowViews.get(j)),
                    ((TextView) rowViews.get(j)).getText());
            preLayoutedViews.put(getPosition(rowViews.get(j)), rect);
        }
        verticalOffset += itemHeightSpace;
        if (rowViews.size() > maxLineNumbers) {
            maxLineNumbers = rowViews.size();
            recycler.setViewCacheSize(maxLineNumbers);
            DebugUtil.debugFormat("%s resetCacheSize:%s",TAG, maxLineNumbers);
        }
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
        int realOffset = startLayout(recycler, state, dy);
        scrollOffset += realOffset;
        offsetChildrenVertical(-realOffset);
        return realOffset;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }
}
