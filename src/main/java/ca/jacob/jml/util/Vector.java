package ca.jacob.jml.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static ca.jacob.jml.util.ML.arrayAsList;
import static ca.jacob.jml.util.ML.toPrimitiveArray;
import static ca.jacob.jml.util.Math.calculateOccurrences;
import static java.lang.Math.sqrt;

public class Vector implements Iterable<Double> {
    private static final Logger LOG = LoggerFactory.getLogger(Vector.class);

    private List<Double> data;

    public Vector(int[] data) {
        this.data = new ArrayList<Double>(Arrays.asList(new Double[data.length]));
        for(int i = 0; i < data.length; i++) {
            this.data.set(i, (double)data[i]);
        }
    }

    public Vector() {
        this.data = new ArrayList<Double>();
    }

    public Vector(double[] data) {
        this.data = arrayAsList(data);
    }

    public Vector(String[] data) {
        this.data = new ArrayList<Double>();
            for(int i = 0; i < data.length; i++) {
                try {
                    this.data.add(Double.parseDouble(data[i]));
                } catch (NumberFormatException e) {
                    if(!data[i].equals("?")) {
                        throw new DataException("data must all be integers, doubles or ?, not " + data[i]);
                    }
                    this.data.add(Double.NaN);
                }
            }
    }

    public Vector(List<Double> data) {
        this.data = data;
    }

    public void add(int value) {
        data.add((double)value);
    }

    public void add(double value) {
        data.add(value);
    }

    public void concat(Vector v) {
        this.data.addAll(v.getData());
    }

    public Vector subVector(int from, int to) {
        return new Vector(this.data.subList(from, to));
    }

    public void remove(int i) {
        data.remove(i);
    }

    public double[] toArray() {
        return toPrimitiveArray(data);
    }

    public int[] tointArray() {
        int[] arr = new int[this.length()];
        for(int i = 0; i < this.length(); i++) {
            arr[i] = this.intAt(i);
        }
        return arr;
    }

    public double at(int i) {
        return data.get(i);
    }

    public Vector at(Vector indices) {
        Vector v = new Vector(new double[indices.length()]);
        for(int i = 0; i < indices.length(); i++) {
            LOG.debug("setting index {} to {}", i, this.data.get(v.intAt(i)));
            v.set(i, this.data.get(indices.intAt(i)));
        }
        return v;
    }

    public int valueOfMaxOccurrence() {
        Map<Integer, Integer> occurrences = calculateOccurrences(this);
        LOG.debug("occurrences: {}", occurrences);
        boolean first = true;
        int valueOfMaxOccurrence = 0;
        for (Map.Entry<Integer, Integer> e : occurrences.entrySet()) {
            if(first) {
                first = false;
                valueOfMaxOccurrence = e.getKey();
            } else if (e.getValue() > occurrences.get(valueOfMaxOccurrence)) {
                valueOfMaxOccurrence = e.getKey();
            }
        }
        return valueOfMaxOccurrence;
    }

    public void fill(double value) {
        Collections.fill(data, value);
    }

    public double dot(int[] other) {
        return dot(new Vector(other));
    }

    public double dot(Vector other) {
        if(this.length() != other.length()) {
            throw new MathException("vector lengths must match");
        }

        double sum = 0.;
        for(int i = 0; i < this.length(); i++) {
            sum += this.at(i) * other.at(i);
        }

        return sum;
    }

    public double sum() {
        double sum = 0.;
        for(int i = 0; i < this.length(); i++) {
            sum += this.at(i);
        }
        return sum;
    }

    public int length() {
        return data.size();
    }

    public Vector mul(double value) {
        Vector v = new Vector(new double[this.length()]);
        for(int i = 0; i < length(); i++) {
            v.set(i, this.at(i) * value);
        }
        return v;
    }

    public Vector mul(Vector other) {
        if(this.length() != other.length()) {
            throw new MathException("vector lengths must match");
        }

        Vector v = new Vector(new double[this.length()]);
        for(int i = 0; i < this.length(); i++) {
            v.set(i, this.at(i) * other.at(i));
        }
        return v;
    }

    public Vector div(double value) {
        Vector v = new Vector(new double[this.length()]);
        for(int i = 0; i < length(); i++) {
            v.set(i, this.at(i) / value);
        }
        return v;
    }

    public Vector clone() {
        return new Vector(this.toArray().clone());
    }

    public void set(int i, double value) {
        data.set(i, value);
    }

    public void set(int i, int value) {
        data.set(i, (double)value);
    }

    public List<Double> getData() {
        return data;
    }

    public void swap(int i, int j) {
        double tmp = data.get(i);
        data.set(j, data.get(i));
        data.set(i, tmp);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof Vector)) {
            return false;
        }

        Vector other = (Vector) object;
        if (this.length() != other.length()) {
            return false;
        }

        for (int i = 0; i < this.length(); i++) {
            if(this.at(i) != other.at(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Iterator<Double> iterator() {
        return new Iterator<Double>() {
            private Integer currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < length();
            }

            @Override
            public Double next() {
                return at(currentIndex++);
            }
        };
    }

    public int intAt(int i) {
        return (int)this.at(i);
    }

    public Vector replace(int one, int two) {
        Vector v = this.clone();
        for(int i = 0; i < this.length();  i++) {
            if(v.at(i) == one) {
                v.set(i, two);
            }
        }
        return v;
    }

    public boolean contains(int num) {
        for(int i = 0; i < data.size(); i++) {
            if(data.get(i) == num) {
                return true;
            }
        }
        return false;
    }

    public Vector sub(double mean) {
        Vector v = new Vector(new double[this.length()]);
        for(int i = 0; i < length(); i++) {
            v.set(i, this.at(i) - mean);
        }
        return v;
    }

    public Vector pow(int n) {
        Vector v = new Vector(new double[this.length()]);
        for(int i = 0; i < length(); i++) {
            v.set(i, java.lang.Math.pow(this.at(i), n));
        }
        return v;
    }

    public double prod() {
        double sum = 1;
        for(Double d : this) {
            sum *= d;
        }
        return sum;
    }

    public double mean() {
        return this.sum() / this.length();
    }

    public int count(double value) {
        int count = 0;
        for(Double d : this.data) {
            if(d.doubleValue() == value) {
                count++;
            }
        }
        return count;
    }

    public double stdev() {
        double mean = this.mean();
        double variance = this.sub(mean).pow(2).sum() / (this.length() - 1);
        return sqrt(variance);
    }

    public Vector sub(Vector other) {
        Vector v = new Vector(new double[other.length()]);
        for(int i = 0; i < other.length(); i++) {
            v.set(i, this.at(i)-other.at(i));
        }
        return v;
    }

    public double median() {
        if(this.length() == 0) {
            throw new DataException("cannot compute median of empty vector");
        }

        Vector v = this.clone();
        v.sort();
        if(v.length()%2 == 1) {
            return v.at(v.length()/2);
        } else {
            return v.at(v.length()/2) + v.at(v.length()/2 + 1);
        }
    }

    public void sort() {
        Collections.sort(data);
    }
}
