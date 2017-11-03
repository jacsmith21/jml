package ca.jacob.cs6735.ensemble;

import ca.jacob.cs6735.Algorithm;
import ca.jacob.cs6735.Model;
import ca.jacob.cs6735.util.Matrix;
import ca.jacob.cs6735.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ca.jacob.cs6735.util.ML.error;
import static ca.jacob.cs6735.util.ML.generateIndices;
import static ca.jacob.cs6735.util.Math.*;
import static java.lang.Math.sqrt;


public class Adaboost implements Algorithm {
    private static final Logger LOG = LoggerFactory.getLogger(Adaboost.class);

    private Algorithm algorithm;
    private int numberOfEstimators;

    public Adaboost(Algorithm algorithm, int numberOfEstimators) {
        this.algorithm = algorithm;
        this.numberOfEstimators = numberOfEstimators;
    }

    public Model fit(Matrix x, Vector y) {
        AdaboostModel model = new AdaboostModel();

        Vector weights = new Vector(new double[x.rowCount()]);
        weights.fill(1./x.rowCount());
        LOG.debug("init weights: {}", weights);

        for(int i = 0; i < numberOfEstimators; i++) {
            LOG.debug("starting iteration {}", i+1);

            Vector indices = generateIndices(weights);
            Matrix xWeighted = x.rows(indices);
            Vector yWeighted = y.at(indices);

            Model classifier = algorithm.fit(xWeighted, yWeighted);
            Vector h = classifier.predict(x);
            LOG.debug("actual: {}, predict: {}", y.subVector(0, 5), h.subVector(0, 5));

            Vector err = error(h, y);

            double epsilon = weights.dot(err); // sum of the weights of misclassified
            double alpha = (1/2)*ln((1-epsilon)/epsilon);
            double Z = weights.sum(); //regularization factor

            weights = weights.mul(exp(y.mul(h).mul(-alpha)).div(Z));
            LOG.debug("weights on iter {}: {}", i+1, weights.subVector(0, 5));

            LOG.debug("alpha: {}", alpha);
            model.add(classifier, alpha);
        }

        return model;
    }
}
