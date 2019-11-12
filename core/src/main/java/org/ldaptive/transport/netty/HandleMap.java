/* See LICENSE for licensing and NOTICE for copyright. */
package org.ldaptive.transport.netty;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ldaptive.LdapException;
import org.ldaptive.extended.UnsolicitedNotification;
import org.ldaptive.transport.DefaultOperationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for operation handles that are waiting on a response from the LDAP server.
 *
 * @author  Middleware Services
 */
final class HandleMap
{

  /** Logger for this class. */
  private static final Logger LOGGER = LoggerFactory.getLogger(HandleMap.class);

  /** Map of message IDs to their operation handle. */
  private final Map<Integer, DefaultOperationHandle> pending = new ConcurrentHashMap<>();

  /** Only one notification can occur at a time. */
  private final AtomicBoolean notificationLock = new AtomicBoolean();

  /** Whether this queue is currently accepting new handles. */
  private boolean open;


  /**
   * Creates a new handle map.
   */
  HandleMap() {}


  /**
   * Open this queue to receive new handles.
   */
  public void open()
  {
    open = true;
  }


  /**
   * Close the queue to new handles.
   */
  public void close()
  {
    open = false;
  }


  /**
   * Returns the operation handle for the supplied message id. Returns null if this queue is not open.
   *
   * @param  id  message id
   *
   * @return  operation handle or null
   */
  public DefaultOperationHandle get(final int id)
  {
    return open ? pending.get(id) : null;
  }


  /**
   * Removes the operation handle from the supplied message id. Returns null if this queue is not open.
   *
   * @param  id  message id
   *
   * @return  operation handle or null
   */
  public DefaultOperationHandle remove(final int id)
  {
    return open ? pending.remove(id) : null;
  }


  /**
   * Puts the supplied operation handle into the queue if the supplied id doesn't already exist in the queue.
   *
   * @param  id  message id
   * @param  handle  to put
   *
   * @return  null or existing operation handle for the id
   *
   * @throws  LdapException  if this queue is not open
   */
  public DefaultOperationHandle put(final int id, final DefaultOperationHandle handle)
    throws LdapException
  {
    if (!open) {
      throw new LdapException("Connection is closed, could not store handle " + handle);
    }
    return pending.putIfAbsent(id, handle);
  }


  /**
   * Returns all the operation handles in the queue.
   *
   * @return  all operation handles
   */
  public Collection<DefaultOperationHandle> handles()
  {
    return pending.values();
  }


  /**
   * Returns the size of this queue.
   *
   * @return  queue size
   */
  public int size()
  {
    return pending.size();
  }


  /**
   * Removes all operation handles from the queue.
   */
  public void clear()
  {
    pending.clear();
  }


  /**
   * Invokes {@link DefaultOperationHandle#abandon()} for all handles that have sent a request but not received a
   * response.
   */
  public void abandonRequests()
  {
    if (notificationLock.compareAndSet(false, true)) {
      try {
        final Iterator<DefaultOperationHandle> i = pending.values().iterator();
        while (i.hasNext()) {
          final DefaultOperationHandle h = i.next();
          if (h.getSentTime() != null && h.getReceivedTime() == null) {
            i.remove();
            h.abandon();
          }
        }
      } finally {
        notificationLock.set(false);
      }
    } else {
      LOGGER.debug("Handle notification is already in progress");
    }
  }


  /**
   * Notifies all operation handles in the queue that an exception has occurred. See {@link
   * DefaultOperationHandle#exception(Throwable)}. This method removes all handles from the queue.
   *
   * @param  e  exception to provides to handles
   */
  public void notifyOperationHandles(final Throwable e)
  {
    if (notificationLock.compareAndSet(false, true)) {
      try {
        final Iterator<DefaultOperationHandle> i = pending.values().iterator();
        while (i.hasNext()) {
          final DefaultOperationHandle h = i.next();
          i.remove();
          h.exception(e);
        }
      } finally {
        notificationLock.set(false);
      }
    } else {
      LOGGER.debug("Handle notification is already in progress");
    }
  }


  /**
   * Send the supplied notification to all handles waiting for a response.
   *
   * @param  notification  to send to response handles
   */
  public void notifyOperationHandles(final UnsolicitedNotification notification)
  {
    if (notificationLock.compareAndSet(false, true)) {
      try {
        pending.values().forEach(h -> {
          if (h.getSentTime() != null && h.getReceivedTime() == null) {
            h.unsolicitedNotification(notification);
          }
        });
      } finally {
        notificationLock.set(false);
      }
    } else {
      LOGGER.debug("Handle notification is already in progress");
    }
  }


  @Override
  public String toString()
  {
    return new StringBuilder(
      getClass().getName()).append("@").append(hashCode()).append("::")
      .append("open=").append(open).append(", ")
      .append("handles=").append(pending).toString();
  }
}
