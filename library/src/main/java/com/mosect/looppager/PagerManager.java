package com.mosect.looppager;

import android.view.ViewGroup;

import java.util.Arrays;

/**
 * 页面管理器，管理页面增、删、改、查，屏蔽垂直与水平的差异，统一使用前或后表示。
 */
public abstract class PagerManager {

    private ViewGroup parent;
    private int beforeCacheCount;
    private int afterCacheCount;
    private boolean beforeLoop;
    private boolean afterLoop;
    private PagerAdapter pagerAdapter;
    private PageHolder[] pageHolders;
    private int showOffset;
    private int showPages;
    private int jumpType;

    public PagerManager(ViewGroup parent) {
        this.parent = parent;
        init(1, 1, true, true, null);
    }

    @Override
    public String toString() {
        return String.format("{pageHolders=%s}", Arrays.toString(pageHolders));
    }

    /**
     * 初始化页管理器
     *
     * @param beforeCacheCount 前缓存数量
     * @param afterCacheCount  后缓存数量
     * @param beforeLoop       前是否支持循环加载
     * @param afterLoop        后是否支持循环加载
     * @param pagerAdapter     页面适配器
     */
    public void init(int beforeCacheCount,
                     int afterCacheCount,
                     boolean beforeLoop,
                     boolean afterLoop,
                     PagerAdapter pagerAdapter) {
        if (beforeCacheCount < 1) {
            throw new IllegalArgumentException("beforeCacheCount must more than 0");
        }
        if (afterCacheCount < 1) {
            throw new IllegalArgumentException("afterCacheCount must more than 0");
        }

        if (null != this.pageHolders) {
            for (int i = 0; i < pageHolders.length; i++) {
                removePage(i);
            }
        }

        this.jumpType = 0;
        if (null == this.pageHolders ||
                this.pageHolders.length != beforeCacheCount + afterCacheCount + 1) {
            this.pageHolders = new PageHolder[beforeCacheCount + 1 + afterCacheCount];
        }
        this.pagerAdapter = pagerAdapter;
        this.beforeLoop = beforeLoop;
        this.afterLoop = afterLoop;
        this.beforeCacheCount = beforeCacheCount;
        this.afterCacheCount = afterCacheCount;

        int cur = null != pagerAdapter && pagerAdapter.getPageCount() > 0 ? 0 : -1;
        initPages(cur);
    }

    /**
     * 页面更新，在适配器发生更改，即{@link PagerAdapter#notifyDataChanged() 通知数据更新}方法触发时，
     * 应该调用本方法，更新缓存的页面
     */
    public void update() {
        // 获取当前页面对应的适配器位置
        int cur = null == pageHolders[getCurrentPageIndex()] ? -1 :
                pageHolders[getCurrentPageIndex()].getAdapterPosition();

        // 移除所有页面
        for (int i = 0; i < pageHolders.length; i++) {
            removePage(i);
        }

        if (null != pagerAdapter && pagerAdapter.getPageCount() > 0) {
            // 存在页面
            if (cur < 0) { // 以前没有选定页面
                cur = 0; // 选定第0页
            } else if (cur > pagerAdapter.getPageCount() - 1) {
                // 超出最后一页，选定最后一页
                cur = pagerAdapter.getPageCount() - 1;
            }
        } else {
            // 不存在页面
            if (cur >= 0) {
                cur = -1; // 不选定任何页面
            }
        }

        initPages(cur);
    }

    /**
     * 准备跳转至某一页，执行此方法后，必须调用{@link #finishJump() 完成跳转}方法后才能进行其他操作
     *
     * @param adapterPosition 页数
     */
    public void readyJump(int adapterPosition) {
        checkJumpType();
        if (null != pagerAdapter && adapterPosition >= 0 &&
                adapterPosition < pagerAdapter.getPageCount()) {
            int cur = getShowPage(getCurrentPageIndex()).getAdapterPosition();
            if (adapterPosition < cur) { // 在前面
                jumpType = -1;
                // 移除并重新加载之前页面
                int pos = adapterPosition;
                for (int i = beforeCacheCount - 1; i >= 0; i--) {
                    removePage(i);
                    addBeforePage(i, pos--);
                }
                updatePagesInfo();

            } else if (adapterPosition > cur) { // 在后面
                jumpType = 1;
                // 移除并重新加载之后页面
                int pos = adapterPosition;
                for (int i = beforeCacheCount + 1; i < pageHolders.length; i++) {
                    removePage(i);
                    addAfterPage(i, pos++);
                }
                updatePagesInfo();
            }
//            System.out.println("PagerManager.readyJump:" + this);
        }
    }

