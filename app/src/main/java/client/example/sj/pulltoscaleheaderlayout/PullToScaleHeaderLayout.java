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

    private float mDownY;

    private float mLastY;

    private int mLastDistance;

    private int currentHeightOfHeader;

    private int currentMode = -1;

    private int mRecordDistance;

    private boolean isTouchIntercept;

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
        setOrientation(VERTICAL);
        listView = createListView(context,attributeSet);
        listView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mDownY = mLastY = event.getY();
                        isTouchIntercept = false;
                        isAllowToScrollBack = false;
                        scroller.forceFinished(false);
                        recordScrollDistance();
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
                        mLastY = event.getY();
                        resetCoordinateIfNeeded();
                        calculateScrollDistance();
                        detectTouchIntercept();
                        if (isTouchIntercept) {
                            dragging();
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        mLastDistance = 0;
                        isAllowToScrollBack = true;
                        if (currentHeightOfHeader > heightOfHeader) {
                            scrollBackToTop();
                        }
                        break;
                }
                if (isTouchIntercept) {
                    return true;
                }
                return false;
            }
        });
        header = new View(context,attributeSet);
        footer = new View(context,attributeSet);
        interpolator = new DecelerateInterpolator();
        scroller = new Scroller(context,interpolator);
        scroller.forceFinished(false);
        headerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        footerLayoutParams.height = heightOfFooter;
        footer.setLayoutParams(footerLayoutParams);
    }

    public void resizeHeader(int newHeight) {
        if (newHeight < this.heightOfActionBar) {
            newHeight = this.heightOfActionBar;
        }
        if (newHeight == currentHeightOfHeader) {
            return;
        }
        currentHeightOfHeader = newHeight;
        notifyHeaderScrollChanged(currentHeightOfHeader);
        headerLayoutParams.height = currentHeightOfHeader;
        header.setLayoutParams(headerLayoutParams);
//        scrollTo(0,heightOfHeader - currentHeightOfHeader);
    }

    public void setOnHeaderScrollChangedListener(OnHeaderScrollChangedListener onHeaderScrollChangedListener) {
        this.onHeaderScrollChangedListener = onHeaderScrollChangedListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = ev.getY();
                isTouchIntercept = false;
                isAllowToScrollBack = false;
                scroller.forceFinished(false);
                recordScrollDistance();
                break;
            case MotionEvent.ACTION_MOVE:
                isTouchIntercept = true;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = ev.getY();
                isTouchIntercept = false;
                isAllowToScrollBack = false;
                scroller.forceFinished(false);
                recordScrollDistance();
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
                mLastY = ev.getY();
                resetCoordinateIfNeeded();
                calculateScrollDistance();
                detectTouchIntercept();
                if (isTouchIntercept) {
                    dragging();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLastDistance = 0;
                isAllowToScrollBack = true;
                if (currentHeightOfHeader > heightOfHeader) {
                    scrollBackToTop();
                }
                break;
        }
        if (isTouchIntercept) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void detectTouchIntercept() {
        if (currentHeightOfHeader > heightOfActionBar) {
            isTouchIntercept = true;
        } else {
            if (isFirstViewVisible()) {
                if (mLastDistance >= OFFSET) {
                    isTouchIntercept = true;
                } else if(mLastDistance <= -OFFSET) {
                    isTouchIntercept = false;
                }
            } else {
                isTouchIntercept = false;
            }
        }
    }

    private void judgeTouchDirection() {
        float diff = mLastY - mDownY;
        if (diff <= -1) {
            currentMode = SCROLL_UP;
        } else {
            currentMode = SCROLL_DOWN;
        }
    }

    private void resetCoordinateIfNeeded() {
        if (isResetCoordinateNeeded) {
            mDownY = mLastY;
            isResetCoordinateNeeded = false;
        }
    }

    private void recordScrollDistance() {
        if (currentHeightOfHeader >= heightOfActionBar) {
            mRecordDistance = (int) ((currentHeightOfHeader - heightOfHeader) * FRICTION);
        }
    }

    private void calculateScrollDistance() {
        if (currentHeightOfHeader >= heightOfActionBar && currentHeightOfHeader <= heightOfActionBar + OFFSET) {
            mRecordDistance = (int) ((currentHeightOfHeader - heightOfHeader) * FRICTION);
            if (mLastDistance != 0) {
                if (mLastY - mDownY > mLastDistance) {
                    mDownY = mLastY - OFFSET;
                } else {
                    mDownY = mLastY + OFFSET;
                }
            } else {
                judgeTouchDirection();
                switch (currentMode) {
                    case SCROLL_UP:
                        mDownY = mLastY + OFFSET;
                        break;
                    default:
                        mDownY = mLastY - OFFSET;
                        break;
                }
            }
        }
        mLastDistance = Math.round(mLastY - mDownY);
    }

    private void scrollBackToTop() {
        scroller.startScroll(0, currentHeightOfHeader, 0, heightOfHeader - currentHeightOfHeader,
                SCROLL_DURATION);
        invalidate();
    }

    private void dragging() {
        if (isFirstViewVisible()) {
            isBeingDraggedFromTop();
        }
    }

    private void isBeingDraggedFromTop() {
        int totalScrollDistance = (int) ((mLastDistance + mRecordDistance) / FRICTION);
        int changedHeight = heightOfHeader + totalScrollDistance;
        resizeHeader(changedHeight);
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
        View firstChild = listView.getChildAt(0);
        if (firstChild == null) {
            return false;
        } else {
            return firstChild.getTop() >= listView.getTop() ;
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (isAllowToScrollBack && currentHeightOfHeader >= heightOfHeader) {
                resizeHeader(scroller.getCurrY());
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
            try {
                super.dispatchDraw(canvas);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
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