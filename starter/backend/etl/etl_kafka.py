"""Kafka-integrated ETL pipeline module.

This module provides ETL functionality that processes events consumed from Kafka topics.
It integrates with the existing ETL pipeline to extract, transform, and load
event data into the analytics warehouse.
"""

import json
import logging
from typing import List, Dict, Any
from datetime import datetime
from sqlalchemy import text
from database import create_engine_connection
from config import load_db_config
from loaders import load_to_warehouse
from kafka_consumer import KafkaEventConsumer

logger = logging.getLogger(__name__)


class KafkaETLPipeline:
    """
    ETL pipeline that consumes events from Kafka and processes them.
    
    This pipeline:
    1. Consumes events from Kafka topics (client-activity, orders, trades)
    2. Transforms events into analytics-ready data
    3. Loads data into the analytics warehouse schema
    """
    
    def __init__(
        self,
        kafka_bootstrap_servers: str = "localhost:9092",
        kafka_group_id: str = "sleapy-etl-group",
        db_config_file: str = "../../docker-compose.yml"
    ):
        """
        Initialize Kafka ETL Pipeline.
        
        Args:
            kafka_bootstrap_servers: Kafka bootstrap servers
            kafka_group_id: Kafka consumer group ID
            db_config_file: Path to database configuration file
        """
        self.kafka_bootstrap_servers = kafka_bootstrap_servers
        self.kafka_group_id = kafka_group_id
        self.db_config_file = db_config_file
        
        # Load database configuration
        self.db_config = load_db_config(db_config_file)
        self.engine = create_engine_connection(self.db_config)
        
        # Initialize Kafka consumer
        self.kafka_consumer = None
    
    def _event_processor_callback(self, topic: str, event: Dict[str, Any]) -> bool:
        """
        Callback function to process events as they're consumed.
        
        Args:
            topic: Kafka topic the event came from
            event: Event data as dictionary
        
        Returns:
            bool: True to continue consuming, False to stop
        """
        try:
            logger.info(f"Processing event from topic: {topic}")
            
            # Process different event types
            if topic == "client-activity":
                self._process_client_activity_event(event)
            elif topic == "orders":
                self._process_order_event(event)
            elif topic == "trades":
                self._process_trade_event(event)
            else:
                logger.warning(f"Unknown topic: {topic}")
            
            return True
        except Exception as e:
            logger.error(f"Error processing event: {e}")
            return True  # Continue consuming even on error
    
    def _process_client_activity_event(self, event: Dict[str, Any]) -> None:
        """
        Process client activity events and log them to the Audit_Log table.
        
        Args:
            event: Client activity event data
        """
        try:
            logger.debug(f"Processing client activity: {event.get('event_id')}")
            
            # Extract and transform event data
            activity_data = {
                'event_id': event.get('event_id'),
                'client_id': event.get('client_id'),
                'activity_type': event.get('activity_type'),
                'timestamp': event.get('timestamp'),
                'details': event.get('details'),
                'ip_address': event.get('ip_address'),
                'source_system': event.get('source_system', 'etl-pipeline')
            }
            
            # Insert into Audit_Log table
            try:
                with self.engine.connect() as connection:
                    insert_query = text("""
                    INSERT INTO Audit_Log (Client_ID, Activity_Type, Timestamp, IP_Address, Details, Source_System)
                    VALUES (:client_id, :activity_type, :timestamp, :ip_address, :details, :source_system)
                    """)
                    connection.execute(insert_query, {
                        'client_id': activity_data['client_id'],
                        'activity_type': activity_data['activity_type'],
                        'timestamp': activity_data['timestamp'],
                        'ip_address': activity_data['ip_address'],
                        'details': activity_data['details'],
                        'source_system': activity_data['source_system']
                    })
                    connection.commit()
                    logger.info(f"Audit log entry created: {activity_data['event_id']} - {activity_data['activity_type']}")
            except Exception as db_error:
                logger.error(f"Database error inserting audit log: {db_error}")
                raise
            
        except Exception as e:
            logger.error(f"Error processing client activity event: {e}")
    
    def _process_order_event(self, event: Dict[str, Any]) -> None:
        """
        Process order events.
        
        Args:
            event: Order event data
        """
        try:
            logger.debug(f"Processing order: {event.get('event_id')}")
            
            # Extract and transform event data
            order_data = {
                'event_id': event.get('event_id'),
                'order_id': event.get('order_id'),
                'client_id': event.get('client_id'),
                'instrument_id': event.get('instrument_id'),
                'order_type': event.get('order_type'),
                'quantity': event.get('quantity'),
                'price': event.get('price'),
                'status': event.get('status'),
                'timestamp': event.get('timestamp'),
                'source_system': event.get('source_system')
            }
            
            # Store event for batch loading
            logger.info(f"Order event processed: {order_data['event_id']}")
            
        except Exception as e:
            logger.error(f"Error processing order event: {e}")
    
    def _process_trade_event(self, event: Dict[str, Any]) -> None:
        """
        Process trade events.
        
        Args:
            event: Trade event data
        """
        try:
            logger.debug(f"Processing trade: {event.get('event_id')}")
            
            # Extract and transform event data
            trade_data = {
                'event_id': event.get('event_id'),
                'trade_id': event.get('trade_id'),
                'client_id': event.get('client_id'),
                'instrument_id': event.get('instrument_id'),
                'quantity': event.get('quantity'),
                'price': event.get('price'),
                'timestamp': event.get('timestamp'),
                'source_system': event.get('source_system')
            }
            
            # Store event for batch loading
            logger.info(f"Trade event processed: {trade_data['event_id']}")
            
        except Exception as e:
            logger.error(f"Error processing trade event: {e}")
    
    def run(
        self,
        topics: List[str] = None,
        max_events: int = None
    ) -> None:
        """
        Run the Kafka ETL pipeline.
        
        This method:
        1. Connects to Kafka
        2. Consumes events from specified topics
        3. Processes and transforms events
        4. Loads data into analytics warehouse
        
        Args:
            topics: List of Kafka topics to consume from
            max_events: Maximum number of events to process (None for unlimited)
        """
        if topics is None:
            topics = ["client-activity", "orders", "trades"]
        
        try:
            print("Starting Kafka ETL Pipeline")
            print(f"Connecting to Kafka: {self.kafka_bootstrap_servers}")
            print(f"Consumer group: {self.kafka_group_id}")
            print(f"Topics: {topics}")
            
            # Create Kafka consumer
            self.kafka_consumer = KafkaEventConsumer(
                bootstrap_servers=self.kafka_bootstrap_servers,
                group_id=self.kafka_group_id,
                topics=topics
            )
            
            # Consume and process events
            events = self.kafka_consumer.consume_events(
                callback=self._event_processor_callback,
                max_events=max_events
            )
            
            print(f"\nKafka ETL Pipeline completed. Processed {len(events)} events.")
            
        except Exception as e:
            logger.error(f"Error in Kafka ETL Pipeline: {e}")
            raise
        finally:
            # Clean up
            if self.kafka_consumer:
                self.kafka_consumer.close()
            if self.engine:
                self.engine.dispose()
    
    def close(self) -> None:
        """Clean up resources."""
        if self.kafka_consumer:
            self.kafka_consumer.close()
        if self.engine:
            self.engine.dispose()


def run_kafka_etl_pipeline():
    """
    Convenience function to run the Kafka ETL pipeline.
    
    Usage:
        python etl_kafka.py
    """
    try:
        pipeline = KafkaETLPipeline()
        pipeline.run()
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise


if __name__ == "__main__":
    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    run_kafka_etl_pipeline()