    /**
     * 完成已准备好的页面跳转跳转
     */
    public void finishJump() {
        if (jumpType > 0) {
            // 加载了后面的页面
            jumpType = 0; // 重置跳转标志
            after(1); // 往后加载一页，让准备好的页面往前挪一个位置

            // 重新加载前面的页面
            int pos = getShowPage(getCurrentPageIndex()).getAdapterPosition();
            for (int i = beforeCacheCount - 1; i >= 0; i--) {
                removePage(i);
                addBeforePage(i, --pos);
            }
            updatePagesInfo();
//            System.out.println("PagerManager.finishJump:" + this);
        } else if (jumpType < 0) {
            // 加载了前面的页面
            jumpType = 0; // 重置跳转标志
            before(1); // 往前加载一页，让准备好的页面往后挪一个位置

            // 重新加载后面的页面
            int pos = getShowPage(getCurrentPageIndex()).getAdapterPosition();
            for (int i = beforeCacheCount + 1; i < pageHolders.length; i++) {
                removePage(i);
                addAfterPage(i, ++pos);
            }
            updatePagesInfo();
//            System.out.println("PagerManager.finishJump:" + this);
        }
    }

    public PagerAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    /**
     * 获取已展示的页面数量（已缓存）
     *
     * @return 已展示的页面数量
     */
    public int getShowPages() {
        return showPages;
    }

    /**
     * 获取展示的页
     *
     * @param index 页下标
     * @return 页
     */
    public PageHolder getShowPage(int index) {
        if (index >= 0 && index < getShowPages()) {
            return pageHolders[showOffset + index];
        }
        return null;
    }

    /**
     * 更改当前页
     *
     * @param index 页缓存下标
     */
    public void setCurrentPage(int index) {
        checkJumpType();
        if (index >= 0 && index < getShowPages() && getCurrentPageIndex() != index) {
            int offset = index - getCurrentPageIndex();
            if (offset > 0) { // 加载后面的页
                after(offset);
                updatePagesInfo();
//                System.out.println(String.format("PagerManager.setCurrentPage[%d]:%s", index, this.toString()));
            } else { // 加载前面的页
                before(-offset);
                updatePagesInfo();
//                System.out.println(String.format("PagerManager.setCurrentPage[%d]:%s", index, this.toString()));
            }
        }
    }

    /**
     * 获取当前页下标
     *
     * @return 当前页下标；负数表示不存在
     */
    public int getCurrentPageIndex() {
        if (getShowPages() > 0) {
            return beforeCacheCount - showOffset;
        }
        return -1;
    }

    public int getBeforeCacheCount() {
        return beforeCacheCount;
    }

    public int getAfterCacheCount() {
        return afterCacheCount;
    }

    /**
     * 获取跳转类型，使用{@link #readyJump(int) 准备跳转}方法更改跳转类型
     *
     * @return 预备跳转类型；0，没有准备跳转；正数，准备往后面跳转；负数，准备往前面跳转
     */
    public int getJumpType() {
        return jumpType;
    }

    /**
     * 加载后面的页
     *
     * @param count 页数量
     */
    private void after(int count) {
        // 移除前面的页
        for (int i = 0; i < count; i++) {
            removePage(i);
        }
        // 挪动页
        for (int i = 0; i < pageHolders.length - count; i++) {
            pageHolders[i] = pageHolders[i + count];
            pageHolders[i + count] = null;
        }
        // 加载后面的页
        for (int i = pageHolders.length - count; i < pageHolders.length; i++) {
            if (null != pageHolders[i - 1]) {
                addAfterPage(i, pageHolders[i - 1].getAdapterPosition() + 1);
            }
        }
    }

