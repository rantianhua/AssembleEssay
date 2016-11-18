package com.example.rth.assembleessay.adpter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.rth.assembleessay.R;
import com.example.rth.assembleessay.util.DebugUtil;
import com.example.rth.assembleessay.widget.ItemTouchHelperAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rth on 16/11/15.
 */
public class AssembleEssayAdapter extends RecyclerView.Adapter<AssembleEssayAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private static final String TAG = "AssembleEssayAdapter";

    private List<String> datas = new ArrayList<>();
    private Context context;

    public AssembleEssayAdapter(Context context) {
        this.context = context;
    }

    public AssembleEssayAdapter(Context context, List<String> datas) {
        this(context);
        setDatas(datas);
    }

    public void setDatas(List<String> datas) {
        this.datas.clear();
        this.datas.addAll(datas);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DebugUtil.debugFormat("%s onCreateViewHolder",TAG);
        View view = LayoutInflater.from(context).inflate(R.layout.item_assemble_essay,parent,false);
        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final String word = datas.get(position);
        holder.getTvWord().setText(word);
        DebugUtil.debugFormat("%s onBindViewHolder: %s, pos:%s",TAG,word,position);
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        final String animStartString = datas.remove(fromPosition);
        datas.add(toPosition,animStartString);
        notifyItemMoved(fromPosition,toPosition);
    }

    public List<String> getDatas() {
        return datas;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvWord;

        ViewHolder(View itemView) {
            super(itemView);
            this.tvWord = (TextView) itemView;
        }

        TextView getTvWord() {
            return tvWord;
        }
    }

}
