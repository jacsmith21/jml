package ca.jacob.jml;

import ca.jacob.jml.math.Tuple;
import ca.jacob.jml.math.Vector;

import java.util.ArrayList;
import java.util.List;

import static ca.jacob.jml.Util.range;
import static ca.jacob.jml.Util.shuffle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ca.jacob.jml.Util.shuffle;

public class KFold {
    private static final Logger LOG = LoggerFactory.getLogger(KFold.class);

    private int numberOfSplits;
    private Long seed;

    public KFold(int numberOfSplits) {
        this.numberOfSplits = numberOfSplits;
    }

    public KFold(int numberOfSplits, Long seed) {
        this.numberOfSplits = numberOfSplits;
        this.seed = seed;
    }

    public Report generateReport(Algorithm a, DataSet dataSet) {
        LOG.debug("generate report starting");
        LOG.debug("dataset types: {}", dataSet.getAttributeTypes());

        Vector accuracies = new Vector();
        List<Tuple<Vector, Vector>> indices = this.generateIndices(dataSet);
        for(Tuple<Vector, Vector> tuple : indices) {
            LOG.info("starting kfold iteration {}", accuracies.length());
            Vector trainIndices = tuple.first();
            Vector testIndices = tuple.last();

            DataSet trainingDataSet = dataSet.samples(trainIndices);
            LOG.debug("training dataset types: {}", trainingDataSet.getAttributeTypes());
            Model m = a.fit(trainingDataSet);

            DataSet testDataSet = dataSet.samples(testIndices);

            double accuracy = m.accuracy(testDataSet);
            accuracies.add(accuracy);
        }
        return new Report(accuracies);
    }

    public List<Tuple<Vector, Vector>> generateIndices(DataSet dataSet) {
        int numberOfSamples = dataSet.sampleCount();
        int splitLength = numberOfSamples / numberOfSplits;

        // List of indices ex. 1, 2, 3 ... n-1, n
        Vector indices = range(0, numberOfSamples);
        shuffle(indices, seed); // shuffle that list

        List<Tuple<Vector, Vector>> trainTestIndices = new ArrayList<>();
        for(int split = 0; split < numberOfSplits-1; split++) {
            Vector trainIndices = new Vector();
            Vector testIndices = new Vector();

            // get test range
            int from = split*splitLength;
            int to = (split+1)*splitLength;
            LOG.debug("split from {} to {}", from, to);

            // add shuffled indices
            testIndices.concat(indices.subVector(from, to));
            trainIndices.concat(indices.subVector(0, from));
            trainIndices.concat(indices.subVector(to, numberOfSamples));

            trainTestIndices.add(new Tuple<>(trainIndices, testIndices));
        }

        // adding remaining elements
        Vector trainIndices = new Vector();
        Vector testIndices = new Vector();

        int from = (numberOfSplits-1)*splitLength;
        int to = numberOfSamples;
        LOG.debug("last split from {} to {}", from, to);

        testIndices.concat(indices.subVector(from, to));
        trainIndices.concat(indices.subVector(0, from));
        trainIndices.concat(indices.subVector(to, numberOfSamples));

        trainTestIndices.add(new Tuple<>(trainIndices, testIndices));

        return trainTestIndices;
    }
}
