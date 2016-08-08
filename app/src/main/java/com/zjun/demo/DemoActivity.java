package com.zjun.demo;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.zjun.view.drag_list_view.DragListView;
import com.zjun.view.drag_list_view.DragListViewAdapter;

import java.util.ArrayList;
import java.util.List;

public class DemoActivity extends AppCompatActivity {

    private DragListView dvl_drag_list;
    private TextView tv_msg_drag_list;

    private List<String> mDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initData();
        initView();
    }

    private void initData() {
        mDataList = new ArrayList<>();
        for (int i = 0; i < 50; ++i) {
            mDataList.add("条目" + i);
        }
    }

    private void initView() {
        tv_msg_drag_list = $(R.id.tv_msg_drag_list);
        dvl_drag_list = $(R.id.dvl_drag_list);

        tv_msg_drag_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = mDataList.size();
                String dataMsg;
                if (size == 0) {
                    dataMsg = "没有数据了";
                } else {
                    dataMsg = "数据大小：" + mDataList.size() + ", 最后一个：" + mDataList.get(mDataList.size() - 1);
                }
                tv_msg_drag_list.setText(dataMsg);
            }
        });

        // 1、添加Header测试
        TextView header = new TextView(this);
        header.setBackgroundColor(Color.GREEN);
        AbsListView.LayoutParams p = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50);
        header.setLayoutParams(p);
        String headerText = "Header of DragListView";
        header.setText(headerText);
        dvl_drag_list.addHeaderView(header);

        // 2、设置Adapter
        BaseAdapter listAdapter = new MyAdapter(this, mDataList);
        dvl_drag_list.setAdapter(listAdapter);

        // 3、设置条目点击监听事件
        dvl_drag_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 先判断位置的有效性
                int index = position - dvl_drag_list.getHeaderViewsCount();
                if (index < 0 || index >= mDataList.size()) {
                    return;
                }
                toast(mDataList.get(index));
            }
        });

        // 4、设置条目长按监听事件
        dvl_drag_list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // 有长按监听事件，为了防止与删除产生冲突，此句必须加上。
                dvl_drag_list.setLongClickFlag();
                int index = position - dvl_drag_list.getHeaderViewsCount();
                if (index < 0 || index >= mDataList.size()) {
                    return false;
                }
                toast(mDataList.get(index) + "，被长按");
                return true;
            }
        });
    }

    public class MyAdapter extends DragListViewAdapter<String>{

        public MyAdapter(Context context, List<String> dataList) {
            super(context, dataList);
        }

        @Override
        public View getItemView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_drag_list, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.tv_name_drag_list);
                viewHolder.desc = (TextView) convertView.findViewById(R.id.tv_desc_drag_list);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.name.setText(mDragDatas.get(position));
            String s = mDragDatas.get(position) + "的描述";
            viewHolder.desc.setText(s);
            return convertView;
        }

        class ViewHolder{
            TextView name;
            TextView desc;
        }

    }


    @SuppressWarnings("unchecked")
    private <V  extends View> V $(int id) {
        return (V) findViewById(id);
    }

    private Toast mToast;
    private void toast(String info) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(info);
        mToast.show();
    }

}
