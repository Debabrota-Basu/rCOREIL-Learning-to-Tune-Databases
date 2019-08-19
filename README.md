# rCOREIL-Learning-to-Tune-Databases


# Running the code
In order to run the programs, follow these steps:

a) Edit ./trunk/config/dftune.cfg (authorization & DBMS-specific)

b) Edit "autotune\tests\sg.edu.nus.autotune\Arguments.java". 
	
  1)Change algorithm by editing "private static final int DEFAULT_ALGO". 
		Available Algorithm is shown in "autotune\tests\sg.edu.nus.autotune\Launcher.java". 
	
  2)Change workload by editing "private static final String DEFAULT_WORKLOAD"

c) Compile & run "autotune\tests\sg.edu.nus.autotune\Launcher.java"
