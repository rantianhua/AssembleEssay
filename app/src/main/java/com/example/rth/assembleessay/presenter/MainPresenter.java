package com.example.rth.assembleessay.presenter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.example.rth.assembleessay.contract.MainContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rth on 16/11/15.
 */
public class MainPresenter implements MainContract.IMainPresenter<MainContract.IMainView> {

    private MainContract.IMainView view;
    private Handler workHandler;
    private HandlerThread workThread;
    private Handler mainHandler;

    private static final String TEST_ESSAY = "Occasionally, Dad would get out his mandolin and play for the family. We three children: Trisha," +
            " Monte and I, George Jr., would often sing along. Songs such as the Tennessee Waltz, Harbor Lights and around Christmas time," +
            " the well-known rendition of Silver Bells. \"Silver Bells, Silver Bells, its Christmas time in the city\" would ring throughout the house." +
            " One of Dad's favorite hymns was \"The Old Rugged Cross\". We learned the words to the hymn when we were very young, and would sing it with Dad when he would play and sing." +
            " Another song that was often shared in our house was a song that accompanied the Walt Disney series: Davey Crockett." +
            " Dad only had to hear the song twice before he learned it well enough to play it. \"Davey, Davey Crockett, King of the Wild Frontier\" was a favorite song for the family." +
            " He knew we enjoyed the song and the program and would often get out the mandolin after the program was over. I could never get over how he could play the songs so well" +
            " after only hearing them a few times. I loved to sing, but I never learned how to play the mandolin. This is something I regret to this day.";

    public MainPresenter(MainContract.IMainView view) {
        this.view = view;
        workThread = new HandlerThread("main_work");
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void loadEssay() {
        workHandler.post(new Runnable() {
            @Override
            public void run() {
                readLoadEssay();
            }
        });
    }

    private void readLoadEssay() {
        String[] essayArray = TEST_ESSAY.split(" ");
        final List<String> datas = new ArrayList<>();
        Collections.addAll(datas,essayArray);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (view == null) return;
                view.onEssayLoaded(datas);
            }
        });
    }

    @Override
    public void release() {
        this.view = null;
        workThread.quit();
    }
}
