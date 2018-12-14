package com.mosect.looppager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * 循环的页控件（类似ViewPager）
 */
public class LoopPager extends FrameLayout implements AdapterHost {

    /**
     * 左边可循环
     */
    public static final int LOOP_LEFT = 0x01;
    /**
     * 上边可循环
     */
    public static final int LOOP_TOP = 0x02;
    /**
     * 右边可循环
     */
    public static final int LOOP_RIGHT = 0x04;
    /**
     * 先把可循环
     */
    public static final int LOOP_BOTTOM = 0x08;
    /**
     * 水平可循环
     */
    public static final int LOOP_HORIZONTAL = LOOP_LEFT | LOOP_RIGHT;
    /**
     * 垂直可循环
     */
    public static final int LOOP_VERTICAL = LOOP_TOP | LOOP_BOTTOM;
    /**
     * 四边都可以循环
     */
    public static final int LOOP_ALL = LOOP_HORIZONTAL | LOOP_VERTICAL;
    /**
     * 四边都不可以循环
     */
    public static final int LOOP_NONE = 0;

    /**
     * 方向，水平
     */
    public static final int ORIENTATION_HORIZONTAL = 1;
    /**
     * 方向，垂直
     */
    public static final int ORIENTATION_VERTICAL = 2;

    /**
     * 初始化PagerManager
     */
    private static final int ACTION_FLAG_INIT = 0x01;
    /**
     * 是否处于滑动中
     */
    private static final int ACTION_FLAG_TOUCHING = 0x02;
    /**
     * 是否需要重置滑动
     */
    private static final int ACTION_FLAG_RESET_TOUCH = 0x04;
    /**
     * 是否可以触摸滑动页面
     */
    private static final int ACTION_FLAG_TOUCH_SCROLL = 0x08;

    /**
     * 默认平滑速度，单位：dp/s
     */
    public static final int DEFAULT_SMOOTH_VELOCITY_DP = 550;

    private int actionFlag;

    private PagerManager pagerManager; // 页面管理器
    private PagerAdapter adapter; // 适配器
    private int beforeCacheCount; // 前缓存数量
    private int afterCacheCount; // 后缓存数量
    private int loop; // 循环配置
    private int orientation; // 方向
    private OnPageChangedListener onPageChangedListener;

    // 辅助布局对象
    private Rect layoutContainer = new Rect();
    private Rect layoutOut = new Rect();

    private Scroller scroller; // 滑动器
    private int pagerScrollX; // 当前页面滑动X
    private int pagerScrollY; // 当前页面滑动Y

    private TouchTool touchTool; // 滑动工具，处理滑动方向、偏移量等
    private InterceptTouchHelper interceptTouchHelper; // 拦截滑动事件辅助器，决定是否拦截滑动事件
    private int touchPagerScrollX; // 滑动时，记录的页面滑动偏移量X
    private int touchPagerScrollY; // 滑动时，记录的页面滑动偏移量Y
    private VelocityTracker velocityTracker; // 速度计算器
    private float smoothVelocity; // 平滑速度，像素/秒

    public LoopPager(Context context) {
        super(context);
        init(null);
    }

