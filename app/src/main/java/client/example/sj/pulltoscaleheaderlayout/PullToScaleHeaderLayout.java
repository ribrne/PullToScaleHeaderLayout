package client.example.sj.pulltoscaleheaderlayout;

import android.content.Context;
import android.util.AttributeSet;
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

    private int heightOfFooter;

    private float mDownY;

    private float mLastY;

    private int mLastDistance;

    private int currentHeightOfHeader;

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
        header = new View(context,attributeSet);
        footer = new View(context,attributeSet);
        interpolator = new DecelerateInterpolator();
        scroller = new Scroller(context,interpolator);
        scroller.forceFinished(false);
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
        if (newHeight == currentHeightOfHeader) {
            return;
        }
        currentHeightOfHeader = newHeight;
        notifyHeaderScrollChanged(currentHeightOfHeader);
        headerLayoutParams.height = currentHeightOfHeader;
        header.setLayoutParams(headerLayoutParams);
    }

    public void resizeHeightOfFooter(int heightOfFooter) {
        if (heightOfFooter < this.heightOfFooter) {
            heightOfFooter = this.heightOfFooter;
        }
        footerLayoutParams.height = heightOfFooter;
        footer.setLayoutParams(footerLayoutParams);
    }

    public void setOnHeaderScrollChangedListener(OnHeaderScrollChangedListener onHeaderScrollChangedListener) {
        this.onHeaderScrollChangedListener = onHeaderScrollChangedListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = ev.getY() ;
                isTouchEventConsumed = false;
                isAllowToScrollBack = false;
                scroller.forceFinished(false);
                recordScrollDistance();
                break;
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
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isAllowToScrollBack = true;
                isTouchEventConsumed = false;
                if (currentHeightOfHeader > heightOfHeader) {
                    scrollBackToTop();
                }
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
        if (currentHeightOfHeader >= heightOfActionBar) {
            mRecordDistance = (int) ((currentHeightOfHeader - heightOfHeader) * FRICTION);
        }
    }

    private void resetCoordinateWhenHeaderReachesTheEnd() {
        if (isFirstViewVisible() && isLastViewVisible()) {
            if (currentHeightOfHeader == heightOfActionBar) {
                mRecordDistance = (int) ((currentHeightOfHeader - heightOfHeader) * FRICTION);
                if (mLastY - mDownY < mLastDistance) {
                    mDownY = mLastY;
                }
            }
        }
        if(isFirstViewVisible()) {
            if (currentHeightOfHeader >= heightOfActionBar && currentHeightOfHeader <= heightOfActionBar + OFFSET) {
                mRecordDistance = (int) ((currentHeightOfHeader - heightOfHeader) * FRICTION);
                if (mLastY - mDownY > mLastDistance) {
                    mDownY = mLastY - OFFSET;
                }
            }
        }
    }

    private void scrollBackToTop() {
        scroller.startScroll(0, currentHeightOfHeader, 0, heightOfHeader - currentHeightOfHeader,
                SCROLL_DURATION);
        invalidate();
    }

    private void detectTouchEventConsumed() {
        if (isFirstViewVisible() && currentHeightOfHeader > heightOfActionBar) {
            clearFocus();
            isTouchEventConsumed = true;
        } else {
            isTouchEventConsumed = false;
        }
    }

    private void scrolling() {
        detectTouchEventConsumed();
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
        resizeHeightOfHeader(changedHeight);
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
        View view = getChildAt(getCount() - 1);
        if (view == null) {
            return false;
        }
        return view.getBottom() <= getBottom();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (isAllowToScrollBack && currentHeightOfHeader >= heightOfHeader) {
                resizeHeightOfHeader(scroller.getCurrY());
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
