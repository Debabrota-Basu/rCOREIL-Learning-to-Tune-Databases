# rCOREIL-Learning-to-Tune-Databases

rCOREIL and COREIL are algorithms that learn-to-do adaptive performance tuning of database applications.

As a theoretical framework, rCOREIL and COREIL model the execution of queries and updates as a Markov
decision process whose states are database configurations, actions are
configuration changes, and rewards are functions of the cost of configuration change and query and update evaluation.

This code instantiates the algorithms for automated index tuning and evaluates rCOREIL and COREIL
on an OLTP dataset.

# Original Papers

1. <b>rCOREIL:</b> Basu, Debabrota, Qian Lin, Weidong Chen, Hoang Tam Vo, Zihong Yuan, Pierre Senellart, and Stéphane Bressan. <a href="https://hal.archives-ouvertes.fr/hal-02115175/document">"Regularized cost-model oblivious database tuning with reinforcement learning."</a> In Transactions on Large-Scale Data-and Knowledge-Centered Systems XXVIII, pp. 96-132. Springer, Berlin, Heidelberg, 2016.
2. <b>COREIL:</b> Basu, Debabrota, Qian Lin, Weidong Chen, Hoang Tam Vo, Zihong Yuan, Pierre Senellart, and Stéphane Bressan. <a href="https://link.springer.com/chapter/10.1007/978-3-319-22849-5_18">"Cost-model oblivious database tuning with reinforcement learning."</a> In Database and Expert Systems Applications, pp. 253-268. Springer, Cham, 2015.

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

# Bibliography
Use this for citation if you use the code:

<b>rCOREIL:</b> @incollection{basu2016regularized,
  title={Regularized cost-model oblivious database tuning with reinforcement learning},
  author={Basu, Debabrota and Lin, Qian and Chen, Weidong and Vo, Hoang Tam and Yuan, Zihong and Senellart, Pierre and Bressan, St{\'e}phane},
  booktitle={Transactions on Large-Scale Data-and Knowledge-Centered Systems XXVIII},
  pages={96--132},
  year={2016},
  publisher={Springer}
}
