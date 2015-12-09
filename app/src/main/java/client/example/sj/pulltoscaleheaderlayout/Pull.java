package client.example.sj.pulltoscaleheaderlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Scroller;

/**
 * Created by sj on 15/12/9.
 */
public class Pull extends ListView {

    static final int SCROLL_DURATION = 500;

    static final float OFFSET = 4f;

    static final float FRICTION = 2.0f;

    static final int SCROLL_DOWN = 1;

    static final int SCROLL_UP = 2;

    private View header;

    private View footer;

    private AbsListView.LayoutParams headerLayoutParams;

    private AbsListView.LayoutParams footerLayoutParams;

    private int heightOfActionBar;

    private int heightOfHeader;

    private float mDownY;

    private float mLastY;

    private ViewConfiguration viewConfiguration;

    private int mTouchSlop;

    private int currentHeightOfHeader;

    private int currentMode = -1;

    private int mPreviousScrollDistance;

    private int mLastScrollDistance;

    private int mRecordDistance;

    private boolean isTouchIntercept;

    private boolean isResetCoordinateNeeded;

    private boolean isAllowToScrollBack;

    private Interpolator interpolator;

    private OnHeaderScrollChangedListener onHeaderScrollChangedListener;

    private Scroller scroller;

    public Pull(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public Pull(Context context) {
        super(context);
        init(context,null);
    }

    private void init (Context context,AttributeSet attributeSet) {
        viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        header = new View(context,attributeSet);
        footer = new View(context,attributeSet);
        interpolator = new DecelerateInterpolator();
        scroller = new Scroller(context,interpolator);
        scroller.forceFinished(false);
        headerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addHeaderView(header);
        addFooterView(footer);
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
    }

    public void setOnHeaderScrollChangedListener(OnHeaderScrollChangedListener onHeaderScrollChangedListener) {
        this.onHeaderScrollChangedListener = onHeaderScrollChangedListener;
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
        return super.onInterceptHoverEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastScrollDistance = 0;
                mDownY = mLastY = ev.getY();
                isTouchIntercept = false;
                isAllowToScrollBack = false;
                scroller.forceFinished(false);
                recordScrollDistance();
                break;
            case MotionEvent.ACTION_MOVE:
                mDownY = mLastY;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_UP:
                if (isTouchIntercept) {
                    isResetCoordinateNeeded = true;
                    recordScrollDistance();
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (isTouchIntercept) {
                    isResetCoordinateNeeded = true;
                    recordScrollDistance();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mLastY = ev.getY();
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
                if (isTouchIntercept) {
                    isAllowToScrollBack = true;
                    if (currentHeightOfHeader > heightOfHeader) {
                        scrollBackToTop();
                    }
                    return true;
                }
                break;
        }
        if (isTouchIntercept) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void detectTouchIntercept() {
        if (Math.abs(mLastScrollDistance) < OFFSET) {
            isTouchIntercept = false;
            return;
        }
        if (currentHeightOfHeader > heightOfActionBar) {
            isTouchIntercept = true;
        } else {
            if (isFirstViewVisible()) {
                if (mLastScrollDistance >= OFFSET) {
                    isTouchIntercept = true;
                } else if(mLastScrollDistance <= -OFFSET) {
                    isTouchIntercept = false;
                }
            } else {
                isTouchIntercept = false;
            }
        }
        Log.e("PullScaleLayout", "是否捕获事件" + isTouchIntercept + "第一个view可见" + isFirstViewVisible() + " previous" + mPreviousScrollDistance + " mLast" + mLastScrollDistance + " header" + currentHeightOfHeader
                + " header" + header.getTop());
    }

    private void judgeTouchDirection() {
        float diff = mLastY - mDownY;
        if (diff <= -OFFSET) {
            currentMode = SCROLL_UP;
        } else if(diff > OFFSET) {
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
        mPreviousScrollDistance = Math.round(mLastY - mDownY);
        if (currentHeightOfHeader >= heightOfActionBar && currentHeightOfHeader <= heightOfActionBar + OFFSET) {
            mRecordDistance = (int) ((currentHeightOfHeader - heightOfHeader) * FRICTION);
            if (mLastScrollDistance != 0) {
                if (mPreviousScrollDistance > mLastScrollDistance) {
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
        mLastScrollDistance = Math.round(mLastY - mDownY);
    }

    private void scrollBackToTop() {
        scroller.startScroll(0, currentHeightOfHeader, 0, heightOfHeader - currentHeightOfHeader,
                SCROLL_DURATION);
        invalidate();
    }

    private void dragging() {
        if (isTouchIntercept) {
            isBeingDraggedFromTop();
        }
    }

    private void isBeingDraggedFromTop() {
        int totalScrollDistance = (int) ((mLastScrollDistance + mRecordDistance) / FRICTION);
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
        View firstChild = getChildAt(0);
        if (firstChild == null) {
            return false;
        } else {
            return firstChild.getTop() >= getTop();
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

}
