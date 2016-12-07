package tests;

import main.DenseMatrix;
import main.Matrix;
import main.SparseMatrix;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Tests
{
    @Test
    public void mulDD() {
        Matrix m1 = new DenseMatrix("m1.txt");
        Matrix m2 = new DenseMatrix("m2.txt");
        Matrix expected = new DenseMatrix("result.txt");
        assertEquals(expected, m1.mul(m2));
    }

    @Test
    public void mulDS() {
        Matrix m1 = new DenseMatrix("m1.txt");
        Matrix m2 = new SparseMatrix("m2.txt");
        Matrix expected = new DenseMatrix("result.txt");
        assertEquals(expected, m1.mul(m2));
    }

    @Test
    public void mulSD() {
        Matrix m1 = new SparseMatrix("m1.txt");
        Matrix m2 = new DenseMatrix("m2.txt");
        Matrix expected = new DenseMatrix("result.txt");
        assertEquals(expected, m1.mul(m2));
    }

    @Test
    public void mulSS() {
        Matrix m1 = new SparseMatrix("m1.txt");
        Matrix m2 = new SparseMatrix("m2.txt");
        Matrix expected = new DenseMatrix("result.txt");
        assertEquals(expected, m1.mul(m2));
    }

    @Test
    public void dMulDD() {
        Matrix m1 = new DenseMatrix("m1.txt");
        Matrix m2 = new DenseMatrix("m2.txt");
        Matrix expected = new DenseMatrix("result.txt");
        assertEquals(expected, m1.threadMul(m2));
    }

    @Test
    public void dMulSS() {
        Matrix m1 = new SparseMatrix("m1.txt");
        Matrix m2 = new SparseMatrix("m2.txt");
        Matrix expected = new SparseMatrix("result.txt");
        Matrix m3 = m1.threadMul(m2);
        assertEquals(expected, m3);
    }
}
