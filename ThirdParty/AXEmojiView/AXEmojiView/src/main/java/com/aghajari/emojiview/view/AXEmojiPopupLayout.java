/*
 * Copyright (C) 2020 - Amir Hossein Aghajari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.aghajari.emojiview.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import android.graphics.Insets;

import com.aghajari.emojiview.listener.PopupListener;
import com.aghajari.emojiview.search.AXEmojiSearchView;
import com.aghajari.emojiview.utils.Utils;


public class AXEmojiPopupLayout extends FrameLayout implements AXPopupInterface {


    AXEmojiPopupView popupView;
    private View keyboard;
    protected boolean changeHeightWithKeyboard = true;
    protected KeyboardHeightProvider heightProvider = null;
    private final Handler keyboardMonitorHandler = new Handler();
    private boolean keyboardMonitorRunning = false;
    private boolean keyboardSeenByMonitor = false;
    private long keyboardMonitorStartTime = 0L;
    private boolean keyboardModeActive = false;

    public AXEmojiPopupLayout(Context context) {
        this(context, null);
    }

    public AXEmojiPopupLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AXEmojiPopupLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Utils.forceLTR(this);
        initKeyboardHeightProvider();
    }

    public void initPopupView(AXEmojiBase content) {
        if (keyboard == null) {
            keyboard = new View(getContext());
            this.addView(keyboard,new FrameLayout.LayoutParams(-1,0));
        }
        content.setPopupInterface(this);
        popupView = new AXEmojiPopupView(this, content);
        popupView.setFocusableInTouchMode(true);
        popupView.setFocusable(true);
        popupView.requestFocus();

        initKeyboardHeightProvider();
    }

    protected void initKeyboardHeightProvider() {
        if (heightProvider == null) heightProvider = new KeyboardHeightProvider(this);
        if (popupView != null) {
            popupView.post(() -> heightProvider.start());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (popupView!=null && popupView.listener!=null)
            popupView.listener.onViewHeightChanged(h);
    }

    public boolean onBackPressed() {
        if (popupView == null) return false;
        return popupView.onBackPressed();
    }

    @Override
    public void reload() {
        if (popupView!=null) popupView.reload();
    }

    public boolean isKeyboardOpen() {
        return (popupView != null && popupView.isKeyboardOpen);
    }

    public void hidePopupView() {
        if (popupView != null) popupView.onlyHide();
    }

    public int getPopupHeight() {
        return ((popupView != null) ? popupView.getPopupHeight() : 0);
    }

    @Override
    public void toggle() {
        if (popupView != null) {
            if (!popupView.isShowing()) {
                leaveKeyboardMode();
            }
            popupView.toggle();
        }
    }

    @Override
    public void show() {
        if (popupView != null) {
            leaveKeyboardMode();
            popupView.show();
        }
    }

    @Override
    public void dismiss() {
        if (popupView != null) {
            leaveKeyboardMode();
            popupView.dismiss();
        }
    }

    @Override
    public boolean isShowing() {
        return popupView != null && popupView.isShowing;
    }

    public void setPopupListener(PopupListener listener) {
        if (popupView != null) popupView.setPopupListener(listener);
    }

    public void hideAndOpenKeyboard() {
        if (popupView != null) {
            keyboardModeActive = true;
            int reservedHeight = popupView.prepareForKeyboard();
            if (reservedHeight > 0) {
                setKeyboardPlaceholderHeight(reservedHeight);
            }
            popupView.openKeyboard();
            startKeyboardMonitor();
            requestApplyInsets();
            postDelayed(this::requestApplyInsets, 80);
        }
    }

    public void openKeyboard() {
        if (popupView != null) {
            keyboardModeActive = true;
            popupView.openKeyboard();
            startKeyboardMonitor();
            requestApplyInsets();
            postDelayed(this::requestApplyInsets, 80);
        }
    }

    public void updateKeyboardStateOpened(int height) {
        if (popupView != null) popupView.updateKeyboardStateOpened(height);
    }

    public void updateKeyboardStateClosed() {
        if (popupView != null) popupView.updateKeyboardStateClosed();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (heightProvider != null) {
            heightProvider.stickOnStart();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        leaveKeyboardMode();
        if (heightProvider != null) heightProvider.close();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30 && popupView != null && isInputKeyboardOwner()) {
            boolean imeVisible = insets.isVisible(WindowInsets.Type.ime());
            if (imeVisible) {
                keyboardModeActive = true;
                Insets imeInsets = insets.getInsets(WindowInsets.Type.ime());
                int keyboardHeight = imeInsets.bottom;
                keyboardSeenByMonitor = true;
                popupView.updateKeyboardStateOpened(keyboardHeight);
                if (changeHeightWithKeyboard) {
                    setKeyboardPlaceholderHeight(keyboardHeight);
                }
                if (!keyboardMonitorRunning) {
                    startKeyboardMonitor();
                }
            } else if (keyboardSeenByMonitor || keyboardModeActive) {
                popupView.updateKeyboardStateClosed();
                leaveKeyboardMode();
            }
        }
        return super.onApplyWindowInsets(insets);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            post(this::restoreKeyboardModeIfNeeded);
            postDelayed(this::restoreKeyboardModeIfNeeded, 80);
            requestApplyInsets();
        }
    }

    public void setMaxHeight(int maxHeight) {
        if (popupView!=null) popupView.setMaxHeight(maxHeight);
    }

    public int getMaxHeight() {
        return popupView!=null ? popupView.getMaxHeight() : -1;
    }

    public void setMinHeight(int minHeight) {
        if (popupView!=null) popupView.setMinHeight(minHeight);
    }

    public int getMinHeight() {
        return popupView!=null ? popupView.getMinHeight() : -1;
    }

    public void setPopupAnimationEnabled(boolean enabled){
        if (popupView!=null) popupView.animationEnabled = enabled;
    }

    public boolean isPopupAnimationEnabled(){
        return popupView == null || popupView.animationEnabled;
    }

    public void setPopupAnimationDuration(long duration){
        if (popupView!=null) popupView.animationDuration = duration;
    }

    public long getPopupAnimationDuration(){
        return popupView!=null ? popupView.animationDuration : 250;
    }

    public AXEmojiSearchView getSearchView() {
        return popupView.getSearchView();
    }

    public void setSearchView(AXEmojiSearchView searchView) {
        popupView.setSearchView(searchView);
    }

    public void hideSearchView(){
        popupView.hideSearchView(true);
    }

    public void showSearchView(){
        popupView.showSearchView();
    }

    public boolean isShowingSearchView(){
        return popupView!=null && popupView.isShowingSearchView();
    }

    public void setSearchViewAnimationEnabled(boolean enabled){
        popupView.searchViewAnimationEnabled = enabled;
    }

    public boolean isSearchViewAnimationEnabled(){
        return popupView == null || popupView.searchViewAnimationEnabled;
    }

    public void setSearchViewAnimationDuration(long duration){
        if (popupView!=null) popupView.searchViewAnimationDuration = duration;
    }

    public long getSearchViewAnimationDuration(){
        return popupView!=null ? popupView.searchViewAnimationDuration : 250;
    }

    /**
     * The keyboard height provider, this class uses a PopupWindow
     * to calculate the window height when the floating keyboard is opened and closed.
     */
    protected static class KeyboardHeightProvider extends PopupWindow {

        /**
         * The view that is used to calculate the keyboard height
         */
        private final View popupView;

        /**
         * The parent view
         */
        private final View parentView;

        private final AXEmojiPopupLayout layout;

        /**
         * Construct a new KeyboardHeightProvider
         */
        public KeyboardHeightProvider(AXEmojiPopupLayout popupLayout) {
            super(popupLayout.getContext());

            this.popupView = new View(popupLayout.getContext());
            this.layout = popupLayout;
            setContentView(popupView);

            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

            parentView = Utils.asActivity(popupLayout.getContext()).findViewById(android.R.id.content);

            setWidth(0);
            setHeight(WindowManager.LayoutParams.MATCH_PARENT);

            popupView.getViewTreeObserver().addOnGlobalLayoutListener(this::handleOnGlobalLayout);
        }

        /**
         * Start the KeyboardHeightProvider, this must be called after the onResume of the Activity.
         * PopupWindows are not allowed to be registered before the onResume has finished
         * of the Activity.
         */
        public void start() {
            if (!isShowing() && parentView.getWindowToken() != null) {
                setBackgroundDrawable(new ColorDrawable(0));
                showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0);
            }
        }

        public void stickOnStart() {
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!isShowing() && parentView.getWindowToken() != null) {
                        setBackgroundDrawable(new ColorDrawable(0));
                        showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0);
                        return;
                    }
                    if (!isShowing()) {
                        handler.post(this);
                    }
                }
            });
        }

        /**
         * Close the keyboard height provider,
         * this provider will not be used anymore.
         */
        public void close() {
            dismiss();
        }

        /**
         * Popup window itself is as big as the window of the Activity.
         * The keyboard can then be calculated by extracting the popup view bottom
         * from the activity window height.
         */
        private void handleOnGlobalLayout() {
            if (layout.popupView == null || layout.popupView.getVisibility() == GONE) return;
            final int keyboardHeight = layout.getCurrentKeyboardHeight();
            layout.popupView.updateKeyboardState(keyboardHeight);
            if (layout.changeHeightWithKeyboard && layout.keyboardModeActive) {
                int reservedKeyboardHeight = layout.popupView.shouldReserveKeyboardHeight() ? keyboardHeight : 0;
                layout.setKeyboardPlaceholderHeight(reservedKeyboardHeight);
            }
        }
    }

    private void setKeyboardPlaceholderHeight(int height) {
        if (keyboard == null || keyboard.getLayoutParams() == null) return;
        if (keyboard.getLayoutParams().height == height) return;
        keyboard.getLayoutParams().height = height;
        keyboard.requestLayout();
        requestLayout();
    }

    private final Runnable keyboardMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (!keyboardMonitorRunning || popupView == null || popupView.isShowing()) {
                stopKeyboardMonitor();
                return;
            }

            int keyboardHeight = getCurrentKeyboardHeight();
            if (keyboardHeight > Utils.dpToPx(getContext(), AXEmojiPopupView.MIN_KEYBOARD_HEIGHT)) {
                keyboardSeenByMonitor = true;
                popupView.updateKeyboardStateOpened(keyboardHeight);
                if (changeHeightWithKeyboard) {
                    setKeyboardPlaceholderHeight(keyboardHeight);
                }
                keyboardMonitorHandler.postDelayed(this, 120);
                return;
            }

            if (keyboardSeenByMonitor || System.currentTimeMillis() - keyboardMonitorStartTime > 1200) {
                popupView.updateKeyboardStateClosed();
                leaveKeyboardMode();
                return;
            }

            keyboardMonitorHandler.postDelayed(this, 80);
        }
    };

    private void startKeyboardMonitor() {
        stopKeyboardMonitor();
        keyboardMonitorRunning = true;
        keyboardSeenByMonitor = false;
        keyboardMonitorStartTime = System.currentTimeMillis();
        keyboardMonitorHandler.post(keyboardMonitorRunnable);
    }

    private void stopKeyboardMonitor() {
        keyboardMonitorRunning = false;
        keyboardSeenByMonitor = false;
        keyboardMonitorHandler.removeCallbacks(keyboardMonitorRunnable);
    }

    private void leaveKeyboardMode() {
        keyboardModeActive = false;
        stopKeyboardMonitor();
        setKeyboardPlaceholderHeight(0);
    }

    private void restoreKeyboardModeIfNeeded() {
        if (!isInputKeyboardOwner()) {
            return;
        }

        int keyboardHeight = getCurrentKeyboardHeight();
        if (keyboardHeight > Utils.dpToPx(getContext(), AXEmojiPopupView.MIN_KEYBOARD_HEIGHT)) {
            keyboardModeActive = true;
            keyboardSeenByMonitor = true;
            popupView.updateKeyboardStateOpened(keyboardHeight);
            if (changeHeightWithKeyboard) {
                setKeyboardPlaceholderHeight(keyboardHeight);
            }
            if (!keyboardMonitorRunning) {
                startKeyboardMonitor();
            }
        } else if (keyboardModeActive) {
            popupView.updateKeyboardStateClosed();
            leaveKeyboardMode();
        }
    }

    private boolean isInputKeyboardOwner() {
        return popupView != null && !popupView.isShowing() && popupView.editText.hasFocus();
    }

    private int getCurrentKeyboardHeight() {
        if (popupView == null) return 0;

        Boolean imeVisible = isImeVisible();
        if (Boolean.FALSE.equals(imeVisible)) {
            return 0;
        }

        return Utils.getInputMethodHeight(popupView.getContext(), popupView.rootView);
    }

    private Boolean isImeVisible() {
        if (Build.VERSION.SDK_INT < 30) {
            return null;
        }

        WindowInsets insets = getRootWindowInsets();
        if (insets == null && popupView != null) {
            insets = popupView.rootView.getRootWindowInsets();
        }
        if (insets == null) {
            return null;
        }

        return insets.isVisible(WindowInsets.Type.ime());
    }
}
