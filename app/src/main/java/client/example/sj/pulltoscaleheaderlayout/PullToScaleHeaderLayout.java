package client.example.sj.pulltoscaleheaderlayout;

import android.content.Context;
import android.util.AttributeSet;
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

    static final int SCROLL_DURATION = 500;

    static final float FRICTION = 2.0f;

    static final int RESET = -1;

    static final int PULL_OVER_SCROLL_TOP = 1;

    static final int PULL_OVER_SCROLL_BOTTOM = 2;

    static final int SCROLL_IN_SCOPE_OF_HEIGHT = 3;

    private View header;

    private View footer;

    private AbsListView.LayoutParams headerLayoutParams;

    private AbsListView.LayoutParams footerLayoutParams;

    private int heightOfHeader;

    private int heightOfFooter;

    private float mLastX;

    private float mLastY;

    private int mLastDistance;

    private int currentMode;

    private int mTouchSlop;

    private int mRecordDistance;

    private boolean isBeingDragged;

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

    public void setHeightOfHeader(int heightOfHeader) {
        this.heightOfHeader = heightOfHeader;
        updateHeightOfHeader(this.heightOfHeader);
    }

    public void setHeightOfFooter(int heightOfFooter) {
        this.heightOfFooter = heightOfFooter;
        updateHeightOfFooter(this.heightOfFooter);
    }

    public void updateHeightOfHeader(int heightOfHeader) {
        if (heightOfHeader < this.heightOfHeader) {
            heightOfHeader = this.heightOfHeader;
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
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = ev.getX();
                mLastY = ev.getY() ;
                isBeingDragged = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
//                mRecordDistance = mLastDistance;
                break;
            case MotionEvent.ACTION_MOVE:
                final float x = ev.getX(),y = ev.getY();
                final float diff,oppositeDiff,absDiff,absOppositeDiff;
                diff = y - mLastY;
                oppositeDiff = x - mLastX;
                absDiff = Math.abs(diff);
                absOppositeDiff = Math.abs(oppositeDiff);
                if(absDiff > absOppositeDiff) {
                    if (isLastViewVisible() && diff <= -1f) {
                        isBeingDragged = false;
                        currentMode = PULL_OVER_SCROLL_BOTTOM;
                    } else if (isFirstViewVisible() && diff >= 1f) {
                        isBeingDragged = true;
                        currentMode = PULL_OVER_SCROLL_TOP;
                    } else {
                        isBeingDragged = false;
                        currentMode = SCROLL_IN_SCOPE_OF_HEIGHT;
                    }
                    mLastDistance = (int)diff;
                    dragging();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                scrollBack();
                break;
        }
        if (isBeingDragged) {
            return true;
        }
        return super.onTouchEvent(ev);
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

    private void dragging() {
        if (currentMode == PULL_OVER_SCROLL_TOP || headerLayoutParams.height > heightOfHeader) {
            isBeingDraggedToTop();
        }
        if (currentMode == PULL_OVER_SCROLL_BOTTOM || footerLayoutParams.height > heightOfFooter) {
            isBeingDraggedToBottom();
        }
    }

    private void isBeingDraggedToTop() {
        if (mLastDistance <= 0) {
            updateHeightOfHeader(heightOfHeader);
        } else {
            updateHeightOfHeader(Math.max(mTouchSlop,heightOfHeader) + (int)(mLastDistance / FRICTION));
        }
    }

    private void isBeingDraggedToBottom() {
        if (mLastDistance >= 0) {
            updateHeightOfFooter(heightOfFooter);
        } else {
            updateHeightOfFooter(Math.max(mTouchSlop,heightOfFooter) - mLastDistance);
        }
    }

    private boolean isFirstViewVisible() {
        View firstChild = getChildAt(0);
        if (firstChild == null) {
            return false;
        } else {
            return heightOfFooter == footerLayoutParams.height && firstChild.getTop() >= getTop();
        }
    }

    private boolean isLastViewVisible() {
        View lastChild = getChildAt(getCount() - 1 - getFirstVisiblePosition() - getHeaderViewsCount() - getFooterViewsCount());
        if (lastChild == null) {
            return false;
        } else {
            return headerLayoutParams.height == heightOfHeader && lastChild.getBottom() <= getBottom();
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (currentMode == PULL_OVER_SCROLL_TOP) {
                updateHeightOfHeader(scroller.getCurrY());
            } else {
                updateHeightOfFooter(scroller.getCurrY());
            }
            if (scroller.getCurrY() == 0) {
                currentMode = RESET;
            } else {
                postInvalidate();
            }
        }
        super.computeScroll();
    }

    public interface OnScrollChangedListener {
        void headerScrollChanged(float scrollDistance);
        void footerScrollChanged(float scrollDistance);
    }


}
