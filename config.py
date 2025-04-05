import os
import redis

# Redis connection settings
REDIS_HOST = os.environ.get('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.environ.get('REDIS_PORT', 6379))
REDIS_DB = int(os.environ.get('REDIS_DB', 0))
REDIS_PASSWORD = os.environ.get('REDIS_PASSWORD', 'strongredispassword')

# Redis client instance
def get_redis_client():
    return redis.Redis(
        host=REDIS_HOST,
        port=REDIS_PORT,
        db=REDIS_DB,
        password=REDIS_PASSWORD,
        decode_responses=True  # Return strings instead of bytes
    )

# Rate limiter settings
RATE_LIMIT_REQUESTS = int(os.environ.get('RATE_LIMIT_REQUESTS', 10))  # Number of requests allowed
RATE_LIMIT_WINDOW = int(os.environ.get('RATE_LIMIT_WINDOW', 7))      # Time window in seconds

# Distributed lock settings
LOCK_TIMEOUT = int(os.environ.get('LOCK_TIMEOUT', 10))  # Lock timeout in seconds
LOCK_RETRY_COUNT = int(os.environ.get('LOCK_RETRY_COUNT', 3))  # Number of retry attempts
LOCK_RETRY_DELAY = float(os.environ.get('LOCK_RETRY_DELAY', 0.2))  # Delay between retries in seconds 