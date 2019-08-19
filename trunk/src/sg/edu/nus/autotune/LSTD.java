/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sg.edu.nus.autotune;

import static org.slf4j.LoggerFactory.getLogger;

import org.jblas.DoubleMatrix;
import org.slf4j.Logger;


/**
 *
 * @author Chen Weidong
 */
public class LSTD {

    private static final double SMALL_NUMBER = 1e-3;
    private static final Logger LOG = getLogger("e2s2");
    private double _gamma;

    private DoubleMatrix _theta;
    private DoubleMatrix _theta0;
    private int _n;
    
    private DoubleMatrix _B;
    private int _count = 0;
    //coverge limit
    private int _m;

    public LSTD(int n, double gamma, int m) {
    	_n = n;
        _gamma = gamma;
        _theta = new DoubleMatrix(n);
        _theta0 = new DoubleMatrix(n); 
        _m = m;
                
        _B = new DoubleMatrix(n, n);
        for (int i = 0; i < n; i++) {
            _B.put(i, i, SMALL_NUMBER);
        }
    }
    
    private double compute(DoubleMatrix phi0, DoubleMatrix phi1, double hat_C) {
    	DoubleMatrix var1 = phi0.sub(phi1.mul(_gamma));
    	double var2 = 1 + var1.transpose().mmul(_B).dot(phi0);
    	DoubleMatrix var3 = _B.mmul(phi0).mmul(var1.transpose()).mmul(_B);
    	double epsilon = hat_C - var1.dot(_theta0);
    	DoubleMatrix error = _B.mmul(phi0).mul(epsilon / var2);
    	_theta0 = _theta0.add(error);
    	_B = _B.sub( var3.mul(1 / var2) );
    	return error.norm2();
    }
    
    public DoubleMatrix get(DoubleMatrix phi0, DoubleMatrix phi1, double hat_C){
    	double err = compute(phi0, phi1, hat_C);
    	
    	_count += 1;
    	
    	if( _m == 1 ){
    		return _theta0;
    	}else if (_count > _m){
    		copyTheta();
    		_count = 0;
	    }
//    	LOG.info("Converge error:\t" + err + ". Will converge in " + (_m - _count));
    	return _theta;
    }
    
    private void copyTheta(){
    	_theta.copy(_theta0);
    }
    
    public DoubleMatrix getVector(){
    	return _theta;
    }
    
    public DoubleMatrix getVector0(){
    	return _theta0;
    }
}