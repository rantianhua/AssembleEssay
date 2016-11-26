package com.develop.rth.assembleessay.contract;

import java.util.List;

/**
 * Created by rth on 16/11/15.
 */
public class MainContract {

    public interface IMainView {

        void onEssayLoaded(List<String> datas);
    }

    public interface IMainPresenter<V extends IMainView> {

        void loadEssay();

        void release();
    }
}
