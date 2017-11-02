package ca.jacob.cs6735.dt;

import ca.jacob.cs6735.util.Matrix;
import ca.jacob.cs6735.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static ca.jacob.cs6735.util.Math.calculateOccurrences;
import static ca.jacob.cs6735.util.Math.log2;

public class Node {
    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    private Double entropy;
    private Integer attribute;
    private Boolean leaf;
    private Matrix data;
    private Map<Integer, Node> children;
    private Integer level;
    private Integer maxLevel;

    public Node(Matrix x, Vector y, Integer level, Integer maxLevel) {
        this.data = x;
        this.data.pushCol(y);
        this.init(level, maxLevel);
    }

    public Node(Integer[][] data, Integer level, Integer maxLevel) {
        this.data = new Matrix(data);
        this.init(level, maxLevel);
    }

    public Node(Matrix data, Integer level, Integer maxLevel) {
        this.data = data;
        this.init(level, maxLevel);
    }

    private void init(Integer level, Integer maxLevel) {
        children = new HashMap<Integer, Node>();
        leaf = false;
        this.level = level;
        this.maxLevel = maxLevel;
    }

    public Double entropy() {
        if (this.entropy != null) return entropy;

        Map<Double, Integer> classes = calculateOccurrences(data.col(data.colCount() - 1));
        LOG.debug("there are {} classes", classes.size());

        Double sum = 0.;
        for (Integer count : classes.values()) {
            sum += count;
        }
        LOG.debug("sum is " + sum);

        entropy = 0.;
        for (Integer count : classes.values()) {
            entropy -= count / sum * log2(count / sum);
        }
        LOG.debug("the entropy is {}", entropy);

        return entropy;
    }

    public void split() {
        LOG.info("split - starting for level {}", level);

        if(level == maxLevel) {
            this.leaf = true;
            return;
        }

        Integer numOfAttributes = data.colCount() - 1;

        Double minEntropy = null;
        for (Integer j = 0; j < numOfAttributes; j++) {
            LOG.trace("checking attribute {}", j);
            Map<Integer, Matrix> split = new HashMap<Integer, Matrix>();
            for (Integer i = 0; i < data.rowCount(); i++) {
                LOG.trace("checking row {}", i);
                Integer value = data.at(i, j).intValue();

                Matrix entry = split.get(value);
                if (entry == null) {
                    LOG.debug("adding node for value {}", value);
                    entry = new Matrix();
                    split.put(value, entry);
                }

                Vector v = data.row(i);
                v.remove(j);
                entry.pushRow(v);
            }

            Double entropy = 0.;
            for (Map.Entry<Integer, Matrix> entry : split.entrySet()) {
                entropy += new Node(entry.getValue(), level + 1, maxLevel).entropy();
            }
            LOG.debug("the total entropy of the children when splitting on attribute {} is {}", j, entropy);

            if (minEntropy == null || entropy < minEntropy) {
                LOG.trace("attribute {} is now the best attribute", j);

                minEntropy = entropy;
                attribute = j;
                children = new HashMap<Integer, Node>();
                for (Map.Entry<Integer, Matrix> entry : split.entrySet()) {
                    children.put(entry.getKey(), new Node(entry.getValue(), level + 1, maxLevel));
                }
            }
        }
        LOG.debug("the best attribute is {} for level {}", attribute, level);

        for (Map.Entry<Integer, Node> entry : children.entrySet()) {
            Node node = entry.getValue();
            if (node.entropy() == 0 || node.entryCount() <= 1) {
                node.isLeaf(true);
            } else {
                node.split();
            }
        }
    }

    public Map<Integer, Node> getChildren() {
        return children;
    }

    public Integer entryCount() {
        return data.rowCount();
    }

    public Integer classify(Vector e) {
        LOG.info("predict - starting for level {} and attribute {}", level, attribute);
        if (this.leaf) {
            LOG.debug("a leaf was found");
            Vector v = data.col(data.colCount() - 1);
            Integer valueOfMaxOccurrance = v.valueOfMaxOccurrance().intValue();
            LOG.debug("value of max occurrance for vector {} is {}", v, valueOfMaxOccurrance);
            return valueOfMaxOccurrance;
        } else {
            for (Map.Entry<Integer, Node> entry : children.entrySet()) {
                if (e.at(attribute).equals(entry.getKey())) {
                    return entry.getValue().classify(e);
                }
            }
            throw new ID3PredictionException("no attribute found for attribute " + attribute);
        }
    }

    public Integer getAttribute() {
        return attribute;
    }

    public Matrix getData() {
        return data;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void isLeaf(boolean leaf) {
        this.leaf = leaf;
    }
}
