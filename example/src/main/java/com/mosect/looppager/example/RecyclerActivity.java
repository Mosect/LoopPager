package com.mosect.looppager.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mosect.looppager.LoopPager;
import com.mosect.looppager.PageHolder;
import com.mosect.looppager.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class RecyclerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);

        RecyclerView rvContent = findViewById(R.id.rv_content);
        LinearLayoutManager llm = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false);
        rvContent.setLayoutManager(llm);
        rvContent.setAdapter(new RecyclerView.Adapter() {
            List<ItemEntity> items;

            {
                items = new ArrayList<>();
                ItemEntity header = new ItemEntity();
                header.type = 0;
                header.object = createHeaderObject();
                items.add(header);

                for (int i = 0; i < 50; i++) {
                    ItemEntity item = new ItemEntity();
                    item.type = 1;
                    item.object = String.valueOf(i);
                    items.add(item);
                }
            }

            @Override
            public int getItemViewType(int position) {
                return items.get(position).type;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
                if (type == 0) {
                    View view = getLayoutInflater().inflate(R.layout.item1, viewGroup, false);
                    return new ItemHolder(view);
                } else {
                    View view = getLayoutInflater().inflate(R.layout.item2, viewGroup, false);
                    return new ItemHolder(view);
                }
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
                ItemEntity item = items.get(position);
                if (item.type == 0) {
                    LoopPager loopPager = viewHolder.itemView.findViewById(R.id.ly_pager);
                    loopPager.setAdapter(null);
                    loopPager.setAdapter((PagerAdapter) item.object);
                } else {
                    TextView textView = viewHolder.itemView.findViewById(R.id.tv_text);
                    textView.setText("ITEM：" + position);
                }
            }

            @Override
            public int getItemCount() {
                return items.size();
            }
        });
    }

    private PagerAdapter createHeaderObject() {
        return new PagerAdapter() {
            @Override
            public int getPageCount() {
                return 5;
            }

            @Override
            protected PageHolder onCreatePage(ViewGroup parent, int type) {
                View view = getLayoutInflater().inflate(R.layout.page, parent, false);
                final PageHolder holder = new PageHolder(view);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(RecyclerActivity.this,
                                "点击了页：" + holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
                    }
                });
                return holder;
            }

            @Override
            protected void onBindPage(int position, PageHolder holder) {
                TextView textView = holder.view.findViewById(R.id.tv_text);
                textView.setText("页：" + position);
            }
        };
    }

    private static class ItemEntity {

        int type;
        Object object;
    }

    private static class ItemHolder extends RecyclerView.ViewHolder {

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
