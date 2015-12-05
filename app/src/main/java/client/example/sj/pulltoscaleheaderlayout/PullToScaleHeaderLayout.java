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
 * Created by sj on 15/12/2.
 */
public class PullToScaleHeaderLayout extends ListView {

    static final String TAG ="PullToScaleHeaderLayout";

    static final int SCROLL_DURATION = 500;

    static final float FRICTION = 2.0f;

    static final int SCROLL_DOWN = 1;

    static final int SCROLL_UP = 2;

    private View header;

    private View footer;

    private AbsListView.LayoutParams headerLayoutParams;

    private AbsListView.LayoutParams footerLayoutParams;

    private int heightOfActionBar;

    private int heightOfHeader;

    private int heightOfFooter;

    private float mLastX;

    private float mLastY;

    private int mLastDistance;

    private int currentMode = -1;

    private int mTouchSlop;

    private int mRecordDistance;

    private boolean isTouchEventConsumed;

    private boolean isResetCoordinateNeeded;

    private boolean isScrollingBack;

    private Interpolator interpolator;

    private OnScrollChangedListener onScrollChangedListener;

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
        header = new View(context,attributeSet);
        footer = new View(context,attributeSet);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        interpolator = new DecelerateInterpolator();
        scroller = new Scroller(context,interpolator);
        headerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,heightOfHeader);
        footerLayoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,heightOfFooter);
        addHeaderView(header);
        addFooterView(footer);
    }

    public void setHeightOfActionBar(int height) {
        heightOfActionBar = height;
    }

    public void setHeightOfHeader(int heightOfHeader) {
        this.heightOfHeader = heightOfHeader;
        updateHeightOfHeader(this.heightOfHeader);
    }

    public void setHeightOfFooter(int heightOfFooter) {
        this.heightOfFooter = heightOfFooter;
        updateHeightOfFooter(this.heightOfFooter);
    }

    public void updateHeightOfHeader(int heightOfHeader) {
        if (heightOfHeader < this.heightOfActionBar) {
            heightOfHeader = this.heightOfActionBar;
        }
        if (this.onScrollChangedListener != null) {
            onScrollChangedListener.headerScrollChanged(heightOfHeader);
        }
        headerLayoutParams.height = heightOfHeader;
        header.setLayoutParams(headerLayoutParams);
    }

    public void updateHeightOfFooter(int heightOfFooter) {
        if (heightOfFooter < this.heightOfFooter) {
            heightOfFooter = this.heightOfFooter;
        }
        footerLayoutParams.height = heightOfFooter;
        footer.setLayoutParams(footerLayoutParams);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = ev.getX();
                mLastY = ev.getY() ;
                isTouchEventConsumed = false;
                isScrollingBack = false;
                recordScrollDistance();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                isResetCoordinateNeeded = true;
                recordScrollDistance();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                final float x = ev.getX(),y = ev.getY();
                final float diff,oppositeDiff,absDiff,absOppositeDiff;
                if (isResetCoordinateNeeded) {
                    mLastX = x;
                    mLastY = y;
                    isResetCoordinateNeeded = false;
                }
                diff = y - mLastY;
                oppositeDiff = x - mLastX;
                absDiff = Math.abs(diff);
                absOppositeDiff = Math.abs(oppositeDiff);
                if(absDiff > absOppositeDiff) {
                    if (diff <= -1f) {
                        currentMode = SCROLL_UP;
                    } else if (diff >= 1f) {
                        clearFocus();
                        currentMode = SCROLL_DOWN;
                    }
                    mLastDistance = (int)diff;
                    scrolling();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isScrollingBack = true;
                isTouchEventConsumed =false;
                scrollBack();
                break;
        }
        if (isTouchEventConsumed) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void recordScrollDistance() {
        if (headerLayoutParams.height >= heightOfActionBar) {
            mRecordDistance = (int) ((headerLayoutParams.height - heightOfHeader) * FRICTION);
        }
    }

    private void clearRecordDistance() {
        if (headerLayoutParams.height == heightOfActionBar) {
            mRecordDistance = 0;
        }
    }

    private void scrollBack() {
        if (mLastDistance > 0) {
            scrollBackToTop();
        } else {
            scrollBackToBottom();
        }
    }

    private void scrollBackToBottom() {
        int height = footerLayoutParams.height;
        scroller.startScroll(0, height, 0, heightOfFooter - height,
                SCROLL_DURATION);
        invalidate();
    }

    private void scrollBackToTop() {
        int height = headerLayoutParams.height;
        scroller.startScroll(0, height, 0, heightOfHeader - height,
                SCROLL_DURATION);
        invalidate();
    }

    private void detectTouchEventConsumed() {
        if (headerLayoutParams.height > heightOfActionBar || footerLayoutParams.height > heightOfFooter) {
            clearFocus();
            isTouchEventConsumed = true;
        } else {
            isTouchEventConsumed = false;
        }
    }

    private void scrolling() {
        detectTouchEventConsumed();
        if (currentMode == SCROLL_DOWN) {
            if (footerLayoutParams.height > heightOfFooter) {
                isBeingDraggedFromBottom();
            } else if (isFirstViewVisible()) {
                isBeingDraggedFromTop();
            }
        }
        if (currentMode == SCROLL_UP) {
            if (headerLayoutParams.height > heightOfActionBar) {
                isBeingDraggedFromTop();
            } else {
                isResetCoordinateNeeded = true;
            }
            if (isLastViewVisible()) {
                isBeingDraggedFromBottom();
            }
        }
    }

    private void isBeingDraggedFromTop() {
        clearRecordDistance();
        int totalScrollDistance = (int) ((mLastDistance + mRecordDistance) / FRICTION);
        int changedHeight = Math.max(mTouchSlop, heightOfHeader) + totalScrollDistance;
        updateHeightOfHeader(changedHeight);
    }

    private void isBeingDraggedFromBottom() {
        if (mLastDistance >= 0) {
            updateHeightOfFooter(heightOfFooter);
        } else {
            int changedHeight = Math.max(mTouchSlop,heightOfFooter) - mLastDistance;
            updateHeightOfFooter(changedHeight);
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

    private boolean isLastViewVisible() {
        View lastChild = getChildAt(getCount() - 1 - getFirstVisiblePosition());
        if (lastChild == null) {
            return false;
        } else {
            return lastChild.getBottom() <= getBottom();
        }
    }

    @Override
    public void computeScroll() {
        if (isScrollingBack) {
            if (scroller.computeScrollOffset()) {
                if (heightOfHeader < headerLayoutParams.height) {
                    updateHeightOfHeader(scroller.getCurrY());
                }
                if(heightOfFooter < footerLayoutParams.height) {
                    updateHeightOfFooter(scroller.getCurrY());
                }
                if (scroller.getCurrY() == 0) {
                    mLastDistance = mRecordDistance = 0;
                }
            }
            super.computeScroll();
        } else {

        }
    }

    public interface OnScrollChangedListener {
        void headerScrollChanged(float scrollDistance);
        void footerScrollChanged(float scrollDistance);
    }


}
