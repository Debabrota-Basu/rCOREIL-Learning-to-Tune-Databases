package edu.ucsc.dbtune.optimizer;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.ucsc.dbtune.ibg.IBGCoveringNodeFinder;
import edu.ucsc.dbtune.ibg.IBGCoveringNodeFinder.FindResult;
import edu.ucsc.dbtune.ibg.IndexBenefitGraph;
import edu.ucsc.dbtune.metadata.Index;
import edu.ucsc.dbtune.workload.SQLStatement;

import static edu.ucsc.dbtune.ibg.IndexBenefitGraphConstructor.construct;

/**
 * Prepared statements that are produced by the {@link IBGOptimizer}.
 *
 * @see IBGOptimizer
 * @author Karl Schnaitter
 * @author Huascar A. Sanchez
 * @author Ivo Jimenez
 * @author Neoklis Polyzotis
 */
public class IBGPreparedSQLStatement extends DefaultPreparedSQLStatement
{
    /** used to find nodes in the ibg. */
    private static final IBGCoveringNodeFinder NODE_FINDER = new IBGCoveringNodeFinder();

    /** The {@link IndexBenefitGraph} used by this prepared statement. */
    private IndexBenefitGraph ibg;

    /** The universe of indexes from which actual explains will occur. */
    private Set<Index> universe;

    /**
     * Constructcs a prepared statement.
     *  
     * @param optimizer 
     *      The optimizer that created this statement
     * @param sql
     *      The SQL statement that corresponds to this prepared statement
     * 
     */
    public IBGPreparedSQLStatement(IBGOptimizer optimizer, SQLStatement sql)
    {
        this(optimizer, sql, null, null);
    }
    
    /**
     * Constructs a prepared statement.
     *
     * @param optimizer
     *     The optimizer that created this statement
     * @param sql
     *     The SQL statement that corresponds to this prepared statement
     * @param ibg
     *     IBG that this new statement will use to execute what-if optimization calls
     * @param universe
     *      The set of all indexes of interest
     */    
    public IBGPreparedSQLStatement(
            IBGOptimizer optimizer,
            SQLStatement sql,
            IndexBenefitGraph ibg,
            Set<Index> universe)
    {
        super(optimizer, sql);

        this.ibg = ibg;
        this.universe = universe;
    }

    /**
     * copy constructor.
     *
     * @param other
     *      other object being copied
     */
    public IBGPreparedSQLStatement(IBGPreparedSQLStatement other)
    {
        super(other);

        ibg = other.ibg;
        universe = other.universe;
    }

    /**
     * @return an {@link IndexBenefitGraph} of this query.
     */
    public IndexBenefitGraph getIndexBenefitGraph()
    {
        return ibg;
    }

    /**
     * Returns the Universe.
     *
     * @return
     *      the universe from which the prepared statement was generated.
     */
    public Set<Index> getUniverse()
    {
        return universe;
    }

    /**
     * Uses the IBG to obtain a new {@link ExplainedSQLStatement}.
     *
     * @param configuration
     *      the configuration considered to estimate the cost of the new statement.
     * @return
     *      a new statement
     * @throws SQLException
     *      if it's not possible to do what-if optimization on the given configuration
     */
    @Override
    public ExplainedSQLStatement explain(Set<Index> configuration)
        throws SQLException
    {
        int optimizationCount = 0;

        if (ibg == null) {
            // time to build the IBG
            int oldOptimizationCount = optimizer.getWhatIfCount();

            ExplainedSQLStatement eStmt = optimizer.getDelegate().explain(sql);
            optimizationCount = optimizer.getWhatIfCount() - oldOptimizationCount;

            this.ibg =
                construct(optimizer.getDelegate(), sql, eStmt.getSelectCost(), configuration);
            this.updatedTable = eStmt.getUpdatedTable();
            this.baseTableUpdateCost = eStmt.getBaseTableUpdateCost();
            this.universe = new HashSet<Index>(configuration);
        }

        if (!getUniverse().containsAll(configuration))
            throw new SQLException(
                "Configuration " + configuration + " not contained in: " + getUniverse());

        if (configuration.isEmpty())
            return new ExplainedSQLStatement(
                    getSQLStatement(),
                    null,
                    getOptimizer(),
                    getIndexBenefitGraph().emptyCost(),
                    getUpdatedTable(),
                    getBaseTableUpdateCost(),
                    new HashMap<Index, Double>(),
                    configuration,
                    new HashSet<Index>(),
                    optimizationCount);

        FindResult result = NODE_FINDER.find(getIndexBenefitGraph(), configuration);

        if (result == null)
            throw new SQLException("IBG construction has not completed yet");
            
        return new ExplainedSQLStatement(
                getSQLStatement(),
                null,
                optimizer,
                result.getCost(),
                getUpdatedTable(),
                getBaseTableUpdateCost(),
                getIndexUpdateCosts(configuration),
                configuration,
                result.getUsedConfiguration(),
                optimizationCount);
    }
}