    /**
     * 加载前面的页
     *
     * @param count 页数量
     */
    private void before(int count) {
        // 移除后面的页
        for (int i = pageHolders.length - 1; i >= pageHolders.length - count; i--) {
            removePage(i);
        }
        // 挪动页
        for (int i = pageHolders.length - 1; i >= count; i--) {
            pageHolders[i] = pageHolders[i - count];
            pageHolders[i - count] = null;
        }
        // 加载前面的页
        for (int i = count - 1; i >= 0; i--) {
            if (null != pageHolders[i + 1]) {
                addBeforePage(i, pageHolders[i + 1].getAdapterPosition() - 1);
            }
        }
    }

    /**
     * 初始化页
     *
     * @param currentAdapterPosition 当前页面适配器下标
     */
    private void initPages(int currentAdapterPosition) {
        if (null != this.pagerAdapter && currentAdapterPosition >= 0 &&
                this.pagerAdapter.getPageCount() > 0) {
            // 加载当前页
            addPage(beforeCacheCount, currentAdapterPosition);

            // 加载之前的页
            int beforePos = currentAdapterPosition;
            for (int i = 0; i < beforeCacheCount; i++) {
                int index = beforeCacheCount - 1 - i;
                addBeforePage(index, --beforePos);
            }

            // 加载之后的页
            int afterPos = currentAdapterPosition;
            for (int i = 0; i < afterCacheCount; i++) {
                int index = i + beforeCacheCount + 1;
                addAfterPage(index, ++afterPos);
            }
        }
        updatePagesInfo();
//        System.out.println("PagerManager.init:" + this);
    }

    /**
     * 更新页信息
     */
    private void updatePagesInfo() {
        showOffset = -1;
        showPages = 0;
        for (int i = 0; i < pageHolders.length; i++) {
            if (showOffset < 0) {
                if (null != pageHolders[i]) {
                    showOffset = i;
                    showPages++;
                }
            } else {
                if (null != pageHolders[i]) {
                    showPages++;
                } else {
                    break;
                }
            }
        }
    }

    private void checkJumpType() {
        if (jumpType != 0) {
            throw new IllegalStateException("Jumping, must finish jump!!!");
        }
    }

    /**
     * 移除某页
     *
     * @param index 缓存下标
     */
    private void removePage(int index) {
        PageHolder holder = pageHolders[index];
        if (null != holder) {
            onRemovePage(parent, holder);
            if (null != holder.view.getParent()) {
                String format = "Must remove view[PageHolder:%s] at parent[%s]";
                String msg = String.format(format, holder, holder.view.getParent());
                throw new IllegalStateException(msg);
            }
            pagerAdapter.unlockPage(holder);
            pageHolders[index] = null;
        }
    }

    /**
     * 添加某页
     *
     * @param index           缓存下标
     * @param adapterPosition 页的适配器位置
     */
    private void addPage(int index, int adapterPosition) {
        PageHolder holder = pagerAdapter.lockPage(parent, adapterPosition);
        onAddPage(parent, holder);
        pageHolders[index] = holder;
    }

    /**
     * 添加之前的页
     *
     * @param index 页缓存下标
     * @param pos   页的适配器位置
     */
    private void addBeforePage(int index, int pos) {
        if (pos >= 0) {
            addPage(index, pos);
        } else {
            if (beforeLoop) {
                int adapterPosition = pagerAdapter.getPageCount() -
                        (-pos) % pagerAdapter.getPageCount();
                addPage(index, adapterPosition);
            } else {
                pageHolders[index] = null;
            }
        }
    }

    /**
     * 添加之后的页
     *
     * @param index 页缓存下标
     * @param pos   页的适配器位置
     */
    private void addAfterPage(int index, int pos) {
        if (pos < pagerAdapter.getPageCount()) {
            addPage(index, pos);
        } else {
            if (afterLoop) {
                int adapterPosition = pos % pagerAdapter.getPageCount();
                addPage(index, adapterPosition);
            } else {
                pageHolders[index] = null;
            }
        }
    }

    /**
     * 往父视图增加某页
     *
     * @param parent 父视图
     * @param holder 页持有者
     */
    protected abstract void onAddPage(ViewGroup parent, PageHolder holder);

    /**
     * 从父视图移除某页
     *
     * @param parent 父视图
     * @param holder 页持有者
     */
    protected abstract void onRemovePage(ViewGroup parent, PageHolder holder);
}
