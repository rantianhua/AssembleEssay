package com.develop.rth.assembleessay.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.develop.rth.assembleessay.R;
import com.develop.rth.assembleessay.adpter.AssembleEssayAdapter;
import com.develop.rth.assembleessay.contract.MainContract;
import com.develop.rth.assembleessay.presenter.MainPresenter;
import com.develop.rth.assembleessay.adpter.DragItemTouchCallBack;
import com.develop.rth.gragwithflowlayout.FlowDragLayoutConstant;
import com.develop.rth.gragwithflowlayout.FlowDragLayoutManager;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MainContract.IMainView {

    private MainContract.IMainPresenter presenter;
    private RecyclerView recyclerView;
    private AssembleEssayAdapter adapter;
    private FlowDragLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        afterView();
        init();
    }

    private void afterView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        layoutManager = new FlowDragLayoutManager();
        recyclerView.setLayoutManager(layoutManager);
        adapter = new AssembleEssayAdapter(this);
        ItemTouchHelper.Callback callback = new DragItemTouchCallBack(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);
    }

    private void init() {
        presenter = new MainPresenter(this);
        presenter.showTags();
    }

    @Override
    protected void onDestroy() {
        presenter.release();
        super.onDestroy();
    }

    @Override
    public void onDataLoaded(List<String> datas, int showType) {
        if (datas == null) return;
        if (datas.size() == 0) return;
        adapter.setDatas(datas, showType);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void addItemIn(int insertPos) {
        adapter.notifyItemInserted(insertPos);
        Toast.makeText(this, "在"+insertPos+"新增了一个元素",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void removeItemIn(int removePos) {
        adapter.notifyItemRemoved(removePos);
        Toast.makeText(this, "删除了第"+removePos+"个元素",Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_align_left:
                layoutManager.setAlignMode(FlowDragLayoutConstant.LEFT);
                break;
            case R.id.menu_align_center:
                layoutManager.setAlignMode(FlowDragLayoutConstant.CENTER);
                break;
            case R.id.menu_align_right:
                layoutManager.setAlignMode(FlowDragLayoutConstant.RIGHT);
                break;
            case R.id.menu_align_two_side:
                layoutManager.setAlignMode(FlowDragLayoutConstant.TWO_SIDE);
                break;
            case R.id.menu_show_tags:
                presenter.showTags();
                break;
            case R.id.menu_show_eassy:
                presenter.showEssay();
                break;
            case R.id.menu_random_add:
                presenter.randomAdd(adapter);
                break;
            case R.id.menu_random_remove:
                presenter.randomDelete(adapter);
                break;
        }
        return true;
    }
}
