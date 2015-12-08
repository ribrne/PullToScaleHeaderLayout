package client.example.sj.pulltoscaleheaderlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

/**
 * Created by sj on 15/12/2.
 */
public class PullToScaleHeaderLayout extends LinearLayout {

    static final int SCROLL_DURATION = 500;

    static final float OFFSET = 4f;

    static final float FRICTION = 2.0f;

    static final int SCROLL_DOWN = 1;

    static final int SCROLL_UP = 2;

    private ListView listView;

    private View header;

    private View footer;

    private AbsListView.LayoutParams headerLayoutParams;

    private AbsListView.LayoutParams footerLayoutParams;

    private int heightOfActionBar;

    private int heightOfHeader;

    private int heightOfFooter;

    private float mDownY;

    private float mLastY;

    private int mLastDistance;

    private int currentScrollDistance;

    private int currentMode = -1;

    private int mRecordDistance;

    private boolean isTouchEventConsumed;

    private boolean isResetCoordinateNeeded;

    private boolean isAllowToScrollBack;

    private Interpolator interpolator;

    private OnHeaderScrollChangedListener onHeaderScrollChangedListener;

    private Scroller scroller;

    public PullToScaleHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public PullToScaleHeaderLayout(Context context) {
        super(context);
        init(context,null);
    }

