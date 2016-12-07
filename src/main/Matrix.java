package main;

public interface Matrix {
    Matrix mul(Matrix o);
    Matrix threadMul(Matrix o);
}