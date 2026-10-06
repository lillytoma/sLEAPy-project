"""Kafka consumer module for the ETL pipeline.

This module provides utilities for consuming events from Kafka topics
and integrating them into the ETL pipeline.
"""

import json
import logging
from typing import Callable, Optional, List
from kafka import KafkaConsumer
from kafka.errors import KafkaError

logger = logging.getLogger(__name__)


class KafkaEventConsumer:
    """
    Kafka consumer for consuming trading events in the ETL pipeline.
    
    This consumer reads events from Kafka topics and can optionally
    process them through a provided callback function.
    """
    
    def __init__(
        self,
        bootstrap_servers: str,
        group_id: str,
        topics: List[str],
        auto_offset_reset: str = 'earliest',
        max_poll_records: int = 100
    ):
        """
        Initialize Kafka consumer.
        
        Args:
            bootstrap_servers: Kafka bootstrap servers (e.g., 'localhost:9092')
            group_id: Consumer group ID (e.g., 'sleapy-etl-group')
            topics: List of topics to consume from
            auto_offset_reset: Offset reset strategy ('earliest' or 'latest')
            max_poll_records: Maximum records per poll
        """
        self.bootstrap_servers = bootstrap_servers
        self.group_id = group_id
        self.topics = topics
        self.consumer = None
        self.max_poll_records = max_poll_records
        
        self._create_consumer(auto_offset_reset)
    
    def _create_consumer(self, auto_offset_reset: str) -> None:
        """Create Kafka consumer with JSON deserialization."""
        try:
            self.consumer = KafkaConsumer(
                *self.topics,
                bootstrap_servers=self.bootstrap_servers.split(','),
                group_id=self.group_id,
                auto_offset_reset=auto_offset_reset,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                key_deserializer=lambda m: m.decode('utf-8') if m else None,
                max_poll_records=self.max_poll_records,
                session_timeout_ms=30000,
                heartbeat_interval_ms=10000
            )
            logger.info(
                f"Kafka consumer created for topics: {self.topics} "
                f"with group: {self.group_id}"
            )
        except Exception as e:
            logger.error(f"Failed to create Kafka consumer: {e}")
            raise
    
    def consume_events(
        self,
        callback: Optional[Callable[[str, dict], bool]] = None,
        timeout_ms: int = 1000,
        max_events: Optional[int] = None
    ) -> List[dict]:
        """
        Consume events from Kafka topics.
        
        Args:
            callback: Optional callback function to process each event
                     (topic, event_dict) -> bool (return True to continue)
            timeout_ms: Timeout for polling (milliseconds)
            max_events: Maximum events to consume (None for unlimited)
        
        Returns:
            List of consumed events
        """
        events = []
        event_count = 0
        
        try:
            logger.info("Starting to consume events from Kafka...")
            
            for message in self.consumer:
                try:
                    event = {
                        'topic': message.topic,
                        'partition': message.partition,
                        'offset': message.offset,
                        'timestamp': message.timestamp,
                        'key': message.key,
                        'value': message.value
                    }
                    
                    events.append(event)
                    event_count += 1
                    
                    logger.debug(
                        f"Consumed event from topic '{message.topic}': "
                        f"{event.get('key')} (offset: {message.offset})"
                    )
                    
                    # Call callback if provided
                    if callback:
                        should_continue = callback(message.topic, message.value)
                        if not should_continue:
                            logger.info("Callback requested to stop consuming")
                            break
                    
                    # Check max events limit
                    if max_events and event_count >= max_events:
                        logger.info(f"Reached max events limit: {max_events}")
                        break
                        
                except json.JSONDecodeError as e:
                    logger.error(f"Failed to decode JSON message: {e}")
                    continue
                except Exception as e:
                    logger.error(f"Error processing event: {e}")
                    continue
            
            logger.info(f"Consumed {event_count} events from Kafka")
            return events
            
        except KafkaError as e:
            logger.error(f"Kafka error during consumption: {e}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error during consumption: {e}")
            raise
    
    def close(self) -> None:
        """Close the Kafka consumer."""
        if self.consumer:
            try:
                self.consumer.close()
                logger.info("Kafka consumer closed")
            except Exception as e:
                logger.error(f"Error closing consumer: {e}")
    
    def __enter__(self):
        """Context manager entry."""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()
