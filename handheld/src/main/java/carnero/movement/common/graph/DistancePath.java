package carnero.movement.common.graph;

import android.content.res.Resources;

import carnero.movement.R;

public class DistancePath extends SplinePath {

    @Override
    public void init(Resources resources) {
        mFillPath = true;
        mFillColor1Res = R.color.graph_distance_fill_start;
        mFillColor2Res = R.color.graph_distance_fill_end;
        mFillGradient = GRADIENT_HORIZONTAL;
        mStrokeWidthRes = R.dimen.graph_stroke;
        mStrokeColor1Res = R.color.graph_distance_outline_start;
        mStrokeColor2Res = R.color.graph_distance_outline_end;
        mStrokeGradient = true;
        mPathWidthRes = R.dimen.graph_path;
        mShowPoints = true;
        mPointColorRes = R.color.graph_distance_point;
        mPointSizeRes = R.dimen.graph_point;
        mPointPaddingRes = R.dimen.graph_point_padding;

        super.init(resources);
    }
}
