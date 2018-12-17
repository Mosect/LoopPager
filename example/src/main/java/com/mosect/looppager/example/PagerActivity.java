package com.mosect.looppager.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import com.mosect.looppager.LoopPager;

public class PagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);
        final EditText etPages = findViewById(R.id.et_pages);
        final EditText etBeforeCache = findViewById(R.id.et_beforeCache);
        final EditText etAfterCache = findViewById(R.id.et_afterCache);
        final EditText etSmoothVelocity = findViewById(R.id.et_smoothVelocity);

        final RadioButton rbHorizontal = findViewById(R.id.rb_horizontal);
        final RadioButton rbVertical = findViewById(R.id.rb_vertical);

        final CheckBox cbTouchScroll = findViewById(R.id.cb_touchScroll);
        final CheckBox cbLeft = findViewById(R.id.cb_left);
        final CheckBox cbTop = findViewById(R.id.cb_top);
        final CheckBox cbRight = findViewById(R.id.cb_right);
        final CheckBox cbBottom = findViewById(R.id.cb_bottom);

        // 默认值
        etPages.setText("5");
        etBeforeCache.setText("1");
        etAfterCache.setText("1");
        etSmoothVelocity.setText(String.valueOf(LoopPager.DEFAULT_SMOOTH_VELOCITY_DP));
        cbTouchScroll.setChecked(true);
        rbHorizontal.setChecked(true);
        cbLeft.setChecked(true);
        cbRight.setChecked(true);

        findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pages = AppUtils.getNumber(etPages);
                int beforeCache = AppUtils.getNumber(etBeforeCache);
                int afterCache = AppUtils.getNumber(etAfterCache);
                int loop = LoopPager.LOOP_NONE;
                int orientation = 0;
                if (cbLeft.isChecked()) {
                    loop |= LoopPager.LOOP_LEFT;
                }
                if (cbTop.isChecked()) {
                    loop |= LoopPager.LOOP_TOP;
                }
                if (cbRight.isChecked()) {
                    loop |= LoopPager.LOOP_RIGHT;
                }
                if (cbBottom.isChecked()) {
                    loop |= LoopPager.LOOP_BOTTOM;
                }
                if (rbHorizontal.isChecked()) {
                    orientation = LoopPager.ORIENTATION_HORIZONTAL;
                } else if (rbVertical.isChecked()) {
                    orientation = LoopPager.ORIENTATION_VERTICAL;
                } else {
                    orientation = LoopPager.ORIENTATION_HORIZONTAL;
                }
                Intent intent = new Intent(PagerActivity.this, InfoActivity.class);
                intent.putExtra("pages", pages);
                intent.putExtra("beforeCache", beforeCache);
                intent.putExtra("afterCache", afterCache);
                intent.putExtra("loop", loop);
                intent.putExtra("orientation", orientation);
                intent.putExtra("smoothVelocity", AppUtils.getNumber(etSmoothVelocity));
                intent.putExtra("touchScroll", cbTouchScroll.isChecked());
                startActivity(intent);
            }
        });
    }
}
