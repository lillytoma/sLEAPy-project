"""Database connection module for PostgreSQL using SQLAlchemy.

Provides utilities to create and configure database engine connections
with connection pooling for efficient database access.
"""

from sqlalchemy import create_engine, pool

def create_engine_connection(db_config):
    """Create SQLAlchemy engine for database connection.
    
    Initializes a PostgreSQL connection pool with optimized settings for
    handling multiple concurrent queries during ETL operations.
    
    Args:
        db_config (dict): Database configuration dictionary containing:
                         - user: PostgreSQL username
                         - password: PostgreSQL password
                         - host: Database server hostname/IP
                         - port: Database server port
                         - dbname: Database name
    
    Returns:
        Engine: SQLAlchemy Engine object configured with connection pooling.
    
    Raises:
        Exception: If there's an error creating the database engine.
    """
    try:
        # Create PostgreSQL connection string using pg8000 driver
        engine = create_engine(
            f"postgresql+pg8000://{db_config['user']}:{db_config['password']}@{db_config['host']}:{db_config['port']}/{db_config['dbname']}",
            # Use QueuePool for thread-safe connection management
            poolclass=pool.QueuePool,
            # Initial size of connection pool
            pool_size=5,
            # Additional connections beyond pool_size when needed
            max_overflow=10,
            # Timeout for getting a connection from the pool
            pool_timeout=30,
            # Recycle connections after 1 hour to avoid stale connections
            pool_recycle=3600,
            # Additional connection options with ETL application identifier
            connect_args={'timeout': 30, 'application_name': 'sleapy_etl'}
        )
        return engine
    except Exception as e:
        print(f"Error creating engine: {e}")
        raise