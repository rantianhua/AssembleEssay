package com.example.rth.assembleessay.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.TextView;

import com.example.rth.assembleessay.R;
import com.example.rth.assembleessay.adpter.AssembleEssayAdapter;
import com.example.rth.assembleessay.contract.MainContract;
import com.example.rth.assembleessay.presenter.MainPresenter;
import com.example.rth.assembleessay.widget.DragItemTouchCallBack;
import com.example.rth.assembleessay.widget.FlowDragLayoutManager;

import java.util.Collections;
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
        (findViewById(R.id.tv)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Collections.swap(adapter.getDatas(),0,12);
                adapter.notifyItemMoved(0,12);
//                adapter.notifyDataSetChanged();
            }
        });
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
}
