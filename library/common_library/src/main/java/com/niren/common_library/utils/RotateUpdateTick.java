package com.niren.common_library.utils;

import android.content.Context;
import android.view.Choreographer;

import com.haihuman.nativelib.HaiSDK;

import java.util.Objects;

public class RotateUpdateTick implements Choreographer.FrameCallback {
    private final Choreographer _choreographer;
    private final int avatarInstID;
    private Context context;
    float speedUpDuration = 1f, keepDuration = 0f, dampDuration = 1.5f;//加速 保持 阻尼
    float finalSpeed = 0;
    float v0 = 0;//初速度
    int state = 0;
    float timeCost = 0;
    float currentSpeed = 0;
    float waitDuration = 0;
    float finalRotate = 0;
    float currentRotate = 0;
    float returnBackDuration = 1;

    public RotateUpdateTick(int avatarInstID, Context context) {
        _choreographer = Choreographer.getInstance();
        _choreographer.postFrameCallback(this);
        this.avatarInstID = avatarInstID;
        this.context = context;
    }

    public void setSpeed(float finalSpeed, float startSpeed) {
        this.finalSpeed = finalSpeed;
        this.v0 = startSpeed;
        this.currentSpeed = v0;
    }

    public void setReturnBackState() {
        timeCost = 0;
        state = 5;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        _choreographer.postFrameCallback(this);
        if (state == 3) {
            setReturnBackState();
        }
        if (state == 7) {
            return;
        }
        if (state == 0) {//加速
            timeCost = timeCost + 0.015f;
            currentSpeed = v0 + (finalSpeed - v0) * (timeCost / speedUpDuration);
            HaiSDK.AvatarRotateBy(avatarInstID, currentSpeed * 0.015f);
            if (timeCost >= speedUpDuration) {
                currentSpeed = finalSpeed;
                timeCost = 0;
                state = 1;
            }
        }
        if (state == 1) {//保持
            timeCost = timeCost + 0.015f;
            HaiSDK.AvatarRotateBy(avatarInstID, keepDuration * 0.015f);
            if (timeCost >= keepDuration) {
                timeCost = 0;
                state = 2;
            }
        }
        if (state == 2) {//阻尼
            timeCost = timeCost + 0.015f;
            if (timeCost >= dampDuration) {
                timeCost = dampDuration;
            }
            float speed = (1 - timeCost / dampDuration) * currentSpeed;
            HaiSDK.AvatarRotateBy(avatarInstID, speed * 0.015f);
            if (Math.abs(timeCost - dampDuration) < 0.00001f) {
                timeCost = 0;
                state = 3;
            }
        }
        if (state == 5) {//等待
            timeCost = timeCost + 0.015f;
            if (timeCost >= waitDuration) {
                currentRotate = Objects.requireNonNull(HaiSDK.AvatarGetTransform(avatarInstID))[1];
                timeCost = 0;
                finalRotate = (float) (Math.round(Math.abs(currentRotate / 360)) * 360) * (currentRotate > 0 ? 1 : -1);
                state = 6;
            }
        }
        if (state == 6) {//归位
            timeCost = timeCost + 0.05f;
//                LogUtil.trace("returnBackDuration:"+returnBackDuration+" currentRotate:"+currentRotate+" finalRotate:"+finalRotate+" timeCost:"+timeCost);
            if (timeCost >= returnBackDuration) {
                timeCost = returnBackDuration;
            }
            float rotate = (1 - timeCost / returnBackDuration) * currentRotate + timeCost / returnBackDuration * finalRotate;
            HaiSDK.AvatarRotateTo(avatarInstID, rotate);
            if (Math.abs(timeCost - returnBackDuration) < 0.00001f) {
                state = 7;
            }
        }
    }
}
