package com.develop.rth.assembleessay.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        afterView();
        init();
    }

    private void afterView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new FlowDragLayoutManager());
        adapter = new AssembleEssayAdapter(this);
        ItemTouchHelper.Callback callback = new DragItemTouchCallBack(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);
    }

    private void init() {
        presenter = new MainPresenter(this);
        presenter.loadEssay();
    }

    @Override
    protected void onDestroy() {
        presenter.release();
        super.onDestroy();
    }

    @Override
    public void onEssayLoaded(List<String> datas) {
        if (datas == null) return;
        if (datas.size() == 0) return;
        adapter.setDatas(datas);
        adapter.notifyDataSetChanged();
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
                recyclerView.setLayoutManager(new FlowDragLayoutManager(FlowDragLayoutConstant.LEFT));
//                adapter.getDatas().remove(0);
//                adapter.notifyDataSetChanged();
                break;
            case R.id.menu_align_center:
                recyclerView.setLayoutManager(new FlowDragLayoutManager(FlowDragLayoutConstant.CENTER));
//                adapter.getDatas().remove(0);
//                adapter.notifyItemRemoved(0);
                break;
            case R.id.menu_align_right:
                recyclerView.setLayoutManager(new FlowDragLayoutManager(FlowDragLayoutConstant.RIGHT));
//                adapter.getDatas().add(0, "New add");
//                adapter.notifyItemInserted(0);
                break;
            case R.id.menu_align_two_side:
                recyclerView.setLayoutManager(new FlowDragLayoutManager(FlowDragLayoutConstant.TWO_SIDE));
                break;
        }
        return true;
    }
}
