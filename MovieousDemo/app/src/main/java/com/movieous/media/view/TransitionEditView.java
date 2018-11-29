package com.movieous.media.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.movieous.media.R;
import com.movieous.media.view.transition.TransitionBase;

public class TransitionEditView extends LinearLayout {
    private Context mContext;
    private Button mBackBtn;
    private EditText mCurFocusText;
    private EditText mTitleEditText;
    private EditText mSubtitleEditText;
    private TransitionBase mTransition;
    private TransitionTextView mTransitionTitle;
    private TransitionTextView mTransitionSubtitle;

    public TransitionEditView(Context context) {
        this(context, null);
    }

    public TransitionEditView(final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        View view = LayoutInflater.from(context).inflate(R.layout.transition_edit_view, this);
        mBackBtn = view.findViewById(R.id.back_button);
        mTitleEditText = view.findViewById(R.id.title_edit_text);
        mSubtitleEditText = view.findViewById(R.id.subtitle_edit_text);
        mTitleEditText.setOnFocusChangeListener(mOnFocusChangeListener);
        mSubtitleEditText.setOnFocusChangeListener(mOnFocusChangeListener);

        mBackBtn.setOnClickListener(v -> {
            hideSoftInput();
            setVisibility(GONE);
        });
    }

    private void hideSoftInput() {
        if (mCurFocusText == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mCurFocusText.getWindowToken(), 0);
        }
    }

    private OnFocusChangeListener mOnFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                mCurFocusText = (EditText) v;
            }
        }
    };

    public void setTransition(TransitionBase transition) {
        mTransition = transition;
        mTransitionTitle = mTransition.getTitle();
        mTransitionSubtitle = mTransition.getSubtitle();

        mTitleEditText.setVisibility(GONE);
        mSubtitleEditText.setVisibility(GONE);

        if (mTransitionTitle != null) {
            mTitleEditText.setVisibility(VISIBLE);
            mCurFocusText = mTitleEditText;
            cloneEditText(mTitleEditText, mTransitionTitle);
            mTitleEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (getVisibility() == View.VISIBLE) {
                        cloneEditText(mTransitionTitle, mTitleEditText);
                        mTransition.updateTransitions();
                    }
                }
            });
        }

        if (mTransitionSubtitle != null) {
            mSubtitleEditText.setVisibility(VISIBLE);
            cloneEditText(mSubtitleEditText, mTransitionSubtitle);
            mSubtitleEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (getVisibility() == View.VISIBLE) {
                        cloneEditText(mTransitionSubtitle, mSubtitleEditText);
                        mTransition.updateTransitions();
                    }
                }
            });
        }
    }

    private void cloneEditText(EditText dstText, EditText srcText) {
        dstText.setText(srcText.getText());
        dstText.setTextColor(srcText.getTextColors());
        dstText.setTypeface(srcText.getTypeface());
        dstText.setTextSize(TypedValue.COMPLEX_UNIT_PX, srcText.getTextSize());
    }
}
