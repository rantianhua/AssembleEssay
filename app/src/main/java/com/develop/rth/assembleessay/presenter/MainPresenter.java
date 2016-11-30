package com.develop.rth.assembleessay.presenter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;

import com.develop.rth.assembleessay.adpter.AssembleEssayAdapter;
import com.develop.rth.assembleessay.contract.MainContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by rth on 16/11/15.
 */
public class MainPresenter implements MainContract.IMainPresenter<MainContract.IMainView> {

    private MainContract.IMainView view;
    private Handler workHandler;
    private HandlerThread workThread;
    private Handler mainHandler;
    private int showingContent = -1;

    private List<String> essayData;
    private List<String> tagsData;
    private Random random = new Random();

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

    private void realLoadEssay() {
        String[] essayArray = TEST_ESSAY.split(" ");
        essayData = new ArrayList<>();
        Collections.addAll(essayData,essayArray);
        showContentData(essayData, ShowingType.ESSAY);
    }

    private void showContentData(final List<String> datas,final int showingType) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (view == null) return;
                showingContent = showingType;
                view.onDataLoaded(datas, showingType);
            }
        });
    }

    @Override
    public void release() {
        this.view = null;
        workThread.quit();
    }

    @Override
    public void showTags() {
        if (showingContent == ShowingType.TAGS) {
            return;
        }
        if (tagsData == null) {
            workHandler.post(new Runnable() {
                @Override
                public void run() {
                    realLoadTags();
                }
            });
        }else {
            showContentData(tagsData, ShowingType.TAGS);
        }
    }

    @Override
    public void showEssay() {
        if (showingContent == ShowingType.ESSAY) {
            return;
        }
        if (essayData == null) {
            workHandler.post(new Runnable() {
                @Override
                public void run() {
                    realLoadEssay();
                }
            });
        }else {
            showContentData(essayData, ShowingType.ESSAY);
        }
    }

    @Override
    public void randomAdd(AssembleEssayAdapter adapter) {
        List<String> datas = adapter.getDatas();
        int insertPos = random.nextInt(datas.size());
        String data = null;
        if (showingContent == ShowingType.ESSAY) {
            datas.add(insertPos, "NewWordIn" + insertPos);
        }else if (showingContent == ShowingType.TAGS) {
            datas.add(insertPos, "新标签" + insertPos);
        }
        view.addItemIn(insertPos);
    }

    @Override
    public void randomDelete(AssembleEssayAdapter adapter) {
        List<String> datas = adapter.getDatas();
        int removePos = random.nextInt(datas.size());
        if (removePos >= datas.size()) removePos = datas.size()-1;
        datas.remove(removePos);
        view.removeItemIn(removePos);
    }

    private void realLoadTags() {
        tagsData = new ArrayList<>();
        tagsData.add("RxJava");
        tagsData.add("JavaScript");
        tagsData.add("PHP");
        tagsData.add("Python");
        tagsData.add("黑客");
        tagsData.add("作家");
        tagsData.add("创业肖邦");
        tagsData.add("世界末日");
        tagsData.add("流感病毒");
        tagsData.add("爸爸去哪儿");
        tagsData.add("钓鱼岛，我们的");
        tagsData.add("Github");
        tagsData.add("打飞机");
        tagsData.add("华尔街之狼");
        tagsData.add("黑暗骑士");
        tagsData.add("你的名字");
        tagsData.add("惊天魔盗团");
        tagsData.add("希拉里落选");
        tagsData.add("热门标签");
        tagsData.add("ImageView");
        tagsData.add("wheel无限循环");
        tagsData.add("ViewPager");
        tagsData.add("数据存储");
        tagsData.add("上拉加载");
        tagsData.add("dialog");
        tagsData.add("滑动浏览");
        tagsData.add("下载");
        tagsData.add("神奇动物在哪里");
        tagsData.add("video");
        tagsData.add("垂直ViewPager");
        tagsData.add("控件");
        tagsData.add("加载更多");
        tagsData.add("Retrofit");
        tagsData.add("肖生克的救赎");
        tagsData.add("工具");
        tagsData.add("加载动画");
        tagsData.add("EditText");
        tagsData.add("电锯惊魂");
        tagsData.add("狙击电话亭");
        tagsData.add("这个杀手不太冷");
        tagsData.add("阿甘正传");
        tagsData.add("十二怒汉");
        tagsData.add("海上钢琴师");
        tagsData.add("搏击俱乐部");
        tagsData.add("忠犬八公的故事");
        tagsData.add("卡比利亚之夜");
        showContentData(tagsData, ShowingType.TAGS);
    }

    public interface ShowingType {
        int TAGS = 0;
        int ESSAY = 1;
    }
}
