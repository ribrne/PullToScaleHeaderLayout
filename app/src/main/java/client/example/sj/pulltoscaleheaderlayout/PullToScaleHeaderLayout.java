package client.example.sj.pulltoscaleheaderlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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

    static final int SCROLL_DOWN = 1;

    static final int SCROLL_UP = 2;

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

    private int currentMode = -1;

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
        resizeHeightOfHeader(this.heightOfHeader);
    }

    public void setHeightOfFooter(int heightOfFooter) {
        this.heightOfFooter = heightOfFooter;
        resizeHeightOfFooter(this.heightOfFooter);
    }

    public void resizeHeightOfHeader(int newHeight) {
        if (newHeight < this.heightOfActionBar) {
            newHeight = this.heightOfActionBar;
        }
        if (newHeight >= this.heightOfHeader) {
            if (this.onScrollChangedListener != null) {
                onScrollChangedListener.headerScrollChanged(newHeight);
            }
        } else {
            if (this.onScrollChangedListener != null) {
                onScrollChangedListener.actionBarTranslate(newHeight - heightOfHeader);
            }
        }
        headerLayoutParams.height = newHeight;
        header.setLayoutParams(headerLayoutParams);
    }

    public void resizeHeightOfFooter(int heightOfFooter) {
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
                mDownY = mLastY = ev.getY() ;
                isTouchEventConsumed = false;
                recordScrollDistance();
                isScrollingBack = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                isResetCoordinateNeeded = true;
                recordScrollDistance();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                mLastY = ev.getY();
                final float diff;
                resetCoordinateIfNeeded();
                recordWhenHeaderReachesTheEnd();
                diff = mLastY - mDownY;
                if (diff <= -1f) {
                    currentMode = SCROLL_UP;
                } else if (diff >= 1f) {
                    clearFocus();
                    currentMode = SCROLL_DOWN;
                }
                mLastDistance = (int)diff;
                scrolling();
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

    private void resetCoordinateIfNeeded() {
        if (isResetCoordinateNeeded) {
            mDownY = mLastY;
            isResetCoordinateNeeded = false;
        }
    }

    private void recordScrollDistance() {
        if (headerLayoutParams.height >= heightOfActionBar) {
            mRecordDistance = (int) ((headerLayoutParams.height - heightOfHeader) * FRICTION);
            isResetCoordinateNeeded = true;
        }
    }

    private void recordWhenHeaderReachesTheEnd() {
        if (headerLayoutParams.height == heightOfActionBar) {
            if (mLastY - mDownY <= -1) {
                mRecordDistance = (int) ((headerLayoutParams.height - heightOfHeader) * FRICTION);
                mDownY = mLastY;
            }
        }
    }

    private void scrollBack() {
        if (mLastDistance > 0) {
            scrollBackToTop();
        }
    }

    private void scrollBackToTop() {
        int height = headerLayoutParams.height;
        scroller.startScroll(0, height, 0, heightOfHeader - height,
                SCROLL_DURATION);
        invalidate();
    }

    private void detectTouchEventConsumed() {
        if (headerLayoutParams.height > heightOfActionBar) {
            clearFocus();
            isTouchEventConsumed = true;
        } else {
            isTouchEventConsumed = false;
        }
    }

    private void scrolling() {
        detectTouchEventConsumed();
        if (currentMode == SCROLL_DOWN) {
            if (isFirstViewVisible()) {
                Log.e("PullToScaleHeader","向下滚动" + mLastDistance);
                isBeingDraggedFromTop();
            }
        }
        if (currentMode == SCROLL_UP) {
            if (headerLayoutParams.height > heightOfActionBar) {
                Log.e("PullToScaleHeader","向上滚动" + mLastDistance);
                isBeingDraggedFromTop();
            }
        }
    }

    private void isBeingDraggedFromTop() {
        int totalScrollDistance = (int) ((mLastDistance + mRecordDistance) / FRICTION);
        int changedHeight = heightOfHeader + totalScrollDistance;
        resizeHeightOfHeader(changedHeight);
    }

    private boolean isFirstViewVisible() {
        View firstChild = getChildAt(0);
        if (firstChild == null) {
            return false;
        } else {
            return firstChild.getTop() >= getTop() ;
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (isScrollingBack) {
                if (heightOfHeader < headerLayoutParams.height) {
                    resizeHeightOfHeader(scroller.getCurrY());
                }
                if (heightOfFooter < footerLayoutParams.height) {
                    resizeHeightOfFooter(scroller.getCurrY());
                }
                super.computeScroll();
            }
        }
    }

    public interface OnScrollChangedListener {
        void headerScrollChanged(float scrollDistance);
        void actionBarTranslate(float translateDistance);
    }


}
