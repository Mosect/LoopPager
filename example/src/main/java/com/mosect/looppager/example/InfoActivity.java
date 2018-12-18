package com.mosect.looppager.example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mosect.looppager.LoopPager;
import com.mosect.looppager.PageHolder;
import com.mosect.looppager.PagerAdapter;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        final EditText etPage = findViewById(R.id.et_page);
        final LoopPager lpContent = findViewById(R.id.lp_content);

        findViewById(R.id.btn_jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转指定页
                int page = AppUtils.getNumber(etPage) - 1;
                if (page >= 0 && page < lpContent.getAdapter().getPageCount()) {
                    lpContent.setCurrentPage(page, true);
                }
            }
        });
        findViewById(R.id.btn_pre).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 上一页
                lpContent.jumpBeforePage(true);
            }
        });
        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 下一页
                lpContent.jumpAfterPage(true);
            }
        });

        int orientation = getIntent().getIntExtra("orientation", 0);
        final int pages = getIntent().getIntExtra("pages", 0);
        int beforeCache = getIntent().getIntExtra("beforeCache", 1);
        int afterCache = getIntent().getIntExtra("afterCache", 1);
        int loop = getIntent().getIntExtra("loop", LoopPager.LOOP_NONE);
        int smoothVelocityDp = getIntent().getIntExtra(
                "smoothVelocity", LoopPager.DEFAULT_SMOOTH_VELOCITY_DP);
        float smoothVelocity = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, smoothVelocityDp,
                getResources().getDisplayMetrics());
        boolean touchScroll = getIntent().getBooleanExtra("touchScroll", true);
        lpContent.setOrientation(orientation);
        lpContent.setLoop(loop);
        lpContent.setPageLimit(beforeCache, afterCache);
        lpContent.setSmoothVelocity(smoothVelocity);
        lpContent.setTouchScroll(touchScroll);
        lpContent.setAdapter(new PagerAdapter() {
            int[] bgColors = {
                    Color.parseColor("#0080ff"),
                    Color.parseColor("#808080"),
                    Color.parseColor("#558080"),
                    Color.parseColor("#008080"),
                    Color.parseColor("#00ff80"),
            };

            @Override
            public int getPageCount() {
                return pages;
            }

            @Override
            protected PageHolder onCreatePage(ViewGroup parent, int type) {
                View view = getLayoutInflater()
                        .inflate(R.layout.page_content, parent, false);
                final PageHolder holder = new PageHolder(view);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(InfoActivity.this,
                                "点击了：" + String.valueOf(holder.getAdapterPosition() + 1),
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return holder;
            }

            @Override
            protected void onBindPage(int position, PageHolder holder) {
                holder.view.setBackgroundColor(bgColors[position % bgColors.length]);
                TextView content = holder.view.findViewById(R.id.tv_content);
                String str = "当前页：" + String.valueOf(position + 1);
                content.setText(str);
            }
        });
        lpContent.setOnPageChangedListener(new LoopPager.OnPageChangedListener() {
            @Override
            public void onPageScroll(LoopPager view, int pagePosition, int pageOffset, int pageSize) {
                System.out.println(String.format("onPageScroll:pagePosition=%d,pageOffset=%d,pageSize=%d",
                        pagePosition, pageOffset, pageSize));
            }

            @Override
            public void onPageSelected(LoopPager view, int pagePosition) {
                System.out.println(String.format("onPageSelected:pagePosition=%d", pagePosition));
                if (pagePosition >= 0) {
                    etPage.setText(String.valueOf(pagePosition + 1));
                } else {
                    etPage.setText("");
                }
            }
        });
    }
}
