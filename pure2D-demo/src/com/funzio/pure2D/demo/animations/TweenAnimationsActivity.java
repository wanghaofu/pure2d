package com.funzio.pure2D.demo.animations;

import com.funzio.pure2D.animators.MoveAnimator;
import com.funzio.pure2D.animators.TweenAnimator;

public class TweenAnimationsActivity extends AnimationActivity {

    @Override
    protected TweenAnimator createAnimator() {
        final MoveAnimator animator = new MoveAnimator(null);
        animator.setDuration(1000);
        animator.start(mDisplaySizeDiv2.x, 0, mDisplaySizeDiv2.x, mDisplaySize.y - OBJ_SIZE / 2);

        return animator;
    };

}
