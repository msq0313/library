/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.communication.client.netty;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joao
 */
class SyncListener implements GenericFutureListener<ChannelFuture> {
            
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private int timeout;
    private int remainingFutures;

    private final Lock futureLock;
    private final Condition enoughCompleted;

    public SyncListener(int timeout) {

        this.timeout = timeout;
        this.remainingFutures = 0;

        this.futureLock = new ReentrantLock();
        this.enoughCompleted = futureLock.newCondition();
    }

    @Override
    public void operationComplete(ChannelFuture f) {

        this.futureLock.lock();

        this.remainingFutures--;

        if (this.remainingFutures <= 0) {

            logger.debug("Signaling that all operations are now done");
            this.enoughCompleted.signalAll();
        }

        logger.debug(this.remainingFutures + " channel operations remaining to complete");

        this.futureLock.unlock();

    }

    public void waitForChannels(int n) {

        this.futureLock.lock();
        if (this.remainingFutures > 0) {

            logger.debug("There are still " + this.remainingFutures + " channel operations pending, waiting to complete");

            try {
                this.enoughCompleted.await(this.timeout, TimeUnit.MILLISECONDS); // timeout if a malicous process refuses to acknowledge the operation as completed
            } catch (InterruptedException ex) {
                logger.error("Interruption while waiting on condition", ex);
            }

        }

            logger.debug("All channel operations completed or timed out");

        this.remainingFutures = n;

        this.futureLock.unlock();
    }
    
    public void setRemainingFutures(int n) {
        
        logger.debug("Remaining futures were at " + this.remainingFutures);
        
        this.remainingFutures += n;
        
        logger.debug("Remaining futures now at " + this.remainingFutures);
    }
}
