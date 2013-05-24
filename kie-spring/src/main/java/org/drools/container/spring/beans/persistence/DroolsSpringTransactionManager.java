/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.container.spring.beans.persistence;

import org.drools.persistence.TransactionManager;
import org.drools.persistence.TransactionSynchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class DroolsSpringTransactionManager
        implements
        TransactionManager {

    Logger logger = LoggerFactory.getLogger(getClass());
    private AbstractPlatformTransactionManager ptm;

    TransactionDefinition td = new DefaultTransactionDefinition();
    TransactionStatus currentTransaction = null;

    public DroolsSpringTransactionManager(AbstractPlatformTransactionManager ptm) {
        this.ptm = ptm;
    }

    public boolean begin() {
        try {
            if (getStatus() == TransactionManager.STATUS_NO_TRANSACTION) {
                // If there is no transaction then start one, we will commit within the same Command
                // it seems in spring calling getTransaction is enough to begin a new transaction
                currentTransaction = this.ptm.getTransaction(td);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.warn("Unable to begin transaction",
                    e);
            throw new RuntimeException("Unable to begin transaction",
                    e);
        }
    }

    public void commit(boolean transactionOwner) {
        if (transactionOwner) {
            try {
                // if we didn't begin this transaction, then do nothing
                this.ptm.commit(currentTransaction);
                currentTransaction = null;
            } catch (Exception e) {
                logger.warn("Unable to commit transaction",
                        e);
                throw new RuntimeException("Unable to commit transaction",
                        e);
            }
        }
    }

    public void rollback(boolean transactionOwner) {
        try {
            if (transactionOwner) {
                this.ptm.rollback(currentTransaction);
                currentTransaction = null;
            }
        } catch (Exception e) {
            logger.warn("Unable to rollback transaction",
                    e);
            throw new RuntimeException("Unable to rollback transaction",
                    e);
        }
    }

    /**
     * Borrowed from Seam
     */
    public int getStatus() {
        if (ptm == null) {
            return TransactionManager.STATUS_NO_TRANSACTION;
        }

        logger.debug("Current TX name (According to TransactionSynchronizationManager) : " + TransactionSynchronizationManager.getCurrentTransactionName());
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionStatus transaction = null;
            try {
                if (currentTransaction == null) {
                    transaction = ptm.getTransaction(td);
                    if (transaction.isNewTransaction()) {
                        return TransactionManager.STATUS_COMMITTED;
                    }
                } else {
                    transaction = currentTransaction;
                }
                logger.debug("Current TX: " + transaction);
                // If SynchronizationManager thinks it has an active transaction but
                // our transaction is a new one
                // then we must be in the middle of committing
                if (transaction.isCompleted()) {
                    if (transaction.isRollbackOnly()) {
                        return TransactionManager.STATUS_ROLLEDBACK;
                    }
                    return TransactionManager.STATUS_COMMITTED;
                } else {
                    // Using the commented-out code in means that if rollback with this manager,
                    //  I always have to catch and check the exception 
                    //  because ROLLEDBACK can mean both "rolled back" and "rollback only".
                    // if ( transaction.isRollbackOnly() ) {
                    //     return TransactionManager.STATUS_ROLLEDBACK;
                    // }

                    return TransactionManager.STATUS_ACTIVE;
                }
            } finally {
                if (currentTransaction == null) {
                    ptm.commit(transaction);
                }
            }
        }
        return TransactionManager.STATUS_NO_TRANSACTION;
    }

    public void registerTransactionSynchronization(TransactionSynchronization ts) {
        TransactionSynchronizationManager.registerSynchronization(new SpringTransactionSynchronizationAdapter(ts));
    }
}