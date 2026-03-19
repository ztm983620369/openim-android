package io.openim.android.ouicore.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class RecordWaveView extends View {
    private final Paint paint = new Paint();
    private int count = 0;
    private float lineHeight = 0f;
    private float start = 0f;
    private float ideaHeight = 0f;
    private float density = 0f;
    private double degrees = 3.0;
    private int currentIndex = -1;
    private float amplitude = 0f;
    private double dOffset = 0.002;
    private WaveValueListener listener;
    private float preValue = 0f;
    private float preMaxValue = 0f;
    private float offsetLine = 0f;

    public RecordWaveView(Context context) {
        this(context, null);
    }

    public RecordWaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#74B3FF"));

        density = getResources().getDisplayMetrics().density;
        ideaHeight = density * 3;
        lineHeight = density * 3;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        count = (int) Math.floor(w / (density * 3));
        if (count % 2 != 1) {
            count -= 1;
        }
        start = (w - count * lineHeight) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < count; i++) {
            double dy = Math.abs(Math.sin(Math.toDegrees(degrees + start + lineHeight * i)));
            int f = (i + 1) % 9;
            float dt = f <= 5 ? f * amplitude : (10 - f) * amplitude;
            float lineWave;
            if (currentIndex != -1) {
                if (i == currentIndex) {
                    lineWave = ideaHeight * 0.8f;
                } else if (currentIndex - 1 == i || currentIndex + 1 == i) {
                    lineWave = ideaHeight * 0.6f;
                } else if (currentIndex - 2 == i || currentIndex + 2 == i) {
                    lineWave = ideaHeight * 0.4f;
                } else {
                    lineWave = 0f;
                }
            } else {
                lineWave = 0f;
            }

            canvas.drawRoundRect(
                start + lineHeight * i,
                (float) (getMeasuredHeight() / 2f - density * 4 - dy * dt - lineWave),
                start + lineHeight * i + density * 2,
                (float) (getMeasuredHeight() / 2f + density * 4 + dy * dt + lineWave),
                density,
                density,
                paint
            );
        }
        updateWave();
    }

    private int getAmplitude() {
        return listener != null ? listener.getValue() : 0;
    }

    public void setAmpListener(WaveValueListener listener) {
        this.listener = listener;
    }

    private void updateWave() {
        int current = getAmplitude();
        if (current > 7) {
            current = 7;
        }

        if (preMaxValue != 0f && current != 0) {
            if (preMaxValue < current) {
                preMaxValue = current;
            }
        } else if (preMaxValue == 0f && current != 0 && preValue < current) {
            preMaxValue = current;
        }

        if (preValue >= preMaxValue) {
            preMaxValue = 0f;
        }

        if (preMaxValue == 0f) {
            if (preValue != 0f && current == 0) {
                preValue -= 0.1f;
                if (preValue < 0) {
                    preValue = 0f;
                }
            } else if (current < preValue) {
                preValue -= 0.1f;
            } else {
                preValue = current;
            }
        } else {
            if (preValue < preMaxValue && preMaxValue != 0f) {
                preValue += 0.5f;
            }
            if (preValue > 7f) {
                preValue = 7f;
            }
        }

        amplitude = preValue;
        if (preValue == 0f && current == 0 && preMaxValue == 0f) {
            offsetLine += 0.4f;
            currentIndex = (int) offsetLine;
            if (currentIndex > count) {
                offsetLine = 0f;
                currentIndex = 0;
            }
            amplitude = 0f;
        } else {
            offsetLine = 0f;
            currentIndex = -1;
        }

        degrees += dOffset;
        postInvalidateOnAnimation();
    }

    public interface WaveValueListener {
        int getValue();
    }
}
