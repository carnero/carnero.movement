package carnero.movement.common.graph;

import android.content.res.Resources;

import carnero.movement.R;

public class StepsPath extends SplinePath {

    @Override
    public void init(Resources resources) {
        mFillPath = false;
        mFillColor1Res = R.color.none;
        mFillColor2Res = R.color.none;
        mFillGradient = false;
        mStrokeWidthRes = R.dimen.graph_stroke;
        mStrokeColor1Res = R.color.graph_steps_outline;
        mStrokeColor2Res = R.color.none;
        mStrokeGradient = false;
        mPathWidthRes = R.dimen.graph_path;
        mShowPoints = false;
        mPointColorRes = R.color.none;
        mPointSizeRes = R.dimen.graph_point;
        mPointPaddingRes = R.dimen.graph_point_padding;

        super.init(resources);
    }
}
