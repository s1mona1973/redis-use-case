# Redis Use Cases Examples

This repository demonstrates common Redis use cases with Python, focusing on:

1. **Distributed Locking**: Coordinate access to shared resources across distributed systems

## Prerequisites

- Python 3.6+
- Redis server (local or remote)

## Installation

```bash
# Clone the repository
cd redis-use-cases

# Install dependencies
pip install redis
python distributed_lock.py
```

## Configuration

Modify the Redis connection settings in `config.py` if needed:

```python
REDIS_HOST = os.environ.get('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.environ.get('REDIS_PORT', 6379))
```

You can also set these values via environment variables.

### Distributed Lock

The distributed lock provides synchronized access to shared resources across multiple processes or servers.

This example:

- Compares counter updates with and without distributed locking
- Shows how race conditions can lead to lost updates without proper locking
- Demonstrates how to implement and use a Redis-based distributed lock

### Distributed Locking

Redis distributed locking uses:

- Atomic operations to ensure only one process holds the lock at a time
- Lock timeouts to prevent deadlocks
- Unique lock values to prevent lock stealing
- Retry mechanisms for high availability

## Real-World Applications

- **Distributed Locking**: Database updates, file access, financial transactions, scheduled tasks

## Additional Redis Use Cases

Other common Redis use cases not demonstrated in this repo:

- Caching
- Pub/Sub messaging
- Leader election
- Session management
- Job queues
- Real-time analytics
