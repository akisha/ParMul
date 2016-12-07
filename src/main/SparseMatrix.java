package main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class SparseMatrix implements Matrix {

    public HashMap<Integer, HashMap<Integer, Double>> mat;
    public int rows;
    public int cols;

    public SparseMatrix(String fileName) {
        rows = 0;
        cols = 0;
        HashMap<Integer, HashMap<Integer, Double>> matrix = new HashMap<>();
        try {
            File f = new File(fileName);
            Scanner in = new Scanner(f);
            String[] line = {};
            HashMap<Integer, Double> temp;
            while (in.hasNextLine()) {
                temp = new HashMap<>();
                line = in.nextLine().split(" ");
                for (int i = 0; i < line.length; i++) {
                    if (line[i] != "0") {
                        temp.put(i, Double.parseDouble(line[i]));
                    }
                }
                if (temp.size()!= 0) {
                    matrix.put(rows++, temp);
                }
            }
            cols = line.length;
            mat = matrix;
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public SparseMatrix(HashMap<Integer, HashMap<Integer, Double>> matrix, int rows, int cols) {
        this.mat = matrix;
        this.rows = rows;
        this.cols = cols;
    }

    public Matrix mul(Matrix o) {
        if (o instanceof DenseMatrix) {
            return mul((DenseMatrix) o);
        }
        if (o instanceof SparseMatrix) {
            return mul((SparseMatrix) o);
        }
        else return null;
    }

    public SparseMatrix transponse() {
        HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<>();
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> row : mat.entrySet()){
            for (HashMap.Entry<Integer, Double> elem : row.getValue().entrySet()) {
                if (!result.containsKey(elem.getKey())) {
                    result.put(elem.getKey(), new HashMap<>());
                }
                result.get(elem.getKey()).put(row.getKey(), elem.getValue());
            }
        }
        return new SparseMatrix(result, cols, rows);
    }

    private SparseMatrix mul(SparseMatrix s) {
        SparseMatrix sT = s.transponse();
        HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<>();
        double sum = 0;
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> row1 : mat.entrySet()){
            for (HashMap.Entry<Integer, HashMap<Integer, Double>> row2 : sT.mat.entrySet()) {
                for (HashMap.Entry<Integer, Double> elem : row1.getValue().entrySet()) {
                    if (row2.getValue().containsKey(elem.getKey())) {
                        sum += elem.getValue()*row2.getValue().get(elem.getKey());
                    }
                }
                if (sum != 0) {
                    if (!result.containsKey(row1.getKey())) {
                        result.put(row1.getKey(), new HashMap<>());
                    }
                    result.get(row1.getKey()).put(row2.getKey(), sum);
                }
                sum = 0;
            }
        }
        return new SparseMatrix(result, rows, s.cols);
    }

    private DenseMatrix mul(DenseMatrix d) {
        double[][] dT = d.transponse();
        double[][] result = new double[rows][dT.length];
        double sum = 0;
        for (Map.Entry<Integer, HashMap<Integer, Double>> row1 : mat.entrySet()){
            for (int j = 0; j<dT.length; j++) {
                for (HashMap.Entry<Integer, Double> elem : row1.getValue().entrySet()) {
                    if (row1.getValue().containsKey(elem.getKey())) {
                        sum += elem.getValue()*dT[j][elem.getKey()];
                    }
                }
                result[row1.getKey()][j] = sum;
                sum = 0;
            }
        }
        return new DenseMatrix(result);
    }

    public Matrix threadMul(Matrix o){
        SparseMatrix s = (SparseMatrix)o;

        class Dispatcher {
            int value = 0;
            public int next() {
                synchronized (this) {
                    return value++;
                }
            }
        }
        Dispatcher dispatcher = new Dispatcher();
        SparseMatrix transedMatrix = s.transponse();
        ConcurrentHashMap<Integer, HashMap<Integer, Double>> cmat = new ConcurrentHashMap<>();
        for (int i = 0; i < rows; i++) {
            if (mat.containsKey(i)) {
                cmat.put(i, new HashMap<>(mat.get(i)));
            }
        }
        ConcurrentHashMap<Integer, HashMap<Integer, Double>> maph = new ConcurrentHashMap<>();
        for (int i = 0; i < transedMatrix.rows; i++) {
            if (transedMatrix.mat.containsKey(i)) {
                maph.put(i, transedMatrix.mat.get(i));
            }
        }
        HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<>();
        ConcurrentHashMap<Integer, HashMap<Integer, Double>> cresult = new ConcurrentHashMap<>();
        double sum = 0;
        class MultRow implements Runnable {
            Thread thread;
            HashMap<Integer, Double> temp = new HashMap<>();
            public MultRow(String s) {
                thread = new Thread(this, s);
                thread.start();
            }
            public void run() {
                int i;
                while ((i = dispatcher.next()) < rows) {
                    double sum = 0;
                    if (mat.containsKey(i)) {
                        temp = new HashMap<>();
                        for (int j = 0; j < transedMatrix.rows; j++) {
                            if (maph.containsKey(j)) {
                                for (int k = 0; k < transedMatrix.cols; k++) {
                                    if (maph.get(j).containsKey(k) && mat.get(i).containsKey(k)) {
                                        sum += maph.get(j).get(k) * mat.get(i).get(k);
                                    }
                                }
                                if (sum != 0) {
                                    temp.put(j, sum);
                                }
                                sum = 0;
                            }
                        }
                        cresult.put(i, temp);
                    }
                }
            }
        }
        MultRow one = new MultRow("1");
        MultRow two = new MultRow("2");
        MultRow three = new MultRow("3");
        MultRow four = new MultRow("4");
        try {
            one.thread.join();
            two.thread.join();
            three.thread.join();
            four.thread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        HashMap<Integer, Double> temp;
        for (ConcurrentHashMap.Entry<Integer, HashMap<Integer, Double>> row1 : cresult.entrySet()){
            temp = new HashMap<>();
            for (HashMap.Entry<Integer, Double> row2 : row1.getValue().entrySet()) {
                temp.put(row2.getKey(), row2.getValue());
            }
            result.put(row1.getKey(), temp);
        }
        return new SparseMatrix(result, rows, s.cols);
    }

    @Override
    public boolean equals(Object o) {
        boolean eq = true;
        if (o instanceof DenseMatrix) {
            DenseMatrix temp = (DenseMatrix)o;
            if (temp.mat.length == rows && temp.mat[0].length == cols) {
                for (int i = 0; i < rows; i++) {
                    if (mat.containsKey(i)) {
                        for (int j = 0; j < cols; j++) {
                            if (mat.get(i).containsKey(j)) {
                                if (mat.get(i).get(j) != temp.mat[i][j]) {
                                    eq = false;
                                }
                            } else {
                                if (temp.mat[i][j] != 0) {
                                    eq = false;
                                }
                            }
                        }
                    } else {
                        for (int j = 0; j < cols; j++) {
                            if (temp.mat[i][j] != 0) {
                                eq = false;
                            }
                        }
                    }
                }
            } else {
                eq = false;
            }
        } else if (o instanceof SparseMatrix) {
            SparseMatrix temp = (SparseMatrix) o;
            if (temp.cols == cols && temp.rows == rows) {
                for (int i = 0; i < rows; i++) {
                    if (mat.containsKey(i) && temp.mat.containsKey(i))  {
                        for (int j = 0; j < cols; j++) {
                            if (mat.get(i).containsKey(j) && temp.mat.get(i).containsKey(j)) {
                                if (mat.get(i).get(j).doubleValue() != temp.mat.get(i).get(j).doubleValue()) {
                                    eq = false;
                                }
                            } else if (mat.get(i).containsKey(j) || temp.mat.get(i).containsKey(j)) {
                                eq = false;
                            }
                        }
                    } else if (mat.containsKey(i) || temp.mat.containsKey(i)) {
                        eq = false;
                    }
                }
            } else {
                eq = false;
            }
        }
        return eq;
    }

    public void printMartix(String filename) {
        try {
            PrintWriter w = new PrintWriter(filename);
            for (int i = 0; i<rows; i++) {
                if (mat.containsKey(i)) {
                    for (int j = 0; j<cols; j++) {
                        if (mat.get(i).containsKey(j)) {
                            w.print(mat.get(i).get(j));
                        } else {
                            w.print((double)0);
                        }
                    }
                } else {
                    for (int j = 0; j < cols; j++) {
                        w.print((double)0);
                    }
                    w.println();
                }
            }
            w.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}