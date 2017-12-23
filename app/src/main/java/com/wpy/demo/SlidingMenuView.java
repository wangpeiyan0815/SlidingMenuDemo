package com.wpy.demo;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;

/**
 * Created by dell on 2017/12/19.
 * 分析
 * 1. 确定布局 只能能有两个子部局 采用自定义ViewGroup继承HorizontalScrollView
 *    会发现布局显示不完整 就需要在 onFinishInflate 重新给view 宽高  这个方法在布局加载完毕会走
 * 2.在onLayout中设置菜单默认是关闭的 scrollTo(mMenuWidth, 0);
 * 3.处理onThuch 事件 控制滑动的大小来判断是否打开或关闭事件
 * 4.GestureDetector 检测快速滑动
 * 5.进行事件拦截 来处理 菜单打开点击右侧内容关闭菜单
 */

public class SlidingMenuView extends HorizontalScrollView {
    // 菜单的宽度
    private int mMenuWidth;
    private View contentView;
    private View menuView;

    private GestureDetector mGestureDetector; // 系统自带的手势处理类
    private boolean mMenuIsOpen = false;  // 当前是否打开
    private boolean mIsIntercept;

    public SlidingMenuView(Context context) {
        this(context, null);
    }

    public SlidingMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //取出自定义属性
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SlidingMenuView);
        float menuRight = array.getDimension(R.styleable.SlidingMenuView_menuRightMargin,
                dip2px(context, 50));
        // 菜单页的宽度是 = 屏幕的宽度 - 右边的一小部分距离（自定义属性）
        mMenuWidth = (int) (getScreenWidth(context) - menuRight);
        array.recycle();

        mGestureDetector = new GestureDetector(new GestureDetectorListener());
    }

    //布局走完会调用该方法
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //重新设置宽度
        //获取LinearLayout
        ViewGroup container = (ViewGroup) getChildAt(0);
        //进行判断里面的布局只能是两个
        int childCount = container.getChildCount();
        if (childCount != 2) {
            //也可以抛异常提示
            return;
        }
        //获取菜单view
        menuView = container.getChildAt(0);
        ViewGroup.LayoutParams menuLayoutParams = menuView.getLayoutParams();
        menuLayoutParams.width = mMenuWidth;
        menuView.setLayoutParams(menuLayoutParams);

        //获取内容View  等于屏幕的宽度
        contentView = container.getChildAt(1);
        ViewGroup.LayoutParams contentLayoutParams = contentView.getLayoutParams();
        contentLayoutParams.width = getScreenWidth(getContext());
        contentView.setLayoutParams(contentLayoutParams);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //一开始展示的是 内容页
        scrollTo(mMenuWidth, 0);
    }

    @Override
    protected void onScrollChanged(int l, int t, int left, int oldt) {
        super.onScrollChanged(l, t, left, oldt);
        //计算滑动距离   梯度值
        float scale = (1f * l / mMenuWidth);
        // 右边的缩放: 最小是 0.7f, 最大是 1f
        float rightScale = 0.8f + 0.2f * scale;
        // 设置右边的缩放,默认是以中心点缩放
        // 设置缩放的中心点位置
        ViewCompat.setPivotX(contentView, 0);
        ViewCompat.setPivotY(contentView, contentView.getHeight() / 2);
        ViewCompat.setScaleX(contentView, rightScale);
        ViewCompat.setScaleY(contentView, rightScale);

        // 菜单的缩放和透明度
        // 透明度是 半透明到完全透明  0.5f - 1.0f
        float alpha = .5f + (1 - scale) * 0.5f;
        ViewCompat.setAlpha(menuView, alpha);
        // 缩放 0.7f - 1.0f
        float leftScale = 0.7f + (1 - scale) * 0.3f;
        ViewCompat.setScaleX(menuView, leftScale);
        ViewCompat.setScaleY(menuView, leftScale);
        // 最后一个效果 退出这个按钮刚开始是在右边，安装我们目前的方式永远都是在左边
        // 设置平移，先看一个抽屉效果
        // ViewCompat.setTranslationX(mMenuView,l);
        // 平移 l*0.7f
        ViewCompat.setTranslationX(menuView, 0.25f * l);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mIsIntercept = false;
        if(mMenuIsOpen){
            float currenX = ev.getX();
            if (currenX > mMenuWidth) {
                //关闭菜单
                closeMenu();
                mIsIntercept = true;
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mIsIntercept){
            return true;
        }
        // 拦截处理事件
        if (mGestureDetector.onTouchEvent(ev)) {
            return mGestureDetector.onTouchEvent(ev);
        }
        //点击松开时进行菜单的关闭打开
        //根据滑动的距离开判断松开时是否打开或关闭菜单
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            //拿到x滑动距离
            int currrent = (int) getScrollX();

            if (currrent > mMenuWidth / 2) {
                closeMenu();
            } else {
                openMenu();
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private class GestureDetectorListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            Log.e("TAG", "velocityX -> " + velocityX);// 向右快速滑动会是正的  +   向左快速滑动 是  -

            // 如果菜单是打开的   向右向左快速滑动都会回调这个方法
            if (mMenuIsOpen) {
                if (velocityX < 0) {
                    toggleMenu();
                    return true;
                }
            } else {
                if (velocityX > 0) {
                    toggleMenu();
                    return true;
                }
            }
            return false;
        }
    }

    // 切换菜单
    private void toggleMenu() {
        if (mMenuIsOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    //菜单打开方法
    private void openMenu() {
        mMenuIsOpen = true;
        smoothScrollTo(0, 0);
    }

    //菜单关闭方法
    private void closeMenu() {
        mMenuIsOpen = false;
//        smoothScrollTo(mMenuWidth, 0);
        smoothScrollBy(mMenuWidth,0);
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    private int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * Dip into pixels
     */
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
