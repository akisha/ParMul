package main;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class DenseMatrix implements Matrix {
    public double[][] mat;

   public DenseMatrix(String fileName) {
        try {
            File f = new File(fileName);
            Scanner in = new Scanner(f);
            String[] line;
            ArrayList<Double[]> a = new ArrayList<>();
            Double[] tmp = {};
            while (in.hasNextLine()) {
                line = in.nextLine().split(" ");
                tmp = new Double[line.length];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = Double.parseDouble(line[i]);
                }
                a.add(tmp);
            }
            double[][] matrix = new double[a.size()][tmp.length];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    matrix[i][j] = a.get(i)[j];
                }
            }
            mat = matrix;
        } catch(IOException e) {
            e.printStackTrace();
        }

   }

    public DenseMatrix(double[][] matrix) {
        mat = matrix;
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

    public double[][] transponse() {
        double[][] matrix = new double[mat[0].length][mat.length];
        for (int i = 0; i < mat[0].length; i++ ) {
            for (int j = 0; j < mat.length; j++ ) {
                matrix[i][j] = mat[j][i];
            }
        }
        return matrix;
    }

    private DenseMatrix mul(DenseMatrix other) {
        double[][] t = other.transponse();
        double[][] result = new double[mat.length][t.length];
        for (int i = 0; i < mat.length; i++) {
            result[i] = new double[t.length];
        }
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < t.length; j++) {
                for (int k = 0; k < t[0].length; k++) {
                    result[i][j] += mat[i][k] * t[j][k];
                }
            }
        }
        return new DenseMatrix(result);
    }

    private DenseMatrix mul(SparseMatrix s) {
        SparseMatrix sT = s.transponse();
        double[][] result = new double[mat.length][s.cols];
        double sum = 0;
        for (int i = 0; i < mat.length; i++){
            for (HashMap.Entry<Integer, HashMap<Integer, Double>> row2 : sT.mat.entrySet()) {
                for (int k = 0; k < mat[0].length; k++) {
                    if (row2.getValue().containsKey(k)) {
                        sum += mat[i][k]*row2.getValue().get(k);
                    }
                }
                result[i][row2.getKey()] = sum;
                sum = 0;
            }
        }
        return new DenseMatrix(result);
    }

    public DenseMatrix threadMul(Matrix o) {
        DenseMatrix object = (DenseMatrix)o;
        class Dispatcher {
            int value = 0;
            public int next() {
                synchronized (this) {
                    return value++;
                }
            }
        }
        double[][] result = new double[mat.length][object.mat[0].length];
        double[][] transpDMatrix = object.transponse();
        Dispatcher dispatcher = new Dispatcher();
        class MultRow implements Runnable {
            Thread thread;
            public MultRow(String s) {
                thread = new Thread(this, s);
                thread.start();
            }
            public void run() {
                int i = dispatcher.next();
                for (int j = 0; j < transpDMatrix.length; j++) {
                    for (int k = 0; k < transpDMatrix[0].length; k++) {
                        result[i][j] += mat[i][k] * transpDMatrix[j][k];
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
        return new DenseMatrix(result);
    }

    @Override
    public boolean equals(Object object) {
        boolean eq = true;
        if (object instanceof DenseMatrix) {
            DenseMatrix tmp = (DenseMatrix)object;
            if (mat.length == tmp.mat.length && mat[0].length == tmp.mat[0].length) {
                for (int i = 0; i < mat.length; i++) {
                    for (int j=0; j < mat[0].length; j++) {
                        if (mat[i][j] != tmp.mat[i][j]) {
                            eq = false;
                        }
                    }
                }
            } else {
                eq = false;
            }
        } else if (object instanceof SparseMatrix) {
            SparseMatrix tmp = (SparseMatrix)object;
            if (mat.length == tmp.rows && mat[0].length == tmp.cols) {
                for (int i = 0; i<tmp.rows; i++) {
                    if (tmp.mat.containsKey(i)) {
                        for (int j = 0; j<tmp.cols; j++) {
                            if (tmp.mat.get(i).containsKey(j)) {
                                if (tmp.mat.get(i).get(j) != mat[i][j]) {
                                    eq = false;
                                }
                            } else {
                                if (mat[i][j] != 0) {
                                    eq = false;
                                }
                            }
                        }
                    } else {
                        for (int j = 0; j < tmp.cols; j++) {
                            if (mat[i][j] != 0) {
                                eq = false;
                            }
                        }
                    }
                }
            } else {
                eq = false;
            }
        }
        return eq;
    }

    public void printMatrix(String filename) {
        try {
            PrintWriter w = new PrintWriter(filename);
            for (int i = 0; i < mat.length; i++) {
                for (int j = 0; j < mat[0].length; j++) {
                    w.print(mat[i][j]+" ");
                }
                w.println();
            }
            w.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}