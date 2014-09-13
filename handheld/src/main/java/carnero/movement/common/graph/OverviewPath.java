package carnero.movement.common.graph;

import android.content.res.Resources;

import carnero.movement.R;

public class OverviewPath extends SplinePath {

    @Override
    public void init(Resources resources) {
        mFillPath = false;
        mFillColor1Res = R.color.none;
        mFillColor2Res = R.color.none;
        mFillGradient = GRADIENT_NONE;
        mStrokeWidthRes = R.dimen.graph_stroke;
        mStrokeColor1Res = R.color.graph_actionbar_outline_start;
        mStrokeColor2Res = R.color.graph_actionbar_outline_end;
        mStrokeGradient = true;
        mPathWidthRes = R.dimen.graph_path;
        mShowPoints = false;
        mPointColorRes = R.color.none;
        mPointSizeRes = R.dimen.graph_point;
        mPointPaddingRes = R.dimen.graph_point_padding;

        super.init(resources);
    }
}
