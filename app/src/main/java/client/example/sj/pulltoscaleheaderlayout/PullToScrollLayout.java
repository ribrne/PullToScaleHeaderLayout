package client.example.sj.pulltoscaleheaderlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * 基于开源项目PullToRefreshBase，抠出了其中一小部分功能实现这个下拉放大头部的listView，这个方案有一些小瑕疵就是，在listView正常滚动到底部或顶部之后不能继续进行手势的上拉或下拉
 * Created by sj on 15/11/27.
 */
public class PullToScrollLayout extends LinearLayout {

    //正常滚动后允许listView的滚动误差
    static final int OFFSET = 20;

    static final int SMOOTH_SCROLL_DURATION_MS = 200;

    static final float FRICTION = 2.0f;

    static final int PULL_TOP = 1;

    static final int PULL_BOTTOM = 2;

    static final int RESET = -1;

    static final int RELEASE_TO_SCROLL_DOWN = 1;

    static final int RELEASE_TO_SCROLL_UP = 2;

    static final int SCROLLING = 4;

    private ListView content;

    //内置头部
    private FrameLayout frameLayout;

    //监听滚动的距离
    private OnViewScrollChangedListener onViewScrollChangedListener;

    private Interpolator interpolator;

    private SmoothScrollRunnable smoothScrollRunnable;

    private int minHeight;

    private int maxHeight;

    private int mCurrentMode;

    private int state = RESET;

    private int mTouchSlop;

    private float mLastX;

    private float mDownY;

    private float mLastY;

    private boolean mIsBeingDragged;

    private int mScrollState;

    private boolean isScrollUp;

    public PullToScrollLayout(Context context) {
        super(context);
        init(context,null);
    }

