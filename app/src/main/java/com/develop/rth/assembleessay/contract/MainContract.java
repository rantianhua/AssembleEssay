package com.develop.rth.assembleessay.contract;

import com.develop.rth.assembleessay.adpter.AssembleEssayAdapter;

import java.util.List;

/**
 * Created by rth on 16/11/15.
 */
public class MainContract {

    public interface IMainView {

        void onDataLoaded(List<String> datas, int showType);

        void addItemIn(int insertPos);

        void removeItemIn(int removePos);
    }

    public interface IMainPresenter<V extends IMainView> {

        void release();

        void showTags();

        void showEssay();

        void randomAdd(AssembleEssayAdapter adapter);

        void randomDelete(AssembleEssayAdapter adapter);
    }
}
