package sg.edu.nus.autotune;

import org.jblas.DoubleMatrix;

public class LeastSquare {
	
	private static final double SMALL_NUMBER = 1e-3;
	
	private DoubleMatrix _zeta;
	private int _n;
	private DoubleMatrix _B;
	
	public LeastSquare(int n) {
    	_n = n;
        _zeta = new DoubleMatrix(n);
                
        _B = new DoubleMatrix(n, n);
        for (int i = 0; i < n; i++) {
            _B.put(i, i, SMALL_NUMBER);
        }
    }
	
	private void compute(DoubleMatrix phi, double epsilon) {
    	double gamma = 1 + phi.transpose().mmul(_B).dot(phi);
    	DoubleMatrix H = _B.mul(1/gamma);
    	_B = _B.sub(_B.mmul(phi).mmul(phi.transpose()).mmul(_B));
    	_zeta = _zeta.sub(H.mmul(phi).mul(epsilon));
    }
	
	public DoubleMatrix get(DoubleMatrix phi, double epsilon){
		compute(phi,epsilon);
                //System.out.println("zeta-error: " + epsilon);
		//System.out.println("zeta: " + _zeta);
		return _zeta;
	}
	
	public DoubleMatrix getVector(){
		return _zeta;
	}
	
}