    private void init (Context context,AttributeSet attributeSet) {
        listView = createListView(context,attributeSet);
        header = new View(context,attributeSet);
        footer = new View(context,attributeSet);
        interpolator = new DecelerateInterpolator();
        scroller = new Scroller(context,interpolator);
        scroller.forceFinished(false);
        headerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,heightOfHeader);
        footerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,heightOfFooter);
        listView.addHeaderView(header);
        listView.addFooterView(footer);
        addView(listView);
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

    public void setAdapter(BaseAdapter baseAdapter) {
        listView.setAdapter(baseAdapter);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        listView.setOnItemClickListener(onItemClickListener);
    }

    public void addHeaderView(View view) {
        listView.addHeaderView(view);
    }

    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        listView.setOnScrollListener(onScrollListener);
    }

    public void setHeightOfActionBar(int height) {
        heightOfActionBar = height;
    }

    public void setHeightOfHeader(int heightOfHeader) {
        this.heightOfHeader = heightOfHeader;
        resizeHeader(this.heightOfHeader);
    }

    public void setHeightOfFooter(int heightOfFooter) {
        this.heightOfFooter = heightOfFooter;
        resizeHeightOfFooter(this.heightOfFooter);
    }

    public void resizeHeader(int heightOfHeader) {
        if (heightOfHeader < this.heightOfHeader) {
            heightOfHeader = this.heightOfHeader;
        }
        headerLayoutParams.height = heightOfHeader;
        header.setLayoutParams(headerLayoutParams);
    }

    public void resizeHeightOfFooter(int heightOfFooter) {
        if (heightOfFooter < this.heightOfFooter) {
            heightOfFooter = this.heightOfFooter;
        }
        footerLayoutParams.height = heightOfFooter;
        footer.setLayoutParams(footerLayoutParams);
    }

    public void dragging(int scrollDistance) {
        if (scrollDistance < this.heightOfActionBar) {
            scrollDistance = this.heightOfActionBar;
        }
        if (scrollDistance == currentScrollDistance) {
            return;
        }
        currentScrollDistance = scrollDistance;
        notifyHeaderScrollChanged(currentScrollDistance);
        scrollTo(0,-currentScrollDistance);
    }

    public void setOnHeaderScrollChangedListener(OnHeaderScrollChangedListener onHeaderScrollChangedListener) {
        this.onHeaderScrollChangedListener = onHeaderScrollChangedListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            isTouchEventConsumed = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && isTouchEventConsumed) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                mLastY = ev.getY();
                detectTouchEventConsumed();
                break;
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = ev.getY();
                isTouchEventConsumed = false;
                isAllowToScrollBack = false;
                scroller.forceFinished(false);
                recordScrollDistance();
                break;
        }
        return isTouchEventConsumed;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = ev.getY() ;
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                recordScrollDistance();
                isResetCoordinateNeeded = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                isResetCoordinateNeeded = true;
                recordScrollDistance();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isTouchEventConsumed) {
                    mLastY = ev.getY();
                    final float diff;
                    resetCoordinateIfNeeded();
                    resetCoordinateWhenHeaderReachesTheEnd();
                    diff = mLastY - mDownY;
                    if (diff <= -OFFSET) {
                        currentMode = SCROLL_UP;
                        mLastDistance = (int)diff;
                        scrolling();
                    } else if (diff >= OFFSET) {
                        clearFocus();
                        currentMode = SCROLL_DOWN;
                        mLastDistance = (int) diff;
                        scrolling();
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isTouchEventConsumed) {
                    isTouchEventConsumed = false;
                    isAllowToScrollBack = true;
                    if (currentScrollDistance > heightOfHeader) {
                        scrollBackToTop();
                    }
                    return true;
                }
                break;
        }
        return isTouchEventConsumed;
    }

    private void resetCoordinateIfNeeded() {
        if (isResetCoordinateNeeded) {
            mDownY = mLastY;
            isResetCoordinateNeeded = false;
        }
    }

    private void recordScrollDistance() {
        if (currentScrollDistance >= heightOfActionBar) {
            mRecordDistance = (int) ((currentScrollDistance - heightOfHeader) * FRICTION);
        }
    }

    private void resetCoordinateWhenHeaderReachesTheEnd() {
        if (isFirstViewVisible() && isLastViewVisible()) {
            if (currentScrollDistance == heightOfActionBar) {
                mRecordDistance = (int) ((currentScrollDistance - heightOfHeader) * FRICTION);
                if (mLastY - mDownY < mLastDistance) {
                    mDownY = mLastY;
                }
            }
        }
        if(isFirstViewVisible()) {
            if (currentScrollDistance >= heightOfActionBar && currentScrollDistance <= heightOfActionBar + OFFSET) {
                mRecordDistance = (int) ((currentScrollDistance - heightOfHeader) * FRICTION);
                if (mLastY - mDownY > mLastDistance) {
                    mDownY = mLastY - OFFSET;
                }
            }
        }
    }

    private void scrollBackToTop() {
        scroller.startScroll(0, currentScrollDistance, 0, heightOfHeader - currentScrollDistance,
                SCROLL_DURATION);
        invalidate();
    }

    private void detectTouchEventConsumed() {
        if (isFirstViewVisible() && currentScrollDistance >= heightOfActionBar) {
            clearFocus();
            isTouchEventConsumed = true;
        } else {
            isTouchEventConsumed = false;
        }
    }

    private void scrolling() {
        if (isFirstViewVisible()) {
            if (currentMode == SCROLL_DOWN) {
                isBeingDraggedFromTop();
            }
            if (currentMode == SCROLL_UP) {
                isBeingDraggedFromTop();
            }
        }
    }

    private void isBeingDraggedFromTop() {
        int totalScrollDistance = (int) ((mLastDistance + mRecordDistance) / FRICTION);
        int changedHeight = heightOfHeader + totalScrollDistance;
        dragging(changedHeight);
    }

    private void notifyHeaderScrollChanged(int changedHeight) {
        int scrollDistance = changedHeight - heightOfHeader;
        if (onHeaderScrollChangedListener == null) {
            return;
        }
        if (scrollDistance > 0) {
            onHeaderScrollChangedListener.headerScrollChanged(scrollDistance);
        } else {
            onHeaderScrollChangedListener.actionBarTranslate(scrollDistance);
        }
    }

    private boolean isFirstViewVisible() {
        View firstChild = getChildAt(0);
        if (firstChild == null) {
            return false;
        } else {
            return firstChild.getTop() >= getTop() ;
        }
    }

    private boolean isLastViewVisible() {
        View view = getChildAt(listView.getCount() - 1);
        if (view == null) {
            return false;
        }
        return view.getBottom() <= listView.getBottom();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (isAllowToScrollBack && currentScrollDistance >= heightOfHeader) {
                dragging(scroller.getCurrY());
            }
            super.computeScroll();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public interface OnHeaderScrollChangedListener {
        void headerScrollChanged(float scrollDistance);
        void actionBarTranslate(float translateDistance);
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

}