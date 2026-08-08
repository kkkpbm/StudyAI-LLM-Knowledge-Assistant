from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.routers import chat, summarize, graph, plan, review, embeddings, chat_memory, insight, flashcards, documents
from app.utils.logger import logger

app = FastAPI(title="Knowledge AI Agent", version="1.0.0")

# Log startup
logger.info("Starting Knowledge AI Agent application")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler for all unhandled errors."""
    logger.error(f"Unhandled exception: {exc.__class__.__name__}: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "detail": str(exc) if app.debug else "An unexpected error occurred"
        }
    )


app.include_router(chat.router, prefix="/agent")
app.include_router(summarize.router, prefix="/agent")
app.include_router(graph.router, prefix="/agent")
app.include_router(plan.router, prefix="/agent")
app.include_router(review.router, prefix="/agent")
app.include_router(embeddings.router, prefix="/agent")
app.include_router(chat_memory.router, prefix="/agent")
app.include_router(insight.router, prefix="/agent")
app.include_router(flashcards.router, prefix="/agent")
app.include_router(documents.router, prefix="/agent")


@app.get("/health")
def health():
    logger.debug("Health check requested")
    return {"status": "ok"}
