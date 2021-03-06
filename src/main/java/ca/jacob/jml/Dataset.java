package ca.jacob.jml;

import ca.jacob.jml.math.Matrix;
import ca.jacob.jml.math.Vector;
import ca.jacob.jml.exceptions.AttributeException;
import ca.jacob.jml.exceptions.DataException;
import ca.jacob.jml.math.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ca.jacob.jml.Util.calculateWeightedEntropy;
import static ca.jacob.jml.Util.calculateOccurrences;
import static ca.jacob.jml.math.Util.log2;

public class Dataset {
    private static final Logger LOG = LoggerFactory.getLogger(Dataset.class);
    public static final int DISCRETE = 0;
    public static final int CONTINUOUS = 1;

    private String name;
    private double entropy;
    private Matrix x;
    private Vector attributeTypes;
    private Vector y;

    public Dataset(Matrix x, Vector y, Vector attributeTypes) {
        this.init(x, y, attributeTypes);
    }

    public Dataset(Matrix x, Vector attributeTypes) {
        Vector y = x.col(x.colCount()-1);
        x.dropCol(x.colCount()-1);
        this.init(x, y, attributeTypes);
    }

    public Dataset(Matrix x, int attributeType) {
        Vector y = x.col(x.colCount()-1);
        x.dropCol(x.colCount()-1);
        this.init(x, y, attributeType);

    }

    public Dataset(Matrix x, Vector y, int attributeType) {
        this.init(x, y, attributeType);
    }

    public Dataset(Vector attributeTypes) {
        this.init(new Matrix(), new Vector(), attributeTypes);
    }

    private void init(Matrix x, Vector y, int attributeType) {
        Vector dataTypes = new Vector(new int[x.colCount()]);
        dataTypes.fill(attributeType);
        this.attributeTypes = dataTypes;
        this.init(x, y, attributeTypes);
    }

    private void init(Matrix x, Vector y, Vector attributeTypes) {
        if(x.colCount() != 0 && x.colCount() != attributeTypes.length()) {
            LOG.error("length mismatch: attributes: {}, attribute types: {}", x.colCount(), attributeTypes.length());
            throw new DataException("attribute type vector length must match attribute count");
        }

        if(x.rowCount() != y.length()) {
            throw new DataException("x row count and y length must match!");
        }

        this.entropy = -1;
        this.x = x;
        this.y = y;
        this.attributeTypes = attributeTypes;
    }

    public int sampleCount() {
        return x.rowCount();
    }

    public Map<Integer, Dataset> splitByClass() {
        Map<Integer, Dataset> separated = new HashMap<Integer, Dataset>();

        for (int i = 0; i < x.rowCount(); i++) {
            LOG.trace("checking row {}", i);
            int value = y.intAt(i);

            Dataset subset = separated.get(value);
            if (subset == null) {
                LOG.trace("adding new split based on value {}", value);
                subset = new Dataset(this.attributeTypes);
                separated.put(value, subset);
            }

            Vector v = this.sample(i);
            subset.add(v);
        }

        return separated;
    }

    public Tuple<List<Integer>, List<Dataset>> splitByDiscreteAttribute(int attribute) {
        if(this.attributeType(attribute) != DISCRETE) {
            throw new AttributeException("must be discrete attribute");
        }

        List<Integer> values = new ArrayList<>();
        List<Dataset> subsets = new ArrayList<>();
        for (int i = 0; i < x.rowCount(); i++) {
            LOG.trace("checking row {}", i);
            int value = x.intAt(i, attribute);

            int index = values.indexOf(value);
            if (index < 0) {
                LOG.trace("adding new split based on value {}", value);
                values.add(value);
                subsets.add(new Dataset(attributeTypes.clone()));
                index = subsets.size()-1;
            }
            Dataset subset = subsets.get(index);

            Vector v = this.sample(i);
            subset.add(v);
        }
        return new Tuple<>(values, subsets);
    }

