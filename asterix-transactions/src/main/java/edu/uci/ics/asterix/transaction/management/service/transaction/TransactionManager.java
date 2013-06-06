/*
 * Copyright 2009-2011 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.asterix.transaction.management.service.transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uci.ics.asterix.common.exceptions.ACIDException;
import edu.uci.ics.asterix.common.transactions.DatasetId;
import edu.uci.ics.asterix.common.transactions.ITransactionContext;
import edu.uci.ics.asterix.common.transactions.ITransactionManager;
import edu.uci.ics.asterix.common.transactions.JobId;
import edu.uci.ics.asterix.transaction.management.service.logging.LogType;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.api.lifecycle.ILifeCycleComponent;

/**
 * An implementation of the @see ITransactionManager interface that provides
 * implementation of APIs for governing the lifecycle of a transaction.
 */
public class TransactionManager implements ITransactionManager, ILifeCycleComponent {

    public static final boolean IS_DEBUG_MODE = false;//true
    private static final Logger LOGGER = Logger.getLogger(TransactionManager.class.getName());
    private final TransactionSubsystem transactionProvider;
    private Map<JobId, ITransactionContext> transactionContextRepository = new HashMap<JobId, ITransactionContext>();
    private AtomicInteger maxJobId = new AtomicInteger(0);

    public TransactionManager(TransactionSubsystem provider) {
        this.transactionProvider = provider;
    }

    @Override
    public void abortTransaction(ITransactionContext txnContext, DatasetId datasetId, int PKHashVal)
            throws ACIDException {
        synchronized (txnContext) {
            if (txnContext.getTxnState().equals(TransactionState.ABORTED)) {
                return;
            }

            try {
                transactionProvider.getRecoveryManager().rollbackTransaction(txnContext);
            } catch (Exception ae) {
                String msg = "Could not complete rollback! System is in an inconsistent state";
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe(msg);
                }
                ae.printStackTrace();
                throw new Error(msg);
            } finally {
                txnContext.releaseResources();
                transactionProvider.getLockManager().releaseLocks(txnContext);
                transactionContextRepository.remove(txnContext.getJobId());
                txnContext.setTxnState(TransactionState.ABORTED);
            }
        }
    }

    @Override
    public ITransactionContext beginTransaction(JobId jobId) throws ACIDException {
        setMaxJobId(jobId.getId());
        ITransactionContext txnContext = new TransactionContext(jobId, transactionProvider);
        synchronized (this) {
            transactionContextRepository.put(jobId, txnContext);
        }
        return txnContext;
    }

    @Override
    public ITransactionContext getTransactionContext(JobId jobId) throws ACIDException {
        setMaxJobId(jobId.getId());
        synchronized (transactionContextRepository) {

            ITransactionContext context = transactionContextRepository.get(jobId);
            if (context == null) {
                context = transactionContextRepository.get(jobId);
                context = new TransactionContext(jobId, transactionProvider);
                transactionContextRepository.put(jobId, context);
            }
            return context;
        }
    }

    @Override
    public void commitTransaction(ITransactionContext txnContext, DatasetId datasetId, int PKHashVal)
            throws ACIDException {
        synchronized (txnContext) {
            if ((txnContext.getTxnState().equals(TransactionState.COMMITTED))) {
                return;
            }

            //There is either job-level commit or entity-level commit.
            //The job-level commit will have -1 value both for datasetId and PKHashVal.

            //for entity-level commit
            if (PKHashVal != -1) {
                boolean countIsZero = transactionProvider.getLockManager().unlock(datasetId, PKHashVal, txnContext,
                        true);
                if (!countIsZero) {
                    // Lock count != 0 for a particular entity implies that the entity has been locked 
                    // more than once (probably due to a hash collision in our current model).
                    // It is safe to decrease the active transaction count on indexes since,  
                    // by virtue of the counter not being zero, there is another transaction 
                    // that has increased the transaction count. Thus, decreasing it will not 
                    // allow the data to be flushed (yet). The flush will occur when the log page
                    // flush thread decides to decrease the count for the last time.
                    try {
                        //decrease the transaction reference count on index
                        txnContext.decreaseActiveTransactionCountOnIndexes();
                    } catch (HyracksDataException e) {
                        throw new ACIDException("failed to complete index operation", e);
                    }
                }
                return;
            }

            //for job-level commit
            try {
                if (txnContext.getTransactionType().equals(ITransactionContext.TransactionType.READ_WRITE)) {
                    transactionProvider.getLogManager().log(LogType.COMMIT, txnContext, -1, -1, -1, (byte) 0, 0, null,
                            null, txnContext.getLastLogLocator());
                }
            } catch (ACIDException ae) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe(" caused exception in commit !" + txnContext.getJobId());
                }
                throw ae;
            } finally {
                txnContext.releaseResources();
                transactionProvider.getLockManager().releaseLocks(txnContext); // release
                transactionContextRepository.remove(txnContext.getJobId());
                txnContext.setTxnState(TransactionState.COMMITTED);
            }
        }
    }

    @Override
    public void completedTransaction(ITransactionContext txnContext, DatasetId datasetId, int PKHashVal, boolean success)
            throws ACIDException {
        if (!success) {
            abortTransaction(txnContext, datasetId, PKHashVal);
        } else {
            commitTransaction(txnContext, datasetId, PKHashVal);
        }
    }

    @Override
    public TransactionSubsystem getTransactionProvider() {
        return transactionProvider;
    }

    public void setMaxJobId(int jobId) {
        maxJobId.set(Math.max(maxJobId.get(), jobId));
    }

    public int getMaxJobId() {
        return maxJobId.get();
    }

    @Override
    public void start() {
        //no op
    }

    @Override
    public void stop(boolean dumpState, OutputStream os) {
        if (dumpState) {
            //#. dump TxnContext
            dumpTxnContext(os);

            try {
                os.flush();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    private void dumpTxnContext(OutputStream os) {
        JobId jobId;
        ITransactionContext txnCtx;
        StringBuilder sb = new StringBuilder();

        try {
            sb.append("\n>>dump_begin\t>>----- [ConfVars] -----");
            Set<Map.Entry<JobId, ITransactionContext>> entrySet = transactionContextRepository.entrySet();
            if (entrySet != null) {
                for (Map.Entry<JobId, ITransactionContext> entry : entrySet) {
                    if (entry != null) {
                        jobId = entry.getKey();
                        if (jobId != null) {
                            sb.append("\n" + jobId);
                        } else {
                            sb.append("\nJID:null");
                        }

                        txnCtx = entry.getValue();
                        if (txnCtx != null) {
                            sb.append(txnCtx.prettyPrint());
                        } else {
                            sb.append("\nTxnCtx:null");
                        }
                    }
                }
            }

            sb.append("\n>>dump_end\t>>----- [ConfVars] -----\n");
            os.write(sb.toString().getBytes());
        } catch (Exception e) {
            //ignore exception and continue dumping as much as possible.
            if (IS_DEBUG_MODE) {
                e.printStackTrace();
            }
        }
    }
}