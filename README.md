[![Release](https://jitpack.io/v/rantianhua/AssembleEssay.svg)](https://jitpack.io/#rantianhua/AssembleEssay)

# AssembleEssay
自定义 LayoutManager 实现一个流动布局，可以用作文章展示，也可以用作标签展示，并且可以很方便地为其添加布局动画，示例代码里添加了拖拽插入的动画，如效果图。

对齐方式可选4中：

1. 向左对齐
2. 向右对齐
3. 居中对齐
4. 两边对齐

默认是两边对齐的布局方式

# 效果图
![image](https://raw.githubusercontent.com/rantianhua/AssembleEssay/master/app/images/Flow_Drag_Essay_View.gif)

# 使用方式
1. 先添加 jitpack 
```
allprojects {
  repositories {
	 ...
	 maven { url 'https://jitpack.io' }
  }
}
```
2. 在 module 对应得 build.gradle 文件中添加
```
implementation 'com.github.rantianhua:AssembleEssay:v1.0.0'
```
3. 示例代码
```
recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
recyclerView.setLayoutManager(new FlowDragLayoutManager());
```
# 设置/更改对齐方式
```
//初始化是设置
//向左对齐
FlowDragLayoutManager layoutManager = new FlowDragLayoutManager(FlowDragLayoutConstant.LEFT);
//居中对齐
FlowDragLayoutManager layoutManager = new FlowDragLayoutManager(FlowDragLayoutConstant.CENTER);
//向右对齐
FlowDragLayoutManager layoutManager = new FlowDragLayoutManager(FlowDragLayoutConstant.RIGHT);
//居中对齐
FlowDragLayoutManager layoutManager = new FlowDragLayoutManager(FlowDragLayoutConstant.TWO_SIDE);

//动态更改
layoutManager.setAlignMode(FlowDragLayoutConstant.LEFT);
layoutManager.setAlignMode(FlowDragLayoutConstant.CENTER);
layoutManager.setAlignMode(FlowDragLayoutConstant.RIGHT);
layoutManager.setAlignMode(FlowDragLayoutConstant.TWO_SIDE);
```

# 优点
1. 当标签很多的时候，可以重用视图，不会一次性产生过多对象
2. 基于 RecyclerView 的 LayoutManager , 使用方便，而且添加动画效果也很容易
3. 对齐方式灵活，目前四种应该n可以满足大部分的需求了