    public LoopPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LoopPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        beforeCacheCount = 1;
        afterCacheCount = 1;
        loop = LOOP_ALL;
        orientation = ORIENTATION_HORIZONTAL;
        smoothVelocity = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SMOOTH_VELOCITY_DP, getContext().getResources().getDisplayMetrics());
        boolean touchScroll = true;
        if (null != attrs) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.LoopPager);
            beforeCacheCount = ta.getInteger(R.styleable.LoopPager_beforeCache, beforeCacheCount);
            afterCacheCount = ta.getInteger(R.styleable.LoopPager_afterCache, afterCacheCount);
            loop = ta.getInteger(R.styleable.LoopPager_loop, loop);
            orientation = ta.getInteger(R.styleable.LoopPager_orientation, orientation);
            smoothVelocity = ta.getDimension(R.styleable.LoopPager_smoothVelocity, smoothVelocity);
            touchScroll = ta.getBoolean(R.styleable.LoopPager_touchScroll, true);
            ta.recycle();
        }
        setTouchScroll(touchScroll);

        scroller = new Scroller(getContext());
        touchTool = new TouchTool(getContext());
        interceptTouchHelper = new InterceptTouchHelper(this);
        velocityTracker = VelocityTracker.obtain();
        pagerManager = new PagerManager(this) {
            @Override
            protected void onAddPage(ViewGroup parent, PageHolder holder) {
                LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                lp.pageHolder = holder;
                addView(holder.view, lp);
            }

            @Override
            protected void onRemovePage(ViewGroup parent, PageHolder holder) {
                removeView(holder.view);
            }
        };
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        touchTool.onTouchEvent(ev);
        boolean result = interceptTouchHelper.onInterceptTouchEvent(ev);
        if (!isTouchScroll()) {
            return false;
        }
        return result;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchScroll()) {
            return false;
        }

        touchTool.onTouchEvent(event);
        velocityTracker.addMovement(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            touchPagerScrollX = getPagerScrollX();
            touchPagerScrollY = getPagerScrollY();

            // 停止滑动动画
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            actionFlag |= ACTION_FLAG_TOUCHING;
            actionFlag &= ~ACTION_FLAG_RESET_TOUCH; // 取消重置滑动
        }

        boolean reset = false; // 表示是否需要重置起始点，滑动超出有效范围应该设置为true
        if ((actionFlag & ACTION_FLAG_RESET_TOUCH) != 0) {
            actionFlag &= ~ACTION_FLAG_RESET_TOUCH; // 取消重置标志
            reset = true;
        }

        if (touchTool.getTouchType() == TouchTool.TOUCH_TYPE_HORIZONTAL &&
                orientation == ORIENTATION_HORIZONTAL) { // 水平方向
            // 滑动点 = 起始点 - 滑动的偏移量（当前滑动位置 - 起始滑动滑动位置）
            int x = (int) (touchPagerScrollX - touchTool.getRangeX());
            if (x < 0) {
                x = 0;
                reset = true;
            } else if (x + computeHorizontalScrollExtent() > computeHorizontalScrollRange()) {
                x = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
                reset = true;
            }
            pagerScrollTo(x, 0);

        } else if (touchTool.getTouchType() == TouchTool.TOUCH_TYPE_VERTICAL &&
                orientation == ORIENTATION_VERTICAL) { // 垂直方向

            // 滑动点 = 起始点 - 滑动的偏移量（当前滑动位置 - 起始滑动滑动位置）
            int y = (int) (touchPagerScrollY - touchTool.getRangeY());
            if (y < 0) {
                y = 0;
                reset = true;
            } else if (y + computeVerticalScrollExtent() > computeVerticalScrollRange()) {
                y = computeVerticalScrollRange() - computeVerticalScrollExtent();
                reset = true;
            }
            pagerScrollTo(0, y);
        }

        if (reset) {
            touchTool.resetFirst(event);
            touchPagerScrollX = getPagerScrollX();
            touchPagerScrollY = getPagerScrollY();
        }

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) { // 滑动抬起或取消
            actionFlag &= ~ACTION_FLAG_TOUCHING; // 取消触摸
            velocityTracker.computeCurrentVelocity(1000); // 计算速度

            if (orientation == ORIENTATION_HORIZONTAL &&
                    computeHorizontalScrollExtent() > 0 &&
                    touchTool.getTouchType() == TouchTool.TOUCH_TYPE_HORIZONTAL) {

                // 水平方向抬起或取消处理：
                // 1.布局方向必须是水平
                // 2.页的大小（宽度）必须大于0
                // 3.滑动方向必须是水平（初次决定的方向）

                float xv = velocityTracker.getXVelocity();
                int index; // 需要滑动至的页面下标
                if (xv >= smoothVelocity) { // 右滑超过特定速度，滑动至当前页（偏移量为0）
                    index = getPagerScrollX() / computeHorizontalScrollExtent();

                } else if (xv <= -smoothVelocity) { // 左滑超过特定速度，滑动至下一页
                    index = getPagerScrollX() / computeHorizontalScrollExtent() + 1;

                } else {
                    // 计算相对于当前滑动页的偏移量
                    int offset = getPagerScrollX() % computeHorizontalScrollExtent();
                    // 计算当前滑动页属于第几页
                    index = getPagerScrollX() / computeHorizontalScrollExtent();
                    // 如果超过一半，应该滑动至下一页
                    if (offset > computeHorizontalScrollExtent() / 2) {
                        index++;
                    }
                }

                int x = computeHorizontalScrollExtent() * index; // 滑动的终点
                // 限定滑动的终点，不能超出有效范围
                if (x < 0) {
                    x = 0;
                } else if (x + computeHorizontalScrollExtent() > computeHorizontalScrollRange()) {
                    x = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
                }
                // 平滑至指定位置
                smoothPagerScrollTo(x, 0);

            } else if (orientation == ORIENTATION_VERTICAL &&
                    computeVerticalScrollExtent() > 0 &&
                    touchTool.getTouchType() == TouchTool.TOUCH_TYPE_VERTICAL) {
                // 垂直方向抬起或取消处理：
                // 1.布局方向必须是垂直
                // 2.页的大小（高度）必须大于0
                // 3.滑动方向必须是垂直（初次决定的方向）

                float yv = velocityTracker.getYVelocity();
                int index; // 需要滑动至的页面下标
                if (yv >= smoothVelocity) { // 下滑超过特定速度，滑动至当前页（偏移量为0）
                    index = getPagerScrollY() / computeVerticalScrollExtent();

                } else if (yv <= -smoothVelocity) { // 左滑超过特定速度，滑动至下一页
                    index = getPagerScrollY() / computeVerticalScrollExtent() + 1;

                } else {
                    // 计算相对于当前滑动页的偏移量
                    int offset = getPagerScrollY() % computeVerticalScrollExtent();
                    // 计算当前滑动页属于第几页
                    index = getPagerScrollY() / computeVerticalScrollExtent();
                    // 如果超过一半，应该滑动至下一页
                    if (offset > computeVerticalScrollExtent() / 2) {
                        index++;
                    }
                }

                int y = computeVerticalScrollExtent() * index; // 滑动的终点
                // 限定滑动的终点，不能超出有效范围
                if (y <= 0) {
                    y = 0;
                } else if (y + computeVerticalScrollExtent() >= computeVerticalScrollRange()) {
                    y = computeVerticalScrollRange() - computeVerticalScrollExtent();
                }
                // 平滑至指定位置
                smoothPagerScrollTo(0, y);

            }
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren();
        if ((actionFlag & ACTION_FLAG_INIT) != 0) {
            initPagerManager();
            actionFlag &= ~ACTION_FLAG_INIT;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
//            System.out.println(String.format("computeScroll:curX=%d,curY=%d,finalX=%d,finalY=%d,pagerX=%d,pagerY=%d",
//                    scroller.getCurrX(), scroller.getCurrY(), scroller.getFinalX(), scroller.getFinalY(), getPagerScrollX(), getPagerScrollY()));
            if (scroller.getCurrX() == scroller.getFinalX() &&
                    getPagerScrollX() == scroller.getFinalX() &&
                    scroller.getCurrY() == scroller.getFinalY() &&
                    getPagerScrollY() == scroller.getFinalY()) {
                // 已到终点，停止滑动
                scroller.abortAnimation();
            }
            pagerScrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        int showPages = pagerManager.getShowPages();
        if (showPages > 0) {
            return showPages * computeVerticalScrollExtent();
        }
        return computeVerticalScrollExtent();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return pagerScrollY;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        int showPages = pagerManager.getShowPages();
        if (showPages > 0) {
            return showPages * computeHorizontalScrollExtent();
        }
        return computeHorizontalScrollExtent();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return pagerScrollX;
    }

    @Override
    public void onAdapterDataChanged(PagerAdapter adapter) {
        if (adapter == this.adapter) {
            pagerManager.update(); // 更新页面
        }
    }

    public OnPageChangedListener getOnPageChangedListener() {
        return onPageChangedListener;
    }

    public void setOnPageChangedListener(OnPageChangedListener onPageChangedListener) {
        this.onPageChangedListener = onPageChangedListener;
    }

    /**
     * 获取页面滑动位置X
     *
     * @return 页面滑动位置X
     */
    public int getPagerScrollX() {
        return pagerScrollX;
    }

    /**
     * 获取页面滑动位置Y
     *
     * @return 页面滑动位置Y
     */
    public int getPagerScrollY() {
        return pagerScrollY;
    }

    /**
     * 页面滑动至某个位置
     *
     * @param x 位置X
     * @param y 位置Y
     */
    public void pagerScrollTo(int x, int y) {
        int oldX = pagerScrollX;
        int oldY = pagerScrollY;
        pagerScrollX = x;
        pagerScrollY = y;
        layoutChildren();
        invalidate();
        changePagerScroll();
        onPagerScrollChanged(false, oldX, oldY, x, y);
    }

    /**
     * 页面平滑至某个位置，会触发{@link #doSmooth(Scroller, int, int) doSmooth}方法
     *
     * @param x 位置X
     * @param y 位置Y
     */
    public void smoothPagerScrollTo(int x, int y) {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
        doSmooth(scroller, x, y);
        if (getParent() instanceof View) {
            ((View) getParent()).invalidate(); // 必须执行父视图更新，不然computeScroll方法不会完全走完
        }
        invalidate();
    }

    /**
     * 页面滑动位置发生更改
     *
     * @param reset 是否是重置操作
     * @param oldX  旧X
     * @param oldY  旧Y
     * @param x     新X
     * @param y     新Y
     */
    protected void onPagerScrollChanged(boolean reset, int oldX, int oldY, int x, int y) {
    }

    /**
     * 获取适配器
     *
     * @return 适配器
     */
    public PagerAdapter getAdapter() {
        return adapter;
    }

    /**
     * 设置适配器
     *
     * @param adapter 适配器
     */
    public void setAdapter(PagerAdapter adapter) {
        if (null != adapter) {
            if (null != adapter.getHost()) {
                throw new IllegalStateException("Adapter has been band a host!!!");
            }
            adapter.setHost(this);
        }
        this.adapter = adapter;
        actionFlag |= ACTION_FLAG_INIT;
        requestLayout();
    }

    /**
     * 获取前面缓存数量
     *
     * @return 前面缓存数量
     */
    public int getBeforeCacheCount() {
        return beforeCacheCount;
    }

    /**
     * 获取后面缓存数量
     *
     * @return 后面缓存数量
     */
    public int getAfterCacheCount() {
        return afterCacheCount;
    }

    /**
     * 设置页面限定数量
     */
    public void setPageLimit(int beforeCacheCount, int afterCacheCount) {
        this.beforeCacheCount = beforeCacheCount;
        this.afterCacheCount = afterCacheCount;
        actionFlag |= ACTION_FLAG_INIT;
        requestLayout();
    }

    /**
     * 获取循环配置
     *
     * @return 循环配置
     */
    public int getLoop() {
        return loop;
    }

    /**
     * 设置循环
     *
     * @param loop 循环
     */
    public void setLoop(int loop) {
        this.loop = loop;
        actionFlag |= ACTION_FLAG_INIT;
        requestLayout();
    }

    /**
     * 获取方向
     *
     * @return 方向：{@link #ORIENTATION_HORIZONTAL 水平}
     * {@link #ORIENTATION_VERTICAL 垂直}
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * 设置方向
     *
     * @param orientation 方向：{@link #ORIENTATION_HORIZONTAL 水平}
     *                    {@link #ORIENTATION_VERTICAL 垂直}
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
        actionFlag |= ACTION_FLAG_INIT;
        requestLayout();
    }

    /**
     * 获取平滑速度
     *
     * @return 平滑速度（像素/秒）
     */
    public float getSmoothVelocity() {
        return smoothVelocity;
    }

    /**
     * 设置平滑速度
     *
     * @param smoothVelocity 平滑速度（像素/秒）
     */
    public void setSmoothVelocity(float smoothVelocity) {
        this.smoothVelocity = smoothVelocity;
        actionFlag |= ACTION_FLAG_INIT;
        requestLayout();
    }

    /**
     * 设置是否可以触摸滑动页面
     *
     * @param touchScroll true，可以触摸滑动页面
     */
    public void setTouchScroll(boolean touchScroll) {
        if (touchScroll) {
            actionFlag |= ACTION_FLAG_TOUCH_SCROLL;
        } else {
            actionFlag &= ~ACTION_FLAG_TOUCH_SCROLL;
        }
    }

    /**
     * 判断是否可以触摸滑动页面
     *
     * @return true，可以触摸滑动页面
     */
    public boolean isTouchScroll() {
        return (actionFlag & ACTION_FLAG_TOUCH_SCROLL) != 0;
    }

    /**
     * 获取当前页
     *
     * @return 当前页
     */
    public int getCurrentPage() {
        int index = pagerManager.getCurrentPageIndex();
        if (index >= 0) {
            return pagerManager.getShowPage(index).getAdapterPosition();
        }
        return -1;
    }

    /**
     * 跳转前一页
     *
     * @param smooth 是否开启平滑
     */
    public void jumpBeforePage(final boolean smooth) {
        if (null != adapter && adapter.getPageCount() > 0) {
            int cur = pagerManager.getCurrentPageIndex();
            if (cur > 0) {
                scrollToPage(cur - 1, smooth);
                requestLayout();
            }
        }
    }

    /**
     * 跳转后一页
     *
     * @param smooth 是否开启平滑
     */
    public void jumpAfterPage(final boolean smooth) {
        if (null != adapter && adapter.getPageCount() > 0) {
            int showPages = pagerManager.getShowPages();
            int cur = pagerManager.getCurrentPageIndex();
            if (cur >= 0 && cur < showPages - 1) {
                scrollToPage(cur + 1, smooth);
                requestLayout();
            }
        }
    }

    /**
     * 设置当前页
     *
     * @param currentPage 当前页
     * @param smooth      是否开启平滑
     */
    public void setCurrentPage(final int currentPage, final boolean smooth) {
        if (null != adapter && adapter.getPageCount() > 0 &&
                currentPage >= 0 && currentPage < adapter.getPageCount()) {
            int curIndex = pagerManager.getCurrentPageIndex();
            if (curIndex >= 0 && pagerManager.getJumpType() == 0) {
                int oldPos = pagerManager.getShowPage(curIndex).getAdapterPosition();
                if (oldPos != currentPage) {
                    pagerManager.readyJump(currentPage);
                    if (currentPage < oldPos) {
                        jumpBeforePage(smooth);
                    } else {
                        jumpAfterPage(smooth);
                    }
                }
            }
        }
    }

    /**
     * 布局子视图
     */
    protected void layoutChildren() {
        if (orientation == ORIENTATION_HORIZONTAL) {
            int count = pagerManager.getShowPages();
            for (int i = 0; i < count; i++) {
                View child = pagerManager.getShowPage(i).view;
                horizontalLayoutChild(child, i);
            }
        } else if (orientation == ORIENTATION_VERTICAL) {
            int count = pagerManager.getShowPages();
            for (int i = 0; i < count; i++) {
                View child = pagerManager.getShowPage(i).view;
                verticalLayoutChild(child, i);
            }
        }
    }

    /**
     * 滑动至某页
     *
     * @param index  页下标（缓存位置的下标）
     * @param smooth 是否启动平滑
     */
    protected void scrollToPage(int index, boolean smooth) {
        if (index >= 0 && index < pagerManager.getShowPages()) {
            int dstX, dstY;
            if (orientation == ORIENTATION_HORIZONTAL) {
                dstX = index * computeHorizontalScrollExtent();
                dstY = 0;

            } else if (orientation == ORIENTATION_VERTICAL) {
                dstX = 0;
                dstY = index * computeVerticalScrollExtent();

            } else {
                dstX = dstY = 0;
            }
            if (smooth) {
                smoothPagerScrollTo(dstX, dstY);
            } else {
                pagerScrollTo(dstX, dstY);
            }
        }
    }

    /**
     * 执行平滑操作，可以复写此方法，更改其滑动速度等
     *
     * @param scroller 滑动器
     * @param dstX     目标位置X
     * @param dstY     目标位置Y
     */
    protected void doSmooth(Scroller scroller, int dstX, int dstY) {
        int duration = 250;
        int dx = dstX - getPagerScrollX();
        int dy = dstY - getPagerScrollY();
        if (smoothVelocity > 0) {
            duration = (int) (Math.max(Math.abs(dx), Math.abs(dy)) / smoothVelocity * 1000);
        }
        scroller.startScroll(getPagerScrollX(), getPagerScrollY(), dx, dy, duration);
    }

    private void changePagerScroll() {
//        System.out.println(String.format("changePagerScroll:x=%d,y=%d", getPagerScrollX(), getPagerScrollY()));

        if (null != onPageChangedListener) {
            if (orientation == ORIENTATION_HORIZONTAL) {
                if (computeHorizontalScrollExtent() > 0) {
                    int index = getPagerScrollX() / computeHorizontalScrollExtent();
                    if (index >= 0 && index < pagerManager.getShowPages()) {
                        int offset = getPagerScrollX() % computeHorizontalScrollExtent();
                        int pos = pagerManager.getShowPage(index).getAdapterPosition();
                        onPageChangedListener.onPageScroll(this,
                                pos, offset, computeHorizontalScrollExtent());
                    }
                }
            } else if (orientation == ORIENTATION_VERTICAL) {
                if (computeVerticalScrollExtent() > 0) {
                    int index = getPagerScrollY() / computeVerticalScrollExtent();
                    if (index >= 0 && index < pagerManager.getShowPages()) {
                        int offset = getPagerScrollY() % computeVerticalScrollExtent();
                        int pos = pagerManager.getShowPage(index).getAdapterPosition();
                        onPageChangedListener.onPageScroll(this,
                                pos, offset, computeVerticalScrollExtent());
                    }
                }
            }
        }

        if (pagerManager.getJumpType() != 0) {
            pagerManager.finishJump();
//            System.out.println("selectPage:" + pagerManager.getCurrentPageIndex());
            selectPage();
        } else {
            int cur = pagerManager.getCurrentPageIndex();
            if (cur >= 0) {
                if (orientation == ORIENTATION_HORIZONTAL) {
                    int pageSize = computeHorizontalScrollExtent(); // 每页的大小
                    if (pageSize > 0 && getPagerScrollX() % pageSize == 0) { // 刚好滑到某页
                        int index = getPagerScrollX() / pageSize;
                        if (index != cur) {
                            pagerManager.setCurrentPage(index);
//                            System.out.println("selectPage:index=" + index);
                            selectPage();
                        }
                    }

                } else if (orientation == ORIENTATION_VERTICAL) {
                    int pageSize = computeVerticalScrollExtent(); // 每页的大小
                    if (pageSize > 0 && getPagerScrollY() % pageSize == 0) { // 刚好滑到某页
                        int index = getPagerScrollY() / pageSize;
                        if (index != cur) {
                            pagerManager.setCurrentPage(index);
//                            System.out.println("selectPage:" + index);
                            selectPage();
                        }
                    }
                }
            }
        }
    }

    /**
     * 选中某页
     */
    private void selectPage() {
        if ((actionFlag & ACTION_FLAG_TOUCHING) != 0) { // 触摸滑动中
            actionFlag |= ACTION_FLAG_RESET_TOUCH; // 需要重置滑动
        }
        // 取消滑动
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }

        int count = pagerManager.getShowPages();
        int oldX = pagerScrollX;
        int oldY = pagerScrollY;
        if (count > 0) {
            if (orientation == ORIENTATION_HORIZONTAL) {
                pagerScrollX = pagerManager.getCurrentPageIndex() * computeHorizontalScrollExtent();
                pagerScrollY = 0;
            } else if (orientation == ORIENTATION_VERTICAL) {
                pagerScrollX = 0;
                pagerScrollY = pagerManager.getCurrentPageIndex() * computeVerticalScrollExtent();
            } else {
                pagerScrollX = pagerScrollY = 0;
            }
        } else {
            pagerScrollX = pagerScrollY = 0;
        }
//        System.out.println(String.format("selectPage:x=%d,y=%d", getPagerScrollX(), getPagerScrollY()));
        layoutChildren();
        onPagerScrollChanged(true, oldX, oldY, pagerScrollX, pagerScrollY);
        if (null != onPageChangedListener) {
            int index = pagerManager.getCurrentPageIndex();
            if (index >= 0) {
                int pos = pagerManager.getShowPage(index).getAdapterPosition();
                onPageChangedListener.onPageSelected(this, pos);
            } else {
                onPageChangedListener.onPageSelected(this, -1);
            }
        }
    }

    private void horizontalLayoutChild(View child, int index) {
        if (child.getVisibility() == GONE) {
            child.layout(0, 0, 0, 0);
            return;
        }

        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (null == lp.pageHolder) {
            layoutContainer.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        } else {
            int x = index * computeHorizontalScrollExtent() - pagerScrollX;
            layoutContainer.set(x, 0, x + getMeasuredWidth(), getMeasuredHeight());
        }
        int width = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        int height = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
        int gravity = lp.gravity == Gravity.NO_GRAVITY ? Gravity.START : lp.gravity;
        Gravity.apply(gravity, width, height, layoutContainer, layoutOut);
        child.layout(layoutOut.left, layoutOut.top, layoutOut.right, layoutOut.bottom);
    }

    private void verticalLayoutChild(View child, int index) {
        if (child.getVisibility() == GONE) {
            child.layout(0, 0, 0, 0);
            return;
        }

        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (null == lp.pageHolder) {
            layoutContainer.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        } else {
            int y = index * computeVerticalScrollExtent() - pagerScrollY;
            layoutContainer.set(0, y, getMeasuredWidth(), y + getMeasuredHeight());
        }
        int width = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        int height = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
        int gravity = lp.gravity == Gravity.NO_GRAVITY ? Gravity.START : lp.gravity;
        Gravity.apply(gravity, width, height, layoutContainer, layoutOut);
        child.layout(layoutOut.left, layoutOut.top, layoutOut.right, layoutOut.bottom);
    }

    /**
     * 初始化页管理器
     */
    private void initPagerManager() {
        boolean beforeLoop = false;
        boolean afterLoop = false;
        if (orientation == ORIENTATION_HORIZONTAL) {
            beforeLoop = (loop & LOOP_LEFT) != 0;
            afterLoop = (loop & LOOP_RIGHT) != 0;
        } else if (orientation == ORIENTATION_VERTICAL) {
            beforeLoop = (loop & LOOP_TOP) != 0;
            afterLoop = (loop & LOOP_BOTTOM) != 0;
        }
        pagerManager.init(beforeCacheCount, afterCacheCount, beforeLoop, afterLoop, adapter);
        selectPage();
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        PageHolder pageHolder;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            if (source instanceof LayoutParams) {
                pageHolder = ((LayoutParams) source).pageHolder;
            }
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            if (source instanceof LayoutParams) {
                pageHolder = ((LayoutParams) source).pageHolder;
            }
        }

        public PageHolder getPageHolder() {
            return pageHolder;
        }
    }

    public interface OnPageChangedListener {

        void onPageScroll(LoopPager view, int pagePosition, int pageOffset, int pageSize);

        void onPageSelected(LoopPager view, int pagePosition);
    }
}
