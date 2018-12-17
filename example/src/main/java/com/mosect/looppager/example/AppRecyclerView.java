package com.mosect.looppager.example;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mosect.viewutils.InterceptTouchHelper;

public class AppRecyclerView extends RecyclerView {

    private InterceptTouchHelper interceptTouchHelper;

    public AppRecyclerView(@NonNull Context context) {
        super(context);
        init();
    }

    public AppRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AppRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        interceptTouchHelper = new InterceptTouchHelper(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean result = super.onInterceptTouchEvent(e);
        boolean ext = interceptTouchHelper.onInterceptTouchEvent(e);
        return result && ext;
    }
}