    public PullToScrollLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        setOrientation(VERTICAL);
        content = createListView(context,attributeSet);
        addView(content);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        createHeader(context,attributeSet);
        content.addHeaderView(frameLayout);
//        content.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//                mScrollState = scrollState;
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                View child = content.getChildAt(0);
//                if (!isReadyForPull()) {
//                    if (onViewScrollChangedListener != null) {
//                        if (mScrollState == SCROLL_STATE_FLING) {
//                            onViewScrollChangedListener.scrollChanged(child.getTop(),false);
//                        } else {
//                            if (child.getTop() + maxHeight > -OFFSET && !isScrollUp) {
//                                onViewScrollChangedListener.scrollChanged(0,false);
//                                return;
//                            }
//                            if (child.getTop() <= minHeight - maxHeight + OFFSET && isScrollUp) {
//                                onViewScrollChangedListener.scrollChanged(minHeight - maxHeight,false);
//                                return;
//                            }
//                            onViewScrollChangedListener.scrollChanged(child.getTop(),false);
//                        }
//                    }
//                }
//            }
//        });
        interpolator = new DecelerateInterpolator();
    }

    private ListView createListView(Context context, AttributeSet attributeSet) {
        final ListView lv;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            lv = new InternalListViewSDK9(context, attributeSet);
        } else {
            lv = new InternalListView(context, attributeSet);
        }
        // Set it to this so it can be used in ListActivity/ListFragment
        lv.setId(android.R.id.list);
        return lv;
    }

    private void createHeader(Context context, AttributeSet attributeSet) {
        frameLayout = new FrameLayout(context,attributeSet);
    }

    //头部不放大的正常高度
    public void setMaxHeightOfHeader(int maxHeight) {
        this.maxHeight = maxHeight;
        AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,maxHeight);
        frameLayout.setLayoutParams(layoutParams);
    }

    //头部平移后最小的高度，目前这个功能还没有实现，因为listView在快速滚动之后，无法精确得到listView滚动距离，目前没有处理好这个误差，之后会继续优化
    public void setMinHeightOfHeader(int minHeight) {
        this.minHeight = minHeight;
    }

    public void setOnViewScrollChangedListener(OnViewScrollChangedListener onViewScrollChangedListener) {
        this.onViewScrollChangedListener = onViewScrollChangedListener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        content.setOnItemClickListener(onItemClickListener);
    }

    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        content.setOnScrollListener(onScrollListener);
    }

    public void addHeader(View view) {
        content.addHeaderView(view);
    }

    public void addFooter(View view) {
        content.addFooterView(view);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            mIsBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (state == SCROLLING) {
                    return true;
                }

                if (isReadyForPull()) {

                    final float y = event.getY() , x = event.getX();
                    final float diff, oppositeDiff , absDiff;

                    diff = y - mLastY;
                    absDiff = Math.abs(diff);
                    oppositeDiff = x - mLastX;
                    if (absDiff > mTouchSlop && absDiff > Math
                            .abs(oppositeDiff)) {
                        if(isFirstViewVisible() && diff >= 1f) {
                            mLastY = y;
                            mLastX = x;
                            mIsBeingDragged = true;
                            mCurrentMode = PULL_TOP;
                            isScrollUp = true;
                        } else if (diff <= -1f && isLastViewVisible()) {
                            mLastY = y;
                            mLastX = x;
                            mIsBeingDragged = true;
                            mCurrentMode = PULL_BOTTOM;
                            isScrollUp = false;
                        }
                    }
                }

                final float y = event.getY() , x = event.getX();
                final float diff, oppositeDiff , absDiff;
                diff = y - mLastY;
                absDiff = Math.abs(diff);
                oppositeDiff = x - mLastX;
                if (absDiff > mTouchSlop && absDiff > Math
                        .abs(oppositeDiff)) {
                    if(isFirstViewVisible() && diff >= 1f) {
                        isScrollUp = true;
                    } else if (diff <= -1f && isLastViewVisible()) {
                        isScrollUp = false;
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if (isReadyForPull()) {
                    mLastX = event.getX();
                    mLastY = mDownY = event.getY();
                    mIsBeingDragged = false;
                }
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        // If we're refreshing, and the flag is set. Eat the event
        if (isScrolling()) {
            return true;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN
                && ev.getEdgeFlags() != 0) {
            return false;
        }

        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mIsBeingDragged) {
                    mLastY = ev.getY();
                    mLastX = ev.getX();
                    pullEvent();
                    return true;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if (isReadyForPull()) {
                    mLastX = ev.getX();
                    mDownY = mLastY = ev.getY();
                    mIsBeingDragged = false;
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;
                    executeScroll(0);
                    return true;
                }
                break;
        }
        return mIsBeingDragged;
    }

    private boolean isScrolling() {
        return state == SCROLLING;
    }

    private void pullEvent() {
        /**
         * init Value
         */
        final int newScrollValue;
        final float initialMotionValue, lastMotionValue;
        initialMotionValue = mDownY;
        lastMotionValue = mLastY;
        /**
         * scale Header of ListView
         */
        switch (mCurrentMode) {
            case PULL_BOTTOM:
                newScrollValue = Math.round(Math.max(initialMotionValue
                        - lastMotionValue, 0)
                        / FRICTION);
                break;
            case PULL_TOP:
            default:
                newScrollValue = Math.round(Math.min(initialMotionValue
                        - lastMotionValue, 0)
                        / FRICTION);
                break;
        }
        int scrollDistance = calculateScrollDistance(newScrollValue);
        setHeaderScroll(scrollDistance);
        if (scrollDistance < 0) {
            state = RELEASE_TO_SCROLL_DOWN;
        } else if (scrollDistance >= minHeight) {
            state = RELEASE_TO_SCROLL_UP;
        }
    }

    private int getMaximumPullScroll() {
        return Math.round(getHeight() / FRICTION);
    }

    private int calculateScrollDistance(int value) {
        final int maximumPullScroll = getMaximumPullScroll();
        value = Math
                .min(maximumPullScroll, Math.max(-maximumPullScroll, value));
        return value;
    }

    /**
     * Helper method which just calls scrollTo() in the correct scrolling
     * direction.
     *
     * @param value
     *            - New Scroll value
     */
    protected final void setHeaderScroll(float value) {
        if (mCurrentMode != PULL_BOTTOM) {
            if (onViewScrollChangedListener != null) {
                onViewScrollChangedListener.scrollChanged(-value,true);
            }
        }
        scrollTo(0,(int)value);
    }

    //双向拉动，listView滚动到底部之后，还可以继续上拉layout，滚动当顶部可以继续下拉layout
    private boolean isReadyForPull() {
        return isFirstViewVisible() || isLastViewVisible();
    }

    private boolean isFirstViewVisible() {
        final Adapter adapter = content.getAdapter();

        if (null == adapter || adapter.isEmpty()) {
            return true;

        } else {
            /**
             * This check should really just be:
             * mRefreshableView.getFirstVisiblePosition() == 0, but PtRListView
             * internally use a HeaderView which messes the positions up. For
             * now we'll just add one to account for it and rely on the inner
             * condition which checks getTop().
             */
            if (content.getFirstVisiblePosition() <= 1) {
                final View firstVisibleChild = content.getChildAt(0);
                if (firstVisibleChild != null) {
                    return firstVisibleChild.getTop() >= content.getTop();
                }
            }
        }

        return false;
    }

    public boolean isLastViewVisible() {
        final Adapter adapter = content.getAdapter();

        if (null == adapter || adapter.isEmpty()) {
            return true;
        } else {
            final int lastItemPosition = content.getCount() - 1;
            final int lastVisiblePosition = content
                    .getLastVisiblePosition();

            /**
             * This check should really just be: lastVisiblePosition ==
             * lastItemPosition, but PtRListView internally uses a FooterView
             * which messes the positions up. For me we'll just subtract one to
             * account for it and rely on the inner condition which checks
             * getBottom().
             */
            if (lastVisiblePosition >= lastItemPosition - 1) {
                final int childIndex = lastVisiblePosition
                        - content.getFirstVisiblePosition();
                final View lastVisibleChild = content
                        .getChildAt(childIndex);
                if (lastVisibleChild != null) {
                    return lastVisibleChild.getBottom() <= content
                            .getBottom();
                }
            }
        }

        return false;
    }

    private void executeScroll(int scrollValue) {
        if (null != smoothScrollRunnable) {
            smoothScrollRunnable.stop();
        }

        final int oldScrollValue;
        oldScrollValue = getScrollY();

        if (oldScrollValue != scrollValue) {
            smoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, scrollValue, SMOOTH_SCROLL_DURATION_MS);
            post(smoothScrollRunnable);
        }
    }

    public void setAdapter(BaseAdapter adapter) {
        content.setAdapter(adapter);
    }

    final class SmoothScrollRunnable implements Runnable {

        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        public SmoothScrollRunnable(int fromY, int toY, long duration) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mInterpolator = interpolator;
            mDuration = duration;
        }

        @Override
        public void run() {

            state = SCROLLING;
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {
                /**
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime))
                        / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY)
                        * mInterpolator
                        .getInterpolation(normalizedTime / 1000f));
                mCurrentY = mScrollFromY - deltaY;
                setHeaderScroll(mCurrentY);
            }

            if (mContinueRunning && mScrollToY != mCurrentY) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    PullToScrollLayout.this.postOnAnimation(this);
                } else {
                    PullToScrollLayout.this.postDelayed(this, 16);
                }
            } else {
                state = RESET;
                setHeaderScroll(0);
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    @TargetApi(9)
    final static class InternalListViewSDK9 extends InternalListView {

        public InternalListViewSDK9(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
                                       int scrollY, int scrollRangeX, int scrollRangeY,
                                       int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

            final boolean returnValue = super.overScrollBy(deltaX, deltaY,
                    scrollX, scrollY, scrollRangeX, scrollRangeY,
                    maxOverScrollX, maxOverScrollY, isTouchEvent);
            return returnValue;
        }
    }

    static class InternalListView extends ListView {

        public InternalListView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOverScrollMode(OVER_SCROLL_NEVER);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            /**
             * This is a bit hacky, but Samsung's ListView has got a bug in it
             * when using Header/Footer Views and the list is empty. This masks
             * the issue so that it doesn't cause an FC. See Issue #66.
             */
            try {
                super.dispatchDraw(canvas);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            /**
             * This is a bit hacky, but Samsung's ListView has got a bug in it
             * when using Header/Footer Views and the list is empty. This masks
             * the issue so that it doesn't cause an FC. See Issue #66.
             */
            try {
                return super.dispatchTouchEvent(ev);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            super.setAdapter(adapter);
        }

    }

    public interface OnViewScrollChangedListener {
        void scrollChanged(float scrollDistance, boolean isDragged);
    }

}
