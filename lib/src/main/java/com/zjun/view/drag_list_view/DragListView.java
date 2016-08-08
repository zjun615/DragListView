package com.zjun.view.drag_list_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

import com.zjun.view.CommonTool;


/**
 * 可拖拽排序的ListView
 * 特点：
 * 1、可拖拽排序（条目右边1/4空间内）
 * 2、可添加头部控件headerView
 * 3、左滑可删除
 * 4、可设置点击和长按事件
 *
 * Created by Ralap on 2016/5/8.
 */

public class DragListView extends ListView {
    @SuppressWarnings("unused")
    private static final String LOG_TAG = DragListView.class.getSimpleName();

    /**
     * 拖拽快照的透明度(0.0f ~ 1.0f)。进入删除时的状态也取此值
     */
    private static final float DRAG_PHOTO_VIEW_ALPHA = .8f;

    /**
     * 上下滚动时的时间
     */
    @SuppressWarnings("unused")
    private static final int SMOOTH_SCROLL_DURATION = 100;

    /**
     * 进入删除状态的X轴方向最小值
     */
    private static final int DELETE_X_MIN = 10;

    /**
     * 在删除状态时，Y轴方向超过溢出值，则不再是删除状态
     */
    private static final int DELETE_Y_SLOP = 10;

    /**
     * 在删除状态时，向左滑，X轴方向的达到删除要求的门槛速度(px/100ms)。根据屏幕密度值来确定
     */
    private float DELETE_X_THRESHOLD;

    /**
     * 上下滚动时的最大距离，可进行设置
     * @see #setMaxDistance(int)
     * @see #getMaxDistance()
     */
    private int mMaxDistance = 30;


    public DragListView(Context context) {
        super(context);
        initialize();
    }

    public DragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public DragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize(){
        DELETE_X_THRESHOLD = CommonTool.dp2px(getContext(), -60);

        // 为了防止滑动时和滑动后背景变灰（系统默认颜色）,且XML中没有设置listSelector和cacheColorHint，这里确保两属性都是透明
        setSelector(android.R.color.transparent); // 或0
        setCacheColorHint(0);
        
        mScroller = new Scroller(getContext());
    }

    /**
     * 是否处于拖拽中
     */
    private boolean mIsDraging = false;

    /**
     * 按下时的坐标位置
     */
    private int mDownX;
    private int mDownY;

    /**
     * 移动时的坐标
     */
    private int mMoveX;
    private int mMoveY;

    /**
     * 原生偏移量。也就是ListView的左上角相对于屏幕的位置
     */
    private int mRawOffsetX;
    private int mRawOffsetY;

    /**
     * 在条目中的位置
     */
    private int mItemOffsetX;
    private int mItemOffsetY;

    /**
     * 拖拽快照的垂直位置范围。根据条目数量和ListView的高度来确定
     */
    private int mMinDragY;
    private int mMaxDragY;

    /**
     * 拖拽条目的高度
     */
    private int mDragItemHeight;

    /**
     * 被拖拽的条目位置
     */
    private int mDragPosition;

    /**
     * 移动前的条目位置
     */
    private int mFromPosition;

    /**
     * 移动后的条目位置
     */
    private int mToPosition;

    /**
     * 窗口管理器，用于显示条目的快照
     */
    private WindowManager mWindowManager;

    /**
     * 窗口管理的布局参数
     */
    private WindowManager.LayoutParams mWindowLayoutParams;

    /**
     * 拖拽条目的快照图片
     */
    private Bitmap mDragPhotoBitmap;

    /**
     * 正在拖拽的条目快照view
     */
    private ImageView mDragPhotoView;


    /**
     * 是否处于删除中
     */
    private boolean mIsDeleting;

    /**
     * 是否处于滚动状态
     */
    private boolean mIsScrolling;

    /**
     * 长按后，执行了长按事件的标记
     */
    private boolean mLongClickFlag;

    /**
     * 条目长按监听器
     */
    private OnItemLongClickListener mDeletingItemLongClickListener;

    /**
     * 正在删除的条目位置
     */
    private int mDeletingPosition;
    /**
     * 正在删除的条目
     */
    private View mDeletingItem;
    /**
     * 正在删除条目的透明度
     */
    private float mDeletingItemAlpha;

    /**
     * 速度追踪器
     */
    private VelocityTracker mVelocityTracker;

    /**
     * 滑动器
     */
    private Scroller mScroller;

    /**
     * 适配器
     */
    private DragListViewAdapter mAdapter;

    /**
     * 头部总高度
     */
    @SuppressWarnings("unused")
    private int mHeaderHeightSum = 0;
    /**
     * 脚部总高度
     */
    @SuppressWarnings("unused")
    private int mFooterHeightSum = 0;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 获取第一个手指点的Action
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 初始化操作
                setOnItemLongClickListener(mDeletingItemLongClickListener);
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(ev);

