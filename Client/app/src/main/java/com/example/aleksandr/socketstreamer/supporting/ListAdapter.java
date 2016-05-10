package com.example.aleksandr.socketstreamer.supporting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.aleksandr.socketstreamer.R;
import com.example.aleksandr.socketstreamer.data.Abstractions.StreamData;

import java.util.ArrayList;

/**
 * Created by Aleksandr on 10.05.2016.
 */
public class ListAdapter extends BaseAdapter {
    Context context;
    ArrayList<StreamData> data;
    private static LayoutInflater inflater = null;

    public ListAdapter(Context context, ArrayList<StreamData> data) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View view = convertView;
        if (view == null)
            view = inflater.inflate(R.layout.item_row, null);
        TextView name = (TextView) view.findViewById(R.id.lbl_nameEnter);
        TextView id = (TextView)   view.findViewById(R.id.lbl_idEnter);
        if (data.get(position).getName() == null){
            name.setText("Не указано");
        }
        else{
            name.setText(data.get(position).getName());
        }
        id.setText(data.get(position).getId());
        return view;
    }
}
