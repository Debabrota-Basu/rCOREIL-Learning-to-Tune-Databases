# rCOREIL-Learning-to-Tune-Databases

# 0. Prerequisites

a) Java, b) IBM DB2, c) Apache Ant, d) OLTP Benchmark

# 1. Setup

Suggestion: Create a Virtual Machine with Windows(<10) OS and set it up inside it.

In order to install, the OLTP-Benchamrk and to run it, follow the instructions in "documentation/Using OLTP-Benchmark.txt".

# 2. Generate databases for experimentation

Follow the steps described in "documentation/how_to_create_a_database_for_tunning.pdf".

# 3. Running the code
In order to run the programs, follow these steps:

a) Edit ./trunk/config/dftune.cfg (authorization & DBMS-specific)

b) Edit "autotune\tests\sg.edu.nus.autotune\Arguments.java". 
	
  1)Change algorithm by editing "private static final int DEFAULT_ALGO". 
		Available Algorithm is shown in "autotune\tests\sg.edu.nus.autotune\Launcher.java". 
	
  2)Change workload by editing "private static final String DEFAULT_WORKLOAD"

c) Compile & run "autotune\tests\sg.edu.nus.autotune\Launcher.java"