                mDownX = (int) ev.getX();
                mDownY = (int) ev.getY();
                // 获取当前触摸位置对应的条目索引
                mDragPosition = pointToPosition(mDownX, mDownY);
                // 如果触摸的坐标不在条目上，在分割线、或外部区域，则为无效值-1; 宽度3/4 以右的区域可拖拽; Header和Footer无效
                if (!isPositionValid(mDragPosition) || mDownX < getWidth() * 3 / 4) {
                    return super.onTouchEvent(ev);
                }
                mIsDraging = true;
                mToPosition = mFromPosition = mDragPosition;

                mRawOffsetX = (int) (ev.getRawX() - mDownX);
                mRawOffsetY = (int) (ev.getRawY() - mDownY);

                // 开始拖拽的前期工作：展示item快照
                startDrag();
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(ev);

                mMoveX = (int) ev.getX();
                mMoveY = (int) ev.getY();
                if (mIsDraging) {
                    // 更新快照位置
                    updateDragView();
                    // 更新当前被替换的位置
                    updateItemView();
                } else if (mIsDeleting) {
                    // 移动将要删除的条目
                    moveDeleting();

                } else {
                    if (!mIsScrolling && !mLongClickFlag && mDownX - mMoveX >= DELETE_X_MIN && Math.abs(mMoveY - mDownY) <= DELETE_Y_SLOP) {
                        mDeletingPosition = pointToPosition(mDownX, mDownY);
                        super.setOnItemLongClickListener(null);
                        if (isPositionValid(mDeletingPosition)) {
                            startDeleting();
                        } else {
                            return super.onTouchEvent(ev);
                        }
                    } else {
                        if (Math.abs(mMoveY - mDownY) >= DELETE_Y_SLOP) {
                            mIsScrolling = true;
                        }
                        return super.onTouchEvent(ev);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                rstLongClickFlag();

                if (mIsDraging) {
                    // 停止拖拽
                    stopDrag();
                } else if (mIsDeleting) {
                    // 停止删除中
                    stopDeleting();
                } else {
                    mIsScrolling = false;
                    recycleVelocityTracker();
                    return super.onTouchEvent(ev);
                }
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 判断位置是否有效
     * @param position 需判断的位置
     * @return 是否有效
     */
    private boolean isPositionValid(int position) {
        return !(position == AdapterView.INVALID_POSITION
                || position < getHeaderViewsCount()
                || position >= getHeaderViewsCount() + mAdapter.getCount());
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null){
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void startDeleting() {
        mIsDeleting = true;
        mDeletingItem = getItemView(mDeletingPosition);
        if (mDeletingItem != null) {
            mDeletingItemAlpha = mDeletingItem.getAlpha();
            mDeletingItem.setAlpha(DRAG_PHOTO_VIEW_ALPHA);
        }
    }

    private void moveDeleting() {
        if (mDeletingItem != null) {
            // 向左运动，并同步透明度
            int scrollX = Math.max(mDownX - mMoveX, 0);
            // 超过宽度，变成全透明
            int transparentBorder = mDeletingItem.getWidth();
            float percent = CommonTool.estimatePercent(0, transparentBorder, Math.min(scrollX, transparentBorder));
            float alpha = CommonTool.estimateFloat(DRAG_PHOTO_VIEW_ALPHA, 0, percent);
            // 参数内正数表示向负轴方向运动
            mDeletingItem.scrollTo(scrollX, 0);
            mDeletingItem.setAlpha(alpha);
        }
    }

    private void stopDeleting() {
        if (mDeletingItem != null) {
            int scrollX = mDeletingItem.getScrollX();
            int width = mDeletingItem.getWidth();
            // 代表监测每100毫秒移动的距离（像素）
            mVelocityTracker.computeCurrentVelocity(100);

//            Log.d(LOG_TAG, "dddd, getXVelocity()=" + mVelocityTracker.getXVelocity());
            // 超过1/3的宽度，或超过固定速度，则删除
            if (scrollX > width / 3 || mVelocityTracker.getXVelocity() < DELETE_X_THRESHOLD) {
                int delta = width - scrollX;
                float percent = CommonTool.estimatePercent(width, 0, delta);
                int duration = CommonTool.estimateInt(250, 0, percent); // 250是系统默认时间
                mScroller.startScroll(scrollX, 0, delta, 0, duration);
                invalidate(); // 强制刷新界面，确保computeScroll()执行
            }else{
                resetDeleteItem();
            }
        }
        mIsDeleting = false;
    }

    private void resetDeleteItem(){
        // 还原位置及透明度
        if (mDeletingItem != null){
            mDeletingItem.scrollTo(0, 0);
            mDeletingItem.setAlpha(mDeletingItemAlpha);
            mDeletingItem = null;
        }
    }
    @Override
    public void computeScroll() {
        // 判断是否还有偏移量，true-还有偏移量，未完成
        if (mScroller.computeScrollOffset()){
            mDeletingItem.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate(); // 刷新界面
            // 判断是否完成
            if (mScroller.isFinished()){
                // 防止显示完全时，最后一个被删除会产生闪烁
                if (isShowAll() && mDeletingPosition == getAdapter().getCount() - 1){
                    mDeletingItem = null;
                }else{
                    resetDeleteItem();
                }
                mAdapter.deleteData(mDeletingPosition - getHeaderViewsCount());
            }
        }
        super.computeScroll();
    }

    private boolean startDrag() {
        // 实际在ListView中的位置，因为涉及到条目的复用
        final View itemView = getItemView(mDragPosition);
        if (itemView == null) {
            return false;
        }
        // 进行绘图缓存
        itemView.setDrawingCacheEnabled(true);
        // 提取缓存中的图片
        mDragPhotoBitmap = Bitmap.createBitmap(itemView.getDrawingCache());
        // 清除绘图缓存，否则复用的时候，会出现前一次的图片。或使用销毁destroyDrawingCache()
        itemView.setDrawingCacheEnabled(false);

        // 隐藏。为了防止隐藏时出现画面闪烁，使用动画去除闪烁效果
        Animation aAnim = new AlphaAnimation(1f, DRAG_PHOTO_VIEW_ALPHA);
        aAnim.setDuration(50);
        aAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mIsDraging && mToPosition == mDragPosition) {
                    itemView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        itemView.startAnimation(aAnim);

        mItemOffsetX = mDownX - itemView.getLeft();
        mItemOffsetY = mDownY - itemView.getTop();
        mDragItemHeight = itemView.getHeight();
//        mMinDragY = mRawOffsetY;
        mMinDragY = mRawOffsetY ;
        // 根据是否显示完全，设定快照在Y轴上可拖到的最大值
        if (isShowAll()) {
            mMaxDragY = mRawOffsetY + getChildAt(getAdapter().getCount() - 1).getTop();
        } else {
            mMaxDragY = mRawOffsetY + getHeight() - mDragItemHeight;
        }
        createDragPhotoView();
        return true;
    }

    /**
     * 判断ListView是否全部显示，即无法上下滚动了
     */
    private boolean isShowAll() {
        if (getChildCount() == 0) {
            return true;
        }
        View firstChild = getChildAt(0);
        int itemAllHeight = firstChild.getBottom() - firstChild.getTop() + getDividerHeight();
        return itemAllHeight * getAdapter().getCount() < getHeight();
    }

    /**
     * 创建拖拽快照
     */
    private void createDragPhotoView() {
        // 获取当前窗口管理器
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        // 创建布局参数
        mWindowLayoutParams = new WindowManager.LayoutParams();
        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mWindowLayoutParams.format = PixelFormat.TRANSLUCENT; // 期望的图片为半透明效果，然而并没有看到不一样的效果
        // 下面这些参数能够帮助准确定位到选中项点击位置
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowLayoutParams.windowAnimations = 0; // 无动画
        mWindowLayoutParams.alpha = DRAG_PHOTO_VIEW_ALPHA; // 微透明

        mWindowLayoutParams.x = mDownX + mRawOffsetX - mItemOffsetX;
        mWindowLayoutParams.y = adjustDragY(mDownY + mRawOffsetY - mItemOffsetY);

        mDragPhotoView = new ImageView(getContext());
        mDragPhotoView.setImageBitmap(mDragPhotoBitmap);
        mWindowManager.addView(mDragPhotoView, mWindowLayoutParams);
    }

    /**
     * 校正Drag的值，不让其越界
     *
     * @param y y坐标
     * @return 校正后的y坐标
     */
    private int adjustDragY(int y) {
        if (y < mMinDragY) {
            return mMinDragY;
        } else if (y > mMaxDragY) {
            return mMaxDragY;
        }
        return y;
    }

    /**
     * 根据Adapter中的位置获取对应ListView的条目
     */
    private View getItemView(int position) {
        if (position < 0 || position >= getAdapter().getCount()) {
            return null;
        }
        int index = position - getFirstVisiblePosition();
        return getChildAt(index);
    }

    private void updateDragView() {
        if (mDragPhotoView != null) {
            mWindowLayoutParams.y = adjustDragY(mMoveY + mRawOffsetY - mItemOffsetY);
            mWindowManager.updateViewLayout(mDragPhotoView, mWindowLayoutParams);
        }
    }

    private void updateItemView() {
        int position = pointToPosition(mMoveX, mMoveY);
        if (isPositionValid(position)) {
            mToPosition = position;
        }

        // 调换位置，并把显示进行调换
        if (mFromPosition != mToPosition) {
            if (exchangePosition()) {
                View view = getItemView(mFromPosition);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                view = getItemView(mToPosition);
                if (view != null) {
                    view.setVisibility(View.INVISIBLE);
                }
                mFromPosition = mToPosition;
            }
        }

        // 如果当前位置已经不到一个条目，则进行上或下的滚动。并根据距离边界的距离，设定滚动速度
        int dragY = mMoveY - mItemOffsetY;
        if (dragY < mDragItemHeight) {
            int value = Math.max(0, dragY); // 防越界
            float percent = CommonTool.estimatePercent(mDragItemHeight, 0, value);
            // 第1种方法，smoothScrollBy()
//            int distance = CommonTool.estimateInt(0, -mMaxDistance, percent);
//            smoothScrollBy(distance, SMOOTH_SCROLL_DURATION);

            // 第2种方法：第1种有时在复杂的情况，会出现卡顿，慢速的问题
            View toView = getItemView(mToPosition);
            if (null == toView) {
                return;
            }
            int y = toView.getTop() + CommonTool.estimateInt(0, 10, percent);
            setSelectionFromTop(mToPosition, y);
        } else if (dragY > getHeight() - 2 * mDragItemHeight) {
            int value = Math.max(0, getHeight() - dragY - mDragItemHeight); // 防越界
            float percent = CommonTool.estimatePercent(mDragItemHeight, 0, value);
//            int distance = CommonTool.estimateInt(0, mMaxDistance, percent);
//            smoothScrollBy(distance, SMOOTH_SCROLL_DURATION);

            View toView = getItemView(mToPosition);
            if (null == toView) {
                return;
            }
            int y = toView.getTop() + CommonTool.estimateInt(0, -10, percent);
            setSelectionFromTop(mToPosition, y);

//            // 或使用setSelectionFromTop(position, y)迅速滑动
//            View itemView = getChildAt(mToPosition - getFirstVisiblePosition());
//            int y = itemView.getTop() - 10;
//            // 设置选定的条目，距离ListView顶部的距离为y
//            setSelectionFromTop(mToPosition, y);
        }
    }

    /**
     * 停止拖拽
     */
    private void stopDrag() {
        // 显示坐标上的条目
        View view = getItemView(mToPosition);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        // 移除快照
        if (mDragPhotoView != null) {
            mWindowManager.removeView(mDragPhotoView);
            mDragPhotoView.setImageDrawable(null);
            mDragPhotoBitmap.recycle();
            mDragPhotoBitmap = null;
            mDragPhotoView = null;
        }
        mIsDraging = false;
    }

    /**
     * 调换位置
     */
    private boolean exchangePosition() {
        int itemCount = getAdapter().getCount();
        if (mFromPosition >= 0 && mFromPosition < itemCount
                && mToPosition >= 0 && mToPosition < itemCount) {
//            mAdapter.swapData(mFromPosition, mToPosition);
            mAdapter.swapData(mFromPosition - getHeaderViewsCount(), mToPosition - getHeaderViewsCount());
            return true;
        }
        return false;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        super.setOnItemLongClickListener(listener);
        if (listener != null) {
            this.mDeletingItemLongClickListener = listener;
        }
    }

    /**
     * 置位长按事件执行标记
     */
    public void setLongClickFlag() {
        this.mLongClickFlag = true;
    }

    /**
     * 复位长按事件执行标记
     */
    public void rstLongClickFlag() {
        this.mLongClickFlag = false;
    }



    /**
     * 为了获取头部高度
     */
    @Override
    public void addHeaderView(View v) {
        mHeaderHeightSum += v.getMeasuredHeight();
        super.addHeaderView(v);
    }

    /**
     * 为了获取底部高度
     */
    @Override
    public void addFooterView(View v) {
        mFooterHeightSum += v.getMeasuredHeight();
        super.addFooterView(v);
    }

    /**
     * 设置适配器
     * 强制使用自身的Adapter，不然在设置了Header或Footer时，getAdapter()获取的不是设置进去的，而是HeaderViewListAdapter
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof DragListViewAdapter)) {
            throw new RuntimeException("Please use its own adapter: DragListViewAdapter");
        }
        mAdapter = (DragListViewAdapter) adapter;
        super.setAdapter(adapter);
    }

    /**
     * Setter and Getter
     */
    @SuppressWarnings("unused")
    public void setMaxDistance(int maxDistance) {
        if (maxDistance > 0) {
            mMaxDistance = maxDistance;
        }
    }

    @SuppressWarnings("unused")
    public int getMaxDistance() {
        return mMaxDistance;
    }
}
