# LoopPager
类似ViewPager，支持循环轮播，采用列表视图缓存机制加载页面。

## 使用
已上传jcenter，可以直接引用。
### Gradle
```
implementation 'com.mosect:LoopPager:1.0.3'
```
### 旧版Gradle
```
compile 'com.mosect:LoopPager:1.0.3'
```
### Maven
```
<dependency>
  <groupId>com.mosect</groupId>
  <artifactId>LoopPager</artifactId>
  <version>1.0.3</version>
  <type>pom</type>
</dependency>
```

## 视图属性

属性名 | JAVA | XML | 取值
----- | ---- | --- | ------
orientation（方向） | setOrientation | app:orientation | vertical horizontal
loop（循环） | setLoop | app:loop | left top right bottom horizontal vertical all none
smoothVelocity（滑动速度） | setSmoothVelocity | app:smoothVelocity | dimen（单位：px/s，dimen值）
beforeCache（前缓存数量） | setPageLimit | app:beforeCache | int（必须大于0）
afterCache（后缓存数量） | setPageLimit | app:afterCache | int （必须大于0）
touchScroll（是否开启触摸平滑） | setTouchScroll | app:touchScroll | boolean

## 视图适配器（Adapter）
此视图采用的是类似RecyclerView的视图复用机制，必须要设置适配器，视图才有内容显示。
```
/**
 * 设置宿主
 *
 * @param host 宿主
 */
public void setHost(AdapterHost host) {...}

/**
 * 获取宿主
 *
 * @return 宿主
 */
public AdapterHost getHost() {...}

/**
 * 通知宿主，数据发生更改
 */
public void notifyDataChanged() {...}

/**
 * 获取页的分类
 *
 * @param position 页位置
 * @return 页分类
 */
public int getPageType(int position) {...}

/**
 * 锁定某页，即加载某页，同一页可能会被加载多次
 *
 * @param parent   父视图
 * @param position 页位置
 * @return 页持有者
 */
public PageHolder lockPage(ViewGroup parent, int position) {...}

/**
 * 解锁某页（移除某页）
 *
 * @param holder 页持有者
 */
public void unlockPage(PageHolder holder) {...}

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
```
**注意：不要去调用setHost方法，内部使用；向外提供是因为，如果你有需求，自定义视图，需要用到缓存机制，可以使用此适配器。**

## 页持有者（PageHolder）
连接页和视图，使得两者关联起来。PageHolder必须需要一个View，在PagerAdapter中创建。
```
/**
 * 获取页分类
 *
 * @return 页的分类
 */
public int getType() {...}

/**
 * 获取页的适配器位置
 *
 * @return 页的适配器位置
 */
public int getAdapterPosition() {...}
```

## 页滑动监听器（LoopPager.OnPageChangedListener）
可以向视图设置一个页滑动监听器，监听页变化。
```
public interface OnPageChangedListener {

    void onPageScroll(LoopPager view, int pagePosition, int pageOffset, int pageSize);

    void onPageSelected(LoopPager view, int pagePosition);
}
```
***可以使用getOnPageChangedListener方法，拓展成多个监听器。***

# 更新记录
## 1.0.0 
### 问题
* 如果页的视图也需要滑动事件，则会出现页面切换错乱问题（已在1.0.3中修复）

## 1.0.1     1.0.2
### 更改
* 优化了滑动，使用了ViewUtils库改写滑动代码。

## 1.0.3
### 问题
* 高版本Android，首次不加载页面内容（已在1.0.4中修复）
### 修复
* 修复页面需要滑动事件时，出现页面切换错乱问题。
* 再度优化滑动

## 1.0.4
### 修复
* 修复高版本Android，首次不加载页面内容的BUG

# 其他：
```
个人网站：http://www.mosect.com:5207 建设中……
邮箱：zhouliuyang1995@163.com
QQ：905340954
```
