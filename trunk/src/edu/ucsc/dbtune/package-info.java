/**
 * <i>DBTune</i>'s root package. Currently, we only support PostgreSQL and DB2.
 * <p>
 * The principal public APIs in this package is
 {@link edu.ucsc.dbtune.DatabaseSystem}, the object that DBTune uses to 
 * provide the abstractions that are needed in order to deal with physical design tuning.
 * <p>
 * The main use case is:
 * <p>
 * <ol>
 *   <li>Connect to a database and extract metadata.</li>
 *   <li>Play with metadata in a static way, i.e. can't `ALTER` db objects:
 *     <ul>
 *       <li>get statistics (cardinalities, histograms, etc.)</li>
 *       <li>schema exploration (relationship among db objects (containment,fk constraints, etc))</li>
 *     </ul>
 *   </li>
 *   <li>What-if functionality
 *     <ul>
 *       <li>create hypotetical configurations</li>
 *       <li>obtain costs for individual queries</li>
 *       <li>compare two configurations</li>
 *       <li>obtain costs at the workload level.</li>
 *     </ul>
 *   </li>
 *   <li>Advising. Get recommendations using a specified technique</li>
 *   <li>Recommendation materlization. Materialize a given configuration.</li>
 * </ol>
 * <p>
 * Some phases are dependant on previous ones. For example, we can't recommend if we haven't created hypothetical 
 * configurations; we can't get hypothetical configurations if we haven't obtained metadata of a database; and so on.
 */
package edu.ucsc.dbtune;
