package uk.ac.susx.tag.classificationframework.datastructures;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;

/**
 * Created by thomas on 1/30/14.
 *
 * Apache Commons Type expressing the Feature Marginals Constraint, see section 3 of http://www.aclweb.org/anthology/P/P13/P13-1034.pdf
 * (last equation in left column)
 *
 */
public class FeatureMarginalsConstraint implements UnivariateDifferentiableFunction
{
    private double nWPos;
    private double nNotWPos;
    private double nWNeg;
    private double nNotWNeg;
    private double k;
    private double l;

    public FeatureMarginalsConstraint(double nWPos, double nNotWPos, double nWNeg, double nNotWNeg, double k, double l)
    {
        this.nWPos      = nWPos;
        this.nWNeg      = nWNeg;
        this.nNotWPos   = nNotWPos;
        this.nNotWNeg   = nNotWNeg;
        this.k          = k;
        this.l          = l;
    }

    @Override
    public DerivativeStructure value(DerivativeStructure derivativeStructure) throws DimensionMismatchException {

        DerivativeStructure t1 = derivativeStructure.reciprocal().multiply(this.nWPos);
        DerivativeStructure t2 = derivativeStructure.subtract(1).reciprocal().multiply(this.nNotWPos);
        DerivativeStructure t3 = derivativeStructure.multiply(this.l).subtract(this.k).reciprocal().multiply(this.l).multiply(this.nWNeg);
        DerivativeStructure t4 = derivativeStructure.multiply(this.l).subtract(this.k).add(1).reciprocal().multiply(this.l).multiply(this.nNotWNeg);

        return t1.add(t2).add(t3).add(t4);
    }

    @Override
    public double value(double thetaWPos)
    {
        return  (this.nWPos / thetaWPos)                                        +
                (this.nNotWPos / (thetaWPos - 1))                               +
                ((this.l * this.nWNeg) / (this.l * thetaWPos - this.k))         +
                ((this.l * this.nNotWNeg) / (this.l * thetaWPos - this.k + 1));
    }
}