package com.mosect.looppager;

import android.annotation.SuppressLint;
import android.view.View;

/**
 * 页持有者，将页与视图联系起来
 */
public class PageHolder {

    public final View view; // 页的视图
    private int type; // 页的分类
    private int adapterPosition = -1; // 页对应的适配器的位置

    public PageHolder(View view) {
        this.view = view;
    }

    /**
     * 获取页分类
     *
     * @return 页的分类
     */
    public int getType() {
        return type;
    }

    void setType(int type) {
        this.type = type;
    }

    /**
     * 获取页的适配器位置
     *
     * @return 页的适配器位置
     */
    public int getAdapterPosition() {
        return adapterPosition;
    }

    void setAdapterPosition(int adapterPosition) {
        this.adapterPosition = adapterPosition;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{view=%d,type=%d,adapterPosition=%d}",
                view.getId(), type, adapterPosition);
    }
}
