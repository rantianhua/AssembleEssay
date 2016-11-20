# AssembleEssay
自定义 LayoutManager 实现一个两边对齐的流动布局，可以用作文章展示，也可以用作标签展示，并且可以很方便地为其添加布局动画，示例代码里添加了拖拽插入的动画。

# 效果图
![image](https://raw.githubusercontent.com/rantianhua/AssembleEssay/master/app/images/Flow_Drag_Essay_View.gif)

# 使用方式
一行代码即可搞定，将 dragwithflowlayout-lib 作为 moudle 引用自己的工程，使用时直接给 RecyclerView 设置 LayoutManager 即可：
```
recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
recyclerView.setLayoutManager(new FlowDragLayoutManager());

adapter = new AssembleEssayAdapter(this);
//这是示例中添加的拖拽插入动画
ItemTouchHelper.Callback callback = new DragItemTouchCallBack(adapter);
ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
touchHelper.attachToRecyclerView(recyclerView);
recyclerView.setAdapter(adapter);
```

# 后续
1. 后面会考虑支持更多的对齐方式（比如向左、向右、居中）。
2. 为标签展示添加一些接口
