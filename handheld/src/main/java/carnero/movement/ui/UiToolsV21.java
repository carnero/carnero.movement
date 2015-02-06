package carnero.movement.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.graphics.Path;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.PathInterpolator;

public class UiToolsV21 {

    public static UiToolsV21 getInstance() {
        if (Build.VERSION.SDK_INT >= 20) {
            return new UiToolsV21();
        }

        return null;
    }

    @TargetApi(21)
    public void setElevation(ActionBar actionBar, int dimension) {
        actionBar.setElevation(dimension);
    }

    @TargetApi(21)
    public void setHideAchievementsAnimator(
        View icon, int position, MainActivity.EndAnimatorListener listener
    ) {
        final PathInterpolator interpolator = new PathInterpolator(0.0f, 0.7f, 0.9f, 1.0f);
        final Path path = new Path();
        path.moveTo(1, 1);
        path.lineTo(0, 0);

        final ObjectAnimator animator = ObjectAnimator.ofFloat(
            icon,
            View.SCALE_X,
            View.SCALE_Y,
            path
        );
        animator.addListener(listener);
        animator.setInterpolator(interpolator);
        animator.setStartDelay((position + 1) * 75);
        animator.start();
    }

    @TargetApi(21)
    public void setDisplayAchievementsAnimator(
        View icon, int total, int position, MainActivity.StartAnimatorListener listener
    ) {
        final PathInterpolator interpolator = new PathInterpolator(0.0f, 0.2f, 0.4f, 1.0f);
        final Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(1, 1);

        final ObjectAnimator animator = ObjectAnimator.ofFloat(
            icon,
            View.SCALE_X,
            View.SCALE_Y,
            path
        );
        animator.addListener(listener);
        animator.setInterpolator(interpolator);
        animator.setStartDelay((total - position) * 75);
        animator.start();
    }

    @TargetApi(21)
    public Animator getCircularAnimator(View view, int cx, int cy, int startRadius, int finalRadius) {
        return ViewAnimationUtils.createCircularReveal(
            view,
            cx,
            cy,
            startRadius,
            finalRadius
        );
    }
}
