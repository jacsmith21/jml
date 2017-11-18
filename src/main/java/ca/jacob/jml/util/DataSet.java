package ca.jacob.jml.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DataSet {
    private static final Logger LOG = LoggerFactory.getLogger(DataSet.class);
    public static final int DISCRETE = 0;
    public static final int CONTINUOUS = 1;

    private String name;
    private Matrix x;
    private Vector y;
    private final Vector attributeTypes;

    public DataSet(Matrix x, Vector y, Vector attributeTypes) {
        if(x.colCount() != attributeTypes.length()) {
            LOG.error("length mismatch: attributes: {}, attribute types: {}", x.colCount(), attributeTypes.length());
            throw new DataException("attribute type vector length must match attribute count");
        }

        if(x.rowCount() != y.length()) {
            throw new DataException("x row count and y length must match!");
        }

        this.x = x;
        this.y = y;
        this.attributeTypes = attributeTypes;
    }

    public DataSet(Matrix data, Vector attributeTypes) {
        if(data.colCount()-1 != attributeTypes.length()) {
            LOG.error("length mismatch: attributes: {}, attribute types: {}", data.colCount()-1, attributeTypes.length());
            throw new DataException("attribute type vector length must match attribute count");
        }

        this.x = data;
        this.y = x.col(x.colCount()-1);
        x.dropCol(x.colCount()-1);
        this.attributeTypes = attributeTypes;
    }

    public DataSet(Matrix data, int attributeType) {
        Vector dataTypes = new Vector(new int[data.colCount()-1]);
        dataTypes.fill(attributeType);
        this.attributeTypes = dataTypes;

        this.x = data;
        this.y = x.col(x.colCount()-1);
        this.x.dropCol(x.colCount()-1);
    }

    public DataSet(Matrix x, Vector y, int attributeType) {
        Vector dataTypes = new Vector(new int[x.colCount()]);
        dataTypes.fill(attributeType);
        this.attributeTypes = dataTypes;

        this.x = x;
        this.y = y;
    }

    public DataSet(Vector attributeTypes) {
        this.attributeTypes = attributeTypes;
        this.x = new Matrix();
        this.y = new Vector();
    }

    public int sampleCount() {
        return x.rowCount();
    }

    public Map<Integer, DataSet> splitByClass() {
        Map<Integer, DataSet> separated = new HashMap<Integer, DataSet>();

        for (int i = 0; i < x.rowCount(); i++) {
            LOG.trace("checking row {}", i);
            int value = y.intAt(i);

            DataSet d = separated.get(value);
            if (d == null) {
                LOG.trace("adding new split based on value {}", value);
                d = new DataSet(this.attributeTypes);
                separated.put(value, d);
            }

            Vector v = this.sample(i);
            d.add(v);
        }

        return separated;
    }

    public Map<Integer, DataSet> splitByAttribute(int j) {
        Map<Integer, DataSet> separated = new HashMap<Integer, DataSet>();

        if(this.attributeType(j) == DISCRETE) {
            for (int i = 0; i < x.rowCount(); i++) {
                LOG.trace("checking row {}", i);
                int value = x.intAt(i, j);

                DataSet d = separated.get(value);
                if (d == null) {
                    LOG.trace("adding new split based on value {}", value);
                    d = new DataSet(this.attributeTypes);
                    separated.put(value, d);
                }

                Vector v = this.sample(i);
                d.add(v);
            }
        } else if(this.attributeType(j) == CONTINUOUS) {
            for (int i = 0; i < x.rowCount(); i++) {
                LOG.trace("checking row {}", i);
                Vector col = x.col(j);
                double median = col.median();
            }
        } else {
            throw new DataException("unsupported data type");
        }


        return separated;
    }

    public void add(Vector sample) {
        LOG.debug("adding sample: {} to y: {} and x: {}", sample, y, x);
        y.add(sample.at(sample.length()-1));
        sample.remove(sample.length()-1);
        x.pushRow(sample.clone());
    }

    public Vector sample(int i) {
        Vector sample = x.row(i);
        sample.add(y.at(i));
        LOG.debug("sample is: {}", sample);
        return sample;
    }

    public double at(int i, int j) {
        return x.at(i, j);
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

    public DataSet samples(Vector indices) {
        return new DataSet(x.rows(indices), y.at(indices), attributeTypes);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setY(Vector y) {
        this.y = y;
    }
}
