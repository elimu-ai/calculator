package com.android.calculator2.floating;

import android.content.Context;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.calculator2.HistoryAdapter;
import com.android.calculator2.util.DigitLabelHelper;
import com.xlythe.math.History;

import ai.elimu.calculator.R;

public class FloatingCalculatorPageAdapter extends PagerAdapter {
    private final Context mContext;
    private final View.OnClickListener mListener;
    private final History mHistory;
    private final View[] mViews = new View[3];

    public FloatingCalculatorPageAdapter(Context context, View.OnClickListener listener, History history) {
        mContext = context;
        mListener = listener;
        mHistory = history;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return 2; //Removed the green pad with additional operators
    }

    @Override
    public void startUpdate(@NonNull ViewGroup container) {
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View v = getViewAt(position);
        container.addView(v);

        return v;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if(mViews[position] != null) mViews[position] = null;
        container.removeView((View) object);
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    public View getViewAt(final int position) {
        if(mViews[position] != null) return mViews[position];
        switch(position) {
            case 0:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_history, null);
                RecyclerView historyView =
                        mViews[position].findViewById(R.id.history);
                setUpHistory(historyView);
                break;
            case 1:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_basic, null);
                DigitLabelHelper.getInstance().getTextForDigits(mContext, (id, text) -> {
                            TextView textView = mViews[position].findViewById(id);
                            textView.setText(text);
                        });
                break;
            case 2:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_advanced, null);
                break;
        }
        applyListener(mViews[position]);
        return mViews[position];
    }

    private void applyListener(View view) {
        if(view instanceof ViewGroup) {
            for(int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                applyListener(((ViewGroup) view).getChildAt(i));
            }
        } else if(view instanceof Button) {
            view.setOnClickListener(mListener);
        } else if(view instanceof ImageButton) {
            view.setOnClickListener(mListener);
        }
    }

    private void setUpHistory(RecyclerView historyView) {
        HistoryAdapter.HistoryItemCallback listener = entry -> {
            // TODO: implement
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);
        layoutManager.setStackFromEnd(true);
        historyView.setLayoutManager(layoutManager);

        FloatingHistoryAdapter historyAdapter = new FloatingHistoryAdapter(mContext, mHistory, listener);
        mHistory.setObserver(historyAdapter);
        historyView.setAdapter(historyAdapter);
    }
}
