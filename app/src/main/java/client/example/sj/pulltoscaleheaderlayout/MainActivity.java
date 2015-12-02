package client.example.sj.pulltoscaleheaderlayout;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * 不下拉头部展现毛玻璃效果，下拉清晰放大头部
 */
public class MainActivity extends AppCompatActivity {

    private final static int MAX_HEIGHT = 160;

    private final static int MIN_HEIGHT = 48;

    private PullToScrollLayout pullToScrollLayout;

    private ImageView fadeCover;

    private ImageView cover;

    private int maxHeight;

    private int minHeight;

    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initScrollToScaleHeaderLayout();
    }

    private void initScrollToScaleHeaderLayout() {
        ArrayList<String> arrayList = new ArrayList<>();
        for(int i = 0 ; i < 30; i++) {
            arrayList.add("text" + i);
        }
        arrayAdapter = new ArrayAdapter<>(this,R.layout.list_item_layout,arrayList);
        maxHeight = dipToPx(this,MAX_HEIGHT);
        minHeight = dipToPx(this,MIN_HEIGHT);
        pullToScrollLayout = (PullToScrollLayout)findViewById(R.id.pull_to_scroll_layout);
        fadeCover = (ImageView)findViewById(R.id.fade_cover);
        cover = (ImageView)findViewById(R.id.cover);
        pullToScrollLayout.setMaxHeightOfHeader(maxHeight);
        pullToScrollLayout.setMinHeightOfHeader(minHeight);
        pullToScrollLayout.setAdapter(arrayAdapter);
        pullToScrollLayout.setOnViewScrollChangedListener(new PullToScrollLayout.OnViewScrollChangedListener() {
            @Override
            public void scrollChanged(float scrollDistance, boolean isDragged) {
                if (isDragged) {
                    //isDragged true的情况下代表整个layout正在被拖动,false代表listView正常滚动
                    float friction = (scrollDistance + maxHeight) / maxHeight;
                    float alphaFriction = maxHeight / (scrollDistance + maxHeight);
                    scaleView(friction,friction,cover,fadeCover);
                    setAlpha(alphaFriction,fadeCover);
                    setTranslate(0,scrollDistance / 2,cover,fadeCover);
                }
            }
        });
    }

    private void scaleView(float frictionX, float fractionY, View... params) {
        for(int i = 0 ; i < params.length; i++) {
            params[i].setScaleX(frictionX);
            params[i].setScaleY(fractionY);
        }
    }

    private void setAlpha(float alpha,View... params) {
        for (int i = 0 ; i < params.length; i++) {
            params[i].setAlpha(alpha);
        }
    }

    private void setTranslate(float offsetX,float offsetY,View... params) {
        for (int i = 0 ; i < params.length; i++) {
            params[i].setTranslationX(offsetX);
            params[i].setTranslationY(offsetY);
        }
    }

    public static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
