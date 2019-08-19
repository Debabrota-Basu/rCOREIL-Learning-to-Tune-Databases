a) edit ./trunk/config/dftune.cfg (authorization & DBMS-specific)
b) edit "autotune\tests\sg.edu.nus.autotune\Arguments.java". 
	1)Change algorithm by editing "private static final int DEFAULT_ALGO". 
		Available Algorithm is shown in "autotune\tests\sg.edu.nus.autotune\Launcher.java". 
	2)Change workload by editing "private static final String DEFAULT_WORKLOAD"
c) compile & run "autotune\tests\sg.edu.nus.autotune\Launcher.java"