    public Tuple<Double, Tuple<Dataset, Dataset>> splitByContinuousAttribute(int attribute) {
        if(this.attributeType(attribute) != CONTINUOUS) {
            throw new DataException("must be continuous attribute");
        }

        Vector c = x.col(attribute);
        c.sort();

        LOG.debug("splitting with attribute -> {}", c);

        Tuple<Double, Tuple<Dataset, Dataset>> bestSubsets = null;
        double minimumEntropy = 0;
        for (int i = 0; i < c.length()-1; i++) {
            if(c.at(i) == c.at(i+1)) {
                continue;
            }

            double pivot = (c.at(i) + c.at(i+1)) / 2;
            Tuple<Dataset, Dataset> subsets = splitAt(attribute, pivot);

            double entropy = calculateWeightedEntropy(subsets);
            if(bestSubsets == null || entropy < minimumEntropy) {
                bestSubsets = new Tuple<>(pivot, subsets);
                minimumEntropy = entropy;
            }
        }
        return bestSubsets;
    }

    public Tuple<Dataset, Dataset> splitAt(int attribute, double pivot) {
        if(this.attributeType(attribute) != CONTINUOUS) {
            throw new DataException("splitAt must use a continuous attribute");
        }

        Dataset under = new Dataset(attributeTypes.clone());
        Dataset over = new Dataset(attributeTypes.clone());
        for (int i = 0; i < x.rowCount(); i++) {
            double value = x.at(i, attribute);

            if(value < pivot) {
                under.add(this.sample(i));
            } else if(value > pivot) {
                over.add(this.sample(i));
            } else {
                throw new DataException("given value must not match value from attribute");
            }
        }

        return new Tuple<>(under, over);
    }

    public void add(Vector sample) {
        LOG.trace("adding sample: {} to y: {} and x: {}", sample, y, x);
        y.add(sample.at(sample.length()-1));
        sample.remove(sample.length()-1);
        x.pushRow(sample.clone());
    }

    public Vector sample(int i) {
        Vector sample = x.row(i);
        sample.add(y.at(i));
        LOG.trace("sample is: {}", sample);
        return sample;
    }

    public int attributeCount() {
        return x.colCount();
    }

    public Vector attribute(int j) {
        return x.col(j).clone();
    }

    public Vector classes() {
        return y.clone();
    }

    public int attributeType(int j) {
        return attributeTypes.intAt(j);
    }

    public Dataset samples(Vector indices) {
        return new Dataset(x.rows(indices), y.at(indices), attributeTypes.clone());
    }

    public Matrix getX() {
        return x;
    }

    public Vector getY() {
        return y;
    }

    public Vector getAttributeTypes() {
        return attributeTypes.clone();
    }

    @Override
    public String toString() {
        return this.name + "["+this.sampleCount()+" x "+this.attributeCount()+"]";
    }

    public String dataToString() {
        String toReturn = "\n";
        for(int i = 0; i < x.rowCount(); i++) {
            toReturn += x.row(i) + " -> " + y.at(i) + "\n";
        }
        return toReturn;
    }

    public int classValue(int i) {
        return y.intAt(i);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setY(Vector y) {
        this.y = y;
    }

    public void dropAttribute(int attribute) {
        x.dropCol(attribute);
        attributeTypes.remove(attribute);
    }

    public double entropy() {
        // Have we already calculated entropy?
        if(entropy > 0) {
            return entropy;
        }

        Map<Integer, Integer> classes = calculateOccurrences(this.classes());
        LOG.trace("there are {} different class", classes.size());
        LOG.debug("classes: {}", classes);

        double sum = 0.;
        for (int count : classes.values()) {
            sum += count;
        }
        LOG.trace("sum is " + sum);

        entropy = 0;
        for (int count : classes.values()) {
            entropy -= count / sum * log2(count / sum);
        }

        return entropy;
    }

    public void replaceClasses(Vector newClasses) {
        this.y = newClasses;
    }
}
