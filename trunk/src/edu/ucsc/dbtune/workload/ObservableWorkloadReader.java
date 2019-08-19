package edu.ucsc.dbtune.workload;

import java.sql.SQLException;

import java.util.List;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.ucsc.dbtune.util.EventBusFactory;

/**
 * A reader that watches the source for newcomers. Each new statement is included in the iterable
 * collection of {@link SQLStatement} objects that the reader can iterate on. Also, this class 
 * allows for subscribers (via guava's {@link EventBus}) that get notified whenever new statements 
 * are added.
 * <p>
 * This class can be thought of as a mutable workload.
 * <p>
 * In order to make use of the class, the implementor has to:
 * <ol>
 * <li>
 *    After setting up, start the watcher "service", which is just a thread that invokes the {@link 
 *    #hasnewStatement()} method periodically (default: 10 second).
 * </li>
 * <li>
 *    Implement the {@link #hasNewStatement()} method in order for new statements to get included in 
 *    the underlying iterable collection and for them to be published through the event bus.
 * </li>
 * </ol>
 * <p>
 * The identifier of the events posted to the bus are implicitly communicated through the {@link 
 * SQLStatement} members {@code workloadName} and {@code workloadIdentifier}, i.e. a subscriber 
 * knows that a particular statement corresponds to it by looking  at these two fields.
 * <p>
 * This class is not thread-safe.
 *
 * @author Ivo Jimenez
 */
public abstract class ObservableWorkloadReader extends AbstractWorkloadReader
{
    // TODO: this class merges the two things in one; separate the publishing service from the
    //       watching facility,

    // flag to identify whether the watcher has been started
    private boolean alreadyStarted;

    /**
     * @param workload
     *      workload that is assigned to each of the statements that are instantiated.
     */
    protected ObservableWorkloadReader(Workload workload)
    {
        super(workload);
    }

    /**
     * Invokes {@link #hasNewStatement} every ten seconds.
     */
    protected void startWatcher()
    {
        startWatcher(10);
    }

    /**
     * Invokes {@link #hasNewStatement} every {@code n} seconds. If new statements are returned, 
     * each is first posted to the event bus and subsequently added to the internal statement 
     * collection, i.e. every individual statement returned by {@link #hasNewStatement} is first 
     * posted and then added to the internal collection.
     *
     * @param n number of seconds to wait for between every check
     */
    protected void startWatcher(int n)
    {
        if (alreadyStarted)
            throw new RuntimeException("watcher already started");

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final Runnable newStatementChecker = new Runnable() {
            public void run()
            {
                try {
                    List<SQLStatement> newStatements = hasNewStatement();

                    for (SQLStatement stmt : newStatements) {

                        // double check that the underlying workload reader is assigning the 
                        // expected workload correctly. This is important since this is the way 
                        // statements are properly routed in the event bus, i.e. a subscriber knows 
                        // that a particular statement corresponds to it by looking at the {@link 
                        // Workload} instance
                        if (!stmt.getWorkload().equals(workload))
                            throw new RuntimeException("Statement's workload should be" + workload);

                        EventBusFactory.getEventBusInstance().post(stmt);
                        sqls.add(stmt);
                    }

                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        scheduler.scheduleAtFixedRate(newStatementChecker, n, n, TimeUnit.SECONDS);

        alreadyStarted = true;
    }

    /**
     * A method that checks for new statements. If one or more statements are returned, they are 
     * passed to every observer of this workload (through the event bus).
     *
     * @return
     *      a list with new statements, or empty of no new statements are in the source
     * @throws SQLException
     *      if an error occurs while checking for new statements
     */
    protected abstract List<SQLStatement> hasNewStatement() throws SQLException;
}
