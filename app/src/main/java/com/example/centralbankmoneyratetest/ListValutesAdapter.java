package com.example.centralbankmoneyratetest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

/**
 * Класс адаптер для списка валют
 */
public class ListValutesAdapter
        extends RecyclerView.Adapter<ListValutesAdapter.TaskViewHolder> {

private ArrayList<ValuteItem> items;
private Context parent;

    /**
     * Конструктор класса-адаптера.
     * @param items список с валютами
     * @param parent контекст
     */
    public ListValutesAdapter(ArrayList<ValuteItem> items, Context parent){
        this.items = items;
        this.parent = parent;
        }

public class TaskViewHolder extends RecyclerView.ViewHolder{
    TextView num_code, char_code, name, value;//Текстовые View для элемента списка
    public TaskViewHolder(@NonNull View itemView) {
        super(itemView);
        num_code = itemView.findViewById(R.id.num_code_item);
        char_code = itemView.findViewById(R.id.char_code_item);
        name = itemView.findViewById(R.id.name_item);
        value = itemView.findViewById(R.id.value_item);
    }
}




    @NonNull
    @Override
    public ListValutesAdapter.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.valute_item, parent, false);
        return new TaskViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, final int position) {
    holder.num_code.setText(items.get(position).getNumcode() + "");
    holder.char_code.setText(items.get(position).getCharcode() + "");
    holder.name.setText(items.get(position).getName() + "");
    holder.value.setText(items.get(position).getValue() + " руб.");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}