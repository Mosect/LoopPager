package com.mosect.looppager;

import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.LinkedList;

/**
 * 页面适配器
 */
public abstract class PagerAdapter {

    private AdapterHost host;
    private SparseArray<LinkedList<PageHolder>> cacheHolders;

    /**
     * 设置宿主
     *
     * @param host 宿主
     */
    public void setHost(AdapterHost host) {
        this.host = host;
    }

    /**
     * 获取宿主
     *
     * @return 宿主
     */
    public AdapterHost getHost() {
        return host;
    }

    /**
     * 通知宿主，数据发生更改
     */
    public void notifyDataChanged() {
        if (null != host) {
            host.onAdapterDataChanged(this);
        }
    }

    /**
     * 获取页的分类
     *
     * @param position 页位置
     * @return 页分类
     */
    public int getPageType(int position) {
        return 0;
    }

    /**
     * 锁定某页，即加载某页，同一页可能会被加载多次
     *
     * @param parent   父视图
     * @param position 页位置
     * @return 页持有者
     */
    public PageHolder lockPage(ViewGroup parent, int position) {
        int type = getPageType(position);
        PageHolder holder = null;
        if (null != cacheHolders) {
            LinkedList<PageHolder> list = cacheHolders.get(type);
            if (null != list && list.size() > 0) {
                holder = list.removeFirst();
            }
        }
        if (null == holder) {
            holder = onCreatePage(parent, type);
            holder.setType(type);
        }
        holder.setAdapterPosition(position);
        onBindPage(position, holder);
        return holder;
    }

    /**
     * 解锁某页（移除某页）
     *
     * @param holder 页持有者
     */
    public void unlockPage(PageHolder holder) {
        if (null == cacheHolders) {
            cacheHolders = new SparseArray<>();
        }
        LinkedList<PageHolder> list = cacheHolders.get(holder.getType());
        if (null == list) {
            list = new LinkedList<>();
            cacheHolders.put(holder.getType(), list);
        }
        list.addLast(holder);
    }

    /**
     * 获取页的数量
     *
     * @return 页数量
     */
    public abstract int getPageCount();

    /**
     * 创建某页
     *
     * @param parent 父视图
     * @param type   页分类
     * @return 页持有者
     */
    protected abstract PageHolder onCreatePage(ViewGroup parent, int type);

    /**
     * 绑定数据到某页
     *
     * @param position 页下标
     * @param holder   页持有者
     */
    protected abstract void onBindPage(int position, PageHolder holder);
}
