"""
Redis Distributed Lock Example

This module demonstrates how to implement a distributed lock using Redis.
It shows a counter update process both with and without using a distributed lock,
highlighting the race condition issues that can occur without proper locking.
"""

import time
import random
import threading
import uuid
from config import get_redis_client, LOCK_TIMEOUT, LOCK_RETRY_COUNT, LOCK_RETRY_DELAY

class RedisDistributedLock:
    def __init__(self, lock_name, redis_client=None):
        self.redis = redis_client if redis_client else get_redis_client()
        self.lock_name = f"lock:{lock_name}"
        self.lock_value = None
    
    def acquire(self, timeout=None, retry_count=None, retry_delay=None):
        timeout = timeout or LOCK_TIMEOUT
        retry_count = retry_count or LOCK_RETRY_COUNT
        retry_delay = retry_delay or LOCK_RETRY_DELAY
        
        # Generate a unique value for this lock instance (using uuid instead of thread id for more uniqueness)
        self.lock_value = f"{threading.get_ident()}:{uuid.uuid4()}"
        
        # Try to acquire the lock with retries
        for attempt in range(retry_count + 1):
            # Use SET NX (only set if key doesn't exist) with expiration
            if self.redis.set(self.lock_name, self.lock_value, ex=timeout, nx=True):
                return True
                
            # Wait before retrying with some randomization to avoid thundering herd
            if attempt < retry_count:
                # Add jitter to retry delay (between 75% and 125% of the specified delay)
                jittered_delay = retry_delay * (0.75 + random.random() * 0.5)
                time.sleep(jittered_delay)
                
        return False
    
    def release(self):
        """
        Release the lock if it's still held by this instance.
        
        Returns:
            bool: True if the lock was released, False otherwise
        """
        # Use Lua script to ensure atomic release operation
        release_script = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """
        
        # Only attempt to release if we have a lock value
        if not self.lock_value:
            return False
            
        # Execute script to atomically check and release
        result = self.redis.eval(release_script, 1, self.lock_name, self.lock_value)
        return result == 1
        
    def __enter__(self):
        """Context manager enter method."""
        self.acquire()
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit method."""
        self.release()


def increment_counter_without_lock(redis_client, counter_key, thread_id):
    # Simulate some processing time
    time.sleep(random.uniform(0.01, 0.05))
    
    # Get the current value
    current_value = redis_client.get(counter_key)
    current_value = int(current_value) if current_value else 0
    
    # Simulate more processing time - this creates a race condition window
    time.sleep(random.uniform(0.01, 0.05))
    
    # Increment and store the new value
    new_value = current_value + 1
    redis_client.set(counter_key, new_value)
    
    print(f"Thread {thread_id}: Incremented counter without lock from {current_value} to {new_value}")


def increment_counter_with_lock(redis_client, counter_key, thread_id):
    max_attempts = 3  # Try multiple times to get the lock
    
    for attempt in range(max_attempts):
        # Create a distributed lock
        lock = RedisDistributedLock("counter_lock", redis_client)
        
        # Try to acquire the lock with increased retry count and shorter delay
        if lock.acquire(retry_count=10, retry_delay=0.1):
            try:
                # Simulate some processing time
                time.sleep(random.uniform(0.01, 0.05))
                
                # Get the current value
                current_value = redis_client.get(counter_key)
                current_value = int(current_value) if current_value else 0
                
                # Simulate more processing time - this would create a race condition window
                # but we're protected by the lock
                time.sleep(random.uniform(0.01, 0.05))
                
                # Increment and store the new value
                new_value = current_value + 1
                redis_client.set(counter_key, new_value)
                
                print(f"Thread {thread_id}: Incremented counter with lock from {current_value} to {new_value}")
                return  # Successfully processed, exit the function
            finally:
                # Always release the lock
                lock.release()
        else:
            # If this is not the last attempt, try again after a delay
            if attempt < max_attempts - 1:
                wait_time = 0.2 * (attempt + 1)  # Increasing backoff
                print(f"Thread {thread_id}: Failed to acquire lock, retrying in {wait_time:.2f}s (attempt {attempt+1}/{max_attempts})")
                time.sleep(wait_time)
            else:
                print(f"Thread {thread_id}: Failed to acquire lock after {max_attempts} attempts")


def demonstrate_counter_with_and_without_lock():
    redis_client = get_redis_client()
    counter_key_no_lock = "counter:no_lock"
    counter_key_with_lock = "counter:with_lock"
    
    # Reset counters
    redis_client.set(counter_key_no_lock, 0)
    redis_client.set(counter_key_with_lock, 0)
    
    print("\n=== Redis Distributed Lock Demonstration ===\n")
    
    # Create threads to update counter without locks
    print("Demonstration WITHOUT distributed locks:")
    threads = []
    for i in range(10):
        thread = threading.Thread(
            target=increment_counter_without_lock,
            args=(redis_client, counter_key_no_lock, i)
        )
        threads.append(thread)
        thread.start()
    
    # Wait for all threads to complete
    for thread in threads:
        thread.join()
    
    # Get the final counter value
    final_value_no_lock = redis_client.get(counter_key_no_lock)
    print(f"\nFinal counter value WITHOUT locks: {final_value_no_lock}")
    print(f"Expected value: 10")
    if int(final_value_no_lock) < 10:
        print("Some updates were lost due to race conditions!\n")
    
    print("\nDemonstration WITH distributed locks:")
    # Create threads to update counter with locks
    threads = []
    for i in range(10):
        thread = threading.Thread(
            target=increment_counter_with_lock,
            args=(redis_client, counter_key_with_lock, i)
        )
        threads.append(thread)
        thread.start()
    
    # Wait for all threads to complete
    for thread in threads:
        thread.join()
    
    # Get the final counter value
    final_value_with_lock = redis_client.get(counter_key_with_lock)
    print(f"\nFinal counter value WITH locks: {final_value_with_lock}")
    print(f"Expected value: 10")
    if int(final_value_with_lock) == 10:
        print("All updates were processed correctly with distributed locking!")


if __name__ == "__main__":
    demonstrate_counter_with_and_without_lock()