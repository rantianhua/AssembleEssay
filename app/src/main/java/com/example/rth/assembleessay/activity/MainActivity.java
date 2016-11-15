package com.example.rth.assembleessay.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.example.rth.assembleessay.R;
import com.example.rth.assembleessay.adpter.AssembleEssayAdapter;
import com.example.rth.assembleessay.contract.MainContract;
import com.example.rth.assembleessay.presenter.MainPresenter;
import com.example.rth.assembleessay.widget.FlowDragLayoutManager;

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
}
