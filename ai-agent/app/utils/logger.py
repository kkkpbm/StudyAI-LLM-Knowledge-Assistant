"""Logging configuration for the application."""
import logging
import sys
from pathlib import Path

from app.config import CHROMA_PERSIST_DIR


def setup_logging(log_level: str = "INFO", log_file: str | None = None) -> logging.Logger:
    """
    Configure application logging.
    
    Args:
        log_level: Log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
        log_file: Optional log file path
    
    Returns:
        Configured logger instance
    """
    # Create logs directory if logging to file
    if log_file:
        log_path = Path(log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Configure root logger
    logger = logging.getLogger("knowledge_ai_agent")
    logger.setLevel(getattr(logging, log_level.upper(), logging.INFO))
    
    # Clear existing handlers
    logger.handlers.clear()
    
    # Create formatter
    formatter = logging.Formatter(
        fmt="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    
    # Console handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)
    
    # File handler (optional)
    if log_file:
        file_handler = logging.FileHandler(log_file, encoding="utf-8")
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    
    # Reduce noise from third-party libraries
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("chromadb").setLevel(logging.WARNING)
    logging.getLogger("sentence_transformers").setLevel(logging.WARNING)
    
    return logger


# Default logger instance
logger = setup_logging(
    log_level="INFO",
    log_file="logs/app.log"
)