package client.example.sj.pulltoscaleheaderlayout;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

    private PullToScaleHeaderLayout pullToScrollLayout;

    private ImageView fadeCover;

    private ImageView cover;

    private int heightOfHeader;

    private int heightOfFooter;

    private int heightOfActionBar;

    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initScrollToScaleHeaderLayout();
    }

    private void initScrollToScaleHeaderLayout() {
        ArrayList<String> arrayList = new ArrayList<>();
        for(int i = 0 ; i < 10; i++) {
            arrayList.add("text" + i);
        }
        arrayAdapter = new ArrayAdapter<>(this,R.layout.list_item_layout,arrayList);
        heightOfHeader = dipToPx(this,MAX_HEIGHT);
        heightOfFooter = dipToPx(this,MIN_HEIGHT);
        heightOfActionBar = dipToPx(this,MIN_HEIGHT);
        pullToScrollLayout = (PullToScaleHeaderLayout)findViewById(R.id.pull_to_scroll_layout);
        fadeCover = (ImageView)findViewById(R.id.fade_cover);
        cover = (ImageView)findViewById(R.id.cover);
//        fadeCover.setVisibility(View.GONE);
//        cover.setVisibility(View.GONE);
        pullToScrollLayout.setHeightOfActionBar(heightOfActionBar);
        pullToScrollLayout.setHeightOfHeader(heightOfHeader);
        pullToScrollLayout.setHeightOfFooter(heightOfFooter);
        pullToScrollLayout.setAdapter(arrayAdapter);
        pullToScrollLayout.setOnScrollChangedListener(new PullToScaleHeaderLayout.OnScrollChangedListener() {
            @Override
            public void headerScrollChanged(float scrollDistance) {
                float friction = scrollDistance / heightOfHeader;
                float alphaFriction = heightOfHeader / scrollDistance;
                scaleView(friction,friction,cover,fadeCover);
                setAlpha(alphaFriction,fadeCover);
                setTranslate(0,(scrollDistance - heightOfHeader) / 2,cover,fadeCover);
            }

            @Override
            public void actionBarTranslate(float translateDistance) {
                setTranslate(0,translateDistance,cover,fadeCover);
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
