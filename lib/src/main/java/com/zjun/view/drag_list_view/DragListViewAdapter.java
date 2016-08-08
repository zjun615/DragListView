package com.zjun.view.drag_list_view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.Collections;
import java.util.List;

/**
 * Created by Ralap on 2016/5/10.
 */
public abstract class DragListViewAdapter<T> extends BaseAdapter{

    protected Context mContext;
    protected List<T> mDragDatas;

    public DragListViewAdapter(Context context, List<T> dataList){
        this.mContext = context;
        this.mDragDatas = dataList;
    }

    @Override
    public int getCount() {
        return mDragDatas == null ? 0 : mDragDatas.size();
    }

    @Override
    public T getItem(int position) {
        return mDragDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItemView(position, convertView, parent);
    }

    public abstract View getItemView(int position, View convertView, ViewGroup parent);

    public void swapData(int from, int to){
        Collections.swap(mDragDatas, from, to);
        notifyDataSetChanged();
    }

    public void deleteData(int index) {
        mDragDatas.remove(index);
        notifyDataSetChanged();
    }

    public void addData(int location, T data) {
        mDragDatas.add(location, data);
        notifyDataSetChanged();
    }

    public void setDataList(List<T> dataList) {
        mDragDatas = dataList;
        notifyDataSetChanged();
    }

    public List<T> getDataList(){
        return mDragDatas;
    }
}
