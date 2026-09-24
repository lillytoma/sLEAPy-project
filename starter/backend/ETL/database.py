from sqlalchemy import create_engine

def create_engine_connection(db_config):
    """Create SQLAlchemy engine for database connection"""
    try:
        engine = create_engine(
            f"postgresql+pg8000://{db_config['user']}:{db_config['password']}@{db_config['host']}:{db_config['port']}/{db_config['dbname']}"
        )
        return engine
    except Exception as e:
        print(f"Error creating engine: {e}")
        raise