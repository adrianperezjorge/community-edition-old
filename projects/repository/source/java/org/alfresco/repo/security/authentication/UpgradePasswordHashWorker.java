/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.security.authentication;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorkerAdaptor;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.patch.PatchDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.JobLockService.JobLockRefreshCallback;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <h1>Upgrade Password Hash Worker</h1>
 * 
 * <h2>What it is</h2>
 * A worker for a scheduled job that checks and upgrades users passwords to the system's preferred encoding.
 * 
 * <h2>Settings that control the behaviour</h2>
 * <ul>
 *  <li><b>${system.upgradePasswordHash.jobBatchSize}</b> - the number of users to process at one time.</li>
 *  <li><b>${system.upgradePasswordHash.jobQueryRange}</b> - the node ID range to query for.
 *         The process will repeat from the first to the last node, querying for up to this many nodes.
 *         Only reduce the value if the NodeDAO query takes a long time.</li>
 *  <li><b>${system.upgradePasswordHash.jobThreadCount}</b> - the number of threads that will handle user checks and changes.
 *         Increase or decrease this to allow for free CPU capacity on the machine executing the job.</li>
 * </ul>
 *
 * @author Gavin Cornwell
 */
public class UpgradePasswordHashWorker implements ApplicationContextAware, InitializingBean
{
    private static final QName LOCK = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "UpgradePasswordHashWorker");
    private static final long LOCK_TTL = 60000L;
    
    private static Log logger = LogFactory.getLog(UpgradePasswordHashWorker.class);
    
    private JobLockService jobLockService;
    private TransactionService transactionService;
    
    private MutableAuthenticationDao authenticationDao;
    private CompositePasswordEncoder passwordEncoder;
    
    private NodeDAO nodeDAO;
    private PatchDAO patchDAO;
    private QNameDAO qnameDAO;
    
    private BehaviourFilter behaviourFilter;
    private ApplicationContext ctx;
    
    private int queryRange = 10000;
    private int threadCount = 2;
    private int batchSize = 100;
    
    public void setJobLockService(JobLockService jobLockService)
    {
        this.jobLockService = jobLockService;
    }
    
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setAuthenticationDao(MutableAuthenticationDao authenticationDao)
    {
        this.authenticationDao = authenticationDao;
    }
    
    public void setCompositePasswordEncoder(CompositePasswordEncoder passwordEncoder)
    {
        this.passwordEncoder = passwordEncoder;
    }
    
    public void setPatchDAO(PatchDAO patchDAO)
    {
        this.patchDAO = patchDAO;
    }

    public void setNodeDAO(NodeDAO nodeDAO)
    {
        this.nodeDAO = nodeDAO;
    }

    public void setQnameDAO(QNameDAO qnameDAO)
    {
        this.qnameDAO = qnameDAO;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }
    
    /**
     * Sets the number of users to retrieve from the repository in each query.
     *  
     * @param queryRange The query range
     */
    public void setQueryRange(int queryRange)
    {
        this.queryRange = queryRange;
    }
    
    /**
     * Sets the number of threads to use to process users.
     * 
     * @param threadCount Number of threads
     */
    public void setThreadCount(int threadCount)
    {
        this.threadCount = threadCount;
    }
    
    /**
     * Sets the number of users to process at one time.
     * 
     * @param batchSize The batch size
     */
    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }

    /**
     * Set the application context for event publishing during batch processing
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.ctx = applicationContext;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception
    {
        ParameterCheck.mandatory("jobLockService", jobLockService);
        ParameterCheck.mandatory("transactionService", transactionService);
        ParameterCheck.mandatory("authenticationDao", authenticationDao);
        ParameterCheck.mandatory("compositePasswordEncoder", passwordEncoder);
        ParameterCheck.mandatory("nodeDAO", nodeDAO);
        ParameterCheck.mandatory("patchDAO", patchDAO);
        ParameterCheck.mandatory("qnameDAO", qnameDAO);
        ParameterCheck.mandatory("behaviourFilter", behaviourFilter);
    }
    
    /**
     * Performs the work, including logging details of progress.
     */
    public UpgradePasswordHashWorkResult execute()
    {
        // Build refresh callback
        final UpgradePasswordHashWorkResult progress = new UpgradePasswordHashWorkResult();
        JobLockRefreshCallback lockCallback = new JobLockRefreshCallback()
        {
            @Override
            public void lockReleased()
            {
                progress.inProgress.set(false);
            }
            
            @Override
            public boolean isActive()
            {
                return progress.inProgress.get();
            }
        };
        
        String lockToken = null;
        try
        {
            progress.inProgress.set(true);
            
            // Get the lock
            lockToken = jobLockService.getLock(LOCK, LOCK_TTL);
            
            // Start the refresh timer
            jobLockService.refreshLock(lockToken, LOCK, LOCK_TTL, lockCallback);

            // Now we know that we'll do something
            if (logger.isInfoEnabled())
            {
                logger.info("Starting upgrade password hash job.");
            }
            
            // Do the work
            doWork(progress);
            
            // Done
            if (logger.isDebugEnabled())
            {
                logger.debug("Upgrade password hash job " + progress);
            }
        }
        catch (LockAcquisitionException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Skipping upgrade password hash job: " + e.getMessage());
            }
        }
        catch (Exception e)
        {
            progress.inProgress.set(false);
            logger.error("Upgrade password hash job " + progress);
            logger.error("Stopping upgrade password hash job with exception.", e);
        }
        finally
        {
            if (lockToken != null)
            {
                jobLockService.releaseLock(lockToken, LOCK);
            }
            
            progress.inProgress.set(false);
        }
        
        // Done
        return progress;
    }
    
    /**
     * @param progress          the thread-safe progress
     */
    private synchronized void doWork(UpgradePasswordHashWorkResult progress) throws Exception
    {
        // Build batch processor
        BatchProcessWorkProvider<Long> workProvider = new UpgradePasswordHashWorkProvider(progress);
        BatchProcessWorker<Long> worker = new UpgradePasswordHashBatch(progress);
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        retryingTransactionHelper.setForceWritable(true);

        //Create the QNames if they don't exist
        retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
        {
            @Override
            public Void execute() throws Throwable
            {
                qnameDAO.getOrCreateQName(ContentModel.PROP_PASSWORD_HASH);
                qnameDAO.getOrCreateQName(ContentModel.PROP_HASH_INDICATOR);
                return null;
            }
        }, false, true);

        BatchProcessor<Long> batchProcessor = new BatchProcessor<Long>(
                "UpgradePasswordHashWorker",
                retryingTransactionHelper,
                workProvider,
                threadCount,
                batchSize,
                ctx,
                logger,
                1000);
        batchProcessor.process(worker, true);
    }
    
    /**
     * Work provider for batch job providing noderefs representing users to process
     */
    private class UpgradePasswordHashWorkProvider implements BatchProcessWorkProvider<Long>
    {
        private final long maxNodeId;
        private final UpgradePasswordHashWorkResult progress;
        private final Pair<Long, QName> userTypeId;
        
        private UpgradePasswordHashWorkProvider(UpgradePasswordHashWorkResult progress)
        {
            this.progress = progress;
            this.maxNodeId = patchDAO.getMaxAdmNodeID();
            this.userTypeId = qnameDAO.getQName(ContentModel.TYPE_USER);
        }
        
        @Override
        public int getTotalEstimatedWorkSize()
        {
            // execute a query to get total number of user nodes in the system.
            long totalUserCount = patchDAO.getCountNodesWithTypId(ContentModel.TYPE_USER);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Max NodeID: " + this.maxNodeId);
                logger.debug("Total number of users: " + totalUserCount);
            }
            
            return (int)totalUserCount;
        }

        @Override
        public Collection<Long> getNextWork()
        {
            // Check that there are not too many errors
            if (progress.errors.get() > 1000)
            {
                logger.warn("Upgrade password hash work terminating; too many errors.");
                return Collections.emptyList();
            }
            
            // Keep shifting the query window up until we get results or we hit the original max node ID
            List<Long> ret = Collections.emptyList();
            while (ret.isEmpty() && progress.currentMinNodeId.get() < maxNodeId)
            {
                // Calculate the node ID range
                Long minNodeId = null;
                if (progress.currentMinNodeId.get() == 0L)
                {
                    minNodeId = 1L;
                    progress.currentMinNodeId.set(minNodeId);
                }
                else
                {
                    minNodeId = progress.currentMinNodeId.addAndGet(queryRange);
                }
                long maxNodeId = minNodeId + queryRange;
                
                // Query for the next set of users
                ret = patchDAO.getNodesByTypeQNameId(this.userTypeId.getFirst(), minNodeId, maxNodeId);
            }
            
            // Done
            if (logger.isDebugEnabled())
            {
                logger.debug("Upgrade password hash work provider found " + ret.size() + " users.");
            }
            
            return ret;
        }
    }
    
    /**
     * Class that does the actual node manipulation to upgrade the password hash.
     */
    private class UpgradePasswordHashBatch extends BatchProcessWorkerAdaptor<Long>
    {
        private final UpgradePasswordHashWorkResult progress;
        
        private UpgradePasswordHashBatch(UpgradePasswordHashWorkResult progress)
        {
            this.progress = progress;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void process(Long nodeId) throws Throwable
        {
            progress.usersProcessed.incrementAndGet();
            
            try
            {
                // get properties for the user
                Map<QName, Serializable> userProps = nodeDAO.getNodeProperties(nodeId);
                
                // get the hash indicator property
                List<String> hashIndicator = (List<String>)userProps.get(ContentModel.PROP_HASH_INDICATOR);
                
                // get the username
                String username = (String)userProps.get(ContentModel.PROP_USER_USERNAME);
                
                // determine whether we need to upgrade the password hash for the user
                if (hashIndicator == null || !passwordEncoder.lastEncodingIsPreferred(hashIndicator))
                {
                    progress.usersChanged.incrementAndGet();
                    
                    // We do not want any behaviours associated with our transactions
                    behaviourFilter.disableBehaviour();

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Upgrading password hash for user: " + username);
                    }
                    authenticationDao.hashUserPassword(username);

                }
                else if (logger.isTraceEnabled())
                {
                    logger.trace("User '" + username + "' has preferred encoding");
                }
            }
            catch (Exception e)
            {
                // Record the failure
                progress.errors.incrementAndGet();
                
                // Rethrow so that the processing framework can handle things
                throw e;
            }
        }

        @Override
        public String getIdentifier(Long nodeId)
        {
            return (String)nodeDAO.getNodeProperty(nodeId, ContentModel.PROP_USER_USERNAME);
        }
    }

    /**
     * Thread-safe helper class to carry the job progress information.
     */
    public static class UpgradePasswordHashWorkResult
    {
        private final AtomicBoolean inProgress = new AtomicBoolean(false);
        private final AtomicInteger usersProcessed = new AtomicInteger(0);
        private final AtomicInteger usersChanged = new AtomicInteger(0);
        private final AtomicInteger errors = new AtomicInteger(0);
        private final AtomicLong currentMinNodeId = new AtomicLong(0L);
        
        @Override
        public String toString()
        {
            String part1 = "Changed";
            String part2 = String.format(" %4d out of a potential %4d users. ", usersChanged.get(), usersProcessed.get());
            String part3 = String.format("[%2d Errors]", errors.get());
            return part1 + part2 + part3;
        }
        
        public int getUsersProcessed()
        {
            return usersProcessed.get();
        }
        
        public int getUsersChanged()
        {
            return usersChanged.get();
        }
        
        public int getErrors()
        {
            return errors.get();
        }
    }
    
    /**
     * A scheduled job that checks and upgrades users passwords to the system's preferred encoding.
     * <p>
     * Job data: 
     * <ul>
     *  <li><b>upgradePasswordHashWorker</b> - The worker that performs the actual processing.</li>
     * </ul>
     * 
     * @see UpgradePasswordHashWorker
     */
    public static class UpgradePasswordHashJob implements Job
    {
        public static final String JOB_DATA_WORKER = "upgradePasswordHashWorker";
        
        public void execute(JobExecutionContext context) throws JobExecutionException
        {
            JobDataMap jobData = context.getJobDetail().getJobDataMap();
            
            // extract the content Cleanup to use
            Object upgradePasswordHashWorkerObj = jobData.get(JOB_DATA_WORKER);
            if (upgradePasswordHashWorkerObj == null || !(upgradePasswordHashWorkerObj instanceof UpgradePasswordHashWorker))
            {
                throw new AlfrescoRuntimeException(
                        "UpgradePasswordHashJob data '" + JOB_DATA_WORKER + "' must reference a " + UpgradePasswordHashWorker.class.getSimpleName());
            }
            
            UpgradePasswordHashWorker worker = (UpgradePasswordHashWorker)upgradePasswordHashWorkerObj;
            worker.execute();
        }
    }
}
